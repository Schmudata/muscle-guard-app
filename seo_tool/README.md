# SEO-Check-Tool

Leichtes Tool, mit dem Kunden ihre Website direkt auf deiner Seite auf SEO prüfen können.

```
Browser (Widget)  ──POST /api/seo-check──▶  Flask-API  ──lädt──▶  Kunden-Website
```

Die Prüfung läuft auf dem Server, weil der Browser fremde Websites wegen CORS nicht direkt auslesen darf.

## Dateien

| Datei | Zweck |
|---|---|
| `seo_checker.py` | Analyse: 15 Checks, Score 0–100 |
| `app.py` | Mini-API mit CORS und Rate-Limit |
| `static/seo-widget.html` | Eingabefeld + Ergebnisanzeige zum Einbetten |

## Lokal starten

```bash
pip install -r requirements.txt
python app.py          # → http://localhost:8000
python seo_checker.py beispiel.de   # Check direkt in der Konsole
```

## Auf der Website einbauen

1. API deployen (z. B. Render, Railway, Fly.io oder ein eigener VPS):
   `gunicorn -w 2 -b 0.0.0.0:8000 app:app`
2. Umgebungsvariable setzen: `ALLOWED_ORIGINS=https://www.deine-domain.de`
3. Den markierten Block aus `static/seo-widget.html` in deine Seite kopieren
   (WordPress: „Individuelles HTML“-Block, Wix/Webflow: „Embed“-Element).
4. Im Snippet `API_URL` auf deine API-Adresse setzen, z. B. `https://seo-api.deine-domain.de/api/seo-check`.

## Geprüfte Punkte

HTTPS · Indexierbarkeit (noindex) · Title · Meta-Description · H1 · Überschriften-Struktur ·
Alt-Texte · Mobile Viewport · Canonical · `lang`-Attribut · Open Graph · Server-Antwortzeit ·
robots.txt · sitemap.xml · Textmenge

## Sicherheit

- Interne/private IP-Adressen werden blockiert (auch nach Weiterleitungen), damit niemand über das Tool deinen Server oder dein Netzwerk ausspäht.
- Max. 3 MB pro Seite, 10 s Timeout, max. 5 Weiterleitungen.
- Rate-Limit pro IP (`RATE_LIMIT_PER_HOUR`, Standard 20).
