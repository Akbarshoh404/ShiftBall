package uz.angrykitten.shiftball

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val ds = DataStoreManager(application)

    // ─── Settings (live) ─────────────────────────────────────────────
    private val _soundOn      = MutableStateFlow(true)
    private val _vibOn        = MutableStateFlow(true)
    private val _ballColorIdx = MutableStateFlow(0)
    private val _difficulty   = MutableStateFlow(Difficulty.NORMAL)
    private val _bestScore    = MutableStateFlow(0)

    val ballColor: Color get() = BallColorOptions[_ballColorIdx.value].first
    val bestScore: StateFlow<Int> = _bestScore

    // ─── Game State ──────────────────────────────────────────────────
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    // ─── Screen dimensions (set by GameScreen on first layout) ───────
    var screenWidthDp  = 0f
    var screenHeightDp = 0f

    // ─── SoundPool ───────────────────────────────────────────────────
    private var soundPool: SoundPool? = null
    private var sfxHit     = 0
    private var sfxGem     = 0
    private var sfxOver    = 0
    private var soundsReady = false

    // ─── Vibrator ────────────────────────────────────────────────────
    private val vibrator =
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    init {
        viewModelScope.launch { ds.soundEffects.collect { _soundOn.value = it } }
        viewModelScope.launch { ds.vibration.collect   { _vibOn.value = it } }
        viewModelScope.launch { ds.ballColorIdx.collect { _ballColorIdx.value = it } }
        viewModelScope.launch {
            ds.difficulty.collect { name ->
                _difficulty.value = Difficulty.entries.find { it.name == name } ?: Difficulty.NORMAL
            }
        }
        viewModelScope.launch {
            ds.bestScore.collect { _bestScore.value = it }
        }
        initSoundPool(application)
    }

    private fun initSoundPool(ctx: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build().also { sp ->
            try {
                sfxHit  = sp.load(ctx, R.raw.sfx_hit,  1)
                sfxGem  = sp.load(ctx, R.raw.sfx_gem,  1)
                sfxOver = sp.load(ctx, R.raw.sfx_over, 1)
                sp.setOnLoadCompleteListener { _, _, _ -> soundsReady = true }
            } catch (e: Exception) {
                soundsReady = false
            }
        }
    }

    private fun playSound(id: Int) {
        if (_soundOn.value && soundsReady && id != 0)
            soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrate(ms: Long = 40) {
        if (_vibOn.value)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ─── Public API ──────────────────────────────────────────────────

    fun startGame() {
        val w = screenWidthDp
        val h = screenHeightDp
        if (w <= 0f || h <= 0f) return  // Guard against uninitialized dimensions

        val wallW  = WALL_WIDTH_DP
        val ballR  = BALL_RADIUS_DP
        val leftX  = wallW + ballR + 2f
        val ballY  = h * 0.72f
        val diff   = _difficulty.value

        // Platform length varies by difficulty
        val maxPlatLen = w * (0.42f - diff.speedMult * 0.04f).coerceAtLeast(0.28f)
        val minPlatLen = w * (0.26f)

        val seeded = buildList {
            var nextY = ballY - PLATFORM_GAP_DP * 1.5f
            repeat(9) {
                val side = if (Random.nextBoolean()) PlatformSide.LEFT else PlatformSide.RIGHT
                val len  = Random.nextFloat() * (maxPlatLen - minPlatLen) + minPlatLen
                // Gap varies: harder = bigger variation
                val gap  = PLATFORM_GAP_DP + Random.nextFloat() * (80f + diff.spawnRate * 60f)
                add(Platform(y = nextY, side = side, length = len))
                nextY -= gap
            }
        }

        _state.value = GameState(
            status      = GameStatus.RUNNING,
            ball        = Ball(x = leftX, y = ballY, side = BallSide.LEFT, targetSide = BallSide.LEFT),
            platforms   = seeded,
            scrollSpeed = BASE_SCROLL_SPEED_DP * diff.speedMult,
            bestScore   = _bestScore.value
        )
    }

    /** Called every frame by the game loop (dt in seconds). */
    fun tick(dt: Float) {
        val st = _state.value
        if (st.status != GameStatus.RUNNING) return

        val w     = screenWidthDp
        val h     = screenHeightDp
        val wallW = WALL_WIDTH_DP
        val ballR = st.ball.radius
        val diff  = _difficulty.value

        // ─ Horizontal Movement ─────────────────────────────────────
        val leftX  = wallW + ballR + 2f
        val rightX = w - wallW - ballR - 2f
        val targetX = if (st.ball.targetSide == BallSide.LEFT) leftX else rightX

        // Smooth, snappy ease-out
        val lerpFactor = 1f - exp(-10f * dt)
        val newX = st.ball.x + (targetX - st.ball.x) * lerpFactor
        val newY = st.ball.y

        val newSide = if (abs(newX - leftX) < abs(newX - rightX)) BallSide.LEFT else BallSide.RIGHT

        // ─ Scrolling & Collision (AABB) ────────────────────────────
        val scroll = st.scrollSpeed * dt

        val movedPlatforms = st.platforms.map { p -> p.copy(y = p.y + scroll) }

        var hitPlatform = false
        val survivedPlatforms = mutableListOf<Platform>()
        for (p in movedPlatforms) {
            val platLeft  = if (p.side == PlatformSide.LEFT) wallW else w - wallW - p.length
            val platRight = platLeft + p.length
            val platTop   = p.y
            val platBot   = p.y + p.height

            val ballLeft  = newX - ballR
            val ballRight = newX + ballR
            val ballTop   = newY - ballR
            val ballBot   = newY + ballR

            val overlap = ballRight > platLeft && ballLeft < platRight &&
                          ballBot > platTop   && ballTop < platBot

            if (overlap) hitPlatform = true
            if (p.y < h + p.height + 50f) survivedPlatforms += p
        }

        if (hitPlatform) {
            endGame()
            return
        }

        // ─ Gem collision ───────────────────────────────────────────
        var gemsCollectedThisFrame = 0
        var burstParticles = mutableListOf<Particle>()
        val color = ballColor

        val survivedGems = st.gems.filter { gem ->
            val gx   = gem.x
            val gy   = gem.y + scroll
            val dist = sqrt((newX - gx).pow(2) + (newY - gy).pow(2))
            if (dist < ballR + gem.size) {
                gemsCollectedThisFrame++
                // Burst particles on gem collect
                repeat(14) {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val speed = Random.nextFloat() * 180f + 60f
                    burstParticles.add(Particle(
                        x       = gx,
                        y       = gy,
                        radius  = Random.nextFloat() * 3f + 1.5f,
                        alpha   = 0.9f,
                        color   = if (Random.nextBoolean()) ColorGem else color,
                        vx      = cos(angle) * speed,
                        vy      = sin(angle) * speed,
                        isBurst = true
                    ))
                }
                false
            } else true
        }.map { g -> g.copy(y = g.y + scroll) }
            .filter { g -> g.y - g.size < h + 50f }

        // ─ Combo logic ─────────────────────────────────────────────
        var gemCombo   = st.gemCombo
        var comboTimer = st.comboTimer
        if (gemsCollectedThisFrame > 0) {
            gemCombo += gemsCollectedThisFrame
            comboTimer = 2.5f  // reset window
        } else {
            comboTimer -= dt
            if (comboTimer <= 0f) gemCombo = 0
        }
        val comboMult = when {
            gemCombo >= 6 -> 4
            gemCombo >= 4 -> 3
            gemCombo >= 2 -> 2
            else          -> 1
        }

        // ─ Particles (ring/dash trail + burst) ─────────────────────
        val trailColor = color

        val newParticles = buildList {
            // Advance existing particles
            addAll(st.particles
                .map { p ->
                    if (p.isBurst) {
                        p.copy(
                            x     = p.x + p.vx * dt,
                            y     = p.y + p.vy * dt + scroll,
                            alpha = p.alpha - dt * 2.2f,
                            vy    = p.vy + 120f * dt  // gravity
                        )
                    } else {
                        p.copy(
                            y     = p.y + scroll,
                            alpha = p.alpha - dt * 3.5f
                        )
                    }
                }
                .filter { p -> p.alpha > 0f })

            // Add burst particles from gem collection
            addAll(burstParticles)

            // Emit 2 ring/dash trail particles per frame
            repeat(2) { i ->
                val offset = if (i == 0) -4f else 4f
                add(Particle(
                    x      = newX + offset,
                    y      = newY,
                    radius = BALL_RADIUS_DP * (0.55f + Random.nextFloat() * 0.25f),
                    alpha  = 0.5f,
                    color  = trailColor
                ))
            }
        }

        // ─ Score tick ──────────────────────────────────────────────
        var scoreTimer = st.scoreTimer + dt
        var newScore   = st.score + gemsCollectedThisFrame * 3 * comboMult
        if (scoreTimer >= 0.5f) {
            newScore += 1
            scoreTimer -= 0.5f
        }

        // ─ Speed scaling ────────────────────────────────────────────
        // Gets meaningfully faster over time using difficulty multiplier
        val base     = BASE_SCROLL_SPEED_DP * diff.speedMult
        val newSpeed = (base + (newScore / 8f) * 22f).coerceAtMost(base * 4.5f)

        // ─ Spawn new platform if needed ────────────────────────────
        val updatedPlatforms = survivedPlatforms.toMutableList()
        val highestY = updatedPlatforms.minOfOrNull { it.y } ?: 0f

        var newlySpawnedPlatform: Platform? = null
        if (highestY > -180f) {
            val maxPlatLen = (w * (0.42f - diff.speedMult * 0.04f)).coerceAtLeast(w * 0.26f)
            val minPlatLen = w * 0.24f
            val side  = if (Random.nextBoolean()) PlatformSide.LEFT else PlatformSide.RIGHT
            val gap   = PLATFORM_GAP_DP + Random.nextFloat() * (80f + diff.spawnRate * 60f)
            val spawnY = highestY - gap
            val len   = Random.nextFloat() * (maxPlatLen - minPlatLen) + minPlatLen
            newlySpawnedPlatform = Platform(y = spawnY, side = side, length = len)
            updatedPlatforms += newlySpawnedPlatform
        }

        // Gem spawn logic
        val updatedGems = survivedGems.toMutableList()
        if (survivedGems.size < 4 && Random.nextFloat() < 0.007f * diff.spawnRate) {
            val gemY = newlySpawnedPlatform?.y?.plus(PLATFORM_GAP_DP * 0.5f) ?: -60f
            val gemX = if (newlySpawnedPlatform != null) {
                if (newlySpawnedPlatform.side == PlatformSide.LEFT)
                    w - wallW * 2 - 24f
                else
                    wallW * 2 + 24f
            } else {
                wallW + 24f + Random.nextFloat() * (w - wallW * 2 - 48f)
            }
            updatedGems += Gem(x = gemX, y = gemY)
        }

        // ─ Screen shake on hit ─────────────────────────────────────
        var shake = (st.screenShake - dt * 4f).coerceAtLeast(0f)
        if (hitPlatform) {
            shake = 7f
            playSound(sfxHit)
            vibrate(35)
        }
        if (gemsCollectedThisFrame > 0) {
            playSound(sfxGem)
            vibrate(18)
        }

        _state.value = st.copy(
            ball          = st.ball.copy(x = newX, y = newY, side = newSide),
            platforms     = updatedPlatforms,
            gems          = updatedGems,
            particles     = newParticles,
            score         = newScore,
            gemsCollected = st.gemsCollected + gemsCollectedThisFrame,
            scrollSpeed   = newSpeed,
            screenShake   = shake,
            scoreTimer    = scoreTimer,
            gemCombo      = gemCombo,
            comboTimer    = comboTimer
        )
    }

    /** Player tap: Toggle target wall. Only when RUNNING. */
    fun onTap() {
        val st = _state.value
        if (st.status != GameStatus.RUNNING) return
        val newTarget = if (st.ball.targetSide == BallSide.LEFT) BallSide.RIGHT else BallSide.LEFT
        _state.value = st.copy(ball = st.ball.copy(targetSide = newTarget))
    }

    fun togglePause() {
        val st = _state.value
        _state.value = when (st.status) {
            GameStatus.RUNNING -> st.copy(status = GameStatus.PAUSED)
            GameStatus.PAUSED  -> st.copy(status = GameStatus.RUNNING)
            else               -> st
        }
    }

    private fun endGame() {
        val st       = _state.value
        playSound(sfxOver)
        vibrate(90)
        val newBest  = maxOf(st.score, _bestScore.value)
        val isNewBest = st.score > 0 && st.score >= _bestScore.value
        _bestScore.value = newBest
        viewModelScope.launch { ds.saveBestScore(newBest) }
        _state.value = st.copy(
            status    = GameStatus.OVER,
            bestScore = newBest,
            isNewBest = isNewBest
        )
    }

    fun resetGame() {
        _state.value = GameState(bestScore = _bestScore.value)
    }

    fun syncSettings(colorIdx: Int, diff: Difficulty) {
        _ballColorIdx.value = colorIdx
        _difficulty.value   = diff
    }

    override fun onCleared() {
        soundPool?.release()
        soundPool = null
        super.onCleared()
    }
}
