package com.juliocezar.zensounds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModelFactory

@Composable
fun MainScreen(soundViewModel: SoundViewModel = viewModel(factory = SoundViewModelFactory(LocalContext.current))) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, soundViewModel.isPlaying, { soundViewModel.onPlayPauseClicked() })
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "zenSounds") {
            composable("zenSounds") { ZenSoundsApp(soundViewModel, innerPadding) }
            composable("bible") { BibleScreen(soundViewModel, navController, innerPadding) }
        }
    }
}

@Composable
fun ZenSoundsApp(soundViewModel: SoundViewModel, innerPadding: PaddingValues) {
    val sounds by soundViewModel.sounds.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1C2C))
            .padding(
                top = 16.dp,
                bottom = innerPadding.calculateBottomPadding() // Usa o padding da Scaffold pra ajustar
            )
            .systemBarsPadding() // Adiciona padding pra barras do sistema
    ) {
        // Título
        Text(
            text = "ZenSounds",
            fontSize = 42.sp,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
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
    }
}

@Composable
fun BottomNavItem(iconId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) Color(0xFF24364D) else Color(0xFF1A2B3D),
                shape = CircleShape
            )
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = Color(0xFF1A2B3D), // Fundo padrão, sem seleção destacada
                shape = CircleShape
            )
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
fun SoundCard(soundName: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF24364D), RoundedCornerShape(16.dp))
    ) {
        // Fundo com imagem para "Rain" ou cor para os outros
        when (soundName) {
            "Rain" -> {
                Image(
                    painter = painterResource(id = R.drawable.rain_card),
                    contentDescription = "Rain background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Rain & Thunder" -> {
                Image(
                    painter = painterResource(id = R.drawable.rainthunder_card),
                    contentDescription = "Rain & Thunder background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Wind" -> {
                Image(
                    painter = painterResource(id = R.drawable.wind_card),
                    contentDescription = "Wind background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Forest" -> {
                Image(
                    painter = painterResource(id = R.drawable.forest_card),
                    contentDescription = "Forest background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Stream" -> {
                Image(
                    painter = painterResource(id = R.drawable.stream_card),
                    contentDescription = "Stream background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Fireplace" -> {
                Image(
                    painter = painterResource(id = R.drawable.fireplace_card),
                    contentDescription = "Fireplace background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "TV Static" -> {
                Image(
                    painter = painterResource(id = R.drawable.tv_card),
                    contentDescription = "TV background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            "Car Engine" -> {
                Image(
                    painter = painterResource(id = R.drawable.engine_card),
                    contentDescription = "Car background",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (isSelected) Color(0xFF24364D) else Color(0xFF1A2B3D),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        }

        // Conteúdo do cartão (ícone e texto)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = icon,
                fontSize = 32.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = soundName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive,
                color = Color.White
            )
        }
    }
}