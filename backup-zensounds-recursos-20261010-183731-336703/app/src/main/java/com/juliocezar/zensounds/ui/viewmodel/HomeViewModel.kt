package com.juliocezar.zensounds.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.juliocezar.zensounds.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyWordState(val verse: SavedVerse? = null, val loading: Boolean = true, val failed: Boolean = false, val bookCode: String? = null)
class HomeViewModel(context: Context) : ViewModel() {
    val preferences = EssencePreferences(context)
    private val repository = BibleRepository(context)
    private val _daily = MutableStateFlow(DailyWordState())
    val daily = _daily.asStateFlow()
    private var dailyJob: Job? = null
    private var lastRequest: String? = null
    fun refresh(language: String, force: Boolean = false) {
        val date = LocalDate.now()
        val request = "$date:$language"
        if (!force && request == lastRequest) return
        lastRequest = request
        dailyJob?.cancel()
        val reference = DailyReadings.forDate(date)
        _daily.value = DailyWordState(bookCode = reference.bookCode)
        dailyJob = viewModelScope.launch {
            try {
                val verse = repository.chapter(reference.bookCode, reference.chapter, language)
                    .first { it.verseNumber == reference.verseNumber }
                _daily.value = DailyWordState(SavedVerse(reference.bookCode, reference.chapter, reference.verseNumber, verse.text, language), loading = false, bookCode = reference.bookCode)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e("ZenSoundsHome", "Daily word unavailable", e)
                if (lastRequest == request) lastRequest = null
                _daily.value = DailyWordState(loading = false, failed = true, bookCode = reference.bookCode)
            }
        }
    }
    override fun onCleared() { preferences.close(); super.onCleared() }
}
class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(context.applicationContext) as T
    }
}