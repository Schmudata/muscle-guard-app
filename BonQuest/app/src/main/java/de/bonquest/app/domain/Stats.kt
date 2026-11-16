package de.bonquest.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class MonthSpend(val label: String, val amount: Double)
data class CategorySpend(val category: Category, val amount: Double, val share: Double)
data class ProductStat(val name: String, val count: Int, val spent: Double)
data class PriceChange(val name: String, val oldPrice: Double, val newPrice: Double) {
    val percent: Int get() = (((newPrice - oldPrice) / oldPrice) * 100).roundToInt()
}
data class StoreSpend(val store: String, val amount: Double, val trips: Int)
data class StorePriceDiff(val product: String, val cheapStore: String, val cheapPrice: Double, val expensiveStore: String, val expensivePrice: Double)
data class DoublePurchase(val label: String, val day: Long, val earlierDay: Long)

data class Stats(
    val totalSpent: Double,
    val totalSaved: Double,
    val receiptCount: Int,
    val itemCount: Int,
    val promoItemCount: Int,
    val avgReceipt: Double,
    val months: List<MonthSpend>,
    val categories: List<CategorySpend>,
    val stores: List<StoreSpend>,
    val topProducts: List<ProductStat>,
    val priceIncreases: List<PriceChange>,
    val storeDiffs: List<StorePriceDiff>,
    val doublePurchases: List<DoublePurchase>,
    val pfandPaid: Double,
    val pfandReturned: Double,
    val wasted: Int,
    val rescued: Int,
    val thisWeekSpent: Double,
    val tripsPerWeek: Double,
)

object StatsCalculator {

    private val monthNames = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun compute(data: AppData, today: LocalDate = LocalDate.now()): Stats {
        val receipts = data.receipts
        val items = receipts.flatMap { it.items }
        val goods = items.filter { it.category != Category.PFAND }
        val totalSpent = receipts.sumOf { it.total }
        val totalSaved = receipts.sumOf { it.savings }

        val months = (5 downTo 0).map { back ->
            val m = today.minusMonths(back.toLong())
            val amount = receipts.filter {
                val d = LocalDate.ofEpochDay(it.epochDay); d.year == m.year && d.month == m.month
            }.sumOf { it.total }
            MonthSpend(monthNames[m.monthValue - 1], amount)
        }

        val goodsTotal = goods.sumOf { it.effectivePrice }.coerceAtLeast(0.01)
        val categories = goods.groupBy { it.category }
            .map { (c, list) -> val a = list.sumOf { it.effectivePrice }; CategorySpend(c, a, a / goodsTotal) }
            .sortedByDescending { it.amount }

        val stores = receipts.groupBy { it.store }
            .map { (s, list) -> StoreSpend(s, list.sumOf { it.total }, list.size) }
            .sortedByDescending { it.amount }

        val topProducts = goods.groupBy { Categorizer.normalize(it.name) }
            .map { (_, list) -> ProductStat(list.first().name, list.size, list.sumOf { it.effectivePrice }) }
            .sortedWith(compareByDescending<ProductStat> { it.count }.thenByDescending { it.spent })
            .take(8)

        // price development of identical products (by name), using undiscounted unit price
        val byName = receipts.sortedBy { it.epochDay }
            .flatMap { r -> r.items.filter { it.category != Category.PFAND && it.unit == "Stk" && it.price > 0 }.map { r.epochDay to it } }
            .groupBy { Categorizer.normalize(it.second.name) }
        val priceIncreases = byName.values.mapNotNull { list ->
            if (list.size < 2) return@mapNotNull null
            val first = list.first().second.unitPrice
            val last = list.last().second.unitPrice
            if (first > 0 && last > first * 1.04 && last - first >= 0.05) PriceChange(list.last().second.name, first, last) else null
        }.sortedByDescending { it.percent }

        // same ingredient, different stores
        val storeDiffs = receipts.flatMap { r -> r.items.filter { it.ingredient != null && it.unit == "Stk" && it.price > 0 }.map { r.store to it } }
            .groupBy { it.second.ingredient!! }
            .mapNotNull { (key, list) ->
                val perStore = list.groupBy { it.first }.mapValues { (_, l) -> l.map { it.second.unitPrice }.average() }
                if (perStore.size < 2) return@mapNotNull null
                val cheap = perStore.minByOrNull { it.value }!!
                val exp = perStore.maxByOrNull { it.value }!!
                if (exp.value - cheap.value >= 0.15) StorePriceDiff(Recipes.labelOf(key), cheap.key, cheap.value, exp.key, exp.value) else null
            }
            .sortedByDescending { it.expensivePrice - it.cheapPrice }

        val pfandPaid = items.filter { it.category == Category.PFAND && it.price > 0 }.sumOf { it.price }
        val pfandReturned = -items.filter { it.category == Category.PFAND && it.price < 0 }.sumOf { it.price }

        val ws = weekStart(today).toEpochDay()
        val thisWeekSpent = receipts.filter { it.epochDay >= ws && it.epochDay <= today.toEpochDay() }.sumOf { it.total }

        val firstDay = receipts.minOfOrNull { it.epochDay }
        val weeks = if (firstDay == null) 1.0 else ((today.toEpochDay() - firstDay + 1) / 7.0).coerceAtLeast(1.0)

        return Stats(
            totalSpent = totalSpent,
            totalSaved = totalSaved,
            receiptCount = receipts.size,
            itemCount = goods.size,
            promoItemCount = goods.count { it.isPromo },
            avgReceipt = if (receipts.isEmpty()) 0.0 else totalSpent / receipts.size,
            months = months,
            categories = categories,
            stores = stores,
            topProducts = topProducts,
            priceIncreases = priceIncreases,
            storeDiffs = storeDiffs,
            doublePurchases = doublePurchases(receipts),
            pfandPaid = pfandPaid,
            pfandReturned = pfandReturned,
            wasted = items.count { it.status == ItemStatus.WASTED },
            rescued = data.game.rescueLog.size,
            thisWeekSpent = thisWeekSpent,
            tripsPerWeek = receipts.size / weeks,
        )
    }

    /** Products bought again while an earlier purchase was most likely still at home. */
    fun doublePurchases(receipts: List<Receipt>): List<DoublePurchase> {
        val sorted = receipts.sortedBy { it.epochDay }
        val result = mutableListOf<DoublePurchase>()
        sorted.forEachIndexed { i, r ->
            for (item in r.items) {
                val key = item.ingredient ?: continue
                val shelf = Pantry.shelfDays(item)
                if (shelf > 60) continue // long-life goods are stocked on purpose
                val earlier = sorted.subList(0, i).lastOrNull { e ->
                    e.epochDay < r.epochDay && e.items.any { o ->
                        o.ingredient == key && e.epochDay + Pantry.shelfDays(o) > r.epochDay &&
                            (o.status == ItemStatus.ACTIVE || (o.statusDay ?: Long.MAX_VALUE) > r.epochDay)
                    }
                }
                if (earlier != null) result += DoublePurchase(Recipes.labelOf(key), r.epochDay, earlier.epochDay)
            }
        }
        return result
    }
}
