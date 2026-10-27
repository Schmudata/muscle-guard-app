# Wissensdatenbank: Google Ads & Digital Marketing für KMU und lokale Unternehmen

Praxisorientierte Wissenssammlung für **kleine Unternehmen** und **lokales Marketing** –
mit Schwerpunkt auf Google Ads. Jede Datei ist ein eigenständiger Wissensartikel mit
Frontmatter (Kategorie, Tags, Prüfdatum), damit die Sammlung in Obsidian, VS Code,
GitHub oder als Kontext für KI-Assistenten gut durchsuchbar ist.

> Plattformen ändern sich laufend. Jeder Artikel hat ein Feld `zuletzt_geprueft`.
> Zahlen, Limits und Features vor Kundeneinsatz in der offiziellen Hilfe gegenprüfen.

## Struktur

| Bereich | Inhalt |
|---|---|
| [00-grundlagen](00-grundlagen/) | Glossar, Marketing-Funnel für KMU |
| [01-google-ads](01-google-ads/) | Kontostruktur, Kampagnentypen, Gebote, Tracking, kleine Budgets, Optimierungsroutine |
| [02-lokales-marketing](02-lokales-marketing/) | Google Unternehmensprofil, Local SEO, Bewertungen, lokale Google Ads |
| [03-keywords-anzeigen-landingpages](03-keywords-anzeigen-landingpages/) | Keyword-Recherche, negative Keywords, Anzeigentexte & Assets, Landingpages |
| [04-digital-marketing](04-digital-marketing/) | Meta Ads, E-Mail, Content, GA4, Budgetplanung & KPIs |
| [05-checklisten](05-checklisten/) | Account-Audit, Kampagnen-Launch, Monatsreport |
| [06-branchen-playbooks](06-branchen-playbooks/) | Handwerk, Gastronomie, Praxen & Dienstleister |
| [_vorlagen](_vorlagen/) | Vorlage für neue Artikel |

## Alle Artikel

### 00 – Grundlagen
- [Glossar](00-grundlagen/glossar.md)
- [Marketing-Funnel für lokale KMU](00-grundlagen/funnel-fuer-kmu.md)

### 01 – Google Ads
- [Kontostruktur](01-google-ads/kontostruktur.md)
- [Kampagnentypen im Überblick](01-google-ads/kampagnentypen.md)
- [Gebotsstrategien](01-google-ads/gebotsstrategien.md)
- [Conversion-Tracking & Consent Mode](01-google-ads/conversion-tracking.md)
- [Google Ads mit kleinem Budget](01-google-ads/kleine-budgets.md)
- [Optimierungsroutine (wöchentlich/monatlich)](01-google-ads/optimierungsroutine.md)

### 02 – Lokales Marketing
- [Google Unternehmensprofil (GBP)](02-lokales-marketing/google-unternehmensprofil.md)
- [Local SEO](02-lokales-marketing/local-seo.md)
- [Bewertungsmanagement](02-lokales-marketing/bewertungen.md)
- [Lokale Google-Ads-Kampagnen](02-lokales-marketing/lokale-google-ads.md)

### 03 – Keywords, Anzeigen & Landingpages
- [Keyword-Recherche](03-keywords-anzeigen-landingpages/keyword-recherche.md)
- [Negative Keywords](03-keywords-anzeigen-landingpages/negative-keywords.md)
- [Anzeigentexte & Assets](03-keywords-anzeigen-landingpages/anzeigentexte-und-assets.md)
- [Landingpages](03-keywords-anzeigen-landingpages/landingpages.md)

### 04 – Digital Marketing
- [Meta Ads (Facebook & Instagram)](04-digital-marketing/meta-ads.md)
- [E-Mail-Marketing](04-digital-marketing/email-marketing.md)
- [Content-Marketing & Social Media](04-digital-marketing/content-marketing.md)
- [Analytics mit GA4](04-digital-marketing/analytics-ga4.md)
- [Budgetplanung & KPIs](04-digital-marketing/budgetplanung-kpis.md)

### 05 – Checklisten
- [Google-Ads-Account-Audit](05-checklisten/account-audit.md)
- [Kampagnen-Launch](05-checklisten/kampagnen-launch.md)
- [Monatsreport](05-checklisten/monatsreport.md)

### 06 – Branchen-Playbooks
- [Handwerk & Hausdienstleistungen](06-branchen-playbooks/handwerk.md)
- [Gastronomie & Einzelhandel vor Ort](06-branchen-playbooks/gastronomie-einzelhandel.md)
- [Praxen, Kanzleien & beratende Dienstleister](06-branchen-playbooks/praxen-dienstleister.md)

## Konventionen

- **Dateinamen:** kleingeschrieben, Bindestriche, keine Umlaute (`lokale-google-ads.md`).
- **Frontmatter:** siehe [_vorlagen/artikel-vorlage.md](_vorlagen/artikel-vorlage.md).
- **Aufbau eines Artikels:** Kurzfassung → Details → Praxis-Tipps → Häufige Fehler → Quellen.
- **Tags:** `google-ads`, `local`, `kmu`, `tracking`, `keywords`, `anzeigen`, `landingpage`,
  `social`, `email`, `analytics`, `budget`, `checkliste`, `branche`.
- **Pflege:** Bei Änderungen `zuletzt_geprueft` aktualisieren. Veraltetes nicht löschen, sondern
  mit „**Veraltet seit …**“ kennzeichnen, wenn es für Altkonten noch relevant ist.

## Automatische Aktualisierung

Eine geplante Claude-Aufgabe läuft **alle 7 Tage (montags früh)** und
1. recherchiert Neuerungen bei Google Ads, Google Unternehmensprofil, Local SEO, GA4,
   Meta Ads und relevantem Recht (DSGVO/UWG) seit der letzten Aktualisierung,
2. korrigiert veraltete Aussagen und ergänzt neue Features in den passenden Artikeln,
3. legt bei Bedarf neue Artikel an und pflegt dieses README,
4. aktualisiert `zuletzt_geprueft` und dokumentiert alles mit Quellen in
   [CHANGELOG.md](CHANGELOG.md),
5. committet und pusht die Änderungen.

## Offizielle Quellen

- Google Ads Hilfe: https://support.google.com/google-ads
- Google Unternehmensprofil Hilfe: https://support.google.com/business
- Google Analytics Hilfe: https://support.google.com/analytics
- Google Search Central: https://developers.google.com/search
- Meta Business Hilfe: https://www.facebook.com/business/help
