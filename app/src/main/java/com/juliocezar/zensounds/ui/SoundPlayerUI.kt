import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.juliocezar.zensounds.ui.BannerAd
import com.juliocezar.zensounds.ui.viewmodel.Sound
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel

@Composable
fun SoundPlayerScreen(
    navController: NavController,
    soundViewModel: SoundViewModel,
    initialSoundName: String
) {
    val allSounds by soundViewModel.soundCategories.collectAsState()
    val availableSounds = allSounds.flatMap { it.sounds }.distinctBy { it.name }

    val currentSound by soundViewModel.selectedSound.collectAsState()
    val isPlaying by soundViewModel.isPlaying.collectAsState()
    val playbackProgress by soundViewModel.playbackProgress.collectAsState()
    val volume by soundViewModel.volume.collectAsState()

    val playingSoundObject = availableSounds.find { it.name == currentSound }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }


    LaunchedEffect(initialSoundName) {
        if (!hasInitialized || currentSound != initialSoundName) {
            Log.d("SoundPlayerScreen", "LaunchedEffect triggered: initialSoundName=$initialSoundName, currentSound=$currentSound, isPlaying=$isPlaying")
            if (currentSound != initialSoundName) {
                soundViewModel.onSoundClicked(initialSoundName)
            } else if (!isPlaying) {
                soundViewModel.onPlayPauseClicked()
            }
            hasInitialized = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C1C2C), Color(0xFF1A2B3D))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Player",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showVolumeSlider = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFFB0C4D4)
                        )
                    }
                    DropdownMenu(
                        expanded = showVolumeSlider,
                        onDismissRequest = { showVolumeSlider = false },
                        modifier = Modifier.background(Color(0xFF2C3E50))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .width(200.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFFB0C4D4), modifier = Modifier.size(24.dp))
                            Slider(
                                value = volume,
                                onValueChange = { soundViewModel.onVolumeChanged(it) },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF89CFF0),
                                    activeTrackColor = Color(0xFF89CFF0).copy(alpha = 0.7f),
                                    inactiveTrackColor = Color(0xFF34495E)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))
            BannerAd(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = playingSoundObject?.name ?: "Loading...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E2F46)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    playingSoundObject != null -> {
                        Image(
                            painter = painterResource(id = playingSoundObject.backgroundResId),
                            contentDescription = "Capa do som",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    else -> {
                        Text(
                            text = "Carregando imagem...",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Outros Sons Zen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(availableSounds.filter { it.name != currentSound }) { sound ->
                        OtherSoundTile(
                            sound = sound,
                            onClick = { soundViewModel.onSoundClicked(sound.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF89CFF0),
                    trackColor = Color(0xFF24364D),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { soundViewModel.onPreviousClicked() }) {
                            Icon(Icons.Filled.SkipPrevious,
                                contentDescription = "Anterior",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFB0C4D4))
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF89CFF0))
                                .clickable { soundViewModel.onPlayPauseClicked() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = Color.Black, modifier = Modifier.size(42.dp))
                        }
                        IconButton(onClick = { soundViewModel.onNextClicked() }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Próximo", modifier = Modifier.size(48.dp), tint = Color(0xFFB0C4D4))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotatedBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .offset(x = (-1).dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = 90f
                    translationX = -this.size.height / 2f
                    translationY = this.size.width / 2f
                }
        ) {
            content()
        }
    }
}

@Composable
fun OtherSoundTile(sound: Sound, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2B3D))
            .border(1.dp, Color(0xFF24364D), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = sound.backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
    }
}