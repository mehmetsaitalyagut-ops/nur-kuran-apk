package com.example

import com.example.data.local.LastListenedProgress
import com.example.model.QuranCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun lastListenedProgress_defaultsAreValid() {
        val progress = LastListenedProgress()
        val surah = QuranCatalog.surahByNumber(progress.surahNumber)
        assertEquals(1, surah.number)
        assertEquals("Fâtiha", progress.surahNameTr)
        assertEquals(1, progress.ayahNumber)
    }

    @Test
    fun buildEveryAyahUrl_formatsAllRecitersCorrectly() {
        QuranCatalog.reciters.forEach { reciter ->
            val url = QuranCatalog.buildEveryAyahUrl(reciter, 1, 1)
            assertEquals("https://everyayah.com/data/${reciter.everyAyahFolder}/001001.mp3", url)
        }
    }
}
