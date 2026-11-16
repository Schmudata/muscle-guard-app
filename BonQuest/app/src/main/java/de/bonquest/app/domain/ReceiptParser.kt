package de.bonquest.app.domain

import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs

/** One recognized text line with its bounding box (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Receipts are two-column layouts (name ... price). OCR engines often return the
 * columns as separate blocks, so we re-assemble physical rows by vertical position.
 */
object RowBuilder {
    fun buildRows(lines: List<OcrLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { (it.top + it.bottom) / 2 }
        if (sorted.isEmpty()) return emptyList()
        val heights = sorted.map { (it.bottom - it.top).coerceAtLeast(1) }.sorted()
        val medianH = heights[heights.size / 2]
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (l in sorted) {
            val cy = (l.top + l.bottom) / 2.0
            val row = rows.lastOrNull()
            if (row != null) {
                val rcy = row.map { (it.top + it.bottom) / 2.0 }.average()
                if (abs(cy - rcy) < medianH * 0.6) {
                    row.add(l); continue
                }
            }
            rows.add(mutableListOf(l))
        }
        return rows.map { r -> r.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    }
}

data class ParsedReceipt(
    val store: String,
    val date: LocalDate?,
    val items: List<PurchaseItem>,
    val printedTotal: Double?,
    val extraDiscount: Double,
)

object ReceiptParser {

    private val priceAtEnd = Regex("""(-\s?)?(\d{1,4})\s?[.,]\s?(\d{2})\s*(-)?\s*(?:€|EUR|eur)?\s*(?:[A-Za-z]{1,2}|\*|[0-9])?\s*$""")
    private val qtyLine = Regex("""^\s*(\d{1,3})\s*(?:stk\.?|st\.?)?\s*[xX*×]\s*(\d{1,4}[.,]\d{2})\s*(?:€|EUR)?(?:\s*/\s*stk\.?)?(?:\s+(-?\d{1,4}[.,]\d{2})\s*\S?)?\s*$""", RegexOption.IGNORE_CASE)
    private val weightLine = Regex("""(\d{1,3}[.,]\d{1,3})\s*kg\s*[xX*×]\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
    private val inlineQtyA = Regex("""(\d{1,4}[.,]\d{2})\s*[xX*×]\s*(\d{1,3})\b""")
    private val inlineQtyB = Regex("""\b(\d{1,3})\s*(?:stk\.?)?\s*[xX*×]\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")

    private val totalRegex = Regex("""\b(summe|zu zahlen|total|gesamtbetrag|endbetrag|gesamtsumme|betrag eur)\b""")
    private val skipRegex = Regex(
        """\b(mwst|steuer\w*|netto|brutto|ust|bar|rueckgeld|gegeben|ec|girocard|kartenzahlung|karte|visa|mastercard|maestro|kreditkarte|kontaktlos|terminal|trace|beleg\w*|bon|kasse|kassierer\w*|tse|signatur|transaktion\w*|uhrzeit|datum|posten|artikel|payback|punkte|danke|www|tel|telefon|zahlung|bezahlt|wechselgeld|zwischensumme|steuernr|filiale|markt|seriennr|nr|uid|gmbh|ohg|str|strasse)\b"""
    )
    private val discountRegex = Regex("""(rabatt|coupon|gutschein|preisvorteil|sie sparen|nachlass|ersparnis|lidl plus|vorteil|reduziert|sofortrabatt|preisred|sparen|abzug|bonus)""")
    private val promoItemRegex = Regex("""\b(aktion|angebot|sonderangebot|super angebot|statt|reduz|deal|knaller)\b""")
    private val pfandReturnRegex = Regex("""(leergut|pfandrueck|pfand rueck|pfandbon|pfand)""")

    private val stores = listOf(
        "rewe" to "REWE", "lidl" to "Lidl", "aldi" to "ALDI", "edeka" to "EDEKA", "penny" to "Penny",
        "kaufland" to "Kaufland", "netto" to "Netto", "globus" to "Globus", "norma" to "Norma",
        "tegut" to "tegut", "rossmann" to "Rossmann", "dm drogerie" to "dm", "dm markt" to "dm",
        "mueller" to "Müller", "denns" to "denn's", "alnatura" to "Alnatura", "famila" to "famila",
        "marktkauf" to "Marktkauf", "real" to "real", "spar" to "SPAR", "billa" to "BILLA",
        "hofer" to "HOFER", "migros" to "Migros", "coop" to "Coop", "metro" to "Metro",
        "hit" to "HIT", "nahkauf" to "nahkauf", "e center" to "E center", "combi" to "Combi",
        "jumbo" to "Jumbo", "albert heijn" to "Albert Heijn", "carrefour" to "Carrefour",
    )

    fun parse(
        rows: List<String>,
        today: LocalDate = LocalDate.now(),
        learned: Map<String, Category> = emptyMap(),
    ): ParsedReceipt {
        val items = mutableListOf<PurchaseItem>()
        var store: String? = null
        var date: LocalDate? = null
        var total: Double? = null
        var extraDiscount = 0.0
        var ended = false
        var pendingQty: Pair<Double, Double>? = null // qty, unit price for the next item

        rows.forEachIndexed { index, raw ->
            val line = raw.trim().replace(Regex("\\s+"), " ")
            if (line.isBlank()) return@forEachIndexed
            val low = Categorizer.normalize(line)

            if (date == null) date = findDate(line, today)
            if (store == null) {
                val limit = if (index < 8) stores else stores.filter { it.first != "netto" }
                store = limit.firstOrNull { (k, _) -> " $low ".contains(" $k ") }?.second
            }
            if (ended) return@forEachIndexed

            val priceMatch = priceAtEnd.find(line)

            if (totalRegex.containsMatchIn(low) && !low.contains("zwischensumme")) {
                if (priceMatch != null) total = abs(toPrice(priceMatch))
                if (items.isNotEmpty()) ended = true
                return@forEachIndexed
            }

            // "0,512 kg x 2,99 EUR/kg" belongs to the previous item
            weightLine.find(line)?.let { w ->
                val kg = num(w.groupValues[1])
                val last = items.lastOrNull()
                val hasName = line.substring(0, w.range.first).any { it.isLetter() }
                if (last != null && !hasName) {
                    items[items.lastIndex] = last.copy(quantity = kg, unit = "kg")
                    return@forEachIndexed
                }
            }

            // "2 Stk x 1,29" belongs to the previous item (REWE) or the next one (ALDI)
            qtyLine.matchEntire(line)?.let { q ->
                val qty = q.groupValues[1].toDouble()
                val unitP = num(q.groupValues[2])
                val last = items.lastOrNull()
                if (last != null && abs(last.price - qty * unitP) < 0.03) {
                    items[items.lastIndex] = last.copy(quantity = qty)
                } else if (q.groupValues[3].isNotEmpty() && last != null && last.quantity == 1.0 && abs(last.price - unitP) < 0.03) {
                    items[items.lastIndex] = last.copy(quantity = qty, price = num(q.groupValues[3]))
                } else {
                    pendingQty = qty to unitP
                }
                return@forEachIndexed
            }

            if (priceMatch == null) return@forEachIndexed
            var price = toPrice(priceMatch)
            var name = line.substring(0, priceMatch.range.first).trim()
            val nameNorm = Categorizer.normalize(name)

            val isDiscountText = discountRegex.containsMatchIn(nameNorm)
            val isPfand = pfandReturnRegex.containsMatchIn(nameNorm)

            if (!name.any { it.isLetter() }) {
                // a bare "-0,50" line: treat as discount for the previous item
                if (price < 0 && items.isNotEmpty()) {
                    val last = items.last()
                    items[items.lastIndex] = last.copy(discount = last.discount + abs(price), promoLabel = last.promoLabel ?: "Rabatt")
                }
                return@forEachIndexed
            }
            if (!isDiscountText && !isPfand && skipRegex.containsMatchIn(nameNorm)) return@forEachIndexed

            if (isPfand) {
                items += PurchaseItem(
                    id = UUID.randomUUID().toString(), name = prettify(cleanName(name)), price = price,
                    category = Category.PFAND,
                )
                return@forEachIndexed
            }

            if (price < 0 || isDiscountText) {
                val amount = abs(price)
                val label = prettify(cleanName(name)).ifBlank { "Rabatt" }
                val last = items.lastOrNull { it.category != Category.PFAND }
                if (last != null) {
                    val idx = items.indexOf(last)
                    items[idx] = last.copy(discount = last.discount + amount, promoLabel = label)
                } else {
                    extraDiscount += amount
                }
                return@forEachIndexed
            }

            var quantity = 1.0
            inlineQtyA.find(name)?.let { m ->
                quantity = m.groupValues[2].toDouble(); name = name.removeRange(m.range)
            } ?: inlineQtyB.find(name)?.let { m ->
                quantity = m.groupValues[1].toDouble(); name = name.removeRange(m.range)
            }
            pendingQty?.let { (q, u) ->
                if (abs(price - q * u) < 0.03) quantity = q
                pendingQty = null
            }

            val cleaned = prettify(cleanName(name))
            if (cleaned.length < 2) return@forEachIndexed
            val promoLabel = if (promoItemRegex.containsMatchIn(nameNorm)) "Aktionsartikel" else null
            val cls = Categorizer.classify(cleaned, learned)
            items += PurchaseItem(
                id = UUID.randomUUID().toString(),
                name = cleaned,
                price = price,
                quantity = quantity,
                category = cls.category,
                ingredient = cls.ingredient,
                promoLabel = promoLabel,
            )
        }

        return ParsedReceipt(
            store = store ?: "Unbekannter Laden",
            date = date,
            items = items,
            printedTotal = total,
            extraDiscount = extraDiscount,
        )
    }

    private fun toPrice(m: MatchResult): Double {
        val v = "${m.groupValues[2]}.${m.groupValues[3]}".toDouble()
        val negative = m.groupValues[1].isNotEmpty() || m.groupValues[4].isNotEmpty()
        return if (negative) -v else v
    }

    private fun num(s: String): Double = s.replace(',', '.').toDouble()

    private fun findDate(line: String, today: LocalDate): LocalDate? {
        for (m in dateRegex.findAll(line)) {
            val d = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            var y = m.groupValues[3].toInt()
            if (y < 100) y += 2000
            val parsed = runCatching { LocalDate.of(y, mo, d) }.getOrNull() ?: continue
            if (!parsed.isAfter(today.plusDays(1)) && parsed.isAfter(today.minusYears(3))) return parsed
        }
        return null
    }

    private fun cleanName(name: String): String = name
        .replace(Regex("""^\d{4,}\s+"""), "")          // article numbers
        .replace(Regex("""\s+[xX*×]\s*$"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '.', '-', '*', ':')

    /** "BIO TOMATEN 500G" -> "Bio Tomaten 500g" */
    fun prettify(name: String): String {
        val letters = name.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() }) return name
        return name.lowercase().split(" ").joinToString(" ") { w ->
            if (w.firstOrNull()?.isLetter() == true) w.replaceFirstChar { it.uppercase() } else w
        }
    }
}
