package de.bonquest.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.AppViewModel
import de.bonquest.app.Screen
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Pantry
import de.bonquest.app.domain.Quests
import de.bonquest.app.domain.StatsCalculator
import de.bonquest.app.domain.TipEngine
import de.bonquest.app.domain.euro
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.GameBar
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Pill
import de.bonquest.app.ui.SectionTitle
import java.time.LocalDate

@Composable
fun HomeScreen(vm: AppViewModel, data: AppData) {
    val today = LocalDate.now()
    val stats = remember(data) { StatsCalculator.compute(data, today) }
    val pantry = remember(data) { Pantry.entries(data.receipts, today.toEpochDay()) }
    val week = remember(data) { Quests.weekContext(data, today) }
    val tip = remember(stats, data.game.weeklyBudget) { TipEngine.tips(stats, data.game.weeklyBudget).first() }
    var editBudget by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Budget as hit points
        item {
            val budget = data.game.weeklyBudget
            val left = budget - stats.thisWeekSpent
            val frac = (left / budget).toFloat()
            GameCard(borderColor = if (left < 0) GameColors.Hp else GameColors.Border, onClick = { editBudget = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️ BUDGET-LEBEN", color = GameColors.Hp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text("Woche · ${euro(budget)}", color = GameColors.TextDim, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                GameBar(frac, if (frac > 0.3f) GameColors.Hp else GameColors.Orange, height = 24.dp,
                    label = if (left >= 0) "${euro(left)} übrig" else "${euro(-left)} drüber!")
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        left < 0 -> "💀 Budget-Boss hat zugeschlagen! Koche jetzt aus dem Vorrat."
                        frac < 0.3f -> "⚠️ Nur noch wenig Leben – plane die restliche Woche gut."
                        else -> "Diese Woche ausgegeben: ${euro(stats.thisWeekSpent)} · Tippen zum Ändern"
                    },
                    color = GameColors.TextDim, fontSize = 13.sp,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("📸 BON SCANNEN", Modifier.weight(1f), color = GameColors.Xp, shadow = GameColors.XpDark, textColor = androidx.compose.ui.graphics.Color.Black) { vm.navigate(Screen.SCAN, asTab = true) }
                ChunkyButton("🛒 EINKAUFS-CHECK", Modifier.weight(1f), color = GameColors.Mana, shadow = GameColors.ManaDark, textColor = androidx.compose.ui.graphics.Color.Black) { vm.navigate(Screen.CHECK) }
            }
        }

        item { SectionTitle("⚔️ Wochen-Quests") }
        Quests.weekly.forEach { q ->
            item(key = q.id) {
                val progress = q.progress(week)
                val done = progress >= q.target
                val claimed = Quests.isClaimed(data, q, today)
                GameCard(borderColor = if (done && !claimed) GameColors.Gold else GameColors.Border) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.title, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            val label = if (q.id == "w_save") "${euro(progress.coerceAtMost(q.target))} / ${euro(q.target)}" else "${progress.toInt().coerceAtMost(q.target.toInt())} / ${q.target.toInt()}"
                            GameBar((progress / q.target).toFloat(), GameColors.Mana, height = 14.dp, label = label)
                        }
                        Spacer(Modifier.width(10.dp))
                        when {
                            claimed -> Pill("✔", GameColors.XpDark, textColor = GameColors.Text)
                            done -> ChunkyButton("HOLEN", Modifier.width(84.dp), color = GameColors.Gold, shadow = GameColors.GoldDark, textColor = androidx.compose.ui.graphics.Color.Black, height = 38.dp) { vm.claimQuest(q) }
                            else -> Column(horizontalAlignment = Alignment.End) {
                                Text("+${q.rewardXp} XP", color = GameColors.Xp, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("+${q.rewardCoins} 🪙", color = GameColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        val expiring = pantry.filter { it.daysLeft in 0..2 }
        if (expiring.isNotEmpty()) {
            item { Column {
                SectionTitle("⏳ Bald verbrauchen") {
                    TextButton(onClick = { vm.navigate(Screen.RECIPES, asTab = true) }) { Text("Rezepte →", color = GameColors.Mana) }
                }
                GameCard(borderColor = GameColors.Orange) {
                    expiring.take(6).forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${e.item.category.emoji}  ${e.item.name}", modifier = Modifier.weight(1f), maxLines = 1)
                            Pill(if (e.daysLeft == 0) "heute" else "${e.daysLeft} T", GameColors.Orange)
                        }
                    }
                }
            } }
        }

        item { Column {
            SectionTitle("💡 Spar-Tipp") {
                TextButton(onClick = { vm.navigate(Screen.STATS, asTab = true) }) { Text("Alle →", color = GameColors.Mana) }
            }
            GameCard(borderColor = GameColors.Gold) {
                Text("${tip.emoji}  ${tip.title}", fontWeight = FontWeight.Black, color = GameColors.Gold)
                Spacer(Modifier.height(4.dp))
                Text(tip.text, color = GameColors.Text)
            }
        } }

        item { Column {
            SectionTitle("🏰 Mehr")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("🏆 ERFOLGE & SHOP", Modifier.weight(1f)) { vm.navigate(Screen.TROPHIES) }
                ChunkyButton("📜 BON-ARCHIV", Modifier.weight(1f)) { vm.navigate(Screen.HISTORY) }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { editName = true }) { Text("✏️ Heldennamen ändern", color = GameColors.TextDim) }
        } }
    }

    if (editBudget) {
        NumberDialog("Wochenbudget (€)", data.game.weeklyBudget, onDismiss = { editBudget = false }) {
            vm.setBudget(it); editBudget = false
        }
    }
    if (editName) {
        var name by remember { mutableStateOf(data.game.heroName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Dein Heldenname") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.setHeroName(name.ifBlank { "Held" }); editName = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
fun NumberDialog(title: String, initial: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(String.format(java.util.Locale.GERMANY, "%.2f", initial)) }
    val value = text.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = value == null,
            )
        },
        confirmButton = { TextButton(enabled = value != null && value > 0, onClick = { onConfirm(value!!) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
