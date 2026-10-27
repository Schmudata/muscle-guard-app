---
titel: Conversion-Tracking & Consent in Deutschland
kategorie: google-ads
tags: [google-ads, tracking, analytics, recht]
zielgruppe: Experten
tiefe: vertieft
zuletzt_geprueft: 2026-10
---

<!-- Kennzeichnung: ✅ belegt · 🧪 Praxiserfahrung · ⚖️ strittig · ❓ ungeklärt -->

# Conversion-Tracking & Consent in Deutschland

## Kurzfassung
Conversion-Tracking ist die **Datengrundlage** für Smart Bidding, Budgetentscheidungen und
Reports. In Deutschland stehen dabei zwei Kräfte gegeneinander: **strenge Einwilligungspflichten**
(§ 25 TDDDG plus DSGVO, konsequent ausgelegt von Behörden und Gerichten) und ein Google-Ökosystem,
das immer stärker auf Daten und Modellierung setzt. Die Folge: Deutsche Werbetreibende sehen
**einen deutlich kleineren Teil ihrer Conversions direkt** als Werbetreibende in den USA.
Professionelles Tracking heißt hier deshalb:
1. rechtssicher einwilligen lassen,
2. **First-Party- und Offline-Daten** nutzen (erweiterte Conversions, CRM-Import, Anrufe),
3. Lücken bewusst über Modellierung und eigene Quellenabfragen schließen.

## Details: Mechanik

### Wie Google Ads eine Conversion zuordnet
1. Klick auf Anzeige → Google hängt die **GCLID** (Click-ID) an die URL (Auto-Tagging).
2. Der Google-Tag speichert die Click-ID in einem First-Party-Cookie (`_gcl_aw`) bzw. liest sie bei
   der Conversion aus.
3. Bei der Conversion (Danke-Seite, Event) sendet der Tag die Conversion mit Click-ID an Google.
4. **Erweiterte Conversions:** Zusätzlich werden **gehashte** (SHA-256) Nutzerdaten wie E-Mail
   oder Telefon übertragen. Google gleicht sie mit angemeldeten Google-Konten ab. Das verbessert
   den Abgleich, wenn Cookies fehlen oder geräteübergreifend gekauft wird.
5. **Offline-Import / erweiterte Conversions für Leads:** Der spätere Abschluss im CRM wird
   nachträglich mit Click-ID und/oder gehashten Daten hochgeladen.
6. **Modellierung:** Fehlende Conversions (fehlende Einwilligung, Browser-Einschränkungen)
   schätzt Google und weist sie in den Berichten mit aus.

### Welche Conversions für lokale KMU wichtig sind

| Conversion | Messung | Primär? | Anmerkung |
|---|---|---|---|
| Kontaktformular | Google-Tag (Event/Danke-Seite) + erweiterte Conversions | ✅ Ja | Spam-Filter, Danke-Seite nicht direkt erreichbar |
| Anruf aus Anzeige (Anruf-Asset) | Google-Weiterleitungsnummer, Mindestdauer | ✅ Ja | In DE verfügbar ✅; Mindestdauer z. B. 60–90 s |
| Anruf von der Website | Website-Anruf-Conversion (Nummerntausch per Snippet) | ✅ Ja | Nummernformat in GTM beachten (national, ohne „+“) ✅ |
| Klick auf Telefonnummer | Event | ⚠️ Sekundär | Ein Klick ist kein Gespräch |
| Termin-/Tischbuchung | Event aus Buchungstool | ✅ Ja | iFrames/Drittdomains erschweren die Messung |
| Routenanfrage / Ladenbesuch | Standortbasierte Conversions | je nach Ziel | Ladenbesuche nur bei ausreichendem Volumen verfügbar |
| Qualifizierter Lead / Auftrag | Offline-Import aus CRM | ✅ Bester Wert | Wichtigstes Signal für Smart Bidding |

## Deutschland-Spezifika

### Rechtsrahmen
- **§ 25 TDDDG** (Telekommunikation-Digitale-Dienste-Datenschutz-Gesetz, bis Mai 2024 „TTDSG“):
  Jeder Zugriff auf das Endgerät (Cookies, Local Storage, Fingerprinting) braucht eine
  **Einwilligung**, außer er ist **unbedingt erforderlich** für einen ausdrücklich gewünschten Dienst.
  Werbe- und Analyse-Tracking fällt nicht unter die Ausnahme. ✅
- **DSGVO:** Für die anschließende Verarbeitung personenbezogener Daten (z. B. Übermittlung an
  Google) braucht es zusätzlich eine Rechtsgrundlage. In der Praxis ist das ebenfalls die
  Einwilligung. Die Wirksamkeit der Einwilligung richtet sich nach Art. 4 Nr. 11 und Art. 7 DSGVO. ✅
- **EuGH „Planet49“ (C-673/17, 01.10.2019) und BGH „Cookie-Einwilligung II“ (I ZR 7/16, 28.05.2020):**
  Vorangekreuzte Häkchen sind keine wirksame Einwilligung. ✅
- **VG Hannover, Urteil vom 19.03.2025, Az. 10 A 5385/22:**
  - Schon der **Google Tag Manager** greift auf das Endgerät zu und braucht daher eine
    Einwilligung. Er darf also erst nach Zustimmung laden bzw. muss ins Consent-Tool eingebunden sein.
  - Ein Banner **ohne gleichwertige Ablehnen-Option** ist unzulässig.
  - Einschätzung: ⚖️ Das ist ein erstinstanzliches Verwaltungsgerichtsurteil, keine
    höchstrichterliche Klärung. Für Behörden ist es aber ein starkes Signal.
- **Einwilligungsverwaltungsverordnung (EinwV), in Kraft seit 01.04.2025:**
  - Sie ermöglicht anerkannte Dienste (PIMS), die Einwilligungen zentral speichern.
  - Für Websitebetreiber ist das **freiwillig**, Banner bleiben erlaubt. Hat ein Nutzer seine
    Präferenzen bei einem anerkannten Dienst gesetzt, müssen sie respektiert werden.
  - Die praktische Wirkung ist bisher gering. ❓
- **Google-Richtlinie zur Nutzereinwilligung in der EU:** Seit März 2024 müssen Werbetreibende im
  EWR Einwilligungssignale über **Consent Mode v2** übermitteln (`ad_user_data`,
  `ad_personalization` zusätzlich zu `ad_storage`, `analytics_storage`). Ohne sie entfallen u. a.
  Remarketing-Listen und ein Teil der Messfunktionen. ✅

### Consent Mode: Basic vs. Advanced ⚖️

| | **Basic** | **Advanced** |
|---|---|---|
| Verhalten | Google-Tags laden erst nach Einwilligung | Tags laden sofort; ohne Einwilligung werden cookielose „Pings“ gesendet |
| Modellierung | Allgemeines Modell (schwächer) | Konto-spezifische Modellierung (besser) |
| Datenschutz | Konservativ, gut vertretbar | Strittig: Auch ohne Einwilligung wird verarbeitet (IP, User-Agent, Zeitstempel) |
| Wer empfiehlt was | Datenschutzorientierte Berater | Viele Agenturen und Shops mit hohem Budget, Google |

Einschätzung: Für die meisten KMU in Deutschland ist **Basic** die rechtlich risikoärmere
Wahl. Das gilt umso mehr nach dem Urteil des VG Hannover, wonach selbst das Laden des GTM vor der
Einwilligung problematisch ist. Advanced Mode bringt messbar bessere Modellierung. Er sollte aber
nur nach Abstimmung mit der Datenschutzberatung eingesetzt werden. Eine Stellungnahme einer
deutschen Aufsichtsbehörde speziell zum Advanced Mode war bei der Recherche nicht auffindbar. ❓

### Consent-Raten in Deutschland
Deutsche Nutzer lehnen im europäischen Vergleich überdurchschnittlich oft ab. Die Zahlen gehen je
nach Definition (nur Marketing-Cookies oder alle, Banner-Design, Branche) weit auseinander:

| Wert | Quelle | Einordnung |
|---|---|---|
| ~40 % Zustimmung bei gleichwertigen Buttons; ~54 % bei „dezenterem“ Ablehnen | etracker Consent Benchmark 2025, 500 Websites (zitiert über blckalpaca.at) | [Anbieter], Sekundärquelle; „dezenteres Ablehnen“ ist rechtlich riskant |
| 36 % Opt-in für Marketing-Cookies (EU-Schnitt 46 %) | Usercentrics/Cookiebot-Benchmarks (zitiert über searchlab.nl) | [Anbieter], Methodik nicht offengelegt |
| Nur ~1 % öffnen die Detail-Einstellungen | advance-metrics Studie | [Anbieter] |

Faustregel (🧪): Bei rechtskonformen Bannern in Deutschland sind **35–55 % Zustimmung** realistisch.
**Etwa die Hälfte der Conversions** ist also nicht direkt beobachtbar. Das ist der Grund, warum
erweiterte Conversions, Offline-Importe und Quellenabfragen in DE wichtiger sind als in den USA.

### Anruf-Tracking in Deutschland
- **Google-Weiterleitungsnummern** sind in Deutschland verfügbar ✅. Ist keine lokale Nummer
  verfügbar, kann eine gebührenfreie Nummer erscheinen. Nummern gehören Google und können
  geändert werden.
- **Alternative:** Deutsche Call-Tracking-Anbieter mit lokalen Rufnummern, Gesprächsauswertung und
  CRM-Anbindung (z. B. matelso). Sie liefern Daten per Offline-Import an Google Ads.
- **Recht:** Gesprächsaufzeichnung nur mit Einwilligung (§ 201 StGB). Dynamischer Nummerntausch auf
  der Website setzt in der Regel ein Script mit Endgerätezugriff voraus und gehört damit in die
  Consent-Logik.

## Ansätze & Denkschulen

| Ansatz | Kernidee | Voraussetzungen | Stärken | Schwächen | Vertreten von |
|---|---|---|---|---|---|
| **1. Minimal & rechtssicher** | Google-Tag nur nach Einwilligung (Basic), Formular + Anrufe, Quellenabfrage im Formular | Consent-Tool, saubere Ziele | Geringes Rechtsrisiko, schnell umgesetzt | Große Datenlücken, Smart Bidding sieht wenig | Datenschutzorientierte Berater, viele KMU |
| **2. First-Party & Offline** | Erweiterte Conversions + CRM-Import (Lead → Termin → Auftrag mit Wert) | CRM oder sauber geführte Liste, GCLID im Formular speichern | Optimiert auf echte Aufträge statt Formulare; robust gegen Cookie-Verlust | Prozessdisziplin nötig; Upload-Wege ändern sich (Data Manager) | Fortgeschrittene PPC-Praktiker, Google |
| **3. Maximal-Modellierung** | Advanced Consent Mode + serverseitiges Tagging + erweiterte Conversions | Budget, technisches Know-how, Rechtsberatung | Höchste Datenmenge, beste Modellierung | Kosten, Komplexität, ⚖️ rechtliches Restrisiko | Shops/Agenturen mit hohem Budget, Tracking-Dienstleister |
| **4. Aggregiert / inkrementell** | Statt Einzelklick-Zuordnung: Geo-Tests, Vorher/Nachher, Marketing-Mix-Modelle | Ausreichendes Volumen, Geduld | Ohne personenbezogene Daten möglich, misst echten Zusatznutzen | Für kleine Budgets statistisch schwierig | Analytics-Puristen, große Marken |

**Empfehlung für KMU (🧪):** Ansatz 1 als Pflicht, Ansatz 2 als wichtigster Hebel. Ansatz 3 erst ab
spürbarem Budget (grob ab einigen tausend Euro pro Monat) und mit Rechtsberatung. Aus Ansatz 4 die
**Denkweise** übernehmen: Ein einfacher Vorher/Nachher-Vergleich der Gesamtanfragen ist oft
aussagekräftiger als die Zahl in Google Ads.

### Werte statt Zählungen (Value-Based Bidding für Leads)
- **Idee:** Jede Conversion bekommt einen Wert, z. B. Ø Auftragswert × Abschlussquote je Lead-Typ.
  Beispiel: 20 % Abschlussquote × 2.500 € Auftrag = 500 € Lead-Wert.
- Zusätzlich lassen sich **Wertregeln** setzen, die Werte nach Standort, Gerät oder Zielgruppe
  anpassen. Sie wirken nur bei wertbasierten Gebotsstrategien. ✅
- **Pro:** Smart Bidding unterscheidet „Anfrage Badsanierung“ von „Anfrage Silikonfuge“.
- **Contra:** Bei wenigen Abschlüssen und langem Verkaufszyklus (Wochen bis Monate) lernt der
  Algorithmus langsam. Grobe Werte können falsch steuern. ⚖️
- Google empfiehlt wertbasiertes Bieten bei Suchkampagnen vor allem, wenn **mindestens zwei
  unterschiedliche Werte** gemeldet werden können.

## Blick in andere Märkte

| Markt | Ansatz | Übertragbar auf DE? |
|---|---|---|
| **USA** | Überwiegend **Opt-out**-Logik. Tracking ist standardmäßig an; Pflichten: Hinweis, funktionierender Widerspruch, in Kalifornien Beachtung von Global Privacy Control. Anbieter berichten von deutlich geringeren Datenverlusten als in der EU. | ❌ Rechtlich nicht. ✅ Aber US-Fachartikel und Benchmarks beruhen auf viel vollständigeren Daten, deshalb US-Zahlen (CPA, CVR) nicht 1:1 übernehmen. |
| **USA / UK (Lead-Gen-Szene)** | Starker Fokus auf **Offline-Conversion-Import** und Call-Tracking mit Gesprächsauswertung; Lead-Gen-Agenturen optimieren auf „Booked Job“/„Sale“ statt Formular. | ✅ Sehr gut übertragbar, und in DE wegen der Consent-Lücken sogar noch wichtiger. |
| **UK** | UK GDPR + PECR: im Kern ebenfalls Einwilligung; Diskussion über Lockerungen für Analyse-Cookies mit geringem Risiko. | ⚠️ Beobachten. Deutsche Regeln bleiben strenger. |
| **Frankreich (CNIL)** | Die Behörde hat Mess-Tools mit strengen Voraussetzungen von der Einwilligung befreit, wenn sie sehr datensparsam konfiguriert sind. | ⚠️ Kein deutsches Recht, aber ein Argument für datensparsame Analyse-Tools. Die Ausnahme gilt nicht für Werbe-Tracking. |
| **Österreich / Schweiz** | AT: ähnlich streng (DSGVO, TKG). CH: revDSG, in der Praxis etwas liberaler bei Analyse-Tracking. | Für DACH-weite Kampagnen das strengste Regime (DE/AT) als Standard nehmen. |

## Zahlen & Studien

| Aussage | Wert | Quelle | Einordnung |
|---|---|---|---|
| Consent-Rate DE | ~36–54 % je nach Definition | siehe oben | [Anbieter], widersprüchlich |
| Upload-Weg für Offline-Conversions | Migration zur Data Manager API; Uploads über die Google Ads API laut Google ab 15.06.2026 gesperrt | Google Ads Hilfe | [Offiziell]; Umfang der Sperre wird unterschiedlich berichtet |
| GCLID-only-Import | Von Google als „Legacy“ eingestuft; empfohlen: erweiterte Conversions für Leads | Google Ads Hilfe | [Offiziell] |

## Praxis-Tipps
- **Consent-Banner:** „Alle ablehnen“ gleichwertig auf der ersten Ebene, Google-Tag/GTM erst nach
  Einwilligung (Basic Mode), Consent Mode v2 korrekt übergeben und im Diagnosebereich prüfen.
- **GCLID speichern:** Ein verstecktes Formularfeld liest die GCLID aus und speichert sie im
  CRM. Nur mit Einwilligung, wenn dafür ein Cookie oder Speicher genutzt wird.
- **Lead-Status-Pipeline:** Anfrage → qualifiziert → Termin → Auftrag mit Wert. Mindestens
  wöchentlich hochladen. Werkzeuge: Google Sheets/Data Manager, Zapier/Make oder eine CRM-Anbindung
  (HubSpot, Pipedrive u. a.).
- **Quellenabfrage** („Wie sind Sie auf uns aufmerksam geworden?“) im Formular und am Telefon.
  Das ist die einwilligungsfreie Wahrheit über die eigenen Kanäle.
- **Plausibilitätscheck monatlich:** Conversions in Google Ads vs. tatsächliche Formulareingänge
  und Anrufe. Abweichungen > 20 % untersuchen.
- **Spam-Leads aussortieren:** Honeypot, serverseitige Validierung. Spam nicht als Conversion
  zählen, sonst lernt der Algorithmus, Spam zu finden (besonders bei PMax).

## Häufige Fehler
- GTM oder Google-Tag laden vor der Einwilligung (⚖️ nach VG Hannover besonders riskant).
- Banner ohne gleichwertige Ablehnen-Option oder mit „dark patterns“.
- Doppelte Conversions (Ads-Tag und GA4-Import beide primär).
- Telefonklick als primäre Conversion. Smart Bidding optimiert dann auf Klicks, nicht auf Gespräche.
- Danke-Seite direkt aufrufbar oder indexiert.
- US-Benchmarks mit deutschen Zahlen vergleichen und daraus „schlechte Performance“ ableiten.
- Keine Rechtsberatung: Dieser Artikel ist eine fachliche Einordnung, keine Rechtsberatung.

## Lernfragen & Übung

1. Warum braucht es in Deutschland sowohl § 25 TDDDG als auch eine DSGVO-Rechtsgrundlage?
2. Welche Folgen hat das Urteil des VG Hannover für die technische Einbindung des Google Tag Managers?
3. Worin unterscheiden sich Basic und Advanced Consent Mode technisch und rechtlich?
4. Warum sind Offline-Conversions in Deutschland noch wichtiger als in den USA?
5. Wie berechnet man einen Conversion-Wert für einen Lead ohne Umsatz?

**Übung:** Prüfe eine echte Website mit den Browser-Entwicklertools (Netzwerk-Tab, Filter „google“):
Werden vor dem Klick auf „Akzeptieren“ Anfragen an googletagmanager.com oder google-analytics.com
gesendet? Dokumentiere das Ergebnis und leite ab, welcher Consent-Modus genutzt wird.

<details><summary>Lösungshinweise</summary>

1. § 25 TDDDG regelt den Zugriff auf das Endgerät, die DSGVO die anschließende Verarbeitung
   personenbezogener Daten. Beides wird getrennt geprüft.
2. GTM darf erst nach Einwilligung laden bzw. muss über das Consent-Tool gesteuert werden. Er gilt
   nicht als „technisch notwendig“.
3. Basic: keine Google-Anfragen vor der Einwilligung. Advanced: cookielose Pings auch ohne
   Einwilligung, dadurch bessere Modellierung, aber strittige Verarbeitung.
4. Wegen niedriger Consent-Raten (etwa 35–55 %) ist ein Großteil der Online-Conversions
   unsichtbar. Offline-Daten aus dem CRM schließen die Lücke.
5. Ø Auftragswert × Abschlussquote des Lead-Typs.

Übung: Gibt es Anfragen vor der Einwilligung, ist es Advanced Mode oder eine fehlerhafte Einbindung.
</details>

## Streitfragen & Perspektiven

**Streitfrage: Advanced Consent Mode – notwendig oder rechtlich zu riskant?**
- *Pro:* Deutlich bessere Modellierung; ohne sie arbeitet Smart Bidding in DE mit halben Daten.
  Viele Shops mit Budget sehen „kaum einen Weg daran vorbei“.
- *Contra:* Verarbeitung ohne Einwilligung. Nach VG Hannover ist schon der Lade-Vorgang heikel. Eine
  behördliche Freigabe gibt es nicht.
- *Einschätzung:* KMU nutzen Basic und gleichen die Lücke über First-Party- und Offline-Daten aus.
  Advanced nur nach dokumentierter Abwägung mit der Datenschutzberatung.

**Streitfrage: Lohnt sich serverseitiges Tracking (sGTM) für KMU?**
- *Pro:* Mehr Kontrolle über die Datenflüsse (welche Daten verlassen den eigenen Server), robustere
  Messung, schnellere Website.
- *Contra:* Hosting-Kosten und Wartung. Die **Einwilligungspflicht bleibt** gleich: Server-Side ist
  kein Weg um den Consent herum, und Googles Bedingungen untersagen ausdrücklich, damit Einwilligungen
  zu umgehen.
- *Einschätzung:* Erst ab spürbarem Werbebudget oder bei konkreten Datenlücken. Für kleine Websites
  mit wenigen Tags übersteigen Kosten und Fehleranfälligkeit oft den Nutzen.

**Streitfrage: Soll ein Telefonklick als Conversion zählen?**
- *Pro:* Auf Mobilgeräten ist er oft das einzige messbare Signal; ein besseres Signal als keins.
- *Contra:* Viele Klicks führen nicht zu Gesprächen; Smart Bidding optimiert auf Neugier.
- *Einschätzung:* Als sekundäre Conversion beobachten. Primär sind echte Anrufe mit Mindestdauer.

**Perspektivwechsel**
- *Datenschutzbehörde:* Einwilligung ist der Normalfall, nicht die Ausnahme. Technik muss sich danach richten.
- *Google:* Braucht Signale für Smart Bidding und füllt Lücken mit Modellen. Modelle sind Schätzungen,
  deren Güte man nicht prüfen kann.
- *Agentur:* Gute Zahlen in Google Ads verkaufen sich leicht. Ehrlicher ist der Abgleich mit dem CRM.
- *Inhaber:* „Wie sind Sie auf uns aufmerksam geworden?“ bleibt oft das verlässlichste Tracking.
- *US-Praktiker:* Schauen auf „Signal Loss“ als Problem. In DE ist er der Normalzustand.

## Blog-Winkel

**Mögliche Überschriften**
- Halbe Daten, ganze Entscheidungen: Google-Ads-Tracking in Deutschland nach dem Tag-Manager-Urteil
- Basic oder Advanced? Consent Mode v2 für kleine Unternehmen ehrlich erklärt
- Warum deutsche Google-Ads-Zahlen nicht mit US-Benchmarks vergleichbar sind
- Vom Formular zum Auftrag: So trainieren Sie Google mit echten Umsätzen

- **Hook:** Etwa jeder zweite Besucher lehnt Cookies ab. Ihr Google-Ads-Konto sieht also nur die halbe Wahrheit.
- **Gegenthese:** Die Datenlücke ist für KMU weniger schlimm als gedacht, wenn Offline-Daten und Quellenabfrage sauber laufen.
- **Keyword-Idee:** `consent mode v2 basic advanced`, `google ads tracking dsgvo`
- **Eigene Erfahrung:** passende Einträge aus dem [Praxis-Log](../praxis-log/) einbauen.

## Verwandte Artikel
- [Analytics mit GA4](../04-digital-marketing/analytics-ga4.md)
- [Gebotsstrategien](gebotsstrategien.md)
- [Budgetplanung & KPIs](../04-digital-marketing/budgetplanung-kpis.md)

## Quellen
- [Recht] § 25 TDDDG – gesetze-im-internet.de (Telekommunikation-Digitale-Dienste-Datenschutz-Gesetz)
- [Recht] VG Hannover, 19.03.2025, 10 A 5385/22 – Besprechungen: https://www.it-recht-kanzlei.de/vg-hannover-cookie-consent-ablehnen-google-tag-manager.html, https://www.tcilaw.de/en/german-court-google-tag-manager-only-permissible-with-consent/
- [Recht] Einwilligungsverwaltungsverordnung – Überblick: https://www.haendlerbund.de/de/ratgeber/recht/pims-einwilligungsverwaltungsverordnung, https://cms.law/de/deu/legal-updates/das-ende-der-cookie-banner-dienste-zur-einwilligungsverwaltung
- [Offiziell] Google Ads: Offline-Conversion-Importe – https://support.google.com/google-ads/answer/2998031
- [Offiziell] Google Ads: Erweiterte Conversions für Leads – https://support.google.com/google-ads/answer/15713840
- [Offiziell] Google Ads: Anrufberichte/Weiterleitungsnummern – https://support.google.com/google-ads/answer/2454052?hl=de
- [Offiziell] Google Ads: Best Practices für hochwertige Leads – https://support.google.com/google-ads/answer/13489421
- [Fachmedium] Search Engine Journal: Conversion-Werte für Lead-Gen – https://searchenginejournal.com/assigning-conversion-values-to-make-value-based-bidding-work-for-lead-gen/526349
- [Anbieter] Consent-Raten DACH (etracker-Benchmark, zitiert) – https://blckalpaca.at/de/wissen/social-media/social-media-analytics-kpis-measurement/consent-rate-tracking-recht-dach
- [Anbieter] Basic vs. Advanced Consent Mode – https://e-dialog.group/en/blog/consent-mode-basic-vs-advanced-an-overview-of-the-differences/
- [Anbieter] Server-Side Tracking und DSGVO – https://stape.io/de/blog/server-side-tracking-und-dsgvo
- Abgerufen: 2026-10
