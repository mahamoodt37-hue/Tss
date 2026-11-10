package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Member
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.MemberPaymentStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentStatusUpdateDialog(
    member: Member,
    monthIndex: Int,
    currentStatus: MemberPaymentStatus,
    currentHeldBy: String,
    knownHolders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (isPaid: Boolean, isPending: Boolean, heldBy: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = MONTHS.getOrElse(monthIndex) { "NOV" }

    // Status selection: "PAID", "PENDING", "UNPAID"
    var selectedStatus by remember {
        mutableStateOf(
            when (currentStatus) {
                MemberPaymentStatus.PAID -> "PAID"
                MemberPaymentStatus.PENDING -> "PENDING"
                else -> "PAID" // Default to PAID when user initiates status update
            }
        )
    }

    // Cash location mode: "MAIN" or "HAND"
    var cashLocationMode by remember {
        mutableStateOf(
            if (currentHeldBy.isNotBlank() && !currentHeldBy.equals("Main", ignoreCase = true)) {
                "HAND"
            } else {
                "MAIN"
            }
        )
    }

    var customHolderName by remember {
        mutableStateOf(
            if (currentHeldBy.isNotBlank() && !currentHeldBy.equals("Main", ignoreCase = true)) {
                currentHeldBy
            } else {
                ""
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("dialog_update_payment_status"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Payment Status",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${member.name} · $monthName 2026",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("button_close_payment_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Member Contribution Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Quota",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(member.currency, member.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = "Allotted: ${member.allottedMonth}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Selector Label
                Text(
                    text = "Select Payment Status",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Choices Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paid Option
                    StatusOptionCard(
                        title = "Paid",
                        isSelected = selectedStatus == "PAID",
                        selectedBg = StatusPaidBg,
                        selectedColor = StatusPaid,
                        icon = Icons.Default.Check,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("option_status_paid"),
                        onClick = { selectedStatus = "PAID" }
                    )

                    // Pending Option
                    StatusOptionCard(
                        title = "Pending",
                        isSelected = selectedStatus == "PENDING",
                        selectedBg = StatusPendingBg,
                        selectedColor = StatusPending,
                        icon = Icons.Default.HourglassTop,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("option_status_pending"),
                        onClick = { selectedStatus = "PENDING" }
                    )

                    // Unpaid Option
                    StatusOptionCard(
                        title = "Unpaid",
                        isSelected = selectedStatus == "UNPAID",
                        selectedBg = StatusUnpaidBg,
                        selectedColor = StatusUnpaid,
                        icon = Icons.Default.Close,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("option_status_unpaid"),
                        onClick = { selectedStatus = "UNPAID" }
                    )
                }

                // If Paid or Pending, show Cash Location Selection (Cash in Main Account or With Anyone's Hand)
                AnimatedVisibility(visible = selectedStatus == "PAID" || selectedStatus == "PENDING") {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "Cash Location (Where is the cash?)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Specify whether money is in the main account or held in hand by someone.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 1: Cash in Main Account
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cashLocationMode = "MAIN" }
                                .testTag("option_cash_in_main"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cashLocationMode == "MAIN") {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (cashLocationMode == "MAIN") {
                                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (cashLocationMode == "MAIN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = if (cashLocationMode == "MAIN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cash in Main Account",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Directly deposited into TSS Fund Main treasury account",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (cashLocationMode == "MAIN") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: With Anyone's Hand / Collector
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cashLocationMode = "HAND" }
                                .testTag("option_cash_in_hand"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cashLocationMode == "HAND") {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (cashLocationMode == "HAND") {
                                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (cashLocationMode == "HAND") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (cashLocationMode == "HAND") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Cash with Collectors",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (customHolderName.isNotBlank() && cashLocationMode == "HAND") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(99.dp),
                                                    color = GoldAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = customHolderName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (customHolderName.isNotBlank() && cashLocationMode == "HAND") {
                                                "Collector: $customHolderName (reflects in dashboard collector column)"
                                            } else {
                                                "Held by committee member / collector (mention collector below)"
                                            },
                                            fontSize = 11.sp,
                                            color = if (customHolderName.isNotBlank() && cashLocationMode == "HAND") {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = if (customHolderName.isNotBlank() && cashLocationMode == "HAND") FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }

                                    if (cashLocationMode == "HAND") {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // If Hand is selected, show name input and quick chips
                                AnimatedVisibility(visible = cashLocationMode == "HAND") {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        OutlinedTextField(
                                            value = customHolderName,
                                            onValueChange = { customHolderName = it },
                                            label = { Text("Collector Name *") },
                                            placeholder = { Text("e.g. Azeez KC, Basheer, etc.") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("input_holder_name")
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Select or Tap Known Collector:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Quick Collector Chips
                                        val filteredHolders = knownHolders.filter { !it.equals("Main", ignoreCase = true) }
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            filteredHolders.forEach { holder ->
                                                val isChipSelected = customHolderName.equals(holder, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isChipSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier
                                                        .clickable { customHolderName = holder }
                                                        .testTag("holder_chip_$holder")
                                                ) {
                                                    Text(
                                                        text = holder,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isChipSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_cancel_payment_update")
                    ) {
                        Text("Cancel")
                    }

                    val effectiveHeldBy = if (cashLocationMode == "MAIN" || selectedStatus == "UNPAID") {
                        "Main"
                    } else {
                        customHolderName.trim().ifBlank { "Main" }
                    }

                    val isSaveEnabled = when (selectedStatus) {
                        "UNPAID" -> true
                        else -> cashLocationMode == "MAIN" || customHolderName.isNotBlank()
                    }

                    Button(
                        onClick = {
                            when (selectedStatus) {
                                "PAID" -> onConfirm(true, false, effectiveHeldBy)
                                "PENDING" -> onConfirm(false, true, effectiveHeldBy)
                                "UNPAID" -> onConfirm(false, false, "Main")
                            }
                            onDismiss()
                        },
                        enabled = isSaveEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedStatus) {
                                "PAID" -> StatusPaid
                                "PENDING" -> StatusPending
                                else -> StatusUnpaid
                            }
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("button_save_payment_status")
                    ) {
                        Text(
                            text = when (selectedStatus) {
                                "PAID" -> "Mark Paid"
                                "PENDING" -> "Mark Pending"
                                else -> "Mark Unpaid"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusOptionCard(
    title: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(selectedColor))
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
