package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: (Int, Int) -> Unit,
    onMenu: () -> Unit
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    // Fix race condition: only set dims+startGame once when real size is known
    var dimsSet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wDp = maxWidth
        val hDp = maxHeight
        LaunchedEffect(wDp, hDp) {
            val wVal = with(density) { wDp.toPx() / density.density }
            val hVal = with(density) { hDp.toPx() / density.density }
            if (wVal > 0f && hVal > 0f) {
                viewModel.screenWidthDp  = wVal
                viewModel.screenHeightDp = hVal
                if (!dimsSet) {
                    dimsSet = true
                    viewModel.startGame()
                }
            }
        }
    }

    // Game loop — only ticks when RUNNING
    LaunchedEffect(Unit) {
        var lastNanos = -1L
        while (isActive) {
            withFrameNanos { nanos ->
                if (lastNanos < 0L) { lastNanos = nanos }
                val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = nanos
                viewModel.tick(dt)
            }
        }
    }

    // Navigate to game-over when status transitions to OVER
    var navigatedOver by remember { mutableStateOf(false) }
    LaunchedEffect(state.status) {
        if (state.status == GameStatus.OVER && !navigatedOver) {
            navigatedOver = true
            onGameOver(state.score, state.gemsCollected)
        }
    }

    // Shake
    val shakeX = if (state.screenShake > 0f)
        (sin(System.currentTimeMillis() * 0.055) * state.screenShake).toFloat()
    else 0f

    // Tap anywhere (except pause zone) to switch sides
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .offset(x = shakeX.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Top-right 56x56dp area → pause
                    val pxLimit = 64 * density.density
                    if (offset.x > size.width - pxLimit && offset.y < pxLimit) {
                        viewModel.togglePause()
                    } else {
                        viewModel.onTap()
                    }
                }
            }
    ) {
        val d = density.density

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w    = size.width
            val h    = size.height
            val wallW = WALL_WIDTH_DP * d

            drawGameBackground(w, h)
            drawGameWalls(w, h, wallW)

            // Platforms
            state.platforms.forEach { platform ->
                drawGamePlatform(platform, wallW, w, d)
            }

            // Gems
            state.gems.forEach { gem ->
                drawGameGem(gem, d)
            }

            // Ring/dash trail + burst particles
            state.particles.forEach { particle ->
                val a = particle.alpha.coerceIn(0f, 1f)
                if (particle.isBurst) {
                    // Small glowing dot burst
                    drawCircle(
                        color  = particle.color.copy(alpha = a),
                        radius = particle.radius * d,
                        center = Offset(particle.x * d, particle.y * d)
                    )
                } else {
                    // Ring trail — hollow circle outline
                    drawCircle(
                        color  = particle.color.copy(alpha = a * 0.55f),
                        radius = particle.radius * d,
                        center = Offset(particle.x * d, particle.y * d),
                        style  = Stroke(width = 1.8f * d)
                    )
                }
            }

            // Ball
            val ball = state.ball
            val bx   = ball.x * d
            val by   = ball.y * d
            val br   = ball.radius * d

            // Outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ColorBallEdge.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(bx, by),
                    radius = br * 2.6f
                ),
                radius = br * 2.6f,
                center = Offset(bx, by)
            )
            // Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(viewModel.ballColor, ColorBallEdge),
                    center = Offset(bx - br * 0.2f, by - br * 0.2f),
                    radius = br
                ),
                radius = br,
                center = Offset(bx, by)
            )
            // Specular
            drawCircle(
                color  = Color.White.copy(alpha = 0.38f),
                radius = br * 0.28f,
                center = Offset(bx - br * 0.28f, by - br * 0.28f)
            )
        }

        // ─ HUD ────────────────────────────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val scoreFs = (screenW.value * 0.115f).coerceIn(36f, 52f).sp
            val labelFs = (screenW.value * 0.032f).coerceIn(10f, 14f).sp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(top = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text          = "SCORE",
                    color         = Color(0xFFC09CF8).copy(alpha = 0.7f),
                    fontSize      = labelFs,
                    fontFamily    = FontFamily.Default,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = state.score.toString(),
                    color      = Color.White,
                    fontSize   = scoreFs,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Black
                )
                if (state.gemsCollected > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◆", color = ColorGem, fontSize = labelFs)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text      = "×${state.gemsCollected}",
                            color     = Color(0xFFE8D5FF),
                            fontSize  = labelFs,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
                // Combo display
                if (state.gemCombo >= 2) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text      = "COMBO ×${
                            when {
                                state.gemCombo >= 6 -> 4
                                state.gemCombo >= 4 -> 3
                                else                -> 2
                            }
                        }!",
                        color     = ColorGem.copy(alpha = 0.9f),
                        fontSize  = labelFs,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        letterSpacing = 2.sp
                    )
                }
            }

            // ─ Pause button (top-right) ───────────────────────────────
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .align(Alignment.TopEnd)
                    .drawBehind {
                        // Pause icon (two bars) or play icon
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = if (state.status == GameStatus.PAUSED) "▶" else "⏸",
                    color     = Color.White.copy(alpha = 0.85f),
                    fontSize  = 16.sp
                )
            }
        }

        // ─ Pause overlay ──────────────────────────────────────────────────
        if (state.status == GameStatus.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D0D1A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PAUSED",
                        color      = Color.White,
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Tap to resume",
                        color      = Color(0xFFE8D5FF).copy(alpha = 0.6f),
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}

// ── Canvas helpers ────────────────────────────────────────────────────────────

private fun DrawScope.drawGameBackground(w: Float, h: Float) {
    drawRect(ColorBackground, size = Size(w, h))
    // Subtle vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x55000000)),
            center = Offset(w / 2f, h / 2f),
            radius = w * 1.0f
        ),
        size = Size(w, h)
    )
}

private fun DrawScope.drawGameWalls(w: Float, h: Float, wallW: Float) {
    val wallBrush = Brush.verticalGradient(
        colors = listOf(ColorWallTop, ColorWallMid, ColorWallBottom),
        startY = 0f, endY = h
    )
    // Left wall
    drawRect(brush = wallBrush, topLeft = Offset(0f, 0f), size = Size(wallW, h))
    // Left inner glow
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(ColorWallBottom.copy(alpha = 0.3f), Color.Transparent),
            startX = wallW, endX = wallW + 24f
        ),
        topLeft = Offset(wallW, 0f), size = Size(24f, h)
    )
    // Right wall
    drawRect(brush = wallBrush, topLeft = Offset(w - wallW, 0f), size = Size(wallW, h))
    // Right inner glow
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, ColorWallBottom.copy(alpha = 0.3f)),
            startX = w - wallW - 24f, endX = w - wallW
        ),
        topLeft = Offset(w - wallW - 24f, 0f), size = Size(24f, h)
    )
}

private fun DrawScope.drawGamePlatform(
    platform: Platform,
    wallW: Float,
    w: Float,
    density: Float
) {
    val pH   = platform.height * density
    val pL   = platform.length * density
    val pY   = platform.y * density
    val left = if (platform.side == PlatformSide.LEFT) wallW else w - wallW - pL

    // Under-glow
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(ColorPlatformStart.copy(alpha = 0.22f), Color.Transparent),
            startY = pY + pH, endY = pY + pH + 18f
        ),
        topLeft      = Offset(left, pY + pH),
        size         = Size(pL, 18f),
        cornerRadius = CornerRadius(6f)
    )
    // Body
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = if (platform.side == PlatformSide.LEFT)
                listOf(ColorPlatformStart, ColorPlatformEnd)
            else
                listOf(ColorPlatformEnd, ColorPlatformStart),
            startX = left, endX = left + pL
        ),
        topLeft      = Offset(left, pY),
        size         = Size(pL, pH),
        cornerRadius = CornerRadius(8f)
    )
    // Top shine
    drawRoundRect(
        color        = Color.White.copy(alpha = 0.15f),
        topLeft      = Offset(left + 4f, pY + 2f),
        size         = Size(pL - 8f, pH * 0.38f),
        cornerRadius = CornerRadius(5f)
    )
}

private fun DrawScope.drawGameGem(gem: Gem, density: Float) {
    val cx = gem.x * density
    val cy = gem.y * density
    val s  = gem.size * density

    // Outer glow
    drawCircle(
        color  = ColorGem.copy(alpha = 0.22f),
        radius = s * 1.8f,
        center = Offset(cx, cy)
    )
    // Ring glow
    drawCircle(
        color  = ColorGem.copy(alpha = 0.35f),
        radius = s * 1.1f,
        center = Offset(cx, cy),
        style  = Stroke(width = 1.5f * density)
    )
    // Diamond path
    val path = Path().apply {
        moveTo(cx,             cy - s)
        lineTo(cx + s * 0.6f, cy)
        lineTo(cx,             cy + s)
        lineTo(cx - s * 0.6f, cy)
        close()
    }
    drawPath(
        path  = path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFE066), ColorGem),
            startY = cy - s, endY = cy + s
        )
    )
    // Inner highlight
    val inner = Path().apply {
        moveTo(cx, cy - s * 0.45f)
        lineTo(cx + s * 0.28f, cy)
        lineTo(cx, cy + s * 0.28f)
        lineTo(cx - s * 0.28f, cy)
        close()
    }
    drawPath(inner, color = Color.White.copy(alpha = 0.28f))
}
