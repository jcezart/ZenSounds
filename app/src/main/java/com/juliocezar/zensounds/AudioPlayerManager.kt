package com.juliocezar.zensounds

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import kotlin.math.exp

class AudioPlayerManager(context: Context) {
    private val player1: ExoPlayer = ExoPlayer.Builder(context).build()
    private val player2: ExoPlayer = ExoPlayer.Builder(context).build()
    private var isPlayer1Active = true
    private var currentSoundUri: String? = null
    private var currentVolume: Float = 1.0f
    private var overlapJob: Job? = null
    private var crossfadeJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun playSound(uri: String, volume: Float, overlapDurationMs: Long) {
        overlapJob?.cancel()
        crossfadeJob?.cancel()

        if (currentSoundUri == uri && (player1.isPlaying || player2.isPlaying)) {
            pause()
            return
        }

        stop()

        currentSoundUri = uri
        currentVolume = volume
        val mediaItem = MediaItem.fromUri(uri)
        if (isPlayer1Active) {
            player1.setMediaItem(mediaItem)
            player1.volume = volume
            player1.prepare()
            player1.play()
        } else {
            player2.setMediaItem(mediaItem)
            player2.volume = volume
            player2.prepare()
            player2.play()
        }

        startOverlap(uri, volume, overlapDurationMs)
    }

    private fun startOverlap(nextUri: String, volume: Float, overlapDurationMs: Long) {
        val currentPlayer = if (isPlayer1Active) player1 else player2
        val nextPlayer = if (isPlayer1Active) player2 else player1

        val nextMediaItem = MediaItem.fromUri(nextUri)
        nextPlayer.setMediaItem(nextMediaItem)
        nextPlayer.volume = 0.0f
        nextPlayer.prepare()
        nextPlayer.seekTo(0)

        overlapJob?.cancel()
        overlapJob = coroutineScope.launch {
            while (isActive) {
                val duration = currentPlayer.duration
                val currentPosition = currentPlayer.currentPosition

                if (duration > 0 && currentPosition > 0 && duration - currentPosition <= overlapDurationMs) {
                    nextPlayer.play()
                    crossfade(currentPlayer, nextPlayer, volume, overlapDurationMs)
                    isPlayer1Active = !isPlayer1Active
                    break // Sai do loop após iniciar o crossfade
                }

                delay(50)
            }

            // Após o crossfade, reiniciar o loop para o próximo ciclo
            delay(overlapDurationMs) // Espera o crossfade terminar antes de iniciar o próximo ciclo
            if (currentSoundUri != null && (player1.isPlaying || player2.isPlaying)) {
                startOverlap(currentSoundUri!!, currentVolume, overlapDurationMs)
            }
        }

        currentPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (!nextPlayer.isPlaying) {
                        overlapJob?.cancel()
                        startOverlap(nextUri, volume, overlapDurationMs)
                    }
                }
            }
        })
    }

    private fun crossfade(currentPlayer: ExoPlayer, nextPlayer: ExoPlayer, targetVolume: Float, durationMs: Long) {
        crossfadeJob?.cancel()
        crossfadeJob = coroutineScope.launch {
            val steps = 400
            val stepDuration = durationMs / steps
            repeat(steps) { step ->
                val fraction = step / steps.toFloat()
                // Usar uma curva exponencial para o crossfade
                val expFraction = 1 - exp(-5 * fraction)
                currentPlayer.volume = (1.0f - expFraction) * targetVolume
                nextPlayer.volume = expFraction * targetVolume
                delay(stepDuration)
            }
            currentPlayer.volume = 0.0f
            nextPlayer.volume = targetVolume
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
            startOverlap(currentSoundUri!!, currentVolume, 18000)
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