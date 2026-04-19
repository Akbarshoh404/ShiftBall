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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private data class Star(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val baseAlpha: Float,
    val driftSpeed: Float
)

@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_inf")

    // Ball pulse
    val ballScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "ball_scale"
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue  = 26f, targetValue = 46f,
        animationSpec = infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow_r"
    )
    val starTick by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label         = "star_tick"
    )

    // Entrance animation
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val titleAlpha by animateFloatAsState(
        targetValue   = if (appeared) 1f else 0f,
        animationSpec = tween(700, delayMillis = 150),
        label         = "ta"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue   = if (appeared) 1f else 0f,
        animationSpec = tween(600, delayMillis = 420),
        label         = "ba"
    )

    // Stable stars
    val stars = remember {
        List(100) {
            Star(
                xFrac      = Random.nextFloat(),
                yFrac      = Random.nextFloat(),
                radius     = Random.nextFloat() * 1.8f + 0.3f,
                baseAlpha  = Random.nextFloat() * 0.5f + 0.15f,
                driftSpeed = Random.nextFloat() * 0.18f + 0.06f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF15092B), ColorBackground, ColorBackground)
                )
            )
    ) {
        // Starfield
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { star ->
                val animY = (star.yFrac + starTick * star.driftSpeed) % 1f
                val twinkle = 0.7f + 0.3f * kotlin.math.sin(starTick * 2f * kotlin.math.PI.toFloat() * star.driftSpeed * 4f)
                drawCircle(
                    color  = Color.White.copy(alpha = star.baseAlpha * twinkle),
                    radius = star.radius,
                    center = Offset(star.xFrac * size.width, animY * size.height)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val titleFs  = (screenW.value * 0.135f).coerceIn(38f, 60f).sp
            val subFs    = (screenW.value * 0.025f).coerceIn(9f, 12f).sp
            val ballSize = (screenW.value * 0.27f).coerceIn(88f, 120f).dp
            val hPad     = (screenW.value * 0.08f).coerceIn(24f, 40f).dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = hPad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1.2f))

                // Animated glowing ball
                Canvas(modifier = Modifier.size(ballSize)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val br = (size.minDimension / 2.6f) * ballScale

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ColorBallEdge.copy(alpha = 0.32f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = glowRadius * (size.minDimension / 110f)
                        ),
                        radius = glowRadius * (size.minDimension / 110f),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ColorBallCenter, ColorBallEdge),
                            center = Offset(cx - br * 0.18f, cy - br * 0.18f),
                            radius = br
                        ),
                        radius = br,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.30f),
                        radius = br * 0.26f,
                        center = Offset(cx - br * 0.26f, cy - br * 0.3f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Title block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer { alpha = titleAlpha; translationY = (1f - titleAlpha) * -24f }
                ) {
                    Text(
                        text          = "VOIDFALL",
                        color         = Color.White,
                        fontSize      = titleFs,
                        fontFamily    = FontFamily.Default,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 5.sp,
                        style         = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color      = Color(0x888B5CF6),
                                offset     = Offset(0f, 10f),
                                blurRadius = 28f
                            )
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1A8B5CF6))
                            .drawBehind {
                                val s = 1f * density
                                drawRoundRect(
                                    color        = Color(0x338B5CF6),
                                    topLeft      = Offset(s, s),
                                    size         = Size(size.width - s*2, size.height - s*2),
                                    cornerRadius = CornerRadius(10.dp.toPx()),
                                    style        = androidx.compose.ui.graphics.drawscope.Stroke(s)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text          = "DODGE  ·  COLLECT  ·  SURVIVE",
                            color         = Color(0xFFE8D5FF).copy(alpha = 0.8f),
                            fontSize      = subFs,
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = buttonAlpha },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuButton(label = "▶  PLAY",   filled = true,  onClick = onPlay)
                    MenuButton(label = "SETTINGS",  filled = false, onClick = onSettings)
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun MenuButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val fillBrush  = Brush.horizontalGradient(listOf(Color(0xFF9B6DFF), Color(0xFF6D28D9)))
    val emptyBrush = Brush.horizontalGradient(listOf(Color(0x1A9B6DFF), Color(0x1A6D28D9)))

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "menu_btn_scale"
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
                        color        = Color(0xFF8B5CF6),
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
            text          = label,
            color         = if (filled) Color.White else Color(0xFFE0D0FF),
            fontSize      = 15.sp,
            fontFamily    = FontFamily.Default,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 3.sp
        )
    }
}
