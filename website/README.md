# Terminanfrage-Formular für marcel-maximiert.net

Ein einfaches Formular für das kostenlose Erstgespräch, das **Calendly ersetzt**:
Besucher wählen Wunschtag + Zeitfenster, du bekommst eine Mail und bestätigst den Termin.
Danach landet der Besucher auf einer **Danke-Seite**, und genau dort wird die
Conversion gezählt.

```
Formular absenden → Server prüft & schickt 2 Mails → Weiterleitung /danke.html → generate_lead
                     ├─ an dich: alle Angaben + Quelle (UTM / gclid)
                     └─ an Interessenten: „Anfrage erhalten, Bestätigung folgt in 24 h“
```

```
website/
├── public/                  → auf deinen Webspace kopieren
│   ├── kontakt.html         Vorlage Kontaktseite (Formular + Konfiguration)
│   ├── danke.html           Danke-Seite (hier feuert die Conversion)
│   └── widgets/
│       ├── mm-tracking.js   AUF ALLEN SEITEN: merkt UTM/gclid, feuert Lead auf der Danke-Seite
│       ├── mm-contact.js    nur auf der Kontaktseite
│       └── mm-widgets.css   Design (Farben oben in :root anpassen)
└── backend/                 → kleiner Python-Server für den Mailversand
    ├── app.py
    ├── .env.example
    └── Dockerfile
```

## Formular

Felder: **Name, E-Mail, Telefon, Wunschtermin (Datum), Uhrzeit (Zeitfenster)**, Nachricht (optional),
Datenschutz-Häkchen. Dazu ein unsichtbares Honeypot-Feld gegen Spam.

- Zeitfenster: die `<option>`s in `kontakt.html` anpassen.
- Wählbare Wochentage und Vorlauf: `bookingDays` und `maxDays` in `MM_CONFIG`.
  Termine sind frühestens ab morgen wählbar.

## 1. Backend starten

Die Website ist statisch. Für den Mailversand braucht es einen kleinen Server,
weil die SMTP-Zugangsdaten nicht in den Browser dürfen. Am einfachsten auf **Google Cloud Run**,
**Render** oder **Railway** mit dem mitgelieferten `Dockerfile`.

1. Umgebungsvariablen aus `.env.example` beim Hoster eintragen. Die SMTP-Daten bekommst du
   von deinem Mail-Anbieter (Port 465 = SSL, 587 = STARTTLS).
2. Deployen und eine Subdomain wie `api.marcel-maximiert.net` darauf zeigen lassen.
3. Test: `https://api.marcel-maximiert.net/health` muss `{"ok":true}` liefern.

Lokal:

```bash
cd website/backend
pip install -r requirements.txt
export $(grep -v '^#' .env.example | xargs)   # vorher Werte eintragen
export ALLOWED_ORIGINS=http://localhost:8080
uvicorn app:app --reload --port 8000
```

## 2. In die Website einbauen

1. `public/widgets/` auf den Webspace kopieren (→ `/widgets/…`).
2. **Alle Seiten:** `<script src="/widgets/mm-tracking.js" defer></script>` in den `<head>`.
   Nur so wird ein Anzeigenklick auf der Startseite auch einer späteren Anfrage zugeordnet.
3. **Kontaktseite:** `MM_CONFIG`-Block, CSS, beide Scripts und das `<form>` aus `kontakt.html` übernehmen
   und `apiBase` anpassen.
4. **Danke-Seite:** `danke.html` übernehmen. Wichtig ist `<body data-mm-danke>`.
5. Calendly-Links und Embeds auf der Website durch Links zur Kontaktseite ersetzen.

## 3. Conversion Tracking

| Event | Wann | Parameter |
|---|---|---|
| `form_start` | erste Eingabe im Formular | `form_id` |
| `form_error` | Senden fehlgeschlagen | `form_id` |
| `generate_lead` | **auf der Danke-Seite, nur nach echter Anfrage** | `form_id`, `lead_type`, `transaction_id`, `value`, `currency`, `user_data` |

`generate_lead` feuert **genau einmal**: nicht bei direktem Aufruf von /danke.html, nicht beim
Neuladen und nicht bei Spam (Honeypot). `transaction_id` ist pro Anfrage eindeutig, damit Google Ads
nichts doppelt zählt.

> Nimm **nicht** den Seitenaufruf von /danke.html als Conversion-Trigger, sondern das Event
> `generate_lead`. Nur das ist gegen Neuladen und Direktaufrufe abgesichert.

### Variante A – Google Tag Manager (empfohlen)

`useGtag: false` lassen. Im GTM:

1. **Variablen** (Typ *Datenschichtvariable*): `transaction_id`, `value`, `currency`, `form_id`,
   `user_data.email`, `user_data.phone_number`.
2. **Trigger** (Typ *Benutzerdefiniertes Ereignis*): Ereignisname `generate_lead`.
3. **GA4:** Tag *GA4-Ereignis* `generate_lead` mit den Parametern oben. In GA4 als
   *Schlüsselereignis* markieren.
4. **Google Ads:** Tag *Google Ads-Conversion-Tracking* mit Conversion-ID/-Label, Wert `{{value}}`,
   Transaktions-ID `{{transaction_id}}`. *Vom Nutzer bereitgestellte Daten einbeziehen* aktivieren
   → Manuell → E-Mail `{{user_data.email}}`, Telefon `{{user_data.phone_number}}`
   (Enhanced Conversions).
5. Einen **Conversion-Linker**-Tag auf *All Pages*, falls noch nicht vorhanden.

### Variante B – gtag.js direkt (ohne GTM)

Auf `kontakt.html` **und** `danke.html` `useGtag: true` setzen und in `adsConversions.generate_lead`
den `send_to`-Wert (`AW-…/…`) deiner Conversion-Aktion eintragen. In Google Ads Enhanced Conversions
für „Google-Tag“ aktivieren.

> ⚠️ Nicht beides gleichzeitig: Wenn GA4/Ads im GTM laufen, muss `useGtag: false` bleiben,
> sonst wird jede Conversion doppelt gezählt.

### Testen

GTM-Vorschau öffnen → Formular abschicken → auf der Danke-Seite muss `generate_lead` mit
`transaction_id` und `user_data` erscheinen. Danke-Seite neu laden → kein zweites `generate_lead`.

## Sonstiges

- **Datenschutz:** In der Datenschutzerklärung ergänzen: Terminanfrage-Formular (Mailversand über
  deinen Server/SMTP-Anbieter) und die Speicherung der Kampagnen-Parameter im `localStorage` (90 Tage).
  Calendly kann raus. Tracking-Tags wie gewohnt nur nach Consent (Consent Mode) feuern lassen.
- **Spam-Schutz:** Honeypot-Feld + Rate-Limit (5 Anfragen / 10 Min. pro IP).
- Die Bestätigungsmail an Interessenten lässt sich mit `SEND_CONFIRMATION=false` abschalten.
