"""Leichtgewichtiger SEO-Check für eine einzelne URL.

Lädt die Seite, prüft die wichtigsten On-Page-Faktoren und gibt
einen Score (0–100) plus konkrete Empfehlungen zurück.
"""

import ipaddress
import socket
import time
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

USER_AGENT = "Mozilla/5.0 (compatible; SEO-Check/1.0)"
TIMEOUT = 10
MAX_BYTES = 3 * 1024 * 1024  # max. 3 MB HTML laden
MAX_REDIRECTS = 5


class CheckError(Exception):
    """Fehler, der dem Kunden angezeigt werden darf."""


# ---------------------------------------------------------------------------
# URL-Validierung & sicheres Laden (Schutz vor SSRF: keine internen Adressen)
# ---------------------------------------------------------------------------

def normalize_url(raw: str) -> str:
    raw = (raw or "").strip()
    if not raw:
        raise CheckError("Bitte eine URL eingeben.")
    if not raw.startswith(("http://", "https://")):
        raw = "https://" + raw
    parsed = urlparse(raw)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise CheckError("Ungültige URL.")
    return raw


def _assert_public_host(hostname: str) -> None:
    try:
        infos = socket.getaddrinfo(hostname, None)
    except socket.gaierror:
        raise CheckError(f"Domain '{hostname}' wurde nicht gefunden.")
    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if not ip.is_global:
            raise CheckError("Diese Adresse darf nicht geprüft werden.")


def safe_get(url: str, max_bytes: int = MAX_BYTES) -> tuple[requests.Response, bytes, str]:
    """GET mit manuellem Redirect-Handling, damit jede Station geprüft wird."""
    session = requests.Session()
    session.headers["User-Agent"] = USER_AGENT
    for _ in range(MAX_REDIRECTS + 1):
        _assert_public_host(urlparse(url).hostname)
        resp = session.get(url, timeout=TIMEOUT, allow_redirects=False, stream=True)
        if resp.is_redirect:
            url = urljoin(url, resp.headers.get("Location", ""))
            resp.close()
            continue
        body = b""
        for chunk in resp.iter_content(64 * 1024):
            body += chunk
            if len(body) > max_bytes:
                break
        resp.close()
        return resp, body, url
    raise CheckError("Zu viele Weiterleitungen.")


# ---------------------------------------------------------------------------
# Einzelne Checks
# ---------------------------------------------------------------------------

def _result(name, status, message, weight):
    # status: "ok" | "warn" | "fail"
    return {"name": name, "status": status, "message": message, "weight": weight}


def check_title(soup):
    tag = soup.find("title")
    title = tag.get_text(strip=True) if tag else ""
    if not title:
        return _result("Title-Tag", "fail", "Kein <title> vorhanden.", 15)
    n = len(title)
    if 30 <= n <= 60:
        return _result("Title-Tag", "ok", f"„{title}“ ({n} Zeichen)", 15)
    return _result("Title-Tag", "warn",
                   f"„{title}“ hat {n} Zeichen – ideal sind 30–60.", 15)


def check_meta_description(soup):
    tag = soup.find("meta", attrs={"name": lambda v: v and v.lower() == "description"})
    desc = (tag.get("content") or "").strip() if tag else ""
    if not desc:
        return _result("Meta-Description", "fail", "Keine Meta-Description vorhanden.", 12)
    n = len(desc)
    if 70 <= n <= 160:
        return _result("Meta-Description", "ok", f"{n} Zeichen – passt.", 12)
    return _result("Meta-Description", "warn",
                   f"{n} Zeichen – ideal sind 70–160.", 12)


def check_h1(soup):
    h1s = [h.get_text(strip=True) for h in soup.find_all("h1")]
    if not h1s:
        return _result("H1-Überschrift", "fail", "Keine H1-Überschrift gefunden.", 10)
    if len(h1s) > 1:
        return _result("H1-Überschrift", "warn",
                       f"{len(h1s)} H1-Überschriften – besser genau eine.", 10)
    return _result("H1-Überschrift", "ok", f"„{h1s[0][:80]}“", 10)


def check_headings(soup):
    counts = {f"h{i}": len(soup.find_all(f"h{i}")) for i in range(1, 7)}
    if counts["h2"] == 0:
        return _result("Überschriften-Struktur", "warn",
                       "Keine H2-Überschriften – Inhalte besser gliedern.", 5)
    summary = ", ".join(f"{k.upper()}: {v}" for k, v in counts.items() if v)
    return _result("Überschriften-Struktur", "ok", summary, 5)


def check_images_alt(soup):
    imgs = soup.find_all("img")
    if not imgs:
        return _result("Bilder Alt-Texte", "ok", "Keine Bilder auf der Seite.", 8)
    missing = [i for i in imgs if not (i.get("alt") or "").strip()]
    if not missing:
        return _result("Bilder Alt-Texte", "ok", f"Alle {len(imgs)} Bilder haben Alt-Texte.", 8)
    status = "fail" if len(missing) / len(imgs) > 0.5 else "warn"
    return _result("Bilder Alt-Texte", status,
                   f"{len(missing)} von {len(imgs)} Bildern ohne Alt-Text.", 8)


def check_viewport(soup):
    tag = soup.find("meta", attrs={"name": lambda v: v and v.lower() == "viewport"})
    if tag:
        return _result("Mobile Viewport", "ok", "Viewport-Meta-Tag vorhanden.", 8)
    return _result("Mobile Viewport", "fail",
                   "Kein Viewport-Tag – Seite ist evtl. nicht mobilfreundlich.", 8)


def check_https(final_url):
    if urlparse(final_url).scheme == "https":
        return _result("HTTPS", "ok", "Seite wird verschlüsselt ausgeliefert.", 10)
    return _result("HTTPS", "fail", "Seite läuft ohne HTTPS.", 10)


def check_canonical(soup):
    tag = soup.find("link", rel=lambda v: v and "canonical" in [r.lower() for r in v])
    if tag and tag.get("href"):
        return _result("Canonical-Tag", "ok", tag["href"], 4)
    return _result("Canonical-Tag", "warn", "Kein Canonical-Tag gesetzt.", 4)


def check_lang(soup):
    html = soup.find("html")
    lang = html.get("lang") if html else None
    if lang:
        return _result("Sprachangabe", "ok", f'lang="{lang}"', 3)
    return _result("Sprachangabe", "warn", "Kein lang-Attribut im <html>-Tag.", 3)


def check_indexable(soup, resp):
    robots = soup.find("meta", attrs={"name": lambda v: v and v.lower() == "robots"})
    content = (robots.get("content") or "").lower() if robots else ""
    header = resp.headers.get("X-Robots-Tag", "").lower()
    if "noindex" in content or "noindex" in header:
        return _result("Indexierbarkeit", "fail",
                       "Seite ist auf „noindex“ gesetzt – Google indexiert sie nicht.", 10)
    return _result("Indexierbarkeit", "ok", "Seite darf indexiert werden.", 10)


def check_open_graph(soup):
    needed = ["og:title", "og:description", "og:image"]
    found = {m.get("property") for m in soup.find_all("meta", property=True)}
    missing = [p for p in needed if p not in found]
    if not missing:
        return _result("Social-Media (Open Graph)", "ok", "OG-Tags vollständig.", 3)
    return _result("Social-Media (Open Graph)", "warn", "Fehlt: " + ", ".join(missing), 3)


def check_content_length(soup):
    for t in soup(["script", "style", "noscript"]):
        t.decompose()
    words = len(soup.get_text(" ", strip=True).split())
    if words >= 300:
        return _result("Textmenge", "ok", f"ca. {words} Wörter", 5)
    return _result("Textmenge", "warn",
                   f"Nur ca. {words} Wörter – mehr Inhalt hilft beim Ranking.", 5)


def check_speed(elapsed, size):
    kb = size / 1024
    msg = f"Antwortzeit {elapsed:.2f}s, HTML {kb:.0f} KB"
    if elapsed < 1.0 and kb < 500:
        return _result("Ladezeit (Server)", "ok", msg, 7)
    if elapsed < 2.5:
        return _result("Ladezeit (Server)", "warn", msg + " – könnte schneller sein.", 7)
    return _result("Ladezeit (Server)", "fail", msg + " – deutlich zu langsam.", 7)


def check_file_exists(base_url, path, label, weight):
    url = urljoin(base_url, path)
    try:
        resp, _, _ = safe_get(url, max_bytes=16 * 1024)
        if resp.status_code == 200:
            return _result(label, "ok", f"{path} gefunden.", weight)
    except (requests.RequestException, CheckError):
        pass
    return _result(label, "warn", f"{path} nicht gefunden.", weight)


# ---------------------------------------------------------------------------
# Hauptfunktion
# ---------------------------------------------------------------------------

def run_seo_check(raw_url: str) -> dict:
    url = normalize_url(raw_url)
    start = time.monotonic()
    try:
        resp, body, final_url = safe_get(url)
    except requests.RequestException:
        raise CheckError("Die Seite konnte nicht geladen werden.")
    elapsed = time.monotonic() - start

    if resp.status_code >= 400:
        raise CheckError(f"Die Seite antwortet mit Status {resp.status_code}.")
    if "html" not in resp.headers.get("Content-Type", "html").lower():
        raise CheckError("Unter dieser URL liegt keine HTML-Seite.")

    soup = BeautifulSoup(body, "html.parser")
    root = f"{urlparse(final_url).scheme}://{urlparse(final_url).netloc}/"

    checks = [
        check_https(final_url),
        check_indexable(soup, resp),
        check_title(soup),
        check_meta_description(soup),
        check_h1(soup),
        check_headings(soup),
        check_images_alt(soup),
        check_viewport(soup),
        check_canonical(soup),
        check_lang(soup),
        check_open_graph(soup),
        check_speed(elapsed, len(body)),
        check_file_exists(root, "/robots.txt", "robots.txt", 2),
        check_file_exists(root, "/sitemap.xml", "Sitemap", 3),
        check_content_length(soup),  # zuletzt, da es soup verändert
    ]

    points = {"ok": 1.0, "warn": 0.5, "fail": 0.0}
    total = sum(c["weight"] for c in checks)
    reached = sum(c["weight"] * points[c["status"]] for c in checks)
    score = round(100 * reached / total)

    return {"url": final_url, "score": score, "checks": checks}


if __name__ == "__main__":
    import json
    import sys

    print(json.dumps(run_seo_check(sys.argv[1]), indent=2, ensure_ascii=False))
