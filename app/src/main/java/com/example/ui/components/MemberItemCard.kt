package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.MemberPaymentStatus
import com.example.ui.viewmodel.MemberWithStatus

@Composable
fun MemberItemCard(
    memberWithStatus: MemberWithStatus,
    onClick: () -> Unit,
    onTogglePaidStatus: () -> Unit,
    onChangeAllottedMonth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val member = memberWithStatus.member
    val status = memberWithStatus.status
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("member_card_${member.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Member Name + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = member.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View Contribution History",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = formatCurrency(member.currency, member.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location + Allotted Month (Interactive Month Change)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val location = listOf(member.emirateOrCity, member.country)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")

                    if (location.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Interactive Allotted Month Chip with Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { monthMenuExpanded = true }
                                .testTag("button_change_month_${member.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Month: ${member.allottedMonth}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            (listOf("ALL") + MONTHS).forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = m,
                                            fontWeight = if (m == member.allottedMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = if (m == member.allottedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onChangeAllottedMonth(m)
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Badge
                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row: Call / WhatsApp + One-Tap Paid/Unpaid Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mobile & Quick Communication Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (member.mobile.isNotBlank()) {
                        IconButton(
                            onClick = { dialNumber(context, member.mobile) },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("button_call_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call ${member.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { openWhatsApp(context, member.mobile, member.name) },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("button_whatsapp_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp ${member.name}",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = member.mobile,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No mobile",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (status == MemberPaymentStatus.PAID && memberWithStatus.heldBy.isNotBlank() && memberWithStatus.heldBy != "Main") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cash with Collector: ${memberWithStatus.heldBy}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Prominent One-Tap Paid / Unpaid Button
                if (status == MemberPaymentStatus.PAID) {
                    OutlinedButton(
                        onClick = onTogglePaidStatus,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusUnpaid
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("button_toggle_unpaid_${member.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = StatusUnpaid,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Mark Unpaid", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    androidx.compose.material3.Button(
                        onClick = onTogglePaidStatus,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusPaid
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("button_toggle_paid_${member.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
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
        val message = "Assalamu Alaikum $name, regards from Thajusunna Pilathara (TSS) Monthly Fund collection."
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
