package com.juliocezar.zensounds.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sounds")
data class Sound(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val filePath: String,
    val volume: Float = 1.0f

)