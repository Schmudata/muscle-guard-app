---
titel: Analytics mit Google Analytics 4
kategorie: digital-marketing
tags: [analytics, tracking]
zielgruppe: Fortgeschrittene
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

## Verwandte Artikel
- [Conversion-Tracking](../01-google-ads/conversion-tracking.md)
- [Monatsreport](../05-checklisten/monatsreport.md)
