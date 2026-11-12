package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.SeedData
import com.example.data.firestore.FirestoreRepository
import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import com.example.data.repository.TssRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val tssRepository = TssRepository(
        database.memberDao(),
        database.paymentDao(),
        database.transferDao(),
        database.extraCollectionDao()
    )
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
        // 1. Ensure initial members in local SQLite Room database immediately
        viewModelScope.launch {
            try {
                if (database.memberDao().getMemberCount() == 0) {
                    database.memberDao().insertAll(SeedData.getInitialMembers())
                }
            } catch (e: Exception) {
                Log.w("TssViewModel", "Local initial seed check: ${e.message}")
            }
        }

        // 2. Ensure initial members in Cloud Firestore as soon as authenticated admin opens the app
        viewModelScope.launch {
            try {
                firestoreRepository.authFlow.collect { user ->
                    if (user != null) {
                        try {
                            firestoreRepository.seedInitialMembersIfEmpty(SeedData.getInitialMembers())
                        } catch (e: Exception) {
                            Log.w("TssViewModel", "Cloud seed check: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TssViewModel", "Auth flow observer: ${e.message}")
            }
        }
    }

    val allTransfers: StateFlow<List<TransferRecord>> = combine(
        firestoreRepository.observeTransfers().catch { emit(emptyList()) },
        tssRepository.allTransfers.catch { emit(emptyList()) }
    ) { cloud, local ->
        if (cloud.isNotEmpty()) cloud else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExtraCollections: StateFlow<List<ExtraCollection>> = combine(
        firestoreRepository.observeExtraCollections().catch { emit(emptyList()) },
        tssRepository.allExtraCollections.catch { emit(emptyList()) }
    ) { cloud, local ->
        if (cloud.isNotEmpty()) cloud else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TssUiState> = combine(
        combine(
            combine(
                firestoreRepository.observeMembers().catch { emit(emptyList()) },
                tssRepository.allMembers.catch { emit(emptyList()) }
            ) { cloud, local ->
                if (cloud.isNotEmpty()) cloud else local
            },
            combine(
                firestoreRepository.observePayments().catch { emit(emptyList()) },
                tssRepository.allPayments.catch { emit(emptyList()) }
            ) { cloud, local ->
                val map = mutableMapOf<String, PaymentRecord>()
                for (p in local) {
                    map["${p.memberId}|${p.monthIndex}"] = p
                }
                for (p in cloud) {
                    val key = "${p.memberId}|${p.monthIndex}"
                    val existing = map[key]
                    if (existing == null || p.timestamp >= existing.timestamp) {
                        map[key] = p
                    }
                }
                map.values.toList()
            },
            allTransfers,
            allExtraCollections
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
                    val amt = if (p != null && p.amount > 0.0) p.amount else m.amount
                    collectedAmount += amt
                }
                MemberPaymentStatus.IN_PROGRESS -> inProgressCount++
                MemberPaymentStatus.PENDING -> pendingCount++
                MemberPaymentStatus.UNPAID -> unpaidCount++
                MemberPaymentStatus.NOT_DUE -> {}
            }
        }

        // Also credit payments marked Paid for this month from members who are not in dueMembers
        // (for example: members allotted another month who paid in advance or off-cycle)
        for (p in payments) {
            if (p.isPaid && p.monthIndex == filter.monthIndex) {
                val member = memberMap[p.memberId]
                val cur = (if (p.currency.isNotBlank()) p.currency else member?.currency ?: "AED").trim().uppercase()
                if (cur.equals(filter.currency, ignoreCase = true) && (member == null || !dueMembers.contains(member))) {
                    val amt = if (p.amount > 0.0) p.amount else (member?.amount ?: 0.0)
                    collectedAmount += amt
                    paidCount++
                }
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
            allMembers = members,
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
        val safeHeldBy = heldBy.trim().ifBlank { "Main" }
        val safeCurrency = currency.trim().uppercase().ifBlank { "AED" }
        viewModelScope.launch {
            // 1. Immediately write to Room database for 0ms local reflection
            try {
                tssRepository.setPaymentStatus(
                    memberId = memberId,
                    monthIndex = monthIndex,
                    isPaid = isPaid,
                    isPending = isPending,
                    heldBy = safeHeldBy,
                    amount = amount,
                    currency = safeCurrency
                )
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local setPayment error: ${e.message}")
            }

            // 2. Persist to Cloud Firestore for cross-device real-time sync
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.setPaymentStatus(
                        memberId = memberId,
                        monthIndex = monthIndex,
                        isPaid = isPaid,
                        isPending = isPending,
                        heldBy = safeHeldBy,
                        amount = amount,
                        currency = safeCurrency
                    )
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud setPayment error: ${e.message}")
            }
        }
    }

    fun toggleMemberPaidStatus(memberId: Long, monthIndex: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            val item = currentState.membersWithStatus.find { it.member.id == memberId }
            val member = item?.member ?: run {
                database.memberDao().getMemberById(memberId)
            } ?: return@launch

            val isCurrentlyPaid = item?.status == MemberPaymentStatus.PAID ||
                currentState.allPayments.any { it.memberId == memberId && it.monthIndex == monthIndex && it.isPaid }

            setPayment(
                memberId = memberId,
                monthIndex = monthIndex,
                isPaid = !isCurrentlyPaid,
                isPending = false,
                heldBy = if (!isCurrentlyPaid) "Main" else (item?.heldBy ?: "Main"),
                amount = member.amount,
                currency = member.currency
            )
        }
    }

    fun updateMemberAllottedMonth(memberId: Long, newMonth: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            val item = currentState.membersWithStatus.find { it.member.id == memberId }
            val member = item?.member ?: run {
                database.memberDao().getMemberById(memberId)
            } ?: return@launch
            val updated = member.copy(allottedMonth = newMonth.trim().uppercase())
            try {
                tssRepository.updateMember(updated)
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local update member month error: ${e.message}")
            }
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.saveMember(updated)
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud update member month error: ${e.message}")
            }
        }
    }

    fun saveMember(member: Member) {
        viewModelScope.launch {
            val memberId = if (member.id <= 0L) System.currentTimeMillis() else member.id
            val memberToSave = member.copy(id = memberId)
            try {
                val existing = database.memberDao().getMemberById(memberId)
                if (existing != null) {
                    tssRepository.updateMember(memberToSave)
                } else {
                    tssRepository.insertMember(memberToSave)
                }
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local saveMember error: ${e.message}")
            }
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.saveMember(memberToSave)
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud saveMember error: ${e.message}")
            }
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            try {
                tssRepository.deleteMember(memberId)
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local deleteMember error: ${e.message}")
            }
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.deleteMember(memberId)
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud deleteMember error: ${e.message}")
            }
        }
    }

    fun addTransfer(holderName: String, currency: String, amount: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val safeHolder = holderName.trim().ifBlank { "Main" }
            val safeCur = currency.trim().uppercase().ifBlank { "AED" }
            try {
                tssRepository.addTransfer(safeHolder, safeCur, amount, dateStr)
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local addTransfer error: ${e.message}")
            }
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.addTransfer(safeHolder, safeCur, amount, dateStr)
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud addTransfer error: ${e.message}")
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
            val safeHolder = heldBy.trim().ifBlank { "Main" }
            val safeCur = currency.trim().uppercase().ifBlank { "AED" }
            try {
                tssRepository.addExtraCollection(name, mobile, purpose, amount, safeCur, safeHolder, dateStr)
            } catch (e: Exception) {
                Log.e("TssViewModel", "Local addExtraCollection error: ${e.message}")
            }
            try {
                if (Firebase.auth.currentUser != null) {
                    firestoreRepository.addExtraCollection(
                        name = name,
                        mobile = mobile,
                        purpose = purpose,
                        amount = amount,
                        currency = safeCur,
                        heldBy = safeHolder,
                        date = dateStr
                    )
                }
                triggerAutoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("TssViewModel", "Cloud addExtraCollection error: ${e.message}")
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

    fun shareMembersAsText(context: Context) {
        val currentState = uiState.value
        val members = currentState.membersWithStatus
        val cur = currentState.selectedCurrency
        val isAll = currentState.showAllMonthsMembers || currentState.searchQuery.isNotBlank()
        val monthName = MONTHS.getOrElse(currentState.selectedMonthIndex) { "NOV" }
        val title = if (isAll) {
            "TSS Fund - Full member list ($cur)"
        } else {
            "TSS Fund - $monthName 2026 ($cur)"
        }

        val rows = members.map { m ->
            val s = m.status.label
            val statusLabel = if (s.equals("Inprogress", ignoreCase = true)) "In progress" else s
            MemberShareRow(
                name = m.member.name,
                mobile = m.member.mobile.ifBlank { "no number" },
                amount = if (cur == "AED") "AED ${m.member.amount}" else "₹ ${m.member.amount}",
                month = m.member.allottedMonth,
                status = statusLabel
            )
        }
        val total = members.sumOf { it.member.amount }
        val totalFormatted = if (cur == "AED") "AED $total" else "₹ $total"

        val textContent = "*$title*\n\n" +
            rows.mapIndexed { index, r ->
                "${index + 1}. ${r.name} - ${r.mobile} - ${r.amount} - ${if (isAll) "Month " + r.month else r.status}"
            }.joinToString("\n") +
            "\n\n*Total: $totalFormatted*"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, textContent)
        }

        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, textContent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(waIntent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(sendIntent, "Share member list")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun shareMembersAsImage(context: Context) {
        val currentState = uiState.value
        val members = currentState.membersWithStatus
        val cur = currentState.selectedCurrency
        val isAll = currentState.showAllMonthsMembers || currentState.searchQuery.isNotBlank()
        val monthName = MONTHS.getOrElse(currentState.selectedMonthIndex) { "NOV" }
        val title = if (isAll) {
            "TSS Fund - Full member list ($cur)"
        } else {
            "TSS Fund - $monthName 2026 ($cur)"
        }

        val rows = members.map { m ->
            val s = m.status.label
            val statusLabel = if (s.equals("Inprogress", ignoreCase = true)) "In progress" else s
            MemberShareRow(
                name = m.member.name,
                mobile = m.member.mobile.ifBlank { "no number" },
                amount = if (cur == "AED") "AED ${m.member.amount}" else "₹ ${m.member.amount}",
                month = m.member.allottedMonth,
                status = statusLabel
            )
        }
        val total = members.sumOf { it.member.amount }
        val totalFormatted = if (cur == "AED") "AED $total" else "₹ $total"

        // Dimensions: W=720, H=124 + rows.length * 54
        val width = 720
        val rowHeight = 54
        val headerHeight = 64
        val height = (124 + rows.size * rowHeight).coerceAtLeast(180)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: #ffffff
        canvas.drawColor(Color.WHITE)

        // Header: #0f766e
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0f766e")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), headerPaint)

        // Title text: bold 24px white at (16, 40)
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(title, 16f, 40f, headerTextPaint)

        // Zebra background: #f3f6f4
        val zebraPaint = Paint().apply {
            color = Color.parseColor("#f3f6f4")
            style = Paint.Style.FILL
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14231c")
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66776e")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14231c")
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        rows.forEachIndexed { i, r ->
            val y = headerHeight.toFloat() + i * rowHeight.toFloat()
            if (i % 2 != 0) {
                canvas.drawRect(0f, y, width.toFloat(), y + rowHeight.toFloat(), zebraPaint)
            }

            // Amount text width to prevent overlap
            val amountWidth = amountPaint.measureText(r.amount)
            val maxNameWidth = width.toFloat() - 32f - amountWidth - 16f

            var nameText = "${i + 1}. ${r.name}"
            if (namePaint.measureText(nameText) > maxNameWidth) {
                var truncated = r.name
                while (truncated.isNotEmpty() && namePaint.measureText("${i + 1}. $truncated...") > maxNameWidth) {
                    truncated = truncated.dropLast(1)
                }
                nameText = "${i + 1}. $truncated..."
            }

            // Name at y + 24
            canvas.drawText(nameText, 16f, y + 24f, namePaint)

            // Subtitle: mobile · (month or status) at y + 46
            val subText = "${r.mobile} · ${if (isAll) "Month " + r.month else r.status}"
            canvas.drawText(subText, 16f, y + 46f, subPaint)

            // Amount right-aligned at width - 16, y + 32
            canvas.drawText(r.amount, (width - 16).toFloat(), y + 32f, amountPaint)
        }

        // Footer Total: bold 22px #0f766e at (16, H - 24)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0f766e")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Total: $totalFormatted", 16f, (height - 24).toFloat(), footerPaint)

        try {
            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val file = File(cacheDir, "tss-list.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share member list image")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("TssViewModel", "Failed to share image: ${e.message}", e)
        }
    }

    fun shareFullDirectoryText(context: Context, members: List<Member>) {
        val title = "Thajusunna Pilathara (TSS) - Full Members Directory"
        val rows = members.mapIndexed { index, m ->
            val phone = if (m.mobile.isNotBlank()) m.mobile else "no number"
            val amt = if (m.currency == "AED") "AED ${m.amount.toLong()}" else "₹ ${m.amount.toLong()}"
            val place = listOf(m.emirateOrCity, m.country).filter { it.isNotBlank() }.joinToString(", ")
            val loc = if (place.isNotBlank()) " ($place)" else ""
            "${index + 1}. ${m.name} - $phone - $amt - Month: ${m.allottedMonth}$loc"
        }
        val aedTotal = members.filter { it.currency == "AED" }.sumOf { it.amount.toLong() }
        val inrTotal = members.filter { it.currency == "INR" }.sumOf { it.amount.toLong() }
        val footer = "*Total Members: ${members.size}*\n*Total AED: AED $aedTotal · Total INR: ₹ $inrTotal*"

        val textContent = "*$title*\n\n" + rows.joinToString("\n") + "\n\n$footer"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, textContent)
        }

        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, textContent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(waIntent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(sendIntent, "Share Members Directory")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun shareFullDirectoryImage(context: Context, members: List<Member>) {
        val title = "TSS Fund - Full Members Directory"
        val width = 720
        val rowHeight = 56
        val headerHeight = 70
        val footerHeight = 60
        val height = (headerHeight + footerHeight + members.size * rowHeight).coerceAtLeast(200)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0f766e")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), headerPaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(title, 16f, 32f, headerTitlePaint)

        val aedTotal = members.filter { it.currency == "AED" }.sumOf { it.amount.toLong() }
        val inrTotal = members.filter { it.currency == "INR" }.sumOf { it.amount.toLong() }

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#d1fae5")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("${members.size} Members · Total AED $aedTotal · Total INR ₹ $inrTotal", 16f, 54f, headerSubPaint)

        val zebraPaint = Paint().apply {
            color = Color.parseColor("#f8fafc")
            style = Paint.Style.FILL
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0f172a")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748b")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0f766e")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        members.forEachIndexed { i, m ->
            val y = headerHeight.toFloat() + i * rowHeight.toFloat()
            if (i % 2 != 0) {
                canvas.drawRect(0f, y, width.toFloat(), y + rowHeight.toFloat(), zebraPaint)
            }
            val amtText = if (m.currency == "AED") "AED ${m.amount.toLong()}" else "₹ ${m.amount.toLong()}"
            val amtWidth = amountPaint.measureText(amtText)
            val maxNameWidth = width.toFloat() - 32f - amtWidth - 20f

            var nameText = "${i + 1}. ${m.name}"
            if (namePaint.measureText(nameText) > maxNameWidth) {
                var truncated = m.name
                while (truncated.isNotEmpty() && namePaint.measureText("${i + 1}. $truncated...") > maxNameWidth) {
                    truncated = truncated.dropLast(1)
                }
                nameText = "${i + 1}. $truncated..."
            }

            canvas.drawText(nameText, 16f, y + 24f, namePaint)

            val phone = if (m.mobile.isNotBlank()) m.mobile else "no number"
            val place = listOf(m.emirateOrCity, m.country).filter { it.isNotBlank() }.joinToString(", ")
            val loc = if (place.isNotBlank()) " · $place" else ""
            val subText = "$phone · Month: ${m.allottedMonth}$loc"
            canvas.drawText(subText, 16f, y + 46f, subPaint)

            canvas.drawText(amtText, (width - 16).toFloat(), y + 32f, amountPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0f766e")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Thajusunna Pilathara (TSS) Gulf Committee · 2026", 16f, (height - 22).toFloat(), footerPaint)

        try {
            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val file = File(cacheDir, "tss-directory.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Directory Image")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("TssViewModel", "Failed to share directory image: ${e.message}", e)
        }
    }
}

private data class MemberShareRow(
    val name: String,
    val mobile: String,
    val amount: String,
    val month: String,
    val status: String
)

