package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.QuranCatalog

data class LastListenedProgress(
    val surahNumber: Int = 1,
    val surahNameTr: String = "Fâtiha",
    val ayahNumber: Int = 1,
    val reciterId: String = QuranCatalog.reciters.first().id,
    val positionMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val hasSavedRecord: Boolean = false,
    val syncedToCloud: Boolean = true
)

class LastListenedStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(userId: String): LastListenedProgress? {
        if (userId.isBlank()) return null
        val hasRecord = prefs.getBoolean(key(userId, "has_record"), false)
        if (!hasRecord) return null
        val surahNumber = prefs.getInt(key(userId, "surah_num"), 1).coerceIn(1, 114)
        val surahInfo = QuranCatalog.surahByNumber(surahNumber)
        val surahNameTr = prefs.getString(key(userId, "surah_name"), surahInfo.nameTr)
            ?.takeIf { it.isNotBlank() }
            ?: surahInfo.nameTr
        val ayahNumber = prefs.getInt(key(userId, "ayah_num"), 1).coerceIn(1, surahInfo.ayahCount)
        val reciterId = prefs.getString(key(userId, "reciter_id"), QuranCatalog.reciters.first().id)
            ?.takeIf { it.isNotBlank() }
            ?: QuranCatalog.reciters.first().id
        val positionMs = prefs.getLong(key(userId, "pos_ms"), 0L).coerceAtLeast(0L)
        val updatedAtMs = prefs.getLong(key(userId, "updated_at_ms"), 0L).coerceAtLeast(0L)
        val synced = prefs.getBoolean(key(userId, "synced"), true)

        return LastListenedProgress(
            surahNumber = surahNumber,
            surahNameTr = surahNameTr,
            ayahNumber = ayahNumber,
            reciterId = reciterId,
            positionMs = positionMs,
            updatedAtMs = updatedAtMs,
            hasSavedRecord = true,
            syncedToCloud = synced
        )
    }

    fun save(
        userId: String,
        progress: LastListenedProgress,
        synchronous: Boolean = false
    ) {
        if (userId.isBlank()) return
        val editor = prefs.edit()
            .putBoolean(key(userId, "has_record"), progress.hasSavedRecord)
            .putInt(key(userId, "surah_num"), progress.surahNumber.coerceIn(1, 114))
            .putString(key(userId, "surah_name"), progress.surahNameTr)
            .putInt(key(userId, "ayah_num"), progress.ayahNumber.coerceAtLeast(1))
            .putString(key(userId, "reciter_id"), progress.reciterId)
            .putLong(key(userId, "pos_ms"), progress.positionMs.coerceAtLeast(0L))
            .putLong(key(userId, "updated_at_ms"), progress.updatedAtMs)
            .putBoolean(key(userId, "synced"), progress.syncedToCloud)
        if (synchronous) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    fun markSynced(userId: String, updatedAtMs: Long) {
        if (userId.isBlank()) return
        val currentUpdated = prefs.getLong(key(userId, "updated_at_ms"), 0L)
        if (currentUpdated <= updatedAtMs) {
            prefs.edit().putBoolean(key(userId, "synced"), true).apply()
        }
    }

    private fun key(userId: String, field: String): String = "u_${userId}_$field"

    private companion object {
        const val PREFS_NAME = "nur_last_listened_prefs"
    }
}
