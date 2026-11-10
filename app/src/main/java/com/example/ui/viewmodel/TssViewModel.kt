package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.SeedData
import com.example.data.firestore.FirestoreRepository
import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TssViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository(application)

    private val prefs = application.getSharedPreferences("tss_prefs", Context.MODE_PRIVATE)

    private val _selectedCurrency = MutableStateFlow("AED")
    private val _selectedMonthIndex = MutableStateFlow(CURRENT_MONTH_INDEX)
    private val _currentTab = MutableStateFlow("dashboard")
    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow("ALL")
    private val _showAllMonthsMembers = MutableStateFlow(false)
    private val _googleSheetUrl = MutableStateFlow(prefs.getString("sheet_url", "") ?: "")
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    init {
        // Seed initial 55 members to cloud if collection is currently empty
        viewModelScope.launch {
            try {
                firestoreRepository.seedInitialMembersIfEmpty(SeedData.getInitialMembers())
            } catch (e: Exception) {
                Log.w("TssViewModel", "Initial seed check: ${e.message}")
            }
        }
    }

    val allTransfers: StateFlow<List<TransferRecord>> = firestoreRepository.observeTransfers()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.e("TssViewModel", "Error observing transfers: ${firestoreError?.message ?: error.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtraCollections: StateFlow<List<ExtraCollection>> = firestoreRepository.observeExtraCollections()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.e("TssViewModel", "Error observing extra collections: ${firestoreError?.message ?: error.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TssUiState> = combine(
        combine(
            firestoreRepository.observeMembers().catch { emit(emptyList()) },
            firestoreRepository.observePayments().catch { emit(emptyList()) },
            firestoreRepository.observeTransfers().catch { emit(emptyList()) },
            firestoreRepository.observeExtraCollections().catch { emit(emptyList()) }
        ) { members, payments, transfers, extras ->
            DataBundle(members, payments, transfers, extras)
        },
        combine(
            _selectedCurrency,
            _selectedMonthIndex,
            _currentTab,
            _searchQuery,
            _filterStatus
        ) { cur, month, tab, query, filter ->
            FilterBundle(cur, month, tab, query, filter)
        },
        combine(
            _showAllMonthsMembers,
            _googleSheetUrl,
            _isSyncing,
            _syncMessage
        ) { allMonths, sheetUrl, syncing, message ->
            SyncBundle(allMonths, sheetUrl, syncing, message)
        }
    ) { data, filter, sync ->
        computeUiState(data, filter, sync)
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.e("TssViewModel", "Error computing UI state: ${firestoreError?.message ?: error.message}")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TssUiState()
    )

    private data class DataBundle(
        val members: List<Member>,
        val payments: List<PaymentRecord>,
        val transfers: List<TransferRecord>,
        val extras: List<ExtraCollection>
    )

    private data class FilterBundle(
        val currency: String,
        val monthIndex: Int,
        val tab: String,
        val query: String,
        val filterStatus: String
    )

    private data class SyncBundle(
        val showAllMonths: Boolean,
        val sheetUrl: String,
        val isSyncing: Boolean,
        val message: String?
    )

    private fun computeUiState(
        data: DataBundle,
        filter: FilterBundle,
        sync: SyncBundle
    ): TssUiState {
        val members = data.members
        val payments = data.payments
        val transfers = data.transfers
        val extras = data.extras

        // Map payments by (memberId|monthIndex)
        val paymentMap = payments.associateBy { "${it.memberId}|${it.monthIndex}" }
        val memberMap = members.associateBy { it.id }

        // Compute balances
        val col = mutableMapOf<String, MutableMap<String, Double>>()
        fun addCash(holder: String, cur: String, amount: Double) {
            val trimmed = holder.trim().ifBlank { "Main" }
            val existingKey = col.keys.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            val h = existingKey ?: trimmed
            val c = cur.trim().uppercase()
            val map = col.getOrPut(h) { mutableMapOf("AED" to 0.0, "INR" to 0.0) }
            map[c] = (map[c] ?: 0.0) + amount
        }

        // Add payments (only paid payments contribute to cash)
        for (p in payments) {
            if (p.isPaid) {
                val member = memberMap[p.memberId]
                val cur = (if (p.currency.isNotBlank()) p.currency else member?.currency ?: "AED").trim().uppercase()
                val amt = if (p.amount > 0.0) p.amount else (member?.amount ?: 0.0)
                addCash(p.heldBy, cur, amt)
            }
        }

        // Add extra collections
        for (x in extras) {
            addCash(x.heldBy, x.currency, x.amount)
        }

        // Process transfers
        for (t in transfers) {
            addCash(t.holderName, t.currency, -t.amount)
            addCash("Main", t.currency, t.amount)
        }

        fun extractBalance(cur: String): CurrencyBalance {
            var main = 0.0
            var oth = 0.0
            val holders = mutableMapOf<String, Double>()
            val targetCur = cur.trim().uppercase()

            for ((holder, currencies) in col) {
                val v = currencies[targetCur] ?: 0.0
                if (holder.equals("Main", ignoreCase = true)) {
                    main += v
                } else if (kotlin.math.abs(v) > 0.001) {
                    oth += v
                    holders[holder] = v
                }
            }
            return CurrencyBalance(
                mainAccount = main,
                cashWithOthers = oth,
                holders = holders
            )
        }

        val aedBal = extractBalance("AED")
        val inrBal = extractBalance("INR")

        // Known cash holders for auto-complete
        val holderSet = sortedSetOf("Main")
        holderSet.addAll(aedBal.holders.keys)
        holderSet.addAll(inrBal.holders.keys)
        extras.forEach { if (it.heldBy.isNotBlank()) holderSet.add(it.heldBy) }
        payments.forEach { if (it.heldBy.isNotBlank()) holderSet.add(it.heldBy) }

        // Month stats for selected currency & month
        val currentMonthName = MONTHS.getOrElse(filter.monthIndex) { "NOV" }
        val dueMembers = members.filter {
            it.currency == filter.currency &&
                    (it.allottedMonth.equals("ALL", ignoreCase = true) ||
                            it.allottedMonth.equals(currentMonthName, ignoreCase = true))
        }

        var paidCount = 0
        var inProgressCount = 0
        var pendingCount = 0
        var unpaidCount = 0
        var expectedAmount = 0.0
        var collectedAmount = 0.0

        for (m in dueMembers) {
            expectedAmount += m.amount
            val p = paymentMap["${m.id}|${filter.monthIndex}"]
            val status = when {
                p?.isPaid == true -> MemberPaymentStatus.PAID
                p?.isPending == true -> MemberPaymentStatus.PENDING
                filter.monthIndex < CURRENT_MONTH_INDEX -> MemberPaymentStatus.UNPAID
                filter.monthIndex == CURRENT_MONTH_INDEX -> MemberPaymentStatus.IN_PROGRESS
                else -> MemberPaymentStatus.PENDING
            }

            when (status) {
                MemberPaymentStatus.PAID -> {
                    paidCount++
                    collectedAmount += m.amount
                }
                MemberPaymentStatus.IN_PROGRESS -> inProgressCount++
                MemberPaymentStatus.PENDING -> pendingCount++
                MemberPaymentStatus.UNPAID -> unpaidCount++
                MemberPaymentStatus.NOT_DUE -> {}
            }
        }

        val monthStats = MonthCollectionStats(
            paidCount = paidCount,
            inProgressCount = inProgressCount,
            pendingCount = pendingCount,
            unpaidCount = unpaidCount,
            totalExpected = expectedAmount,
            totalCollected = collectedAmount,
            totalDueMembers = dueMembers.size
        )

        // Filter and map members with status
        val filteredMembers = members.filter { m ->
            m.currency == filter.currency &&
                    (sync.showAllMonths || filter.query.isNotBlank() ||
                            m.allottedMonth.equals("ALL", ignoreCase = true) ||
                            m.allottedMonth.equals(currentMonthName, ignoreCase = true)) &&
                    (filter.query.isBlank() ||
                            m.name.contains(filter.query, ignoreCase = true) ||
                            m.mobile.contains(filter.query, ignoreCase = true) ||
                            m.emirateOrCity.contains(filter.query, ignoreCase = true) ||
                            m.country.contains(filter.query, ignoreCase = true))
        }.map { m ->
            val p = paymentMap["${m.id}|${filter.monthIndex}"]
            val isDue = m.allottedMonth.equals("ALL", ignoreCase = true) ||
                    m.allottedMonth.equals(currentMonthName, ignoreCase = true)

            val status = when {
                p?.isPaid == true -> MemberPaymentStatus.PAID
                p?.isPending == true -> MemberPaymentStatus.PENDING
                !isDue -> MemberPaymentStatus.NOT_DUE
                filter.monthIndex < CURRENT_MONTH_INDEX -> MemberPaymentStatus.UNPAID
                filter.monthIndex == CURRENT_MONTH_INDEX -> MemberPaymentStatus.IN_PROGRESS
                else -> MemberPaymentStatus.PENDING
            }
            MemberWithStatus(
                member = m,
                status = status,
                heldBy = p?.heldBy ?: "Main",
                isDueThisMonth = isDue
            )
        }.filter {
            when (filter.filterStatus) {
                "PAID" -> it.status == MemberPaymentStatus.PAID
                "PENDING" -> it.status == MemberPaymentStatus.PENDING
                "UNPAID" -> it.status == MemberPaymentStatus.UNPAID
                "IN_PROGRESS" -> it.status == MemberPaymentStatus.IN_PROGRESS
                else -> true
            }
        }

        return TssUiState(
            selectedCurrency = filter.currency,
            selectedMonthIndex = filter.monthIndex,
            currentTab = filter.tab,
            searchQuery = filter.query,
            filterStatus = filter.filterStatus,
            showAllMonthsMembers = sync.showAllMonths,
            aedBalance = aedBal,
            inrBalance = inrBal,
            monthStats = monthStats,
            membersWithStatus = filteredMembers,
            allPayments = payments,
            knownHolders = holderSet.toList(),
            googleSheetUrl = sync.sheetUrl,
            isSyncing = sync.isSyncing,
            syncStatusMessage = sync.message
        )
    }

    fun setCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun setMonthIndex(index: Int) {
        _selectedMonthIndex.value = index.coerceIn(0, 11)
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun toggleShowAllMonths(show: Boolean) {
        _showAllMonthsMembers.value = show
    }

    fun setPayment(
        memberId: Long,
        monthIndex: Int,
        isPaid: Boolean,
        isPending: Boolean,
        heldBy: String,
        amount: Double,
        currency: String
    ) {
        viewModelScope.launch {
            try {
                firestoreRepository.setPaymentStatus(
                    memberId = memberId,
                    monthIndex = monthIndex,
                    isPaid = isPaid,
                    isPending = isPending,
                    heldBy = heldBy,
                    amount = amount,
                    currency = currency
                )
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to set payment: ${e.message}")
            }
        }
    }

    fun toggleMemberPaidStatus(memberId: Long, monthIndex: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            val item = currentState.membersWithStatus.find { it.member.id == memberId }
            val member = item?.member ?: return@launch
            val isCurrentlyPaid = item.status == MemberPaymentStatus.PAID

            setPayment(
                memberId = memberId,
                monthIndex = monthIndex,
                isPaid = !isCurrentlyPaid,
                isPending = false,
                heldBy = if (!isCurrentlyPaid) "Main" else item.heldBy,
                amount = member.amount,
                currency = member.currency
            )
        }
    }

    fun updateMemberAllottedMonth(memberId: Long, newMonth: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            val item = currentState.membersWithStatus.find { it.member.id == memberId }
            val member = item?.member ?: return@launch
            val updated = member.copy(allottedMonth = newMonth.trim().uppercase())
            try {
                firestoreRepository.saveMember(updated)
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to update allotted month: ${e.message}")
            }
        }
    }

    fun saveMember(member: Member) {
        viewModelScope.launch {
            try {
                firestoreRepository.saveMember(member)
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to save member: ${e.message}")
            }
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteMember(memberId)
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to delete member: ${e.message}")
            }
        }
    }

    fun addTransfer(holderName: String, currency: String, amount: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            try {
                firestoreRepository.addTransfer(holderName, currency, amount, dateStr)
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to add transfer: ${e.message}")
            }
        }
    }

    fun addExtraCollection(
        name: String,
        mobile: String,
        purpose: String,
        amount: Double,
        currency: String,
        heldBy: String
    ) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            try {
                firestoreRepository.addExtraCollection(
                    name = name,
                    mobile = mobile,
                    purpose = purpose,
                    amount = amount,
                    currency = currency,
                    heldBy = heldBy,
                    date = dateStr
                )
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Failed to add extra collection: ${e.message}")
            }
        }
    }

    fun setGoogleSheetUrl(url: String) {
        _googleSheetUrl.value = url.trim()
        prefs.edit().putString("sheet_url", url.trim()).apply()
        if (url.isNotBlank()) {
            syncWithGoogleSheet()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncWithGoogleSheet() {
        val url = _googleSheetUrl.value
        if (url.isBlank()) return
        viewModelScope.launch {
            _isSyncing.value = true
            val currentState = uiState.value
            val payload = buildSyncJsonPayload(currentState)
            val result = firestoreRepository.syncWithSheetWebhook(url, payload)
            _isSyncing.value = false
            _syncMessage.value = if (result.isSuccess) {
                "Synced successfully with Google Sheet"
            } else {
                "Sync failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    private fun triggerAutoSyncIfEnabled() {
        if (_googleSheetUrl.value.isNotBlank()) {
            syncWithGoogleSheet()
        }
    }

    private fun buildSyncJsonPayload(state: TssUiState): String {
        return """
        {
          "updatedAt": "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
          "aedBalance": { "main": ${state.aedBalance.mainAccount}, "cashWithOthers": ${state.aedBalance.cashWithOthers}, "total": ${state.aedBalance.total} },
          "inrBalance": { "main": ${state.inrBalance.mainAccount}, "cashWithOthers": ${state.inrBalance.cashWithOthers}, "total": ${state.inrBalance.total} }
        }
        """.trimIndent()
    }

    fun generateCsvAndShare(context: Context) {
        viewModelScope.launch {
            val currentState = uiState.value
            val allMembersList = currentState.membersWithStatus
            val csvBuilder = StringBuilder()
            csvBuilder.append("Name,Emirate,Country,Mobile,Allotted Month,Amount,Currency,Current Status,Held By\n")
            for (m in allMembersList) {
                val line = listOf(
                    "\"${m.member.name}\"",
                    "\"${m.member.emirateOrCity}\"",
                    "\"${m.member.country}\"",
                    "\"${m.member.mobile}\"",
                    "\"${m.member.allottedMonth}\"",
                    "\"${m.member.amount}\"",
                    "\"${m.member.currency}\"",
                    "\"${m.status.label}\"",
                    "\"${m.heldBy}\""
                ).joinToString(",")
                csvBuilder.append(line).append("\n")
            }

            try {
                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val file = File(cacheDir, "tss-fund-report-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}.csv")
                file.writeText(csvBuilder.toString())

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "TSS Fund 2026 Collection Report")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share TSS Fund Report"))
            } catch (e: Exception) {
                // Fallback text share if file provider fails
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "TSS Fund 2026 Collection Report")
                    putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share TSS Fund Report"))
            }
        }
    }
}
