package com.example.ui.viewmodel

import com.example.data.model.Member
import com.example.data.model.PaymentRecord

val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
const val CURRENT_MONTH_INDEX = 10 // November (0-based) for 2026 fund cycle

enum class MemberPaymentStatus(val label: String) {
    PAID("Paid"),
    IN_PROGRESS("In Progress"),
    PENDING("Pending"),
    UNPAID("Unpaid"),
    NOT_DUE("Not Due")
}

data class MemberWithStatus(
    val member: Member,
    val status: MemberPaymentStatus,
    val heldBy: String = "Main",
    val isDueThisMonth: Boolean = false
)

data class CurrencyBalance(
    val mainAccount: Double = 0.0,
    val cashWithOthers: Double = 0.0,
    val holders: Map<String, Double> = emptyMap()
) {
    val total: Double get() = mainAccount + cashWithOthers
}

data class MonthCollectionStats(
    val paidCount: Int = 0,
    val inProgressCount: Int = 0,
    val pendingCount: Int = 0,
    val unpaidCount: Int = 0,
    val totalExpected: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalDueMembers: Int = 0
)

data class TssUiState(
    val selectedCurrency: String = "AED", // "AED" or "INR"
    val selectedMonthIndex: Int = CURRENT_MONTH_INDEX,
    val currentTab: String = "dashboard", // "dashboard", "members", "transfers", "extra"
    val searchQuery: String = "",
    val filterStatus: String = "ALL", // "ALL", "PAID", "PENDING", "UNPAID", "IN_PROGRESS"
    val showAllMonthsMembers: Boolean = false,
    val aedBalance: CurrencyBalance = CurrencyBalance(),
    val inrBalance: CurrencyBalance = CurrencyBalance(),
    val monthStats: MonthCollectionStats = MonthCollectionStats(),
    val membersWithStatus: List<MemberWithStatus> = emptyList(),
    val allPayments: List<PaymentRecord> = emptyList(),
    val knownHolders: List<String> = listOf("Main"),
    val googleSheetUrl: String = "",
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null
)
