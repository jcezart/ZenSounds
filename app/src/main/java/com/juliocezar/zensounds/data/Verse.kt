package com.juliocezar.zensounds.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verses",
    indices = [
        Index(
            value = ["book", "chapter", "verseNumber", "language"],
            unique = true
        )
    ]
)
data class Verse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val language: String
)
