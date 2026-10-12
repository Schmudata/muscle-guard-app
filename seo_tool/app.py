"""Mini-API für den SEO-Check.

Start lokal:   python app.py
Produktion:    gunicorn -w 2 -b 0.0.0.0:8000 app:app
"""

import os
import time
from collections import defaultdict, deque

from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS

from seo_checker import CheckError, run_seo_check

app = Flask(__name__, static_folder="static")

# Nur deine eigene Website darf die API aufrufen, z. B.
# ALLOWED_ORIGINS="https://www.deine-domain.de,https://deine-domain.de"
origins = os.environ.get("ALLOWED_ORIGINS", "*").split(",")
CORS(app, resources={r"/api/*": {"origins": origins}})

# Einfaches Rate-Limit pro IP (im Speicher, reicht für einen Server-Prozess)
RATE_LIMIT = int(os.environ.get("RATE_LIMIT_PER_HOUR", "20"))
_hits: dict[str, deque] = defaultdict(deque)


def _rate_limited(ip: str) -> bool:
    now = time.time()
    q = _hits[ip]
    while q and now - q[0] > 3600:
        q.popleft()
    if len(q) >= RATE_LIMIT:
        return True
    q.append(now)
    return False


@app.post("/api/seo-check")
def seo_check():
    ip = request.headers.get("X-Forwarded-For", request.remote_addr).split(",")[0].strip()
    if _rate_limited(ip):
        return jsonify(error="Zu viele Anfragen – bitte später erneut versuchen."), 429

    url = (request.get_json(silent=True) or {}).get("url", "")
    try:
        return jsonify(run_seo_check(url))
    except CheckError as e:
        return jsonify(error=str(e)), 400
    except Exception:
        app.logger.exception("SEO-Check fehlgeschlagen")
        return jsonify(error="Unerwarteter Fehler beim Prüfen der Seite."), 500


@app.get("/")
def demo():
    return send_from_directory(app.static_folder, "seo-widget.html")


if __name__ == "__main__":
    app.run(port=int(os.environ.get("PORT", "8000")), debug=True)
