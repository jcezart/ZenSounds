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

class BibleViewModel(private val context: Context) : ViewModel() {

    private val verseDao: VerseDao = AppDatabase.getDatabase(context).verseDao()

    // O que a UI consome
    private val _bibleVerses = MutableStateFlow<List<Verse>>(emptyList())
    val bibleVerses: StateFlow<List<Verse>> get() = _bibleVerses

    // Idioma atual (padrão pt). Os versos gravados usam este campo.
    private var language: String = "pt"

    // ---------------------------
    // Catálogo básico de livros
    // ---------------------------
    data class BookMeta(
        val code: String,      // ex.: "GEN"
        val namePt: String,    // "Gênesis"
        val nameEn: String,    // "Genesis"
        val chapters: Int      // 50
    )

    // Pentateuco (expanda quando quiser)
    val allBooks: List<BookMeta> = listOf(
        BookMeta("GEN", "Gênesis",       "Genesis",      50),
        BookMeta("EXO", "Êxodo",         "Exodus",       40),
        BookMeta("LEV", "Levítico",      "Leviticus",    27),
        BookMeta("NUM", "Números",       "Numbers",      36),
        BookMeta("DEU", "Deuteronômio",  "Deuteronomy",  34)
    )

    fun bookByCode(code: String): BookMeta? = allBooks.find { it.code == code }

    // Nome padronizado salvo no BD (usei inglês)
    private fun dbBookNameByCode(code: String): String? = when (code) {
        "GEN" -> "Genesis"
        "EXO" -> "Exodus"
        "LEV" -> "Leviticus"
        "NUM" -> "Numbers"
        "DEU" -> "Deuteronomy"
        else  -> null
    }

    init {
        // Ao abrir: tenta exibir GEN 1 do BD (pt → en fallback) ou baixa se necessário.
        viewModelScope.launch {
            val hasPt = verseDao.getVersesByLanguage("pt").isNotEmpty()
            val hasEn = verseDao.getVersesByLanguage("en").isNotEmpty()
            language = when {
                hasPt -> "pt"
                hasEn -> "en"
                else  -> "pt"
            }
            loadChapter("GEN", 1)
        }
    }

    fun setLanguage(lang: String) {
        language = lang
        // Se quiser manter o capítulo atual, guarde estado externo. Aqui recarrego GEN 1.
        loadChapter("GEN", 1)
    }

    private fun translationFor(lang: String): String = when (lang) {
        "pt" -> "por_blj"  // Ou outra opção portuguesa disponível
        "en" -> "eng_kjv"
        else -> "eng_kjv"
    }

    /**
     * Carrega UM capítulo:
     * 1) tenta do Room por (book, chapter, language)
     * 2) se vazio, baixa só aquele capítulo e insere
     * 3) atualiza a UI com o que ficou no Room
     * 4) fallback para o outro idioma, se ainda assim estiver vazio
     */
    fun loadChapter(bookCode: String, chapter: Int) {
        viewModelScope.launch {
            val dbName = dbBookNameByCode(bookCode) ?: return@launch

            // 1) Tenta no idioma atual (capítulo específico)
            var verses = verseDao.getVersesByBookChapter(dbName, chapter, language)
            if (verses.isEmpty()) {
                // 2) Se não tem, baixa e persiste nesse idioma
                fetchAndInsertChapter(bookCode, chapter, language)
                verses = verseDao.getVersesByBookChapter(dbName, chapter, language)
            }

            // 3) Fallback (pt <-> en) se ainda vazio
            if (verses.isEmpty()) {
                val altLang = if (language == "pt") "en" else "pt"
                fetchAndInsertChapter(bookCode, chapter, altLang)
                val alt = verseDao.getVersesByBookChapter(dbName, chapter, altLang)
                if (alt.isNotEmpty()) {
                    language = altLang
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
        // Chama a API correta (com /api/ e IDs novos de tradução)
        val translationId = translationFor(targetLang) // ex.: "por_blj" | "eng_kjv"
        val resp = runCatching {
            RetrofitClient.bibleService.getChapter(translationId, bookCode, chapter).awaitResponse()
        }.getOrNull()

        if (resp?.isSuccessful != true) return
        val root = resp.body() ?: return
        if (!root.isJsonObject) return

        val obj = root.asJsonObject
        val chapterObj = obj.getAsJsonObject("chapter") ?: return
        val content = chapterObj.getAsJsonArray("content") ?: return

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
