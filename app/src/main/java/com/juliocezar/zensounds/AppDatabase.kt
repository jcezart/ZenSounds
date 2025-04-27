package com.juliocezar.zensounds

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Sound::class], version = 2, exportSchema = false) // Versão atualizada para 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zensounds_database"
                )
                    .addMigrations(MIGRATION_1_2) // Adiciona a migração
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adiciona a coluna 'volume' com valor padrão 1.0
                database.execSQL("ALTER TABLE sounds ADD COLUMN volume REAL NOT NULL DEFAULT 1.0")
            }
        }
    }
}