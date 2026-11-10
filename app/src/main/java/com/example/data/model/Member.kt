package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emirateOrCity: String,
    val country: String,
    val mobile: String,
    val allottedMonth: String, // "ALL", "JAN", "FEB", etc.
    val amount: Double,
    val currency: String // "AED" or "INR"
)
