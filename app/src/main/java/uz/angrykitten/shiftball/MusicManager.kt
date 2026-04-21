package uz.angrykitten.shiftball

import android.content.Context
import android.media.MediaPlayer

/**
 * Manages background music across screens.
 * Only one track plays at a time; seamlessly swaps when the screen changes.
 */
object MusicManager {

    enum class Track { MENU, GAME, LOST, NONE }

    private var player: MediaPlayer? = null
    private var currentTrack: Track = Track.NONE
    private var musicEnabled: Boolean = true

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) stop()
        else if (currentTrack != Track.NONE) { /* caller must re-play */ }
    }

    /** Call when navigating to a screen. No-ops if the same track is already playing. */
    fun play(ctx: Context, track: Track) {
        if (!musicEnabled) return
        if (track == currentTrack && player?.isPlaying == true) return

        stop()
        currentTrack = track

        val resId = when (track) {
            Track.MENU -> R.raw.menu
            Track.GAME -> R.raw.game
            Track.LOST -> R.raw.lost
            Track.NONE -> return
        }

        player = MediaPlayer.create(ctx, resId)?.apply {
            isLooping = (track != Track.LOST)
            setVolume(0.75f, 0.75f)
            start()
        }
    }

    /** Pause without resetting position (e.g. app goes to background). */
    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    /** Resume after a pause. */
    fun resume() {
        if (!musicEnabled) return
        player?.takeIf { !it.isPlaying }?.start()
    }

    /** Stop and release current player completely. */
    fun stop() {
        player?.apply { if (isPlaying) stop(); release() }
        player = null
        currentTrack = Track.NONE
    }
}
