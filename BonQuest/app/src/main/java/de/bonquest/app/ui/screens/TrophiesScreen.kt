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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.AppViewModel
import de.bonquest.app.domain.Achievements
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Avatars
import de.bonquest.app.domain.Receipt
import de.bonquest.app.domain.euro
import de.bonquest.app.ui.AvatarBadge
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.EmptyState
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Pill
import de.bonquest.app.ui.SectionTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TrophiesScreen(vm: AppViewModel, data: AppData) {
    val g = data.game
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text("🏆 HALL OF TROPHIES", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("${g.unlocked.size} of ${Achievements.all.size} achievements unlocked", color = GameColors.TextDim)
            }
        }
        items(Achievements.all.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    val done = a.id in g.unlocked
                    GameCard(Modifier.weight(1f).alpha(if (done) 1f else 0.55f), borderColor = if (done) GameColors.Gold else GameColors.Border) {
                        Text(if (done) a.emoji else "🔒", fontSize = 30.sp)
                        Text(a.name, fontWeight = FontWeight.Black, color = if (done) GameColors.Gold else GameColors.Text)
                        Text(a.description, color = GameColors.TextDim, fontSize = 12.sp, minLines = 2)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOfNotNull(if (a.rewardXp > 0) "+${a.rewardXp} XP" else null, if (a.rewardCoins > 0) "+${a.rewardCoins} 🪙" else null).joinToString("  "),
                            color = GameColors.Xp, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { SectionTitle("🛡️ Hero shop") }
        items(Avatars.all.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { av ->
                    val owned = av.id in g.ownedAvatars
                    val active = g.avatar == av.id
                    GameCard(Modifier.weight(1f), borderColor = if (active) GameColors.Gold else GameColors.Border, onClick = { vm.buyOrSelectAvatar(av.id) }) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            AvatarBadge(av.emoji, 52.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(av.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            when {
                                active -> Pill("ACTIVE", GameColors.Gold)
                                owned -> Pill("SELECT", GameColors.Mana)
                                else -> Pill("${av.price} 🪙", if (g.coins >= av.price) GameColors.Xp else GameColors.CardHi, textColor = if (g.coins >= av.price) Color.Black else GameColors.TextDim)
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", java.util.Locale.ENGLISH)

@Composable
fun HistoryScreen(vm: AppViewModel, data: AppData) {
    var toDelete by remember { mutableStateOf<Receipt?>(null) }
    var openId by remember { mutableStateOf<String?>(null) }
    val receipts = data.receipts.sortedByDescending { it.epochDay }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("📜 RECEIPT ARCHIVE", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp) }
        if (receipts.isEmpty()) item { EmptyState("📭", "No receipts yet", "Your scanned receipts will show up here.") }
        items(receipts, key = { it.id }) { r ->
            GameCard(onClick = { openId = if (openId == r.id) null else r.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🏪 ${r.store}", fontWeight = FontWeight.Black)
                        Text("${LocalDate.ofEpochDay(r.epochDay).format(dateFmt)} · ${r.items.size} items", color = GameColors.TextDim, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(euro(r.total), fontWeight = FontWeight.Black)
                        if (r.savings > 0) Text("-${euro(r.savings)}", color = GameColors.Xp, fontSize = 12.sp)
                    }
                    IconButton(onClick = { toDelete = r }) { Icon(Icons.Default.Delete, "Delete", tint = GameColors.Hp) }
                }
                if (openId == r.id) {
                    Spacer(Modifier.height(6.dp))
                    r.items.forEach { i ->
                        Row {
                            Text("${i.category.emoji} ${i.name}", Modifier.weight(1f), fontSize = 13.sp, maxLines = 1)
                            Text(euro(i.effectivePrice), fontSize = 13.sp, color = if (i.isPromo) GameColors.Xp else GameColors.Text)
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete receipt?") },
            text = { Text("${r.store} from ${LocalDate.ofEpochDay(r.epochDay).format(dateFmt)} will be removed. Earned XP stays.") },
            confirmButton = { TextButton(onClick = { vm.deleteReceipt(r.id); toDelete = null }) { Text("Delete", color = GameColors.Hp) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
