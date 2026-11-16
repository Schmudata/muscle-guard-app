# Fala Já – Prototyp (Phase 1)

Live-Sprachtutor für brasilianisches Portugiesisch: Du sprichst, die Tutorin
(Ana aus Rio oder Bruno aus São Paulo) antwortet sofort mit echt klingender
Stimme, korrigiert dich nebenbei und lässt sich jederzeit unterbrechen.

## So funktioniert's

```
Chrome-Spracherkennung ─► WebSocket ─► Claude (Streaming) ─► Satz-Zerteiler
                                                               │
           Lautsprecher ◄── Wiedergabe in Reihenfolge ◄── ElevenLabs (pro Satz, parallel)
```

- **Schnell:** Der erste Satz wird schon vorgelesen, während Claude noch weiterschreibt.
  Die Zeit bis zum ersten Ton steht unten links (⚡ ms).
- **Unterbrechen:** Sprichst du dazwischen (oder drückst Esc / „Stopp“), bricht
  die laufende Antwort sofort ab – im Browser und auf dem Server.
- **Korrekturen:** Fehler erscheinen als gelbe Karte ✏️ und werden nicht vorgelesen.
- **Modi:** *Gespräch* (alles auf Portugiesisch) und *Frag mich* (du fragst auf Deutsch).
- Niveau A1–B2, Langsam-Modus, Untertitel ausblenden (Antippen zeigt sie).

## Starten

Voraussetzung: Python 3.10+, **Chrome oder Edge** (für die Spracherkennung).

```bash
cd fala-ja
pip install -r requirements.txt
cp .env.example .env      # dann .env öffnen und Schlüssel eintragen
uvicorn server:app --port 8000
```

Beim Start zeigt das Terminal, ob die ElevenLabs-Stimme aktiv ist.

Dann <http://localhost:8000> öffnen und aufs Mikrofon tippen.

Ohne API-Schlüssel ausprobieren (feste Testantwort): `FALA_FAKE_LLM=1 uvicorn server:app`

### Eine brasilianische Stimme wählen

In ElevenLabs → *Voice Library* nach „Portuguese“ + Akzent „Brazilian“ filtern,
Stimme zu „My Voices“ hinzufügen, die *Voice ID* kopieren und als
`ELEVENLABS_VOICE_ID_CARIOCA` bzw. `ELEVENLABS_VOICE_ID_PAULISTA` setzen.
Ohne eigene ID nutzt die App eine Standardstimme (spricht Portugiesisch, aber
nicht unbedingt mit brasilianischem Akzent).

## Tipps

- **Kopfhörer** verwenden: Dann hört das Mikrofon die Tutorin nicht selbst.
  Ohne Kopfhörer filtert die App Echos heraus; klappt das nicht, Häkchen bei
  „Unterbrechen per Stimme“ entfernen und mit Esc/„Stopp“ unterbrechen.
- Chromes Spracherkennung schickt Audio an Google. Für den Prototyp ok, für
  die echte App kommt eine eigene Spracherkennung (Phase 2, z. B. Deepgram).

## Tests

```bash
pytest -q tests
```

## Einstellungen (Umgebungsvariablen)

| Variable | Standard | Zweck |
|---|---|---|
| `FALA_MODEL` | `claude-opus-5-5` | Claude-Modell |
| `FALA_EFFORT` | `low` | Denkaufwand – `low` antwortet am schnellsten |
| `ELEVENLABS_MODEL` | `eleven_flash_v2_5` | schnellstes ElevenLabs-Modell |
| `FALA_FAKE_LLM` | – | `1` = Testmodus ohne Claude |

## Nächste Schritte (laut Bauplan)

1. Eigene Spracherkennung mit früherer Satzende-Erkennung (spart ~0,5 s).
2. ElevenLabs-WebSocket-Streaming statt Satz-für-Satz (erster Ton noch früher).
3. Mobile App (Expo), Login, Vokabeltrainer, Aussprache-Bewertung.
