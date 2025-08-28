package com.juliocezar.zensounds.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.juliocezar.zensounds.api.RetrofitClient
import com.juliocezar.zensounds.data.AppDatabase
import com.juliocezar.zensounds.data.Verse
import com.juliocezar.zensounds.data.VerseDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.awaitResponse
import java.util.Locale

class BibleViewModel(private val context: Context) : ViewModel() {

    private val verseDao: VerseDao = AppDatabase.getDatabase(context).verseDao()

    private val _bibleVerses = MutableStateFlow<List<Verse>>(emptyList())
    val bibleVerses: StateFlow<List<Verse>> get() = _bibleVerses

    //private var language: String = "en"
    private val _language = MutableStateFlow(devicePreferredLanguage()) // "pt" ou "en"
    val languageFlow: StateFlow<String> = _language

    fun currentLanguage(): String = _language.value

    /** Retorna "pt" ou "en" conforme o aparelho; se for outro idioma, padroniza "en". */
    private fun devicePreferredLanguage(): String {
        val lang = try {
            val loc = context.resources.configuration.locales.get(0)
            loc?.language ?: Locale.getDefault().language
        } catch (_: Throwable) {
            Locale.getDefault().language
        }
        return when (lang.lowercase()) {
            "pt", "pt_br", "pt-pt", "pt-br" -> "pt"
            "en", "en_us", "en_gb", "en-us", "en-gb" -> "en"
            else -> "en"
        }
    }

    // Catalogo básico (Pentateuco)
    data class BookMeta(
        val code: String,      // ex.: "GEN"
        val namePt: String,    // "Gênesis"
        val nameEn: String,    // "Genesis"
        val chapters: Int      // 50
    )

    val allBooks = listOf(
        BookMeta("GEN", "Gênesis",      "Genesis",     50),
        BookMeta("EXO", "Êxodo",        "Exodus",      40),
        BookMeta("LEV", "Levítico",     "Leviticus",   27),
        BookMeta("NUM", "Números",      "Numbers",     36),
        BookMeta("DEU", "Deuteronômio", "Deuteronomy", 34),
    )

    fun bookByCode(code: String): BookMeta? = allBooks.find { it.code == code }

    private fun dbBookNameByCode(code: String): String? = when (code) {
        "GEN" -> "Genesis"
        "EXO" -> "Exodus"
        "LEV" -> "Leviticus"
        "NUM" -> "Numbers"
        "DEU" -> "Deuteronomy"
        else  -> null
    }

    // ----> AJUSTE AQUI: usar idioma do aparelho e deixar o loadChapter cuidar do fallback
    init {
        viewModelScope.launch {
            _language.value = devicePreferredLanguage()       // "pt" ou "en" (ou "en" por padrão)
            loadChapter("GEN", 1)                      // se a tradução não existir, loadChapter cai pra EN
        }
    }

    /** Opcional: sincroniza manualmente com o idioma do aparelho e recarrega GEN 1 */
    fun setLanguageFromDevice() {
        setLanguage(devicePreferredLanguage())
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        loadChapter("GEN", 1)
    }

    private fun translationFor(lang: String): String = when (lang) {
        "pt" -> "por_blj"
        "en" -> "eng_kjv"
        else -> "eng_kjv"
    }

    fun loadChapter(bookCode: String, chapter: Int) {
        viewModelScope.launch {
            val dbName = dbBookNameByCode(bookCode) ?: return@launch

            // 1) Tenta no idioma atual (capítulo específico)
            var verses = verseDao.getVersesByBookChapter(dbName, chapter, _language.value)
            if (verses.isEmpty()) {
                // 2) Se não tem, baixa e persiste nesse idioma
                fetchAndInsertChapter(bookCode, chapter, _language.value)
                verses = verseDao.getVersesByBookChapter(dbName, chapter, _language.value)
            }

            // 3) Fallback (pt <-> en) se ainda vazio
            if (verses.isEmpty()) {
                val altLang = if (_language.value == "pt") "en" else "pt"
                fetchAndInsertChapter(bookCode, chapter, altLang)
                val alt = verseDao.getVersesByBookChapter(dbName, chapter, altLang)
                if (alt.isNotEmpty()) {
                    _language.value = altLang
                    verses = alt
                }
            }

            _bibleVerses.value = verses
        }
    }

    private suspend fun fetchAndInsertChapter(
        bookCode: String,
        chapter: Int,
        targetLang: String
    ) {
        val translationId = translationFor(targetLang)
        val resp = runCatching {
            RetrofitClient.bibleService.getChapter(translationId, bookCode, chapter).awaitResponse()
        }.getOrNull()

        if (resp?.isSuccessful != true) return
        val root = resp.body() ?: return
        if (!root.isJsonObject) return

        val obj: JsonObject = root.asJsonObject
        val chapterObj: JsonObject = obj.getAsJsonObject("chapter") ?: return
        val content: JsonArray = chapterObj.getAsJsonArray("content") ?: return

        val dbBook = dbBookNameByCode(bookCode) ?: return

        val toInsert = buildList {
            content.forEach { el ->
                val item = el.asJsonObject
                if (item.get("type")?.asString == "verse") {
                    val num = item.get("number")?.asInt ?: return@forEach
                    val pieces = item.getAsJsonArray("content")
                    val text = buildString {
                        pieces?.forEach { p ->
                            if (p.isJsonPrimitive && p.asJsonPrimitive.isString) {
                                append(p.asString).append(' ')
                            }
                        }
                    }.trim()
                    add(
                        Verse(
                            book = dbBook,
                            chapter = chapter,
                            verseNumber = num,
                            text = text,
                            language = targetLang
                        )
                    )
                }
            }
        }

        if (toInsert.isNotEmpty()) {
            verseDao.insertAll(toInsert)
        }
    }
}

class BibleViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BibleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BibleViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}