import importlib
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def load_server(fake: bool):
    os.environ["FALA_FAKE_LLM"] = "1" if fake else "0"
    os.environ["ELEVENLABS_API_KEY"] = ""  # .env darf Tests nicht beeinflussen
    import server
    return importlib.reload(server)


def chunks(text, step=3):
    server = load_server(fake=True)
    c = server.SpeechChunker()
    out = []
    for i in range(0, len(text), step):
        out += c.feed(text[i : i + step])
    return out + c.flush()


def test_splits_sentences_and_extracts_note():
    out = chunks("[[eu é → eu sou · Bei eu: sou.]] Que legal! Eu também. E você?")
    assert out == [
        ("note", "eu é → eu sou · Bei eu: sou."),
        ("speech", "Que legal!"),
        ("speech", "Eu também."),
        ("speech", "E você?"),
    ]


def test_strips_markdown_and_handles_newlines():
    out = chunks("**Oi!**\nTudo bem")
    assert out == [("speech", "Oi!"), ("speech", "Tudo bem")]


def test_early_split_of_long_sentence():
    long = "Eu fui pra praia com os meus amigos, " * 6
    out = chunks(long, step=10)
    assert len(out) >= 2
    assert all(kind == "speech" and len(t) <= 141 for kind, t in out)


def test_unclosed_note_is_flushed_as_note():
    assert chunks("[[nur Notiz") == [("note", "nur Notiz")]


def test_websocket_turn_in_fake_mode():
    from fastapi.testclient import TestClient

    server = load_server(fake=True)
    with TestClient(server.app) as client:
        assert client.get("/api/config").json() == {"tts": "browser", "model": "fake"}
        with client.websocket_connect("/ws") as ws:
            ws.send_json({"type": "user_text", "text": "eu é feliz", "level": "A2"})
            seen = []
            while True:
                msg = ws.receive_json()
                seen.append(msg)
                if msg["type"] in ("done", "error"):
                    break
    types = [m["type"] for m in seen]
    assert types[0] == "correction"
    assert types.count("sentence") == 3
    assert types[-1] == "done"
    assert all(m["turn"] == 1 for m in seen)


def test_audio_sent_in_order_with_tts(monkeypatch):
    import asyncio
    from fastapi.testclient import TestClient

    server = load_server(fake=True)
    monkeypatch.setattr(server, "ELEVEN_KEY", "test")

    async def fake_tts(text, accent):
        # erster Satz langsamer: Reihenfolge muss trotzdem stimmen
        await asyncio.sleep(0.2 if text.startswith("Que") else 0.01)
        return text.encode()

    monkeypatch.setattr(server, "tts_elevenlabs", fake_tts)
    with TestClient(server.app) as client, client.websocket_connect("/ws") as ws:
        ws.send_json({"type": "user_text", "text": "oi"})
        audio = []
        while (msg := ws.receive_json())["type"] != "done":
            if msg["type"] == "audio":
                audio.append(msg["idx"])
    assert audio == [0, 1, 2]


def test_interrupt_stops_turn():
    from fastapi.testclient import TestClient

    server = load_server(fake=True)
    with TestClient(server.app) as client, client.websocket_connect("/ws") as ws:
        ws.send_json({"type": "user_text", "text": "oi"})
        ws.receive_json()
        ws.send_json({"type": "interrupt"})
        while (msg := ws.receive_json())["type"] != "stopped":
            assert msg["type"] != "done"
