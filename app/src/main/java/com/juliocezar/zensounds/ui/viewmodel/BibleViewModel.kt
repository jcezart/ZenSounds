package com.juliocezar.zensounds.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.BibleCatalog
import com.juliocezar.zensounds.data.BibleRepository
import com.juliocezar.zensounds.data.Verse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class BibleViewModel(
    private val context: Context
) : ViewModel() {

    private val repository = BibleRepository(context)
    private var chapterJob: Job? = null

    private val _bibleVerses = MutableStateFlow<List<Verse>>(emptyList())
    val bibleVerses = _bibleVerses.asStateFlow()

    private val _language = MutableStateFlow(
        context.getString(R.string.zen_bible_language)
    )
    val languageFlow = _language.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _chapterError = MutableStateFlow<String?>(null)
    val chapterError = _chapterError.asStateFlow()

    val allBooks = BibleCatalog.books

    fun bookByCode(code: String) = BibleCatalog.book(code)

    fun currentLanguage() = _language.value

    fun setLanguageFromDevice() {
        setLanguage(context.getString(R.string.zen_bible_language))
    }

    fun setLanguage(lang: String) {
        val normalized = if (
            lang.lowercase() in listOf("pt", "pt-br", "pt_br")
        ) {
            "pt"
        } else {
            "en"
        }

        if (normalized == _language.value) return

        chapterJob?.cancel()
        _language.value = normalized
        _bibleVerses.value = emptyList()
        _chapterError.value = null
        _isLoading.value = false
    }

    fun loadChapter(bookCode: String, chapter: Int) {
        val book = bookByCode(bookCode) ?: return

        if (chapter !in 1..book.chapters) return

        val language = _language.value

        chapterJob?.cancel()
        _bibleVerses.value = emptyList()
        _chapterError.value = null
        _isLoading.value = true

        chapterJob = viewModelScope.launch {
            try {
                val verses = repository.chapter(
                    code = bookCode,
                    number = chapter,
                    language = language
                )

                coroutineContext.ensureActive()
                _bibleVerses.value = verses
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "ZenSoundsBible",
                    "Failed $bookCode/$chapter ($language)",
                    e
                )

                _chapterError.value = "chapter_unavailable"
            } finally {
                if (coroutineContext[Job]?.isActive == true) {
                    _isLoading.value = false
                }
            }
        }
    }
}

class BibleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(BibleViewModel::class.java))

        @Suppress("UNCHECKED_CAST")
        return BibleViewModel(context.applicationContext) as T
    }
}