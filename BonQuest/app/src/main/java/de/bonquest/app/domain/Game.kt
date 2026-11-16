package de.bonquest.app.domain

import java.time.LocalDate

object Levels {
    private val titles = listOf(
        "Einkaufs-Lehrling", "Korb-Knappe", "Bon-Sammler", "Angebots-Späher", "Spar-Ritter",
        "Rabatt-Ranger", "Vorrats-Magier", "Budget-Paladin", "Preis-Ninja", "Coupon-König",
        "Schnäppchen-Drache", "Spar-Legende",
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
        Achievement("first_scan", "📜", "Erste Beute", "Scanne deinen ersten Bon", 30, 10) { it.receipts >= 1 },
        Achievement("scan_5", "📚", "Sammler", "Scanne 5 Bons", 60, 20) { it.receipts >= 5 },
        Achievement("scan_25", "🏛️", "Archivar", "Scanne 25 Bons", 150, 60) { it.receipts >= 25 },
        Achievement("items_100", "🛒", "Großeinkäufer", "Erfasse 100 Produkte", 100, 40) { it.items >= 100 },
        Achievement("first_promo", "🏷️", "Schnäppchenjäger", "Kaufe dein erstes Aktionsprodukt", 30, 10) { it.promoItems >= 1 },
        Achievement("promo_20", "🎯", "Angebots-Sniper", "Kaufe 20 Aktionsprodukte", 100, 40) { it.promoItems >= 20 },
        Achievement("save_10", "💰", "Sparschwein", "Spare insgesamt 10 € durch Aktionen", 60, 25) { it.totalSaved >= 10 },
        Achievement("save_50", "💎", "Schatzhüter", "Spare insgesamt 50 € durch Aktionen", 200, 80) { it.totalSaved >= 50 },
        Achievement("first_cook", "👩‍🍳", "Küchen-Novize", "Koche dein erstes Rezept", 40, 15) { it.cooked >= 1 },
        Achievement("cook_10", "🍲", "Meisterkoch", "Koche 10 Rezepte", 150, 60) { it.cooked >= 10 },
        Achievement("rescue_10", "🦸", "Lebensmittelretter", "Verbrauche 10 Produkte vor Ablauf", 100, 40) { it.rescued >= 10 },
        Achievement("streak_3", "🔥", "Dranbleiber", "Scanne 3 Wochen in Folge", 120, 50) { it.streakWeeks >= 3 },
        Achievement("explorer", "🧭", "Entdecker", "Kaufe Produkte aus 8 Kategorien", 80, 30) { it.categoriesSeen >= 8 },
        Achievement("pfand", "♻️", "Pfandjäger", "Gib Pfand im Wert von 5 € zurück", 60, 25) { it.pfandReturned >= 5 },
        Achievement("stores_3", "🗺️", "Weltenbummler", "Kaufe in 3 verschiedenen Läden ein", 60, 25) { it.stores >= 3 },
        Achievement("level_5", "⚔️", "Ritterschlag", "Erreiche Level 5", 0, 50) { it.level >= 5 },
        Achievement("level_10", "👑", "Königswürde", "Erreiche Level 10", 0, 150) { it.level >= 10 },
        Achievement("fashion", "🎭", "Verwandlungskünstler", "Besitze 3 Helden-Avatare", 50, 0) { it.avatars >= 3 },
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
        Quest("w_scan", "📸", "Scanne 2 Bons", 2.0, 60, 20) { it.scans.toDouble() },
        Quest("w_items", "🧺", "Erfasse 15 Produkte", 15.0, 50, 15) { it.items.toDouble() },
        Quest("w_cook", "🍳", "Koche 2 Rezepte aus dem Vorrat", 2.0, 80, 30) { it.cooked.toDouble() },
        Quest("w_rescue", "🛟", "Rette 3 Lebensmittel vor dem Ablauf", 3.0, 70, 25) { it.rescued.toDouble() },
        Quest("w_save", "💸", "Spare 3 € durch Aktionen", 3.0, 70, 25) { it.saved },
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
        Avatar("knappe", "🧑‍🌾", "Knappe", 0),
        Avatar("ritter", "🤺", "Ritter", 60),
        Avatar("magier", "🧙", "Magier", 120),
        Avatar("ninja", "🥷", "Preis-Ninja", 200),
        Avatar("elfe", "🧝", "Waldelfe", 250),
        Avatar("held", "🦸", "Superheld", 350),
        Avatar("robo", "🤖", "Spar-Bot", 450),
        Avatar("drache", "🐉", "Schnäppchen-Drache", 700),
        Avatar("koenig", "👑", "Coupon-König", 1000),
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
            stores = data.receipts.map { it.store }.filter { it != "Unbekannter Laden" }.distinct().size,
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
