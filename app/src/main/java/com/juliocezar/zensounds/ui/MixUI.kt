package com.juliocezar.zensounds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.*
import com.juliocezar.zensounds.services.PlaybackCommands
import com.juliocezar.zensounds.ui.theme.LocalEssencePalette
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel
import com.juliocezar.zensounds.widgets.DailyWordWidgets
import java.util.Locale

fun timerLabel(remaining: Long): String {
    val seconds = (remaining.coerceAtLeast(0L) + 999L) / 1000L
    return if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}

@Composable
fun SoundMixScreen(viewModel: SoundViewModel, preferences: EssencePreferences, openSavedMix: (String) -> Unit, back: () -> Unit) {
    val layers by viewModel.mixLayers.collectAsState()
    val playing by viewModel.playWhenReady.collectAsState()
    val current by viewModel.selectedSound.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val error by viewModel.playbackError.collectAsState()
    val prefs by preferences.state.collectAsState()
    val p = LocalEssencePalette.current
    val context = LocalContext.current
    var saveOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(EssenceIcons.Back, stringResource(R.string.zen_back), tint = p.text) }
                Text(stringResource(R.string.ess_mix_title), style = MaterialTheme.typography.headlineMedium, color = p.text)
            }
            Text(stringResource(R.string.ess_mix_description), color = p.secondaryText, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.lilac).padding(18.dp)) {
                Text(if (layers.isEmpty()) stringResource(R.string.ess_mix_empty) else pluralStringResource(R.plurals.ess_mix_layer_count, layers.size, layers.size), color = p.text)
                if (layers.isEmpty() && current != null && current != PlaybackCommands.MIX_ID) {
                    TextButton(onClick = viewModel::useCurrentSoundInMix) { Text(stringResource(R.string.ess_mix_current)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = viewModel::onPlayPauseClicked, enabled = layers.isNotEmpty(), modifier = Modifier.size(56.dp)) {
                        Icon(if (playing) EssenceIcons.Pause else EssenceIcons.Play, stringResource(if (playing) R.string.zen_pause else R.string.zen_play))
                    }
                    IconButton(onClick = viewModel::onStopClicked, enabled = layers.isNotEmpty()) { Icon(EssenceIcons.Close, stringResource(R.string.ess_stop), tint = p.text) }
                    TextButton(onClick = { saveOpen = true }, enabled = layers.isNotEmpty()) { Text(stringResource(R.string.ess_mix_save)) }
                }
                Text(stringResource(R.string.ess_master_volume), color = p.secondaryText, style = MaterialTheme.typography.bodySmall)
                Slider(volume, viewModel::onVolumeChanged)
                TimerControls(viewModel)
            }
            if (error) Text(stringResource(R.string.ess_audio_error), color = p.accent, modifier = Modifier.padding(top = 8.dp))
        }
        items(MixRules.soundNames, key = { it }) { name ->
            val layer = layers.find { it.soundName == name }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (layer == null) p.surface else p.lilac).padding(12.dp)) {
                Row(Modifier.fillMaxWidth().clickable { viewModel.toggleMixLayer(name) }, verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(SoundCatalog.artwork(name)), null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Text(localizedSoundName(name), color = p.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.toggleMixLayer(name) }) {
                        Icon(if (layer == null) EssenceIcons.Plus else EssenceIcons.Delete, stringResource(if (layer == null) R.string.ess_add_layer else R.string.ess_remove_layer), tint = p.accent)
                    }
                }
                if (layer != null) {
                    Text(stringResource(R.string.ess_layer_volume, localizedSoundName(name)), color = p.secondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    Slider(value = layer.volume, onValueChange = { viewModel.onLayerVolumeChanged(name, it) })
                }
            }
        }
        item { Text(stringResource(R.string.ess_mix_saved), color = p.text, style = MaterialTheme.typography.titleLarge) }
        if (prefs.savedMixes.isEmpty()) item { Text(stringResource(R.string.ess_mix_saved_empty), color = p.secondaryText) }
        items(prefs.savedMixes, key = { it.id }) { mix ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { openSavedMix(mix.id) }.padding(6.dp)) {
                    Text(mix.name, color = p.text, style = MaterialTheme.typography.titleMedium)
                    Text(mix.layers.joinToString(" + ") { LocalizedLabels.soundName(context, it.soundName) }, color = p.secondaryText, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { openSavedMix(mix.id) }) { Icon(EssenceIcons.Play, stringResource(R.string.ess_mix_play), tint = p.accent) }
                IconButton(onClick = { preferences.removeMix(mix.id) }) { Icon(EssenceIcons.Delete, stringResource(R.string.ess_mix_delete), tint = p.secondaryText) }
            }
        }
    }
    if (saveOpen) SaveMixDialog(layers, preferences, onSaved = { saved ->
        viewModel.loadMix(saved, play = viewModel.playWhenReady.value)
        saveOpen = false
        openSavedMix(saved.id)
    }) { saveOpen = false }
}

@Composable
private fun SaveMixDialog(layers: List<MixLayer>, preferences: EssencePreferences, onSaved: (SavedMix) -> Unit, close: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.ess_mix_save)) }, text = {
        Column {
            OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.ess_mix_name)) }, placeholder = { Text(stringResource(R.string.ess_mix_name_hint)) }, singleLine = true)
            Text(stringResource(R.string.ess_mix_name_help), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }, confirmButton = {
        TextButton(onClick = { if (preferences.saveMix(name, layers)) {
            preferences.state.value.savedMixes.find { it.name.equals(name.trim().take(40), ignoreCase = true) }?.let(onSaved)
        } }, enabled = name.isNotBlank() && layers.isNotEmpty()) { Text(stringResource(R.string.ess_save)) }
    }, dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.ess_cancel)) } })
}

@Composable
fun TimerControls(viewModel: SoundViewModel) {
    val remaining by viewModel.timerRemaining.collectAsState()
    val current by viewModel.selectedSound.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { open = true }, enabled = current != null, modifier = Modifier.weight(1f)) {
            Icon(EssenceIcons.Clock, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (remaining > 0) stringResource(R.string.ess_timer_remaining, timerLabel(remaining)) else stringResource(R.string.ess_timer))
        }
        if (remaining > 0) IconButton(onClick = viewModel::cancelTimer) { Icon(EssenceIcons.Close, stringResource(R.string.ess_timer_cancel)) }
    }
    if (open) TimerDialog(viewModel) { open = false }
}

@Composable
private fun TimerDialog(viewModel: SoundViewModel, close: () -> Unit) {
    var minutes by rememberSaveable { mutableStateOf("30") }
    var fade by rememberSaveable { mutableIntStateOf(10) }
    val value = minutes.toIntOrNull()
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.ess_timer)) }, text = {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(5, 15, 30, 60).forEach { duration ->
                    TextButton(onClick = { minutes = duration.toString() }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text(stringResource(R.string.ess_timer_preset, duration)) }
                }
            }
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.ess_timer_minutes)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = value == null || value !in 1..1440)
            Text(stringResource(R.string.ess_timer_range), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ess_timer_fade, fade))
            Slider(fade.toFloat(), { fade = it.toInt() }, valueRange = 0f..60f, steps = 11)
            Text(stringResource(R.string.ess_timer_fade_help), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(onClick = { value?.let { viewModel.setTimer(it, fade) }; close() }, enabled = value != null && value in 1..1440) { Text(stringResource(R.string.ess_timer_start)) }
    }, dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.ess_cancel)) } })
}

@Composable
fun WidgetPicker(close: () -> Unit) {
    val context = LocalContext.current
    var unsupported by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = close, title = { Text(stringResource(R.string.ess_widgets)) }, text = {
        Column {
            Text(stringResource(R.string.ess_widget_help))
            listOf(false to R.string.ess_widget_large, true to R.string.ess_widget_compact).forEach { (compact, label) ->
                TextButton(onClick = { if (DailyWordWidgets.requestPin(context, compact)) close() else unsupported = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
            }
            if (unsupported) Text(stringResource(R.string.ess_widget_unsupported), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.ess_close)) } })
}
