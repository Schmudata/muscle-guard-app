---
titel: Conversion-Tracking & Consent Mode
kategorie: google-ads
tags: [google-ads, tracking, analytics]
zielgruppe: Fortgeschrittene
zuletzt_geprueft: 2026-10
---

# Conversion-Tracking & Consent Mode

## Kurzfassung
Ohne sauberes Tracking ist Google Ads Blindflug – und Smart Bidding optimiert auf die falschen
Signale. Für lokale KMU müssen neben Formularen vor allem **Anrufe** erfasst werden. Im EWR
ist **Consent Mode v2** Voraussetzung für Remarketing und personalisierte Messung.

## Was sollte gemessen werden?

| Conversion | Typ | Primär (Gebotsziel)? |
|---|---|---|
| Kontaktformular abgeschickt (Danke-Seite / Event) | Website | Ja |
| Anruf über Anzeige (Anruf-Asset) | Anrufe aus Anzeigen | Ja, mit Mindestdauer (z. B. 60 s) |
| Anruf von Website (Google-Weiterleitungsnummer) | Website-Anruf | Ja, mit Mindestdauer |
| Klick auf Telefonnummer (mobil) | Website-Event | Eher sekundär (kein echtes Gespräch) |
| Online-Terminbuchung / Reservierung | Website | Ja |
| Kauf (Shop) mit Wert | Website | Ja |
| Routenanfrage / Ladenbesuch | GBP-/Standort-Conversion | je nach Ziel |
| Abgeschlossener Auftrag aus CRM | Offline-Import | Ja (bester Wert) |
| Scrolltiefe, Verweildauer, Seitenaufrufe | Mikro | Nein – nur beobachten |

## Technische Umsetzung
1. **Google-Tag** (gtag.js) oder **Google Tag Manager** einbinden.
2. Conversions in Google Ads anlegen **oder** aus GA4 importieren (nicht beides doppelt als
   primär zählen).
3. **Erweiterte Conversions** aktivieren (gehashte E-Mail/Telefon aus dem Formular).
4. **Consent Mode v2** über ein zertifiziertes CMP (Cookie-Banner) einrichten:
   Parameter `ad_storage`, `analytics_storage`, `ad_user_data`, `ad_personalization`.
5. **Zählweise:** Leads = „Eine“, Käufe = „Jede“.
6. **Conversion-Werte** hinterlegen – auch bei Leads ein geschätzter Wert
   (z. B. Ø Auftragswert × Abschlussquote) hilft bei der Bewertung.
7. **Offline-Conversions:** GCLID im Formular mitspeichern bzw. erweiterte Conversions für
   Leads nutzen und Abschlüsse regelmäßig hochladen (manuell, Sheets, Zapier, CRM-Anbindung).

## Testen
- Google Tag Assistant / GTM-Vorschau.
- Testformular absenden → Status in Google Ads nach einigen Stunden prüfen.
- Diagnose-Tab der Conversion-Aktion und von erweiterten Conversions ansehen.

## Rechtliches (DACH)
- Cookie-Banner mit echter Ablehnungsmöglichkeit, Tracking erst nach Einwilligung (TTDSG/TDDDG,
  DSGVO).
- Datenschutzerklärung um Google Ads, GA4, Conversion-Linker etc. ergänzen.
- Hinweis: Keine Rechtsberatung – im Zweifel Datenschutzbeauftragte/Anwalt fragen.

## Häufige Fehler
- Doppelte Conversions (Ads-Tag + GA4-Import beide primär).
- Danke-Seite ist direkt aufrufbar und wird von Bots/Nutzern ohne Formular erreicht.
- Spam-Formulare werden als Conversion gezählt → Honeypot/Captcha, Pflichtfelder prüfen.
- Anrufe gar nicht gemessen, obwohl 70 % der Anfragen telefonisch kommen.

## Lernfragen & Übung

1. Warum sollte ein Klick auf die Telefonnummer eher sekundär sein?
2. Was regelt Consent Mode v2 und was passiert ohne ihn im EWR?
3. Wie entstehen doppelte Conversions?

**Übung:** Erstelle für einen Betrieb eine Tracking-Tabelle: alle Kontaktwege, wie sie gemessen werden, primär/sekundär, Wert. Prüfe dann die Website mit Tag Assistant.

<details><summary>Lösungshinweise</summary>

Ein Klick ist kein Gespräch – Anrufe mit Mindestdauer sind aussagekräftiger. Consent Mode übermittelt Einwilligungsstatus; ohne ihn fehlen Remarketing- und Messfunktionen. Doppelzählung: Ads-Tag und GA4-Import gleichzeitig primär.

</details>

## Streitfragen & Perspektiven

**Streitfrage: Lohnt sich serverseitiges Tracking für KMU?**
- *Pro:* Robustere Daten, weniger Verluste durch Browser-Einschränkungen, mehr Kontrolle über Datenflüsse.
- *Contra:* Zusätzliche Kosten und Komplexität; Einwilligungspflicht bleibt trotzdem.
- *Einschätzung:* Erst Basis-Tracking, Consent und Offline-Import sauber; Server-Side erst ab spürbarem Budget.

**Perspektivwechsel**
- *Datenschutz:* Tracking ist rechtlich heikel – Einwilligung ist keine Formalie.
- *Google:* braucht Daten für Smart Bidding und modelliert fehlende Daten – Modelle sind Schätzungen.
- *Inhaber:* „Wie sind Sie auf uns aufmerksam geworden?“ ist oft das ehrlichste Tracking.

## Blog-Winkel

**Mögliche Überschriften**
- Ohne Tracking keine Optimierung: Conversion-Tracking für kleine Unternehmen erklärt
- Anrufe messen: So sehen Sie, welche Anzeige das Telefon klingeln lässt
- Consent Mode v2 – was Sie als kleines Unternehmen wirklich tun müssen

- **Hook:** Ein Großteil der Anfragen lokaler Betriebe kommt per Telefon – und taucht in keinem Bericht auf.
- **Gegenthese:** Perfektes Tracking gibt es nicht mehr; wer auf 100 % Daten wartet, entscheidet nie.
- **Keyword-Idee:** `google ads conversion tracking einrichten`
- **Eigene Erfahrung:** passende Einträge aus dem [Praxis-Log](../praxis-log/) einbauen.

## Verwandte Artikel
- [Analytics mit GA4](../04-digital-marketing/analytics-ga4.md)
- [Gebotsstrategien](gebotsstrategien.md)
