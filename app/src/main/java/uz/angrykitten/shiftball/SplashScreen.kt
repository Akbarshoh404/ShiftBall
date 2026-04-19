package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val theme = LocalVoidFallTheme.current

    LaunchedEffect(Unit) { delay(1900); onDone() }

    val inf = rememberInfiniteTransition(label = "s")
    val ballScale by inf.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bs")
    val glowR     by inf.animateFloat(0.8f,  1.2f,  infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "gr")
    val dotTick   by inf.animateFloat(0f, 3f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "dt")

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val alpha  by animateFloatAsState(if (appeared) 1f else 0f, tween(600), label = "a")
    val titleY by animateFloatAsState(if (appeared) 0f else 30f, tween(700, easing = FastOutSlowInEasing), label = "ty")

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.menuGradTop, theme.background))),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sw       = maxWidth
            val ballSize = (sw.value * 0.30f).coerceIn(100f, 130f).dp
            val titleFs  = (sw.value * 0.13f).coerceIn(40f, 56f).sp
            val subFs    = (sw.value * 0.030f).coerceIn(10f, 13f).sp

            Column(
                modifier            = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated glowing ball
                Canvas(modifier = Modifier.size(ballSize)) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val br = size.minDimension / 2.4f * ballScale
                    val gr = br * 1.9f * glowR
                    drawCircle(Brush.radialGradient(listOf(theme.ballEdge.copy(alpha = 0.2f), Color.Transparent), Offset(cx, cy), gr), gr, Offset(cx, cy))
                    drawCircle(Brush.radialGradient(listOf(theme.btnPrimary1.copy(alpha = 0.15f), Color.Transparent), Offset(cx, cy), gr * 1.5f), gr * 1.5f, Offset(cx, cy))
                    drawCircle(Brush.radialGradient(listOf(theme.ballCenter, theme.ballEdge), Offset(cx - br * 0.18f, cy - br * 0.18f), br), br, Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.34f), br * 0.26f, Offset(cx - br * 0.25f, cy - br * 0.28f))
                }

                Spacer(Modifier.height(28.dp))

                // Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { translationY = titleY }
                ) {
                    Text(
                        text          = "ShiftBall",
                        color         = theme.score,
                        fontSize      = titleFs,
                        fontFamily    = FontFamily.Default,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style         = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color      = theme.btnPrimary1.copy(alpha = if (theme.isLight) 0.3f else 0.55f),
                                offset     = Offset(0f, 8f),
                                blurRadius = 22f
                            )
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text          = "SHIFT · DODGE · SURVIVE",
                        color         = theme.accent,
                        fontSize      = subFs,
                        fontFamily    = FontFamily.Default,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(Modifier.height(56.dp))

                // Loading dots
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { i ->
                        val active = dotTick.toInt() % 3 == i
                        val a = if (active) 0.9f else 0.25f
                        Canvas(Modifier.size(7.dp)) { drawCircle(theme.ballCenter.copy(alpha = a)) }
                    }
                }
            }
        }
    }
}
