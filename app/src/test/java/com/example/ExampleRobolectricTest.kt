package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LastListenedProgress
import com.example.data.local.LastListenedStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NUR – Kur’an-ı Kerim", appName)
    }

    @Test
    fun `last listened store persists surah and ayah per user`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LastListenedStore(context)

        assertNull(store.load("user_a"))

        val saved = LastListenedProgress(
            surahNumber = 36,
            surahNameTr = "Yâsîn",
            ayahNumber = 42,
            reciterId = "ar.alafasy",
            positionMs = 3450L,
            updatedAtMs = 1700000000000L,
            hasSavedRecord = true,
            syncedToCloud = false
        )
        store.save("user_a", saved, synchronous = true)

        val restored = store.load("user_a")
        assertNotNull(restored)
        assertEquals(36, restored?.surahNumber)
        assertEquals("Yâsîn", restored?.surahNameTr)
        assertEquals(42, restored?.ayahNumber)
        assertEquals(3450L, restored?.positionMs)
        assertFalse(restored?.syncedToCloud ?: true)

        assertNull(store.load("user_b"))

        store.markSynced("user_a", 1700000000000L)
        val afterSync = store.load("user_a")
        assertTrue(afterSync?.syncedToCloud ?: false)
    }
}
