package de.bonquest.app.domain

import java.util.Locale
import kotlin.math.roundToInt

data class Tip(val emoji: String, val title: String, val text: String, val priority: Int)

fun euro(v: Double): String = String.format(Locale.GERMANY, "%.2f €", v)

object TipEngine {

    fun tips(stats: Stats, budget: Double): List<Tip> {
        val tips = mutableListOf<Tip>()
        if (stats.receiptCount == 0) {
            return listOf(
                Tip("📸", "Scanne deinen ersten Bon", "Je mehr Bons du scannst, desto schlauer werden deine Spar-Tipps. Dein erster Scan bringt Bonus-XP!", 100),
                Tip("📝", "Mit Einkaufszettel losziehen", "Wer mit Liste einkauft, gibt im Schnitt deutlich weniger für Spontankäufe aus.", 10),
            )
        }
        val share = stats.categories.associate { it.category to it.share }
        val snacks = (share[Category.SNACKS] ?: 0.0) + (share[Category.ALKOHOL] ?: 0.0)
        if (snacks > 0.18) {
            val amount = stats.categories.filter { it.category == Category.SNACKS || it.category == Category.ALKOHOL }.sumOf { it.amount }
            tips += Tip("🍫", "Snack-Monster gesichtet!", "${(snacks * 100).roundToInt()} % deiner Ausgaben (${euro(amount)}) gehen in Süßes, Snacks & Alkohol. Halbiere das und du sparst ca. ${euro(amount / 2)}.", 90)
        }
        val drinks = share[Category.GETRAENKE] ?: 0.0
        if (drinks > 0.12) {
            tips += Tip("🚰", "Getränke-Falle", "${(drinks * 100).roundToInt()} % gehen für Getränke drauf. Leitungswasser (mit Sprudler) ist bis zu 100× günstiger als Flaschenwasser.", 70)
        }
        val meat = share[Category.FLEISCH_FISCH] ?: 0.0
        if (meat > 0.25) {
            tips += Tip("🥕", "Veggie-Tage einlegen", "Fleisch & Fisch machen ${(meat * 100).roundToInt()} % aus. Zwei fleischfreie Tage pro Woche sparen spürbar – Linsen & Bohnen sind günstige Proteinquellen.", 60)
        }
        if (stats.tripsPerWeek > 3.0) {
            tips += Tip("🧭", "Weniger Einkaufstouren", "Du gehst ca. ${String.format(Locale.GERMANY, "%.1f", stats.tripsPerWeek)}× pro Woche einkaufen. Jeder Ladenbesuch verführt zu Spontankäufen – plane 1–2 Großeinkäufe.", 75)
        }
        stats.storeDiffs.firstOrNull()?.let { d ->
            tips += Tip("🏪", "${d.product}: Laden wechseln", "${d.product} kostet bei ${d.cheapStore} im Schnitt ${euro(d.cheapPrice)}, bei ${d.expensiveStore} ${euro(d.expensivePrice)}.", 80)
        }
        stats.priceIncreases.firstOrNull()?.let { p ->
            tips += Tip("📈", "Preisalarm: ${p.name}", "Der Preis ist von ${euro(p.oldPrice)} auf ${euro(p.newPrice)} gestiegen (+${p.percent} %). Achte auf Angebote oder Eigenmarken.", 78)
        }
        if (stats.itemCount >= 10) {
            val promoShare = stats.promoItemCount.toDouble() / stats.itemCount
            if (promoShare < 0.1) {
                tips += Tip("🏷️", "Aktionen nutzen", "Nur ${(promoShare * 100).roundToInt()} % deiner Produkte waren im Angebot. Ein Blick in Prospekte oder die Händler-App vor dem Einkauf lohnt sich.", 65)
            } else {
                tips += Tip("🏆", "Angebots-Jäger", "Stark! ${(promoShare * 100).roundToInt()} % deiner Produkte waren Aktionen – du hast bereits ${euro(stats.totalSaved)} gespart.", 30)
            }
        }
        if (stats.doublePurchases.isNotEmpty()) {
            val names = stats.doublePurchases.map { it.label }.distinct().take(3).joinToString(", ")
            tips += Tip("🔁", "Doppelt gekauft?", "Du hast ${stats.doublePurchases.size}× Produkte gekauft, die vermutlich noch zu Hause waren (z. B. $names). Nutze den Einkaufs-Check vor dem Einkauf!", 85)
        }
        if (stats.wasted > 0) {
            tips += Tip("🗑️", "Weniger wegwerfen", "${stats.wasted} Produkte sind im Müll gelandet. Schau öfter in den Vorrat und koche Rezepte mit bald ablaufenden Zutaten.", 82)
        }
        if (stats.pfandPaid - stats.pfandReturned > 3.0) {
            tips += Tip("♻️", "Pfand liegt rum", "Du hast ${euro(stats.pfandPaid - stats.pfandReturned)} mehr Pfand bezahlt als zurückgegeben. Bring die Flaschen zurück – das ist geschenktes Geld!", 72)
        }
        if (stats.thisWeekSpent > budget) {
            tips += Tip("💔", "Budget überschritten", "Diese Woche hast du ${euro(stats.thisWeekSpent)} ausgegeben – ${euro(stats.thisWeekSpent - budget)} über deinem Budget. Koch aus dem Vorrat, um den Rest der Woche zu überstehen.", 95)
        }
        val smallTrips = stats.receiptCount > 3 && stats.avgReceipt < 12
        if (smallTrips) {
            tips += Tip("🛒", "Viele Mini-Einkäufe", "Dein Durchschnittsbon liegt bei ${euro(stats.avgReceipt)}. Kleine Einkäufe sind oft Impulskäufe – bündeln spart Zeit und Geld.", 55)
        }
        tips += Tip("🥫", "Eigenmarken testen", "Eigenmarken (z. B. ja!, Milbona, GUT&GÜNSTIG) sind oft 30–50 % günstiger und kommen häufig aus denselben Werken.", 5)
        return tips.sortedByDescending { it.priority }
    }
}
