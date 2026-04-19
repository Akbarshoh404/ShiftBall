package uz.angrykitten.shiftball

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Theme Enum ───────────────────────────────────────────────────────────────
enum class AppTheme(val label: String) {
    VOID("Void"),
    NEON("Neon"),
    FIRE("Fire"),
    LIGHT("Light")
}

// ─── Theme Colors Data Class ──────────────────────────────────────────────────
data class VoidFallTheme(
    val id: AppTheme,
    val isLight: Boolean = false,
    val background: Color,
    val surfaceCard: Color,
    val wallTop: Color,
    val wallMid: Color,
    val wallBottom: Color,
    val platformStart: Color,
    val platformEnd: Color,
    val ballCenter: Color,
    val ballEdge: Color,
    val gem: Color,
    val score: Color,       // main text — dark on light, light on dark
    val accent: Color,      // muted secondary text
    val star: Color,
    val starEmpty: Color,
    val menuGradTop: Color,
    val btnPrimary1: Color,
    val btnPrimary2: Color,
    val btnOutline: Color,
    val divider: Color,
    val overlayBg: Color,   // pause/modal scrim
)

// ─── Dark: Void ───────────────────────────────────────────────────────────────
val VoidTheme = VoidFallTheme(
    id            = AppTheme.VOID,
    background    = Color(0xFF0D0D1A),
    surfaceCard   = Color(0xFF160D24),
    wallTop       = Color(0xFF6D28D9),
    wallMid       = Color(0xFF4C1D95),
    wallBottom    = Color(0xFF7C3AED),
    platformStart = Color(0xFFEC4899),
    platformEnd   = Color(0xFFF472B6),
    ballCenter    = Color(0xFFC084FC),
    ballEdge      = Color(0xFF7C3AED),
    gem           = Color(0xFFFFBF24),
    score         = Color(0xFFE8D5FF),
    accent        = Color(0xFF7B5EA0),
    star          = Color(0xFFFFBF24),
    starEmpty     = Color(0xFF3D2F5E),
    menuGradTop   = Color(0xFF15092B),
    btnPrimary1   = Color(0xFF9B6DFF),
    btnPrimary2   = Color(0xFF6D28D9),
    btnOutline    = Color(0xFF8B5CF6),
    divider       = Color(0x22FFFFFF),
    overlayBg     = Color(0xCC0A0A1A),
)

// ─── Dark: Neon ───────────────────────────────────────────────────────────────
val NeonTheme = VoidFallTheme(
    id            = AppTheme.NEON,
    background    = Color(0xFF050E14),
    surfaceCard   = Color(0xFF0A1E28),
    wallTop       = Color(0xFF0891B2),
    wallMid       = Color(0xFF0E7490),
    wallBottom    = Color(0xFF06B6D4),
    platformStart = Color(0xFF0D9488),
    platformEnd   = Color(0xFF2DD4BF),
    ballCenter    = Color(0xFF67E8F9),
    ballEdge      = Color(0xFF0891B2),
    gem           = Color(0xFFFFE44D),
    score         = Color(0xFFCCF5FF),
    accent        = Color(0xFF3B7A8F),
    star          = Color(0xFFFFE44D),
    starEmpty     = Color(0xFF1A3A44),
    menuGradTop   = Color(0xFF030C12),
    btnPrimary1   = Color(0xFF06B6D4),
    btnPrimary2   = Color(0xFF0891B2),
    btnOutline    = Color(0xFF22D3EE),
    divider       = Color(0x22FFFFFF),
    overlayBg     = Color(0xCC030C14),
)

// ─── Dark: Fire ───────────────────────────────────────────────────────────────
val FireTheme = VoidFallTheme(
    id            = AppTheme.FIRE,
    background    = Color(0xFF110806),
    surfaceCard   = Color(0xFF1C0E0A),
    wallTop       = Color(0xFFEA580C),
    wallMid       = Color(0xFFC2410C),
    wallBottom    = Color(0xFFF97316),
    platformStart = Color(0xFFDC2626),
    platformEnd   = Color(0xFFF87171),
    ballCenter    = Color(0xFFFBBF24),
    ballEdge      = Color(0xFFEF4444),
    gem           = Color(0xFF86EFAC),
    score         = Color(0xFFFFE4D6),
    accent        = Color(0xFF8F5040),
    star          = Color(0xFFFBBF24),
    starEmpty     = Color(0xFF3D1A10),
    menuGradTop   = Color(0xFF0D0503),
    btnPrimary1   = Color(0xFFEF4444),
    btnPrimary2   = Color(0xFFC2410C),
    btnOutline    = Color(0xFFF97316),
    divider       = Color(0x22FFFFFF),
    overlayBg     = Color(0xCC100606),
)

// ─── Light ────────────────────────────────────────────────────────────────────
val LightTheme = VoidFallTheme(
    id            = AppTheme.LIGHT,
    isLight       = true,
    background    = Color(0xFFF4F0FD),
    surfaceCard   = Color(0xFFFFFFFF),
    wallTop       = Color(0xFF7C3AED),
    wallMid       = Color(0xFF6D28D9),
    wallBottom    = Color(0xFF8B5CF6),
    platformStart = Color(0xFFDB2777),
    platformEnd   = Color(0xFFF472B6),
    ballCenter    = Color(0xFFA855F7),
    ballEdge      = Color(0xFF7C3AED),
    gem           = Color(0xFFD97706),
    score         = Color(0xFF1E0A32),  // dark text for light bg
    accent        = Color(0xFF7E6A9E),
    star          = Color(0xFFF59E0B),
    starEmpty     = Color(0xFFD8CCEE),
    menuGradTop   = Color(0xFFE9D8FF),
    btnPrimary1   = Color(0xFF9B6DFF),
    btnPrimary2   = Color(0xFF7C3AED),
    btnOutline    = Color(0xFF8B5CF6),
    divider       = Color(0x18000000),
    overlayBg     = Color(0xBBF0EAFF),
)

// ─── Helpers ──────────────────────────────────────────────────────────────────
val AllThemes: List<VoidFallTheme> = listOf(VoidTheme, NeonTheme, FireTheme, LightTheme)

fun themeByEnum(id: AppTheme): VoidFallTheme = AllThemes.first { it.id == id }

val LocalVoidFallTheme = compositionLocalOf<VoidFallTheme> { VoidTheme }

// ─── Ball color options ───────────────────────────────────────────────────────
val BallColorOptions = listOf(
    Color(0xFFC084FC) to "Purple",
    Color(0xFFEC4899) to "Pink",
    Color(0xFF22D3EE) to "Cyan",
    Color(0xFFFBBF24) to "Amber"
)

// ─── Difficulty ───────────────────────────────────────────────────────────────
enum class Difficulty(val label: String, val speedMult: Float, val spawnRate: Float) {
    EASY("Easy", 0.75f, 0.7f),
    NORMAL("Normal", 1.0f, 1.0f),
    HARD("Hard", 1.5f, 1.5f)
}
