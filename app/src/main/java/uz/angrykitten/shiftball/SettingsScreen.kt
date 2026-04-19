package uz.angrykitten.shiftball

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val soundEffects by viewModel.soundEffects.collectAsStateWithLifecycle()
    val music        by viewModel.music.collectAsStateWithLifecycle()
    val vibration    by viewModel.vibration.collectAsStateWithLifecycle()
    val ballColorIdx by viewModel.ballColorIdx.collectAsStateWithLifecycle()
    val difficulty   by viewModel.difficulty.collectAsStateWithLifecycle()
    val bestScore    by viewModel.bestScore.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(380),
        label         = "settings_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .graphicsLayer { this.alpha = alpha }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val hPad    = (screenW.value * 0.06f).coerceIn(16f, 28f).dp
            val labelFs = (screenW.value * 0.028f).coerceIn(9f, 12f).sp
            val bodyFs  = (screenW.value * 0.038f).coerceIn(13f, 16f).sp
            val headerFs = (screenW.value * 0.055f).coerceIn(18f, 23f).sp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1040))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text          = "SETTINGS",
                        color         = ColorScore,
                        fontSize      = headerFs,
                        fontFamily    = FontFamily.Default,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }

                SectionLabel("AUDIO", labelFs)

                SettingsToggleRow(
                    label   = "Sound Effects",
                    checked = soundEffects,
                    onToggle = { viewModel.toggleSoundEffects() },
                    bodyFs  = bodyFs
                )
                SettingsToggleRow(
                    label   = "Music",
                    checked = music,
                    onToggle = { viewModel.toggleMusic() },
                    bodyFs  = bodyFs
                )
                SettingsToggleRow(
                    label   = "Vibration",
                    checked = vibration,
                    onToggle = { viewModel.toggleVibration() },
                    bodyFs  = bodyFs
                )

                SectionLabel("BALL COLOR", labelFs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BallColorOptions.forEachIndexed { idx, (color, label) ->
                        val selected = idx == ballColorIdx
                        val scale by animateFloatAsState(
                            targetValue   = if (selected) 1.14f else 1f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label         = "color_scale_$idx"
                        )
                        Column(
                            modifier                 = Modifier.weight(1f),
                            horizontalAlignment      = Alignment.CenterHorizontally,
                            verticalArrangement      = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .clip(CircleShape)
                                    .background(
                                        if (selected)
                                            Brush.radialGradient(listOf(color, ColorBallEdge))
                                        else
                                            Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent))
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null
                                    ) { viewModel.setBallColorIdx(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text       = label,
                                color      = if (selected) color else ColorAccent,
                                fontSize   = labelFs,
                                fontFamily = FontFamily.Default,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                SectionLabel("DIFFICULTY", labelFs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1030))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val selected = diff == difficulty
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected)
                                        Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
                                    else
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { viewModel.setDifficulty(diff) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = diff.label,
                                color      = if (selected) Color.White else ColorAccent,
                                fontSize   = bodyFs,
                                fontFamily = FontFamily.Default,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                SectionLabel("STATISTICS", labelFs)

                // Best score card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF1E1040), Color(0xFF120A20)))
                        )
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text          = "PERSONAL BEST",
                                color         = ColorAccent,
                                fontSize      = labelFs,
                                fontFamily    = FontFamily.Default,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text       = bestScore.toString(),
                                color      = ColorScore,
                                fontSize   = (headerFs.value * 1.7f).sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text("🏆", fontSize = 36.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text          = text,
        color         = ColorAccent,
        fontSize      = fontSize,
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 3.sp
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    bodyFs: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1030))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onToggle
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = label,
            color      = ColorScore,
            fontSize   = bodyFs,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = Color(0xFF7C3AED),
                uncheckedThumbColor  = ColorAccent,
                uncheckedTrackColor  = Color(0xFF2A1850),
                uncheckedBorderColor = Color(0xFF3D2F5E)
            )
        )
    }
}
