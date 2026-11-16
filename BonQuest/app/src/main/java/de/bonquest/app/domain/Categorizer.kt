package de.bonquest.app.domain

data class Ingredient(
    val key: String,
    val label: String,
    val emoji: String,
    val category: Category,
    val shelfDays: Int,
    val keywords: List<String>,
)

data class Classification(val category: Category, val ingredient: String?)

/**
 * Keyword based product classifier tuned for (abbreviated) German receipt texts.
 * Keywords are written in normalized form: lowercase, ä->ae, ö->oe, ü->ue, ß->ss.
 * The longest matching keyword wins, so "tomatenmark" beats "tomat".
 */
object Categorizer {

    val ingredients: List<Ingredient> = listOf(
        // Obst & Gemüse
        Ingredient("tomate", "Tomaten", "🍅", Category.OBST_GEMUESE, 7, listOf("tomat", "cherrytom", "rispentom", "strauchtom", "cocktailtom", "romatom")),
        Ingredient("zwiebel", "Zwiebeln", "🧅", Category.OBST_GEMUESE, 30, listOf("zwieb", "schalotte", "gemuesezwiebel")),
        Ingredient("knoblauch", "Knoblauch", "🧄", Category.OBST_GEMUESE, 60, listOf("knobl")),
        Ingredient("kartoffel", "Kartoffeln", "🥔", Category.OBST_GEMUESE, 30, listOf("kartof", "drilling", "speisekart")),
        Ingredient("karotte", "Karotten", "🥕", Category.OBST_GEMUESE, 21, listOf("karott", "moehre", "mohrrueb")),
        Ingredient("paprika", "Paprika", "🫑", Category.OBST_GEMUESE, 10, listOf("paprika", "spitzpaprika")),
        Ingredient("gurke", "Gurke", "🥒", Category.OBST_GEMUESE, 7, listOf("gurke", "salatgurke", "minigurke")),
        Ingredient("zucchini", "Zucchini", "🥒", Category.OBST_GEMUESE, 7, listOf("zucchin")),
        Ingredient("brokkoli", "Brokkoli", "🥦", Category.OBST_GEMUESE, 5, listOf("brokkoli", "broccoli", "blumenkohl")),
        Ingredient("spinat", "Spinat", "🥬", Category.OBST_GEMUESE, 4, listOf("spinat", "blattspinat", "babyspinat")),
        Ingredient("salat", "Salat", "🥬", Category.OBST_GEMUESE, 5, listOf("eisberg", "kopfsalat", "rucola", "feldsalat", "salatherz", "romana", "salatmix", "lollo")),
        Ingredient("pilze", "Pilze", "🍄", Category.OBST_GEMUESE, 5, listOf("champ", "pilze", "pilz ", "kraeuterseitl")),
        Ingredient("lauch", "Lauch", "🥬", Category.OBST_GEMUESE, 10, listOf("lauch", "porree", "fruehlingszw")),
        Ingredient("avocado", "Avocado", "🥑", Category.OBST_GEMUESE, 5, listOf("avocado")),
        Ingredient("zitrone", "Zitronen", "🍋", Category.OBST_GEMUESE, 21, listOf("zitrone", "limette")),
        Ingredient("apfel", "Äpfel", "🍎", Category.OBST_GEMUESE, 21, listOf("apfel", "aepfel", "elstar", "braeburn", "pink lady", "jonagold", "gala ", "granny")),
        Ingredient("banane", "Bananen", "🍌", Category.OBST_GEMUESE, 6, listOf("banan")),
        Ingredient("beeren", "Beeren", "🍓", Category.OBST_GEMUESE, 4, listOf("erdbeer", "heidelb", "himbeer", "blaubeer", "beeren")),
        Ingredient("orange", "Orangen", "🍊", Category.OBST_GEMUESE, 14, listOf("orange", "mandarin", "clementin")),
        Ingredient("trauben", "Trauben", "🍇", Category.OBST_GEMUESE, 7, listOf("traube")),
        Ingredient("ingwer", "Ingwer", "🫚", Category.OBST_GEMUESE, 21, listOf("ingwer")),
        Ingredient("kraeuter", "Kräuter", "🌿", Category.OBST_GEMUESE, 5, listOf("petersil", "basilikum", "schnittlauch", "koriander", "dill ")),
        // Milch & Eier
        Ingredient("milch", "Milch", "🥛", Category.MILCH, 7, listOf("milch", "h milch", "vollmilch", "frischmilch", "hafer drink", "haferdrink", "barista")),
        Ingredient("butter", "Butter", "🧈", Category.MILCH, 30, listOf("butter", "kerrygold", "margarine", "rama ")),
        Ingredient("kaese", "Käse", "🧀", Category.MILCH, 21, listOf("kaese", "gouda", "emmentaler", "edamer", "cheddar", "leerdammer", "gratin", "scheibl", "tilsiter", "butterkaese")),
        Ingredient("mozzarella", "Mozzarella", "🧀", Category.MILCH, 10, listOf("mozz", "burrata")),
        Ingredient("parmesan", "Parmesan", "🧀", Category.MILCH, 60, listOf("parmes", "grana padano", "pecorino", "hartkaese")),
        Ingredient("feta", "Feta", "🧀", Category.MILCH, 14, listOf("feta", "hirtenkaese", "schafskaese", "salatkaese")),
        Ingredient("joghurt", "Joghurt", "🥣", Category.MILCH, 14, listOf("joghurt", "jogurt", "joghu", "skyr", "erdbeerjoghurt", "fruchtjoghurt")),
        Ingredient("quark", "Quark", "🥣", Category.MILCH, 10, listOf("quark")),
        Ingredient("sahne", "Sahne", "🥛", Category.MILCH, 10, listOf("sahne", "schmand", "creme fraiche", "cremefine", "kochcreme")),
        Ingredient("frischkaese", "Frischkäse", "🧀", Category.MILCH, 14, listOf("frischkaese", "philadelphia", "exquisa", "frischk")),
        Ingredient("eier", "Eier", "🥚", Category.MILCH, 21, listOf(" ei ", "eier", " eier", "freiland", "bodenhaltung")),
        // Fleisch & Fisch
        Ingredient("gefluegel", "Hähnchen/Pute", "🍗", Category.FLEISCH_FISCH, 2, listOf("haehn", "haenchen", "huhn", "chicken", "pute", "gefluegel")),
        Ingredient("hackfleisch", "Hackfleisch", "🥩", Category.FLEISCH_FISCH, 1, listOf("hack", "gemischt hack", "hackfleisch", "rinderhack")),
        Ingredient("rind", "Rindfleisch", "🥩", Category.FLEISCH_FISCH, 3, listOf("rind", "rindfleisch", "steak", "gulasch", "roastbeef")),
        Ingredient("schwein", "Schweinefleisch", "🥩", Category.FLEISCH_FISCH, 3, listOf("schwein", "schweinefleisch", "schnitzel", "kotelett", "nacken", "geschnetz")),
        Ingredient("wurst", "Wurst & Schinken", "🥓", Category.FLEISCH_FISCH, 10, listOf("wurst", "salami", "schinken", "mortadella", "wiener", "lyoner", "aufschnitt", "speck", "bacon", "wuerstch", "chorizo")),
        Ingredient("fisch", "Fisch", "🐟", Category.FLEISCH_FISCH, 2, listOf("lachs", "fisch", "forelle", "garnele", "kabeljau", "seelachs", "shrimps")),
        // Backwaren
        Ingredient("brot", "Brot", "🍞", Category.BACKWAREN, 4, listOf("brot", "broetchen", "baguette", "ciabatta", "laugen", "brezel", "breze", "croissant", "semmel", "bagel", "kaiserbr")),
        Ingredient("toast", "Toastbrot", "🍞", Category.BACKWAREN, 7, listOf("toast", "sandwich")),
        Ingredient("wraps", "Wraps", "🌯", Category.BACKWAREN, 14, listOf("wrap", "tortilla")),
        // Vorrat
        Ingredient("nudeln", "Nudeln", "🍝", Category.VORRAT, 365, listOf("nudel", "spaghet", "penne", "fusilli", "farfalle", "tagliat", "makkaroni", "maccheroni", "lasagne", "pasta", "spirelli", "rigatoni", "linguine", "tortellini", "gnocchi")),
        Ingredient("reis", "Reis", "🍚", Category.VORRAT, 365, listOf("reis ", " reis", "basmati", "jasmin", "risotto", "langkorn", "milchreis")),
        Ingredient("mehl", "Mehl", "🌾", Category.VORRAT, 300, listOf("mehl")),
        Ingredient("zucker", "Zucker", "🍬", Category.VORRAT, 720, listOf("zucker")),
        Ingredient("oel", "Öl", "🫒", Category.VORRAT, 365, listOf(" oel", "olivenoel", "rapsoel", "sonnenblumenoel", "speiseoel")),
        Ingredient("haferflocken", "Haferflocken/Müsli", "🥣", Category.VORRAT, 300, listOf("haferfl", "muesli", "porridge", "cornflakes", "granola", "flocken")),
        Ingredient("linsen", "Linsen/Bohnen", "🫘", Category.VORRAT, 365, listOf("linsen", "kichererb", "bohnen", "kidney")),
        Ingredient("mais", "Mais", "🌽", Category.VORRAT, 365, listOf(" mais", "gemuesemais", "zuckermais")),
        Ingredient("tomatenkonserve", "Dosentomaten/Passata", "🥫", Category.VORRAT, 365, listOf("passata", "pizzatomat", "gehackte tomat", "tomaten stueck", "dosentomat", "tomatenmark", "tomatenpuere", "polpa")),
        Ingredient("kokosmilch", "Kokosmilch", "🥥", Category.VORRAT, 365, listOf("kokosmilch", "kokosnussmilch")),
        Ingredient("bruehe", "Brühe", "🍲", Category.VORRAT, 365, listOf("bruehe", "fond", "bouillon")),
        Ingredient("thunfisch", "Thunfisch (Dose)", "🐟", Category.VORRAT, 720, listOf("thunfisch")),
        Ingredient("honig", "Honig", "🍯", Category.VORRAT, 720, listOf("honig")),
        Ingredient("marmelade", "Marmelade", "🍓", Category.VORRAT, 180, listOf("marmelade", "konfituere", "fruchtaufstr")),
        Ingredient("kaffee", "Kaffee", "☕", Category.GETRAENKE, 180, listOf("kaffee", "espresso", "cappuccino", "crema", "kaffeepad")),
        Ingredient("tee", "Tee", "🍵", Category.GETRAENKE, 365, listOf(" tee", "teebeutel", "kraeutertee", "fruechtetee")),
        Ingredient("saft", "Saft", "🧃", Category.GETRAENKE, 10, listOf("saft", "nektar", "smoothie")),
    )

    private val categoryKeywords: Map<Category, List<String>> = mapOf(
        Category.GETRAENKE to listOf("wasser", "mineralw", "sprudel", "cola", "fanta", "sprite", "limo", "eistee", "energy", "red bull", "mate", "schorle", "spezi", "tonic", "gerolsteiner", "volvic", "vio "),
        Category.ALKOHOL to listOf("bier", "pils", "wein", "riesling", "merlot", "sekt", "prosecco", "vodka", "wodka", "whisky", "rum ", "gin ", "radler", "weissbier", "helles", "aperol", "likoer", "lambrusco", "rose "),
        Category.SNACKS to listOf("schoko", "schokolade", "chips", "keks", "gummi", "haribo", "bonbon", "praline", "riegel", "snickers", "twix", "kinder", "cracker", "flips", "popcorn", "kuchen", "waffel", "nutella", "milka", "ritter sport", "lindt", "erdnuss", "nuss", "mars ", "bounty", "oreo", "toffifee", "duplo", "salzstangen", "brezeln", "lakritz", "donut", "muffin", "butterkeks", "milchschnitte", "milchschoko"),
        Category.TIEFKUEHL to listOf("tiefk", " tk ", "pizza", "fischstaeb", "pommes", "rahmspinat", "eiscreme", "speiseeis", "magnum", "ben jerry", "kroketten", "rahmgemuese", "tk "),
        Category.HAUSHALT to listOf("spuel", "waschmittel", "toilettenp", "klopapier", "kuechenrolle", "kuechentuch", "zahnpasta", "zahncreme", "zahnbuerste", "shampoo", "duschgel", "deo", "seife", "muellbeutel", "schwamm", "tempo", "taschent", "windel", "tabs", "reiniger", "alufolie", "frischhaltef", "backpapier", "weichspueler", "spuelmaschin", "rasier", "binden", "tampon", "wattepad", "creme ", "bodylotion", "batterie", "kerze", "nivea", "colgate", "fairy", "persil", "ariel", "katzenfutter", "hundefutter", "tiernahr"),
        Category.PFAND to listOf("pfand", "leergut", "einweg", "mehrweg"),
        Category.VORRAT to listOf("konserve", "dose", "ketchup", "senf", "mayo", "essig", "gewuerz", "salz", "pfeffer", "sauce", "sosse", "pesto", "suppe", "backpulver", "hefe", "vanille", "kakao", "erdnussbutter", "ravioli"),
        Category.BACKWAREN to listOf("baeck", "kuchen", "plunder", "berliner", "zwieback"),
        Category.FLEISCH_FISCH to listOf("fleisch", "frikadelle", "leberkaese", "gyros", "doener", "burger patty"),
        Category.MILCH to listOf("pudding", "kefir", "buttermilch", "dessert", "milchreis"),
        Category.OBST_GEMUESE to listOf("obst", "gemuese", "bio ", "kohl", "sellerie", "kiwi", "mango", "ananas", "melone", "birne", "nektarine", "pfirsich", "radieschen", "rote bete", "aubergine", "suesskart", "kuerbis", "fenchel", "spargel"),
    )

    private val byKey = ingredients.associateBy { it.key }

    fun ingredient(key: String?): Ingredient? = key?.let { byKey[it] }

    fun normalize(text: String): String = text.lowercase()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun classify(name: String, learned: Map<String, Category> = emptyMap()): Classification {
        val norm = normalize(name)
        val padded = " $norm "
        var bestLen = 0
        var bestCategory = Category.SONSTIGES
        var bestIngredient: String? = null
        for (ing in ingredients) {
            for (kw in ing.keywords) {
                if (kw.length > bestLen && padded.contains(kw)) {
                    bestLen = kw.length; bestCategory = ing.category; bestIngredient = ing.key
                }
            }
        }
        for ((cat, kws) in categoryKeywords) {
            for (kw in kws) {
                if (kw.length > bestLen && padded.contains(kw)) {
                    bestLen = kw.length; bestCategory = cat; bestIngredient = null
                }
            }
        }
        learned[norm]?.let { cat ->
            // keep the ingredient only if it still fits the corrected category
            val ing = bestIngredient?.let { byKey[it] }
            return Classification(cat, if (ing?.category == cat) ing.key else null)
        }
        return Classification(bestCategory, bestIngredient)
    }
}
