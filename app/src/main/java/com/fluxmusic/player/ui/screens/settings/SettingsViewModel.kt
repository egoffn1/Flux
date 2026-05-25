package com.fluxmusic.player.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxmusic.player.data.local.UserPreferences
import com.fluxmusic.player.data.update.AppUpdater
import com.fluxmusic.player.data.update.AppUpdateInfo
import com.fluxmusic.player.playback.AudioEqualizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = 0,
    val waveEnabled: Boolean = true,
    val waveType: Int = 0,
    val waveBarCount: Int = 28,
    val waveSpeed: Int = 1000,
    val autoUpdateEnabled: Boolean = true,
    val bassBoost: Float = 0.5f,
    val currentVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateStatus: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val appUpdater: AppUpdater,
    private val audioEqualizer: AudioEqualizer
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }
        _state.update { it.copy(currentVersion = versionName) }

        viewModelScope.launch {
            userPreferences.equalizerEnabled.collect { v ->
                _state.update { it.copy(equalizerEnabled = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.equalizerPreset.collect { v ->
                _state.update { it.copy(equalizerPreset = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.waveEnabled.collect { v ->
                _state.update { it.copy(waveEnabled = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.waveType.collect { v ->
                _state.update { it.copy(waveType = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.waveBarCount.collect { v ->
                _state.update { it.copy(waveBarCount = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.waveSpeed.collect { v ->
                _state.update { it.copy(waveSpeed = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.autoUpdateEnabled.collect { v ->
                _state.update { it.copy(autoUpdateEnabled = v) }
            }
        }
        viewModelScope.launch {
            userPreferences.bassBoost.collect { v ->
                _state.update { it.copy(bassBoost = v) }
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setEqualizerEnabled(enabled)
        }
    }

    fun setEqualizerPreset(preset: Int) {
        viewModelScope.launch {
            userPreferences.setEqualizerPreset(preset)
            audioEqualizer.applyPreset(preset)
        }
    }

    fun setWaveEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setWaveEnabled(enabled) }
    }

    fun setWaveType(type: Int) {
        viewModelScope.launch { userPreferences.setWaveType(type) }
    }

    fun setWaveBarCount(count: Int) {
        viewModelScope.launch { userPreferences.setWaveBarCount(count) }
    }

    fun setWaveSpeed(speed: Int) {
        viewModelScope.launch { userPreferences.setWaveSpeed(speed) }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAutoUpdateEnabled(enabled) }
    }

    fun setBassBoost(level: Float) {
        viewModelScope.launch {
            userPreferences.setBassBoost(level)
            audioEqualizer.setBassBoost(level)
        }
    }

    fun checkForUpdate() {
        _state.update { it.copy(isCheckingUpdate = true, updateStatus = null) }

        appUpdater.checkAndUpdate { result ->
            result.onSuccess { info ->
                if (info != null) {
                    _state.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateStatus = "Update available: v${info.latestVersion}. Downloading..."
                        )
                    }
                    appUpdater.downloadAndInstall(info)
                } else {
                    _state.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateStatus = "You have the latest version"
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateStatus = "Error: ${error.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appUpdater.cleanup()
    }
}
