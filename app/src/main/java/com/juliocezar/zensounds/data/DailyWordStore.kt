package com.juliocezar.zensounds.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.random.Random

object DailyWordStore {
    private val mutex = Mutex()
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("zensounds_daily_word", Context.MODE_PRIVATE)
    fun reference(context: Context, date: LocalDate = LocalDate.now()): DailyReading {
        val p = prefs(context)
        if (p.getString("date", null) == date.toString()) {
            val stored = DailyReading(p.getString("book", "") ?: "", p.getInt("chapter", 0), p.getInt("verse", 0))
            if (stored in DailyReadings.entries) return stored
        }
        return DailyReadings.forDate(date)
    }
    fun alternative(current: DailyReading, random: Random = Random.Default): DailyReading =
        DailyReadings.entries.filter { it != current }.random(random)

    suspend fun load(context: Context, language: String, shuffle: Boolean = false): SavedVerse = mutex.withLock {
        val date = LocalDate.now()
        val current = reference(context, date)
        val chosen = if (shuffle) alternative(current) else current
        val verse = BibleRepository(context).chapter(chosen.bookCode, chosen.chapter, language)
            .first { it.verseNumber == chosen.verseNumber }
        if (shuffle) prefs(context).edit().putString("date", date.toString())
            .putString("book", chosen.bookCode).putInt("chapter", chosen.chapter).putInt("verse", chosen.verseNumber).apply()
        SavedVerse(chosen.bookCode, chosen.chapter, chosen.verseNumber, verse.text, language)
    }
}
