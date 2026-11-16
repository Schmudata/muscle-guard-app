package de.bonquest.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Sample Portuguese receipts in the shape OCR returns them, to try the app without a real receipt. */
object DemoData {

    fun demoRows(variant: Int, today: LocalDate = LocalDate.now()): List<String> = when (variant % 3) {
        0 -> {
            val d = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            listOf(
                "Continente Modelo Continente Hipermercados S.A.", "NIF 502011475", "Rua das Flores 12, Lisboa",
                "ESPARGUETE CONTINENTE 500G 0,69 C", "POLPA DE TOMATE 0,89 C", "CEBOLA 1KG 1,29 C", "ALHO 0,59 C",
                "CARNE PICADA MISTA 500G 3,99 C", "Desconto Imediato -0,80", "LEITE MEIO GORDO 1L 1,70 A", "2 X 0,85",
                "OVOS CLASSE M 12UN 2,79 C", "CENOURA 0,99 C", "QUEIJO PARMESAO RALADO 2,49 C",
                "Poupança Cartão Continente -0,50", "BATATAS FRITAS LAYS 1,99 D", "COCA COLA 1,5L 1,79 D",
                "TOTAL A PAGAR 17,90", "Multibanco 17,90", "IVA 6% 0,62", "Data: $d 18:42", "Obrigado pela sua visita",
            )
        }
        1 -> {
            val d = today.minusDays(4).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            listOf(
                "PINGO DOCE", "Pingo Doce - Distribuição Alimentar S.A.", "Contribuinte: 500829993",
                "(C) BANANA 1,19", "0,850 kg x 1,40 €/kg", "(C) TOMATE CHERRY 1,99", "(C) QUEIJO MOZZARELLA 0,99",
                "(C) QUEIJO MOZZARELLA 0,99", "Poupança -0,30", "(C) PEPINO 0,69", "(C) QUEIJO FETA 2,29",
                "(C) ARROZ BASMATI 1,89", "(A) LEITE DE COCO 1,49", "(C) PEITO DE FRANGO 4,99", "Desconto Promoção -1,00",
                "(C) PIMENTO VERMELHO 0,99", "(C) PÃO DE MISTURA 1,39",
                "TOTAL 17,59", "Numerário 20,00", "Troco 2,41", "Total Poupança 1,30", "$d 11:03",
            )
        }
        else -> {
            val d = today.toString() // yyyy-MM-dd
            listOf(
                "Lidl & Companhia", "NIF 503340855", "EUR",
                "Batata Branca 2kg 1,99 A", "Manteiga com Sal 2,29 A", "Farinha Trigo 0,79 A", "Flocos de Aveia 0,99 A",
                "Iogurte Natural 0,45 A", "Morangos 2,49 A", "Promo Salmão Fresco 5,49 A", "Papel Higiénico 3,99 B",
                "Detergente Loiça 1,29 B", "Chocolate de Leite 0,99 A", "Vinho Tinto Douro 3,49 C", "Brócolos 1,49 A",
                "Limões 1,19 A", "Total 26,93", "Cartão Multibanco 26,93", "$d 19:20",
            )
        }
    }
}
