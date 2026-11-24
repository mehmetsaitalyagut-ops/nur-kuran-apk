package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.model.Ayah
import com.example.model.Reciter
import com.example.model.SurahInfo
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDataRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Devam etmek için Google ile giriş yapmalısınız.")
    }

    private fun userRoot(userId: String) = db.collection("users").document(userId)

    fun observePreferences(userId: String): Flow<UserPreferencesDoc> = flow {
        val path = "users/$userId/settings/preferences"
        emitAll(
            userRoot(userId).collection("settings").document("preferences")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserPreferencesDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserPreferencesDoc(userId = userId)
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteItemDoc>> = flow {
        val path = "users/$userId/favorites"
        emitAll(
            userRoot(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteItemDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeHistory(userId: String): Flow<List<ListeningHistoryDoc>> = flow {
        val path = "users/$userId/history"
        emitAll(
            userRoot(userId).collection("history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ListeningHistoryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeDailyIbadat(userId: String): Flow<List<DailyIbadatDoc>> = flow {
        val path = "users/$userId/daily_ibadat"
        emitAll(
            userRoot(userId).collection("daily_ibadat")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyIbadatDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    @Volatile
    private var ensuredUserId: String? = null

    suspend fun ensureUserPreferences(userId: String): Result<Unit> {
        val docRef = userRoot(userId).collection("settings").document("preferences")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Kullanıcı oturumu değişti." }
            if (ensuredUserId == uid) {
                return Result.success(Unit)
            }
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "selectedReciterId" to "ar.alafasy",
                    "darkTheme" to true,
                    "dailyReadGoalAyahs" to 20L,
                    "dailyListenGoalMinutes" to 15L,
                    "lastSurahNumber" to 1L,
                    "lastSurahNameTr" to "Fâtiha",
                    "lastAyahNumber" to 1L,
                    "lastReciterId" to "ar.alafasy",
                    "lastPlaybackPositionMs" to 0L,
                    "completedJuzs" to emptyList<Long>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            ensuredUserId = uid
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun updatePreferencesFields(userId: String, fields: Map<String, Any>): Result<Unit> {
        val docRef = userRoot(userId).collection("settings").document("preferences")
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Kullanıcı oturumu değişti." }
            ensureUserPreferences(uid).getOrThrow()
            val updateMap = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                docRef.update(updateMap).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                    ensuredUserId = null
                    ensureUserPreferences(uid).getOrThrow()
                    docRef.update(updateMap).await()
                } else {
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSelectedReciter(userId: String, reciterId: String): Result<Unit> {
        require(reciterId.isNotBlank() && reciterId.length <= 64)
        return updatePreferencesFields(userId, mapOf("selectedReciterId" to reciterId))
    }

    suspend fun updateTheme(userId: String, darkTheme: Boolean): Result<Unit> {
        return updatePreferencesFields(userId, mapOf("darkTheme" to darkTheme))
    }

    suspend fun updateGoals(userId: String, readGoalAyahs: Long, listenGoalMinutes: Long): Result<Unit> {
        val clampedRead = readGoalAyahs.coerceIn(1L, 1000L)
        val clampedListen = listenGoalMinutes.coerceIn(1L, 600L)
        return updatePreferencesFields(
            userId,
            mapOf(
                "dailyReadGoalAyahs" to clampedRead,
                "dailyListenGoalMinutes" to clampedListen
            )
        )
    }

    suspend fun updateLastProgress(
        userId: String,
        surahNumber: Int,
        surahNameTr: String,
        ayahNumber: Int,
        reciterId: String,
        positionMs: Long
    ): Result<Unit> {
        return updatePreferencesFields(
            userId,
            mapOf(
                "lastSurahNumber" to surahNumber.coerceIn(1, 114).toLong(),
                "lastSurahNameTr" to surahNameTr.take(100).ifBlank { "Fâtiha" },
                "lastAyahNumber" to ayahNumber.coerceIn(1, 286).toLong(),
                "lastReciterId" to reciterId.take(64).ifBlank { "ar.alafasy" },
                "lastPlaybackPositionMs" to positionMs.coerceAtLeast(0L)
            )
        )
    }

    suspend fun toggleJuzCompleted(userId: String, juzNumber: Int, currentList: List<Long>): Result<Unit> {
        val juz = juzNumber.toLong()
        require(juz in 1L..30L)
        val updated = if (currentList.contains(juz)) {
            currentList.filterNot { it == juz }
        } else {
            (currentList + juz).distinct().sorted().take(30)
        }
        return updatePreferencesFields(userId, mapOf("completedJuzs" to updated))
    }

    suspend fun toggleFavoriteSurah(userId: String, surah: SurahInfo, isCurrentlyFav: Boolean): Result<Unit> {
        val docId = "surah_${surah.number}"
        val docRef = userRoot(userId).collection("favorites").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            if (isCurrentlyFav) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "kind" to "surah",
                    "surahNumber" to surah.number.toLong(),
                    "surahNameTr" to surah.nameTr.take(100),
                    "surahNameAr" to surah.nameAr.take(100),
                    "ayahNumber" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCurrentlyFav) OperationType.DELETE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteAyah(
        userId: String,
        surah: SurahInfo,
        ayah: Ayah,
        isCurrentlyFav: Boolean
    ): Result<Unit> {
        val docId = "ayah_${surah.number}_${ayah.numberInSurah}"
        val docRef = userRoot(userId).collection("favorites").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            if (isCurrentlyFav) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "kind" to "ayah",
                    "surahNumber" to surah.number.toLong(),
                    "surahNameTr" to surah.nameTr.take(100),
                    "surahNameAr" to surah.nameAr.take(100),
                    "ayahNumber" to ayah.numberInSurah.toLong(),
                    "arabicText" to ayah.arabicText.take(3900),
                    "turkishText" to ayah.turkishText.take(3900),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCurrentlyFav) OperationType.DELETE else OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFavoriteById(userId: String, favId: String): Result<Unit> {
        val docRef = userRoot(userId).collection("favorites").document(favId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(favId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordListeningHistory(
        userId: String,
        surah: SurahInfo,
        reciter: Reciter,
        ayahNumber: Int
    ): Result<Unit> {
        val docId = "surah_${surah.number}"
        val docRef = userRoot(userId).collection("history").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "reciterId" to reciter.id.take(64),
                        "reciterName" to reciter.nameTr.take(100),
                        "ayahNumber" to ayahNumber.coerceIn(1, 286).toLong(),
                        "listenCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "surahNumber" to surah.number.toLong(),
                        "surahNameTr" to surah.nameTr.take(100),
                        "surahNameAr" to surah.nameAr.take(100),
                        "reciterId" to reciter.id.take(64),
                        "reciterName" to reciter.nameTr.take(100),
                        "ayahNumber" to ayahNumber.coerceIn(1, 286).toLong(),
                        "listenCount" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateListeningHistoryAyah(
        userId: String,
        surah: SurahInfo,
        reciter: Reciter,
        ayahNumber: Int
    ): Result<Unit> {
        val docId = "surah_${surah.number}"
        val docRef = userRoot(userId).collection("history").document(docId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            try {
                docRef.update(
                    mapOf(
                        "reciterId" to reciter.id.take(64),
                        "reciterName" to reciter.nameTr.take(100),
                        "ayahNumber" to ayahNumber.coerceIn(1, 286).toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "surahNumber" to surah.number.toLong(),
                            "surahNameTr" to surah.nameTr.take(100),
                            "surahNameAr" to surah.nameAr.take(100),
                            "reciterId" to reciter.id.take(64),
                            "reciterName" to reciter.nameTr.take(100),
                            "ayahNumber" to ayahNumber.coerceIn(1, 286).toLong(),
                            "listenCount" to 1L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordDailyProgress(
        userId: String,
        ayahsDelta: Long,
        secondsDelta: Long,
        readGoalAyahs: Long,
        listenGoalMinutes: Long
    ): Result<Unit> {
        val dateKey = todayKey()
        val docRef = userRoot(userId).collection("daily_ibadat").document(dateKey)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val snap = docRef.get().await()
            val currentRead = snap.getLong("ayahsRead") ?: 0L
            val currentSec = snap.getLong("listenedSeconds") ?: 0L
            val nextRead = (currentRead + ayahsDelta).coerceIn(0L, 10000L)
            val nextSec = (currentSec + secondsDelta).coerceIn(0L, 86400L)
            val completed = nextRead >= readGoalAyahs || nextSec >= (listenGoalMinutes * 60L)

            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "ayahsRead" to nextRead,
                        "listenedSeconds" to nextSec,
                        "completedDailyGoal" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "ayahsRead" to nextRead,
                        "listenedSeconds" to nextSec,
                        "completedDailyGoal" to completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun todayKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}
