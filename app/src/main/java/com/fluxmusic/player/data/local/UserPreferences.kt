package com.fluxmusic.player.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flux_settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_PRESET = intPreferencesKey("equalizer_preset")
        val WAVE_ENABLED = booleanPreferencesKey("wave_enabled")
        val WAVE_TYPE = intPreferencesKey("wave_type")
        val WAVE_COLOR = intPreferencesKey("wave_color")
        val WAVE_BAR_COUNT = intPreferencesKey("wave_bar_count")
        val WAVE_SPEED = intPreferencesKey("wave_speed")
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val LAST_TRACK_ID = longPreferencesKey("last_track_id")
        val LAST_POSITION = longPreferencesKey("last_position")
        val LAST_SHUFFLE = booleanPreferencesKey("last_shuffle")
        val LAST_REPEAT = intPreferencesKey("last_repeat")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val BALANCE_LEFT = floatPreferencesKey("balance_left")
        val BALANCE_RIGHT = floatPreferencesKey("balance_right")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val equalizerEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EQUALIZER_ENABLED] ?: false }
    val equalizerPreset: Flow<Int> = context.dataStore.data.map { it[Keys.EQUALIZER_PRESET] ?: 0 }
    val waveEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAVE_ENABLED] ?: true }
    val waveType: Flow<Int> = context.dataStore.data.map { it[Keys.WAVE_TYPE] ?: 0 }
    val waveColor: Flow<Int> = context.dataStore.data.map { it[Keys.WAVE_COLOR] ?: 0 }
    val waveBarCount: Flow<Int> = context.dataStore.data.map { it[Keys.WAVE_BAR_COUNT] ?: 28 }
    val waveSpeed: Flow<Int> = context.dataStore.data.map { it[Keys.WAVE_SPEED] ?: 1000 }
    val autoUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_UPDATE_ENABLED] ?: true }
    val sleepTimerMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SLEEP_TIMER_MINUTES] ?: 15 }
    val lastTrackId: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_TRACK_ID] ?: -1L }
    val lastPosition: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_POSITION] ?: 0L }
    val lastShuffle: Flow<Boolean> = context.dataStore.data.map { it[Keys.LAST_SHUFFLE] ?: false }
    val lastRepeat: Flow<Int> = context.dataStore.data.map { it[Keys.LAST_REPEAT] ?: 0 }
    val bassBoost: Flow<Float> = context.dataStore.data.map { it[Keys.BASS_BOOST] ?: 0.5f }
    val balanceLeft: Flow<Float> = context.dataStore.data.map { it[Keys.BALANCE_LEFT] ?: 1.0f }
    val balanceRight: Flow<Float> = context.dataStore.data.map { it[Keys.BALANCE_RIGHT] ?: 1.0f }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQUALIZER_ENABLED] = enabled }
    }

    suspend fun setEqualizerPreset(preset: Int) {
        context.dataStore.edit { it[Keys.EQUALIZER_PRESET] = preset }
    }

    suspend fun setWaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAVE_ENABLED] = enabled }
    }

    suspend fun setWaveType(type: Int) {
        context.dataStore.edit { it[Keys.WAVE_TYPE] = type }
    }

    suspend fun setWaveColor(color: Int) {
        context.dataStore.edit { it[Keys.WAVE_COLOR] = color }
    }

    suspend fun setWaveBarCount(count: Int) {
        context.dataStore.edit { it[Keys.WAVE_BAR_COUNT] = count.coerceIn(8, 64) }
    }

    suspend fun setWaveSpeed(speed: Int) {
        context.dataStore.edit { it[Keys.WAVE_SPEED] = speed.coerceIn(200, 3000) }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_UPDATE_ENABLED] = enabled }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_TIMER_MINUTES] = minutes }
    }

    suspend fun savePlaybackState(trackId: Long, position: Long, shuffle: Boolean, repeat: Int) {
        context.dataStore.edit {
            it[Keys.LAST_TRACK_ID] = trackId
            it[Keys.LAST_POSITION] = position
            it[Keys.LAST_SHUFFLE] = shuffle
            it[Keys.LAST_REPEAT] = repeat
        }
    }

    suspend fun setBassBoost(level: Float) {
        context.dataStore.edit { it[Keys.BASS_BOOST] = level.coerceIn(0f, 1f) }
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: 0 }
    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setBalance(left: Float, right: Float) {
        context.dataStore.edit {
            it[Keys.BALANCE_LEFT] = left.coerceIn(0f, 1f)
            it[Keys.BALANCE_RIGHT] = right.coerceIn(0f, 1f)
        }
    }
}
