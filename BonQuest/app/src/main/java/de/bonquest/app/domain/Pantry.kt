package de.bonquest.app.domain

data class PantryEntry(
    val receiptId: String,
    val store: String,
    val item: PurchaseItem,
    val purchaseDay: Long,
    val shelfDays: Int,
    val daysLeft: Int,
) {
    val daysAgo: Int get() = (shelfDays - daysLeft)
    /** 1.0 = just bought, 0.0 = probably used up / expired. */
    val freshness: Float get() = if (shelfDays <= 0) 0f else (daysLeft.toFloat() / shelfDays).coerceIn(0f, 1f)
    val isExpiringSoon: Boolean get() = daysLeft <= 2
    val label: String get() = Categorizer.ingredient(item.ingredient)?.label ?: item.name
}

data class AtHomeHit(val query: String, val entries: List<PantryEntry>)

/**
 * Estimates which purchased products are probably still at home, based on the purchase
 * date and a typical shelf life / consumption time per product type.
 */
object Pantry {

    fun shelfDays(item: PurchaseItem): Int =
        Categorizer.ingredient(item.ingredient)?.shelfDays ?: item.category.defaultShelfDays

    fun entries(receipts: List<Receipt>, today: Long): List<PantryEntry> =
        receipts.flatMap { r ->
            r.items
                .filter { it.status == ItemStatus.ACTIVE && it.category != Category.PFAND && it.price > 0 }
                .map { item ->
                    val shelf = shelfDays(item)
                    PantryEntry(
                        receiptId = r.id,
                        store = r.store,
                        item = item,
                        purchaseDay = r.epochDay,
                        shelfDays = shelf,
                        daysLeft = (r.epochDay + shelf - today).toInt(),
                    )
                }
        }
            .filter { it.daysLeft >= -3 && it.purchaseDay <= today }
            .sortedBy { it.daysLeft }

    /** Shopping-list check: which of the planned products are probably still at home? */
    fun checkList(list: String, entries: List<PantryEntry>): List<AtHomeHit> =
        list.split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { q -> AtHomeHit(q, findAtHome(q, entries)) }

    fun findAtHome(query: String, entries: List<PantryEntry>): List<PantryEntry> {
        val cls = Categorizer.classify(query)
        val qn = Categorizer.normalize(query)
        return entries.filter { e ->
            e.daysLeft >= 0 && (
                (cls.ingredient != null && e.item.ingredient == cls.ingredient) ||
                    (qn.length >= 3 && Categorizer.normalize(e.item.name).contains(qn))
                )
        }
    }
}
