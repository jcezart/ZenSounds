
package com.juliocezar.zensounds

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import kotlin.math.min

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
        println("DEBUG: playSound called with uri=$uri, volume=$volume")
        overlapJob?.cancel()
        crossfadeJob?.cancel()

        if (currentSoundUri == uri && (player1.isPlaying || player2.isPlaying)) {
            pause()
            return
        }

        stop()

        currentSoundUri = uri
        currentVolume = volume.coerceIn(0.0f, 1.0f)
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
        println("DEBUG: Started playing uri=$uri on player${if (isPlayer1Active) 1 else 2}")

        startOverlap(overlapDurationMs)
    }

    private fun startOverlap(overlapDurationMs: Long) {
        val currentPlayer = if (isPlayer1Active) player1 else player2
        val nextPlayer = if (isPlayer1Active) player2 else player1
        val uri = currentSoundUri ?: return
        println("DEBUG: Starting overlap for uri=$uri")

        val nextMediaItem = MediaItem.fromUri(uri)
        nextPlayer.setMediaItem(nextMediaItem)
        nextPlayer.volume = 0.0f
        nextPlayer.prepare()
        nextPlayer.seekTo(0)
        println("DEBUG: Prepared nextPlayer with uri=$uri")

        overlapJob?.cancel()
        overlapJob = coroutineScope.launch {
            var duration = currentPlayer.duration
            var attempts = 0
            while (duration <= 0 || duration == C.TIME_UNSET) {
                if (attempts++ > 50) { // Limite de tentativas para evitar loop infinito
                    duration = overlapDurationMs // Fallback para duração padrão
                    println("DEBUG: Duration not detected for uri=$uri, using fallback=$duration")
                    break
                }
                delay(100)
                duration = currentPlayer.duration
                println("DEBUG: Waiting for duration, attempt=$attempts, current=$duration")
            }

            val effectiveOverlap = min(overlapDurationMs, duration / 2).coerceAtLeast(1000L) // Mínimo de 1s
            println("DEBUG: duration=$duration, effectiveOverlap=$effectiveOverlap")

            while (isActive) {
                val currentPosition = currentPlayer.currentPosition
                if (duration > 0 && currentPosition > 0 && duration - currentPosition <= effectiveOverlap) {
                    nextPlayer.play()
                    println("DEBUG: nextPlayer started for uri=$uri")
                    crossfade(currentPlayer, nextPlayer, currentVolume, effectiveOverlap)
                    isPlayer1Active = !isPlayer1Active
                    break
                }
                delay(50)
                if (!currentPlayer.isPlaying && !nextPlayer.isPlaying) {
                    println("DEBUG: Both players stopped unexpectedly for uri=$uri")
                    break // Sai do loop para evitar polling inútil
                }
            }

            delay(effectiveOverlap)
            if (currentSoundUri == uri) {
                println("DEBUG: Restarting loop for uri=$uri")
                startOverlap(overlapDurationMs)
            } else {
                println("DEBUG: Loop not restarted: uri=$uri, currentSoundUri=$currentSoundUri, player1Playing=${player1.isPlaying}, player2Playing=${player2.isPlaying}")
            }
        }

        currentPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && currentSoundUri == uri) {
                    if (!nextPlayer.isPlaying) {
                        println("DEBUG: Fallback triggered for uri=$uri")
                        overlapJob?.cancel()
                        startOverlap(overlapDurationMs)
                    }
                }
            }

            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                println("DEBUG: Player error for uri=$uri, error=${error.message}")
                if (currentSoundUri == uri) {
                    overlapJob?.cancel()
                    startOverlap(overlapDurationMs) // Tenta reiniciar
                }
            }
        })
    }

    private fun crossfade(currentPlayer: ExoPlayer, nextPlayer: ExoPlayer, targetVolume: Float, durationMs: Long) {
        crossfadeJob?.cancel()
        crossfadeJob = coroutineScope.launch {
            val steps = 100
            val stepDuration = durationMs / steps
            repeat(steps) { step ->
                val fraction = step / steps.toFloat()
                val currentVol = (1.0f - fraction) * targetVolume
                val nextVol = fraction * targetVolume
                currentPlayer.volume = currentVol
                nextPlayer.volume = nextVol
                delay(stepDuration)
            }
            currentPlayer.volume = 0.0f
            nextPlayer.volume = targetVolume
            currentPlayer.pause()
            println("DEBUG: Crossfade completed for uri=$currentSoundUri")
        }
    }

    fun pause() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.pause()
        player2.pause()
        println("DEBUG: Paused players")
    }

    fun resume() {
        if (isPlayer1Active) {
            player1.play()
        } else {
            player2.play()
        }
        currentSoundUri?.let {
            println("DEBUG: Resuming uri=$it")
            startOverlap(18000)
        }
    }

    fun stop() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.stop()
        player2.stop()
        currentSoundUri = null
        println("DEBUG: Stopped players")
    }

    fun release() {
        overlapJob?.cancel()
        crossfadeJob?.cancel()
        player1.release()
        player2.release()
        println("DEBUG: Released players")
    }
}
