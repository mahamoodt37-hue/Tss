package com.example.data.repository

import com.example.data.dao.ExtraCollectionDao
import com.example.data.dao.MemberDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.TransferDao
import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TssRepository(
    private val memberDao: MemberDao,
    private val paymentDao: PaymentDao,
    private val transferDao: TransferDao,
    private val extraCollectionDao: ExtraCollectionDao
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val allPayments: Flow<List<PaymentRecord>> = paymentDao.getAllPayments()
    val allTransfers: Flow<List<TransferRecord>> = transferDao.getAllTransfers()
    val allExtraCollections: Flow<List<ExtraCollection>> = extraCollectionDao.getAllExtraCollections()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun insertMember(member: Member): Long = withContext(Dispatchers.IO) {
        memberDao.insertMember(member)
    }

    suspend fun updateMember(member: Member) = withContext(Dispatchers.IO) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(memberId: Long) = withContext(Dispatchers.IO) {
        memberDao.deleteById(memberId)
        paymentDao.deletePaymentsForMember(memberId)
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
        if (!isPaid && !isPending) {
            paymentDao.deletePayment(memberId, monthIndex)
        } else {
            val record = PaymentRecord(
                memberId = memberId,
                monthIndex = monthIndex,
                isPaid = isPaid,
                isPending = isPending,
                heldBy = heldBy.ifBlank { "Main" },
                amount = amount,
                currency = currency
            )
            paymentDao.upsertPayment(record)
        }
    }

    suspend fun addTransfer(holderName: String, currency: String, amount: Double, date: String) =
        withContext(Dispatchers.IO) {
            val transfer = TransferRecord(
                holderName = holderName,
                currency = currency,
                amount = amount,
                date = date
            )
            transferDao.insertTransfer(transfer)
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
        val extra = ExtraCollection(
            name = name,
            mobile = mobile,
            purpose = purpose,
            amount = amount,
            currency = currency,
            heldBy = heldBy.ifBlank { "Main" },
            date = date
        )
        extraCollectionDao.insertExtraCollection(extra)
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
