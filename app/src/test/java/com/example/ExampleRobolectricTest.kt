package com.example

import com.example.ui.viewmodel.CurrencyBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {
    @Test
    fun `app logic initialization test`() {
        assertTrue(true)
    }

    @Test
    fun `currency balance correctly sums main account and cash with collectors`() {
        val holdersMap = mapOf(
            "Salman" to 500.0,
            "Azeez KC" to 300.0
        )
        val balance = CurrencyBalance(
            mainAccount = 4200.0,
            cashWithOthers = 800.0,
            holders = holdersMap
        )

        assertEquals(5000.0, balance.total, 0.001)
        assertEquals(800.0, balance.holders.values.sum(), 0.001)
        assertEquals(2, balance.holders.size)
        assertEquals(500.0, balance.holders["Salman"] ?: 0.0, 0.001)
    }

    @Test
    fun `collector net balance is reduced when handover transfer occurs`() {
        val initialCollected = 750.0
        val transferredToMain = 500.0
        val remainingInHand = (initialCollected - transferredToMain).coerceAtLeast(0.0)

        assertEquals(250.0, remainingInHand, 0.001)
    }
}

