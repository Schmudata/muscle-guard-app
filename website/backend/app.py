"""Backend für Chatbot & Kontaktformular von marcel-maximiert.net.

Endpunkte:
  POST /api/chat     – KI-Antwort (Gemini) auf Basis von knowledge.md
  POST /api/contact  – Kontaktanfrage per E-Mail weiterleiten
  GET  /health       – Health-Check fürs Hosting

Start lokal:  uvicorn app:app --reload --port 8000
"""

import logging
import os
import smtplib
import time
import uuid
from collections import defaultdict, deque
from email.message import EmailMessage
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr, Field

log = logging.getLogger("mm")
logging.basicConfig(level=logging.INFO)

GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
ALLOWED_ORIGINS = [o.strip() for o in os.getenv(
    "ALLOWED_ORIGINS", "https://marcel-maximiert.net,https://www.marcel-maximiert.net"
).split(",") if o.strip()]
BOOKING_TOKEN = "[TERMIN]"

KNOWLEDGE = (Path(__file__).parent / "knowledge.md").read_text(encoding="utf-8")
SYSTEM_PROMPT = f"""Du bist der freundliche Website-Assistent von Marcel (marcel-maximiert.net).

Regeln:
- Antworte auf Deutsch, per Du, kurz (max. 3–4 Sätze), ohne Markdown-Formatierung.
- Nutze ausschließlich die Informationen aus dem WISSEN unten. Erfinde keine Preise,
  Termine, Leistungen oder Versprechen. Wenn du etwas nicht weißt, sag das ehrlich und
  biete ein kostenloses Erstgespräch oder das Kontaktformular an.
- Ziel: Fragen klären und Interessenten zu einem Termin führen – ohne aufdringlich zu sein.
- Wenn der Besucher einen Termin möchte, nach Verfügbarkeit fragt oder ein Gespräch
  sinnvoll ist, beende deine Antwort mit {BOOKING_TOKEN} – dann wird ein Buchungsbutton
  angezeigt. Nenne keine konkreten freien Zeiten, die sieht der Besucher im Kalender.
- Frage nicht nach Gesundheits-, Zahlungs- oder anderen sensiblen Daten.
- Bleib beim Thema der Website; lehne fachfremde Aufgaben höflich ab.

WISSEN:
{KNOWLEDGE}
"""

app = FastAPI(title="marcel-maximiert API", docs_url=None, redoc_url=None)
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_methods=["POST"],
    allow_headers=["Content-Type"],
)

# --- Einfaches Rate-Limit pro IP (In-Memory, reicht für eine Instanz) ---------------
_hits: dict[str, deque] = defaultdict(deque)


def rate_limit(request: Request, bucket: str, limit: int, window: int) -> None:
    fwd = request.headers.get("x-forwarded-for", "")
    ip = fwd.split(",")[0].strip() or (request.client.host if request.client else "?")
    q = _hits[f"{bucket}:{ip}"]
    now = time.monotonic()
    while q and now - q[0] > window:
        q.popleft()
    if len(q) >= limit:
        raise HTTPException(429, "Zu viele Anfragen. Bitte versuche es gleich noch einmal.")
    q.append(now)


@app.get("/health")
def health():
    return {"ok": True}


# --- Chat -------------------------------------------------------------------------
class ChatMessage(BaseModel):
    role: Literal["user", "model"]
    text: str = Field(min_length=1, max_length=2000)


class ChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(min_length=1, max_length=20)
    page: str = Field("", max_length=300)


_client = None


def gemini():
    global _client
    if _client is None:
        from google import genai
        _client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
    return _client


@app.post("/api/chat")
def chat(req: ChatRequest, request: Request):
    rate_limit(request, "chat", limit=15, window=60)
    if req.messages[-1].role != "user":
        raise HTTPException(400, "Letzte Nachricht muss vom Nutzer sein.")

    from google.genai import types

    contents = [types.Content(role=m.role, parts=[types.Part(text=m.text)]) for m in req.messages]
    system = SYSTEM_PROMPT + (f"\nDer Besucher ist gerade auf der Seite: {req.page}" if req.page else "")
    try:
        resp = gemini().models.generate_content(
            model=GEMINI_MODEL,
            contents=contents,
            config=types.GenerateContentConfig(
                system_instruction=system,
                temperature=0.3,
                max_output_tokens=2048,
            ),
        )
        text = (resp.text or "").strip()
    except Exception:
        log.exception("Gemini-Aufruf fehlgeschlagen")
        raise HTTPException(502, "KI gerade nicht erreichbar.")

    show_booking = BOOKING_TOKEN in text
    text = text.replace(BOOKING_TOKEN, "").strip()
    if not text:
        text = "Das kann ich dir am besten in einem kurzen Gespräch beantworten."
        show_booking = True
    return {"reply": text, "show_booking": show_booking}


# --- Kontaktformular --------------------------------------------------------------
class ContactRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr
    phone: str = Field("", max_length=40)
    message: str = Field(min_length=1, max_length=5000)
    consent: bool
    website: str = ""  # Honeypot – muss leer bleiben
    page: str = Field("", max_length=500)
    attribution: dict[str, str | int | None] = Field(default_factory=dict)


def send_mail(lead_id: str, c: ContactRequest) -> None:
    attribution = "\n".join(
        f"  {k}: {str(v)[:300]}" for k, v in list(c.attribution.items())[:15] if k != "ts" and v
    ) or "  (direkt / unbekannt)"
    msg = EmailMessage()
    msg["Subject"] = f"Neue Anfrage von {c.name} (marcel-maximiert.net)"
    msg["From"] = os.environ["MAIL_FROM"]
    msg["To"] = os.environ["CONTACT_TO"]
    msg["Reply-To"] = c.email
    msg.set_content(
        f"Name:    {c.name}\n"
        f"E-Mail:  {c.email}\n"
        f"Telefon: {c.phone or '-'}\n\n"
        f"Nachricht:\n{c.message}\n\n"
        f"---\nSeite: {c.page or '-'}\nLead-ID: {lead_id}\nQuelle:\n{attribution}\n"
    )
    host = os.environ["SMTP_HOST"]
    port = int(os.getenv("SMTP_PORT", "465"))
    user, password = os.environ["SMTP_USER"], os.environ["SMTP_PASSWORD"]
    if port == 465:
        with smtplib.SMTP_SSL(host, port, timeout=15) as s:
            s.login(user, password)
            s.send_message(msg)
    else:
        with smtplib.SMTP(host, port, timeout=15) as s:
            s.starttls()
            s.login(user, password)
            s.send_message(msg)


@app.post("/api/contact")
def contact(c: ContactRequest, request: Request):
    rate_limit(request, "contact", limit=5, window=600)
    lead_id = uuid.uuid4().hex
    if c.website:
        # Bot erkannt: so tun, als ob alles geklappt hat, aber nichts senden.
        log.info("Honeypot ausgelöst")
        return {"ok": True, "lead_id": lead_id}
    if not c.consent:
        raise HTTPException(400, "Bitte stimme der Datenschutzerklärung zu.")
    try:
        send_mail(lead_id, c)
    except Exception:
        log.exception("Mailversand fehlgeschlagen")
        raise HTTPException(502, "Senden hat nicht geklappt. Bitte schreib mir direkt per E-Mail.")
    return {"ok": True, "lead_id": lead_id}
