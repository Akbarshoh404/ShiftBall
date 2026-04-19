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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Simple confetti data ────────────────────────────────────────────────────
private data class Confetti(
    val x: Float,
    val y: Float,
    val color: Color,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val rotSpeed: Float
)

@Composable
fun GameOverScreen(
    score: Int,
    gems: Int,
    bestScore: Int,
    isNewBest: Boolean,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    val stars = when {
        score >= 60 -> 3
        score >= 25 -> 2
        score >= 8  -> 1
        else        -> 0
    }

    // Entrance animation
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val contentScale by animateFloatAsState(
        targetValue   = if (show) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "content_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (show) 1f else 0f,
        animationSpec = tween(450),
        label         = "content_alpha"
    )

    val starBounce by rememberInfiniteTransition(label = "star_bounce").animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "star_pulse"
    )

    // Confetti ticker (for new best)
    val confettiTick by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label         = "confetti_tick"
    )

    val confetti = remember {
        if (isNewBest) List(48) {
            Confetti(
                x        = Random.nextFloat(),
                y        = Random.nextFloat() * 0.6f - 0.1f,
                color    = listOf(
                    Color(0xFFFFD700), Color(0xFFFF6FD8), Color(0xFF00C6FF),
                    Color(0xFFA0FF8C), Color(0xFFFF9A3C)
                ).random(),
                angle    = Random.nextFloat() * 2f * PI.toFloat(),
                speed    = Random.nextFloat() * 0.18f + 0.06f,
                size     = Random.nextFloat() * 5f + 3f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 4f
            )
        } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {
        // Background ambient glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(ColorPlatformStart.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.4f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width / 2f, size.height * 0.4f)
            )

            // Confetti
            if (isNewBest) {
                confetti.forEachIndexed { i, c ->
                    val animY  = (c.y + confettiTick * c.speed + i * 0.013f) % 1.15f
                    val animX  = c.x + sin(confettiTick * 2f * PI.toFloat() + i) * 0.02f
                    val rot    = confettiTick * c.rotSpeed * PI.toFloat()
                    val cx     = animX * size.width
                    val cy     = animY * size.height
                    val s      = c.size

                    withTransform({
                        translate(cx, cy)
                        rotate(rot * 180f / PI.toFloat())
                    }) {
                        drawRect(
                            color   = c.color.copy(alpha = 0.75f),
                            topLeft = Offset(-s / 2f, -s / 4f),
                            size    = Size(s, s / 2f)
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val scoreFs = (screenW.value * 0.18f).coerceIn(56f, 80f).sp
            val labelFs = (screenW.value * 0.029f).coerceIn(10f, 13f).sp
            val titleFs = (screenW.value * 0.075f).coerceIn(24f, 34f).sp
            val hPad    = (screenW.value * 0.065f).coerceIn(18f, 32f).dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad)
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                        alpha  = contentAlpha
                    }
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text          = if (isNewBest) "NEW BEST!" else "ROUND OVER",
                    color         = if (isNewBest) ColorGem else Color(0xFFF472B6),
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
                        .background(Color(0xFF160D24))
                        .drawBehind {
                            // Border
                            val strokeW = 1f * density
                            drawRoundRect(
                                color        = Color(0x44FFFFFF),
                                topLeft      = Offset(strokeW, strokeW),
                                size         = Size(size.width - strokeW * 2, size.height - strokeW * 2),
                                cornerRadius = CornerRadius(20.dp.toPx()),
                                style        = androidx.compose.ui.graphics.drawscope.Stroke(strokeW)
                            )
                        }
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text          = "YOUR SCORE",
                            color         = Color(0xFFAFA7C1),
                            fontSize      = labelFs,
                            fontFamily    = FontFamily.Default,
                            letterSpacing = 3.sp,
                            fontWeight    = FontWeight.SemiBold
                        )
                        Text(
                            text       = score.toString(),
                            color      = Color.White,
                            fontSize   = scoreFs,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black
                        )

                        // Best score row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "★",
                                color    = ColorStar,
                                fontSize = labelFs
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text       = "Best: $bestScore",
                                color      = ColorStar,
                                fontSize   = labelFs,
                                fontFamily = FontFamily.Default
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x28FFFFFF))
                        )

                        // Stars
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            repeat(3) { i ->
                                val filled    = i < stars
                                val starScale = if (filled) 1f + starBounce * 0.1f else 1f
                                Text(
                                    text     = "★",
                                    color    = if (filled) ColorStar else ColorStarEmpty,
                                    fontSize = (if (i == 1) 38 else 30).sp,
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = starScale; scaleY = starScale }
                                        .padding(horizontal = 6.dp)
                                )
                            }
                        }

                        // Gems
                        if (gems > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("◆", color = ColorGem, fontSize = labelFs)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text      = "$gems gems",
                                    color     = Color(0xFFAFA7C1),
                                    fontSize  = labelFs,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Action buttons
                GameOverButton(
                    text    = "▶  PLAY AGAIN",
                    filled  = true,
                    onClick = onPlayAgain
                )
                GameOverButton(
                    text    = "MAIN MENU",
                    filled  = false,
                    onClick = onMenu
                )
            }
        }
    }
}

@Composable
private fun GameOverButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val fillBrush  = Brush.horizontalGradient(listOf(Color(0xFFB462FE), Color(0xFF7C3AED)))
    val emptyBrush = Brush.horizontalGradient(listOf(Color(0x1AB462FE), Color(0x1A7C3AED)))

    // Press animation
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "btn_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) fillBrush else emptyBrush)
            .drawBehind {
                if (!filled) {
                    val strokeW = 1.2f * density
                    drawRoundRect(
                        color        = Color(0xFFB462FE).copy(alpha = 0.5f),
                        size         = size.copy(
                            width  = size.width  - strokeW,
                            height = size.height - strokeW
                        ),
                        topLeft      = Offset(strokeW / 2f, strokeW / 2f),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(strokeW)
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = text,
            color         = if (filled) Color.White else Color(0xFFE8D5FF),
            fontSize      = 15.sp,
            fontFamily    = FontFamily.Default,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
