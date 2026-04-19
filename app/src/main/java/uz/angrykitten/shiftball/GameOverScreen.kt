package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Confetti(
    val x: Float, val y: Float,
    val color: Color,
    val angle: Float, val speed: Float,
    val size: Float, val rotSpeed: Float
)

@Composable
fun GameOverScreen(
    score: Int, gems: Int, bestScore: Int, isNewBest: Boolean,
    onPlayAgain: () -> Unit, onMenu: () -> Unit
) {
    val theme = LocalVoidFallTheme.current

    val stars = when { score >= 60 -> 3; score >= 25 -> 2; score >= 8 -> 1; else -> 0 }

    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val cScale by animateFloatAsState(if (show) 1f else 0.85f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "cscale")
    val cAlpha by animateFloatAsState(if (show) 1f else 0f,    tween(450),                                                       label = "calpha")

    val starBounce by rememberInfiniteTransition(label = "sb").animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sp")
    val confettiTick by rememberInfiniteTransition(label = "ct").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "ctick")

    val confetti = remember {
        if (isNewBest) List(52) {
            val colors = listOf(Color(0xFFFFD700), Color(0xFFFF6FD8), Color(0xFF00C6FF), Color(0xFFA0FF8C), Color(0xFFFF9A3C))
            Confetti(Random.nextFloat(), Random.nextFloat() * 0.6f - 0.12f, colors.random(),
                Random.nextFloat() * 2f * PI.toFloat(), Random.nextFloat() * 0.16f + 0.06f,
                Random.nextFloat() * 5f + 3f, (Random.nextFloat() - 0.5f) * 4.5f)
        } else emptyList()
    }

    Box(
        modifier           = Modifier.fillMaxSize().background(theme.background),
        contentAlignment   = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(listOf(theme.platformStart.copy(alpha = 0.06f), Color.Transparent), Offset(size.width / 2f, size.height * 0.38f), size.width * 0.75f),
                radius = size.width * 0.75f, center = Offset(size.width / 2f, size.height * 0.38f)
            )
            if (isNewBest) {
                confetti.forEachIndexed { i, c ->
                    val animY = (c.y + confettiTick * c.speed + i * 0.013f) % 1.15f
                    val animX = c.x + sin(confettiTick * 2f * PI.toFloat() + i) * 0.018f
                    val rot   = confettiTick * c.rotSpeed * PI.toFloat()
                    withTransform({
                        translate(animX * size.width, animY * size.height)
                        rotate(rot * 180f / PI.toFloat())
                    }) {
                        drawRect(c.color.copy(alpha = 0.78f), Offset(-c.size / 2f, -c.size / 4f), Size(c.size, c.size / 2f))
                    }
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sw      = maxWidth
            val scoreFs = (sw.value * 0.175f).coerceIn(52f, 78f).sp
            val labelFs = (sw.value * 0.028f).coerceIn(9f, 13f).sp
            val titleFs = (sw.value * 0.072f).coerceIn(22f, 32f).sp
            val hPad    = (sw.value * 0.065f).coerceIn(16f, 30f).dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad)
                    .graphicsLayer { scaleX = cScale; scaleY = cScale; alpha = cAlpha }
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text          = if (isNewBest) "NEW BEST!" else "ROUND OVER",
                    color         = if (isNewBest) theme.gem else theme.platformEnd,
                    fontSize      = titleFs,
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 3.sp,
                    textAlign     = TextAlign.Center
                )

                // Score card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.surfaceCard)
                        .drawBehind {
                            val s = 1f * density
                            drawRoundRect(theme.divider, Offset(s, s), size.copy(size.width - s * 2, size.height - s * 2), CornerRadius(20.dp.toPx()), Stroke(s))
                        }
                        .padding(horizontal = 22.dp, vertical = 22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("YOUR SCORE", color = theme.accent, fontSize = labelFs, fontFamily = FontFamily.Default, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                        Text(score.toString(), color = theme.score, fontSize = scoreFs, fontFamily = FontFamily.Default, fontWeight = FontWeight.Black)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★", color = theme.star, fontSize = labelFs)
                            Spacer(Modifier.width(4.dp))
                            Text("Best: $bestScore", color = theme.star, fontSize = labelFs, fontFamily = FontFamily.Default)
                        }

                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))

                        // Stars row
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { i ->
                                val filled    = i < stars
                                val starScale = if (filled) 1f + starBounce * 0.12f else 1f
                                Text(
                                    "★",
                                    color    = if (filled) theme.star else theme.starEmpty,
                                    fontSize = (if (i == 1) 36 else 28).sp,
                                    modifier = Modifier.graphicsLayer { scaleX = starScale; scaleY = starScale }.padding(horizontal = 5.dp)
                                )
                            }
                        }

                        if (gems > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("◆", color = theme.gem, fontSize = labelFs)
                                Spacer(Modifier.width(4.dp))
                                Text("$gems gems", color = theme.accent, fontSize = labelFs, fontFamily = FontFamily.Default)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                GameOverButton("PLAY AGAIN", filled = true,  theme = theme, onClick = onPlayAgain)
                GameOverButton("MAIN MENU",  filled = false, theme = theme, onClick = onMenu)
            }
        }
    }
}

@Composable
private fun GameOverButton(text: String, filled: Boolean, theme: VoidFallTheme, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "btn_s")

    val fillBrush  = Brush.horizontalGradient(listOf(theme.btnPrimary1, theme.btnPrimary2))
    val emptyBrush = Brush.horizontalGradient(listOf(theme.btnPrimary1.copy(alpha = 0.1f), theme.btnPrimary2.copy(alpha = 0.1f)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) fillBrush else emptyBrush)
            .drawBehind {
                if (!filled) {
                    val sw = 1.2f * density
                    drawRoundRect(theme.btnOutline.copy(alpha = 0.45f), Offset(sw, sw), size.copy(size.width - sw * 2, size.height - sw * 2), CornerRadius(16.dp.toPx()), Stroke(sw))
                }
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = text,
            color         = if (filled) Color.White else theme.score.copy(alpha = 0.8f),
            fontSize      = 14.sp,
            fontFamily    = FontFamily.Default,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
