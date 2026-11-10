package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val holderName: String,
    val currency: String,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)
