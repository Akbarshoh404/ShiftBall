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
        val KEY_BEST_SCORE     = intPreferencesKey("best_score")
        val KEY_SOUND_EFFECTS  = booleanPreferencesKey("sound_effects")
        val KEY_MUSIC          = booleanPreferencesKey("music")
        val KEY_VIBRATION      = booleanPreferencesKey("vibration")
        val KEY_BALL_COLOR_IDX = intPreferencesKey("ball_color_idx")
        val KEY_DIFFICULTY     = stringPreferencesKey("difficulty")
        val KEY_APP_THEME      = stringPreferencesKey("app_theme")
    }

    private fun <T> flow(default: T, mapper: (Preferences) -> T): Flow<T> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { mapper(it) }

    val bestScore:    Flow<Int>     = flow(0)     { it[KEY_BEST_SCORE]     ?: 0 }
    val soundEffects: Flow<Boolean> = flow(true)  { it[KEY_SOUND_EFFECTS]  ?: true }
    val music:        Flow<Boolean> = flow(true)  { it[KEY_MUSIC]          ?: true }
    val vibration:    Flow<Boolean> = flow(true)  { it[KEY_VIBRATION]      ?: true }
    val ballColorIdx: Flow<Int>     = flow(0)     { it[KEY_BALL_COLOR_IDX] ?: 0 }
    val difficulty:   Flow<String>  = flow(Difficulty.NORMAL.name) { it[KEY_DIFFICULTY] ?: Difficulty.NORMAL.name }
    val appTheme:     Flow<String>  = flow(AppTheme.VOID.name)     { it[KEY_APP_THEME]  ?: AppTheme.VOID.name }

    suspend fun saveBestScore(score: Int)     = edit { it[KEY_BEST_SCORE]     = score }
    suspend fun saveSoundEffects(v: Boolean)  = edit { it[KEY_SOUND_EFFECTS]  = v }
    suspend fun saveMusic(v: Boolean)         = edit { it[KEY_MUSIC]          = v }
    suspend fun saveVibration(v: Boolean)     = edit { it[KEY_VIBRATION]      = v }
    suspend fun saveBallColorIdx(idx: Int)    = edit { it[KEY_BALL_COLOR_IDX] = idx }
    suspend fun saveDifficulty(name: String)  = edit { it[KEY_DIFFICULTY]     = name }
    suspend fun saveAppTheme(name: String)    = edit { it[KEY_APP_THEME]      = name }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
