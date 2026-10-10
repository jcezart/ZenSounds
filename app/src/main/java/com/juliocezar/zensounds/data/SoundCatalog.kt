package com.juliocezar.zensounds.data

import android.content.Context
import android.net.Uri
import com.juliocezar.zensounds.R

object SoundCatalog {
    fun artwork(name: String): Int = when (name) {
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
    fun uri(context: Context, name: String): Uri? {
        val id = when (name) {
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
        return Uri.Builder().scheme("android.resource").authority(context.packageName)
            .appendPath("raw").appendPath(context.resources.getResourceEntryName(id)).build()
    }
}
