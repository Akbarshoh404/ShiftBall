package uz.angrykitten.shiftball

import androidx.compose.ui.graphics.Color

// ─── Color Palette ───────────────────────────────────────────────────
val ColorBackground     = Color(0xFF0D0D1A)
val ColorWallTop        = Color(0xFF6D28D9)
val ColorWallMid        = Color(0xFF4C1D95)
val ColorWallBottom     = Color(0xFF7C3AED)
val ColorPlatformStart  = Color(0xFFEC4899)
val ColorPlatformEnd    = Color(0xFFF472B6)
val ColorBallCenter     = Color(0xFFC084FC)
val ColorBallEdge       = Color(0xFF7C3AED)
val ColorGem            = Color(0xFFFFBF24)
val ColorScore          = Color(0xFFE8D5FF)
val ColorAccent         = Color(0xFF7B5EA0)
val ColorStar           = Color(0xFFFFBF24)
val ColorStarEmpty      = Color(0xFF3D2F5E)

// Ball color options for settings
val BallColorOptions = listOf(
    Color(0xFFC084FC) to "Purple",
    Color(0xFFEC4899) to "Pink",
    Color(0xFF22D3EE) to "Cyan",
    Color(0xFFFBBF24) to "Amber"
)

// Difficulty settings
enum class Difficulty(val label: String, val speedMult: Float, val spawnRate: Float) {
    EASY("Easy", 0.75f, 0.6f),
    NORMAL("Normal", 1.0f, 1.0f),
    HARD("Hard", 1.5f, 1.5f)
}
