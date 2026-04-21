package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val theme        = LocalVoidFallTheme.current
    val soundEffects by viewModel.soundEffects.collectAsStateWithLifecycle()
    val music        by viewModel.music.collectAsStateWithLifecycle()
    val vibration    by viewModel.vibration.collectAsStateWithLifecycle()
    val ballColorIdx by viewModel.ballColorIdx.collectAsStateWithLifecycle()
    val difficulty   by viewModel.difficulty.collectAsStateWithLifecycle()
    val bestScore    by viewModel.bestScore.collectAsStateWithLifecycle()
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "sa")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .graphicsLayer { this.alpha = alpha }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sw      = maxWidth
            val hPad    = responsiveDp(sw, 0.06f, 16f, 28f).dp
            val labelFs = responsiveSp(sw, 0.027f, 9f, 12f)
            val bodyFs  = responsiveSp(sw, 0.038f, 13f, 16f)
            val headerFs = responsiveSp(sw, 0.052f, 17f, 22f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─ Header ────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    // Back button — canvas-drawn arrow, perfectly centered
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.surfaceCard)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val sw2   = 2f * density
                            val midY  = size.height * 0.5f
                            val tipX  = size.width * 0.12f
                            val endX  = size.width * 0.88f
                            val headH = size.height * 0.38f
                            val arrowColor = theme.score
                            // Shaft
                            drawLine(arrowColor, Offset(tipX, midY), Offset(endX, midY), strokeWidth = sw2, cap = StrokeCap.Round)
                            // Arrow head top
                            drawLine(arrowColor, Offset(tipX, midY), Offset(tipX + headH, midY - headH), strokeWidth = sw2, cap = StrokeCap.Round)
                            // Arrow head bottom
                            drawLine(arrowColor, Offset(tipX, midY), Offset(tipX + headH, midY + headH), strokeWidth = sw2, cap = StrokeCap.Round)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text          = "SETTINGS",
                        color         = theme.score,
                        fontSize      = headerFs,
                        fontFamily    = FontFamily.Default,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }

                // ─ Theme selector ─────────────────────────────────────────────
                SettingSection("THEME", labelFs, theme)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AllThemes.forEach { t ->
                        val selected = t.id == currentTheme
                        val scale by animateFloatAsState(if (selected) 1.0f else 0.94f, spring(Spring.DampingRatioMediumBouncy), label = "tscale_${t.id}")

                        Column(
                            modifier            = Modifier.weight(1f).graphicsLayer { scaleX = scale; scaleY = scale },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(t.btnPrimary1, t.btnPrimary2)))
                                    .then(
                                        if (selected) Modifier.drawBehind {
                                            drawRoundRect(Color.White.copy(alpha = 0.35f), style = Stroke(2f * density), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
                                        } else Modifier
                                    )
                                    .clickable(remember { MutableInteractionSource() }, null) { viewModel.setTheme(t.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Canvas(Modifier.size(14.dp)) {
                                        val path = Path().apply {
                                            moveTo(0f, size.height * 0.5f)
                                            lineTo(size.width * 0.38f, size.height)
                                            lineTo(size.width, 0f)
                                        }
                                        drawPath(path, Color.White, style = Stroke(width = 2.4f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                }
                            }
                            Text(t.id.label, color = if (selected) theme.score else theme.accent, fontSize = labelFs, fontFamily = FontFamily.Default, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // ─ Audio ─────────────────────────────────────────────────────
                SettingSection("AUDIO", labelFs, theme)
                SettingsToggleRow("Sound Effects", soundEffects, { viewModel.toggleSoundEffects() }, bodyFs, theme)
                SettingsToggleRow("Music",         music,        { viewModel.toggleMusic()        }, bodyFs, theme)
                SettingsToggleRow("Vibration",     vibration,    { viewModel.toggleVibration()    }, bodyFs, theme)

                // ─ Ball color ─────────────────────────────────────────────────
                SettingSection("BALL COLOR", labelFs, theme)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BallColorOptions.forEachIndexed { idx, (color, label) ->
                        val selected = idx == ballColorIdx
                        val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "cs_$idx")
                        Column(
                            modifier            = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .clip(CircleShape)
                                    .background(if (selected) Brush.radialGradient(listOf(color, theme.ballEdge)) else Brush.radialGradient(listOf(color.copy(alpha = 0.42f), Color.Transparent)))
                                    .clickable(remember { MutableInteractionSource() }, null) { viewModel.setBallColorIdx(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Canvas(Modifier.size(14.dp)) {
                                        val p = Path().apply { moveTo(0f, size.height * 0.5f); lineTo(size.width * 0.38f, size.height); lineTo(size.width, 0f) }
                                        drawPath(p, Color.White, style = Stroke(2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                }
                            }
                            Text(label, color = if (selected) color else theme.accent, fontSize = labelFs, fontFamily = FontFamily.Default, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // ─ Difficulty ────────────────────────────────────────────────
                SettingSection("DIFFICULTY", labelFs, theme)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surfaceCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val selected = diff == difficulty
                        Box(
                            modifier = Modifier
                                .weight(1f).height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Brush.horizontalGradient(listOf(theme.btnPrimary1, theme.btnPrimary2)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                                .clickable(remember { MutableInteractionSource() }, null) { viewModel.setDifficulty(diff) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(diff.label, color = if (selected) (if (theme.isLight) Color.White else Color.White) else theme.accent, fontSize = bodyFs, fontFamily = FontFamily.Default, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // ─ Stats ─────────────────────────────────────────────────────
                SettingSection("STATISTICS", labelFs, theme)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(theme.surfaceCard, theme.background)))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PERSONAL BEST", color = theme.accent, fontSize = labelFs, fontFamily = FontFamily.Default, letterSpacing = 2.sp)
                            Text(bestScore.toString(), color = theme.score, fontSize = responsiveSp(sw, 0.11f, 34f, 46f), fontFamily = FontFamily.Default, fontWeight = FontWeight.Black)
                        }
                        // Trophy icon via Canvas
                        Canvas(Modifier.size(38.dp)) {
                            val cx = size.width / 2f; val cy = size.height / 2f
                            // Simple star as trophy proxy
                            val path = starPath(cx, cy * 0.9f, size.minDimension * 0.42f, size.minDimension * 0.20f, 5)
                            drawPath(path, theme.star.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingSection(label: String, fs: TextUnit, theme: VoidFallTheme) {
    Text(label, color = theme.accent, fontSize = fs, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onToggle: () -> Unit, fs: TextUnit, theme: VoidFallTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.surfaceCard)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = theme.score, fontSize = fs, fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium)
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = theme.btnPrimary2,
                uncheckedThumbColor  = theme.accent,
                uncheckedTrackColor  = theme.background,
                uncheckedBorderColor = theme.accent.copy(alpha = 0.5f)
            )
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
private fun responsiveSp(sw: Dp, frac: Float, min: Float, max: Float): TextUnit = (sw.value * frac).coerceIn(min, max).sp
private fun responsiveDp(sw: Dp, frac: Float, min: Float, max: Float): Float    = (sw.value * frac).coerceIn(min, max)

private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, pts: Int): Path {
    val path = Path(); val step = Math.PI.toFloat() / pts
    for (i in 0 until pts * 2) {
        val a = i * step - Math.PI.toFloat() / 2f
        val r = if (i % 2 == 0) outerR else innerR
        if (i == 0) path.moveTo(cx + r * kotlin.math.cos(a), cy + r * kotlin.math.sin(a))
        else        path.lineTo(cx + r * kotlin.math.cos(a), cy + r * kotlin.math.sin(a))
    }
    path.close(); return path
}
