"""Fala Já – Prototyp eines Live-Sprachtutors für brasilianisches Portugiesisch.

Ablauf pro Gesprächsrunde (alles als Datenstrom):
  Browser (Spracherkennung) ─► WebSocket ─► Claude (Streaming)
      ─► Satz-Zerteiler ─► ElevenLabs (pro Satz, parallel) ─► Browser spielt ab

Sobald der/die Lernende dazwischenspricht, schickt der Browser "interrupt" und
die laufende Antwort (Claude-Stream + Sprachausgabe) wird sofort abgebrochen.
"""

from __future__ import annotations

import asyncio
import base64
import hashlib
import hmac
import html
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import AsyncIterator
from urllib.parse import parse_qs

import anthropic
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, Request, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse, HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles

BASE_DIR = Path(__file__).parent
STATIC_DIR = BASE_DIR / "static"

# Schlüssel aus fala-ja/.env laden (bereits gesetzte Umgebungsvariablen haben Vorrang)
load_dotenv(BASE_DIR / ".env")

MODEL = os.getenv("FALA_MODEL", "claude-opus-5-5")
EFFORT = os.getenv("FALA_EFFORT", "low")  # low = schnellste Antworten
FAKE_LLM = os.getenv("FALA_FAKE_LLM") == "1"  # Testmodus ohne API-Schlüssel

ELEVEN_KEY = os.getenv("ELEVENLABS_API_KEY", "")
ELEVEN_MODEL = os.getenv("ELEVENLABS_MODEL", "eleven_flash_v2_5")
# Standard ist eine vorgefertigte ElevenLabs-Stimme. Für echten brasilianischen
# Akzent eine pt-BR-Stimme aus der Voice Library wählen und ihre ID eintragen.
ELEVEN_VOICE_DEFAULT = os.getenv("ELEVENLABS_VOICE_ID", "EXAVITQu4vr4xnSDxMaL")
# Ausgewählte brasilianische Stimmen (per .env überschreibbar)
ELEVEN_VOICES = {
    "carioca": os.getenv("ELEVENLABS_VOICE_ID_CARIOCA") or "GM2UA3fbsIaLHcswCDX9",
    "paulista": os.getenv("ELEVENLABS_VOICE_ID_PAULISTA") or "wTABSOIKENuRDeLiHYDF",
}

MAX_HISTORY = 40  # Nachrichten, die an Claude gehen

# Passwortschutz für die Online-Version (leer = kein Schutz, z. B. lokal)
APP_PASSWORD = os.getenv("APP_PASSWORD", "")
AUTH_COOKIE = "fala_auth"

START_PROMPT = (
    "(Der Lernende hat die App gerade geöffnet. Begrüße ihn kurz und herzlich "
    "und stell eine einfache Frage, um das Gespräch zu beginnen.)"
)

LEVELS = {
    "A1": "Anfänger: sehr einfache Wörter, kurze Sätze, langsam und deutlich, nur Präsens",
    "A2": "Grundkenntnisse: einfache Alltagssätze, Präsens und Vergangenheit",
    "B1": "Mittelstufe: natürliches Alltagsportugiesisch, gelegentlich Redewendungen",
    "B2": "Fortgeschritten: normales Sprechtempo, Umgangssprache und Redewendungen",
}

PERSONAS = {
    "carioca": (
        "Du bist Ana, 28, aus Rio de Janeiro (Carioca). Du bist warmherzig, "
        "lustig und geduldig, liebst den Strand, Samba und gutes Essen."
    ),
    "paulista": (
        "Du bist Bruno, 31, aus São Paulo (Paulistano). Du bist freundlich, "
        "neugierig und entspannt, liebst Kaffee, Musik und die Food-Szene der Stadt."
    ),
}


@dataclass
class Settings:
    level: str = "A2"
    accent: str = "carioca"
    mode: str = "conversa"  # conversa | pergunta

    @classmethod
    def from_msg(cls, msg: dict) -> "Settings":
        s = cls()
        if msg.get("level") in LEVELS:
            s.level = msg["level"]
        if msg.get("accent") in PERSONAS:
            s.accent = msg["accent"]
        if msg.get("mode") in ("conversa", "pergunta"):
            s.mode = msg["mode"]
        return s


def build_system(s: Settings) -> str:
    prompt = f"""{PERSONAS[s.accent]}
Du bist Sprachtutor:in für brasilianisches Portugiesisch. Dein Gegenüber spricht Deutsch als Muttersprache, Niveau {s.level} ({LEVELS[s.level]}).

Deine Antworten werden sofort laut vorgelesen. Schreibe deshalb gesprochene Sprache:
- Kein Markdown, keine Listen, keine Emojis, keine Lautschrift.
- Kurz: höchstens zwei bis drei kurze Sätze pro Antwort.
- Echtes brasilianisches Alltagsportugiesisch (você, a gente, tá, né, pra). Niemals europäisches Portugiesisch.
- Die Eingaben des Lernenden kommen aus einer Spracherkennung: ignoriere fehlende Satzzeichen, Groß- und Kleinschreibung und offensichtliche Erkennungsfehler.

Korrekturen:
- Hatte der letzte Satz des Lernenden einen echten Fehler, beginne deine Antwort mit genau einer Zeile in diesem Format:
  [[<falscher Ausschnitt> → <richtige Form> · <ganz kurze Erklärung auf Deutsch>]]
  Diese Zeile wird nicht vorgelesen, sondern angezeigt. Höchstens eine Korrektur pro Antwort, und nur bei echten Fehlern.
- Verwende danach die richtige Form beiläufig in deiner Antwort.
"""
    if s.mode == "conversa":
        prompt += """
Modus Gespräch:
- Antworte nur auf Portugiesisch.
- Ende fast immer mit einer kurzen Rückfrage, damit das Gespräch weiterläuft.
- Spricht der Lernende Deutsch oder kommt nicht weiter, gib ihm einen einfachen portugiesischen Satz, den er nachsprechen kann.
"""
    else:
        prompt += """
Modus Fragen:
- Der Lernende stellt Fragen zur Sprache, meist auf Deutsch. Antworte auf Deutsch, knapp und klar.
- Gib genau einen portugiesischen Beispielsatz und fordere ihn auf, ihn nachzusprechen.
- Spricht der Lernende einen Satz auf Portugiesisch nach, gib kurzes Feedback.
"""
    return prompt


# ---------------------------------------------------------------------------
# Satz-Zerteiler: macht aus dem Token-Strom vorlesbare Stücke und Korrekturen
# ---------------------------------------------------------------------------

_SENTENCE_END = re.compile(r"[.!?…]+[\"'»”)]*\s+")
_MARKDOWN = re.compile(r"[*_#`~>]+")
_EARLY_SPLIT_AT = 140  # sehr lange Sätze früher abschneiden


def _clean(text: str) -> str:
    return re.sub(r"\s+", " ", _MARKDOWN.sub("", text)).strip()


class SpeechChunker:
    """Zerteilt gestreamten Text in ("speech", Satz) und ("note", Korrektur)."""

    def __init__(self) -> None:
        self.buf = ""

    def feed(self, text: str) -> list[tuple[str, str]]:
        self.buf += text
        return self._drain(final=False)

    def flush(self) -> list[tuple[str, str]]:
        return self._drain(final=True)

    def _drain(self, final: bool) -> list[tuple[str, str]]:
        out: list[tuple[str, str]] = []
        while True:
            b = self.buf
            if not b.strip():
                if final:
                    self.buf = ""
                break

            tag = b.find("[[")
            m = _SENTENCE_END.search(b)
            cut = m.end() if m else None
            nl = b.find("\n")
            if nl != -1 and (cut is None or nl + 1 < cut):
                cut = nl + 1

            # Korrektur-Markierung [[...]] kommt vor dem nächsten Satzende
            if tag != -1 and (cut is None or tag < cut):
                before = b[:tag]
                if before.strip():
                    out.append(("speech", _clean(before)))
                    self.buf = b[tag:]
                    continue
                end = b.find("]]", tag)
                if end == -1:
                    if final:
                        note = _clean(b[tag + 2 :])
                        if note:
                            out.append(("note", note))
                        self.buf = ""
                    break
                note = _clean(b[tag + 2 : end])
                if note:
                    out.append(("note", note))
                self.buf = b[end + 2 :]
                continue

            if cut is None:
                if final:
                    seg = _clean(b)
                    if seg:
                        out.append(("speech", seg))
                    self.buf = ""
                elif len(b) > _EARLY_SPLIT_AT:
                    pos = b.rfind(", ", 0, _EARLY_SPLIT_AT)
                    if pos == -1:
                        pos = b.rfind(" ", 0, _EARLY_SPLIT_AT)
                    if pos > 0:
                        out.append(("speech", _clean(b[: pos + 1])))
                        self.buf = b[pos + 1 :]
                        continue
                break

            seg = _clean(b[:cut])
            self.buf = b[cut:]
            if seg:
                out.append(("speech", seg))
        return out


# ---------------------------------------------------------------------------
# Sprachmodell
# ---------------------------------------------------------------------------


class Refused(Exception):
    pass


_anthropic_client: anthropic.AsyncAnthropic | None = None


def _claude() -> anthropic.AsyncAnthropic:
    global _anthropic_client
    if _anthropic_client is None:
        _anthropic_client = anthropic.AsyncAnthropic()
    return _anthropic_client


async def claude_stream(system: str, messages: list[dict]) -> AsyncIterator[str]:
    async with _claude().beta.messages.stream(
        model=MODEL,
        max_tokens=4096,
        system=system,
        messages=messages,
        output_config={"effort": EFFORT},
        # Lehnt ein Sicherheitsfilter ab, übernimmt serverseitig ein Ersatzmodell
        betas=["server-side-fallback-2026-07-01"],
        fallbacks="default",
    ) as stream:
        async for text in stream.text_stream:
            yield text
        final = await stream.get_final_message()
        if final.stop_reason == "refusal":
            raise Refused()


async def fake_stream(system: str, messages: list[dict]) -> AsyncIterator[str]:
    """Feste Antwort, um die Pipeline ohne API-Schlüssel zu testen."""
    reply = (
        "[[eu é → eu sou · Bei „eu“ heißt es „sou“.]] "
        "Que legal! Eu também adoro praia. "
        "E você, vai pra praia no fim de semana?"
    )
    for word in re.findall(r"\S+\s*", reply):
        await asyncio.sleep(0.03)
        yield word


llm_stream = fake_stream if FAKE_LLM else claude_stream


# ---------------------------------------------------------------------------
# Sprachausgabe (ElevenLabs)
# ---------------------------------------------------------------------------

_http: httpx.AsyncClient | None = None


def _http_client() -> httpx.AsyncClient:
    global _http
    if _http is None:
        _http = httpx.AsyncClient(timeout=httpx.Timeout(15.0, connect=5.0))
    return _http


async def tts_elevenlabs(text: str, accent: str) -> bytes:
    voice = ELEVEN_VOICES.get(accent, ELEVEN_VOICE_DEFAULT)
    r = await _http_client().post(
        f"https://api.elevenlabs.io/v1/text-to-speech/{voice}",
        params={"output_format": "mp3_44100_64"},
        headers={"xi-api-key": ELEVEN_KEY},
        json={
            "text": text,
            "model_id": ELEVEN_MODEL,
            "voice_settings": {"stability": 0.45, "similarity_boost": 0.8},
        },
    )
    r.raise_for_status()
    return r.content


# ---------------------------------------------------------------------------
# Gesprächssitzung pro WebSocket
# ---------------------------------------------------------------------------


def _trim(history: list[dict]) -> list[dict]:
    msgs = history[-MAX_HISTORY:]
    while msgs and msgs[0]["role"] != "user":
        msgs = msgs[1:]
    return msgs


class Session:
    def __init__(self, ws: WebSocket) -> None:
        self.ws = ws
        self.history: list[dict] = []
        self.task: asyncio.Task | None = None
        self.turn = 0
        self._send_lock = asyncio.Lock()

    async def send(self, obj: dict) -> None:
        async with self._send_lock:
            await self.ws.send_json(obj)

    async def cancel(self) -> None:
        if self.task and not self.task.done():
            self.task.cancel()
            try:
                await self.task
            except BaseException:
                pass
        self.task = None

    def start_turn(self, user_text: str, settings: Settings) -> None:
        self.turn += 1
        self.task = asyncio.create_task(self._run(self.turn, user_text, settings))

    async def _run(self, turn: int, user_text: str, settings: Settings) -> None:
        try:
            await self._respond(turn, user_text, settings)
        except asyncio.CancelledError:
            raise
        except Refused:
            await self.send({"type": "error", "turn": turn,
                             "message": "Darauf kann ich leider nicht antworten. Versuch es anders."})
        except anthropic.AuthenticationError:
            await self.send({"type": "error", "turn": turn,
                             "message": "ANTHROPIC_API_KEY fehlt oder ist ungültig."})
        except anthropic.RateLimitError:
            await self.send({"type": "error", "turn": turn,
                             "message": "Zu viele Anfragen – bitte kurz warten."})
        except anthropic.APIStatusError as e:
            await self.send({"type": "error", "turn": turn,
                             "message": f"Claude-Fehler ({e.status_code})."})
        except anthropic.APIConnectionError:
            await self.send({"type": "error", "turn": turn,
                             "message": "Keine Verbindung zu Claude."})
        except Exception as e:  # noqa: BLE001 – Prototyp: Fehler sichtbar machen
            await self.send({"type": "error", "turn": turn, "message": f"Fehler: {e}"})

    async def _respond(self, turn: int, user_text: str, settings: Settings) -> None:
        self.history.append({"role": "user", "content": user_text})
        system = build_system(settings)
        chunker = SpeechChunker()
        raw: list[str] = []
        idx = 0
        completed = False
        audio_q: asyncio.Queue = asyncio.Queue()
        sender = asyncio.create_task(self._audio_sender(turn, audio_q)) if ELEVEN_KEY else None

        async def emit(kind: str, text: str) -> None:
            nonlocal idx
            if kind == "note":
                await self.send({"type": "correction", "turn": turn, "text": text})
                return
            await self.send({"type": "sentence", "turn": turn, "idx": idx, "text": text})
            if sender:
                # Sprachausgabe sofort starten, während Claude weiterschreibt
                task = asyncio.create_task(tts_elevenlabs(text, settings.accent))
                await audio_q.put((idx, text, task))
            idx += 1

        try:
            async for delta in llm_stream(system, _trim(self.history)):
                raw.append(delta)
                for kind, text in chunker.feed(delta):
                    await emit(kind, text)
            for kind, text in chunker.flush():
                await emit(kind, text)
            if sender:
                await audio_q.put(None)
                await sender
            completed = True
            await self.send({"type": "done", "turn": turn})
        finally:
            if sender and not sender.done():
                sender.cancel()
            reply = "".join(raw).strip()
            if reply:
                self.history.append(
                    {"role": "assistant", "content": reply if completed else reply + " …"}
                )

    async def _audio_sender(self, turn: int, q: asyncio.Queue) -> None:
        pending: list[asyncio.Task] = []
        try:
            while (item := await q.get()) is not None:
                idx, text, task = item
                pending.append(task)
                try:
                    audio = await task
                except Exception:
                    await self.send({"type": "tts_error", "turn": turn, "idx": idx})
                    continue
                await self.send({"type": "audio", "turn": turn, "idx": idx,
                                 "data": base64.b64encode(audio).decode("ascii")})
        finally:
            for t in pending:
                t.cancel()


# ---------------------------------------------------------------------------
# Web-App
# ---------------------------------------------------------------------------

app = FastAPI(title="Fala Já")
print(f"[Fala Já] Modell: {'Testmodus' if FAKE_LLM else MODEL} · "
      f"Stimme: {'ElevenLabs' if ELEVEN_KEY else 'Browser (kein ELEVENLABS_API_KEY)'}")
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


def _auth_token() -> str:
    return hashlib.sha256(f"fala-ja:{APP_PASSWORD}".encode()).hexdigest()


def _is_authed(cookies: dict) -> bool:
    if not APP_PASSWORD:
        return True
    return hmac.compare_digest(cookies.get(AUTH_COOKIE, ""), _auth_token())


def _login_page(error: str = "") -> HTMLResponse:
    err = f'<p class="login-error">{html.escape(error)}</p>' if error else ""
    return HTMLResponse(f"""<!doctype html>
<html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Fala Já</title><link rel="stylesheet" href="/static/style.css"></head>
<body><main class="login">
  <h1>💬 Fala Já</h1>
  <form method="post" action="/login">
    <label for="pw">Passwort</label>
    <input id="pw" name="password" type="password" autocomplete="current-password" autofocus required>
    <button type="submit" class="ghost">Los geht's</button>
  </form>{err}
</main></body></html>""")


@app.get("/")
async def index(request: Request):
    if not _is_authed(request.cookies):
        return _login_page()
    return FileResponse(STATIC_DIR / "index.html")


@app.post("/login")
async def login(request: Request):
    form = parse_qs((await request.body()).decode("utf-8", "replace"))
    given = form.get("password", [""])[0]
    if not APP_PASSWORD or not hmac.compare_digest(given.encode(), APP_PASSWORD.encode()):
        return _login_page("Falsches Passwort.")
    resp = RedirectResponse("/", status_code=303)
    resp.set_cookie(AUTH_COOKIE, _auth_token(), max_age=90 * 24 * 3600, httponly=True,
                    samesite="lax", secure=request.url.scheme == "https"
                    or request.headers.get("x-forwarded-proto") == "https")
    return resp


@app.get("/api/config")
async def config() -> dict:
    return {
        "tts": "elevenlabs" if ELEVEN_KEY else "browser",
        "model": "fake" if FAKE_LLM else MODEL,
    }


@app.websocket("/ws")
async def ws_endpoint(ws: WebSocket) -> None:
    await ws.accept()
    if not _is_authed(ws.cookies):
        # erst annehmen, dann schließen: so sieht der Browser den Code 1008
        await ws.close(code=1008)
        return
    session = Session(ws)
    try:
        while True:
            msg = await ws.receive_json()
            kind = msg.get("type")
            if kind in ("user_text", "start"):
                await session.cancel()
                text = START_PROMPT if kind == "start" else str(msg.get("text", "")).strip()[:2000]
                if text:
                    session.start_turn(text, Settings.from_msg(msg))
            elif kind == "interrupt":
                await session.cancel()
                await session.send({"type": "stopped"})
            elif kind == "reset":
                await session.cancel()
                session.history.clear()
    except WebSocketDisconnect:
        pass
    finally:
        await session.cancel()
