package com.juliocezar.zensounds.services

import android.os.Bundle
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand

object PlaybackCommands {
    // Não herda o resultado vazio do callback legado do Media3 1.11.
    fun appConnectionResult(): MediaSession.ConnectionResult {
        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        actions.forEach { commands.add(SessionCommand(it, Bundle.EMPTY)) }
        return MediaSession.ConnectionResult.accept(commands.build(), MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
    }

    const val MIX_ID = "zensounds_mix"
    const val APPLY_MIX = "com.juliocezar.zensounds.APPLY_MIX"
    const val LAYER_VOLUME = "com.juliocezar.zensounds.LAYER_VOLUME"
    const val MASTER_VOLUME = "com.juliocezar.zensounds.MASTER_VOLUME"
    const val SET_TIMER = "com.juliocezar.zensounds.SET_TIMER"
    const val CANCEL_TIMER = "com.juliocezar.zensounds.CANCEL_TIMER"
    const val STOP_ALL = "com.juliocezar.zensounds.STOP_ALL"
    val actions = listOf(APPLY_MIX, LAYER_VOLUME, MASTER_VOLUME, SET_TIMER, CANCEL_TIMER, STOP_ALL)
    const val LAYERS = "layers"
    const val MIX_NAME = "mix_name"
    const val MIX_SAVED_ID = "mix_saved_id"
    const val SELECTED_ID = "selected_id"
    const val PLAYING = "playing"
    const val PLAY_WHEN_READY = "play_when_ready"
    const val NAME = "name"
    const val VOLUME = "volume"
    const val PLAY = "play"
    const val MINUTES = "minutes"
    const val FADE_SECONDS = "fade_seconds"
    const val TIMER_DEADLINE = "timer_deadline_elapsed"
    const val ERROR = "playback_error"
    const val VERSION = "protocol_version"
}
