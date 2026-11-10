package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusInProgressBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.viewmodel.MemberPaymentStatus
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(currency: String, amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return "$currency ${formatter.format(amount)}"
}

@Composable
fun StatusBadge(
    status: MemberPaymentStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        MemberPaymentStatus.PAID -> Triple(StatusPaidBg, StatusPaid, "Paid")
        MemberPaymentStatus.IN_PROGRESS -> Triple(StatusInProgressBg, StatusInProgress, "In Progress")
        MemberPaymentStatus.PENDING -> Triple(StatusPendingBg, StatusPending, "Pending")
        MemberPaymentStatus.UNPAID -> Triple(StatusUnpaidBg, StatusUnpaid, "Unpaid")
        MemberPaymentStatus.NOT_DUE -> Triple(Color(0xFFE2E8F0), Color(0xFF64748B), "Not Due")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CurrencySegmentedSwitch(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
            .testTag("currency_switch")
    ) {
        listOf("AED", "INR").forEach { cur ->
            val isSelected = cur == selectedCurrency
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onCurrencySelected(cur) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("currency_option_$cur"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cur,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
