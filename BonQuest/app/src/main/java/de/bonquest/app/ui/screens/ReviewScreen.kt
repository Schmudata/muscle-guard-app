package de.bonquest.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.AppViewModel
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Category
import de.bonquest.app.domain.GameEngine
import de.bonquest.app.domain.Pantry
import de.bonquest.app.domain.PurchaseItem
import de.bonquest.app.domain.Receipt
import de.bonquest.app.domain.euro
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Pill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ReviewScreen(vm: AppViewModel, data: AppData) {
    val draft = vm.draft ?: return
    val pantry = remember(data) { Pantry.entries(data.receipts, LocalDate.now().toEpochDay()) }
    val (xp, coins) = GameEngine.xpForReceipt(draft)
    var showRaw by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Column {
            Text("🎁 CHECK YOUR LOOT", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("Check the recognized products – tap one to edit.", color = GameColors.TextDim)
        } }
        item { HeaderCard(vm, draft) }
        vm.scanError?.let { err ->
            item { GameCard(borderColor = GameColors.Orange) { Text("⚠️ $err", color = GameColors.Orange) } }
        }
        items(draft.items, key = { it.id }) { item ->
            val atHome = item.ingredient != null && Pantry.findAtHome(item.name, pantry).any { it.item.ingredient == item.ingredient }
            ItemEditor(vm, item, atHome)
        }
        item {
            TextButton(onClick = { vm.addDraftItem() }) { Text("➕ Add product", color = GameColors.Mana, fontWeight = FontWeight.Bold) }
        }
        item {
            GameCard(borderColor = GameColors.Gold) {
                Row {
                    Text("Recognized total", Modifier.weight(1f), color = GameColors.TextDim)
                    Text(euro(draft.total), fontWeight = FontWeight.Black)
                }
                draft.printedTotal?.let { printed ->
                    Row {
                        Text("Total on receipt", Modifier.weight(1f), color = GameColors.TextDim)
                        Text(euro(printed), fontWeight = FontWeight.Black)
                    }
                    if (abs(printed - draft.total) > 0.05) {
                        Text("⚠️ Off by ${euro(abs(printed - draft.total))} – a product may be missing or a price was misread.", color = GameColors.Orange, fontSize = 13.sp)
                    } else {
                        Text("✅ Total matches exactly!", color = GameColors.Xp, fontSize = 13.sp)
                    }
                }
                if (draft.savings > 0) {
                    Row {
                        Text("🏷️ Saved through promotions", Modifier.weight(1f), color = GameColors.Xp)
                        Text(euro(draft.savings), color = GameColors.Xp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { Column {
            ChunkyButton("⚔️ COLLECT LOOT  (+$xp XP, +$coins 🪙)", Modifier.fillMaxWidth(), color = GameColors.Xp, shadow = GameColors.XpDark, textColor = Color.Black, height = 58.dp, enabled = draft.items.isNotEmpty()) { vm.saveDraft() }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = { vm.discardDraft() }) { Text("Discard", color = GameColors.Hp) }
                Spacer(Modifier.weight(1f))
                if (vm.lastRawRows.isNotEmpty()) TextButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Hide raw text" else "Show recognized raw text", color = GameColors.TextDim) }
            }
        } }
        if (showRaw) {
            item {
                GameCard {
                    vm.lastRawRows.forEach { Text(it, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = GameColors.TextDim) }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(vm: AppViewModel, draft: Receipt) {
    var dateText by remember(draft.id) { mutableStateOf(LocalDate.ofEpochDay(draft.epochDay).format(dateFmt)) }
    GameCard {
        OutlinedTextField(draft.store, { v -> vm.updateDraft { it.copy(store = v) } }, label = { Text("🏪 Store") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            dateText,
            { v ->
                dateText = v
                runCatching { LocalDate.parse(v, dateFmt) }.getOrNull()?.let { d -> vm.updateDraft { it.copy(epochDay = d.toEpochDay()) } }
            },
            label = { Text("📅 Purchase date (DD/MM/YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ItemEditor(vm: AppViewModel, item: PurchaseItem, atHome: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    var priceText by remember(item.id) { mutableStateOf(String.format(java.util.Locale("pt", "PT"), "%.2f", item.price)) }
    var discountText by remember(item.id) { mutableStateOf(if (item.discount > 0) String.format(java.util.Locale("pt", "PT"), "%.2f", item.discount) else "") }
    var catMenu by remember { mutableStateOf(false) }

    GameCard(borderColor = if (item.isPromo) GameColors.Xp else GameColors.Border, onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.category.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.category.label, color = Color(item.category.color), fontSize = 12.sp)
                    if (item.quantity != 1.0) Text("· ${formatQty(item)}", color = GameColors.TextDim, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(euro(item.effectivePrice), fontWeight = FontWeight.Black)
                if (item.isPromo) Pill(if (item.discount > 0) "-${euro(item.discount)}" else "PROMO", GameColors.Xp)
            }
        }
        if (atHome) {
            Spacer(Modifier.height(6.dp))
            Text("🏠 You probably still had this at home!", color = GameColors.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(item.name, { vm.renameDraftItem(item.id, it) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    priceText,
                    { v -> priceText = v; v.replace(',', '.').toDoubleOrNull()?.let { p -> vm.updateDraftItem(item.id) { it.copy(price = p) } } },
                    label = { Text("Price €") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    discountText,
                    { v -> discountText = v; val d = v.replace(',', '.').toDoubleOrNull() ?: 0.0; vm.updateDraftItem(item.id) { it.copy(discount = d, promoLabel = if (d > 0) it.promoLabel ?: "Discount" else it.promoLabel) } },
                    label = { Text("Discount €") }, singleLine = true, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { catMenu = true }) { Text("${item.category.emoji} Change category", color = GameColors.Mana) }
                    DropdownMenu(catMenu, { catMenu = false }) {
                        Category.entries.forEach { c ->
                            DropdownMenuItem(text = { Text("${c.emoji}  ${c.label}") }, onClick = {
                                vm.updateDraftItem(item.id) { it.copy(category = c, ingredient = if (c == it.category) it.ingredient else null) }
                                catMenu = false
                            })
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (item.promoLabel == null) "🏷️ mark as promo" else "🏷️ not a promo",
                    color = GameColors.Xp, fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        vm.updateDraftItem(item.id) { if (it.promoLabel == null) it.copy(promoLabel = "On promotion") else it.copy(promoLabel = null, discount = 0.0) }
                    }.padding(8.dp),
                )
                IconButton(onClick = { vm.removeDraftItem(item.id) }) { Icon(Icons.Default.Delete, "Delete", tint = GameColors.Hp) }
            }
        }
    }
}

fun formatQty(item: PurchaseItem): String =
    if (item.unit == "kg") String.format(java.util.Locale("pt", "PT"), "%.3f kg", item.quantity)
    else "${item.quantity.toInt()} pcs"
