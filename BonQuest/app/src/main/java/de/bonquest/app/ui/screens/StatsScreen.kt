package de.bonquest.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.AppViewModel
import de.bonquest.app.domain.AppData
import de.bonquest.app.domain.Stats
import de.bonquest.app.domain.StatsCalculator
import de.bonquest.app.domain.TipEngine
import de.bonquest.app.domain.euro
import de.bonquest.app.ui.EmptyState
import de.bonquest.app.ui.GameBar
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.SectionTitle
import de.bonquest.app.ui.StatTile
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: AppViewModel, data: AppData) {
    val stats = remember(data) { StatsCalculator.compute(data, LocalDate.now()) }
    val tips = remember(stats, data.game.weeklyBudget) { TipEngine.tips(stats, data.game.weeklyBudget) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("📊 KAMPF-STATISTIK", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp) }
        if (stats.receiptCount == 0) {
            item { EmptyState("📉", "Noch keine Daten", "Scanne Bons, um deine Ausgaben, Ersparnisse und Spar-Tipps zu sehen.") }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("💶", euro(stats.totalSpent), "ausgegeben", GameColors.Hp, Modifier.weight(1f))
                    StatTile("🏷️", euro(stats.totalSaved), "gespart", GameColors.Xp, Modifier.weight(1f))
                    StatTile("🧾", "${stats.receiptCount}", "Bons · Ø ${euro(stats.avgReceipt)}", GameColors.Mana, Modifier.weight(1f))
                }
            }
            item { Column { SectionTitle("📅 Ausgaben pro Monat"); GameCard { MonthBars(stats) } } }
            item { Column { SectionTitle("🧩 Kategorien"); GameCard { CategoryBreakdown(stats) } } }
            if (stats.stores.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("🏪 Läden")
                        GameCard {
                            val max = stats.stores.maxOf { it.amount }.coerceAtLeast(0.01)
                            stats.stores.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(s.store, Modifier.width(100.dp), maxLines = 1, fontWeight = FontWeight.Bold)
                                    Box(Modifier.weight(1f)) { GameBar((s.amount / max).toFloat(), GameColors.Purple, height = 14.dp) }
                                    Text(" ${euro(s.amount)} · ${s.trips}×", color = GameColors.TextDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    SectionTitle("🔁 Am häufigsten gekauft")
                    GameCard {
                        stats.topProducts.forEachIndexed { i, p ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text(listOf("🥇", "🥈", "🥉").getOrElse(i) { "▫️" } + " " + p.name, Modifier.weight(1f), maxLines = 1)
                                Text("${p.count}× · ${euro(p.spent)}", color = GameColors.TextDim, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            if (stats.priceIncreases.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("📈 Preisalarm")
                        GameCard(borderColor = GameColors.Hp) {
                            stats.priceIncreases.take(6).forEach { p ->
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Text(p.name, Modifier.weight(1f), maxLines = 1)
                                    Text("${euro(p.oldPrice)} → ${euro(p.newPrice)} (+${p.percent} %)", color = GameColors.Hp, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("🏷️", "${if (stats.itemCount > 0) 100 * stats.promoItemCount / stats.itemCount else 0} %", "Aktionsanteil", GameColors.Xp, Modifier.weight(1f))
                    StatTile("🛟", "${stats.rescued}", "gerettet", GameColors.Mana, Modifier.weight(1f))
                    StatTile("🗑️", "${stats.wasted}", "weggeworfen", GameColors.Orange, Modifier.weight(1f))
                }
            }
        }
        item { SectionTitle("💡 Tipps vom Spar-Orakel") }
        items(tips) { t ->
            GameCard(borderColor = if (t.priority >= 75) GameColors.Gold else GameColors.Border) {
                Text("${t.emoji}  ${t.title}", fontWeight = FontWeight.Black, color = if (t.priority >= 75) GameColors.Gold else GameColors.Text)
                Spacer(Modifier.height(4.dp))
                Text(t.text, color = GameColors.TextDim)
            }
        }
    }
}

@Composable
private fun MonthBars(stats: Stats) {
    val max = stats.months.maxOf { it.amount }.coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        stats.months.forEachIndexed { i, m ->
            val current = i == stats.months.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(if (m.amount > 0) "${m.amount.roundToInt()}€" else "", fontSize = 11.sp, color = GameColors.TextDim, maxLines = 1)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((m.amount / max).toFloat().coerceIn(0.02f, 0.8f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (current) GameColors.Gold else GameColors.Purple)
                )
                Text(m.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (current) GameColors.Gold else GameColors.TextDim, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(stats: Stats) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(120.dp)) {
            var start = -90f
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            stats.categories.forEach { c ->
                val sweep = (c.share * 360f).toFloat()
                drawArc(
                    Color(c.category.color), start, sweep - 1.5f, false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                )
                start += sweep
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            stats.categories.take(7).forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(c.category.color)))
                    Text(" ${c.category.emoji} ${c.category.label}", Modifier.weight(1f), fontSize = 13.sp, maxLines = 1)
                    Text("${(c.share * 100).roundToInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
