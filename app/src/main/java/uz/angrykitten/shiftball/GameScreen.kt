package uz.angrykitten.shiftball

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: (Int, Int) -> Unit,
    onMenu: () -> Unit
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val theme   = LocalVoidFallTheme.current

    // Dimensions → startGame once when IDLE
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wDp = maxWidth; val hDp = maxHeight
        LaunchedEffect(wDp, hDp) {
            val wVal = with(density) { wDp.toPx() / density.density }
            val hVal = with(density) { hDp.toPx() / density.density }
            if (wVal > 0f && hVal > 0f) {
                viewModel.screenWidthDp  = wVal
                viewModel.screenHeightDp = hVal
                if (state.status == GameStatus.IDLE) viewModel.startGame()
            }
        }
    }

    // Frame loop
    LaunchedEffect(Unit) {
        var last = -1L
        while (isActive) {
            withFrameNanos { ns ->
                if (last < 0L) last = ns
                val dt = ((ns - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = ns; viewModel.tick(dt)
            }
        }
    }

    // Navigate on OVER
    var navigatedOver by remember { mutableStateOf(false) }
    LaunchedEffect(state.status) {
        if (state.status == GameStatus.OVER && !navigatedOver) {
            navigatedOver = true; onGameOver(state.score, state.gemsCollected)
        }
    }

    val shakeX = if (state.screenShake > 0f)
        (kotlin.math.sin(System.currentTimeMillis() * 0.055) * state.screenShake).toFloat()
    else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .offset(x = shakeX.dp)
    ) {
        // ── Game canvas (tap → switch sides) ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { viewModel.onTap() } }
        ) {
            val d = density.density
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w     = size.width; val h = size.height
                val wallW = WALL_WIDTH_DP * d
                drawGameBackground(theme, w, h)
                drawGameWalls(theme, w, h, wallW)
                state.platforms.forEach { drawGamePlatform(theme, it, wallW, w, d) }
                state.gems.forEach      { drawGameGem(theme, it, d) }

                state.particles.forEach { p ->
                    val a = p.alpha.coerceIn(0f, 1f)
                    if (p.isBurst) drawCircle(p.color.copy(alpha = a), p.radius * d, Offset(p.x * d, p.y * d))
                    else drawCircle(p.color.copy(alpha = a * 0.5f), p.radius * d, Offset(p.x * d, p.y * d), style = Stroke(1.6f * d))
                }

                val ball = state.ball
                val bx = ball.x * d; val by = ball.y * d; val br = ball.radius * d
                drawCircle(Brush.radialGradient(listOf(theme.ballEdge.copy(alpha = 0.3f), Color.Transparent), Offset(bx, by), br * 2.8f), br * 2.8f, Offset(bx, by))
                drawCircle(Brush.radialGradient(listOf(viewModel.ballColor, theme.ballEdge), Offset(bx - br * 0.2f, by - br * 0.2f), br), br, Offset(bx, by))
                drawCircle(Color.White.copy(alpha = 0.38f), br * 0.28f, Offset(bx - br * 0.28f, by - br * 0.28f))
            }

            // HUD
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val sw      = maxWidth
                val scoreFs = (sw.value * 0.11f).coerceIn(34f, 50f).sp
                val labelFs = (sw.value * 0.030f).coerceIn(10f, 13f).sp

                Column(
                    modifier            = Modifier.fillMaxWidth().systemBarsPadding().padding(top = 22.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SCORE", color = theme.score.copy(alpha = 0.55f), fontSize = labelFs, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(state.score.toString(), color = theme.score, fontSize = scoreFs, fontFamily = FontFamily.Default, fontWeight = FontWeight.Black)
                    if (state.gemsCollected > 0) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("◆", color = theme.gem, fontSize = labelFs)
                            Spacer(Modifier.width(4.dp))
                            Text("×${state.gemsCollected}", color = theme.score, fontSize = labelFs, fontFamily = FontFamily.Default)
                        }
                    }
                    if (state.gemCombo >= 2) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "COMBO ×${when { state.gemCombo >= 6 -> 4; state.gemCombo >= 4 -> 3; else -> 2 }}!",
                            color = theme.gem.copy(alpha = 0.9f), fontSize = labelFs, fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // ── PAUSE OVERLAY — styled like a loss/break screen ──────────────────
        if (state.status == GameStatus.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.overlayBg)
                    .pointerInput(Unit) { detectTapGestures { viewModel.togglePause() } },
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val sw  = maxWidth
                    val cardW = (sw.value * 0.84f).coerceIn(260f, 360f).dp

                    // Pause card (just like game-over card)
                    Box(
                        modifier = Modifier
                            .width(cardW)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(24.dp))
                            .background(theme.surfaceCard)
                            .drawBehind {
                                val s = 1f * density.density
                                drawRoundRect(
                                    color        = theme.btnPrimary1.copy(alpha = 0.3f),
                                    topLeft      = Offset(s, s),
                                    size         = androidx.compose.ui.geometry.Size(size.width - s * 2f, size.height - s * 2f),
                                    cornerRadius = CornerRadius(24.dp.toPx()),
                                    style        = Stroke(s)
                                )
                            }
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Pause icon
                            Canvas(modifier = Modifier.size(28.dp)) {
                                val bw   = this.size.width * 0.26f
                                val gap  = this.size.width * 0.2f
                                val left = (this.size.width - bw * 2 - gap) / 2f
                                drawRect(theme.score.copy(alpha = 0.8f), Offset(left, 0f), Size(bw, this.size.height))
                                drawRect(theme.score.copy(alpha = 0.8f), Offset(left + bw + gap, 0f), Size(bw, this.size.height))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text          = "PAUSED",
                                color         = theme.score,
                                fontSize      = (sw.value * 0.07f).coerceIn(22f, 30f).sp,
                                fontFamily    = FontFamily.Default,
                                fontWeight    = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = "tap anywhere to resume",
                                color      = theme.accent,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Default
                            )

                            Spacer(Modifier.height(22.dp))

                            // Divider
                            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.divider))

                            Spacer(Modifier.height(18.dp))

                            // Score + Best row
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn("SCORE", state.score.toString(), theme, sw)
                                // Vertical divider
                                Box(Modifier.width(1.dp).height(48.dp).background(theme.divider))
                                StatColumn("BEST", state.bestScore.toString(), theme, sw)
                            }

                            if (state.gemsCollected > 0) {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("◆", color = theme.gem, fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${state.gemsCollected} gems", color = theme.accent, fontSize = 12.sp, fontFamily = FontFamily.Default)
                                }
                            }

                            Spacer(Modifier.height(22.dp))

                            // Resume button
                            PauseActionButton(
                                text    = "RESUME",
                                filled  = true,
                                theme   = theme
                            ) { viewModel.togglePause() }

                            Spacer(Modifier.height(10.dp))

                            // Main menu button
                            PauseActionButton(
                                text    = "MAIN MENU",
                                filled  = false,
                                theme   = theme
                            ) { viewModel.resetGame(); onMenu() }
                        }
                    }
                }
            }
        }

        // ── Pause/Resume button — always Z-top ───────────────────────────────
        Box(
            modifier = Modifier
                .systemBarsPadding()
                .padding(top = 12.dp, end = 14.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (theme.isLight) theme.surfaceCard.copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.10f)
                )
                .clickable(remember { MutableInteractionSource() }, null) { viewModel.togglePause() }
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val isPaused = state.status == GameStatus.PAUSED
                val c = theme.score.copy(alpha = 0.85f)
                if (isPaused) {
                    val path = Path().apply {
                        moveTo(this@Canvas.size.width * 0.18f, 0f)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.5f)
                        lineTo(this@Canvas.size.width * 0.18f, this@Canvas.size.height)
                        close()
                    }
                    drawPath(path, c)
                } else {
                    val bw   = this.size.width * 0.27f
                    val gap  = this.size.width * 0.18f
                    val left = (this.size.width - bw * 2 - gap) / 2f
                    drawRect(c, Offset(left, 0f), Size(bw, this.size.height))
                    drawRect(c, Offset(left + bw + gap, 0f), Size(bw, this.size.height))
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, theme: VoidFallTheme, sw: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = theme.accent, fontSize = (sw.value * 0.025f).coerceIn(8f, 11f).sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = theme.score, fontSize = (sw.value * 0.085f).coerceIn(26f, 36f).sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PauseActionButton(text: String, filled: Boolean, theme: VoidFallTheme, onClick: () -> Unit) {
    val bg = if (filled) Brush.horizontalGradient(listOf(theme.btnPrimary1, theme.btnPrimary2))
             else Brush.horizontalGradient(listOf(theme.btnPrimary1.copy(alpha = 0.08f), theme.btnPrimary2.copy(alpha = 0.08f)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Outline for secondary button drawn via Canvas
        if (!filled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color        = theme.btnOutline.copy(alpha = 0.4f),
                    topLeft      = Offset(1f, 1f),
                    size         = Size(size.width - 2f, size.height - 2f),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style        = Stroke(1f)
                )
            }
        }
        Text(text, color = if (filled) Color.White else theme.score.copy(alpha = 0.8f), fontSize = 13.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

// ── Canvas helpers ────────────────────────────────────────────────────────────
private fun DrawScope.drawGameBackground(theme: VoidFallTheme, w: Float, h: Float) {
    drawRect(theme.background, size = Size(w, h))
    if (!theme.isLight) {
        drawRect(
            brush = Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)), Offset(w / 2f, h / 2f), w),
            size  = Size(w, h)
        )
    }
}

private fun DrawScope.drawGameWalls(theme: VoidFallTheme, w: Float, h: Float, wallW: Float) {
    val brush = Brush.verticalGradient(listOf(theme.wallTop, theme.wallMid, theme.wallBottom), 0f, h)
    drawRect(brush, Offset(0f, 0f), Size(wallW, h))
    drawRect(Brush.horizontalGradient(listOf(theme.wallBottom.copy(alpha = 0.28f), Color.Transparent), wallW, wallW + 22f), Offset(wallW, 0f), Size(22f, h))
    drawRect(brush, Offset(w - wallW, 0f), Size(wallW, h))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, theme.wallBottom.copy(alpha = 0.28f)), w - wallW - 22f, w - wallW), Offset(w - wallW - 22f, 0f), Size(22f, h))
}

private fun DrawScope.drawGamePlatform(theme: VoidFallTheme, p: Platform, wallW: Float, w: Float, d: Float) {
    val pH  = p.height * d; val pL = p.length * d; val pY = p.y * d
    val left = if (p.side == PlatformSide.LEFT) wallW else w - wallW - pL
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(theme.platformStart.copy(alpha = 0.2f), Color.Transparent), pY + pH, pY + pH + 16f),
        topLeft = Offset(left, pY + pH), size = Size(pL, 16f), cornerRadius = CornerRadius(5f)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            if (p.side == PlatformSide.LEFT) listOf(theme.platformStart, theme.platformEnd) else listOf(theme.platformEnd, theme.platformStart), left, left + pL
        ), topLeft = Offset(left, pY), size = Size(pL, pH), cornerRadius = CornerRadius(8f)
    )
    drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(left + 4f, pY + 2f), Size(pL - 8f, pH * 0.36f), CornerRadius(5f))
}

private fun DrawScope.drawGameGem(theme: VoidFallTheme, gem: Gem, d: Float) {
    val cx = gem.x * d; val cy = gem.y * d; val s = gem.size * d
    drawCircle(theme.gem.copy(alpha = 0.2f), s * 1.9f, Offset(cx, cy))
    drawCircle(theme.gem.copy(alpha = 0.3f), s * 1.1f, Offset(cx, cy), style = Stroke(1.4f * d))
    val path = Path().apply { moveTo(cx, cy - s); lineTo(cx + s * 0.6f, cy); lineTo(cx, cy + s); lineTo(cx - s * 0.6f, cy); close() }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFE066), theme.gem), cy - s, cy + s))
    val inner = Path().apply { moveTo(cx, cy - s * 0.44f); lineTo(cx + s * 0.27f, cy); lineTo(cx, cy + s * 0.27f); lineTo(cx - s * 0.27f, cy); close() }
    drawPath(inner, Color.White.copy(alpha = 0.26f))
}
