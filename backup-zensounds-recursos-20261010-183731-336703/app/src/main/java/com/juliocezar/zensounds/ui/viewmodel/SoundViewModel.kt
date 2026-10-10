package com.juliocezar.zensounds.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.ui.LocalizedLabels
import com.juliocezar.zensounds.data.AppDatabase
import com.juliocezar.zensounds.data.Sound as SoundEntity
import com.juliocezar.zensounds.data.SoundDao
import com.juliocezar.zensounds.services.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controllerReady = CompletableDeferred<MediaController>()
    private var playbackCommandVersion = 0L

    private companion object {
        const val TAG = "ZenSoundsPlayback"
    }

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> =
        _playbackProgress.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedSound = MutableStateFlow<String?>(null)
    val selectedSound: StateFlow<String?> =
        _selectedSound.asStateFlow()

    private val _soundCategories =
        MutableStateFlow<List<SoundCategory>>(emptyList())

    val soundCategories: StateFlow<List<SoundCategory>> =
        _soundCategories.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(context)
        soundDao = database.soundDao()
        initializeSoundsInDb()

        viewModelScope.launch {
            soundDao.getAllSounds().collect { soundEntities ->
                _soundCategories.value = mapToUiCategories(soundEntities)
            }
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(
            context,
            sessionToken
        ).buildAsync()

        controllerFuture.addListener({
            try {
                val player = controllerFuture.get()

                if (!controllerReady.isCancelled) {
                    mediaController = player
                    setupPlayerListener()

                    _isPlaying.value = player.isPlaying
                    _selectedSound.value = player.currentMediaItem
                        ?.mediaId
                        ?.takeIf { it.isNotEmpty() }

                    _volume.value = player.volume

                    val duration = player.duration

                    if (duration > 0 && duration != C.TIME_UNSET) {
                        _playbackProgress.value =
                            player.currentPosition.toFloat() /
                                    duration.toFloat()
                    }

                    controllerReady.complete(player)

                    Log.d(
                        TAG,
                        "MediaController conectado; " +
                                "comandos=${player.availableCommands}"
                    )
                }
            } catch (error: Exception) {
                controllerReady.completeExceptionally(error)
                Log.e(
                    TAG,
                    "Falha ao conectar ao PlaybackService",
                    error
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun awaitPlayer(): MediaController? {
        return try {
            controllerReady.await()
                .takeIf { it.isConnected }
                .also {
                    if (it == null) {
                        Log.e(TAG, "MediaController desconectado")
                    }
                }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                TAG,
                "Controller indisponível para reproduzir",
                error
            )
            null
        }
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    TAG,
                    "Erro de reprodução: ${error.errorCodeName}",
                    error
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val player = mediaController

                Log.d(
                    TAG,
                    "Estado=$playbackState; " +
                            "playWhenReady=${player?.playWhenReady}; " +
                            "suppression=${player?.playbackSuppressionReason}"
                )
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                _isPlaying.value = isPlayingValue
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                _selectedSound.value = mediaItem
                    ?.mediaId
                    ?.takeIf { it.isNotEmpty() }
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
                        _playbackProgress.value =
                            position.toFloat() / duration.toFloat()
                    }
                }

                delay(200)
            }
        }
    }

    private fun mapToUiCategories(
        entities: List<SoundEntity>
    ): List<SoundCategory> {
        val uiSounds = entities.map { entity ->
            Sound(
                name = entity.name,
                backgroundResId = getBackgroundForSound(entity.name)
            )
        }

        return listOf(
            SoundCategory(
                "Nature Sounds",
                uiSounds.filter {
                    it.name in listOf(
                        "Rain",
                        "Storm",
                        "Wind",
                        "Forest",
                        "Stream"
                    )
                }
            ),
            SoundCategory(
                "Object Sounds",
                uiSounds.filter {
                    it.name in listOf("Fireplace", "TV Static")
                }
            ),
            SoundCategory(
                "Vehicle Sounds",
                uiSounds.filter { it.name == "Car Engine" }
            )
        ).filter { it.sounds.isNotEmpty() }
    }

    private fun getBackgroundForSound(soundName: String): Int {
        return when (soundName) {
            "Rain" -> R.drawable.rain_card
            "Storm" -> R.drawable.rainthunder_card
            "Wind" -> R.drawable.wind_card
            "Forest" -> R.drawable.forest_card
            "Stream" -> R.drawable.stream_card
            "Fireplace" -> R.drawable.fireplace_card
            "TV Static" -> R.drawable.tv_card
            "Car Engine" -> R.drawable.engine_card
            else -> R.drawable.placeholder_card
        }
    }

    private fun getSoundUri(soundName: String): Uri? {
        val resourceId = when (soundName) {
            "Rain" -> R.raw.rain_sound
            "Storm" -> R.raw.thunder_sound
            "Wind" -> R.raw.wind_sound
            "Forest" -> R.raw.forest_sound
            "Stream" -> R.raw.stream_sound
            "Fireplace" -> R.raw.fire_sound
            "TV Static" -> R.raw.tv_sound
            "Car Engine" -> R.raw.car_sound
            else -> return null
        }

        return Uri.Builder()
            .scheme("android.resource")
            .authority(context.packageName)
            .appendPath("raw")
            .appendPath(
                context.resources.getResourceEntryName(resourceId)
            )
            .build()
    }

    private fun initializeSoundsInDb() {
        viewModelScope.launch {
            if (soundDao.getSoundCount() == 0) {
                val defaultSounds = listOf(
                    SoundEntity(
                        name = "Rain",
                        filePath = requireNotNull(
                            getSoundUri("Rain")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "Storm",
                        filePath = requireNotNull(
                            getSoundUri("Storm")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "Wind",
                        filePath = requireNotNull(
                            getSoundUri("Wind")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "Forest",
                        filePath = requireNotNull(
                            getSoundUri("Forest")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "Stream",
                        filePath = requireNotNull(
                            getSoundUri("Stream")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "Fireplace",
                        filePath = requireNotNull(
                            getSoundUri("Fireplace")
                        ).toString(),
                        volume = 1.0f
                    ),
                    SoundEntity(
                        name = "TV Static",
                        filePath = requireNotNull(
                            getSoundUri("TV Static")
                        ).toString(),
                        volume = 0.1f
                    ),
                    SoundEntity(
                        name = "Car Engine",
                        filePath = requireNotNull(
                            getSoundUri("Car Engine")
                        ).toString(),
                        volume = 1.0f
                    )
                )

                soundDao.insertSounds(defaultSounds)
            }
        }
    }

    fun onSoundClicked(soundName: String) {
        val commandVersion = ++playbackCommandVersion

        viewModelScope.launch {
            val soundEntity = soundDao.getAllSounds()
                .firstOrNull()
                ?.find { it.name == soundName }
                ?: return@launch

            val player = awaitPlayer() ?: return@launch

            if (commandVersion != playbackCommandVersion) {
                return@launch
            }

            val uri = getSoundUri(soundEntity.name) ?: run {
                Log.e(
                    TAG,
                    "Recurso de áudio desconhecido: ${soundEntity.name}"
                )
                return@launch
            }

            if (
                player.currentMediaItem?.mediaId == soundName &&
                player.isPlaying
            ) {
                player.pause()
            } else {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(soundEntity.name)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(
                                LocalizedLabels.soundName(
                                    context,
                                    soundEntity.name
                                )
                            )
                            .setArtworkUri(
                                Uri.parse(
                                    "android.resource://" +
                                            "${context.packageName}/" +
                                            getBackgroundForSound(
                                                soundEntity.name
                                            )
                                )
                            )
                            .build()
                    )
                    .build()

                if (
                    !player.isCommandAvailable(
                        Player.COMMAND_SET_MEDIA_ITEM
                    ) ||
                    !player.isCommandAvailable(
                        Player.COMMAND_PREPARE
                    ) ||
                    !player.isCommandAvailable(
                        Player.COMMAND_PLAY_PAUSE
                    )
                ) {
                    Log.e(
                        TAG,
                        "Sessão não autorizou os comandos de reprodução: " +
                                player.availableCommands
                    )
                    return@launch
                }

                Log.d(
                    TAG,
                    "Iniciando ${soundEntity.name}; uri=$uri; " +
                            "caminhoSalvo=${soundEntity.filePath}"
                )

                _playbackProgress.value = 0f
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    fun onPlayPauseClicked() {
        val commandVersion = playbackCommandVersion

        viewModelScope.launch {
            val player = awaitPlayer() ?: return@launch

            if (commandVersion != playbackCommandVersion) {
                return@launch
            }

            if (
                !player.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE
                )
            ) {
                Log.e(TAG, "Sessão não autorizou play/pause")
                return@launch
            }

            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.mediaItemCount == 0) {
                    Log.w(TAG, "Play solicitado sem áudio selecionado")
                    return@launch
                }

                if (
                    player.playbackState == Player.STATE_IDLE &&
                    player.isCommandAvailable(Player.COMMAND_PREPARE)
                ) {
                    player.prepare()
                }

                if (
                    player.playbackState == Player.STATE_ENDED &&
                    player.isCommandAvailable(
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
                    )
                ) {
                    player.seekTo(0)
                }

                player.play()
            }
        }
    }

    fun onStopClicked() {
        val commandVersion = ++playbackCommandVersion

        viewModelScope.launch {
            val player = awaitPlayer() ?: return@launch

            if (commandVersion != playbackCommandVersion) {
                return@launch
            }

            if (
                !player.isCommandAvailable(Player.COMMAND_STOP) ||
                !player.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS
                )
            ) {
                Log.e(
                    TAG,
                    "Sessão não autorizou parar e limpar a reprodução"
                )
                return@launch
            }

            player.stop()
            player.clearMediaItems()

            _isPlaying.value = false
            _playbackProgress.value = 0f
            _selectedSound.value = null
        }
    }

    fun onPreviousClicked() {
        val commandVersion = playbackCommandVersion

        viewModelScope.launch {
            val allSounds = soundDao.getAllSounds()
                .firstOrNull()
                ?: return@launch

            if (commandVersion != playbackCommandVersion) {
                return@launch
            }

            if (allSounds.isEmpty()) return@launch

            val currentIndex = allSounds.indexOfFirst {
                it.name == _selectedSound.value
            }

            val previousIndex = if (currentIndex <= 0) {
                allSounds.size - 1
            } else {
                currentIndex - 1
            }

            onSoundClicked(allSounds[previousIndex].name)
        }
    }

    fun onNextClicked() {
        val commandVersion = playbackCommandVersion

        viewModelScope.launch {
            val allSounds = soundDao.getAllSounds()
                .firstOrNull()
                ?: return@launch

            if (commandVersion != playbackCommandVersion) {
                return@launch
            }

            if (allSounds.isEmpty()) return@launch

            val currentIndex = allSounds.indexOfFirst {
                it.name == _selectedSound.value
            }

            val nextIndex = if (
                currentIndex == -1 ||
                currentIndex == allSounds.size - 1
            ) {
                0
            } else {
                currentIndex + 1
            }

            onSoundClicked(allSounds[nextIndex].name)
        }
    }

    fun onVolumeChanged(newVolume: Float) {
        _volume.value = newVolume
        mediaController?.volume = newVolume
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        controllerReady.cancel()
        MediaController.releaseFuture(controllerFuture)
        mediaController = null
    }
}