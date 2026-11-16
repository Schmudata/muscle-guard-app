package de.bonquest.app.domain

import java.text.Normalizer

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
 * Keyword based product classifier for (abbreviated) receipt texts.
 * Primary language: Portuguese (PT/BR). English and German keywords are kept as fallback.
 *
 * Keywords are written in normalized form (see [normalize]): lowercase, no accents
 * (pão -> pao, açúcar -> acucar), German umlauts as ae/oe/ue, ß as ss.
 * The longest matching keyword wins, so "polpa de tomate" beats "tomat".
 * Keywords with a leading/trailing space only match whole words.
 */
object Categorizer {

    val ingredients: List<Ingredient> = listOf(
        // Fruit & veg
        Ingredient("tomate", "Tomatoes", "🍅", Category.OBST_GEMUESE, 7, listOf("tomat", "cherrytom", "rispentom", "strauchtom", "cocktailtom", "romatom", "tomato")),
        Ingredient("zwiebel", "Onions", "🧅", Category.OBST_GEMUESE, 30, listOf("cebola", "chalota", "onion", "zwieb", "schalotte")),
        Ingredient("knoblauch", "Garlic", "🧄", Category.OBST_GEMUESE, 60, listOf("alho", "garlic", "knobl")),
        Ingredient("kartoffel", "Potatoes", "🥔", Category.OBST_GEMUESE, 30, listOf("batata", "potato", "kartof", "drilling")),
        Ingredient("karotte", "Carrots", "🥕", Category.OBST_GEMUESE, 21, listOf("cenoura", "carrot", "karott", "moehre")),
        Ingredient("paprika", "Peppers", "🫑", Category.OBST_GEMUESE, 10, listOf("pimento", "pimentao", "bell pepper", "paprika")),
        Ingredient("gurke", "Cucumber", "🥒", Category.OBST_GEMUESE, 7, listOf("pepino", "cucumber", "gurke")),
        Ingredient("zucchini", "Zucchini", "🥒", Category.OBST_GEMUESE, 7, listOf("curgete", "courgette", "abobrinha", "zucchin")),
        Ingredient("brokkoli", "Broccoli", "🥦", Category.OBST_GEMUESE, 5, listOf("brocol", "couve flor", "broccoli", "brokkoli", "blumenkohl")),
        Ingredient("spinat", "Spinach", "🥬", Category.OBST_GEMUESE, 4, listOf("espinafre", "spinach", "spinat")),
        Ingredient("salat", "Lettuce/Salad", "🥬", Category.OBST_GEMUESE, 5, listOf("alface", "rucula", "salada", "canonigos", "agriao", "mistura de folhas", "lettuce", "eisberg", "kopfsalat", "rucola", "feldsalat")),
        Ingredient("pilze", "Mushrooms", "🍄", Category.OBST_GEMUESE, 5, listOf("cogumelo", "champignon", "mushroom", "champ", "pilze")),
        Ingredient("lauch", "Leek", "🥬", Category.OBST_GEMUESE, 10, listOf("alho frances", "alho porro", "leek", "lauch", "porree")),
        Ingredient("avocado", "Avocado", "🥑", Category.OBST_GEMUESE, 5, listOf("abacate", "avocado")),
        Ingredient("zitrone", "Lemons/Limes", "🍋", Category.OBST_GEMUESE, 21, listOf("limao", "limoes", " lima ", "lemon", "zitrone", "limette")),
        Ingredient("apfel", "Apples", "🍎", Category.OBST_GEMUESE, 21, listOf(" maca ", " macas", "maca gala", "maca fuji", "maca golden", "reineta", "royal gala", "apple", "apfel", "aepfel")),
        Ingredient("banane", "Bananas", "🍌", Category.OBST_GEMUESE, 6, listOf("banan")),
        Ingredient("beeren", "Berries", "🍓", Category.OBST_GEMUESE, 4, listOf("morango", "mirtilo", "framboesa", "frutos vermelhos", "amora", "berries", "strawberr", "erdbeer", "himbeer", "heidelb")),
        Ingredient("orange", "Oranges", "🍊", Category.OBST_GEMUESE, 14, listOf("laranja", "tangerina", "clementina", "mandarina", "orange", "mandarin")),
        Ingredient("trauben", "Grapes", "🍇", Category.OBST_GEMUESE, 7, listOf(" uva", "grape", "traube")),
        Ingredient("ingwer", "Ginger", "🫚", Category.OBST_GEMUESE, 21, listOf("gengibre", "ginger", "ingwer")),
        Ingredient("kraeuter", "Fresh herbs", "🌿", Category.OBST_GEMUESE, 5, listOf("coentro", "salsa fresca", " salsa ", "manjericao", "cebolinho", "hortela", "basil", "parsley", "petersil", "basilikum")),
        // Dairy & eggs
        Ingredient("milch", "Milk", "🥛", Category.MILCH, 7, listOf("leite", "bebida de aveia", "bebida aveia", "bebida vegetal", "bebida de soja", " milk", "milch", "haferdrink")),
        Ingredient("butter", "Butter", "🧈", Category.MILCH, 30, listOf("manteiga", "margarina", "becel", "butter", "margarine")),
        Ingredient("kaese", "Cheese", "🧀", Category.MILCH, 21, listOf("queijo", "flamengo", "emmental", "gouda", "edam", "cheddar", "cheese", "kaese")),
        Ingredient("mozzarella", "Mozzarella", "🧀", Category.MILCH, 10, listOf("mozzarella", "queijo mozzarella", "burrata")),
        Ingredient("parmesan", "Parmesan", "🧀", Category.MILCH, 60, listOf("parmesao", "queijo parmesao", "parmigiano", "grana padano", "parmesan", "pecorino")),
        Ingredient("feta", "Feta", "🧀", Category.MILCH, 14, listOf("queijo feta", "feta")),
        Ingredient("joghurt", "Yoghurt", "🥣", Category.MILCH, 14, listOf("iogurte", "iogurte ", "iogurte de", "iog ", "skyr", "yogurt", "yoghurt", "joghurt", "jogurt")),
        Ingredient("quark", "Quark", "🥣", Category.MILCH, 10, listOf("quark", "queijo quark")),
        Ingredient("sahne", "Cream", "🥛", Category.MILCH, 10, listOf("natas", " nata ", "creme culinario", "chantilly", "cream", "sahne")),
        Ingredient("frischkaese", "Cream cheese", "🧀", Category.MILCH, 14, listOf("queijo fresco", "queijo creme", "requeijao", "philadelphia", "cream cheese", "frischkaese")),
        Ingredient("eier", "Eggs", "🥚", Category.MILCH, 21, listOf("ovos", " ovo ", " egg", "eier", " ei ")),
        // Meat & fish
        Ingredient("gefluegel", "Chicken/Turkey", "🍗", Category.FLEISCH_FISCH, 2, listOf("frango", "peito de frango", " peru ", "galinha", " aves", "chicken", "turkey", "haehn", "pute")),
        Ingredient("hackfleisch", "Minced meat", "🥩", Category.FLEISCH_FISCH, 1, listOf("carne picada", "picado", "hamburguer", "mince", "hackfleisch", "hack ")),
        Ingredient("rind", "Beef", "🥩", Category.FLEISCH_FISCH, 3, listOf("vaca", "novilho", " bife", "vitela", " acem", "beef", "steak", "rind")),
        Ingredient("schwein", "Pork", "🥩", Category.FLEISCH_FISCH, 3, listOf("porco", "febras", "costeleta", "lombo", "entremeada", "bifana", "pork", "schwein", "schnitzel")),
        Ingredient("wurst", "Ham & sausages", "🥓", Category.FLEISCH_FISCH, 10, listOf("fiambre", "presunto", "chourico", "salsicha", "mortadela", "bacon", "paio", "linguica", "alheira", "salpicao", "toucinho", " ham ", "sausage", "wurst", "salami", "schinken")),
        Ingredient("fisch", "Fish", "🐟", Category.FLEISCH_FISCH, 2, listOf("salmao", "peixe", "pescada", "bacalhau", "dourada", "robalo", "sardinha", "camarao", "polvo", "lulas", "salmon", "fish", "lachs", "fisch")),
        // Bakery
        Ingredient("brot", "Bread", "🍞", Category.BACKWAREN, 4, listOf(" pao", "pao ", "pao de",  "carcaca", " broa", "baguete", "croissant", "papo seco", "bread", "brot", "broetchen")),
        Ingredient("toast", "Sliced bread", "🍞", Category.BACKWAREN, 7, listOf("pao de forma", "pao forma", "toast")),
        Ingredient("wraps", "Wraps", "🌯", Category.BACKWAREN, 14, listOf("tortilha", "tortilla", "wrap")),
        // Pantry staples
        Ingredient("nudeln", "Pasta", "🍝", Category.VORRAT, 365, listOf("massa", "esparguete", "spaghet", "macarrao", "penne", "fusilli", "lasanha", "lasagn", "tagliatel", "cotovelos", "pasta", "nudel")),
        Ingredient("reis", "Rice", "🍚", Category.VORRAT, 365, listOf("arroz", " rice", "basmati", "risotto", "reis ")),
        Ingredient("mehl", "Flour", "🌾", Category.VORRAT, 300, listOf("farinha", "flour", "mehl")),
        Ingredient("zucker", "Sugar", "🍬", Category.VORRAT, 720, listOf("acucar", "sugar", "zucker")),
        Ingredient("oel", "Oil", "🫒", Category.VORRAT, 365, listOf("azeite", "oleo", "olive oil", " oil", " oel", "olivenoel")),
        Ingredient("haferflocken", "Oats/Cereal", "🥣", Category.VORRAT, 300, listOf("flocos de aveia", "aveia", "cereais", "granola", "muesli", "oats", "cornflakes", "haferfl")),
        Ingredient("linsen", "Lentils/Beans", "🫘", Category.VORRAT, 365, listOf("lentilha", "grao de bico", " grao", "feijao", "ervilha", "lentil", "beans", "chickpea", "linsen", "bohnen")),
        Ingredient("mais", "Sweetcorn", "🌽", Category.VORRAT, 365, listOf(" milho", "sweetcorn", " mais")),
        Ingredient("tomatenkonserve", "Canned tomatoes/Passata", "🥫", Category.VORRAT, 365, listOf("polpa de tomate", "polpa tomate", "tomate pelado", "tomate triturado", "concentrado de tomate", "concentrado tomate", "molho de tomate", "molho tomate", "massa de tomate", "tomate frito", "passata", "chopped tomatoes", "tomatenmark", "passierte")),
        Ingredient("kokosmilch", "Coconut milk", "🥥", Category.VORRAT, 365, listOf("leite de coco", "leite coco", "coconut milk", "kokosmilch")),
        Ingredient("bruehe", "Stock", "🍲", Category.VORRAT, 365, listOf("caldo", "caldo de", "caldo knorr", "stock cube", "bouillon", "bruehe")),
        Ingredient("thunfisch", "Canned tuna", "🐟", Category.VORRAT, 720, listOf("atum", "atum em", "atum posta", "atum natural", "tuna", "thunfisch")),
        Ingredient("honig", "Honey", "🍯", Category.VORRAT, 720, listOf(" mel ", "honey", "honig")),
        Ingredient("marmelade", "Jam", "🍓", Category.VORRAT, 180, listOf("doce de", "compota", "geleia", "marmelada", " jam", "marmelade", "konfituere")),
        Ingredient("kaffee", "Coffee", "☕", Category.GETRAENKE, 180, listOf(" cafe", "capsulas", "nescafe", "coffee", "espresso", "kaffee")),
        Ingredient("tee", "Tea", "🍵", Category.GETRAENKE, 365, listOf(" cha ", "infusao", " tea ", " tee")),
        Ingredient("saft", "Juice", "🧃", Category.GETRAENKE, 10, listOf("sumo", "sumo de", "sumo de laranja", "sumo laranja", "sumo maca", "suco de", "nectar de", "nectar", "suco", "juice", "saft")),
    )

    private val categoryKeywords: Map<Category, List<String>> = mapOf(
        Category.GETRAENKE to listOf("agua", "coca cola", "refrigerante", "ice tea", "sumol", "pepsi", "tonica", "gaseificada", "agua das pedras", "luso", "water", "cola", "lemonade", "energy", "red bull", "wasser", "limo", "eistee", "schorle"),
        Category.ALKOHOL to listOf("cerveja", "vinho", "super bock", "sagres", "espumante", "licor", "aguardente", "sangria", "tinto", "whisky", "vodka", " gin ", " rum ", "beer", "wine", "bier", "wein", "sekt", "prosecco"),
        Category.SNACKS to listOf("chocolate", "bolacha", "biscoito", "batata frita", "batatas fritas", "gomas", "rebucado", " bolo", "pastel de nata", "donut", "snack", "pipocas", "amendoim", "frutos secos", "waffle", "nutella", "chips", "cookie", "candy", "schoko", "keks", "gummi", "haribo"),
        Category.TIEFKUEHL to listOf("congelad", "gelado de", "ultracongelad", "gelado", "pizza", "douradinhos", "frozen", "ice cream", "tiefk", "pommes"),
        Category.HAUSHALT to listOf("detergente", "papel higienico", "rolo de cozinha", "rolo cozinha", "guardanapo", "pasta de dentes", "pasta dentes", "dentifrico", "escova de dentes", "champo", "gel de banho", "gel banho", "desodorizante", "sabonete", "sabao", "sacos do lixo", "sacos lixo", "esponja", "lenco", "fraldas", "lixivia", "amaciador", "pastilhas", "papel aluminio", "pelicula aderente", "comida gato", "comida cao", "racao", "pilhas", "toilet paper", "detergent", "shampoo", "soap", "toothpaste", "klopapier", "spuelmittel", "waschmittel", "duschgel", "zahnpasta"),
        Category.PFAND to listOf("vasilhame", " tara ", "deposit", "pfand", "leergut"),
        Category.VORRAT to listOf("conserva", " lata", "ketchup", "mostarda", "maionese", "vinagre", " sal ", "pimenta", "molho", "especiarias", "fermento", "cacau", " sopa", "manteiga de amendoim", "pesto", "sauce", "salt", "senf", "essig", "gewuerz"),
        Category.FLEISCH_FISCH to listOf("carne", "talho", "kebab", "meat", "fleisch"),
        Category.MILCH to listOf("pudim", "sobremesa", "dessert", "pudding", "kefir"),
        Category.OBST_GEMUESE to listOf("fruta", "legumes", "hortalica", "kiwi", "manga", "ananas", "melao", "melancia", " pera", "pessego", "nabo", "couve", "beringela", "abobora", "batata doce", "espargo", "aipo", "beterraba", "rabanete", " figo", "ameixa", "fruit", "veg", "obst", "gemuese"),
    )

    private val byKey = ingredients.associateBy { it.key }

    fun ingredient(key: String?): Ingredient? = key?.let { byKey[it] }

    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(text: String): String {
        val german = text.lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        return Normalizer.normalize(german, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

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
            val ing = bestIngredient?.let { byKey[it] }
            return Classification(cat, if (ing?.category == cat) ing.key else null)
        }
        return Classification(bestCategory, bestIngredient)
    }
}
