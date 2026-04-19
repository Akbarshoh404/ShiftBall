package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val xFrac: Float, val yFrac: Float,
    val radius: Float, val baseAlpha: Float, val driftSpeed: Float
)

@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val theme     = LocalVoidFallTheme.current
    val bestScore by settingsViewModel.bestScore.collectAsStateWithLifecycle()

    val inf = rememberInfiniteTransition(label = "menu_inf")

    // Glow pulse for the play button
    val glowPulse by inf.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    // Dot animation (revolving highlight)
    val dotTick by inf.animateFloat(
        0f, 3f,
        infiniteRepeatable(tween(780, easing = LinearEasing), RepeatMode.Restart),
        label = "dot"
    )
    // Stars drift
    val starTick by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "star"
    )

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val contentAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(550), label = "ca")
    val slideUp     by animateFloatAsState(if (appeared) 0f else 40f,  tween(600, easing = FastOutSlowInEasing), label = "su")

    val stars = remember {
        List(80) {
            Star(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.4f + 0.3f,
                Random.nextFloat() * 0.40f + 0.12f, Random.nextFloat() * 0.12f + 0.04f)
        }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (theme.isLight)
                    Brush.verticalGradient(listOf(theme.menuGradTop, theme.background))
                else
                    Brush.verticalGradient(listOf(theme.menuGradTop, theme.background, theme.background))
            )
    ) {
        // Starfield (dark themes only)
        if (!theme.isLight) {
            Canvas(Modifier.fillMaxSize()) {
                stars.forEach { s ->
                    val ay = (s.yFrac + starTick * s.driftSpeed) % 1f
                    val tw = 0.6f + 0.4f * sin(starTick * 2f * PI.toFloat() * s.driftSpeed * 5f)
                    drawCircle(Color.White.copy(alpha = s.baseAlpha * tw), s.radius, Offset(s.xFrac * size.width, ay * size.height))
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sw      = maxWidth
            val sh      = maxHeight
            val isSmall = sw < 360.dp
            val hPad    = responsiveDp(sw, 0.06f, 16f, 28f).dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .graphicsLayer { alpha = contentAlpha; translationY = slideUp },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))

                // ── BEST SCORE CARD ───────────────────────────────────────────
                BestScoreCard(bestScore = bestScore, theme = theme, sw = sw, hPad = hPad, onSettings = onSettings)

                Spacer(Modifier.height(48.dp))

                // ── FIRST SECTION: 3 CENTERED TEXTS ──────────────────────────
                Text(
                    text          = "· ENDLESS RUNNER ·",
                    color         = theme.accent,
                    fontSize      = responsiveSp(sw, 0.028f, 9f, 12f),
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text          = "ShiftBall",
                    color         = theme.score,
                    fontSize      = responsiveSp(sw, 0.13f, 40f, 58f),
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp,
                    style         = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color      = theme.btnPrimary1.copy(alpha = if (theme.isLight) 0.3f else 0.6f),
                            offset     = Offset(0f, 6f),
                            blurRadius = 18f
                        )
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Shift sides, survive the fall.",
                    color      = theme.accent,
                    fontSize   = responsiveSp(sw, 0.034f, 11f, 14f),
                    fontFamily = FontFamily.Default
                )

                Spacer(Modifier.height(36.dp))

                // ── SECOND SECTION: BIG CIRCULAR PLAY BUTTON ─────────────────
                val playDp = if (isSmall) 120.dp else 136.dp
                CirclePlayButton(size = playDp, theme = theme, glowPulse = glowPulse, onClick = onPlay)

                Spacer(Modifier.height(24.dp))

                // ── ANIMATED INDICATOR DOTS ───────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { i ->
                        val active   = dotTick.toInt() % 3 == i
                        val dotScale by animateFloatAsState(if (active) 1.5f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "ds$i")
                        val dotAlpha by animateFloatAsState(if (active) 0.9f else 0.25f, tween(200), label = "da$i")
                        Canvas(Modifier.size(7.dp).graphicsLayer { scaleX = dotScale; scaleY = dotScale }) {
                            drawCircle(theme.ballCenter.copy(alpha = dotAlpha))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Best Score Card ──────────────────────────────────────────────────────────
@Composable
private fun BestScoreCard(bestScore: Int, theme: VoidFallTheme, sw: Dp, hPad: Dp, onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPad)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surfaceCard)
    ) {
        // Thin top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(theme.btnPrimary1, theme.btnPrimary2)))
                .align(Alignment.TopCenter)
        )
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text          = "PERSONAL BEST",
                    color         = theme.accent,
                    fontSize      = responsiveSp(sw, 0.025f, 8f, 11f),
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(2.dp))
                // Score number + inline star
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = bestScore.toString(),
                        color      = theme.score,
                        fontSize   = responsiveSp(sw, 0.09f, 28f, 40f),
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = "★",
                        color    = theme.star,
                        fontSize = responsiveSp(sw, 0.048f, 16f, 22f)
                    )
                }
            }
            // Settings icon button — ⚙ unicode symbol (icon-like, not an emoji)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(theme.btnPrimary1.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onSettings() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "\u2699",  // ⚙ GEAR (U+2699, Miscellaneous Technical)
                    color      = theme.score.copy(alpha = 0.8f),
                    fontSize   = 20.sp,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}

// ─── Settings Row ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsRow(theme: VoidFallTheme, hPad: Dp, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "set_s")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = hPad)
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surfaceCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier              = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hamburger / settings icon
                Canvas(Modifier.size(20.dp)) {
                    val sw2   = 2f * density
                    val lines = listOf(size.height * 0.22f, size.height * 0.5f, size.height * 0.78f)
                    lines.forEach { y ->
                        drawLine(theme.score.copy(alpha = 0.9f), Offset(0f, y), Offset(size.width, y), sw2, StrokeCap.Round)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text          = "SETTINGS",
                    color         = theme.score,
                    fontSize      = 15.sp,
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
            // Right arrow
            Canvas(Modifier.size(16.dp)) {
                val sw2  = 2f * density
                val midY = size.height * 0.5f
                val tipX = size.width * 0.88f
                drawLine(theme.accent, Offset(0f, midY), Offset(tipX, midY), sw2, StrokeCap.Round)
                drawLine(theme.accent, Offset(tipX - size.width * 0.35f, 0f), Offset(tipX, midY), sw2, StrokeCap.Round)
                drawLine(theme.accent, Offset(tipX - size.width * 0.35f, size.height), Offset(tipX, midY), sw2, StrokeCap.Round)
            }
        }
    }
}

// ─── Circular Play Button ─────────────────────────────────────────────────────
@Composable
private fun CirclePlayButton(size: Dp, theme: VoidFallTheme, glowPulse: Float, onClick: () -> Unit) {
    val btnDp = size
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "p_s")

    Box(
        modifier = Modifier
            .size(btnDp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = if (theme.isLight) 8.dp else 24.dp, shape = CircleShape, spotColor = theme.btnPrimary1.copy(alpha = 0.65f))
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(theme.btnPrimary1, theme.btnPrimary2), radius = btnDp.value * 2f))
            .clickable(remember { MutableInteractionSource() }, null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Subtle glow ring
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f * glowPulse), this.size.minDimension / 2f * 0.9f)
        }
        // Play triangle
        Canvas(Modifier.size(btnDp * 0.36f)) {
            val w = this.size.width; val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.18f, 0f); lineTo(w, h * 0.5f); lineTo(w * 0.18f, h); close()
            }
            drawPath(path, Color.White)
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
private fun responsiveSp(sw: Dp, frac: Float, min: Float, max: Float) =
    (sw.value * frac).coerceIn(min, max).sp

private fun responsiveDp(sw: Dp, frac: Float, min: Float, max: Float) =
    (sw.value * frac).coerceIn(min, max)

private fun starPath5(cx: Float, cy: Float, outer: Float, inner: Float): Path {
    val path = Path(); val step = PI.toFloat() / 5
    for (i in 0 until 10) {
        val a = i * step - PI.toFloat() / 2f
        val r = if (i % 2 == 0) outer else inner
        if (i == 0) path.moveTo(cx + r * kotlin.math.cos(a), cy + r * kotlin.math.sin(a))
        else        path.lineTo(cx + r * kotlin.math.cos(a), cy + r * kotlin.math.sin(a))
    }
    path.close(); return path
}