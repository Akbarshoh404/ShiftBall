package uz.angrykitten.shiftball

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ds = DataStoreManager(application)

    private val _soundEffects = MutableStateFlow(true)
    val soundEffects: StateFlow<Boolean> = _soundEffects

    private val _music = MutableStateFlow(true)
    val music: StateFlow<Boolean> = _music

    private val _vibration = MutableStateFlow(true)
    val vibration: StateFlow<Boolean> = _vibration

    private val _ballColorIdx = MutableStateFlow(0)
    val ballColorIdx: StateFlow<Int> = _ballColorIdx

    private val _difficulty = MutableStateFlow(Difficulty.NORMAL)
    val difficulty: StateFlow<Difficulty> = _difficulty

    private val _bestScore = MutableStateFlow(0)
    val bestScore: StateFlow<Int> = _bestScore

    val ballColor: Color get() = BallColorOptions[_ballColorIdx.value].first

    init {
        viewModelScope.launch {
            ds.soundEffects.collect { _soundEffects.value = it }
        }
        viewModelScope.launch {
            ds.music.collect { _music.value = it }
        }
        viewModelScope.launch {
            ds.vibration.collect { _vibration.value = it }
        }
        viewModelScope.launch {
            ds.ballColorIdx.collect { _ballColorIdx.value = it }
        }
        viewModelScope.launch {
            ds.difficulty.collect { name ->
                _difficulty.value = Difficulty.entries.find { it.name == name } ?: Difficulty.NORMAL
            }
        }
        viewModelScope.launch {
            ds.bestScore.collect { _bestScore.value = it }
        }
    }

    fun toggleSoundEffects() {
        val v = !_soundEffects.value
        _soundEffects.value = v
        viewModelScope.launch { ds.saveSoundEffects(v) }
    }

    fun toggleMusic() {
        val v = !_music.value
        _music.value = v
        viewModelScope.launch { ds.saveMusic(v) }
    }

    fun toggleVibration() {
        val v = !_vibration.value
        _vibration.value = v
        viewModelScope.launch { ds.saveVibration(v) }
    }

    fun setBallColorIdx(idx: Int) {
        _ballColorIdx.value = idx
        viewModelScope.launch { ds.saveBallColorIdx(idx) }
    }

    fun setDifficulty(diff: Difficulty) {
        _difficulty.value = diff
        viewModelScope.launch { ds.saveDifficulty(diff.name) }
    }

    fun updateBestScore(score: Int) {
        if (score > _bestScore.value) {
            _bestScore.value = score
            viewModelScope.launch { ds.saveBestScore(score) }
        }
    }
}
