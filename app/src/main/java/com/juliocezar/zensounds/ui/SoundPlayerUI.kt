package com.juliocezar.zensounds.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.EssencePreferences
import com.juliocezar.zensounds.data.SoundCatalog
import com.juliocezar.zensounds.services.PlaybackCommands
import com.juliocezar.zensounds.ui.theme.LocalEssencePalette
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel

@Composable
fun SoundPlayerScreen(
    navController: NavController,
    soundViewModel: SoundViewModel,
    initialSoundName: String,
    preferences: EssencePreferences,
    openMix: () -> Unit,
    initialMixId: String? = null
) {
    val categories by soundViewModel.soundCategories.collectAsState()
    val current by soundViewModel.selectedSound.collectAsState()
    val playRequested by soundViewModel.playWhenReady.collectAsState()
    val progress by soundViewModel.playbackProgress.collectAsState()
    val volume by soundViewModel.volume.collectAsState()
    val layers by soundViewModel.mixLayers.collectAsState()
    val activeMixName by soundViewModel.activeMixName.collectAsState()
    val prefs by preferences.state.collectAsState()
    val error by soundViewModel.playbackError.collectAsState()
    val sounds = categories.flatMap { it.sounds }.distinctBy { it.name }
    val isMixPlayer = initialMixId != null
    val savedMix = prefs.savedMixes.find { it.id == initialMixId }
    val showingMix = isMixPlayer && current == PlaybackCommands.MIX_ID
    val visibleLayers = if (showingMix) layers else savedMix?.layers.orEmpty()
    val sound = if (isMixPlayer) null else sounds.find { it.name == current } ?: sounds.find { it.name == initialSoundName }
    val title = if (isMixPlayer) activeMixName?.takeIf { showingMix } ?: savedMix?.name ?: stringResource(R.string.ess_mix_title)
    else localizedSoundName(sound?.name ?: initialSoundName)
    val artwork = if (isMixPlayer) SoundCatalog.artwork(visibleLayers.firstOrNull()?.soundName.orEmpty()) else SoundCatalog.artwork(sound?.name ?: initialSoundName)
    val hasPlayback = if (isMixPlayer) showingMix && layers.isNotEmpty() else current != null && current != PlaybackCommands.MIX_ID
    val canStart = if (isMixPlayer) hasPlayback || savedMix != null else true
    val p = LocalEssencePalette.current
    val context = LocalContext.current

    // Reabrir o player de um áudio pausado preserva a pausa e a posição.
    LaunchedEffect(initialSoundName, initialMixId) {
        if (isMixPlayer) {
            savedMix?.let(soundViewModel::openSavedMix)
        } else if (soundViewModel.selectedSound.value != initialSoundName) {
            soundViewModel.onSoundClicked(initialSoundName)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(EssenceIcons.Back, stringResource(R.string.zen_back), tint = p.text) }
            Text(stringResource(if (isMixPlayer) R.string.ess_mix_title else R.string.ess_ambient_sound), style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.weight(1f))
            if (!isMixPlayer) sound?.let { s ->
                IconButton(onClick = { preferences.toggleSound(s.name) }) {
                    Icon(if (s.name in prefs.favoriteSounds) EssenceIcons.Check else EssenceIcons.Heart,
                        stringResource(if (s.name in prefs.favoriteSounds) R.string.ess_remove_favorite else R.string.ess_add_favorite), tint = p.accent)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Image(painterResource(artwork), stringResource(R.string.zen_sound_artwork), Modifier.fillMaxWidth(.88f).aspectRatio(1f).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = p.text)
        Text(if (isMixPlayer) visibleLayers.joinToString(" + ") { LocalizedLabels.soundName(context, it.soundName) }
        else stringResource(R.string.ess_continuous_sound), style = MaterialTheme.typography.bodyMedium, color = p.secondaryText, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(26.dp))
        ThinProgress(if (hasPlayback) progress else 0f, Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (!isMixPlayer) IconButton(onClick = soundViewModel::onPreviousClicked) {
                Icon(EssenceIcons.Previous, stringResource(R.string.zen_previous), tint = p.text, modifier = Modifier.size(28.dp))
            }
            FilledIconButton(onClick = {
                if (hasPlayback) soundViewModel.onPlayPauseClicked()
                else if (isMixPlayer) savedMix?.let { soundViewModel.loadMix(it) }
                else soundViewModel.onSoundClicked(initialSoundName)
            }, enabled = canStart, modifier = Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = p.accent, contentColor = p.onAccent)) {
                val pause = hasPlayback && playRequested
                Icon(if (pause) EssenceIcons.Pause else EssenceIcons.Play, stringResource(if (pause) R.string.zen_pause else R.string.zen_play), modifier = Modifier.size(30.dp))
            }
            if (isMixPlayer) IconButton(onClick = soundViewModel::onStopClicked, enabled = hasPlayback) {
                Icon(EssenceIcons.Close, stringResource(R.string.ess_stop), tint = p.text, modifier = Modifier.size(28.dp))
            } else IconButton(onClick = soundViewModel::onNextClicked) {
                Icon(EssenceIcons.Next, stringResource(R.string.zen_next), tint = p.text, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(EssenceIcons.Volume, stringResource(R.string.zen_volume), tint = p.secondaryText)
            Spacer(Modifier.width(12.dp))
            Slider(volume, soundViewModel::onVolumeChanged, modifier = Modifier.weight(1f))
        }
        if (showingMix) visibleLayers.forEach { layer ->
            Text(stringResource(R.string.ess_layer_volume, localizedSoundName(layer.soundName)), color = p.secondaryText, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
            Slider(layer.volume, { soundViewModel.onLayerVolumeChanged(layer.soundName, it) })
        }
        TimerControls(soundViewModel)
        TextButton(onClick = openMix) {
            Icon(EssenceIcons.Sliders, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (isMixPlayer) R.string.ess_mix_edit else R.string.ess_my_mix))
        }
        if (error) Text(stringResource(R.string.ess_audio_error), color = p.accent)
        if (isMixPlayer && !canStart) Text(stringResource(R.string.ess_mix_empty), color = p.secondaryText)
        if (!isMixPlayer) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.zen_other_sounds), style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sounds.filter { it.name != current }, key = { it.name }) { other ->
                    Column(Modifier.width(85.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(other.backgroundResId), localizedSoundName(other.name), Modifier.size(72.dp).clip(CircleShape).clickable { soundViewModel.onSoundClicked(other.name) }, contentScale = ContentScale.Crop)
                        Text(localizedSoundName(other.name), style = MaterialTheme.typography.bodySmall, color = p.secondaryText, modifier = Modifier.padding(top = 7.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
