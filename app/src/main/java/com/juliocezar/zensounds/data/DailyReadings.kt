package com.juliocezar.zensounds.data

import java.time.LocalDate
import kotlin.random.Random

data class DailyReading(
    val bookCode: String,
    val chapter: Int,
    val verseNumber: Int
)

object DailyReadings {
    // Seleção editorial: esperança, paz, gratidão, bondade e encorajamento.
    // Somente referências. Os textos vêm integralmente da tradução bíblica.
    val entries: List<DailyReading> = """
        GEN 1:31
        GEN 28:15
        GEN 50:20
        EXO 14:14
        DEU 31:6
        DEU 31:8
        JOS 1:9
        RUT 2:12
        NEH 8:10
        JOB 19:25
        PSA 4:8
        PSA 16:11
        PSA 23:1
        PSA 23:4
        PSA 23:6
        PSA 27:1
        PSA 27:14
        PSA 30:5
        PSA 34:8
        PSA 34:18
        PSA 37:5
        PSA 46:1
        PSA 46:10
        PSA 55:22
        PSA 62:1
        PSA 62:5
        PSA 91:2
        PSA 94:19
        PSA 100:5
        PSA 103:8
        PSA 118:24
        PSA 119:114
        PSA 121:2
        PSA 139:14
        PSA 145:18
        PSA 147:3
        PRO 3:5
        PRO 3:6
        PRO 12:25
        PRO 15:1
        PRO 16:3
        PRO 17:17
        PRO 17:22
        PRO 18:10
        ECC 3:11
        ECC 4:9
        ECC 4:10
        ISA 26:3
        ISA 40:29
        ISA 40:31
        ISA 41:10
        ISA 43:2
        ISA 49:15
        ISA 54:10
        ISA 58:11
        JER 17:7
        JER 29:11
        JER 31:3
        LAM 3:22
        LAM 3:23
        LAM 3:25
        MIC 6:8
        NAM 1:7
        HAB 3:18
        HAB 2:3
        ZEP 3:17
        MAT 5:9
        MAT 6:26
        MAT 6:34
        MAT 7:7
        MAT 11:28
        MAT 11:29
        MAT 28:20
        MRK 10:27
        LUK 1:37
        LUK 6:31
        LUK 12:32
        JHN 3:16
        JHN 8:12
        JHN 14:1
        JHN 14:27
        JHN 15:9
        JHN 15:12
        JHN 16:33
        ROM 5:5
        ROM 8:28
        ROM 8:37
        ROM 8:31
        ROM 12:12
        ROM 12:18
        ROM 15:13
        1CO 13:4
        1CO 15:58
        1CO 13:13
        1CO 16:14
        2CO 1:3
        2CO 4:16
        2CO 5:17
        2CO 12:9
        GAL 5:13
        GAL 6:9
        EPH 2:10
        EPH 5:2
        EPH 4:32
        PHP 1:6
        PHP 4:4
        PHP 4:6
        PHP 4:7
        PHP 4:8
        PHP 4:13
        COL 3:12
        COL 3:14
        COL 3:15
        1TH 5:11
        1TH 5:16
        1TH 5:17
        1TH 5:18
        2TH 3:16
        2TI 1:7
        HEB 4:16
        HEB 10:23
        HEB 13:5
        HEB 13:6
        JAS 1:5
        JAS 1:17
        1PE 4:8
        1PE 5:7
        1JN 3:1
        1JN 4:7
        1JN 4:16
        1JN 4:19
        REV 21:4
    """.trimIndent().lineSequence().filter { it.isNotBlank() }.map { line ->
        val (code, position) = line.trim().split(" ")
        val (chapter, verse) = position.split(":").map(String::toInt)
        DailyReading(code, chapter, verse)
    }.toList().shuffled(Random(104729))

    fun forDate(date: LocalDate): DailyReading {
        val index = Math.floorMod(date.toEpochDay(), entries.size.toLong()).toInt()
        return entries[index]
    }
}
