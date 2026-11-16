package de.bonquest.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.bonquest.app.data.Repository
import de.bonquest.app.domain.Achievement
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Avatars
import de.bonquest.app.domain.Categorizer
import de.bonquest.app.domain.Category
import de.bonquest.app.domain.DemoData
import de.bonquest.app.domain.GameEngine
import de.bonquest.app.domain.ItemStatus
import de.bonquest.app.domain.PantryEntry
import de.bonquest.app.domain.PurchaseItem
import de.bonquest.app.domain.Quest
import de.bonquest.app.domain.Quests
import de.bonquest.app.domain.Receipt
import de.bonquest.app.domain.ReceiptParser
import de.bonquest.app.domain.RecipeSuggestion
import de.bonquest.app.domain.Pantry
import de.bonquest.app.ocr.OcrScanner
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class Screen { HOME, SCAN, REVIEW, PANTRY, CHECK, RECIPES, STATS, TROPHIES, HISTORY }

sealed class Popup {
    data class LevelUp(val level: Int) : Popup()
    data class Unlocked(val achievement: Achievement) : Popup()
}

data class Toast(val id: Long, val text: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val data: StateFlow<AppData> = repo.data

    var screen by mutableStateOf(Screen.HOME)
        private set
    private val backStack = ArrayDeque<Screen>()

    var draft by mutableStateOf<Receipt?>(null)
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanError by mutableStateOf<String?>(null)
    var lastRawRows by mutableStateOf<List<String>>(emptyList())
        private set

    val popups = mutableStateListOf<Popup>()
    val toasts = mutableStateListOf<Toast>()
    private var demoCounter = 0

    // ---------- navigation ----------
    fun navigate(to: Screen, asTab: Boolean = false) {
        if (to == screen) return
        if (asTab) backStack.clear() else backStack.addLast(screen)
        screen = to
    }

    fun back(): Boolean {
        val prev = backStack.removeLastOrNull() ?: return if (screen != Screen.HOME) { screen = Screen.HOME; true } else false
        screen = prev
        return true
    }

    // ---------- scanning ----------
    fun scanImage(uri: Uri) {
        scanning = true
        scanError = null
        viewModelScope.launch {
            try {
                val rows = OcrScanner.scan(getApplication(), uri)
                lastRawRows = rows
                if (rows.isEmpty()) {
                    scanError = "Kein Text erkannt. Achte auf gutes Licht und halte den Bon gerade."
                } else {
                    openDraft(rows)
                }
            } catch (e: Exception) {
                scanError = "Scan fehlgeschlagen: ${e.message ?: "unbekannter Fehler"}"
            } finally {
                scanning = false
            }
        }
    }

    fun loadDemo() {
        val rows = DemoData.demoRows(demoCounter++)
        lastRawRows = rows
        openDraft(rows)
    }

    fun startManual() {
        draft = Receipt(UUID.randomUUID().toString(), "Unbekannter Laden", LocalDate.now().toEpochDay(), emptyList())
        navigate(Screen.REVIEW)
    }

    private fun openDraft(rows: List<String>) {
        val today = LocalDate.now()
        val parsed = ReceiptParser.parse(rows, today, data.value.learnedCategories)
        val day = (parsed.date ?: today).toEpochDay()
        draft = Receipt(
            id = UUID.randomUUID().toString(),
            store = parsed.store,
            epochDay = day,
            items = parsed.items,
            printedTotal = parsed.printedTotal,
            extraDiscount = parsed.extraDiscount,
            scannedDay = today.toEpochDay(),
        )
        navigate(Screen.REVIEW)
        if (parsed.items.isEmpty()) scanError = "Keine Produkte erkannt – du kannst sie manuell hinzufügen."
    }

    fun updateDraft(transform: (Receipt) -> Receipt) {
        draft = draft?.let(transform)
    }

    fun updateDraftItem(id: String, transform: (PurchaseItem) -> PurchaseItem) = updateDraft { r ->
        r.copy(items = r.items.map { if (it.id == id) transform(it) else it })
    }

    fun renameDraftItem(id: String, name: String) = updateDraftItem(id) { item ->
        val cls = Categorizer.classify(name, data.value.learnedCategories)
        item.copy(name = name, category = cls.category, ingredient = cls.ingredient)
    }

    fun addDraftItem() = updateDraft { r ->
        r.copy(items = r.items + PurchaseItem(UUID.randomUUID().toString(), "Neues Produkt", 0.0))
    }

    fun removeDraftItem(id: String) = updateDraft { r -> r.copy(items = r.items.filterNot { it.id == id }) }

    fun discardDraft() {
        draft = null
        scanError = null
        navigate(Screen.HOME, asTab = true)
    }

    fun saveDraft() {
        val receipt = draft ?: return
        val cleaned = receipt.copy(items = receipt.items.filter { it.name.isNotBlank() })
        // remember manual category corrections for future scans
        val learned = cleaned.items.mapNotNull { item ->
            val auto = Categorizer.classify(item.name).category
            if (auto != item.category) Categorizer.normalize(item.name) to item.category else null
        }.toMap()
        val (xp, coins) = GameEngine.xpForReceipt(cleaned)
        val savings = cleaned.savings
        applyAward(xp, coins) { d ->
            d.copy(receipts = d.receipts + cleaned, learnedCategories = d.learnedCategories + learned)
        }
        toast("+$xp XP  +$coins 🪙  Bon erfasst!")
        if (savings > 0) toast("🏷️ ${de.bonquest.app.domain.euro(savings)} durch Aktionen gespart")
        draft = null
        scanError = null
        navigate(Screen.HOME, asTab = true)
    }

    // ---------- pantry ----------
    private fun setStatus(receiptId: String, itemId: String, status: ItemStatus, d: AppData): AppData {
        val today = LocalDate.now().toEpochDay()
        return d.copy(receipts = d.receipts.map { r ->
            if (r.id != receiptId) r else r.copy(items = r.items.map { if (it.id == itemId) it.copy(status = status, statusDay = today) else it })
        })
    }

    fun consume(entry: PantryEntry) {
        val today = LocalDate.now().toEpochDay()
        val rescued = entry.daysLeft >= 0
        applyAward(if (rescued) 10 else 3, if (rescued) 2 else 0) { d ->
            val updated = setStatus(entry.receiptId, entry.item.id, ItemStatus.CONSUMED, d)
            if (rescued) updated.copy(game = updated.game.copy(rescueLog = updated.game.rescueLog + today)) else updated
        }
        toast(if (rescued) "🛟 ${entry.label} gerettet! +10 XP" else "✔️ ${entry.label} verbraucht")
    }

    fun waste(entry: PantryEntry) {
        val today = LocalDate.now().toEpochDay()
        repo.update { d ->
            val updated = setStatus(entry.receiptId, entry.item.id, ItemStatus.WASTED, d)
            updated.copy(game = updated.game.copy(wasteLog = updated.game.wasteLog + today))
        }
        toast("🗑️ ${entry.label} weggeworfen – nächstes Mal retten wir es!")
    }

    fun cook(s: RecipeSuggestion) {
        val today = LocalDate.now()
        applyAward(40, 10) { d ->
            val entries = Pantry.entries(d.receipts, today.toEpochDay())
            var next = d
            for (key in s.usedKeys) {
                val e = entries.firstOrNull { it.item.ingredient == key && it.daysLeft >= 0 } ?: continue
                // perishable items are used up, long-life goods (pasta, rice …) stay in stock
                if (e.shelfDays <= 30) next = setStatus(e.receiptId, e.item.id, ItemStatus.CONSUMED, next)
            }
            next.copy(game = next.game.copy(cookLog = next.game.cookLog + today.toEpochDay()))
        }
        toast("${s.recipe.emoji} ${s.recipe.name} gekocht! +40 XP +10 🪙")
    }

    // ---------- game ----------
    fun claimQuest(q: Quest) {
        val ws = de.bonquest.app.domain.StatsCalculator.weekStart(LocalDate.now()).toEpochDay()
        if (Quests.isClaimed(data.value, q, LocalDate.now())) return
        applyAward(q.rewardXp, q.rewardCoins) { d -> d.copy(game = d.game.copy(claimedQuests = d.game.claimedQuests + (q.id to ws))) }
        toast("${q.emoji} Quest abgeschlossen! +${q.rewardXp} XP +${q.rewardCoins} 🪙")
    }

    fun buyOrSelectAvatar(id: String) {
        val avatar = Avatars.byId(id)
        val g = data.value.game
        when {
            id in g.ownedAvatars -> repo.update { it.copy(game = it.game.copy(avatar = id)) }
            g.coins >= avatar.price -> {
                applyAward(0, -avatar.price) { d ->
                    d.copy(game = d.game.copy(avatar = id, ownedAvatars = d.game.ownedAvatars + id))
                }
                toast("${avatar.emoji} ${avatar.name} freigeschaltet!")
            }
            else -> toast("Dir fehlen ${avatar.price - g.coins} 🪙")
        }
    }

    fun setBudget(budget: Double) = repo.update { it.copy(game = it.game.copy(weeklyBudget = budget)) }
    fun setHeroName(name: String) = repo.update { it.copy(game = it.game.copy(heroName = name.take(20))) }

    fun deleteReceipt(id: String) = repo.update { d -> d.copy(receipts = d.receipts.filterNot { it.id == id }) }

    fun setItemCategory(receiptId: String, itemId: String, category: Category) = repo.update { d ->
        var learnedName: String? = null
        val receipts = d.receipts.map { r ->
            if (r.id != receiptId) r else r.copy(items = r.items.map {
                if (it.id == itemId) { learnedName = Categorizer.normalize(it.name); it.copy(category = category) } else it
            })
        }
        d.copy(receipts = receipts, learnedCategories = learnedName?.let { d.learnedCategories + (it to category) } ?: d.learnedCategories)
    }

    private fun applyAward(xp: Int, coins: Int, change: (AppData) -> AppData) {
        var unlocked: List<Achievement> = emptyList()
        var level: Int? = null
        repo.update { d ->
            val result = GameEngine.award(change(d), xp, coins)
            unlocked = result.unlocked
            level = result.newLevel
            result.data
        }
        level?.let { popups += Popup.LevelUp(it) }
        unlocked.forEach { popups += Popup.Unlocked(it) }
    }

    fun dismissPopup() { if (popups.isNotEmpty()) popups.removeAt(0) }

    private var toastId = 0L
    fun toast(text: String) {
        val t = Toast(toastId++, text)
        toasts += t
        viewModelScope.launch {
            kotlinx.coroutines.delay(2600)
            toasts.remove(t)
        }
    }
}
