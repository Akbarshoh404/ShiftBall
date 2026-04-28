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

    private val ctx = application.applicationContext
    private val ds  = DataStoreManager(application)

    private val _soundOn      = MutableStateFlow(true)
    private val _vibOn        = MutableStateFlow(true)
    private val _ballColorIdx = MutableStateFlow(0)
    private val _difficulty   = MutableStateFlow(Difficulty.NORMAL)
    private val _bestScore    = MutableStateFlow(0)

    // Track current theme's gem color so tick() can use it without CompositionLocal
    private var _gemColor = Color(0xFFFFBF24)

    val ballColor: Color get() = BallColorOptions[_ballColorIdx.value].first
    val bestScore: StateFlow<Int> = _bestScore

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    var screenWidthDp  = 0f
    var screenHeightDp = 0f

    // ── SoundPool for quick in-game SFX (gem collect) ─────────────────────────
    private var soundPool: SoundPool? = null
    private var sfxGem      = 0
    private var soundsReady = false

    @Suppress("DEPRECATION")
    private val vibrator =
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    init {
        viewModelScope.launch { ds.soundEffects.collect { _soundOn.value = it } }
        viewModelScope.launch { ds.vibration.collect    { _vibOn.value   = it } }
        viewModelScope.launch { ds.ballColorIdx.collect { _ballColorIdx.value = it } }
        viewModelScope.launch {
            ds.difficulty.collect { name ->
                _difficulty.value = Difficulty.entries.find { it.name == name } ?: Difficulty.NORMAL
            }
        }
        viewModelScope.launch { ds.bestScore.collect { _bestScore.value = it } }
        initSoundPool()
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()
                .also { sp ->
                    // Only load the gem sfx – the hit/over WAVs are empty placeholders
                    // so we use MediaPlayer one-shots for those instead.
                    sfxGem = sp.load(ctx, R.raw.sfx_gem, 1)
                    sp.setOnLoadCompleteListener { _, _, status ->
                        if (status == 0) soundsReady = true
                    }
                }
        } catch (_: Exception) { soundsReady = false }
    }

    /** Play a SoundPool sample if ready and sound is on. */
    private fun playPoolSound(id: Int) {
        if (_soundOn.value && soundsReady && id != 0)
            soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    /** Play a one-shot MediaPlayer sound (for important stings like lost.mp3). */
    private fun playOneShot(resId: Int) {
        if (!_soundOn.value) return
        MusicManager.playOneShot(ctx, resId)
    }

    private fun vibrate(ms: Long = 40) {
        if (_vibOn.value)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ── Game logic ────────────────────────────────────────────────────────────

    fun startGame() {
        val w    = screenWidthDp
        val h    = screenHeightDp
        if (w <= 0f || h <= 0f) return

        val wallW = WALL_WIDTH_DP
        val ballR = BALL_RADIUS_DP
        val leftX = wallW + ballR + 2f
        val ballY = h * 0.72f
        val diff  = _difficulty.value

        val maxPlatLen = (w * (0.44f - diff.speedMult * 0.04f)).coerceAtLeast(w * 0.26f)
        val minPlatLen = w * 0.24f

        val seeded = buildList {
            var nextY = ballY - PLATFORM_GAP_DP * 1.5f
            repeat(9) {
                val side = if (Random.nextBoolean()) PlatformSide.LEFT else PlatformSide.RIGHT
                val len  = Random.nextFloat() * (maxPlatLen - minPlatLen) + minPlatLen
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

    fun tick(dt: Float) {
        val st = _state.value
        if (st.status != GameStatus.RUNNING) return

        val w     = screenWidthDp
        val h     = screenHeightDp
        val wallW = WALL_WIDTH_DP
        val ballR = st.ball.radius
        val diff  = _difficulty.value

        val leftX  = wallW + ballR + 2f
        val rightX = w - wallW - ballR - 2f
        val targetX = if (st.ball.targetSide == BallSide.LEFT) leftX else rightX

        val lerpFactor = 1f - exp(-10f * dt)
        val newX = st.ball.x + (targetX - st.ball.x) * lerpFactor
        val newY = st.ball.y
        val newSide = if (abs(newX - leftX) < abs(newX - rightX)) BallSide.LEFT else BallSide.RIGHT

        val scroll = st.scrollSpeed * dt
        val movedPlatforms = st.platforms.map { p -> p.copy(y = p.y + scroll) }

        // ── Obstacle collision: die instantly ─────────────────────────────────
        var hitPlatform = false
        val survivedPlatforms = mutableListOf<Platform>()
        for (p in movedPlatforms) {
            val platLeft  = if (p.side == PlatformSide.LEFT) wallW else w - wallW - p.length
            val platRight = platLeft + p.length
            val ballLeft  = newX - ballR; val ballRight = newX + ballR
            val ballTop   = newY - ballR; val ballBot   = newY + ballR

            if (ballRight > platLeft && ballLeft < platRight && ballBot > p.y && ballTop < p.y + p.height)
                hitPlatform = true
            if (p.y < h + p.height + 50f) survivedPlatforms += p
        }
        if (hitPlatform) {
            // Sound + vibration BEFORE ending so they trigger right away
            playOneShot(R.raw.lost)   // play lost sting immediately on hit
            vibrate(120)
            endGame()
            return
        }

        // ── Gem collision + burst particles ───────────────────────────────────
        var gemsCollectedThisFrame = 0
        val burstParticles = mutableListOf<Particle>()
        val color  = ballColor
        val gemCol = _gemColor

        val survivedGems = st.gems.filter { gem ->
            val gx   = gem.x; val gy = gem.y + scroll
            val dist = sqrt((newX - gx).pow(2) + (newY - gy).pow(2))
            if (dist < ballR + gem.size) {
                gemsCollectedThisFrame++
                repeat(14) {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val speed = Random.nextFloat() * 180f + 60f
                    burstParticles += Particle(
                        x = gx, y = gy,
                        radius  = Random.nextFloat() * 3f + 1.5f,
                        alpha   = 0.9f,
                        color   = if (Random.nextBoolean()) gemCol else color,
                        vx      = cos(angle) * speed,
                        vy      = sin(angle) * speed,
                        isBurst = true
                    )
                }
                false
            } else true
        }.map { g -> g.copy(y = g.y + scroll) }.filter { g -> g.y - g.size < h + 50f }

        // ── Combo ─────────────────────────────────────────────────────────────
        var gemCombo   = st.gemCombo
        var comboTimer = st.comboTimer
        if (gemsCollectedThisFrame > 0) { gemCombo += gemsCollectedThisFrame; comboTimer = 2.5f }
        else { comboTimer -= dt; if (comboTimer <= 0f) gemCombo = 0 }
        val comboMult = when { gemCombo >= 6 -> 4; gemCombo >= 4 -> 3; gemCombo >= 2 -> 2; else -> 1 }

        // ── Trail particles ───────────────────────────────────────────────────
        val newParticles = buildList {
            addAll(st.particles.map { p ->
                if (p.isBurst) p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt + scroll, alpha = p.alpha - dt * 2.2f, vy = p.vy + 120f * dt)
                else p.copy(y = p.y + scroll, alpha = p.alpha - dt * 3.8f)
            }.filter { p -> p.alpha > 0f })
            addAll(burstParticles)
            repeat(2) { i ->
                add(Particle(x = newX + (if (i == 0) -4f else 4f), y = newY, radius = BALL_RADIUS_DP * (0.5f + Random.nextFloat() * 0.25f), alpha = 0.45f, color = color))
            }
        }

        // ── Score ─────────────────────────────────────────────────────────────
        var scoreTimer = st.scoreTimer + dt
        var newScore   = st.score + gemsCollectedThisFrame * 3 * comboMult
        if (scoreTimer >= 0.5f) { newScore += 1; scoreTimer -= 0.5f }

        // ── Speed ─────────────────────────────────────────────────────────────
        val base     = BASE_SCROLL_SPEED_DP * diff.speedMult
        val newSpeed = (base + (newScore / 8f) * 22f).coerceAtMost(base * 4.5f)

        // ── Spawn platform ────────────────────────────────────────────────────
        val updatedPlatforms = survivedPlatforms.toMutableList()
        val highestY = updatedPlatforms.minOfOrNull { it.y } ?: 0f
        var newlySpawned: Platform? = null
        if (highestY > -180f) {
            val maxPL = (w * (0.44f - diff.speedMult * 0.04f)).coerceAtLeast(w * 0.26f)
            val minPL = w * 0.24f
            val side  = if (Random.nextBoolean()) PlatformSide.LEFT else PlatformSide.RIGHT
            val gap   = PLATFORM_GAP_DP + Random.nextFloat() * (80f + diff.spawnRate * 60f)
            val len   = Random.nextFloat() * (maxPL - minPL) + minPL
            newlySpawned = Platform(y = highestY - gap, side = side, length = len)
            updatedPlatforms += newlySpawned
        }

        // ── Spawn gems ────────────────────────────────────────────────────────
        val updatedGems = survivedGems.toMutableList()
        if (survivedGems.size < 4 && Random.nextFloat() < 0.007f * diff.spawnRate) {
            val gemY = newlySpawned?.y?.plus(PLATFORM_GAP_DP * 0.5f) ?: -60f
            val gemX = if (newlySpawned != null) {
                if (newlySpawned.side == PlatformSide.LEFT) w - wallW * 2 - 24f else wallW * 2 + 24f
            } else wallW + 24f + Random.nextFloat() * (w - wallW * 2 - 48f)
            updatedGems += Gem(x = gemX, y = gemY)
        }

        // ── Gem SFX + vibration ───────────────────────────────────────────────
        val shake = (st.screenShake - dt * 4f).coerceAtLeast(0f)
        if (gemsCollectedThisFrame > 0) { playPoolSound(sfxGem); vibrate(18) }

        // ── Level milestone every 10 pts ──────────────────────────────────────
        val prevLevel = st.score / 10
        val nextLevel = newScore / 10
        if (nextLevel > prevLevel && nextLevel > 0) {
            MusicManager.playOneShot(ctx, R.raw.level)
            vibrate(55)
        }

        _state.value = st.copy(
            ball          = st.ball.copy(x = newX, y = newY, side = newSide),
            platforms     = updatedPlatforms, gems = updatedGems, particles = newParticles,
            score         = newScore, gemsCollected = st.gemsCollected + gemsCollectedThisFrame,
            scrollSpeed   = newSpeed, screenShake = shake, scoreTimer = scoreTimer,
            gemCombo      = gemCombo, comboTimer = comboTimer
        )
    }

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
        val st      = _state.value
        val newBest = maxOf(st.score, _bestScore.value)
        val isNewBest = st.score > 0 && st.score >= _bestScore.value
        _bestScore.value = newBest
        viewModelScope.launch { ds.saveBestScore(newBest) }
        _state.value = st.copy(status = GameStatus.OVER, bestScore = newBest, isNewBest = isNewBest)
    }

    fun resetGame() {
        _state.value = GameState(bestScore = _bestScore.value)
    }

    fun syncSettings(colorIdx: Int, diff: Difficulty, theme: VoidFallTheme) {
        _ballColorIdx.value = colorIdx
        _difficulty.value   = diff
        _gemColor           = theme.gem
    }

    override fun onCleared() {
        soundPool?.release()
        soundPool = null
        super.onCleared()
    }
}
