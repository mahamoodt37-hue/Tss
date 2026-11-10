package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_collections")
data class ExtraCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String,
    val purpose: String,
    val amount: Double,
    val currency: String,
    val heldBy: String = "Main",
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)
