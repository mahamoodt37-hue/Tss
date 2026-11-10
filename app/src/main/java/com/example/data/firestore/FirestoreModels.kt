package com.example.data.firestore

import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreMember(
    @DocumentId val docId: String = "",
    val id: Long = 0L,
    val name: String = "",
    val emirateOrCity: String = "",
    val country: String = "",
    val mobile: String = "",
    val allottedMonth: String = "ALL",
    val amount: Double = 0.0,
    val currency: String = "AED"
) {
    fun toDomainModel(): Member = Member(
        id = id,
        name = name,
        emirateOrCity = emirateOrCity,
        country = country,
        mobile = mobile,
        allottedMonth = allottedMonth,
        amount = amount,
        currency = currency
    )

    companion object {
        fun fromDomain(member: Member): FirestoreMember = FirestoreMember(
            docId = "member_${member.id}",
            id = member.id,
            name = member.name,
            emirateOrCity = member.emirateOrCity,
            country = member.country,
            mobile = member.mobile,
            allottedMonth = member.allottedMonth,
            amount = member.amount,
            currency = member.currency
        )
    }
}

@IgnoreExtraProperties
data class FirestorePayment(
    @DocumentId val docId: String = "",
    val memberId: Long = 0L,
    val monthIndex: Int = 0,
    val isPaid: Boolean = false,
    val isPending: Boolean = false,
    val heldBy: String = "Main",
    val amount: Double = 0.0,
    val currency: String = "AED",
    val updatedBy: String = "",
    val date: String = "",
    val timestamp: Long = 0L
) {
    fun toDomainModel(): PaymentRecord = PaymentRecord(
        memberId = memberId,
        monthIndex = monthIndex,
        isPaid = isPaid,
        isPending = isPending,
        heldBy = heldBy,
        amount = amount,
        currency = currency,
        timestamp = if (timestamp > 0L) timestamp else System.currentTimeMillis()
    )
}

@IgnoreExtraProperties
data class FirestoreTransfer(
    @DocumentId val docId: String = "",
    val holderName: String = "",
    val currency: String = "AED",
    val amount: Double = 0.0,
    val date: String = "",
    val createdBy: String = ""
) {
    fun toDomainModel(): TransferRecord = TransferRecord(
        id = 0,
        holderName = holderName,
        currency = currency,
        amount = amount,
        date = date
    )
}

@IgnoreExtraProperties
data class FirestoreExtra(
    @DocumentId val docId: String = "",
    val name: String = "",
    val mobile: String = "",
    val purpose: String = "",
    val amount: Double = 0.0,
    val currency: String = "AED",
    val heldBy: String = "Main",
    val date: String = "",
    val createdBy: String = ""
) {
    fun toDomainModel(): ExtraCollection = ExtraCollection(
        id = 0,
        name = name,
        mobile = mobile,
        purpose = purpose,
        amount = amount,
        currency = currency,
        heldBy = heldBy,
        date = date
    )
}
