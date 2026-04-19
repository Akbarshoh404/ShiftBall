package uz.angrykitten.shiftball

import androidx.compose.ui.graphics.Color

// ─── Wall Constants ───────────────────────────────────────────────────────────
const val WALL_WIDTH_DP   = 14f
const val BALL_RADIUS_DP  = 13f

// Platform spawning
const val PLATFORM_HEIGHT_DP  = 16f
const val PLATFORM_LENGTH_DP  = 90f   // extends inward from wall
const val PLATFORM_GAP_DP     = 240f  // minimum vertical gap
const val BASE_SCROLL_SPEED_DP = 160f // dp/s at normal difficulty

// ─── Game Status ─────────────────────────────────────────────────────────────
enum class GameStatus { IDLE, RUNNING, PAUSED, OVER }

// ─── Particle (ring/dash trail) ──────────────────────────────────────────────
data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val color: Color,
    // velocity for burst particles
    val vx: Float = 0f,
    val vy: Float = 0f,
    val isBurst: Boolean = false
)

// ─── Ball ────────────────────────────────────────────────────────────────────
enum class BallSide { LEFT, RIGHT }

data class Ball(
    val x: Float,
    val y: Float,
    val side: BallSide = BallSide.LEFT,
    val targetSide: BallSide = BallSide.LEFT,
    val radius: Float = BALL_RADIUS_DP
)

// ─── Platform ────────────────────────────────────────────────────────────────
enum class PlatformSide { LEFT, RIGHT }

data class Platform(
    val y: Float,
    val side: PlatformSide,
    val length: Float = PLATFORM_LENGTH_DP,
    val height: Float = PLATFORM_HEIGHT_DP,
    val id: Long = System.nanoTime()
)

// ─── Gem ─────────────────────────────────────────────────────────────────────
data class Gem(
    val x: Float,
    val y: Float,
    val size: Float = 12f,
    val id: Long = System.nanoTime(),
    val collected: Boolean = false
)

// ─── Game State ──────────────────────────────────────────────────────────────
data class GameState(
    val status: GameStatus = GameStatus.IDLE,
    val ball: Ball = Ball(0f, 0f),
    val platforms: List<Platform> = emptyList(),
    val gems: List<Gem> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val score: Int = 0,
    val gemsCollected: Int = 0,
    val bestScore: Int = 0,
    val scrollSpeed: Float = BASE_SCROLL_SPEED_DP,
    val screenShake: Float = 0f,
    val scoreTimer: Float = 0f,
    val gemCombo: Int = 0,           // consecutive gems collected
    val comboTimer: Float = 0f,      // resets combo after inactivity
    val isNewBest: Boolean = false
)
