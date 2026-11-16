package de.bonquest.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import de.bonquest.app.AppViewModel
import de.bonquest.app.ui.ChunkyButton
import de.bonquest.app.ui.GameCard
import de.bonquest.app.ui.GameColors
import java.io.File

@Composable
fun ScanScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) vm.scanImage(Uri.parse(uri))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.scanImage(uri)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("BON-SCANNER", color = GameColors.Gold, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 3.sp)
        ScannerVisual(vm.scanning)

        if (vm.scanning) {
            Text("🔮 Der Bon wird entschlüsselt …", color = GameColors.Mana, fontWeight = FontWeight.Bold)
        } else {
            vm.scanError?.let {
                GameCard(borderColor = GameColors.Hp) { Text("⚠️ $it", color = GameColors.Hp) }
            }
            ChunkyButton("📷 FOTO AUFNEHMEN", Modifier.fillMaxWidth(), color = GameColors.Xp, shadow = GameColors.XpDark, textColor = Color.Black, height = 60.dp) {
                val dir = File(context.cacheDir, "images").apply { mkdirs() }
                val file = File(dir, "bon_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingUri = uri.toString()
                camera.launch(uri)
            }
            ChunkyButton("🖼️ AUS GALERIE WÄHLEN", Modifier.fillMaxWidth(), color = GameColors.Mana, shadow = GameColors.ManaDark, textColor = Color.Black) {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            ChunkyButton("✍️ MANUELL ERFASSEN", Modifier.fillMaxWidth()) { vm.startManual() }
            ChunkyButton("🧪 DEMO-BON LADEN", Modifier.fillMaxWidth(), color = GameColors.CardHi, shadow = GameColors.BgDeep) { vm.loadDemo() }
        }

        GameCard {
            Text("🎯 Tipps für perfekte Scans", fontWeight = FontWeight.Black, color = GameColors.Gold)
            Spacer(Modifier.height(6.dp))
            listOf(
                "Bon flach auf einen dunklen Untergrund legen",
                "Gutes Licht, kein Schatten vom Handy",
                "Ganzen Bon ins Bild – lange Bons ggf. in zwei Fotos",
                "Danach kurz prüfen: Du kannst alles korrigieren",
            ).forEach { Text("• $it", color = GameColors.TextDim, fontSize = 14.sp) }
            Spacer(Modifier.height(6.dp))
            Text("🔒 Die Texterkennung läuft komplett offline auf deinem Gerät.", color = GameColors.TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScannerVisual(active: Boolean) {
    val t = rememberInfiniteTransition(label = "scan")
    val line by t.animateFloat(0f, 1f, infiniteRepeatable(tween(if (active) 900 else 2200), RepeatMode.Reverse), label = "line")
    Box(
        Modifier
            .size(200.dp, 230.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(GameColors.CardHi, GameColors.BgDeep)))
            .border(3.dp, if (active) GameColors.Xp else GameColors.Border, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧾", fontSize = 90.sp)
            if (active) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(color = GameColors.Xp, modifier = Modifier.size(28.dp))
            } else {
                Text("Bereit für Beute!", color = GameColors.TextDim, textAlign = TextAlign.Center, fontSize = 13.sp)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter)
                .offset(y = (226 * line).dp)
                .background(GameColors.Xp.copy(alpha = 0.85f))
        )
    }
}
