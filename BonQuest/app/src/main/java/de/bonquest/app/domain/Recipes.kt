package de.bonquest.app.domain

data class Recipe(
    val id: String,
    val name: String,
    val emoji: String,
    val minutes: Int,
    /** Each entry is an ingredient key; alternatives separated by '|'. */
    val ingredients: List<String>,
    val steps: List<String>,
)

data class RecipeSuggestion(
    val recipe: Recipe,
    val have: List<String>,
    val missing: List<String>,
    val usedKeys: List<String>,
    val expiringUsed: Int,
    val score: Double,
) {
    val matchPercent: Int get() = (100 * have.size / recipe.ingredients.size.coerceAtLeast(1))
}

object Recipes {

    val all: List<Recipe> = listOf(
        Recipe("bolo", "Spaghetti Bolognese", "🍝", 35, listOf("nudeln", "hackfleisch", "tomatenkonserve|tomate", "zwiebel", "knoblauch", "karotte"),
            listOf("Zwiebel, Knoblauch und Karotte fein würfeln.", "Hackfleisch scharf anbraten, Gemüse dazugeben.", "Tomaten zugeben, 20 Min. köcheln, würzen.", "Nudeln kochen und mit der Soße servieren.")),
        Recipe("pomodoro", "Pasta Pomodoro", "🍅", 20, listOf("nudeln", "tomate|tomatenkonserve", "knoblauch", "oel", "parmesan|kaese"),
            listOf("Nudeln kochen.", "Knoblauch in Öl anschwitzen, Tomaten zugeben und einkochen.", "Mit Nudeln mischen, Käse drüber.")),
        Recipe("shakshuka", "Shakshuka", "🍳", 25, listOf("eier", "tomatenkonserve|tomate", "paprika", "zwiebel", "knoblauch", "brot"),
            listOf("Zwiebel, Paprika, Knoblauch anbraten.", "Tomaten dazu, 10 Min. köcheln, würzen (Kreuzkümmel, Paprika).", "Mulden formen, Eier hineinschlagen, zugedeckt stocken lassen.", "Mit Brot servieren.")),
        Recipe("omelett", "Gemüse-Omelett", "🥚", 15, listOf("eier", "paprika|tomate|pilze|spinat", "kaese", "zwiebel"),
            listOf("Gemüse klein schneiden und anbraten.", "Eier verquirlen, würzen und darübergießen.", "Käse darüber streuen, zuklappen.")),
        Recipe("ruehrei", "Rührei auf Toast", "🍞", 10, listOf("eier", "toast|brot", "butter", "kraeuter|tomate"),
            listOf("Eier verquirlen, in Butter langsam stocken lassen.", "Brot toasten, Rührei darauf, mit Kräutern/Tomate toppen.")),
        Recipe("ofenkartoffel", "Ofenkartoffeln mit Kräuterquark", "🥔", 45, listOf("kartoffel", "quark", "knoblauch", "oel", "kraeuter|zwiebel"),
            listOf("Kartoffeln halbieren, mit Öl und Salz bei 200 °C 35 Min. backen.", "Quark mit Knoblauch, Kräutern, Salz verrühren.", "Zusammen servieren.")),
        Recipe("kartoffelsuppe", "Kartoffelsuppe", "🍲", 40, listOf("kartoffel", "karotte", "lauch|zwiebel", "bruehe", "sahne", "wurst"),
            listOf("Gemüse würfeln und anschwitzen.", "Mit Brühe aufgießen, 20 Min. kochen.", "Teilweise pürieren, Sahne dazu, Würstchen in Scheiben ergänzen.")),
        Recipe("curry", "Hähnchen-Curry", "🍛", 30, listOf("gefluegel", "kokosmilch", "reis", "paprika|brokkoli|zucchini", "zwiebel", "knoblauch|ingwer"),
            listOf("Reis kochen.", "Hähnchen würfeln und anbraten, Zwiebel, Knoblauch, Gemüse dazu.", "Currypaste/-pulver einrühren, mit Kokosmilch ablöschen, 10 Min. köcheln.")),
        Recipe("gemuesepfanne", "Bunte Gemüsepfanne mit Reis", "🥘", 25, listOf("reis", "paprika", "zucchini|brokkoli", "karotte", "zwiebel", "oel"),
            listOf("Reis kochen.", "Gemüse in Streifen schneiden und in Öl kräftig braten.", "Mit Sojasoße oder Gewürzen abschmecken.")),
        Recipe("greek", "Griechischer Salat", "🥗", 10, listOf("gurke", "tomate", "feta", "zwiebel", "paprika", "oel"),
            listOf("Gemüse grob schneiden.", "Feta darüber bröseln.", "Mit Öl, Salz, Oregano anmachen.")),
        Recipe("caprese", "Caprese", "🍅", 5, listOf("tomate", "mozzarella", "oel", "kraeuter"),
            listOf("Tomaten und Mozzarella in Scheiben schneiden, abwechselnd anrichten.", "Mit Öl, Salz, Pfeffer und Basilikum toppen.")),
        Recipe("pfannkuchen", "Pfannkuchen", "🥞", 20, listOf("mehl", "eier", "milch", "zucker", "butter|oel"),
            listOf("Mehl, Eier, Milch, Prise Zucker zu glattem Teig rühren.", "In heißer Pfanne dünne Pfannkuchen ausbacken.", "Süß oder herzhaft füllen.")),
        Recipe("oats", "Overnight Oats", "🥣", 5, listOf("haferflocken", "milch|joghurt", "banane|beeren|apfel", "honig"),
            listOf("Haferflocken mit Milch/Joghurt mischen.", "Über Nacht kühlen.", "Mit Obst und Honig toppen.")),
        Recipe("bananenbrot", "Bananenbrot", "🍌", 60, listOf("banane", "mehl", "eier", "zucker|honig", "butter|oel"),
            listOf("Überreife Bananen zerdrücken.", "Mit Eiern, Zucker, Fett und Mehl (+ Backpulver) verrühren.", "Bei 175 °C ca. 50 Min. backen.")),
        Recipe("chili", "Chili sin Carne", "🌶️", 35, listOf("linsen", "mais", "tomatenkonserve", "zwiebel", "paprika", "reis|wraps"),
            listOf("Zwiebel und Paprika anbraten.", "Bohnen/Linsen, Mais und Tomaten zugeben.", "Mit Chili, Kreuzkümmel würzen, 20 Min. köcheln.")),
        Recipe("lachs", "Lachs mit Ofengemüse", "🐟", 30, listOf("fisch", "brokkoli|zucchini|paprika", "kartoffel", "zitrone", "oel"),
            listOf("Kartoffeln und Gemüse würfeln, mit Öl bei 200 °C 15 Min. vorbacken.", "Lachs dazulegen, weitere 12 Min. garen.", "Mit Zitrone servieren.")),
        Recipe("risotto", "Pilzrisotto", "🍄", 35, listOf("reis", "pilze", "zwiebel", "parmesan|kaese", "bruehe", "butter"),
            listOf("Zwiebel in Butter glasig dünsten, Reis kurz mitrösten.", "Nach und nach Brühe angießen und rühren.", "Gebratene Pilze und Parmesan unterheben.")),
        Recipe("wrap", "Spinat-Feta-Wraps", "🌯", 15, listOf("wraps", "spinat|salat", "feta|frischkaese", "tomate", "gurke|paprika"),
            listOf("Spinat kurz andünsten (oder Salat frisch verwenden).", "Wraps mit Käse, Gemüse belegen.", "Einrollen und in der Pfanne kurz anrösten.")),
        Recipe("toastie", "Käse-Schinken-Toast", "🥪", 10, listOf("toast|brot", "wurst", "kaese", "butter"),
            listOf("Toast mit Butter bestreichen.", "Mit Schinken und Käse belegen.", "In der Pfanne oder im Ofen goldbraun backen.")),
        Recipe("auflauf", "Nudelauflauf", "🧀", 40, listOf("nudeln", "sahne|milch", "kaese", "brokkoli|spinat|paprika", "wurst|eier"),
            listOf("Nudeln vorkochen.", "Mit Gemüse und Schinken in eine Form geben.", "Sahne-Ei-Guss darüber, Käse obendrauf, 20 Min. bei 200 °C.")),
        Recipe("dal", "Linsen-Dal", "🫘", 30, listOf("linsen", "kokosmilch", "tomatenkonserve|tomate", "zwiebel", "knoblauch|ingwer", "reis"),
            listOf("Zwiebel, Knoblauch, Ingwer anschwitzen, Curry dazu.", "Linsen, Tomaten, Kokosmilch und Wasser zugeben.", "20 Min. köcheln, mit Reis servieren.")),
        Recipe("schnitzel", "Schnitzel mit Kartoffelsalat", "🥩", 45, listOf("schwein|gefluegel", "kartoffel", "eier", "mehl", "gurke", "zwiebel"),
            listOf("Kartoffeln kochen, schneiden, mit Zwiebel, Gurke, Brühe/Essig anmachen.", "Schnitzel in Mehl, Ei und Paniermehl wenden.", "Goldbraun ausbraten.")),
        Recipe("bowl", "Frühstücks-Bowl", "🍓", 5, listOf("joghurt|quark", "beeren|banane|apfel", "haferflocken", "honig"),
            listOf("Joghurt in eine Schale geben.", "Mit Obst, Flocken und Honig toppen.")),
        Recipe("avotoast", "Avocado-Toast mit Ei", "🥑", 10, listOf("brot|toast", "avocado", "zitrone", "eier"),
            listOf("Avocado mit Zitrone, Salz zerdrücken.", "Auf geröstetes Brot streichen.", "Spiegel- oder pochiertes Ei obendrauf.")),
        Recipe("quiche", "Lauch-Quiche", "🥧", 60, listOf("mehl", "butter", "eier", "sahne", "lauch|spinat|brokkoli", "kaese"),
            listOf("Mehl, Butter, Ei, Prise Salz zu Mürbteig kneten, in Form drücken.", "Gemüse andünsten, auf den Teig geben.", "Eier mit Sahne verquirlen, darüber, Käse drauf, 35 Min. bei 180 °C.")),
        Recipe("burgerbowl", "Burger-Bowl", "🍔", 20, listOf("hackfleisch", "salat", "tomate", "gurke", "kaese", "zwiebel"),
            listOf("Hackfleisch krümelig braten und würzen.", "Salat, Tomate, Gurke schneiden.", "Alles in Schüsseln anrichten, Käse und Soße drüber.")),
        Recipe("tomatensuppe", "Tomatensuppe", "🥣", 25, listOf("tomatenkonserve|tomate", "zwiebel", "knoblauch", "sahne", "bruehe", "brot"),
            listOf("Zwiebel und Knoblauch anschwitzen.", "Tomaten und Brühe zugeben, 15 Min. köcheln.", "Pürieren, Sahne einrühren, mit Brot servieren.")),
        Recipe("obstsalat", "Obstsalat", "🍇", 10, listOf("apfel", "banane", "orange|trauben|beeren", "zitrone", "joghurt|honig"),
            listOf("Obst klein schneiden.", "Mit Zitronensaft und etwas Honig mischen.", "Optional mit Joghurt servieren.")),
        Recipe("thunfischpasta", "Thunfisch-Pasta", "🐟", 20, listOf("nudeln", "thunfisch", "tomatenkonserve|tomate", "zwiebel", "zitrone|kraeuter"),
            listOf("Nudeln kochen.", "Zwiebel anbraten, Tomaten und Thunfisch dazugeben.", "Mit Zitrone und Kräutern abschmecken, mischen.")),
    )

    fun suggest(entries: List<PantryEntry>, limit: Int = 30): List<RecipeSuggestion> {
        val available = entries.filter { it.daysLeft >= 0 && it.item.ingredient != null }
        val keys = available.mapNotNull { it.item.ingredient }.toSet()
        val expiring = available.filter { it.daysLeft <= 3 }.mapNotNull { it.item.ingredient }.toSet()

        return all.map { recipe ->
            val have = mutableListOf<String>()
            val missing = mutableListOf<String>()
            val used = mutableListOf<String>()
            var expiringUsed = 0
            for (slot in recipe.ingredients) {
                val options = slot.split('|')
                val hit = options.firstOrNull { it in keys }
                if (hit != null) {
                    have += labelOf(hit); used += hit
                    if (hit in expiring) expiringUsed++
                } else {
                    missing += options.joinToString(" / ") { labelOf(it) }
                }
            }
            val ratio = have.size.toDouble() / recipe.ingredients.size
            RecipeSuggestion(recipe, have, missing, used, expiringUsed, ratio + 0.15 * expiringUsed)
        }
            .filter { it.have.isNotEmpty() }
            .sortedWith(compareByDescending<RecipeSuggestion> { it.score }.thenBy { it.missing.size })
            .take(limit)
    }

    fun labelOf(key: String): String = Categorizer.ingredient(key)?.label ?: key
}
