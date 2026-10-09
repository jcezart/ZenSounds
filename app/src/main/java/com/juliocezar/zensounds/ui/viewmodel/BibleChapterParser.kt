package com.juliocezar.zensounds.ui.viewmodel

import com.google.gson.JsonElement
import com.juliocezar.zensounds.data.Verse

// O JSON contém tanto strings quanto objetos com texto formatado.
internal object BibleChapterParser {
    fun parse(root: JsonElement, dbBook: String, chapter: Int, language: String): List<Verse> {
        require(root.isJsonObject) { "Invalid chapter response" }
        val obj = root.asJsonObject
        require(obj.getAsJsonObject("chapter")?.get("number")?.asInt == chapter) {
            "Unexpected chapter number"
        }
        val content = obj.getAsJsonObject("chapter")?.getAsJsonArray("content")
            ?: error("Missing chapter content")
        return content.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject
            if (item.get("type")?.asString != "verse") return@mapNotNull null
            val number = item.get("number")?.asInt ?: return@mapNotNull null
            val pieces = item.getAsJsonArray("content") ?: return@mapNotNull null
            val text = buildString {
                pieces.forEach { piece ->
                    when {
                        piece.isJsonPrimitive && piece.asJsonPrimitive.isString -> {
                            append(piece.asString).append(' ')
                        }
                        piece.isJsonObject -> {
                            val formatted = piece.asJsonObject
                            val value = formatted.get("text")
                            if (value?.isJsonPrimitive == true && value.asJsonPrimitive.isString) {
                                append(value.asString).append(' ')
                            } else if (formatted.get("lineBreak")?.asBoolean == true) {
                                append('\n')
                            }
                        }
                    }
                }
            }.trim()
            if (text.isBlank()) return@mapNotNull null
            Verse(book = dbBook, chapter = chapter, verseNumber = number,
                text = text, language = language)
        }.sortedBy { it.verseNumber }
    }
}
