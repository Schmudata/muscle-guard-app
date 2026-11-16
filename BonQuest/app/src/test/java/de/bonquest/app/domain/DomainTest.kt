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
    fun categorizerReadsPortuguese() {
        assertEquals("tomate", Categorizer.classify("TOMATE CHERRY").ingredient)
        assertEquals("tomatenkonserve", Categorizer.classify("Polpa de Tomate").ingredient)
        assertEquals("milch", Categorizer.classify("Leite Meio Gordo 1L").ingredient)
        assertEquals("kokosmilch", Categorizer.classify("Leite de Coco").ingredient)
        assertEquals(Category.SNACKS, Categorizer.classify("Chocolate de Leite").category)
        assertEquals("brot", Categorizer.classify("Pão de Mistura").ingredient)
        assertEquals("toast", Categorizer.classify("Pão de Forma").ingredient)
        assertEquals("zucker", Categorizer.classify("Açúcar Branco").ingredient)
        assertEquals("hackfleisch", Categorizer.classify("Carne Picada Mista").ingredient)
        assertEquals("gefluegel", Categorizer.classify("Peito de Frango").ingredient)
        assertEquals("eier", Categorizer.classify("Ovos Classe M").ingredient)
        assertEquals("joghurt", Categorizer.classify("Iogurte de Morango").ingredient)
        assertEquals("saft", Categorizer.classify("Sumo de Laranja").ingredient)
        assertEquals("thunfisch", Categorizer.classify("Atum em Azeite").ingredient)
        assertEquals("lauch", Categorizer.classify("Alho Francês").ingredient)
        assertEquals("knoblauch", Categorizer.classify("Alho").ingredient)
        assertEquals("nudeln", Categorizer.classify("Massa Espiral").ingredient)
        assertEquals("apfel", Categorizer.classify("Maçã Gala").ingredient)
        assertEquals(Category.HAUSHALT, Categorizer.classify("Papel Higiénico").category)
        assertEquals(Category.ALKOHOL, Categorizer.classify("Vinho Tinto Douro").category)
        assertEquals(Category.SNACKS, Categorizer.classify("Batatas Fritas Lays").category)
        assertEquals(Category.GETRAENKE, Categorizer.classify("Água das Pedras").category)
        assertEquals(Category.SONSTIGES, Categorizer.classify("XYZ 123").category)
        // German & English fallback still works
        assertEquals("milch", Categorizer.classify("H-Milch 3,5%").ingredient)
        assertEquals("butter", Categorizer.classify("Butter").ingredient)
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
                OcrLine("MANTEIGA", 10, 100, 120, 130),
                OcrLine("LEITE", 10, 140, 110, 170),
                OcrLine("1,99 A", 400, 102, 480, 131),
                OcrLine("0,99 A", 400, 141, 480, 171),
            )
        )
        assertEquals(listOf("MANTEIGA 1,99 A", "LEITE 0,99 A"), rows)
    }

    private fun sum(p: ParsedReceipt) = p.items.sumOf { it.effectivePrice } - p.extraDiscount

    @Test
    fun parsesContinente() {
        val p = ReceiptParser.parse(DemoData.demoRows(0, today), today)
        assertEquals("Continente", p.store)
        assertEquals(today, p.date)
        assertEquals(17.90, p.printedTotal!!, 0.001)
        assertEquals(2.0, p.items.first { it.ingredient == "milch" }.quantity, 0.0)
        assertEquals(0.8, p.items.first { it.ingredient == "hackfleisch" }.discount, 0.001)
        assertEquals(0.5, p.items.first { it.ingredient == "parmesan" }.discount, 0.001)
        assertEquals(11, p.items.size)
        assertEquals(p.printedTotal!!, sum(p), 0.01)
    }

    @Test
    fun parsesPingoDoce() {
        val p = ReceiptParser.parse(DemoData.demoRows(1, today), today)
        assertEquals("Pingo Doce", p.store)
        assertEquals(today.minusDays(4), p.date)
        assertEquals("kg", p.items.first { it.ingredient == "banane" }.unit)
        assertEquals("Banana", p.items.first { it.ingredient == "banane" }.name)
        assertEquals(0.3, p.items.filter { it.ingredient == "mozzarella" }.sumOf { it.discount }, 0.001)
        assertEquals(1.0, p.items.first { it.ingredient == "gefluegel" }.discount, 0.001)
        assertEquals("brot", p.items.last().ingredient)
        assertEquals(p.printedTotal!!, sum(p), 0.01)
    }

    @Test
    fun parsesLidlPortugal() {
        val p = ReceiptParser.parse(DemoData.demoRows(2, today), today)
        assertEquals("Lidl", p.store)
        assertEquals(today, p.date)
        assertEquals("On promotion", p.items.first { it.ingredient == "fisch" }.promoLabel)
        assertEquals(13, p.items.size)
        assertEquals(p.printedTotal!!, sum(p), 0.01)
    }

    @Test
    fun parsesGermanReceipt() {
        val rows = listOf(
            "REWE Markt GmbH", "EUR", "SPAGHETTI 1,29 A", "GEMISCHTES HACK 500G 4,99 A", "AKTION -1,00 A",
            "H-MILCH 3,5% 2,58 A", "2 Stk x 1,29", "PFAND 0,25 B", "SUMME EUR 8,11", "Bar 10,00",
        )
        val p = ReceiptParser.parse(rows, today)
        assertEquals("REWE", p.store)
        assertEquals(4, p.items.size)
        assertEquals(p.printedTotal!!, sum(p), 0.01)
    }

    @Test
    fun skipsPaymentAndTaxLines() {
        val p = ReceiptParser.parse(listOf("Manteiga 1,99", "IVA 23% 0,37", "Numerário 5,00", "Troco 3,01", "Total IVA 0,37"), today)
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
        val r3 = receipt("3", today, listOf("CONTINENTE", "Leite UHT 0,85", "Total 0,85"))
        val data = AppData(receipts = listOf(r1, r2, r3))
        val entries = Pantry.entries(data.receipts, today.toEpochDay())
        assertTrue(entries.any { it.item.ingredient == "nudeln" })
        assertTrue(entries.none { it.item.ingredient == "hackfleisch" })
        val check = Pantry.checkList("massa\npasta de dentes", entries)
        assertTrue(check[0].entries.isNotEmpty())
        assertTrue(check[1].entries.isEmpty())

        val recipes = Recipes.suggest(entries)
        assertTrue(recipes.isNotEmpty())
        assertNotNull(recipes.firstOrNull { it.recipe.id == "greek" })

        val stats = StatsCalculator.compute(data, today)
        assertEquals(3, stats.receiptCount)
        assertTrue(stats.totalSaved > 2.5)
        assertTrue(stats.doublePurchases.any { it.label == "Milk" })
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
        assertTrue(GameEngine.award(result.data, 0, 0).unlocked.isEmpty())
    }
}
