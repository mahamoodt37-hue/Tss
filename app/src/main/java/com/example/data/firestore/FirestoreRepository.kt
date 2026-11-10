package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = Firebase.auth
    val currentUser = auth.currentUser

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

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val context: Context
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context
    )

    private val auth = Firebase.auth

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeMembers(): Flow<List<Member>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "members"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestoreMember::class.java)?.toDomainModel()
                        } catch (e: Exception) {
                            Log.e("FirestoreRepo", "Failed to deserialize member ${doc.id}", e)
                            null
                        }
                    }.sortedBy { it.id }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePayments(): Flow<List<PaymentRecord>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "payments"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestorePayment::class.java)?.toDomainModel()
                        } catch (e: Exception) {
                            Log.e("FirestoreRepo", "Failed to deserialize payment ${doc.id}", e)
                            null
                        }
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransfers(): Flow<List<TransferRecord>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "transfers"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestoreTransfer::class.java)?.toDomainModel()
                        } catch (e: Exception) {
                            Log.e("FirestoreRepo", "Failed to deserialize transfer ${doc.id}", e)
                            null
                        }
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeExtraCollections(): Flow<List<ExtraCollection>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "extra_collections"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(FirestoreExtra::class.java)?.toDomainModel()
                        } catch (e: Exception) {
                            Log.e("FirestoreRepo", "Failed to deserialize extra collection ${doc.id}", e)
                            null
                        }
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedInitialMembersIfEmpty(initialMembers: List<Member>) = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        val path = "members"
        try {
            val snapshot = db.collection(path).limit(1).get().await()
            if (snapshot.isEmpty && initialMembers.isNotEmpty()) {
                Log.d("FirestoreRepo", "Seeding initial ${initialMembers.size} members to cloud Firestore...")
                val batch = db.batch()
                for (member in initialMembers) {
                    val docRef = db.collection(path).document("member_${member.id}")
                    val model = FirestoreMember.fromDomain(member)
                    val map = mapOf(
                        "id" to model.id,
                        "name" to model.name,
                        "emirateOrCity" to model.emirateOrCity,
                        "country" to model.country,
                        "mobile" to model.mobile,
                        "allottedMonth" to model.allottedMonth,
                        "amount" to model.amount,
                        "currency" to model.currency
                    )
                    batch.set(docRef, map)
                }
                batch.commit().await()
                Log.d("FirestoreRepo", "Cloud seeding complete.")
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$path/seed")
        }
    }

    suspend fun saveMember(member: Member): Long = withContext(Dispatchers.IO) {
        requireUserId()
        val memberId = if (member.id <= 0L) System.currentTimeMillis() else member.id
        val memberToSave = member.copy(id = memberId)
        val docRef = db.collection("members").document("member_$memberId")
        val payload = mapOf(
            "id" to memberToSave.id,
            "name" to memberToSave.name.trim(),
            "emirateOrCity" to memberToSave.emirateOrCity.trim(),
            "country" to memberToSave.country.trim(),
            "mobile" to memberToSave.mobile.trim(),
            "allottedMonth" to memberToSave.allottedMonth.trim().uppercase(),
            "amount" to memberToSave.amount,
            "currency" to memberToSave.currency.trim().uppercase()
        )
        try {
            docRef.set(payload).await()
            memberId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMember(memberId: Long) = withContext(Dispatchers.IO) {
        requireUserId()
        val docRef = db.collection("members").document("member_$memberId")
        try {
            docRef.delete().await()
            // Also cleanup payment records for this member
            val paymentsSnapshot = db.collection("payments")
                .whereEqualTo("memberId", memberId)
                .get()
                .await()
            val batch = db.batch()
            for (doc in paymentsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun setPaymentStatus(
        memberId: Long,
        monthIndex: Int,
        isPaid: Boolean,
        isPending: Boolean,
        heldBy: String,
        amount: Double,
        currency: String
    ) = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val docId = "${memberId}_$monthIndex"
        val docRef = db.collection("payments").document(docId)

        if (!isPaid && !isPending) {
            try {
                docRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                throw e
            }
        } else {
            val payload = mapOf(
                "memberId" to memberId,
                "monthIndex" to monthIndex,
                "isPaid" to isPaid,
                "isPending" to isPending,
                "heldBy" to heldBy.trim().ifBlank { "Main" },
                "amount" to amount,
                "currency" to currency.trim().uppercase(),
                "updatedBy" to (auth.currentUser?.email ?: uid)
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                throw e
            }
        }
    }

    suspend fun addTransfer(
        holderName: String,
        currency: String,
        amount: Double,
        date: String
    ) = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val docId = "transfer_${System.currentTimeMillis()}"
        val docRef = db.collection("transfers").document(docId)
        val payload = mapOf(
            "holderName" to holderName.trim(),
            "currency" to currency.trim().uppercase(),
            "amount" to amount,
            "date" to date.trim(),
            "createdBy" to (auth.currentUser?.email ?: uid)
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addExtraCollection(
        name: String,
        mobile: String,
        purpose: String,
        amount: Double,
        currency: String,
        heldBy: String,
        date: String
    ) = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val docId = "extra_${System.currentTimeMillis()}"
        val docRef = db.collection("extra_collections").document(docId)
        val payload = mapOf(
            "name" to name.trim(),
            "mobile" to mobile.trim(),
            "purpose" to purpose.trim(),
            "amount" to amount,
            "currency" to currency.trim().uppercase(),
            "heldBy" to heldBy.trim().ifBlank { "Main" },
            "date" to date.trim(),
            "createdBy" to (auth.currentUser?.email ?: uid)
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun syncWithSheetWebhook(webhookUrl: String, jsonPayload: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val mediaType = "text/plain".toMediaType()
                val body = jsonPayload.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("Synced successfully with Google Sheet")
                } else {
                    Result.failure(Exception("Sync failed: HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
