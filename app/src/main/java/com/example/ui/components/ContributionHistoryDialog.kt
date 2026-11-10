package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.ui.theme.EmeraldMidnight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.TealDark
import com.example.ui.viewmodel.CURRENT_MONTH_INDEX
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.MemberPaymentStatus
import com.example.ui.viewmodel.MemberWithStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MonthHistoryItem(
    val monthIndex: Int,
    val monthName: String,
    val year: Int = 2026,
    val status: MemberPaymentStatus,
    val amount: Double,
    val currency: String,
    val heldBy: String,
    val paymentDateText: String,
    val isDueThisMonth: Boolean,
    val isCurrentMonth: Boolean,
    val isPaid: Boolean
)

@Composable
fun ContributionHistoryDialog(
    memberWithStatus: MemberWithStatus,
    allPayments: List<PaymentRecord>,
    onDismiss: () -> Unit,
    onRequestUpdatePayment: (monthIndex: Int, currentStatus: MemberPaymentStatus, currentHeldBy: String) -> Unit,
    onEditMember: (Member) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val member = memberWithStatus.member
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "PAID", "UNPAID"

    // Filter payments specifically for this member
    val memberPayments = remember(allPayments, member.id) {
        allPayments.filter { it.memberId == member.id }.associateBy { it.monthIndex }
    }

    // Build the 12-month history list
    val historyItems = remember(member, memberPayments) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        (0..11).map { index ->
            val monthName = MONTHS[index]
            val isDue = member.allottedMonth.equals("ALL", ignoreCase = true) ||
                    member.allottedMonth.equals(monthName, ignoreCase = true)
            val p = memberPayments[index]
            val isPaid = p?.isPaid == true
            val isPending = p?.isPending == true

            val status = when {
                isPaid -> MemberPaymentStatus.PAID
                isPending -> MemberPaymentStatus.PENDING
                !isDue -> MemberPaymentStatus.NOT_DUE
                index < CURRENT_MONTH_INDEX -> MemberPaymentStatus.UNPAID
                index == CURRENT_MONTH_INDEX -> MemberPaymentStatus.IN_PROGRESS
                else -> MemberPaymentStatus.PENDING
            }

            val dateText = when {
                isPaid && (p?.timestamp ?: 0L) > 0L -> "Paid on " + dateFormat.format(Date(p!!.timestamp))
                isPaid -> "Paid (Recorded)"
                isPending -> "Marked Pending"
                !isDue -> "Not due for $monthName"
                index == CURRENT_MONTH_INDEX -> "Due for current cycle (Nov)"
                index < CURRENT_MONTH_INDEX -> "Overdue for $monthName 2026"
                else -> "Scheduled for $monthName 2026"
            }

            MonthHistoryItem(
                monthIndex = index,
                monthName = monthName,
                year = 2026,
                status = status,
                amount = if (p != null && p.amount > 0) p.amount else member.amount,
                currency = if (p != null && p.currency.isNotBlank()) p.currency else member.currency,
                heldBy = p?.heldBy ?: "Main",
                paymentDateText = dateText,
                isDueThisMonth = isDue,
                isCurrentMonth = index == CURRENT_MONTH_INDEX,
                isPaid = isPaid
            )
        }
    }

    // Contribution Analytics
    val totalDueMonths = remember(historyItems) { historyItems.count { it.isDueThisMonth } }
    val paidMonthsCount = remember(historyItems) { historyItems.count { it.isPaid } }
    val totalContributed = remember(historyItems) { historyItems.filter { it.isPaid }.sumOf { it.amount } }
    val totalExpected = remember(historyItems, member) {
        if (member.allottedMonth.equals("ALL", ignoreCase = true)) member.amount * 12 else member.amount
    }
    val outstandingAmount = (totalExpected - totalContributed).coerceAtLeast(0.0)
    val progress = if (totalExpected > 0) (totalContributed / totalExpected).coerceIn(0.0, 1.0).toFloat() else 0f

    val filteredItems = remember(historyItems, selectedFilter) {
        when (selectedFilter) {
            "PAID" -> historyItems.filter { it.isPaid }
            "UNPAID" -> historyItems.filter { !it.isPaid && it.isDueThisMonth }
            else -> historyItems
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("contribution_history_modal"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Luxury Top Gradient Header with Member Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(EmeraldMidnight, TealDark)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar circle with luxury rim
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GoldAccent, CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = member.name.trim().split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .take(2)
                                        .joinToString("")
                                    Text(
                                        text = initials.ifBlank { "M" },
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = member.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val loc = listOf(member.emirateOrCity, member.country)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", ")
                                        if (loc.isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = loc,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.White.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Cycle: ${member.allottedMonth}",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("button_close_history_dialog")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Contact row with Quick Call & WhatsApp
                        if (member.mobile.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = member.mobile,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { dialNumber(context, member.mobile) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { openWhatsApp(context, member.mobile, member.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Contribution Progress & Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2026 Contribution History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Completion Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (progress >= 1f) StatusPaidBg else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}% Paid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progress >= 1f) StatusPaid else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Key Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = "Contributed",
                                value = formatCurrency(member.currency, totalContributed),
                                valueColor = MaterialTheme.colorScheme.primary
                            )

                            MetricItem(
                                label = "Months Paid",
                                value = "$paidMonthsCount / $totalDueMonths",
                                valueColor = MaterialTheme.colorScheme.onSurface
                            )

                            MetricItem(
                                label = "Outstanding",
                                value = formatCurrency(member.currency, outstandingAmount),
                                valueColor = if (outstandingAmount > 0) StatusUnpaid else StatusPaid
                            )
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${historyItems.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(30.dp)
                    )

                    FilterChip(
                        selected = selectedFilter == "PAID",
                        onClick = { selectedFilter = "PAID" },
                        label = { Text("Paid ($paidMonthsCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPaidBg,
                            selectedLabelColor = StatusPaid
                        ),
                        modifier = Modifier.height(30.dp)
                    )

                    FilterChip(
                        selected = selectedFilter == "UNPAID",
                        onClick = { selectedFilter = "UNPAID" },
                        label = {
                            Text(
                                "Unpaid (${totalDueMonths - paidMonthsCount})",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusUnpaidBg,
                            selectedLabelColor = StatusUnpaid
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Chronological Month Contributions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .testTag("contribution_history_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.monthIndex }) { item ->
                        MonthHistoryRow(
                            item = item,
                            onUpdatePayment = {
                                onRequestUpdatePayment(item.monthIndex, item.status, item.heldBy)
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEditMember(member)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("button_edit_member_from_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Member", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("button_done_history")
                    ) {
                        Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHistoryRow(
    item: MonthHistoryItem,
    onUpdatePayment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onUpdatePayment() }
            .testTag("history_item_${item.monthName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrentMonth) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (item.isCurrentMonth) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primary, GoldAccent)
                )
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Month Badge + Date & Status Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Month Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (item.isPaid) StatusPaidBg else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.monthName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isPaid) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "2026",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatCurrency(item.currency, item.amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isCurrentMonth) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Current",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.paymentDateText + if (item.isPaid && item.heldBy.isNotBlank() && item.heldBy != "Main") " · Cash with Collector: ${item.heldBy}" else if (item.isPaid) " · In Main Account" else "",
                        fontSize = 11.sp,
                        color = if (item.isPaid) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (item.isPaid) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            // Status Chip & Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                HistoryStatusBadge(status = item.status)

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onUpdatePayment,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("button_update_month_${item.monthName}")
                ) {
                    Icon(
                        imageVector = if (item.isPaid) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = "Update Payment Status",
                        tint = if (item.isPaid) StatusPaid else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusBadge(status: MemberPaymentStatus) {
    val (bgColor, textColor, label) = when (status) {
        MemberPaymentStatus.PAID -> Triple(StatusPaidBg, StatusPaid, "Paid")
        MemberPaymentStatus.IN_PROGRESS -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), "In Progress")
        MemberPaymentStatus.PENDING -> Triple(StatusPendingBg, StatusPending, "Pending")
        MemberPaymentStatus.UNPAID -> Triple(StatusUnpaidBg, StatusUnpaid, "Unpaid")
        MemberPaymentStatus.NOT_DUE -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Not Due")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

private fun dialNumber(context: Context, mobile: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$mobile")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openWhatsApp(context: Context, mobile: String, name: String) {
    try {
        val cleanNumber = mobile.replace(Regex("[^0-9]"), "")
        val message = "Assalamu Alaikum $name, regards from Thajusunna Pilathra (TSS) Monthly Fund collection."
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
