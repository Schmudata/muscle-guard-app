package de.bonquest.app.domain

import java.util.Locale
import kotlin.math.roundToInt

data class Tip(val emoji: String, val title: String, val text: String, val priority: Int)

private val moneyLocale = Locale("pt", "PT")

/** Formats an amount the way Portuguese receipts show it: "1,29 €". */
fun euro(v: Double): String = String.format(moneyLocale, "%.2f €", v)

private fun pct(v: Double) = "${(v * 100).roundToInt()}%"

object TipEngine {

    fun tips(stats: Stats, budget: Double): List<Tip> {
        val tips = mutableListOf<Tip>()
        if (stats.receiptCount == 0) {
            return listOf(
                Tip("📸", "Scan your first receipt", "The more receipts you scan, the smarter your saving tips get. Your first scan earns bonus XP!", 100),
                Tip("📝", "Shop with a list", "People who shop with a list spend noticeably less on impulse buys.", 10),
            )
        }
        val share = stats.categories.associate { it.category to it.share }
        val snacks = (share[Category.SNACKS] ?: 0.0) + (share[Category.ALKOHOL] ?: 0.0)
        if (snacks > 0.18) {
            val amount = stats.categories.filter { it.category == Category.SNACKS || it.category == Category.ALKOHOL }.sumOf { it.amount }
            tips += Tip("🍫", "Snack monster spotted!", "${pct(snacks)} of your spending (${euro(amount)}) goes on sweets, snacks & alcohol. Halve it and you save about ${euro(amount / 2)}.", 90)
        }
        val drinks = share[Category.GETRAENKE] ?: 0.0
        if (drinks > 0.12) {
            tips += Tip("🚰", "Drinks trap", "${pct(drinks)} goes on drinks. Tap water (filtered or sparkling at home) is up to 100× cheaper than bottled water.", 70)
        }
        val meat = share[Category.FLEISCH_FISCH] ?: 0.0
        if (meat > 0.25) {
            tips += Tip("🥕", "Add veggie days", "Meat & fish make up ${pct(meat)}. Two meat-free days a week save real money – lentils, chickpeas and beans are cheap protein.", 60)
        }
        if (stats.tripsPerWeek > 3.0) {
            tips += Tip("🧭", "Fewer shopping trips", "You shop about ${String.format(Locale.US, "%.1f", stats.tripsPerWeek)}× a week. Every visit tempts you into impulse buys – plan 1–2 bigger shops.", 75)
        }
        stats.storeDiffs.firstOrNull()?.let { d ->
            tips += Tip("🏪", "${d.product}: switch store", "${d.product} costs on average ${euro(d.cheapPrice)} at ${d.cheapStore}, but ${euro(d.expensivePrice)} at ${d.expensiveStore}.", 80)
        }
        stats.priceIncreases.firstOrNull()?.let { p ->
            tips += Tip("📈", "Price alert: ${p.name}", "The price went up from ${euro(p.oldPrice)} to ${euro(p.newPrice)} (+${p.percent}%). Look for promotions or the store's own brand.", 78)
        }
        if (stats.itemCount >= 10) {
            val promoShare = stats.promoItemCount.toDouble() / stats.itemCount
            if (promoShare < 0.1) {
                tips += Tip("🏷️", "Use promotions", "Only ${pct(promoShare)} of your products were on promotion. Check the weekly leaflet or the store app (e.g. Cartão Continente, Poupa Mais, Lidl Plus) before you shop.", 65)
            } else {
                tips += Tip("🏆", "Deal hunter", "Great! ${pct(promoShare)} of your products were on promotion – you've already saved ${euro(stats.totalSaved)}.", 30)
            }
        }
        if (stats.doublePurchases.isNotEmpty()) {
            val names = stats.doublePurchases.map { it.label }.distinct().take(3).joinToString(", ")
            tips += Tip("🔁", "Bought twice?", "${stats.doublePurchases.size}× you bought something you probably still had at home (e.g. $names). Run the Shopping Check before you go!", 85)
        }
        if (stats.wasted > 0) {
            tips += Tip("🗑️", "Waste less", "${stats.wasted} products ended up in the bin. Check your pantry more often and cook recipes with items that expire soon.", 82)
        }
        if (stats.pfandPaid - stats.pfandReturned > 3.0) {
            tips += Tip("♻️", "Deposit lying around", "You paid ${euro(stats.pfandPaid - stats.pfandReturned)} more deposit than you got back. Return those bottles – it's free money!", 72)
        }
        if (stats.thisWeekSpent > budget) {
            tips += Tip("💔", "Budget exceeded", "This week you spent ${euro(stats.thisWeekSpent)} – ${euro(stats.thisWeekSpent - budget)} over budget. Cook from your pantry to survive the rest of the week.", 95)
        }
        if (stats.receiptCount > 3 && stats.avgReceipt < 12) {
            tips += Tip("🛒", "Lots of mini shops", "Your average receipt is ${euro(stats.avgReceipt)}. Small shops are often impulse buys – bundling saves time and money.", 55)
        }
        tips += Tip("🥫", "Try own brands", "Store brands (Continente, Pingo Doce, Milbona, Auchan …) are often 30–50% cheaper and frequently come from the same factories.", 5)
        return tips.sortedByDescending { it.priority }
    }
}
