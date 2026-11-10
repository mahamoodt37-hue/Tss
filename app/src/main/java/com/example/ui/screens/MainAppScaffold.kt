package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoogleSheetDialog
import com.example.ui.components.MemberActionDialog
import com.example.ui.components.TopHeader
import com.example.ui.components.TransferDialog
import com.example.ui.viewmodel.MemberWithStatus
import com.example.ui.viewmodel.TssViewModel

@Composable
fun MainAppScaffold(
    viewModel: TssViewModel,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transfers by viewModel.allTransfers.collectAsStateWithLifecycle()
    val extraCollections by viewModel.allExtraCollections.collectAsStateWithLifecycle()

    var selectedMemberForAction by remember { mutableStateOf<MemberWithStatus?>(null) }
    var isNewMemberDialogOpen by remember { mutableStateOf(false) }

    var transferDialogState by remember {
        mutableStateOf<Triple<String, String, Double>?>(null) // holder, cur, amount
    }

    var isSheetDialogOpen by remember { mutableStateOf(false) }

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
                            onOpenSheetDialog = { isSheetDialogOpen = true },
                            onExportCsv = { viewModel.generateCsvAndShare(context) },
                            onInitiateTransfer = { holder, cur, amount ->
                                transferDialogState = Triple(holder, cur, amount)
                            },
                            onNavigateToTab = { viewModel.setTab(it) },
                            onAddNewMember = { isNewMemberDialogOpen = true }
                        )
                    }

                    "members" -> {
                        MembersScreen(
                            uiState = uiState,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setFilterStatus(it) },
                            onToggleShowAllMonths = { viewModel.toggleShowAllMonths(it) },
                            onMemberClick = { selectedMemberForAction = it },
                            onAddNewMember = { isNewMemberDialogOpen = true },
                            onTogglePaidStatus = { item ->
                                viewModel.toggleMemberPaidStatus(item.member.id, uiState.selectedMonthIndex)
                            },
                            onChangeAllottedMonth = { item, newMonth ->
                                viewModel.updateMemberAllottedMonth(item.member.id, newMonth)
                            }
                        )
                    }

                    "transfers" -> {
                        TransfersScreen(
                            uiState = uiState,
                            transfers = transfers,
                            onInitiateTransfer = { holder, cur, amount ->
                                transferDialogState = Triple(holder, cur, amount)
                            }
                        )
                    }

                    "extra" -> {
                        ExtraScreen(
                            uiState = uiState,
                            extraCollections = extraCollections,
                            onAddExtra = { name, mobile, purpose, amount, cur, heldBy ->
                                viewModel.addExtraCollection(name, mobile, purpose, amount, cur, heldBy)
                            }
                        )
                    }
                }
            }
        }
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
