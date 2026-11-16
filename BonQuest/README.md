# 🧾 BonQuest – Einkaufszettel-Scanner im Spielformat (Android)

Bons scannen, Produkte erkennen und kategorisieren, Aktionen finden, Vorrat im Blick behalten,
Rezepte aus den Einkäufen bekommen – und dabei XP, Münzen und Erfolge sammeln.

## Funktionen
- **Bon-Scan** per Kamera oder Galerie, Texterkennung offline mit Google ML Kit. Dazu ein Demo-Bon und manuelle Eingabe.
- **Produkterkennung**: Preise, Mengen (`2 Stk x 1,29`), Gewicht (`0,850 kg x …`), Pfand/Leergut, Laden, Datum und Summe (mit Abgleich).
- **Kategorien**: 12 Kategorien. Korrigierst du eine Kategorie, merkt sich die App das für künftige Scans.
- **Aktionen**: Rabatt- und Coupon-Zeilen, Preisvorteile und negative Beträge werden dem Produkt zugeordnet; „Angebot“/„Aktion“ wird markiert.
- **Vorrat**: geschätzte Haltbarkeit je Produkt, Hinweis „bald verbrauchen“, Buttons *Verbraucht* und *Weg*.
- **Einkaufs-Check**: Einkaufszettel eintippen, die App zeigt, was vermutlich noch zu Hause ist. Beim Scan: Hinweis „🏠 hattest du noch“.
- **Rezepte**: 29 Rezepte, sortiert nach vorhandenen Zutaten, bald ablaufende zuerst.
- **Statistik und Tipps**: Monate, Kategorien, Läden, Top-Produkte, Preisalarm, Preisvergleich zwischen Läden, Doppelkäufe, Pfand und Budget.
- **Spiel**: Level und Titel, XP-Leiste, Wochenbudget als Lebensleiste, Wochen-Quests, 18 Erfolge, Avatar-Shop mit Münzen, Konfetti beim Level-Up.

## Bauen
- **Ohne Android Studio**: Bei jedem Push baut GitHub Actions die APK → *Actions* → letzter Lauf → Artefakt `BonQuest-debug-apk`.
- **Android Studio**: den Ordner `BonQuest/` öffnen und starten.
- **Kommandozeile**: `./gradlew assembleDebug` (Tests: `./gradlew testDebugUnitTest`).

Alle Daten bleiben lokal auf dem Gerät (`bonquest.json` im App-Speicher).
