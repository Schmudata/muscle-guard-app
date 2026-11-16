package de.bonquest.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Sample receipts in the shape OCR returns them, to try the app without a real receipt. */
object DemoData {
    private val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun demoRows(variant: Int, today: LocalDate = LocalDate.now()): List<String> {
        val d = today.minusDays(if (variant % 2 == 0) 0 else 4).format(fmt)
        return when (variant % 3) {
            0 -> listOf(
                "REWE Markt GmbH", "Hauptstr. 12", "12345 Musterstadt", "EUR",
                "SPAGHETTI 1,29 A", "PASSATA 0,99 A", "ZWIEBELN 1KG 1,49 A", "KNOBLAUCH 0,79 A",
                "GEMISCHTES HACK 500G 4,99 A", "AKTION -1,00 A", "H-MILCH 3,5% 2,58 A", "2 Stk x 1,29",
                "BIO EIER 10ER 3,49 A", "KAROTTEN 1,19 A", "PARMESAN 2,99 A", "Rabatt Coupon -0,60",
                "CHIPS PAPRIKA 1,79 A", "COLA 1,5L 1,29 B", "PFAND 0,25 B",
                "--------------------", "SUMME EUR 21,53", "Geg. EC-Karte EUR 21,53",
                "Datum: $d 18:42", "Vielen Dank für Ihren Einkauf",
            )
            1 -> listOf(
                "LIDL", "Lidl Vertriebs-GmbH & Co. KG", "EUR",
                "Bananen 1,19 A", "0,850 kg x 1,39 EUR/kg", "Tomaten Rispe 1,99 A", "Mozzarella 0,79 A",
                "Mozzarella 0,79 A", "Lidl Plus Rabatt -0,30", "Gurke 0,69 A", "Feta 1,49 A",
                "Basmati Reis 1,99 A", "Kokosmilch 1,29 A", "Haehnchenbrust 5,49 A", "Preisvorteil -1,10",
                "Paprika Mix 1,99 A", "Vollkornbrot 1,59 A", "Leergut -1,50 B",
                "zu zahlen 16,39", "Bar 20,00", "Rückgeld 3,61", "$d 11:03 Kasse 3",
            )
            else -> listOf(
                "ALDI SÜD", "Filiale 0815", "EUR",
                "Kartoffeln 2kg 2,49 A", "Butter 1,99 A", "Mehl 0,59 A", "Haferflocken 0,79 A",
                "Joghurt Natur 0,59 A", "Erdbeeren 2,49 A", "Angebot Lachsfilet 4,99 A",
                "Toilettenpapier 3,95 B", "Spülmittel 0,95 B", "Schokolade Vollmilch 1,19 A",
                "Weißwein Riesling 3,49 B", "Brokkoli 1,49 A", "Zitronen 1,29 A",
                "Summe 26,29", "Kartenzahlung 26,29", "$d",
            )
        }
    }
}
