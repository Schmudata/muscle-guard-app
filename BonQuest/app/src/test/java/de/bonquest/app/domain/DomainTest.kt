package de.bonquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainTest {
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun categorizerFindsIngredients() {
        assertEquals("tomate", Categorizer.classify("BIO TOMATEN").ingredient)
        assertEquals("tomatenkonserve", Categorizer.classify("Tomatenmark 200g").ingredient)
        assertEquals(Category.SNACKS, Categorizer.classify("Milchschokolade").category)
        assertEquals("milch", Categorizer.classify("H-Milch 3,5%").ingredient)
        assertEquals("hackfleisch", Categorizer.classify("Gemischtes Hackfleisch").ingredient)
        assertEquals("eier", Categorizer.classify("Bio Eier 10er").ingredient)
        assertEquals(Category.HAUSHALT, Categorizer.classify("Toilettenpapier").category)
        assertEquals(Category.ALKOHOL, Categorizer.classify("Weißwein Riesling").category)
        assertEquals("joghurt", Categorizer.classify("Erdbeerjoghurt").ingredient)
        assertEquals(Category.SONSTIGES, Categorizer.classify("XYZ 123").category)
    }

    @Test
    fun learnedCategoryOverrides() {
        val learned = mapOf(Categorizer.normalize("Mystery Box") to Category.SNACKS)
        assertEquals(Category.SNACKS, Categorizer.classify("Mystery Box", learned).category)
    }

    @Test
    fun rowBuilderMergesColumns() {
        val rows = RowBuilder.buildRows(
            listOf(
                OcrLine("BUTTER", 10, 100, 120, 130),
                OcrLine("MILCH", 10, 140, 110, 170),
                OcrLine("1,99 A", 400, 102, 480, 131),
                OcrLine("0,99 A", 400, 141, 480, 171),
            )
        )
        assertEquals(listOf("BUTTER 1,99 A", "MILCH 0,99 A"), rows)
    }

    @Test
    fun parsesReweDemo() {
        val p = ReceiptParser.parse(DemoData.demoRows(0, today), today)
        assertEquals("REWE", p.store)
        assertEquals(today, p.date)
        assertEquals(21.53, p.printedTotal!!, 0.001)
        val milk = p.items.first { it.ingredient == "milch" }
        assertEquals(2.0, milk.quantity, 0.0)
        val hack = p.items.first { it.ingredient == "hackfleisch" }
        assertEquals(1.0, hack.discount, 0.001)
        val parmesan = p.items.first { it.ingredient == "parmesan" }
        assertEquals(0.6, parmesan.discount, 0.001)
        assertTrue(p.items.any { it.category == Category.PFAND })
        val sum = p.items.sumOf { it.effectivePrice } - p.extraDiscount
        assertEquals(p.printedTotal!!, sum, 0.01)
    }

    @Test
    fun parsesLidlDemo() {
        val p = ReceiptParser.parse(DemoData.demoRows(1, today), today)
        assertEquals("Lidl", p.store)
        assertEquals(today.minusDays(4), p.date)
        val banana = p.items.first { it.ingredient == "banane" }
        assertEquals("kg", banana.unit)
        val pfand = p.items.first { it.category == Category.PFAND }
        assertEquals(-1.5, pfand.price, 0.001)
        assertEquals(0.3, p.items.filter { it.ingredient == "mozzarella" }.sumOf { it.discount }, 0.001)
        val sum = p.items.sumOf { it.effectivePrice } - p.extraDiscount
        assertEquals(p.printedTotal!!, sum, 0.01)
    }

    @Test
    fun parsesAldiDemo() {
        val p = ReceiptParser.parse(DemoData.demoRows(2, today), today)
        assertEquals("ALDI", p.store)
        assertEquals("Aktionsartikel", p.items.first { it.ingredient == "fisch" }.promoLabel)
        val sum = p.items.sumOf { it.effectivePrice }
        assertEquals(p.printedTotal!!, sum, 0.01)
        assertEquals(13, p.items.size)
    }

    @Test
    fun skipsPaymentLines() {
        val p = ReceiptParser.parse(listOf("Butter 1,99", "MwSt 19% 0,32", "Bar 5,00", "Rückgeld 3,01"), today)
        assertEquals(1, p.items.size)
        assertNull(p.printedTotal)
    }

    private fun receipt(id: String, day: LocalDate, rows: List<String>): Receipt {
        val p = ReceiptParser.parse(rows, today)
        return Receipt(id, p.store, day.toEpochDay(), p.items, p.printedTotal, p.extraDiscount, day.toEpochDay())
    }

    @Test
    fun pantryRecipesAndStats() {
        val r1 = receipt("1", today.minusDays(5), DemoData.demoRows(0, today))
        val r2 = receipt("2", today.minusDays(1), DemoData.demoRows(1, today))
        val r3 = receipt("3", today, listOf("REWE", "Milch 1,09", "Summe 1,09"))
        val data = AppData(receipts = listOf(r1, r2, r3))
        val entries = Pantry.entries(data.receipts, today.toEpochDay())
        assertTrue(entries.any { it.item.ingredient == "nudeln" })
        // fresh minced meat from 5 days ago should be gone
        assertTrue(entries.none { it.item.ingredient == "hackfleisch" })
        val check = Pantry.checkList("Nudeln\nZahnpasta", entries)
        assertTrue(check[0].entries.isNotEmpty())
        assertTrue(check[1].entries.isEmpty())

        val recipes = Recipes.suggest(entries)
        assertTrue(recipes.isNotEmpty())
        assertNotNull(recipes.firstOrNull { it.recipe.id == "greek" })

        val stats = StatsCalculator.compute(data, today)
        assertEquals(3, stats.receiptCount)
        assertTrue(stats.totalSaved > 2.9)
        assertTrue(stats.doublePurchases.any { it.label == "Milch" })
        assertTrue(TipEngine.tips(stats, 80.0).isNotEmpty())
    }

    @Test
    fun gameAwardsAchievementsAndLevels() {
        assertEquals(1, Levels.levelFor(0))
        assertEquals(2, Levels.levelFor(100))
        assertEquals(3, Levels.levelFor(250))
        val r = receipt("1", today, DemoData.demoRows(0, today))
        val data = AppData(receipts = listOf(r))
        val (xp, coins) = GameEngine.xpForReceipt(r)
        val result = GameEngine.award(data, xp, coins)
        assertTrue(result.unlocked.any { it.id == "first_scan" })
        assertTrue(result.unlocked.any { it.id == "first_promo" })
        assertNotNull(result.newLevel)
        // awarding again does not unlock twice
        assertTrue(GameEngine.award(result.data, 0, 0).unlocked.isEmpty())
    }
}
