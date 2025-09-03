package com.juliocezar.zensounds.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.AppDatabase
import com.juliocezar.zensounds.data.Sound as SoundEntity
import com.juliocezar.zensounds.data.SoundDao
import com.juliocezar.zensounds.services.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class Sound(
    val name: String,
    val backgroundResId: Int
)

data class SoundCategory(
    val title: String,
    val sounds: List<Sound>
)

class SoundViewModel(private val context: Context) : ViewModel() {
    private val soundDao: SoundDao
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedSound = MutableStateFlow<String?>(null)
    val selectedSound: StateFlow<String?> = _selectedSound.asStateFlow()

    private val _soundCategories = MutableStateFlow<List<SoundCategory>>(emptyList())
    val soundCategories: StateFlow<List<SoundCategory>> = _soundCategories.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(context)
        soundDao = database.soundDao()
        initializeSoundsInDb()

        viewModelScope.launch {
            soundDao.getAllSounds().collect { soundEntities ->
                _soundCategories.value = mapToUiCategories(soundEntities)
            }
        }

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            setupPlayerListener()
            // Sincronizar estado do PlaybackService
            viewModelScope.launch {
                mediaController?.let { player ->
                    _isPlaying.value = player.isPlaying
                    _selectedSound.value = player.currentMediaItem?.mediaMetadata?.title?.toString()
                    if (player.isPlaying) {
                        val duration = player.duration
                        val position = player.currentPosition
                        if (duration > 0 && duration != C.TIME_UNSET) {
                            _playbackProgress.value = position.toFloat() / duration.toFloat()
                        }
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                _isPlaying.value = isPlayingValue
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _selectedSound.value = mediaItem?.mediaMetadata?.title?.toString()
            }
        })

        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val player = mediaController
                if (player?.isPlaying == true) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (duration > 0 && duration != C.TIME_UNSET) {
                        _playbackProgress.value = position.toFloat() / duration.toFloat()
                    }
                }
                delay(200)
            }
        }
    }

    // --- SUA LÓGICA DE DADOS ORIGINAL (INTACTA) ---
    private fun mapToUiCategories(entities: List<SoundEntity>): List<SoundCategory> {
        val uiSounds = entities.map { entity ->
            Sound(
                name = entity.name,
                backgroundResId = getBackgroundForSound(entity.name)
            )
        }
        return listOf(
            SoundCategory("Nature Sounds", uiSounds.filter { it.name in listOf("Rain", "Storm", "Wind", "Forest", "Stream") }),
            SoundCategory("Object Sounds", uiSounds.filter { it.name in listOf("Fireplace", "TV Static") }),
            SoundCategory("Vehicle Sounds", uiSounds.filter { it.name == "Car Engine" })
        ).filter { it.sounds.isNotEmpty() }
    }

    private fun getBackgroundForSound(soundName: String): Int = when (soundName) {
        "Rain" -> R.drawable.rain_card; "Storm" -> R.drawable.rainthunder_card; "Wind" -> R.drawable.wind_card
        "Forest" -> R.drawable.forest_card; "Stream" -> R.drawable.stream_card; "Fireplace" -> R.drawable.fireplace_card
        "TV Static" -> R.drawable.tv_card; "Car Engine" -> R.drawable.engine_card
        else -> R.drawable.placeholder_card
    }

    private fun initializeSoundsInDb() {
        viewModelScope.launch {
            if (soundDao.getSoundCount() == 0) {
                val defaultSounds = listOf(
                    SoundEntity(name = "Rain", filePath = "android.resource://${context.packageName}/${R.raw.rain_sound}", volume = 1.0f),
                    SoundEntity(name = "Storm", filePath = "android.resource://${context.packageName}/${R.raw.thunder_sound}", volume = 1.0f),
                    SoundEntity(name = "Wind", filePath = "android.resource://${context.packageName}/${R.raw.wind_sound}", volume = 1.0f),
                    SoundEntity(name = "Forest", filePath = "android.resource://${context.packageName}/${R.raw.forest_sound}", volume = 1.0f),
                    SoundEntity(name = "Stream", filePath = "android.resource://${context.packageName}/${R.raw.stream_sound}", volume = 1.0f),
                    SoundEntity(name = "Fireplace", filePath = "android.resource://${context.packageName}/${R.raw.fire_sound}", volume = 1.0f),
                    SoundEntity(name = "TV Static", filePath = "android.resource://${context.packageName}/${R.raw.tv_sound}", volume = 0.1f),
                    SoundEntity(name = "Car Engine", filePath = "android.resource://${context.packageName}/${R.raw.car_sound}", volume = 1.0f)
                )
                soundDao.insertSounds(defaultSounds)
            }
        }
    }

    // --- FUNÇÕES DE CONTROLE REFATORADAS PARA USAR O MEDIACONTROLLER ---

    fun onSoundClicked(soundName: String) {
        viewModelScope.launch {
            val soundEntity = soundDao.getAllSounds().firstOrNull()?.find { it.name == soundName } ?: return@launch
            val player = mediaController ?: return@launch

            if (player.currentMediaItem?.mediaMetadata?.title == soundName && player.isPlaying) {
                player.pause()
            } else {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(soundEntity.filePath))
                    .setMediaId(soundEntity.name)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(soundEntity.name)
                            .setArtworkUri(Uri.parse("android.resource://${context.packageName}/${getBackgroundForSound(soundEntity.name)}"))
                            .build()
                    )
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    fun onPlayPauseClicked() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun onPreviousClicked() {
        viewModelScope.launch {
            val allSounds = soundDao.getAllSounds().firstOrNull() ?: return@launch
            if (allSounds.isEmpty()) return@launch

            val currentIndex = allSounds.indexOfFirst { it.name == _selectedSound.value }
            val previousIndex = if (currentIndex <= 0) allSounds.size - 1 else currentIndex - 1
            val previousSound = allSounds[previousIndex]

            // Reutiliza a lógica de onSoundClicked para tocar o som anterior
            onSoundClicked(previousSound.name)
        }
    }

    fun onNextClicked() {
        viewModelScope.launch {
            val allSounds = soundDao.getAllSounds().firstOrNull() ?: return@launch
            if (allSounds.isEmpty()) return@launch

            val currentIndex = allSounds.indexOfFirst { it.name == _selectedSound.value }
            val nextIndex = if (currentIndex == -1 || currentIndex == allSounds.size - 1) 0 else currentIndex + 1
            val nextSound = allSounds[nextIndex]

            // Reutiliza a lógica de onSoundClicked para tocar o próximo som
            onSoundClicked(nextSound.name)
        }
    }

    fun onVolumeChanged(newVolume: Float) {
        _volume.value = newVolume
        mediaController?.volume = newVolume
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}