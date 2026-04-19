package uz.angrykitten.shiftball

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voidfall_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_BEST_SCORE      = intPreferencesKey("best_score")
        val KEY_SOUND_EFFECTS   = booleanPreferencesKey("sound_effects")
        val KEY_MUSIC           = booleanPreferencesKey("music")
        val KEY_VIBRATION       = booleanPreferencesKey("vibration")
        val KEY_BALL_COLOR_IDX  = intPreferencesKey("ball_color_idx")
        val KEY_DIFFICULTY      = stringPreferencesKey("difficulty")
    }

    val bestScore: Flow<Int> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_BEST_SCORE] ?: 0 }

    val soundEffects: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_SOUND_EFFECTS] ?: true }

    val music: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_MUSIC] ?: true }

    val vibration: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_VIBRATION] ?: true }

    val ballColorIdx: Flow<Int> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_BALL_COLOR_IDX] ?: 0 }

    val difficulty: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_DIFFICULTY] ?: Difficulty.NORMAL.name }

    suspend fun saveBestScore(score: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_BEST_SCORE] = score }
    }

    suspend fun saveSoundEffects(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SOUND_EFFECTS] = enabled }
    }

    suspend fun saveMusic(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MUSIC] = enabled }
    }

    suspend fun saveVibration(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VIBRATION] = enabled }
    }

    suspend fun saveBallColorIdx(idx: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_BALL_COLOR_IDX] = idx }
    }

    suspend fun saveDifficulty(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DIFFICULTY] = name }
    }
}
