package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "payments",
    primaryKeys = ["memberId", "monthIndex"]
)
data class PaymentRecord(
    val memberId: Long,
    val monthIndex: Int, // 0 for JAN ... 11 for DEC
    val isPaid: Boolean,
    val isPending: Boolean,
    val heldBy: String = "Main",
    val amount: Double,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis()
)
