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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.EssencePreferences
import com.juliocezar.zensounds.ui.theme.LocalEssencePalette
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel

@Composable
fun SoundPlayerScreen(
    navController: NavController,
    soundViewModel: SoundViewModel,
    initialSoundName: String,
    preferences: EssencePreferences
) {
    val categories by soundViewModel.soundCategories.collectAsState()
    val current by soundViewModel.selectedSound.collectAsState()
    val playing by soundViewModel.isPlaying.collectAsState()
    val progress by soundViewModel.playbackProgress.collectAsState()
    val volume by soundViewModel.volume.collectAsState()
    val prefs by preferences.state.collectAsState()

    val sounds = categories
        .flatMap { it.sounds }
        .distinctBy { it.name }

    val sound = sounds.find {
        it.name == (current ?: initialSoundName)
    } ?: sounds.find {
        it.name == initialSoundName
    }

    val palette = LocalEssencePalette.current

    LaunchedEffect(initialSoundName) {
        if (soundViewModel.selectedSound.value != initialSoundName) {
            soundViewModel.onSoundClicked(initialSoundName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = EssenceIcons.Back,
                    contentDescription = stringResource(R.string.zen_back),
                    tint = palette.text
                )
            }

            Text(
                text = stringResource(R.string.ess_ambient_sound),
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                modifier = Modifier.weight(1f)
            )

            sound?.let { selected ->
                val favorite = selected.name in prefs.favoriteSounds

                IconButton(
                    onClick = {
                        preferences.toggleSound(selected.name)
                    }
                ) {
                    Icon(
                        imageVector = if (favorite) {
                            EssenceIcons.Check
                        } else {
                            EssenceIcons.Heart
                        },
                        contentDescription = stringResource(
                            if (favorite) {
                                R.string.ess_remove_favorite
                            } else {
                                R.string.ess_add_favorite
                            }
                        ),
                        tint = palette.accent
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (sound != null) {
            Image(
                painter = painterResource(sound.backgroundResId),
                contentDescription = stringResource(
                    R.string.zen_sound_artwork
                ),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = localizedSoundName(sound.name),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.text
            )

            Text(
                text = stringResource(R.string.ess_continuous_sound),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(26.dp))

        ThinProgress(
            fraction = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = soundViewModel::onPreviousClicked
            ) {
                Icon(
                    imageVector = EssenceIcons.Previous,
                    contentDescription = stringResource(
                        R.string.zen_previous
                    ),
                    tint = palette.text,
                    modifier = Modifier.size(28.dp)
                )
            }

            FilledIconButton(
                onClick = soundViewModel::onPlayPauseClicked,
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent
                )
            ) {
                Icon(
                    imageVector = if (playing) {
                        EssenceIcons.Pause
                    } else {
                        EssenceIcons.Play
                    },
                    contentDescription = stringResource(
                        if (playing) {
                            R.string.zen_pause
                        } else {
                            R.string.zen_play
                        }
                    ),
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = soundViewModel::onNextClicked
            ) {
                Icon(
                    imageVector = EssenceIcons.Next,
                    contentDescription = stringResource(R.string.zen_next),
                    tint = palette.text,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = EssenceIcons.Volume,
                contentDescription = stringResource(R.string.zen_volume),
                tint = palette.secondaryText
            )

            Spacer(Modifier.width(12.dp))

            Slider(
                value = volume,
                onValueChange = soundViewModel::onVolumeChanged,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.zen_other_sounds),
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = sounds.filter { it.name != current },
                key = { it.name }
            ) { other ->
                Column(
                    modifier = Modifier.width(85.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(other.backgroundResId),
                        contentDescription = localizedSoundName(other.name),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable {
                                soundViewModel.onSoundClicked(other.name)
                            },
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        text = localizedSoundName(other.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}