package com.juliocezar.zensounds.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.juliocezar.zensounds.AppDatabase
import com.juliocezar.zensounds.Sound
import com.juliocezar.zensounds.SoundDao
import com.juliocezar.zensounds.AudioPlayerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SoundViewModel(private val context: Context) : ViewModel() {
    private val soundDao: SoundDao
    private val audioPlayerManager: AudioPlayerManager
    private val _isPlaying = mutableStateOf(false)
    val isPlaying: Boolean get() = _isPlaying.value

    private val _selectedSound = mutableStateOf("")
    val selectedSound: String get() = _selectedSound.value

    // Lista de sons do banco de dados
    val sounds: Flow<List<Sound>>

    init {
        val database = AppDatabase.getDatabase(context)
        soundDao = database.soundDao()
        sounds = soundDao.getAllSounds()
        audioPlayerManager = AudioPlayerManager(context)

        // Inicializar o banco de dados com sons padrão, se necessário
        initializeSounds()
    }

    private fun initializeSounds() {
        viewModelScope.launch {
            val currentSounds = soundDao.getAllSounds().firstOrNull() ?: emptyList()
            //soundDao.deleteAllSounds()
            if (currentSounds.isEmpty()) {
                val defaultSounds = listOf(
                    Sound(name = "Rain", icon = "☔", filePath = "android.resource://${context.packageName}/raw/rain_sound"),
                    Sound(name = "Rain & Thunder", icon = "💧⚡", filePath = "android.resource://${context.packageName}/raw/thunder_sound"),
                    Sound(name = "Wind", icon = "💨", filePath = "android.resource://${context.packageName}/raw/wind_sound"),
                    Sound(name = "Forest", icon = "🌲", filePath = "android.resource://${context.packageName}/raw/forest_sound"),
                    Sound(name = "Stream", icon = "🌊", filePath = "android.resource://${context.packageName}/raw/stream_sound"),
                    Sound(name = "Fireplace", icon = "🔥🪵", filePath = "android.resource://${context.packageName}/raw/fire_sound"),
                    Sound(name = "TV Static", icon = "📺", filePath = "android.resource://${context.packageName}/raw/tv_sound"),
                    Sound(name = "Car Engine", icon = "🚗", filePath = "android.resource://${context.packageName}/raw/car_sound"))

                soundDao.insertSounds(defaultSounds)
            }
        }
    }

    fun onSoundClicked(soundName: String) {
        viewModelScope.launch {
            val sound = sounds.firstOrNull()?.find { it.name == soundName } ?: return@launch
            if (_selectedSound.value == soundName && _isPlaying.value) {
                _isPlaying.value = false
                audioPlayerManager.pause()
            } else {
                _selectedSound.value = soundName
                _isPlaying.value = true
                audioPlayerManager.playSound(sound.filePath, overlapDurationMs = 5000)
            }
        }
    }

    fun onPlayPauseClicked() {
        if (_isPlaying.value) {
            _isPlaying.value = false
            audioPlayerManager.pause()
        } else {
            _isPlaying.value = true
            audioPlayerManager.resume()
        }
    }

    fun onPreviousClicked() {
        viewModelScope.launch {
            val currentSounds = sounds.firstOrNull() ?: return@launch
            val currentIndex = currentSounds.indexOfFirst { it.name == _selectedSound.value }
            if (currentIndex > 0) {
                val previousSound = currentSounds[currentIndex - 1]
                _selectedSound.value = previousSound.name
                _isPlaying.value = true
                audioPlayerManager.playSound(previousSound.filePath, overlapDurationMs = 5000)
            }
        }
    }

    fun onNextClicked() {
        viewModelScope.launch {
            val currentSounds = sounds.firstOrNull() ?: return@launch
            val currentIndex = currentSounds.indexOfFirst { it.name == _selectedSound.value }
            if (currentIndex < currentSounds.size - 1) {
                val nextSound = currentSounds[currentIndex + 1]
                _selectedSound.value = nextSound.name
                _isPlaying.value = true
                audioPlayerManager.playSound(nextSound.filePath, overlapDurationMs = 5000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}

class SoundViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SoundViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}