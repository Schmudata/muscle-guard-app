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

/**
 * Parses receipt rows. Built for Portuguese receipts (Continente, Pingo Doce, Lidl,
 * Auchan, Intermarché, Minipreço, Mercadona …, also Brazilian ones) and still
 * understands German and English receipts.
 */
object ReceiptParser {

    const val UNKNOWN_STORE = "Unknown store"

    // price at the end of a row, optionally followed by a tax code: "1,29 A", "1.29€", "0,89 (C)", "2,49 23%"
    private val priceAtEnd = Regex("""(-\s?)?(\d{1,4})\s?[.,]\s?(\d{2})\s*(-)?\s*(?:€|EUR|eur)?\s*(?:\(?[A-Za-z]{1,2}\)?|\*|[0-9]|\(?\d{1,2}\s?%\)?)?\s*$""")
    private val qtyLine = Regex("""^\s*(\d{1,3})(?:[.,]0{1,3})?\s*(?:stk\.?|st\.?|un\.?|uni\.?|und\.?|unid\.?|pcs?\.?)?\s*[xX*×]\s*(\d{1,4}[.,]\d{2})\s*(?:€|EUR)?(?:\s*/\s*\w+\.?)?(?:\s+(-?\d{1,4}[.,]\d{2})\s*\S{0,4})?\s*$""", RegexOption.IGNORE_CASE)
    private val weightLine = Regex("""(\d{1,3}[.,]\d{1,3})\s*kg\s*[xX*×]\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
    private val inlineQtyA = Regex("""(\d{1,4}[.,]\d{2})\s*[xX*×]\s*(\d{1,3})\b""")
    private val inlineQtyB = Regex("""\b(\d{1,3})\s*(?:stk\.?|un\.?|uni\.?|und\.?)?\s*[xX*×]\s*(\d{1,4}[.,]\d{2})""", RegexOption.IGNORE_CASE)
    private val dateDmy = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")
    private val dateYmd = Regex("""\b(\d{4})[./-](\d{1,2})[./-](\d{1,2})\b""")

    private val totalRegex = Regex("""\b(total|total a pagar|a pagar|valor a pagar|valor total|summe|zu zahlen|gesamtbetrag|endbetrag|amount due|grand total)\b""")
    /** "total" lines that are NOT the amount paid */
    private val notTotalRegex = Regex("""\b(iva|taxa|base|incidencia|poupanca|poupou|desconto\w*|artigos|itens|items|saldo|acumul\w*|mwst|steuer|netto|tax|vat|savings)\b|subtotal|zwischensumme""")

    private val skipRegex = Regex(
        """\b(""" +
            // Portuguese / Brazilian
            "iva|nif|nipc|contribuinte|cnpj|cpf|multibanco|mb|troco|numerario|dinheiro|pagamento|pago|cartao|visa|mastercard|maestro|fatura|factura|simplificada|recibo|atcud|operador\\w*|caixa|obrigado\\w*|volte|sempre|base|taxa|incidencia|subtotal|saldo|acumul\\w*|talao|documento|original|processado|programa|certificado|cliente|morada|rua|avenida|av|lda|sa|telefone|tel|hora|data|artigos|itens|loja|terminal|autorizacao|aut|transacao|referencia|ref|euros|" +
            // English
            "tax|vat|cash|change|card|thank|thanks|receipt|store|tender|" +
            // German
            "mwst|steuer\\w*|netto|brutto|ust|bar|rueckgeld|gegeben|ec|girocard|kartenzahlung|karte|kreditkarte|kontaktlos|trace|beleg\\w*|bon|kasse|tse|signatur|uhrzeit|datum|posten|artikel|payback|punkte|danke|www|zahlung|wechselgeld|zwischensumme|filiale|markt|gmbh|ohg|str|strasse" +
            """)\b"""
    )
    /** Words that always mean "this row is a reduction" (assigned to the previous product). */
    private val strongDiscountRegex = Regex(
        """\b(desconto\w*|desc|poupanca|poupe|promocao imediata|cupao|cupom|vale|reducao|discount|coupon|saving|savings|rabatt|preisvorteil|nachlass|ersparnis|gutschein|sofortrabatt|lidl plus)\b|rabatt|leve \d+ pague|2a unidade|2 unidade"""
    )
    /** Words that mark a product as being on promotion. */
    private val promoItemRegex = Regex("""\b(promo|promocao|campanha|oferta|preco especial|desconto direto|aktion|angebot|sonderangebot|offer|deal|special)\b""")
    private val pfandRegex = Regex("""(vasilhame|\btara\b|leergut|pfand|deposit)""")

    private val stores = listOf(
        // Portugal
        "continente" to "Continente", "pingo doce" to "Pingo Doce", "lidl" to "Lidl", "auchan" to "Auchan",
        "intermarche" to "Intermarché", "minipreco" to "Minipreço", "mercadona" to "Mercadona", "aldi" to "ALDI",
        "el corte ingles" to "El Corte Inglés", "apolonia" to "Apolónia", "spar" to "SPAR", "froiz" to "Froiz",
        "meu super" to "Meu Super", "recheio" to "Recheio", "makro" to "Makro", "jumbo" to "Jumbo",
        "coviran" to "Covirán", "amanhecer" to "Amanhecer", "celeiro" to "Celeiro", "e leclerc" to "E.Leclerc",
        "leclerc" to "E.Leclerc", "glood" to "Glood",
        // Brazil
        "pao de acucar" to "Pão de Açúcar", "carrefour" to "Carrefour", "assai" to "Assaí", "atacadao" to "Atacadão",
        "zaffari" to "Zaffari", "sonda" to "Sonda", "savegnago" to "Savegnago", "guanabara" to "Guanabara",
        // Germany & others
        "rewe" to "REWE", "edeka" to "EDEKA", "penny" to "Penny", "kaufland" to "Kaufland", "netto" to "Netto",
        "rossmann" to "Rossmann", "tesco" to "Tesco", "sainsbury" to "Sainsbury's", "dia" to "Dia",
    )
    /** Short / ambiguous store names only count in the receipt header. */
    private val headerOnlyStores = setOf("netto", "dia", "spar", "sonda", "jumbo")

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
        var pendingQty: Pair<Double, Double>? = null

        rows.forEachIndexed { index, raw ->
            val line = raw.trim().replace(Regex("\\s+"), " ")
            if (line.isBlank()) return@forEachIndexed
            val low = Categorizer.normalize(line)

            if (date == null) date = findDate(line, today)
            if (store == null) {
                store = stores.firstOrNull { (k, _) ->
                    (index < 8 || k !in headerOnlyStores) && " $low ".contains(" $k ")
                }?.second
            }
            if (ended) return@forEachIndexed

            val priceMatch = priceAtEnd.find(line)

            if (totalRegex.containsMatchIn(low)) {
                if (!notTotalRegex.containsMatchIn(low)) {
                    if (priceMatch != null) total = abs(toPrice(priceMatch))
                    if (items.isNotEmpty()) ended = true
                }
                return@forEachIndexed
            }

            // "0,512 kg x 2,99 €/kg" belongs to the previous item
            weightLine.find(line)?.let { w ->
                val kg = num(w.groupValues[1])
                val last = items.lastOrNull()
                val hasName = line.substring(0, w.range.first).any { it.isLetter() }
                if (last != null && !hasName) {
                    items[items.lastIndex] = last.copy(quantity = kg, unit = "kg")
                    return@forEachIndexed
                }
            }

            // "2 x 1,29" belongs to the previous item, or to the next one
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
            val price = toPrice(priceMatch)
            var name = line.substring(0, priceMatch.range.first).trim()
            val nameNorm = Categorizer.normalize(name)

            if (!name.any { it.isLetter() }) {
                // a bare "-0,50" row: discount for the previous item
                if (price < 0 && items.isNotEmpty()) {
                    val last = items.last()
                    items[items.lastIndex] = last.copy(discount = last.discount + abs(price), promoLabel = last.promoLabel ?: "Discount")
                }
                return@forEachIndexed
            }

            val isStrongDiscount = strongDiscountRegex.containsMatchIn(nameNorm)
            val isPfand = pfandRegex.containsMatchIn(nameNorm)
            val isPromoWord = promoItemRegex.containsMatchIn(nameNorm)

            if (!isStrongDiscount && !isPfand && !isPromoWord && skipRegex.containsMatchIn(nameNorm)) return@forEachIndexed

            if (isPfand && !isStrongDiscount) {
                items += PurchaseItem(id = UUID.randomUUID().toString(), name = prettify(cleanName(name)), price = price, category = Category.PFAND)
                return@forEachIndexed
            }

            // A reduction row: negative price, or a "Desconto/Poupança…" row. A bare promo word
            // ("Promo", "Oferta") with a negative price is also a reduction.
            if (price < 0 || isStrongDiscount) {
                val amount = abs(price)
                val label = prettify(cleanName(name)).ifBlank { "Discount" }
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
            val cls = Categorizer.classify(cleaned, learned)
            items += PurchaseItem(
                id = UUID.randomUUID().toString(),
                name = cleaned,
                price = price,
                quantity = quantity,
                category = cls.category,
                ingredient = cls.ingredient,
                promoLabel = if (isPromoWord) "On promotion" else null,
            )
        }

        return ParsedReceipt(
            store = store ?: UNKNOWN_STORE,
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
        val candidates = dateYmd.findAll(line).map { Triple(it.groupValues[3], it.groupValues[2], it.groupValues[1]) } +
            dateDmy.findAll(line).map { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
        for ((ds, ms, ys) in candidates) {
            var y = ys.toInt()
            if (y < 100) y += 2000
            val parsed = runCatching { LocalDate.of(y, ms.toInt(), ds.toInt()) }.getOrNull() ?: continue
            if (!parsed.isAfter(today.plusDays(1)) && parsed.isAfter(today.minusYears(3))) return parsed
        }
        return null
    }

    private fun cleanName(name: String): String = name
        .replace(Regex("""^\d{4,}\s+"""), "")              // article numbers / EAN
        .replace(Regex("""^\(\s?[A-Za-z]\s?\)\s*"""), "")  // leading tax code "(C) "
        .replace(Regex("""\s+\(?\d{1,2}\s?%\)?$"""), "")   // trailing VAT rate "23%"
        .replace(Regex("""\s+[xX*×]\s*$"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '.', '-', '*', ':')

    /** "LEITE MEIO GORDO 1L" -> "Leite Meio Gordo 1l" */
    fun prettify(name: String): String {
        val letters = name.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() }) return name
        return name.lowercase().split(" ").joinToString(" ") { w ->
            if (w.firstOrNull()?.isLetter() == true) w.replaceFirstChar { it.uppercase() } else w
        }
    }
}
