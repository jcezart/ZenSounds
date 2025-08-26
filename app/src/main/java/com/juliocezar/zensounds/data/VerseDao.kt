package com.juliocezar.zensounds.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses WHERE language = :language")
    suspend fun getVersesByLanguage(language: String): List<Verse>

    @Query("""
        SELECT * FROM verses
        WHERE language = :language AND book = :book AND chapter = :chapter
        ORDER BY verseNumber ASC
    """)
    suspend fun getVersesByBookChapter(book: String, chapter: Int, language: String): List<Verse>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(verses: List<Verse>)
}
