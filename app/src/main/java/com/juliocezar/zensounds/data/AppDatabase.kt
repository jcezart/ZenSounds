package com.juliocezar.zensounds.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Sound::class, Verse::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
    abstract fun verseDao(): VerseDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_4, MIGRATION_4_5)
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

        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `verses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `book` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verseNumber` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `language` TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_verses_book_chapter_verseNumber_language " +
                            "ON verses(book, chapter, verseNumber, language)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_verses_book_chapter_verseNumber_language " +
                            "ON verses(book, chapter, verseNumber, language)"
                )
            }
        }


    }
}
