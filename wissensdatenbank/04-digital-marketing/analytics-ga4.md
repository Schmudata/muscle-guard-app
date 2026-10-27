---
titel: Analytics mit Google Analytics 4
kategorie: digital-marketing
tags: [analytics, tracking]
zielgruppe: Fortgeschrittene
tiefe: basis
zuletzt_geprueft: 2026-10
---

# Analytics mit GA4

## Kurzfassung
GA4 ist ereignisbasiert. Für KMU reicht ein **schlankes Setup**: wichtige Ereignisse als
**Schlüsselereignisse** (Key Events), saubere Kanalzuordnung per UTM, Verknüpfung mit
Google Ads und Search Console, ein kompakter Bericht.

## Mindest-Setup
- [ ] Property + Web-Datenstream, erweiterte Messung aktiv.
- [ ] Consent Mode v2 mit Cookie-Banner.
- [ ] **Interne Zugriffe** (Büro-IP) ausschließen, Referral-Ausschlüsse (Zahlungsanbieter).
- [ ] Datenaufbewahrung auf 14 Monate stellen.
- [ ] Ereignisse: `generate_lead` (Formular), `click_to_call`/Telefonklick, `click_whatsapp`,
      `book_appointment`, `purchase`.
- [ ] Die wichtigsten als **Schlüsselereignisse** markieren.
- [ ] Verknüpfungen: Google Ads, Search Console, ggf. Merchant Center.
- [ ] Google Signals bewusst entscheiden (Datenschutz).

## UTM-Konvention
```
utm_source   = google | facebook | instagram | newsletter | gbp
utm_medium   = cpc | paid_social | email | organic | qr
utm_campaign = [kampagnenname, kleingeschrieben-mit-bindestrichen]
```
Beispiel GBP: `?utm_source=google&utm_medium=organic&utm_campaign=gbp`
Beispiel Flyer: `?utm_source=flyer&utm_medium=qr&utm_campaign=fruehjahr-2026`

## Wichtige Berichte
- **Akquisition → Traffic-Akquisition:** Kanäle nach Sitzungen und Schlüsselereignissen.
- **Seiten und Bildschirme:** Welche Leistungsseiten bringen Anfragen?
- **Explorationen:** Trichter (Landingpage → Formular → Danke).
- **Looker Studio** für einen einseitigen Kundenreport.

## Grenzen
- Durch Consent-Ablehnung fehlen Daten (Modellierung mildert nur teilweise).
- Anrufe ohne Tracking-Nummer sind unsichtbar → Telefonnummer-Klicks + Gesprächserfassung kombinieren.
- **Wahrheit liegt im CRM:** Leads und Aufträge auch dort mit Quelle erfassen
  („Wie sind Sie auf uns aufmerksam geworden?“).

## Lernfragen & Übung

1. Was sind Schlüsselereignisse in GA4?
2. Warum braucht es eine einheitliche UTM-Konvention?
3. Warum liegt die „Wahrheit im CRM“?

**Übung:** Erstelle UTM-Links für drei echte Kanäle (GBP, Newsletter, Flyer-QR) und prüfe nach zwei Wochen in der Traffic-Akquisition, ob sie korrekt erscheinen.

<details><summary>Lösungshinweise</summary>

Wichtige Ereignisse, die als Erfolg zählen (früher „Conversions“ in GA4). Uneinheitliche Schreibweisen zersplittern Berichte. Analytics sieht keine Abschlüsse, Anrufe ohne Tracking und abgelehnte Cookies – das CRM schon.

</details>

## Streitfragen & Perspektiven

**Streitfrage: Braucht ein kleiner Betrieb überhaupt GA4?**
- *Pro:* Kostenlos, verknüpft mit Google Ads, zeigt Kanäle und Seiten.
- *Contra:* Komplex, Datenlücken durch Consent, Datenschutzfragen.
- *Einschätzung:* Schlank einrichten oder datenschutzfreundliche Alternativen prüfen; entscheidend ist die Quellenabfrage bei Anfragen.

**Perspektivwechsel**
- *Datenschutz:* jede Datensammlung braucht einen Zweck.
- *Inhaber:* will eine Seite mit 5 Zahlen, kein Dashboard mit 50.
- *Google:* GA4 speist das Google-Werbeökosystem.

## Blog-Winkel

**Mögliche Überschriften**
- GA4 für kleine Unternehmen: Das Minimal-Setup in 30 Minuten
- UTM-Parameter erklärt: Woher kommen Ihre Kunden wirklich?
- Warum Ihre Analytics-Zahlen nicht mit der Realität übereinstimmen

- **Hook:** Ihre Statistik sagt, Instagram bringt nichts. Ihre Kunden sagen etwas anderes.
- **Gegenthese:** Eine einfache Frage im Kontaktformular liefert oft bessere Daten als jedes Analytics-Setup.
- **Keyword-Idee:** `google analytics 4 einrichten`
- **Eigene Erfahrung:** passende Einträge aus dem [Praxis-Log](../praxis-log/) einbauen.

## Verwandte Artikel
- [Conversion-Tracking](../01-google-ads/conversion-tracking.md)
- [Monatsreport](../05-checklisten/monatsreport.md)
