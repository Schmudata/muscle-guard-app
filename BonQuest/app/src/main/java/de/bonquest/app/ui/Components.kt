package de.bonquest.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.bonquest.app.domain.Avatars
import de.bonquest.app.domain.GameState
import de.bonquest.app.domain.Levels
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GameColors.Border,
    background: Color = GameColors.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier) {
        // hard drop shadow for a chunky "game UI" look
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(shape)
                .background(GameColors.BgDeep)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(background)
                .border(2.dp, borderColor, shape)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GameColors.Purple,
    shadow: Color = Color(0xFF4B2DA8),
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(80), label = "press")
    val shape = RoundedCornerShape(14.dp)
    val c = if (enabled) color else Color(0xFF4A4260)
    val s = if (enabled) shadow else Color(0xFF2E2840)
    Box(
        modifier
            .height(height + 5.dp)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 5.dp)
                .clip(shape)
                .background(s)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = (5 * press).dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(c.lighten(0.15f), c)))
                .border(2.dp, Color.White.copy(alpha = 0.18f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = if (enabled) textColor else GameColors.TextDim, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

fun Color.lighten(f: Float) = Color(
    red = red + (1 - red) * f, green = green + (1 - green) * f, blue = blue + (1 - blue) * f, alpha = alpha
)

@Composable
fun GameBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    label: String? = null,
    track: Color = GameColors.BgDeep,
) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "bar")
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .border(2.dp, Color.Black.copy(alpha = 0.35f), shape)
    ) {
        Box(
            Modifier
                .fillMaxWidth(anim)
                .height(height)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(color.lighten(0.35f), color, color.copy(alpha = 0.85f))))
        )
        // glossy highlight
        Box(
            Modifier
                .fillMaxWidth(anim)
                .height(height / 3)
                .offset(y = 2.dp)
                .padding(horizontal = 6.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.25f))
        )
        if (label != null) {
            Text(
                label,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = (height.value * 0.62f).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, textColor: Color = Color.Black) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.titleMedium, color = GameColors.Gold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun AvatarBadge(emoji: String, size: Dp = 56.dp, level: Int? = null) {
    val t = rememberInfiniteTransition(label = "avatar")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "bob")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GameColors.Purple, GameColors.BgDeep)))
                .border(3.dp, GameColors.Gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = (size.value * 0.55f).sp, modifier = Modifier.offset(y = (-2 * bob).dp))
        }
        if (level != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(GameColors.Gold)
                    .border(2.dp, GameColors.BgDeep, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("$level", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

/** Top "heads up display": avatar, level, XP bar and coins. */
@Composable
fun Hud(game: GameState, modifier: Modifier = Modifier) {
    val level = Levels.levelFor(game.xp)
    val next = Levels.xpForLevel(level + 1)
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GameColors.BgDeep, GameColors.Bg)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(Avatars.byId(game.avatar).emoji, 50.dp, level)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${game.heroName} · ${Levels.title(level)}", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            GameBar(Levels.progress(game.xp), GameColors.Xp, height = 14.dp, label = "${game.xp} / $next XP")
        }
        Spacer(Modifier.width(10.dp))
        CoinCounter(game.coins)
    }
}

@Composable
fun CoinCounter(coins: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(GameColors.BgDeep)
            .border(2.dp, GameColors.Gold, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text("$coins", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun StatTile(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    GameCard(modifier, borderColor = color.copy(alpha = 0.7f)) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1)
        Text(label, color = GameColors.TextDim, fontSize = 12.sp, maxLines = 2)
    }
}

@Composable
fun EmptyState(emoji: String, title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, fontSize = 56.sp)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(text, color = GameColors.TextDim, textAlign = TextAlign.Center)
    }
}

private data class Particle(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

/** Burst of confetti used for level-ups and achievements. */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val colors = listOf(GameColors.Gold, GameColors.Xp, GameColors.Hp, GameColors.Mana, GameColors.Purple, GameColors.Orange)
    val particles = remember {
        List(90) {
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = 0.3f + Random.nextFloat() * 0.9f,
                color = colors.random(),
                size = 8f + Random.nextFloat() * 14f,
                spin = Random.nextFloat() * 6f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1800, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        val cx = size.width / 2
        val cy = size.height / 2.6f
        val maxR = size.minDimension * 0.75f
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val r = maxR * p.speed * t
            val x = cx + (cos(rad) * r).toFloat()
            val y = cy + (sin(rad) * r).toFloat() + 900f * t * t * p.speed
            val alpha = (1f - t).coerceIn(0f, 1f)
            drawRect(
                p.color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(p.size, p.size * (0.4f + 0.6f * kotlin.math.abs(sin(p.spin * t * 6f)))),
            )
        }
    }
}

@Composable
fun Pulsing(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    Box(Modifier.scale(s)) { content() }
}
