package com.juliocezar.zensounds

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*

class AudioPlayerManager(context: Context) {
    private val player1: ExoPlayer = ExoPlayer.Builder(context).build()
    private val player2: ExoPlayer = ExoPlayer.Builder(context).build()
    private var isPlayer1Active = true
    private var currentSoundUri: String? = null
    private var overlapJob: Job? = null
    private var crossfadeJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun playSound(uri: String, overlapDurationMs: Long) {
        overlapJob?.cancel()
        crossfadeJob?.cancel()

        if (currentSoundUri == uri && (player1.isPlaying || player2.isPlaying)) {
            pause()
            return
        }

        stop()

        currentSoundUri = uri
        val mediaItem = MediaItem.fromUri(uri)
        if (isPlayer1Active) {
            player1.setMediaItem(mediaItem)
            player1.volume = 1.0f // Volume inicial
            player1.prepare()
            player1.play()
        } else {
            player2.setMediaItem(mediaItem)
            player2.volume = 1.0f // Volume inicial
            player2.prepare()
            player2.play()
        }

        startOverlap(uri, overlapDurationMs)
    }

    private fun startOverlap(nextUri: String, overlapDurationMs: Long) {
        val currentPlayer = if (isPlayer1Active) player1 else player2
        val nextPlayer = if (isPlayer1Active) player2 else player1

        val nextMediaItem = MediaItem.fromUri(nextUri)
        nextPlayer.setMediaItem(nextMediaItem)
        nextPlayer.volume = 0.0f // Volume inicial do próximo player é 0
        nextPlayer.prepare()

        overlapJob = coroutineScope.launch {
            while (isActive) {
                val duration = currentPlayer.duration
                val currentPosition = currentPlayer.currentPosition

                if (duration > 0 && currentPosition > 0 && duration - currentPosition <= overlapDurationMs) {
                    // Iniciar o próximo player e o crossfade
                    nextPlayer.play()
                    crossfade(currentPlayer, nextPlayer, overlapDurationMs)
                    isPlayer1Active = !isPlayer1Active
                    startOverlap(nextUri, overlapDurationMs)
                    break
                }

                delay(100)
            }
        }

        currentPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (!nextPlayer.isPlaying) {
                        startOverlap(nextUri, overlapDurationMs)
                    }
                }
            }
        })
    }

    private fun crossfade(currentPlayer: ExoPlayer, nextPlayer: ExoPlayer, durationMs: Long) {
        crossfadeJob?.cancel()
        crossfadeJob = coroutineScope.launch {
            val steps = 100 // Número de passos para o crossfade
            val stepDuration = durationMs / steps // Duração de cada passo
            repeat(steps) { step ->
                val fraction = step / steps.toFloat()
                currentPlayer.volume = 1.0f - fraction // Diminui o volume do player atual
                nextPlayer.volume = fraction // Aumenta o volume do próximo player
                delay(stepDuration)
            }
            // Garantir que o volume final esteja correto
            currentPlayer.volume = 0.0f
            nextPlayer.volume = 1.0f
        }
    }

    fun pause() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.pause()
        player2.pause()
    }

    fun resume() {
        if (isPlayer1Active) {
            player1.play()
        } else {
            player2.play()
        }
        if (currentSoundUri != null) {
            startOverlap(currentSoundUri!!, 7000)
        }
    }

    fun stop() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.stop()
        player2.stop()
        currentSoundUri = null
    }

    fun release() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.release()
        player2.release()
    }
}