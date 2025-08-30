package com.juliocezar.zensounds.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {
    @Query("SELECT * FROM sounds")
    fun getAllSounds(): Flow<List<Sound>>

    @Query("SELECT COUNT(*) FROM sounds")
    suspend fun getSoundCount(): Int

    @Insert
    suspend fun insertSounds(sounds: List<Sound>)

    @Query("DELETE FROM sounds")
    suspend fun deleteAllSounds()
}