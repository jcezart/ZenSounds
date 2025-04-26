package com.juliocezar.zensounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.juliocezar.zensounds.viewmodel.SoundViewModel
import com.juliocezar.zensounds.viewmodel.SoundViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenSoundsApp()
        }
    }
}

@Composable
fun ZenSoundsApp(soundViewModel: SoundViewModel = viewModel(factory = SoundViewModelFactory(LocalContext.current))) {
    val sounds by soundViewModel.sounds.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A3C34)) // Cor de fundo verde escura
            .padding(top = 16.dp)
    ) {
        // Título
        Text(
            text = "ZenSounds",
            fontSize = 24.sp,
            color = Color.White,
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
            containerColor = if (isSelected) Color(0xFFE0E0E0) else Color.White
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
                color = Color(0xFF1A3C34)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = soundName,
                fontSize = 16.sp,
                color = Color(0xFF1A3C34)
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
            .background(Color(0xFF1A3C34))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Text(
                text = "⏪",
                fontSize = 24.sp,
                color = Color.White
            )
        }
        IconButton(onClick = onPlayPauseClick) {
            Text(
                text = if (isPlaying) "⏸️" else "▶️",
                fontSize = 32.sp,
                color = Color.White
            )
        }
        IconButton(onClick = onNextClick) {
            Text(
                text = "⏩",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}