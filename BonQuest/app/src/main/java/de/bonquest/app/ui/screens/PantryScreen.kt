package de.bonquest.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
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
import de.bonquest.app.Screen
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Category
import de.bonquest.app.domain.Pantry
import de.bonquest.app.domain.PantryEntry
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.EmptyState
import de.bonquest.app.ui.GameBar
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Pill
import de.bonquest.app.ui.SectionTitle
import java.time.LocalDate

@Composable
fun PantryScreen(vm: AppViewModel, data: AppData) {
    val entries = remember(data) { Pantry.entries(data.receipts, LocalDate.now().toEpochDay()) }
    var filter by rememberSaveable { mutableStateOf<Category?>(null) }
    val shown = entries.filter { filter == null || it.item.category == filter }
    val cats = entries.map { it.item.category }.distinct()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text("🏠 YOUR PANTRY", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("Estimated from your purchases: what is probably still at home?", color = GameColors.TextDim)
                Spacer(Modifier.height(10.dp))
                ChunkyButton("🛒 CHECK SHOPPING LIST", Modifier.fillMaxWidth(), color = GameColors.Mana, shadow = GameColors.ManaDark, textColor = Color.Black) { vm.navigate(Screen.CHECK) }
            }
        }
        if (entries.isEmpty()) {
            item { EmptyState("🕸️", "The pantry is empty", "Scan a receipt – your products will appear here with their estimated shelf life.") }
        } else {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(filter == null, { filter = null }, label = { Text("All (${entries.size})") }, colors = chipColors())
                    cats.forEach { c ->
                        FilterChip(filter == c, { filter = if (filter == c) null else c }, label = { Text("${c.emoji} ${c.label}") }, colors = chipColors())
                    }
                }
            }
            items(shown, key = { it.item.id }) { e -> PantryRow(vm, e) }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = GameColors.Purple,
    selectedLabelColor = Color.White,
    labelColor = GameColors.TextDim,
)

@Composable
private fun PantryRow(vm: AppViewModel, e: PantryEntry) {
    val color = when {
        e.daysLeft < 0 -> GameColors.Hp
        e.isExpiringSoon -> GameColors.Orange
        else -> GameColors.Xp
    }
    GameCard(borderColor = color.copy(alpha = 0.7f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.item.category.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(e.item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "${if (e.daysAgo == 0) "today" else "${e.daysAgo} d ago"} · ${e.store}",
                    color = GameColors.TextDim, fontSize = 12.sp,
                )
            }
            Pill(
                when {
                    e.daysLeft < 0 -> "check!"
                    e.daysLeft == 0 -> "today"
                    e.daysLeft > 60 -> "long"
                    else -> "~${e.daysLeft} d"
                },
                color,
            )
        }
        Spacer(Modifier.height(8.dp))
        GameBar(e.freshness, color, height = 10.dp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChunkyButton("✔ USED UP", Modifier.weight(1f), color = GameColors.XpDark, shadow = Color(0xFF0F4A2A), height = 38.dp) { vm.consume(e) }
            ChunkyButton("🗑 TOSSED", Modifier.weight(0.6f), color = GameColors.HpDark, shadow = Color(0xFF5A1020), height = 38.dp) { vm.waste(e) }
        }
    }
}

@Composable
fun CheckScreen(vm: AppViewModel, data: AppData) {
    val entries = remember(data) { Pantry.entries(data.receipts, LocalDate.now().toEpochDay()) }
    var list by rememberSaveable { mutableStateOf("") }
    val hits = remember(list, entries) { Pantry.checkList(list, entries) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text("🛒 SHOPPING CHECK", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("Type your shopping list – I'll tell you what you probably already have at home. Portuguese or English works.", color = GameColors.TextDim)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    list, { list = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("leite\nmassa\ntomate\npapel higiénico") },
                    label = { Text("One product per line or comma-separated") },
                )
            }
        }
        if (hits.isNotEmpty()) {
            val home = hits.count { it.entries.isNotEmpty() }
            item {
                GameCard(borderColor = GameColors.Gold) {
                    Text(
                        if (home > 0) "🎯 You probably already have $home of ${hits.size} products!" else "✅ None of it is in your pantry – off to the store!",
                        fontWeight = FontWeight.Black, color = GameColors.Gold,
                    )
                }
            }
            items(hits) { h ->
                val first = h.entries.firstOrNull()
                GameCard(borderColor = if (first != null) GameColors.Orange else GameColors.Border) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (first != null) "🏠" else "🛍️", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.query, fontWeight = FontWeight.Bold)
                            if (first != null) {
                                Text(
                                    "Already there: ${first.item.name} (bought ${if (first.daysAgo == 0) "today" else "${first.daysAgo} days ago"}" +
                                        (if (first.daysLeft <= 60) ", ~${first.daysLeft} days left)" else ")"),
                                    color = GameColors.Orange, fontSize = 13.sp,
                                )
                                if (h.entries.size > 1) Text("+ ${h.entries.size - 1} more", color = GameColors.TextDim, fontSize = 12.sp)
                            } else {
                                Text("Needs to be bought", color = GameColors.TextDim, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            item { SectionTitle("Note"); }
            item { Text("No pantry yet – scan a few receipts first.", color = GameColors.TextDim) }
        }
    }
}
