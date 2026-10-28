# Chatbot & Kontaktformular für marcel-maximiert.net

Zwei Bausteine für die eigene HTML-Seite, beide mit sauberem Conversion Tracking
(Google Tag Manager, GA4, Google Ads inkl. Enhanced Conversions):

| Baustein | Was er macht | Conversion-Event |
|---|---|---|
| **Kontaktformular** | Name, E-Mail, Telefon (optional), Nachricht → Mail an dich | `generate_lead` |
| **Chatbot** | Beantwortet Fragen mit KI (Gemini) aus `knowledge.md`, bietet Calendly-Termin an | `book_appointment` |

```
website/
├── public/                 → auf deinen Webspace kopieren
│   ├── widgets/
│   │   ├── mm-tracking.js  UTM/gclid speichern, Events an dataLayer/gtag, Calendly-Buchungen erkennen
│   │   ├── mm-contact.js   Kontaktformular
│   │   ├── mm-chat.js      Chat-Widget
│   │   └── mm-widgets.css  Design (Farben oben in :root anpassen)
│   └── beispiel.html       Vorlage zum Kopieren
└── backend/                → kleiner Python-Server (Chat + Mailversand)
    ├── app.py
    ├── knowledge.md        ← HIER deine Angebote, Preise, FAQ eintragen
    ├── .env.example
    └── Dockerfile
```

## 1. Wissen für den Bot pflegen

`backend/knowledge.md` öffnen und alle `[Platzhalter]` ersetzen. Der Bot antwortet
**nur** mit diesen Infos und verweist sonst auf ein Erstgespräch.

## 2. Backend starten

Die Website selbst ist statisch – für die KI (API-Key muss geheim bleiben) und den
Mailversand braucht es einen kleinen Server. Am einfachsten z. B. auf **Google Cloud Run**,
**Render** oder **Railway** mit dem mitgelieferten `Dockerfile`.

1. Gemini-API-Key holen: https://aistudio.google.com/apikey
2. Umgebungsvariablen aus `.env.example` beim Hoster eintragen (SMTP-Daten bekommst du
   von deinem Mail-Anbieter; Port 465 = SSL, 587 = STARTTLS).
3. Deployen und idealerweise eine Subdomain wie `api.marcel-maximiert.net` darauf zeigen lassen.
4. Test: `https://api.marcel-maximiert.net/health` muss `{"ok":true}` liefern.

Lokal:

```bash
cd website/backend
pip install -r requirements.txt
export $(grep -v '^#' .env.example | xargs)   # vorher Werte eintragen
export ALLOWED_ORIGINS=http://localhost:8080
uvicorn app:app --reload --port 8000
```

## 3. In die Website einbauen

1. Den Ordner `public/widgets/` auf den Webspace kopieren (→ `/widgets/…`).
2. Aus `public/beispiel.html` übernehmen:
   - den `<script>window.MM_CONFIG = …</script>`-Block und die 4 Zeilen mit
     `mm-widgets.css` / `mm-*.js` in den `<head>` **jeder Seite**, auf der der Chat erscheinen soll,
   - das `<form class="mm-form" …>` auf der Kontaktseite.
3. In `MM_CONFIG` mindestens `apiBase` und `calendlyUrl` anpassen.

Nur das Formular ohne Chat? Einfach `mm-chat.js` weglassen (und umgekehrt).
`mm-tracking.js` wird immer gebraucht und muss zuerst geladen werden.

## 4. Conversion Tracking einrichten

Alle Events landen im `dataLayer`:

| Event | Wann | Parameter |
|---|---|---|
| `form_start` | erste Eingabe im Formular | `form_id` |
| `generate_lead` | **erst wenn der Server die Anfrage angenommen hat** | `form_id`, `transaction_id`, `value`, `currency`, `user_data` |
| `form_error` | Senden fehlgeschlagen | `form_id` |
| `chat_open` | Chat geöffnet (1× pro Sitzung) | `chat_location` |
| `chat_engaged` | erste Nachricht an den Bot (1× pro Sitzung) | `chat_location` |
| `booking_click` | Calendly geöffnet | `booking_source` |
| `book_appointment` | **Termin tatsächlich in Calendly gebucht** | `transaction_id`, `value`, `currency` |

`transaction_id` ist pro Lead/Buchung eindeutig → Google Ads zählt keine Doppelungen,
auch wenn jemand die Danke-Ansicht neu lädt.

### Variante A – Google Tag Manager (empfohlen)

`useGtag: false` lassen. Im GTM:

1. **Variablen** (Typ *Datenschichtvariable*): `transaction_id`, `value`, `currency`, `form_id`,
   `user_data.email`, `user_data.phone_number`.
2. **Trigger** (Typ *Benutzerdefiniertes Ereignis*): `generate_lead`, `book_appointment`
   (optional auch `chat_engaged`, `booking_click`, `form_start`).
3. **GA4**: je Trigger ein Tag *GA4-Ereignis* mit gleichem Ereignisnamen + Parametern.
   In GA4 `generate_lead` und `book_appointment` als *Schlüsselereignis* markieren.
4. **Google Ads**: je Conversion-Aktion ein Tag *Google Ads-Conversion-Tracking* mit
   Conversion-ID/-Label, Wert `{{value}}`, Transaktions-ID `{{transaction_id}}`.
   *Vom Nutzer bereitgestellte Daten einbeziehen* aktivieren → neue Variable
   *Vom Nutzer bereitgestellte Daten* → Manuell → E-Mail `{{user_data.email}}`,
   Telefon `{{user_data.phone_number}}` (Enhanced Conversions).
5. Zusätzlich einen **Conversion-Linker**-Tag auf *All Pages* (falls noch nicht vorhanden).

### Variante B – gtag.js direkt (ohne GTM)

`useGtag: true` setzen und in `adsConversions` die `send_to`-Werte (`AW-…/…`) deiner
Conversion-Aktionen eintragen. In Google Ads unter der Conversion-Aktion Enhanced
Conversions für „Google-Tag“ aktivieren. GA4-Events laufen über dein bestehendes `gtag('config', 'G-…')`.

> ⚠️ Nicht beides gleichzeitig: Wenn GA4/Ads im GTM konfiguriert sind, muss `useGtag: false`
> bleiben, sonst wird jede Conversion doppelt gezählt.

### Testen

GTM-Vorschau öffnen → Formular abschicken → im Tag Assistant muss `generate_lead` mit
`transaction_id` und `user_data` erscheinen. Für Calendly eine Testbuchung machen →
`book_appointment`.

## Sonstiges

- **Datenschutz**: In der Datenschutzerklärung ergänzen: Kontaktformular (Mailversand über
  deinen Server/SMTP-Anbieter), Chatbot (Nachrichten werden zur Beantwortung an Google
  Gemini übermittelt), Calendly, Speicherung der Kampagnen-Parameter im `localStorage`
  (90 Tage). Tracking-Tags wie gewohnt nur nach Consent (Consent Mode) feuern lassen.
- **Spam-Schutz**: Honeypot-Feld + Rate-Limit (5 Anfragen / 10 Min. pro IP, Chat 15 / Min.).
- **Gemini-Modell** über `GEMINI_MODEL` änderbar.
- Statt Danke-Text im Formular lieber eine eigene Danke-Seite? `thankYouUrl: '/danke'` setzen –
  das Tracking wird vor der Weiterleitung abgeschlossen.
