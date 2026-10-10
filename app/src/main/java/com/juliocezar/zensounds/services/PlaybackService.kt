package com.juliocezar.zensounds.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import androidx.media3.session.SessionError
import com.google.common.util.concurrent.ListenableFuture
import com.juliocezar.zensounds.MainActivity
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.*
import com.juliocezar.zensounds.ui.LocalizedLabels

@SuppressLint("UnsafeOptInUsageError")
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val secondaryPlayers = linkedMapOf<String, ExoPlayer>()
    private var layers = emptyList<MixLayer>()
    private var mixName: String? = null
    private var savedMixId: String? = null
    private var masterVolume = 1f
    private var timerDeadline = 0L
    private var fadeSeconds = 10
    private var applyingMix = false
    private var playbackError = false
    private var lastPublished = ""
    private val handler = Handler(Looper.getMainLooper())
    private val timerTick = object : Runnable {
        override fun run() {
            if (timerDeadline == 0L) return
            if (TimerRules.remaining(timerDeadline, SystemClock.elapsedRealtime()) == 0L) {
                stopAll()
            } else {
                applyVolumes()
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("zensounds_playback_channel", getString(R.string.zen_notification_channel_name), NotificationManager.IMPORTANCE_LOW)
        channel.description = getString(R.string.zen_notification_channel_description)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        player = newPlayer(primary = true)
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (!applyingMix) {
                    if (player.currentMediaItem?.mediaId != PlaybackCommands.MIX_ID && layers.isNotEmpty()) clearLayers()
                    if (player.mediaItemCount == 0 && timerDeadline > 0L) cancelTimer()
                    synchronizeLayers()
                    publishState()
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!applyingMix && mediaItem?.mediaId != PlaybackCommands.MIX_ID) {
                    playbackError = false
                    clearLayers()
                    applyVolumes()
                    publishState()
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                synchronizeLayers()
                publishState()
            }
        })
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        mediaSession = MediaSession.Builder(this, player).setSessionActivity(activity)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    if (controller.packageName != packageName) return super.onConnect(session, controller)
                    return PlaybackCommands.appConnectionResult()
                }
                override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    if (controller.packageName != packageName) return completeResult(SessionError.ERROR_PERMISSION_DENIED)
                    val result = try {
                        when (customCommand.customAction) {
                            PlaybackCommands.APPLY_MIX -> applyMix(
                                MixCodec.decodeLayers(args.getString(PlaybackCommands.LAYERS) ?: "[]"),
                                args.getBoolean(PlaybackCommands.PLAY, true),
                                args.getString(PlaybackCommands.MIX_NAME), args.getString(PlaybackCommands.MIX_SAVED_ID))
                            PlaybackCommands.LAYER_VOLUME -> {
                                val name = args.getString(PlaybackCommands.NAME)
                                val volume = args.getFloat(PlaybackCommands.VOLUME)
                                require(volume.isFinite())
                                layers = layers.map { if (it.soundName == name) it.copy(volume = volume.coerceIn(0f, 1f)) else it }
                                applyVolumes(); publishState()
                            }
                            PlaybackCommands.MASTER_VOLUME -> {
                                val volume = args.getFloat(PlaybackCommands.VOLUME)
                                require(volume.isFinite())
                                masterVolume = volume.coerceIn(0f, 1f)
                                applyVolumes(); publishState()
                            }
                            PlaybackCommands.SET_TIMER -> {
                                require(player.mediaItemCount > 0)
                                val duration = TimerRules.durationMillis(args.getInt(PlaybackCommands.MINUTES))
                                fadeSeconds = args.getInt(PlaybackCommands.FADE_SECONDS, 10).coerceIn(0, 60)
                                timerDeadline = SystemClock.elapsedRealtime() + duration
                                handler.removeCallbacks(timerTick)
                                handler.post(timerTick)
                                publishState()
                            }
                            PlaybackCommands.CANCEL_TIMER -> cancelTimer()
                            PlaybackCommands.STOP_ALL -> stopAll()
                            else -> return completeResult(SessionError.ERROR_NOT_SUPPORTED)
                        }
                        SessionResult.RESULT_SUCCESS
                    } catch (error: Exception) {
                        android.util.Log.e("ZenSoundsPlayback", "Playback command failed", error)
                        playbackError = true
                        publishState()
                        SessionError.ERROR_BAD_VALUE
                    }
                    return completeResult(result)
                }
            }).build()
        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(notificationProvider)
        publishState()
    }

    private fun completeResult(code: Int): ListenableFuture<SessionResult> = Futures.immediateFuture(SessionResult(code))

    private fun newPlayer(primary: Boolean): ExoPlayer = ExoPlayer.Builder(this)
        .setAudioAttributes(AudioAttributes.DEFAULT, primary)
        .setHandleAudioBecomingNoisy(primary)
        .setWakeMode(C.WAKE_MODE_LOCAL).build().apply { repeatMode = Player.REPEAT_MODE_ONE }

    private fun item(name: String, mix: Boolean): MediaItem = MediaItem.Builder()
        .setUri(requireNotNull(SoundCatalog.uri(this, name)))
        .setMediaId(if (mix) PlaybackCommands.MIX_ID else name)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(if (mix) mixName ?: getString(R.string.ess_mix_title) else LocalizedLabels.soundName(this, name))
            .setArtist(if (mix) layers.joinToString(" + ") { LocalizedLabels.soundName(this, it.soundName) } else "ZenSounds")
            .setArtworkUri(Uri.parse("android.resource://$packageName/${SoundCatalog.artwork(name)}")).build()).build()

    private fun applyMix(request: List<MixLayer>, play: Boolean, name: String?, savedId: String?) {
        val normalized = MixRules.normalize(request)
        if (normalized.isEmpty()) { stopAll(); return }
        val label = name?.trim()?.take(40)?.takeIf { it.isNotEmpty() }
        val identity = savedId?.takeIf { it.isNotBlank() }
        // Salvar ou reabrir a mesma combinação atualiza seu nome sem reiniciar o áudio.
        if (normalized == layers && player.currentMediaItem?.mediaId == PlaybackCommands.MIX_ID && !playbackError) {
            mixName = label
            savedMixId = identity
            player.replaceMediaItem(0, item(normalized.first().soundName, mix = true))
            player.playWhenReady = play
            synchronizeLayers()
            publishState()
            return
        }
        applyingMix = true
        try {
            player.pause()
            clearLayers()
            layers = normalized
            mixName = label
            savedMixId = identity
            playbackError = false
            for (layer in normalized.drop(1)) {
                val extra = newPlayer(primary = false)
                secondaryPlayers[layer.soundName] = extra
                extra.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = true
                        // Falha em uma camada pausa a mistura inteira, sem áudio órfão.
                        player.pause(); synchronizeLayers(); publishState()
                    }
                })
                extra.setMediaItem(item(layer.soundName, mix = false))
                extra.prepare()
            }
            player.setMediaItem(item(normalized.first().soundName, mix = true))
            applyVolumes()
            player.prepare()
            player.playWhenReady = play
        } finally {
            applyingMix = false
            synchronizeLayers()
            publishState()
        }
    }

    private fun synchronizeLayers() {
        val playing = player.isPlaying && player.currentMediaItem?.mediaId == PlaybackCommands.MIX_ID
        secondaryPlayers.values.forEach { it.playWhenReady = playing }
    }
    private fun clearLayers() {
        secondaryPlayers.values.forEach { it.release() }
        secondaryPlayers.clear()
        layers = emptyList()
        mixName = null
        savedMixId = null
    }
    private fun applyVolumes() {
        val fade = TimerRules.fade(timerDeadline, SystemClock.elapsedRealtime(), fadeSeconds)
        if (layers.isEmpty()) player.volume = masterVolume * fade else {
            val gains = MixRules.gains(layers, masterVolume, fade)
            player.volume = gains[layers.first().soundName] ?: 0f
            secondaryPlayers.forEach { (name, extra) -> extra.volume = gains[name] ?: 0f }
        }
    }
    private fun cancelTimer() {
        timerDeadline = 0L
        handler.removeCallbacks(timerTick)
        applyVolumes()
        publishState()
    }
    private fun stopAll() {
        applyingMix = true
        try {
            player.pause(); player.stop(); player.clearMediaItems()
            clearLayers()
            playbackError = false
            cancelTimer()
        } finally {
            applyingMix = false
            publishState()
        }
    }
    private fun publishState() {
        val encoded = MixCodec.encodeLayers(layers)
        val selected = player.currentMediaItem?.mediaId.orEmpty()
        val signature = "$encoded:$masterVolume:$timerDeadline:$fadeSeconds:$playbackError:$mixName:$savedMixId:$selected:${player.isPlaying}:${player.playWhenReady}"
        if (signature == lastPublished || mediaSession == null) return
        lastPublished = signature
        mediaSession?.setSessionExtras(Bundle().apply {
            putInt(PlaybackCommands.VERSION, 1)
            putString(PlaybackCommands.MIX_NAME, mixName)
            putString(PlaybackCommands.MIX_SAVED_ID, savedMixId)
            putString(PlaybackCommands.SELECTED_ID, selected)
            putBoolean(PlaybackCommands.PLAYING, player.isPlaying)
            putBoolean(PlaybackCommands.PLAY_WHEN_READY, player.playWhenReady)
            putString(PlaybackCommands.LAYERS, encoded)
            putFloat(PlaybackCommands.VOLUME, masterVolume)
            putLong(PlaybackCommands.TIMER_DEADLINE, timerDeadline)
            putInt(PlaybackCommands.FADE_SECONDS, fadeSeconds)
            putBoolean(PlaybackCommands.ERROR, playbackError)
        })
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        clearLayers()
        mediaSession?.release()
        mediaSession = null
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }
}
