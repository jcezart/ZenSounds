package com.juliocezar.zensounds.data

import android.content.Context
import com.google.gson.JsonParser
import com.juliocezar.zensounds.ui.viewmodel.BibleChapterParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.Reader
import java.util.zip.GZIPInputStream

data class BibleBook(val code: String, val namePt: String, val nameEn: String, val chapters: Int) {
    fun label(language: String) = if (language == "pt") namePt else nameEn
}

object BibleCatalog {
    val books = listOf(
        BibleBook("GEN", "Gênesis", "Genesis", 50),
        BibleBook("EXO", "Êxodo", "Exodus", 40),
        BibleBook("LEV", "Levítico", "Leviticus", 27),
        BibleBook("NUM", "Números", "Numbers", 36),
        BibleBook("DEU", "Deuteronômio", "Deuteronomy", 34),
        BibleBook("JOS", "Josué", "Joshua", 24),
        BibleBook("JDG", "Juízes", "Judges", 21),
        BibleBook("RUT", "Rute", "Ruth", 4),
        BibleBook("1SA", "1 Samuel", "1 Samuel", 31),
        BibleBook("2SA", "2 Samuel", "2 Samuel", 24),
        BibleBook("1KI", "1 Reis", "1 Kings", 22),
        BibleBook("2KI", "2 Reis", "2 Kings", 25),
        BibleBook("1CH", "1 Crônicas", "1 Chronicles", 29),
        BibleBook("2CH", "2 Crônicas", "2 Chronicles", 36),
        BibleBook("EZR", "Esdras", "Ezra", 10),
        BibleBook("NEH", "Neemias", "Nehemiah", 13),
        BibleBook("EST", "Ester", "Esther", 10),
        BibleBook("JOB", "Jó", "Job", 42),
        BibleBook("PSA", "Salmos", "Psalms", 150),
        BibleBook("PRO", "Provérbios", "Proverbs", 31),
        BibleBook("ECC", "Eclesiastes", "Ecclesiastes", 12),
        BibleBook("SNG", "Cantares", "Song of Solomon", 8),
        BibleBook("ISA", "Isaías", "Isaiah", 66),
        BibleBook("JER", "Jeremias", "Jeremiah", 52),
        BibleBook("LAM", "Lamentações", "Lamentations", 5),
        BibleBook("EZK", "Ezequiel", "Ezekiel", 48),
        BibleBook("DAN", "Daniel", "Daniel", 12),
        BibleBook("HOS", "Oseias", "Hosea", 14),
        BibleBook("JOL", "Joel", "Joel", 3),
        BibleBook("AMO", "Amós", "Amos", 9),
        BibleBook("OBA", "Obadias", "Obadiah", 1),
        BibleBook("JON", "Jonas", "Jonah", 4),
        BibleBook("MIC", "Miqueias", "Micah", 7),
        BibleBook("NAM", "Naum", "Nahum", 3),
        BibleBook("HAB", "Habacuque", "Habakkuk", 3),
        BibleBook("ZEP", "Sofonias", "Zephaniah", 3),
        BibleBook("HAG", "Ageu", "Haggai", 2),
        BibleBook("ZEC", "Zacarias", "Zechariah", 14),
        BibleBook("MAL", "Malaquias", "Malachi", 4),
        BibleBook("MAT", "Mateus", "Matthew", 28),
        BibleBook("MRK", "Marcos", "Mark", 16),
        BibleBook("LUK", "Lucas", "Luke", 24),
        BibleBook("JHN", "João", "John", 21),
        BibleBook("ACT", "Atos", "Acts", 28),
        BibleBook("ROM", "Romanos", "Romans", 16),
        BibleBook("1CO", "1 Coríntios", "1 Corinthians", 16),
        BibleBook("2CO", "2 Coríntios", "2 Corinthians", 13),
        BibleBook("GAL", "Gálatas", "Galatians", 6),
        BibleBook("EPH", "Efésios", "Ephesians", 6),
        BibleBook("PHP", "Filipenses", "Philippians", 4),
        BibleBook("COL", "Colossenses", "Colossians", 4),
        BibleBook("1TH", "1 Tessalonicenses", "1 Thessalonians", 5),
        BibleBook("2TH", "2 Tessalonicenses", "2 Thessalonians", 3),
        BibleBook("1TI", "1 Timóteo", "1 Timothy", 6),
        BibleBook("2TI", "2 Timóteo", "2 Timothy", 4),
        BibleBook("TIT", "Tito", "Titus", 3),
        BibleBook("PHM", "Filemom", "Philemon", 1),
        BibleBook("HEB", "Hebreus", "Hebrews", 13),
        BibleBook("JAS", "Tiago", "James", 5),
        BibleBook("1PE", "1 Pedro", "1 Peter", 5),
        BibleBook("2PE", "2 Pedro", "2 Peter", 3),
        BibleBook("1JN", "1 João", "1 John", 5),
        BibleBook("2JN", "2 João", "2 John", 1),
        BibleBook("3JN", "3 João", "3 John", 1),
        BibleBook("JUD", "Judas", "Jude", 1),
        BibleBook("REV", "Apocalipse", "Revelation", 22),
    )
    fun book(code: String) = books.find { it.code == code }
}

class BibleRepository(context: Context) {
    private val assets = context.applicationContext.assets

    suspend fun chapter(code: String, number: Int, language: String): List<Verse> =
        withContext(Dispatchers.IO) {
            val book = requireNotNull(BibleCatalog.book(code))
            require(number in 1..book.chapters)
            require(language == "pt" || language == "en")
            // Cada livro já acompanha o app. Não há consulta à API na leitura.
            BundledBibleReader.readAsset({ path -> assets.open(path) }, book, number, language)
        }
}

internal object BundledBibleReader {
    fun readAsset(
        openAsset: (String) -> InputStream,
        book: BibleBook,
        number: Int,
        language: String
    ): List<Verse> {
        val basePath = "bible/$language/${book.code}.json"
        // O empacotador pode remover .gz e disponibilizar JSON já descomprimido.
        val input = try {
            openAsset("$basePath.gz")
        } catch (_: FileNotFoundException) {
            openAsset(basePath)
        }
        return input.buffered().use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            // Detecta gzip pelo cabeçalho, em vez de depender da extensão.
            val decoded = if (first == 0x1f && second == 0x8b) {
                GZIPInputStream(buffered)
            } else {
                buffered
            }
            decoded.bufferedReader(Charsets.UTF_8).use { reader ->
                read(reader, book, number, language)
            }
        }
    }

    fun read(reader: Reader, book: BibleBook, number: Int, language: String): List<Verse> {
        val root = JsonParser.parseReader(reader).asJsonObject
        val translation = if (language == "pt") "por_blj" else "eng_kjv"
        check(root.get("schema")?.asInt == 1)
        check(root.getAsJsonObject("translation")?.get("id")?.asString == translation)
        check(root.getAsJsonObject("book")?.get("id")?.asString == book.code)
        check(root.getAsJsonObject("book")?.get("numberOfChapters")?.asInt == book.chapters)
        val chapter = requireNotNull(
            root.getAsJsonObject("chapters")?.getAsJsonObject(number.toString())
        ) { "Missing bundled chapter ${book.code}/$number ($language)" }
        val verses = BibleChapterParser.parse(chapter, book.nameEn, number, language)
        check(verses.isNotEmpty())
        check(verses.size == chapter.get("numberOfVerses")?.asInt)
        return verses
    }
}
