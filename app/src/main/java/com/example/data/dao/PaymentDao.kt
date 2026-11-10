package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE monthIndex = :monthIndex")
    fun getPaymentsForMonth(monthIndex: Int): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId AND monthIndex = :monthIndex LIMIT 1")
    suspend fun getPayment(memberId: Long, monthIndex: Int): PaymentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: PaymentRecord)

    @Query("DELETE FROM payments WHERE memberId = :memberId AND monthIndex = :monthIndex")
    suspend fun deletePayment(memberId: Long, monthIndex: Int)

    @Query("DELETE FROM payments WHERE memberId = :memberId")
    suspend fun deletePaymentsForMember(memberId: Long)
}
