package com.fluxmusic.player.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxmusic.player.playback.AudioEqualizer
import com.fluxmusic.player.ui.components.WaveType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            SectionHeader("Sound")

            EqualizerSection(
                equalizerEnabled = state.equalizerEnabled,
                currentPreset = state.equalizerPreset,
                bassBoost = state.bassBoost,
                onEqualizerToggle = { viewModel.setEqualizerEnabled(it) },
                onPresetChange = { viewModel.setEqualizerPreset(it) },
                onBassBoostChange = { viewModel.setBassBoost(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("Wave Visualization")

            WaveSection(
                waveEnabled = state.waveEnabled,
                waveType = state.waveType,
                waveBarCount = state.waveBarCount,
                waveSpeed = state.waveSpeed,
                onWaveToggle = { viewModel.setWaveEnabled(it) },
                onWaveTypeChange = { viewModel.setWaveType(it) },
                onWaveBarCountChange = { viewModel.setWaveBarCount(it) },
                onWaveSpeedChange = { viewModel.setWaveSpeed(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("Theme")

            ThemeSection(
                themeMode = state.themeMode,
                dynamicColorEnabled = state.dynamicColorEnabled,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onDynamicColorChange = { viewModel.setDynamicColorEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("Updates")

            UpdateSection(
                autoUpdateEnabled = state.autoUpdateEnabled,
                isCheckingUpdate = state.isCheckingUpdate,
                updateStatus = state.updateStatus,
                currentVersion = state.currentVersion,
                onAutoUpdateToggle = { viewModel.setAutoUpdateEnabled(it) },
                onCheckUpdate = { viewModel.checkForUpdate() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("About")

            AboutSection(versionName = state.currentVersion)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun EqualizerSection(
    equalizerEnabled: Boolean,
    currentPreset: Int,
    bassBoost: Float,
    onEqualizerToggle: (Boolean) -> Unit,
    onPresetChange: (Int) -> Unit,
    onBassBoostChange: (Float) -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val centerFrequencies = remember { viewModel.getCenterFrequencies() }
    var bandLevels by remember(equalizerEnabled) { mutableStateOf(viewModel.getBandLevels().ifEmpty { List(8) { 0 } }) }

    SettingToggle(
        icon = Icons.Default.Tune,
        title = "Equalizer",
        description = if (equalizerEnabled) AudioEqualizer.PRESET_NAMES[currentPreset] ?: "Manual" else "Disabled",
        checked = equalizerEnabled,
        onCheckedChange = onEqualizerToggle
    )

    if (equalizerEnabled) {
        Text(
            text = "Preset",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioEqualizer.PRESET_NAMES.forEach { (preset, name) ->
                FilterChip(
                    selected = currentPreset == preset,
                    onClick = {
                        onPresetChange(preset)
                        bandLevels = viewModel.getBandLevels().ifEmpty { List(8) { 0 } }
                    },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Frequency Bands",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bandLevels.forEachIndexed { index, level ->
                val freqLabel = centerFrequencies.getOrElse(index) { 0 }.let {
                    if (it >= 1000) "${it / 1000}k" else "$it"
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        text = "${level / 100}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = (level.toFloat() + 1500) / 3000f,
                        onValueChange = { fraction ->
                            val newLevel = (fraction * 3000 - 1500).toInt()
                            viewModel.setBandLevel(index, newLevel)
                            bandLevels = viewModel.getBandLevels()
                        },
                        modifier = Modifier.height(120.dp),
                        valueRange = 0f..1f
                    )
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bass Boost: ${(bassBoost * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = bassBoost,
            onValueChange = onBassBoostChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun WaveSection(
    waveEnabled: Boolean,
    waveType: Int,
    waveBarCount: Int,
    waveSpeed: Int,
    onWaveToggle: (Boolean) -> Unit,
    onWaveTypeChange: (Int) -> Unit,
    onWaveBarCountChange: (Int) -> Unit,
    onWaveSpeedChange: (Int) -> Unit
) {
    SettingToggle(
        icon = Icons.Default.Waves,
        title = "Wave Visualization",
        description = "Show animated wave on Now Playing screen",
        checked = waveEnabled,
        onCheckedChange = onWaveToggle
    )

    if (waveEnabled) {
        Text(
            text = "Wave Style",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WaveType.entries.forEach { type ->
                FilterChip(
                    selected = waveType == type.ordinal,
                    onClick = { onWaveTypeChange(type.ordinal) },
                    label = { Text(type.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bar Count: $waveBarCount",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = waveBarCount.toFloat(),
            onValueChange = { onWaveBarCountChange(it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            valueRange = 8f..64f,
            steps = 55
        )

        Text(
            text = "Speed: ${waveSpeed}ms",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = waveSpeed.toFloat(),
            onValueChange = { onWaveSpeedChange(it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            valueRange = 200f..3000f,
            steps = 27
        )
    }
}

@Composable
private fun UpdateSection(
    autoUpdateEnabled: Boolean,
    isCheckingUpdate: Boolean,
    updateStatus: String?,
    currentVersion: String,
    onAutoUpdateToggle: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit
) {
    SettingToggle(
        icon = Icons.Default.SystemUpdate,
        title = "Auto-check Updates",
        description = "Automatically check GitHub for new versions",
        checked = autoUpdateEnabled,
        onCheckedChange = onAutoUpdateToggle
    )

    Button(
        onClick = onCheckUpdate,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        enabled = !isCheckingUpdate
    ) {
        if (isCheckingUpdate) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(if (isCheckingUpdate) "Checking..." else "Check for Updates")
    }

    updateStatus?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    Text(
        text = "Current version: v$currentVersion",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun AboutSection(versionName: String) {
    val context = LocalContext.current

    AboutItem(
        icon = Icons.Default.Info,
        title = "Version",
        description = "v$versionName"
    )
    AboutItem(
        icon = Icons.Default.Code,
        title = "Open Source",
        description = "MIT License"
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    AboutItem(
        icon = Icons.Default.Star,
        title = "GitHub",
        description = "github.com/egoffn1/Flux",
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/egoffn1/Flux"))
            context.startActivity(intent)
        }
    )
}

@Composable
private fun ThemeSection(
    themeMode: Int,
    dynamicColorEnabled: Boolean,
    onThemeModeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val themeOptions = listOf(
        0 to "System",
        1 to "Light",
        2 to "Dark"
    )

    SettingToggle(
        icon = Icons.Default.Palette,
        title = "Dynamic Colors",
        description = if (dynamicColorEnabled) "Material You colors" else "Custom palette",
        checked = dynamicColorEnabled,
        onCheckedChange = onDynamicColorChange
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Theme Mode",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themeOptions.forEach { (mode, label) ->
            FilterChip(
                selected = themeMode == mode,
                onClick = { onThemeModeChange(mode) },
                label = { Text(label) }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
