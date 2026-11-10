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
}
