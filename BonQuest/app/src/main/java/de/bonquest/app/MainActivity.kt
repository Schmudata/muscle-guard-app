package de.bonquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.bonquest.app.domain.Levels
import de.bonquest.app.ui.BonQuestTheme
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.Confetti
import de.bonquest.app.ui.GameColors
import de.bonquest.app.ui.Hud
import de.bonquest.app.ui.Pulsing
import de.bonquest.app.ui.screens.CheckScreen
import de.bonquest.app.ui.screens.HistoryScreen
import de.bonquest.app.ui.screens.HomeScreen
import de.bonquest.app.ui.screens.PantryScreen
import de.bonquest.app.ui.screens.RecipesScreen
import de.bonquest.app.ui.screens.ReviewScreen
import de.bonquest.app.ui.screens.ScanScreen
import de.bonquest.app.ui.screens.StatsScreen
import de.bonquest.app.ui.screens.TrophiesScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BonQuestTheme { App(vm) } }
    }
}

@Composable
fun App(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    BackHandler(enabled = vm.screen != Screen.HOME) { vm.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GameColors.BgDeep, GameColors.Bg, GameColors.Bg)))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Hud(data.game)
            Box(Modifier.weight(1f)) {
                AnimatedContent(vm.screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
                    when (s) {
                        Screen.HOME -> HomeScreen(vm, data)
                        Screen.SCAN -> ScanScreen(vm)
                        Screen.REVIEW -> ReviewScreen(vm, data)
                        Screen.PANTRY -> PantryScreen(vm, data)
                        Screen.CHECK -> CheckScreen(vm, data)
                        Screen.RECIPES -> RecipesScreen(vm, data)
                        Screen.STATS -> StatsScreen(vm, data)
                        Screen.TROPHIES -> TrophiesScreen(vm, data)
                        Screen.HISTORY -> HistoryScreen(vm, data)
                    }
                }
            }
            if (vm.screen != Screen.REVIEW) BottomBar(vm)
        }

        // floating reward toasts
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            vm.toasts.forEach { t ->
                androidx.compose.runtime.key(t.id) {
                    AnimatedVisibility(visible = true, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                        Text(
                            t.text,
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GameColors.BgDeep.copy(alpha = 0.95f))
                                .border(2.dp, GameColors.Gold, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = GameColors.Gold, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        vm.popups.firstOrNull()?.let { PopupOverlay(it) { vm.dismissPopup() } }
    }
}

private data class Tab(val screen: Screen, val emoji: String, val label: String)

@Composable
private fun BottomBar(vm: AppViewModel) {
    val tabs = listOf(
        Tab(Screen.HOME, "🏰", "Base"),
        Tab(Screen.PANTRY, "🏠", "Pantry"),
        Tab(Screen.SCAN, "📸", "Scan"),
        Tab(Screen.RECIPES, "🍳", "Recipes"),
        Tab(Screen.STATS, "📊", "Stats"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(GameColors.BgDeep)
            .border(width = 2.dp, color = GameColors.Border, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = vm.screen == tab.screen || (tab.screen == Screen.PANTRY && vm.screen == Screen.CHECK)
            val isScan = tab.screen == Screen.SCAN
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.navigate(tab.screen, asTab = true) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isScan) {
                    Box(
                        Modifier
                            .offset(y = (-10).dp)
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(GameColors.Xp, GameColors.XpDark)))
                            .border(3.dp, GameColors.Gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(tab.emoji, fontSize = 26.sp) }
                    Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = GameColors.Gold, modifier = Modifier.offset(y = (-8).dp))
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) GameColors.Purple.copy(alpha = 0.35f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) { Text(tab.emoji, fontSize = if (selected) 24.sp else 21.sp) }
                    Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (selected) GameColors.Gold else GameColors.TextDim)
                }
            }
        }
    }
}

@Composable
private fun PopupOverlay(popup: Popup, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(popup) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.key(popup) { Confetti() }
        AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
            Column(
                Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(GameColors.CardHi, GameColors.Card)))
                    .border(3.dp, GameColors.Gold, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (popup) {
                    is Popup.LevelUp -> {
                        Text("LEVEL UP!", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 3.sp)
                        Spacer(Modifier.height(8.dp))
                        Pulsing { Text("⭐", fontSize = 72.sp) }
                        Text("Level ${popup.level}", fontWeight = FontWeight.Black, fontSize = 26.sp)
                        Text("New title: ${Levels.title(popup.level)}", color = GameColors.Xp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    is Popup.Unlocked -> {
                        Text("ACHIEVEMENT UNLOCKED", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Pulsing { Text(popup.achievement.emoji, fontSize = 72.sp) }
                        Text(popup.achievement.name, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center)
                        Text(popup.achievement.description, color = GameColors.TextDim, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            listOfNotNull(
                                if (popup.achievement.rewardXp > 0) "+${popup.achievement.rewardXp} XP" else null,
                                if (popup.achievement.rewardCoins > 0) "+${popup.achievement.rewardCoins} 🪙" else null,
                            ).joinToString("   "),
                            color = GameColors.Xp, fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                ChunkyButton("CONTINUE", Modifier.fillMaxWidth(), color = GameColors.Gold, shadow = GameColors.GoldDark, textColor = Color.Black, onClick = onDismiss)
            }
        }
    }
}
