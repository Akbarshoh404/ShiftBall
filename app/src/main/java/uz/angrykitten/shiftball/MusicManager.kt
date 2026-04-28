package uz.angrykitten.shiftball

import android.content.Context
import android.media.MediaPlayer

/**
 * Manages all background music across screens.
 * A single soundEnabled flag (driven by the "Sound Effects" toggle) controls everything.
 * Only one looping background track plays at a time; one-shot stings are on separate players.
 */
object MusicManager {

    enum class Track { MENU, GAME, LOST, NONE }

    private var bgPlayer: MediaPlayer? = null
    private var currentTrack: Track = Track.NONE
    private var soundEnabled: Boolean = true

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        if (!enabled) stopBg()
        else {
            // If a track was already set, restart it
            bgPlayer?.takeIf { !it.isPlaying }?.start()
        }
    }

    /** Switch background track. No-ops if the same track is already playing. */
    fun play(ctx: Context, track: Track) {
        if (track == Track.NONE) { stopBg(); return }
        // Already playing the right track – nothing to do
        if (track == currentTrack && bgPlayer?.isPlaying == true) return

        stopBg()
        currentTrack = track

        if (!soundEnabled) return          // respect the toggle

        val resId = when (track) {
            Track.MENU -> R.raw.menu
            Track.GAME -> R.raw.game
            Track.LOST -> R.raw.lost
            Track.NONE -> return
        }

        bgPlayer = MediaPlayer.create(ctx, resId)?.apply {
            isLooping = (track == Track.MENU || track == Track.GAME)
            setVolume(0.8f, 0.8f)
            start()
        }
    }

    /**
     * Play a one-shot level-up jingle without interrupting background music.
     * Safe to call even if level.wav is absent – it's loaded inside a try/catch.
     */
    fun playOneShot(ctx: Context, resId: Int) {
        if (!soundEnabled) return
        try {
            MediaPlayer.create(ctx, resId)?.apply {
                isLooping = false
                setVolume(0.9f, 0.9f)
                setOnCompletionListener { release() }
                start()
            }
        } catch (_: Exception) { /* file missing or decode error – silently skip */ }
    }

    /** Pause background (e.g. app goes to background). */
    fun pause() {
        bgPlayer?.takeIf { it.isPlaying }?.pause()
    }

    /** Resume background after a pause. */
    fun resume() {
        if (!soundEnabled) return
        bgPlayer?.takeIf { !it.isPlaying }?.start()
    }

    /** Stop and release the background player. */
    fun stopBg() {
        bgPlayer?.apply {
            runCatching { if (isPlaying) stop() }
            runCatching { release() }
        }
        bgPlayer = null
        currentTrack = Track.NONE
    }

    /** Alias used by MainActivity lifecycle. */
    fun stop() = stopBg()
}
