package com.example.data.firebase

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = FirebaseAuth.getInstance().currentUser
    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@Keep
@IgnoreExtraProperties
data class UserPreferencesDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val selectedReciterId: String = "ar.alafasy",
    val darkTheme: Boolean = true,
    val dailyReadGoalAyahs: Long = 20L,
    val dailyListenGoalMinutes: Long = 15L,
    val lastSurahNumber: Long = 1L,
    val lastSurahNameTr: String = "Fâtiha",
    val lastAyahNumber: Long = 1L,
    val lastReciterId: String = "ar.alafasy",
    val lastPlaybackPositionMs: Long = 0L,
    val completedJuzs: List<Long> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteItemDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "surah",
    val surahNumber: Long = 1L,
    val surahNameTr: String = "",
    val surahNameAr: String = "",
    val ayahNumber: Long = 0L,
    val arabicText: String? = null,
    val turkishText: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ListeningHistoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val surahNumber: Long = 1L,
    val surahNameTr: String = "",
    val surahNameAr: String = "",
    val reciterId: String = "ar.alafasy",
    val reciterName: String = "",
    val ayahNumber: Long = 1L,
    val listenCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyIbadatDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val ayahsRead: Long = 0L,
    val listenedSeconds: Long = 0L,
    val completedDailyGoal: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
