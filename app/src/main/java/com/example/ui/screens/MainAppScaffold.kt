package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Member
import com.example.ui.components.ContributionHistoryDialog
import com.example.ui.components.GoogleSheetDialog
import com.example.ui.components.MemberActionDialog
import com.example.ui.components.PaymentStatusUpdateDialog
import com.example.ui.components.TopHeader
import com.example.ui.components.TransferDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.MemberPaymentStatus
import com.example.ui.viewmodel.MemberWithStatus
import com.example.ui.viewmodel.TssViewModel

data class PaymentUpdateTarget(
    val member: Member,
    val monthIndex: Int,
    val currentStatus: MemberPaymentStatus,
    val currentHeldBy: String
)

@Composable
fun MainAppScaffold(
    viewModel: TssViewModel,
    userEmail: String? = null,
    isAdmin: Boolean = !userEmail.isNullOrBlank(),
    onSignInRequest: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transfers by viewModel.allTransfers.collectAsStateWithLifecycle()
    val extraCollections by viewModel.allExtraCollections.collectAsStateWithLifecycle()

    var selectedMemberForHistory by remember { mutableStateOf<MemberWithStatus?>(null) }
    var selectedMemberForAction by remember { mutableStateOf<MemberWithStatus?>(null) }
    var paymentUpdateTarget by remember { mutableStateOf<PaymentUpdateTarget?>(null) }
    var isNewMemberDialogOpen by remember { mutableStateOf(false) }
    var showAdminRequiredDialog by remember { mutableStateOf(false) }

    var transferDialogState by remember {
        mutableStateOf<Triple<String, String, Double>?>(null) // holder, cur, amount
    }

    var isSheetDialogOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.currentTab != "dashboard") {
        viewModel.setTab("dashboard")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == "dashboard",
                    onClick = { viewModel.setTab("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == "members",
                    onClick = { viewModel.setTab("members") },
                    icon = { Icon(Icons.Default.People, contentDescription = "Members") },
                    label = { Text("Members") },
                    modifier = Modifier.testTag("nav_tab_members")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == "directory",
                    onClick = { viewModel.setTab("directory") },
                    icon = { Icon(Icons.Default.ContactPhone, contentDescription = "Full List") },
                    label = { Text("Full List") },
                    modifier = Modifier.testTag("nav_tab_directory")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == "transfers",
                    onClick = { viewModel.setTab("transfers") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Transfers") },
                    label = { Text("Transfers") },
                    modifier = Modifier.testTag("nav_tab_transfers")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == "extra",
                    onClick = { viewModel.setTab("extra") },
                    icon = { Icon(Icons.Default.CardGiftcard, contentDescription = "Extra") },
                    label = { Text("Extra") },
                    modifier = Modifier.testTag("nav_tab_extra")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Edge-to-edge top header with notch padding
            Box(modifier = Modifier.statusBarsPadding()) {
                TopHeader(
                    selectedCurrency = uiState.selectedCurrency,
                    onCurrencyChange = { viewModel.setCurrency(it) },
                    selectedMonthIndex = uiState.selectedMonthIndex,
                    onMonthChange = { viewModel.setMonthIndex(it) },
                    onShareCsv = { viewModel.generateCsvAndShare(context) },
                    onOpenSheetDialog = { isSheetDialogOpen = true },
                    isSheetLinked = uiState.googleSheetUrl.isNotBlank(),
                    userEmail = userEmail,
                    onSignInRequest = onSignInRequest,
                    onSignOut = onSignOut
                )
            }

            // Sync message banner if present
            AnimatedVisibility(
                visible = uiState.syncStatusMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.syncStatusMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearSyncMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.currentTab) {
                    "dashboard" -> {
                        DashboardScreen(
                            uiState = uiState,
                            transfers = transfers,
                            extraCollections = extraCollections,
                            onOpenSheetDialog = { isSheetDialogOpen = true },
                            onExportCsv = { viewModel.generateCsvAndShare(context) },
                            onInitiateTransfer = { holder, cur, amount ->
                                if (isAdmin) {
                                    transferDialogState = Triple(holder, cur, amount)
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onNavigateToTab = { viewModel.setTab(it) },
                            onAddNewMember = {
                                if (isAdmin) {
                                    isNewMemberDialogOpen = true
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            }
                        )
                    }

                    "members" -> {
                        MembersScreen(
                            uiState = uiState,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setFilterStatus(it) },
                            onToggleShowAllMonths = { viewModel.toggleShowAllMonths(it) },
                            onMemberClick = { selectedMemberForHistory = it },
                            onAddNewMember = {
                                if (isAdmin) {
                                    isNewMemberDialogOpen = true
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onTogglePaidStatus = { item ->
                                if (isAdmin) {
                                    paymentUpdateTarget = PaymentUpdateTarget(
                                        member = item.member,
                                        monthIndex = uiState.selectedMonthIndex,
                                        currentStatus = item.status,
                                        currentHeldBy = item.heldBy
                                    )
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onChangeAllottedMonth = { item, newMonth ->
                                if (isAdmin) {
                                    viewModel.updateMemberAllottedMonth(item.member.id, newMonth)
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onShareText = { viewModel.shareMembersAsText(context) },
                            onShareImage = { viewModel.shareMembersAsImage(context) }
                        )
                    }

                    "directory" -> {
                        FullMembersListScreen(
                            uiState = uiState,
                            isAdmin = isAdmin,
                            onMemberClick = { member ->
                                val mws = uiState.membersWithStatus.find { it.member.id == member.id }
                                    ?: com.example.ui.viewmodel.MemberWithStatus(
                                        member = member,
                                        status = com.example.ui.viewmodel.MemberPaymentStatus.NOT_DUE
                                    )
                                selectedMemberForHistory = mws
                            },
                            onAddNewMember = {
                                if (isAdmin) {
                                    isNewMemberDialogOpen = true
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onShareDirectoryText = { list ->
                                viewModel.shareFullDirectoryText(context, list)
                            },
                            onShareDirectoryImage = { list ->
                                viewModel.shareFullDirectoryImage(context, list)
                            }
                        )
                    }

                    "transfers" -> {
                        TransfersScreen(
                            uiState = uiState,
                            transfers = transfers,
                            onInitiateTransfer = { holder, cur, amount ->
                                if (isAdmin) {
                                    transferDialogState = Triple(holder, cur, amount)
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            }
                        )
                    }

                    "extra" -> {
                        ExtraScreen(
                            uiState = uiState,
                            extraCollections = extraCollections,
                            onAddExtra = { name, mobile, purpose, amount, cur, heldBy ->
                                if (isAdmin) {
                                    viewModel.addExtraCollection(name, mobile, purpose, amount, cur, heldBy)
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onAddNewMember = {
                                if (isAdmin) {
                                    isNewMemberDialogOpen = true
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onMemberClick = { selectedMemberForHistory = it },
                            onTogglePaidStatus = { item ->
                                if (isAdmin) {
                                    paymentUpdateTarget = PaymentUpdateTarget(
                                        member = item.member,
                                        monthIndex = uiState.selectedMonthIndex,
                                        currentStatus = item.status,
                                        currentHeldBy = item.heldBy
                                    )
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            },
                            onChangeAllottedMonth = { item, newMonth ->
                                if (isAdmin) {
                                    viewModel.updateMemberAllottedMonth(item.member.id, newMonth)
                                } else {
                                    showAdminRequiredDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Admin Access Required Dialog (friendly prompt for viewers)
    if (showAdminRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showAdminRequiredDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Access Required", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Only authorized admins can mark payments, change member months, or add transactions.\n\nSign in with your Google account to enable Admin editing.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdminRequiredDialog = false
                        onSignInRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdminRequiredDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Contribution History Modal
    if (selectedMemberForHistory != null) {
        val selected = selectedMemberForHistory!!
        val liveMember = uiState.membersWithStatus.find { it.member.id == selected.member.id } ?: selected
        ContributionHistoryDialog(
            memberWithStatus = liveMember,
            allPayments = uiState.allPayments,
            onDismiss = { selectedMemberForHistory = null },
            onRequestUpdatePayment = { monthIndex, currentStatus, currentHeldBy ->
                if (isAdmin) {
                    paymentUpdateTarget = PaymentUpdateTarget(
                        member = liveMember.member,
                        monthIndex = monthIndex,
                        currentStatus = currentStatus,
                        currentHeldBy = currentHeldBy
                    )
                } else {
                    showAdminRequiredDialog = true
                }
            },
            onEditMember = { memberToEdit ->
                if (isAdmin) {
                    selectedMemberForHistory = null
                    selectedMemberForAction = liveMember.copy(member = memberToEdit)
                } else {
                    showAdminRequiredDialog = true
                }
            }
        )
    }

    // Payment Status Update Dialog (Cash in Main Account or with Anyone's Hand)
    if (paymentUpdateTarget != null) {
        val target = paymentUpdateTarget!!
        PaymentStatusUpdateDialog(
            member = target.member,
            monthIndex = target.monthIndex,
            currentStatus = target.currentStatus,
            currentHeldBy = target.currentHeldBy,
            knownHolders = uiState.knownHolders,
            onDismiss = { paymentUpdateTarget = null },
            onConfirm = { isPaid, isPending, heldBy ->
                viewModel.setPayment(
                    memberId = target.member.id,
                    monthIndex = target.monthIndex,
                    isPaid = isPaid,
                    isPending = isPending,
                    heldBy = heldBy,
                    amount = target.member.amount,
                    currency = target.member.currency
                )
                paymentUpdateTarget = null
            }
        )
    }

    // Member Action / Edit Dialog
    if (selectedMemberForAction != null) {
        val item = selectedMemberForAction!!
        MemberActionDialog(
            memberWithStatus = item,
            defaultCurrency = uiState.selectedCurrency,
            selectedMonthIndex = uiState.selectedMonthIndex,
            knownHolders = uiState.knownHolders,
            onDismiss = { selectedMemberForAction = null },
            onSavePayment = { isPaid, isPending, heldBy ->
                viewModel.setPayment(
                    memberId = item.member.id,
                    monthIndex = uiState.selectedMonthIndex,
                    isPaid = isPaid,
                    isPending = isPending,
                    heldBy = heldBy,
                    amount = item.member.amount,
                    currency = item.member.currency
                )
            },
            onSaveMember = { updatedMember ->
                viewModel.saveMember(updatedMember)
            },
            onDeleteMember = { memberId ->
                viewModel.deleteMember(memberId)
            }
        )
    }

    // Add New Member Dialog
    if (isNewMemberDialogOpen) {
        MemberActionDialog(
            memberWithStatus = null,
            defaultCurrency = uiState.selectedCurrency,
            selectedMonthIndex = uiState.selectedMonthIndex,
            knownHolders = uiState.knownHolders,
            onDismiss = { isNewMemberDialogOpen = false },
            onSavePayment = { _, _, _ -> },
            onSaveMember = { newMember ->
                viewModel.saveMember(newMember)
            },
            onDeleteMember = { }
        )
    }

    // Transfer Dialog
    transferDialogState?.let { (holder, cur, heldAmount) ->
        TransferDialog(
            holderName = holder,
            currency = cur,
            currentHeldAmount = heldAmount,
            onDismiss = { transferDialogState = null },
            onConfirmTransfer = { amount ->
                viewModel.addTransfer(holder, cur, amount)
            }
        )
    }

    // Google Sheet Webhook Dialog
    if (isSheetDialogOpen) {
        GoogleSheetDialog(
            currentUrl = uiState.googleSheetUrl,
            isSyncing = uiState.isSyncing,
            syncStatusMessage = uiState.syncStatusMessage,
            onDismiss = { isSheetDialogOpen = false },
            onSaveUrl = { url ->
                viewModel.setGoogleSheetUrl(url)
            },
            onManualSync = {
                viewModel.syncWithGoogleSheet()
            }
        )
    }
}
