package com.juliocezar.zensounds.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.math.max

data class MixLayer(val soundName: String, val volume: Float = 0.6f)
data class SavedMix(val id: String, val name: String, val layers: List<MixLayer>)

object MixRules {
    val soundNames = listOf("Rain", "Storm", "Wind", "Forest", "Stream", "Fireplace", "TV Static", "Car Engine")
    fun normalize(layers: List<MixLayer>): List<MixLayer> = layers
        .filter { it.soundName in soundNames && it.volume.isFinite() }
        .distinctBy { it.soundName }
        .map { it.copy(volume = it.volume.coerceIn(0f, 1f)) }
    fun gains(layers: List<MixLayer>, master: Float, fade: Float = 1f): Map<String, Float> {
        val normalized = normalize(layers)
        val scale = master.coerceIn(0f, 1f) * fade.coerceIn(0f, 1f) / max(1f, normalized.sumOf { it.volume.toDouble() }.toFloat())
        return normalized.associate { it.soundName to it.volume * scale }
    }
}

object MixCodec {
    private val gson = Gson()
    fun encodeLayers(layers: List<MixLayer>): String = gson.toJson(MixRules.normalize(layers))
    fun decodeLayers(json: String): List<MixLayer> = runCatching {
        val array = JsonParser.parseString(json).asJsonArray
        require(array.size() <= MixRules.soundNames.size)
        MixRules.normalize(array.map { element ->
            val obj = element.asJsonObject
            MixLayer(obj.get("soundName").asString, obj.get("volume").asFloat)
        })
    }.getOrDefault(emptyList())
    fun encodeSaved(mixes: List<SavedMix>): String = gson.toJson(mixes)
    fun decodeSaved(json: String): List<SavedMix> = runCatching {
        JsonParser.parseString(json).asJsonArray.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                val id = obj.get("id").asString
                val name = obj.get("name").asString.trim().take(40)
                val layers = decodeLayers(obj.get("layers").toString())
                if (id.isBlank() || name.isBlank() || layers.isEmpty()) null else SavedMix(id, name, layers)
            }.getOrNull()
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())
}

object TimerRules {
    fun durationMillis(minutes: Int): Long {
        require(minutes in 1..1440)
        return minutes * 60_000L
    }
    fun remaining(deadline: Long, now: Long): Long = if (deadline <= 0L) 0L else (deadline - now).coerceAtLeast(0L)
    fun fade(deadline: Long, now: Long, fadeSeconds: Int): Float {
        if (deadline <= 0L || fadeSeconds <= 0) return 1f
        return (remaining(deadline, now).toFloat() / (fadeSeconds.coerceIn(0, 60) * 1000f)).coerceIn(0f, 1f)
    }
}
