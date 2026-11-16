package de.bonquest.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.AppViewModel
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Pantry
import de.bonquest.app.domain.RecipeSuggestion
import de.bonquest.app.domain.Recipes
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.EmptyState
import de.bonquest.app.ui.GameBar
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Pill
import java.time.LocalDate

@Composable
fun RecipesScreen(vm: AppViewModel, data: AppData) {
    val suggestions = remember(data) {
        Recipes.suggest(Pantry.entries(data.receipts, LocalDate.now().toEpochDay()))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text("🍳 RECIPE MAGIC", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("Dishes from what you bought. Ingredients that expire soon come first!", color = GameColors.TextDim)
            }
        }
        if (suggestions.isEmpty()) {
            item { EmptyState("🧑‍🍳", "No ingredients yet", "Scan a grocery receipt and I'll conjure up matching recipes.") }
        }
        items(suggestions, key = { it.recipe.id }) { s -> RecipeCard(vm, s) }
    }
}

@Composable
private fun RecipeCard(vm: AppViewModel, s: RecipeSuggestion) {
    var open by rememberSaveable(s.recipe.id) { mutableStateOf(false) }
    val complete = s.missing.isEmpty()
    GameCard(borderColor = if (complete) GameColors.Gold else GameColors.Border, onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.recipe.emoji, fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.recipe.name, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⏱ ${s.recipe.minutes} Min", color = GameColors.TextDim, fontSize = 12.sp)
                    if (s.expiringUsed > 0) Text("⏳ rescues ${s.expiringUsed} ingredient${if (s.expiringUsed > 1) "s" else ""}", color = GameColors.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (complete) Pill("ALL THERE!", GameColors.Gold) else Pill("${s.have.size}/${s.recipe.ingredients.size}", GameColors.Mana)
        }
        Spacer(Modifier.height(8.dp))
        GameBar(s.matchPercent / 100f, if (complete) GameColors.Gold else GameColors.Mana, height = 10.dp)
        Spacer(Modifier.height(6.dp))
        Text("✅ " + s.have.joinToString(", "), color = GameColors.Xp, fontSize = 13.sp)
        if (s.missing.isNotEmpty()) Text("🛍️ Missing: " + s.missing.joinToString(", "), color = GameColors.TextDim, fontSize = 13.sp)
        if (open) {
            Spacer(Modifier.height(10.dp))
            s.recipe.steps.forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${i + 1}.", color = GameColors.Gold, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
                    Text(step)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text("Salt, pepper & spices are assumed.", color = GameColors.TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            ChunkyButton("🔥 COOKED IT!  (+40 XP)", Modifier.fillMaxWidth(), color = GameColors.Orange, shadow = Color(0xFFA35A12), textColor = Color.Black) { vm.cook(s) }
        } else {
            Text("Tap for instructions ▾", color = GameColors.Mana, fontSize = 12.sp)
        }
    }
}
