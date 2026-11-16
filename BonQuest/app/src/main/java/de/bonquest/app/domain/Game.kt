package de.bonquest.app.domain

import java.time.LocalDate

object Levels {
    private val titles = listOf(
        "Shopping Apprentice", "Basket Squire", "Receipt Collector", "Deal Scout", "Savings Knight",
        "Discount Ranger", "Pantry Mage", "Budget Paladin", "Price Ninja", "Coupon King",
        "Bargain Dragon", "Savings Legend",
    )

    /** Total XP needed to reach [level] (level 1 = 0 XP). */
    fun xpForLevel(level: Int): Int = (1 until level).sumOf { 100 + (it - 1) * 50 }

    fun levelFor(xp: Int): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    fun title(level: Int): String = titles[(level - 1).coerceIn(0, titles.lastIndex)]

    /** Progress inside the current level, 0..1 */
    fun progress(xp: Int): Float {
        val l = levelFor(xp)
        val start = xpForLevel(l)
        val end = xpForLevel(l + 1)
        return ((xp - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }
}

data class GameContext(
    val receipts: Int,
    val items: Int,
    val totalSaved: Double,
    val promoItems: Int,
    val cooked: Int,
    val rescued: Int,
    val level: Int,
    val categoriesSeen: Int,
    val streakWeeks: Int,
    val pfandReturned: Double,
    val stores: Int,
    val avatars: Int,
)

data class Achievement(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val rewardXp: Int,
    val rewardCoins: Int,
    val check: (GameContext) -> Boolean,
)

object Achievements {
    val all = listOf(
        Achievement("first_scan", "📜", "First Loot", "Scan your first receipt", 30, 10) { it.receipts >= 1 },
        Achievement("scan_5", "📚", "Collector", "Scan 5 receipts", 60, 20) { it.receipts >= 5 },
        Achievement("scan_25", "🏛️", "Archivist", "Scan 25 receipts", 150, 60) { it.receipts >= 25 },
        Achievement("items_100", "🛒", "Bulk Buyer", "Log 100 products", 100, 40) { it.items >= 100 },
        Achievement("first_promo", "🏷️", "Bargain Hunter", "Buy your first promo product", 30, 10) { it.promoItems >= 1 },
        Achievement("promo_20", "🎯", "Deal Sniper", "Buy 20 promo products", 100, 40) { it.promoItems >= 20 },
        Achievement("save_10", "💰", "Piggy Bank", "Save €10 in total through promotions", 60, 25) { it.totalSaved >= 10 },
        Achievement("save_50", "💎", "Treasure Keeper", "Save €50 in total through promotions", 200, 80) { it.totalSaved >= 50 },
        Achievement("first_cook", "👩‍🍳", "Kitchen Novice", "Cook your first recipe", 40, 15) { it.cooked >= 1 },
        Achievement("cook_10", "🍲", "Master Chef", "Cook 10 recipes", 150, 60) { it.cooked >= 10 },
        Achievement("rescue_10", "🦸", "Food Rescuer", "Use up 10 products before they expire", 100, 40) { it.rescued >= 10 },
        Achievement("streak_3", "🔥", "On a Roll", "Scan receipts 3 weeks in a row", 120, 50) { it.streakWeeks >= 3 },
        Achievement("explorer", "🧭", "Explorer", "Buy products from 8 categories", 80, 30) { it.categoriesSeen >= 8 },
        Achievement("pfand", "♻️", "Deposit Hunter", "Return €5 worth of deposit", 60, 25) { it.pfandReturned >= 5 },
        Achievement("stores_3", "🗺️", "Globetrotter", "Shop at 3 different stores", 60, 25) { it.stores >= 3 },
        Achievement("level_5", "⚔️", "Knighted", "Reach level 5", 0, 50) { it.level >= 5 },
        Achievement("level_10", "👑", "Crowned", "Reach level 10", 0, 150) { it.level >= 10 },
        Achievement("fashion", "🎭", "Shape-shifter", "Own 3 hero avatars", 50, 0) { it.avatars >= 3 },
    )
}

data class WeekContext(
    val scans: Int, val items: Int, val cooked: Int, val rescued: Int, val saved: Double,
)

data class Quest(
    val id: String,
    val emoji: String,
    val title: String,
    val target: Double,
    val rewardXp: Int,
    val rewardCoins: Int,
    val progress: (WeekContext) -> Double,
)

object Quests {
    val weekly = listOf(
        Quest("w_scan", "📸", "Scan 2 receipts", 2.0, 60, 20) { it.scans.toDouble() },
        Quest("w_items", "🧺", "Log 15 products", 15.0, 50, 15) { it.items.toDouble() },
        Quest("w_cook", "🍳", "Cook 2 recipes from your pantry", 2.0, 80, 30) { it.cooked.toDouble() },
        Quest("w_rescue", "🛟", "Rescue 3 items before they expire", 3.0, 70, 25) { it.rescued.toDouble() },
        Quest("w_save", "💸", "Save €3 through promotions", 3.0, 70, 25) { it.saved },
    )

    fun weekContext(data: AppData, today: LocalDate): WeekContext {
        val ws = StatsCalculator.weekStart(today).toEpochDay()
        val week = data.receipts.filter { it.scannedDay >= ws }
        return WeekContext(
            scans = week.size,
            items = week.sumOf { r -> r.items.count { it.category != Category.PFAND } },
            cooked = data.game.cookLog.count { it >= ws },
            rescued = data.game.rescueLog.count { it >= ws },
            saved = week.sumOf { it.savings },
        )
    }

    fun isClaimed(data: AppData, quest: Quest, today: LocalDate): Boolean =
        data.game.claimedQuests[quest.id] == StatsCalculator.weekStart(today).toEpochDay()
}

data class Avatar(val id: String, val emoji: String, val name: String, val price: Int)

object Avatars {
    val all = listOf(
        Avatar("knappe", "🧑‍🌾", "Squire", 0),
        Avatar("ritter", "🤺", "Knight", 60),
        Avatar("magier", "🧙", "Wizard", 120),
        Avatar("ninja", "🥷", "Price Ninja", 200),
        Avatar("elfe", "🧝", "Forest Elf", 250),
        Avatar("held", "🦸", "Superhero", 350),
        Avatar("robo", "🤖", "Saver Bot", 450),
        Avatar("drache", "🐉", "Bargain Dragon", 700),
        Avatar("koenig", "👑", "Coupon King", 1000),
    )

    fun byId(id: String): Avatar = all.firstOrNull { it.id == id } ?: all.first()
}

data class AwardResult(val data: AppData, val unlocked: List<Achievement>, val newLevel: Int?)

object GameEngine {

    fun context(data: AppData): GameContext {
        val items = data.receipts.flatMap { it.items }
        val goods = items.filter { it.category != Category.PFAND }
        return GameContext(
            receipts = data.receipts.size,
            items = goods.size,
            totalSaved = data.receipts.sumOf { it.savings },
            promoItems = goods.count { it.isPromo },
            cooked = data.game.cookLog.size,
            rescued = data.game.rescueLog.size,
            level = Levels.levelFor(data.game.xp),
            categoriesSeen = goods.map { it.category }.distinct().size,
            streakWeeks = streakWeeks(data, LocalDate.now()),
            pfandReturned = -items.filter { it.category == Category.PFAND && it.price < 0 }.sumOf { it.price },
            stores = data.receipts.map { it.store }.filter { it != ReceiptParser.UNKNOWN_STORE }.distinct().size,
            avatars = data.game.ownedAvatars.size,
        )
    }

    /** Consecutive weeks (ending this or last week) with at least one scanned receipt. */
    fun streakWeeks(data: AppData, today: LocalDate): Int {
        val weeks = data.receipts.map { StatsCalculator.weekStart(LocalDate.ofEpochDay(it.scannedDay)).toEpochDay() }.toSet()
        var w = StatsCalculator.weekStart(today).toEpochDay()
        if (w !in weeks) w -= 7
        var streak = 0
        while (w in weeks) { streak++; w -= 7 }
        return streak
    }

    /** Adds XP/coins, then unlocks achievements (which may give more XP) and reports level ups. */
    fun award(data: AppData, xp: Int, coins: Int): AwardResult {
        val startLevel = Levels.levelFor(data.game.xp)
        var current = data.copy(game = data.game.copy(xp = data.game.xp + xp, coins = data.game.coins + coins))
        val unlocked = mutableListOf<Achievement>()
        while (true) {
            val ctx = context(current)
            val fresh = Achievements.all.filter { it.id !in current.game.unlocked && it.check(ctx) }
            if (fresh.isEmpty()) break
            unlocked += fresh
            current = current.copy(
                game = current.game.copy(
                    unlocked = current.game.unlocked + fresh.map { it.id },
                    xp = current.game.xp + fresh.sumOf { it.rewardXp },
                    coins = current.game.coins + fresh.sumOf { it.rewardCoins },
                )
            )
        }
        val endLevel = Levels.levelFor(current.game.xp)
        return AwardResult(current, unlocked, if (endLevel > startLevel) endLevel else null)
    }

    fun xpForReceipt(r: Receipt): Pair<Int, Int> {
        val goods = r.items.count { it.category != Category.PFAND }
        val promos = r.items.count { it.isPromo }
        val xp = 50 + goods * 2 + promos * 10
        val coins = 5 + (r.savings * 10).toInt()
        return xp to coins
    }
}
