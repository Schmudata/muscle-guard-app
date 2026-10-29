"""Backend für das Terminanfrage-Formular von marcel-maximiert.net.

Endpunkte:
  POST /api/contact  – Anfrage per E-Mail an dich + Bestätigung an den Interessenten
  GET  /health       – Health-Check fürs Hosting

Start lokal:  uvicorn app:app --reload --port 8000
"""

import datetime as dt
import logging
import os
import smtplib
import time
import uuid
from collections import defaultdict, deque
from email.message import EmailMessage

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr, Field

log = logging.getLogger("mm")
logging.basicConfig(level=logging.INFO)

ALLOWED_ORIGINS = [o.strip() for o in os.getenv(
    "ALLOWED_ORIGINS", "https://marcel-maximiert.net,https://www.marcel-maximiert.net"
).split(",") if o.strip()]
MAX_DAYS = int(os.getenv("MAX_DAYS", "90"))
SEND_CONFIRMATION = os.getenv("SEND_CONFIRMATION", "true").lower() == "true"

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
        raise HTTPException(429, "Zu viele Anfragen. Bitte versuche es später noch einmal.")
    q.append(now)


@app.get("/health")
def health():
    return {"ok": True}


class ContactRequest(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    email: EmailStr
    phone: str = Field(min_length=5, max_length=40, pattern=r"^[+\d\s()/.-]+$")
    date: dt.date
    slot: str = Field(min_length=1, max_length=50)
    message: str = Field("", max_length=3000)
    consent: bool
    website: str = ""  # Honeypot – muss leer bleiben
    page: str = Field("", max_length=500)
    attribution: dict[str, str | int | None] = Field(default_factory=dict)


def owner_mail(lead_id: str, c: ContactRequest, termin: str) -> EmailMessage:
    attribution = "\n".join(
        f"  {k}: {str(v)[:300]}" for k, v in list(c.attribution.items())[:15] if k != "ts" and v
    ) or "  (direkt / unbekannt)"
    msg = EmailMessage()
    msg["Subject"] = f"Terminanfrage: {c.name} – {termin}"
    msg["From"] = os.environ["MAIL_FROM"]
    msg["To"] = os.environ["CONTACT_TO"]
    msg["Reply-To"] = c.email
    msg.set_content(
        f"Wunschtermin: {termin}\n\n"
        f"Name:    {c.name}\n"
        f"E-Mail:  {c.email}\n"
        f"Telefon: {c.phone}\n\n"
        f"Nachricht:\n{c.message or '-'}\n\n"
        f"---\nSeite: {c.page or '-'}\nLead-ID: {lead_id}\nQuelle:\n{attribution}\n"
    )
    return msg


def confirmation_mail(c: ContactRequest, termin: str) -> EmailMessage:
    msg = EmailMessage()
    msg["Subject"] = "Deine Terminanfrage bei Marcel Maximiert"
    msg["From"] = os.environ["MAIL_FROM"]
    msg["To"] = c.email
    msg["Reply-To"] = os.environ["CONTACT_TO"]
    msg.set_content(
        f"Hallo {c.name},\n\n"
        f"danke für deine Anfrage! Dein Wunschtermin: {termin}.\n\n"
        "Ich bestätige dir den Termin innerhalb von 24 Stunden – oder schlage dir eine "
        "Alternative vor, falls es nicht passt. Du kannst einfach auf diese E-Mail antworten.\n\n"
        "Viele Grüße\nMarcel\n\nmarcel-maximiert.net\n"
    )
    return msg


def send(messages: list[EmailMessage]) -> None:
    host = os.environ["SMTP_HOST"]
    port = int(os.getenv("SMTP_PORT", "465"))
    user, password = os.environ["SMTP_USER"], os.environ["SMTP_PASSWORD"]
    if port == 465:
        server = smtplib.SMTP_SSL(host, port, timeout=15)
    else:
        server = smtplib.SMTP(host, port, timeout=15)
        server.starttls()
    with server:
        server.login(user, password)
        for i, msg in enumerate(messages):
            try:
                server.send_message(msg)
            except Exception:
                if i == 0:
                    raise  # Mail an dich ist Pflicht
                log.exception("Bestätigungsmail fehlgeschlagen")


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
    today = dt.date.today()
    if not today <= c.date <= today + dt.timedelta(days=MAX_DAYS):
        raise HTTPException(400, "Bitte wähle einen Termin in der Zukunft.")

    weekdays = ["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"]
    termin = f"{weekdays[c.date.weekday()]}, {c.date:%d.%m.%Y}, {c.slot}"
    messages = [owner_mail(lead_id, c, termin)]
    if SEND_CONFIRMATION:
        messages.append(confirmation_mail(c, termin))
    try:
        send(messages)
    except Exception:
        log.exception("Mailversand fehlgeschlagen")
        raise HTTPException(502, "Senden hat nicht geklappt. Bitte schreib mir direkt per E-Mail.")
    return {"ok": True, "lead_id": lead_id}
