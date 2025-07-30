package com.juliocezar.zensounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juliocezar.zensounds.ui.theme.ZenSoundsTheme
import com.juliocezar.zensounds.viewmodel.SoundViewModel
import com.juliocezar.zensounds.viewmodel.SoundViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenSoundsTheme { // Aplica o tema personalizado
                ZenSoundsApp()
            }
        }
    }
}

@Composable
fun ZenSoundsApp(soundViewModel: SoundViewModel = viewModel(factory = SoundViewModelFactory(LocalContext.current))) {
    val sounds by soundViewModel.sounds.collectAsState(initial = emptyList())
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            //.background(Color(0xFF1A3C34)) // Cor de fundo verde escura
            .background(Color(0xFF0C1C2C))
            .padding(
                top = 16.dp,
                bottom = navigationBarPadding.calculateBottomPadding())
    ) {
        // Título
        Text(
            text = "ZenSounds",
            fontSize = 24.sp,
            color = Color(0xFFB0C4D4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(42.dp),
            textAlign = TextAlign.Center
        )

        // Grade de sons
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sounds.size) { index ->
                val sound = sounds[index]
                SoundCard(
                    soundName = sound.name,
                    icon = sound.icon,
                    isSelected = soundViewModel.selectedSound == sound.name,
                    onClick = {
                        soundViewModel.onSoundClicked(sound.name)
                    }
                )
            }
        }

        // Controles de reprodução
        PlaybackControls(
            isPlaying = soundViewModel.isPlaying,
            onPlayPauseClick = { soundViewModel.onPlayPauseClicked() },
            onPreviousClick = { soundViewModel.onPreviousClicked() },
            onNextClick = { soundViewModel.onNextClicked() }
        )
    }
}

@Composable
fun SoundCard(soundName: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            //containerColor = if (isSelected) Color(0xFFE0E0E0) else Color.White
            containerColor = if (isSelected) Color(0xFF24364D) else Color(0xFF1A2B3D)

        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 32.sp,
                //color = Color(0xFF1A3C34)
                color = Color(0xFF0C1C2C)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = soundName,
                fontSize = 16.sp,
                //color = Color(0xFF1A3C34)
                color = Color(0xFF0C1C2C)
            )
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            //.background(Color(0xFF1A3C34))
            .background(Color(0xFF0C1C2C))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onPreviousClick)
                .background(Color(0xFF1A2B3D), shape = CircleShape)
                .size(48.dp)
                .padding(0.dp), // Remove padding interno pra evitar sobreposição
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⏪",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.background(Color.Transparent) // Garante que o texto não herde fundo
            )
        }

        // Botão Play/Pause
        Box(
            modifier = Modifier
                .clickable(onClick = onPlayPauseClick)
                .background(Color(0xFF1A2B3D), shape = CircleShape)
                .size(56.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) "⏸️" else "▶️",
                fontSize = 32.sp,
                color = Color.White,
                modifier = Modifier.background(Color.Transparent)
            )
        }

        // Botão Próximo
        Box(
            modifier = Modifier
                .clickable(onClick = onNextClick)
                .background(Color(0xFF1A2B3D), shape = CircleShape)
                .size(48.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⏩",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.background(Color.Transparent)
            )
        }
    }
}