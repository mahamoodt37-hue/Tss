package com.example

import com.example.data.db.SeedData
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.ui.viewmodel.CurrencyBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `seed data provides all 55 community members`() {
        val members = SeedData.getInitialMembers()
        assertEquals(55, members.size)
        val aedCount = members.count { it.currency == "AED" }
        val inrCount = members.count { it.currency == "INR" }
        assertEquals(39, aedCount)
        assertEquals(16, inrCount)
        assertTrue(members.any { it.name == "Azeez KC" && it.allottedMonth == "ALL" && it.amount == 250.0 })
        assertTrue(members.any { it.name == "Salman UK" && it.currency == "INR" && it.amount == 2000.0 })
    }

    @Test
    fun `member month can be changed cleanly`() {
        val member = Member(
            id = 101L,
            name = "Test Member",
            emirateOrCity = "Dubai",
            country = "UAE",
            mobile = "501234567",
            allottedMonth = "NOV",
            amount = 250.0,
            currency = "AED"
        )
        val updatedMember = member.copy(allottedMonth = "DEC")
        assertEquals("DEC", updatedMember.allottedMonth)
    }

    @Test
    fun `paid payment increases total balance`() {
        var balance = CurrencyBalance(mainAccount = 0.0, cashWithOthers = 0.0)
        assertEquals(0.0, balance.total, 0.001)

        val payment = PaymentRecord(
            memberId = 1L,
            monthIndex = 10,
            isPaid = true,
            isPending = false,
            heldBy = "Main",
            amount = 250.0,
            currency = "AED"
        )

        if (payment.isPaid) {
            balance = balance.copy(mainAccount = balance.mainAccount + payment.amount)
        }
        assertEquals(250.0, balance.total, 0.001)
    }

    @Test
    fun `contribution history correctly tracks 12 months and computes stats`() {
        val member = Member(
            id = 1L,
            name = "Azeez KC",
            emirateOrCity = "Al Ain",
            country = "UAE",
            mobile = "507607968",
            allottedMonth = "ALL",
            amount = 250.0,
            currency = "AED"
        )

        val payments = listOf(
            PaymentRecord(memberId = 1L, monthIndex = 0, isPaid = true, isPending = false, amount = 250.0, currency = "AED"),
            PaymentRecord(memberId = 1L, monthIndex = 1, isPaid = true, isPending = false, amount = 250.0, currency = "AED"),
            PaymentRecord(memberId = 1L, monthIndex = 2, isPaid = true, isPending = false, amount = 250.0, currency = "AED")
        )

        val memberPayments = payments.filter { it.memberId == member.id }.associateBy { it.monthIndex }
        assertEquals(3, memberPayments.size)
        val paidAmount = memberPayments.values.filter { it.isPaid }.sumOf { it.amount }
        assertEquals(750.0, paidAmount, 0.001)

        val totalExpected = if (member.allottedMonth == "ALL") member.amount * 12 else member.amount
        assertEquals(3000.0, totalExpected, 0.001)
        assertEquals(2250.0, totalExpected - paidAmount, 0.001)
    }
}
