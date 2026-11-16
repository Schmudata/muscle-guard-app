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
            listOf("Finely dice onion, garlic and carrot.", "Brown the mince, then add the vegetables.", "Add the tomatoes, simmer for 20 min and season.", "Cook the pasta and serve with the sauce.")),
        Recipe("pomodoro", "Pasta Pomodoro", "🍅", 20, listOf("nudeln", "tomate|tomatenkonserve", "knoblauch", "oel", "parmesan|kaese"),
            listOf("Cook the pasta.", "Sweat the garlic in oil, add tomatoes and reduce.", "Toss with the pasta and top with cheese.")),
        Recipe("shakshuka", "Shakshuka", "🍳", 25, listOf("eier", "tomatenkonserve|tomate", "paprika", "zwiebel", "knoblauch", "brot"),
            listOf("Fry onion, pepper and garlic.", "Add tomatoes, simmer 10 min, season with cumin and paprika.", "Make wells, crack in the eggs, cover until set.", "Serve with bread.")),
        Recipe("omelett", "Veggie Omelette", "🥚", 15, listOf("eier", "paprika|tomate|pilze|spinat", "kaese", "zwiebel"),
            listOf("Chop the vegetables and fry them.", "Whisk and season the eggs, pour over.", "Sprinkle with cheese and fold.")),
        Recipe("ruehrei", "Scrambled Eggs on Toast", "🍞", 10, listOf("eier", "toast|brot", "butter", "kraeuter|tomate"),
            listOf("Whisk the eggs and cook slowly in butter.", "Toast the bread, top with the eggs and herbs or tomato.")),
        Recipe("ofenkartoffel", "Baked Potatoes with Herb Dip", "🥔", 45, listOf("kartoffel", "quark|joghurt", "knoblauch", "oel", "kraeuter|zwiebel"),
            listOf("Halve the potatoes, toss with oil and salt, bake 35 min at 200 °C.", "Mix quark/yoghurt with garlic, herbs and salt.", "Serve together.")),
        Recipe("kartoffelsuppe", "Potato Soup (Caldo Verde style)", "🍲", 40, listOf("kartoffel", "karotte|salat|spinat", "lauch|zwiebel", "bruehe", "oel", "wurst"),
            listOf("Dice the vegetables and sweat them in oil.", "Add stock and cook for 20 min.", "Blend, add greens and sliced chouriço, simmer 5 min.")),
        Recipe("curry", "Chicken Curry", "🍛", 30, listOf("gefluegel", "kokosmilch", "reis", "paprika|brokkoli|zucchini", "zwiebel", "knoblauch|ingwer"),
            listOf("Cook the rice.", "Dice and brown the chicken, add onion, garlic and vegetables.", "Stir in curry, add coconut milk and simmer 10 min.")),
        Recipe("gemuesepfanne", "Veggie Stir-fry with Rice", "🥘", 25, listOf("reis", "paprika", "zucchini|brokkoli", "karotte", "zwiebel", "oel"),
            listOf("Cook the rice.", "Cut the vegetables into strips and stir-fry in hot oil.", "Season with soy sauce or spices.")),
        Recipe("greek", "Greek Salad", "🥗", 10, listOf("gurke", "tomate", "feta", "zwiebel", "paprika", "oel"),
            listOf("Roughly chop the vegetables.", "Crumble the feta on top.", "Dress with olive oil, salt and oregano.")),
        Recipe("caprese", "Caprese", "🍅", 5, listOf("tomate", "mozzarella", "oel", "kraeuter"),
            listOf("Slice tomatoes and mozzarella and layer them.", "Top with olive oil, salt, pepper and basil.")),
        Recipe("pfannkuchen", "Pancakes", "🥞", 20, listOf("mehl", "eier", "milch", "zucker", "butter|oel"),
            listOf("Whisk flour, eggs, milk and a pinch of sugar into a smooth batter.", "Fry thin pancakes in a hot pan.", "Fill sweet or savoury.")),
        Recipe("oats", "Overnight Oats", "🥣", 5, listOf("haferflocken", "milch|joghurt", "banane|beeren|apfel", "honig"),
            listOf("Mix oats with milk or yoghurt.", "Chill overnight.", "Top with fruit and honey.")),
        Recipe("bananenbrot", "Banana Bread", "🍌", 60, listOf("banane", "mehl", "eier", "zucker|honig", "butter|oel"),
            listOf("Mash very ripe bananas.", "Mix with eggs, sugar, fat and flour (+ baking powder).", "Bake at 175 °C for about 50 min.")),
        Recipe("chili", "Chili sin Carne", "🌶️", 35, listOf("linsen", "mais", "tomatenkonserve", "zwiebel", "paprika", "reis|wraps"),
            listOf("Fry onion and pepper.", "Add beans/lentils, corn and tomatoes.", "Season with chili and cumin, simmer 20 min.")),
        Recipe("lachs", "Salmon with Roast Veg", "🐟", 30, listOf("fisch", "brokkoli|zucchini|paprika", "kartoffel", "zitrone", "oel"),
            listOf("Dice potatoes and veg, roast with oil at 200 °C for 15 min.", "Add the fish and roast 12 more minutes.", "Serve with lemon.")),
        Recipe("bacalhau", "Bacalhau à Brás style", "🐟", 30, listOf("fisch", "kartoffel", "eier", "zwiebel", "oel", "kraeuter"),
            listOf("Cut potatoes into thin matchsticks and fry until crisp (or use ready-made).", "Fry onion in olive oil, add the flaked fish.", "Stir in the potatoes and beaten eggs until just set, top with parsley.")),
        Recipe("risotto", "Mushroom Risotto", "🍄", 35, listOf("reis", "pilze", "zwiebel", "parmesan|kaese", "bruehe", "butter"),
            listOf("Sweat the onion in butter, toast the rice briefly.", "Add stock ladle by ladle, stirring.", "Fold in fried mushrooms and parmesan.")),
        Recipe("wrap", "Spinach & Feta Wraps", "🌯", 15, listOf("wraps", "spinat|salat", "feta|frischkaese", "tomate", "gurke|paprika"),
            listOf("Wilt the spinach (or use fresh lettuce).", "Fill the wraps with cheese and vegetables.", "Roll up and toast briefly in a pan.")),
        Recipe("toastie", "Ham & Cheese Toastie (Tosta Mista)", "🥪", 10, listOf("toast|brot", "wurst", "kaese", "butter"),
            listOf("Butter the bread.", "Fill with ham and cheese.", "Toast in a pan or press until golden.")),
        Recipe("auflauf", "Pasta Bake", "🧀", 40, listOf("nudeln", "sahne|milch", "kaese", "brokkoli|spinat|paprika", "wurst|eier"),
            listOf("Pre-cook the pasta.", "Put it in a dish with vegetables and ham.", "Pour over cream, top with cheese, bake 20 min at 200 °C.")),
        Recipe("dal", "Lentil Dal", "🫘", 30, listOf("linsen", "kokosmilch", "tomatenkonserve|tomate", "zwiebel", "knoblauch|ingwer", "reis"),
            listOf("Sweat onion, garlic and ginger, add curry powder.", "Add lentils, tomatoes, coconut milk and water.", "Simmer 20 min and serve with rice.")),
        Recipe("bifanas", "Pork Steaks with Potatoes", "🥩", 40, listOf("schwein|gefluegel", "kartoffel", "knoblauch", "zitrone|kraeuter", "oel", "salat|gurke"),
            listOf("Marinate the meat with garlic, lemon/wine and olive oil.", "Boil or roast the potatoes.", "Pan-fry the meat and serve with salad.")),
        Recipe("bowl", "Breakfast Bowl", "🍓", 5, listOf("joghurt|quark", "beeren|banane|apfel", "haferflocken", "honig"),
            listOf("Put the yoghurt in a bowl.", "Top with fruit, oats and honey.")),
        Recipe("avotoast", "Avocado Toast with Egg", "🥑", 10, listOf("brot|toast", "avocado", "zitrone", "eier"),
            listOf("Mash the avocado with lemon and salt.", "Spread on toasted bread.", "Top with a fried or poached egg.")),
        Recipe("quiche", "Leek Quiche", "🥧", 60, listOf("mehl", "butter", "eier", "sahne", "lauch|spinat|brokkoli", "kaese"),
            listOf("Knead flour, butter, an egg and salt into a dough, press into a tin.", "Sweat the vegetables and spread on the dough.", "Whisk eggs with cream, pour over, add cheese, bake 35 min at 180 °C.")),
        Recipe("burgerbowl", "Burger Bowl", "🍔", 20, listOf("hackfleisch", "salat", "tomate", "gurke", "kaese", "zwiebel"),
            listOf("Brown and season the mince.", "Chop lettuce, tomato and cucumber.", "Arrange in bowls, top with cheese and sauce.")),
        Recipe("tomatensuppe", "Tomato Soup", "🥣", 25, listOf("tomatenkonserve|tomate", "zwiebel", "knoblauch", "sahne", "bruehe", "brot"),
            listOf("Sweat onion and garlic.", "Add tomatoes and stock, simmer 15 min.", "Blend, stir in cream and serve with bread.")),
        Recipe("obstsalat", "Fruit Salad", "🍇", 10, listOf("apfel", "banane", "orange|trauben|beeren", "zitrone", "joghurt|honig"),
            listOf("Chop the fruit.", "Mix with lemon juice and a little honey.", "Serve with yoghurt if you like.")),
        Recipe("thunfischpasta", "Tuna Pasta", "🐟", 20, listOf("nudeln", "thunfisch", "tomatenkonserve|tomate", "zwiebel", "zitrone|kraeuter"),
            listOf("Cook the pasta.", "Fry the onion, add tomatoes and tuna.", "Season with lemon and herbs and toss.")),
        Recipe("arrozfrango", "Chicken Rice (Arroz de Frango)", "🍚", 40, listOf("gefluegel", "reis", "zwiebel", "tomate|tomatenkonserve", "wurst|paprika", "bruehe"),
            listOf("Brown chicken pieces with onion and chouriço.", "Add tomato and the rice, stir.", "Add stock and cook covered for 18 min.")),
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
