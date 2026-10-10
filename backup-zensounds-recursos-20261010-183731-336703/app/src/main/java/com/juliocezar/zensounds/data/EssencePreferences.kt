package com.juliocezar.zensounds.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class ReadingPosition(val bookCode: String, val chapter: Int)
data class SavedVerse(val bookCode: String, val chapter: Int, val verseNumber: Int, val text: String, val language: String) {
    val key: String get() = "$language:$bookCode:$chapter:$verseNumber"
}
data class EssencePreferencesState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reading: ReadingPosition? = null,
    val favoriteSounds: Set<String> = emptySet(),
    val favoriteVerses: List<SavedVerse> = emptyList()
)

class EssencePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("zensounds_essence", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state = _state.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _state.value = read() }
    init { preferences.registerOnSharedPreferenceChangeListener(listener) }
    fun close() = preferences.unregisterOnSharedPreferenceChangeListener(listener)

    private fun read(): EssencePreferencesState {
        val theme = runCatching { ThemeMode.valueOf(preferences.getString("theme", "SYSTEM") ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
        val code = preferences.getString("reading_book", null)
        val chapter = preferences.getInt("reading_chapter", 1)
        val book = code?.let(BibleCatalog::book)
        val reading = if (book != null && chapter in 1..book.chapters) ReadingPosition(book.code, chapter) else null
        val verses = runCatching {
            val json = JSONArray(preferences.getString("favorite_verses", "[]"))
            (0 until json.length()).mapNotNull { index ->
                runCatching {
                    val v = json.getJSONObject(index)
                    SavedVerse(v.getString("book"), v.getInt("chapter"), v.getInt("verse"), v.getString("text"), v.getString("language"))
                }.getOrNull()?.takeIf { BibleCatalog.book(it.bookCode) != null && it.text.isNotBlank() }
            }
        }.getOrDefault(emptyList())
        return EssencePreferencesState(theme, reading, preferences.getStringSet("favorite_sounds", emptySet()).orEmpty().toSet(), verses)
    }
    fun setTheme(mode: ThemeMode) {
        preferences.edit().putString("theme", mode.name).apply()
        _state.value = read()
    }
    fun saveReading(code: String, chapter: Int) {
        val book = BibleCatalog.book(code) ?: return
        if (chapter !in 1..book.chapters) return
        preferences.edit().putString("reading_book", code).putInt("reading_chapter", chapter).apply()
        _state.value = read()
    }
    fun toggleSound(name: String) {
        val sounds = _state.value.favoriteSounds.toMutableSet()
        if (!sounds.add(name)) sounds.remove(name)
        preferences.edit().putStringSet("favorite_sounds", sounds).apply()
        _state.value = read()
    }
    fun toggleVerse(verse: SavedVerse) {
        val verses = _state.value.favoriteVerses.toMutableList()
        val index = verses.indexOfFirst { it.key == verse.key }
        if (index >= 0) verses.removeAt(index) else verses.add(0, verse)
        val json = JSONArray()
        verses.forEach { v -> json.put(JSONObject().put("book", v.bookCode).put("chapter", v.chapter).put("verse", v.verseNumber).put("text", v.text).put("language", v.language)) }
        preferences.edit().putString("favorite_verses", json.toString()).apply()
        _state.value = read()
    }
}
