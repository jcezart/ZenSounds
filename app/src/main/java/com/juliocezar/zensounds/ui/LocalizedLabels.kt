package com.juliocezar.zensounds.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.juliocezar.zensounds.R

// Os identificadores em inglês permanecem iguais no banco, nas rotas e no player.
object LocalizedLabels {
    fun soundResource(name: String): Int? = when (name) {
        "Rain" -> R.string.zen_sound_rain
        "Storm" -> R.string.zen_sound_storm
        "Wind" -> R.string.zen_sound_wind
        "Forest" -> R.string.zen_sound_forest
        "Stream" -> R.string.zen_sound_stream
        "Fireplace" -> R.string.zen_sound_fireplace
        "TV Static" -> R.string.zen_sound_tv_static
        "Car Engine" -> R.string.zen_sound_car_engine
        else -> null
    }

    fun categoryResource(title: String): Int? = when (title) {
        "Nature Sounds" -> R.string.zen_category_nature
        "Object Sounds" -> R.string.zen_category_objects
        "Vehicle Sounds" -> R.string.zen_category_vehicles
        else -> null
    }

    fun soundName(context: Context, name: String): String =
        soundResource(name)?.let { context.getString(it) } ?: name
}

@Composable
fun localizedSoundName(name: String): String =
    LocalizedLabels.soundResource(name)?.let { stringResource(it) } ?: name

@Composable
fun localizedCategoryName(title: String): String =
    LocalizedLabels.categoryResource(title)?.let { stringResource(it) } ?: title
