package de.bonquest.app.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val label: String, val emoji: String, val color: Long, val defaultShelfDays: Int) {
    OBST_GEMUESE("Fruit & Veg", "🥦", 0xFF3DDC84, 7),
    MILCH("Dairy & Eggs", "🧀", 0xFFFFE08A, 10),
    FLEISCH_FISCH("Meat & Fish", "🍗", 0xFFFF6B6B, 3),
    BACKWAREN("Bakery", "🥖", 0xFFE0A15A, 4),
    VORRAT("Pantry Staples", "🥫", 0xFFB48CFF, 270),
    TIEFKUEHL("Frozen", "🧊", 0xFF6EC8FF, 120),
    SNACKS("Sweets & Snacks", "🍫", 0xFFFF8FD8, 90),
    GETRAENKE("Drinks", "🥤", 0xFF4FD1C5, 45),
    ALKOHOL("Alcohol", "🍺", 0xFFFFB347, 180),
    HAUSHALT("Household", "🧴", 0xFF9AA5B1, 90),
    PFAND("Deposit", "♻️", 0xFF7BD389, 0),
    SONSTIGES("Other", "📦", 0xFFC0C0C0, 21);
}

@Serializable
enum class ItemStatus { ACTIVE, CONSUMED, WASTED }

@Serializable
data class PurchaseItem(
    val id: String,
    val name: String,
    /** Line price before discounts (quantity already included). Negative for Pfand returns. */
    val price: Double,
    val quantity: Double = 1.0,
    val unit: String = "Stk",
    /** Amount saved through promotions (positive). */
    val discount: Double = 0.0,
    val promoLabel: String? = null,
    val category: Category = Category.SONSTIGES,
    val ingredient: String? = null,
    val status: ItemStatus = ItemStatus.ACTIVE,
    val statusDay: Long? = null,
) {
    val effectivePrice: Double get() = price - discount
    val unitPrice: Double get() = if (quantity > 0 && unit == "Stk") price / quantity else price
    val isPromo: Boolean get() = discount > 0.004 || promoLabel != null
}

@Serializable
data class Receipt(
    val id: String,
    val store: String,
    /** Purchase date as LocalDate.toEpochDay(). */
    val epochDay: Long,
    val items: List<PurchaseItem>,
    /** Total printed on the receipt, if it could be read. */
    val printedTotal: Double? = null,
    /** Discounts that could not be assigned to a single item. */
    val extraDiscount: Double = 0.0,
    val scannedDay: Long = epochDay,
) {
    val total: Double get() = items.sumOf { it.effectivePrice } - extraDiscount
    val savings: Double get() = items.sumOf { it.discount } + extraDiscount
}

@Serializable
data class GameState(
    val xp: Int = 0,
    val coins: Int = 0,
    val unlocked: Set<String> = emptySet(),
    val cookLog: List<Long> = emptyList(),
    val rescueLog: List<Long> = emptyList(),
    val wasteLog: List<Long> = emptyList(),
    /** questId -> epochDay of the week start it was claimed for. */
    val claimedQuests: Map<String, Long> = emptyMap(),
    val weeklyBudget: Double = 80.0,
    val avatar: String = "knappe",
    val ownedAvatars: Set<String> = setOf("knappe"),
    val heroName: String = "Hero",
)

@Serializable
data class AppData(
    val receipts: List<Receipt> = emptyList(),
    val game: GameState = GameState(),
    /** Categories the user corrected manually: normalized product name -> category. */
    val learnedCategories: Map<String, Category> = emptyMap(),
)
