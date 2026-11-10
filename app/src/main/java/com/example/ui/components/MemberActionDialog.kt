package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Member
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusUnpaid
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.MemberPaymentStatus
import com.example.ui.viewmodel.MemberWithStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberActionDialog(
    memberWithStatus: MemberWithStatus?,
    defaultCurrency: String,
    selectedMonthIndex: Int,
    knownHolders: List<String>,
    onDismiss: () -> Unit,
    onSavePayment: (isPaid: Boolean, isPending: Boolean, heldBy: String) -> Unit,
    onSaveMember: (Member) -> Unit,
    onDeleteMember: (Long) -> Unit
) {
    val context = LocalContext.current
    val isNewMember = memberWithStatus == null
    val currentMonthName = MONTHS.getOrElse(selectedMonthIndex) { "NOV" }

    var name by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.name ?: "") }
    var mobile by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.mobile ?: "") }
    var emirateOrCity by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.emirateOrCity ?: "") }
    var country by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.country ?: "UAE") }
    var allottedMonth by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.allottedMonth ?: currentMonthName) }
    var amountText by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.amount?.toInt()?.toString() ?: "250") }
    var currency by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.member?.currency ?: defaultCurrency) }

    var heldBy by remember(memberWithStatus) { mutableStateOf(memberWithStatus?.heldBy ?: "Main") }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp)),
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
                            text = if (isNewMember) "Add New Member" else memberWithStatus.member.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isNewMember) {
                            Text(
                                text = "$currentMonthName 2026 Status: ${memberWithStatus.status.label}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("button_dialog_close")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // If existing member, show Quick Payment Recording section
                if (!isNewMember) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Record Payment for $currentMonthName 2026",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Held By field
                            OutlinedTextField(
                                value = heldBy,
                                onValueChange = { heldBy = it },
                                label = { Text("Cash received / held by") },
                                placeholder = { Text("Main or collector name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_held_by")
                            )

                            // Quick Holder Chips
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                knownHolders.take(5).forEach { h ->
                                    val isCurrent = heldBy.equals(h, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .clickable { heldBy = h }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = h,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Status Actions: Paid, Unpaid, Pending
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Mark Paid Button (Green)
                                Button(
                                    onClick = {
                                        onSavePayment(true, false, heldBy)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusPaid
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_mark_paid")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Mark Unpaid Button (Red)
                                Button(
                                    onClick = {
                                        onSavePayment(false, false, heldBy)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusUnpaid
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_mark_unpaid")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Unpaid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Mark Pending Button (Amber)
                                Button(
                                    onClick = {
                                        onSavePayment(false, true, heldBy)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusPending
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_mark_pending")
                                ) {
                                    Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Quick Contact row
                            if (memberWithStatus.member.mobile.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${memberWithStatus.member.mobile}"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Member")
                                    }

                                    TextButton(
                                        onClick = {
                                            val clean = memberWithStatus.member.mobile.replace(Regex("[^0-9]"), "")
                                            val msg = "Assalamu Alaikum ${memberWithStatus.member.name}, reminder regarding TSS monthly fund."
                                            val url = "https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(msg)}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Member Details Form
                Text(
                    text = "Member Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_member_mobile")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emirateOrCity,
                        onValueChange = { emirateOrCity = it },
                        label = { Text("Emirate / City") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_member_emirate")
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_member_country")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Allotted Month dropdown
                ExposedDropdownMenuBox(
                    expanded = monthMenuExpanded,
                    onExpandedChange = { monthMenuExpanded = !monthMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = allottedMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Allotted Month (or ALL)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_allotted_month")
                    )

                    ExposedDropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        listOf("ALL") + MONTHS.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    allottedMonth = m
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Amount & Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_member_amount")
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyMenuExpanded,
                        onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_member_currency")
                        )

                        ExposedDropdownMenu(
                            expanded = currencyMenuExpanded,
                            onDismissRequest = { currencyMenuExpanded = false }
                        ) {
                            listOf("AED", "INR").forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text(cur) },
                                    onClick = {
                                        currency = cur
                                        currencyMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Member & Delete buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isNewMember) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StatusUnpaid
                            ),
                            modifier = Modifier.testTag("button_delete_member")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = StatusUnpaid, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Member", color = StatusUnpaid, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val member = Member(
                                        id = memberWithStatus?.member?.id ?: 0L,
                                        name = name.trim(),
                                        emirateOrCity = emirateOrCity.trim(),
                                        country = country.trim(),
                                        mobile = mobile.trim(),
                                        allottedMonth = allottedMonth,
                                        amount = amountText.toDoubleOrNull() ?: 0.0,
                                        currency = currency
                                    )
                                    onSaveMember(member)
                                    onDismiss()
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.testTag("button_save_member")
                        ) {
                            Text(if (isNewMember) "+ Add Member" else "Save Changes")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && memberWithStatus != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Member?") },
            text = { Text("Are you sure you want to remove ${memberWithStatus.member.name}? This will delete all their recorded payments.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(memberWithStatus.member.id)
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusUnpaid)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
