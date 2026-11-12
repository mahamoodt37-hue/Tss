package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.ui.components.formatCurrency
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.TssUiState

@Composable
fun FullMembersListScreen(
    uiState: TssUiState,
    isAdmin: Boolean,
    onMemberClick: (Member) -> Unit,
    onAddNewMember: () -> Unit,
    onShareDirectoryText: (List<Member>) -> Unit,
    onShareDirectoryImage: (List<Member>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCurrencyFilter by remember { mutableStateOf("ALL") } // "ALL", "AED", "INR"
    var selectedMonthFilter by remember { mutableStateOf("ALL") } // "ALL", "JAN", ...
    var phoneFilter by remember { mutableStateOf("ALL") } // "ALL", "WITH_PHONE", "NO_PHONE"

    val allMembers = uiState.allMembers

    val filteredMembers = remember(allMembers, searchQuery, selectedCurrencyFilter, selectedMonthFilter, phoneFilter) {
        allMembers.filter { m ->
            val matchesCur = when (selectedCurrencyFilter) {
                "AED" -> m.currency.equals("AED", ignoreCase = true)
                "INR" -> m.currency.equals("INR", ignoreCase = true)
                else -> true
            }
            val matchesMonth = when (selectedMonthFilter) {
                "ALL" -> true
                else -> m.allottedMonth.equals(selectedMonthFilter, ignoreCase = true)
            }
            val matchesPhone = when (phoneFilter) {
                "WITH_PHONE" -> m.mobile.isNotBlank()
                "NO_PHONE" -> m.mobile.isBlank()
                else -> true
            }
            val queryTrimmed = searchQuery.trim()
            val matchesQuery = queryTrimmed.isBlank() ||
                m.name.contains(queryTrimmed, ignoreCase = true) ||
                m.mobile.contains(queryTrimmed, ignoreCase = true) ||
                m.emirateOrCity.contains(queryTrimmed, ignoreCase = true) ||
                m.country.contains(queryTrimmed, ignoreCase = true) ||
                m.allottedMonth.contains(queryTrimmed, ignoreCase = true)

            matchesCur && matchesMonth && matchesPhone && matchesQuery
        }
    }

    val totalAedAmount = remember(filteredMembers) {
        filteredMembers.filter { it.currency.equals("AED", ignoreCase = true) }.sumOf { it.amount.toLong() }
    }
    val totalInrAmount = remember(filteredMembers) {
        filteredMembers.filter { it.currency.equals("INR", ignoreCase = true) }.sumOf { it.amount.toLong() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Header Directory Summary Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Full Members Directory",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "Showing ${filteredMembers.size} of ${allMembers.size} members",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        // Currency Totals Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedCurrencyFilter != "INR") {
                                    Text(
                                        text = "AED $totalAedAmount",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (selectedCurrencyFilter != "AED") {
                                    Text(
                                        text = "₹ $totalInrAmount",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_directory_search_input"),
                placeholder = { Text("Search by name, contact number, emirate...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency filters
                listOf("ALL" to "All Currencies", "AED" to "AED Only", "INR" to "INR Only").forEach { (key, label) ->
                    val isSelected = selectedCurrencyFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCurrencyFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_currency_$key")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Phone filter
                listOf("ALL" to "All", "WITH_PHONE" to "With Phone", "NO_PHONE" to "No Phone").forEach { (key, label) ->
                    val isSelected = phoneFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { phoneFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_phone_$key")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Month filter
                FilterChip(
                    selected = selectedMonthFilter == "ALL",
                    onClick = { selectedMonthFilter = "ALL" },
                    label = { Text("All Months", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_month_ALL")
                )
                MONTHS.forEach { m ->
                    val isSelected = selectedMonthFilter == m
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMonthFilter = m },
                        label = { Text(m, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_month_$m")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Share Bar (WhatsApp & Image share of directory)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // WhatsApp Button
                        Button(
                            onClick = { onShareDirectoryText(filteredMembers) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_share_directory_whatsapp")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share WhatsApp", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Image Button
                        OutlinedButton(
                            onClick = { onShareDirectoryImage(filteredMembers) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_share_directory_image")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Share Image", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Image", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "${filteredMembers.size} members",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Member Cards List
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No members match your search or filter",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(filteredMembers, key = { _, m -> m.id }) { index, member ->
                        MemberDirectoryCard(
                            serialNumber = index + 1,
                            member = member,
                            onClick = { onMemberClick(member) }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Adding New Member (visible to Admin)
        if (isAdmin) {
            FloatingActionButton(
                onClick = onAddNewMember,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_member_directory")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Member")
            }
        }
    }
}

@Composable
fun MemberDirectoryCard(
    serialNumber: Int,
    member: Member,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPhone = member.mobile.isNotBlank()

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("member_directory_card_${member.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Serial + Name + Allotted Month + Amount Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Serial avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$serialNumber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Allotted Month
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Month: ${member.allottedMonth}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (member.emirateOrCity.isNotBlank() || member.country.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            val locationText = listOf(member.emirateOrCity, member.country)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            Text(
                                text = locationText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = formatCurrency(member.currency, member.amount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Contact Number & Direct Action Buttons (Call, WhatsApp, Copy)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (hasPhone) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = member.mobile,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // WhatsApp direct button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF25D366),
                                modifier = Modifier
                                    .clickable { launchMemberWhatsApp(context, member.mobile, member.name) }
                                    .testTag("btn_wa_${member.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "WhatsApp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Call direct button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clickable { launchMemberDialer(context, member.mobile) }
                                    .testTag("btn_call_${member.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Call",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = { copyNumber(context, member.mobile, member.name) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy number",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneDisabled,
                                contentDescription = "No phone",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No contact number recorded",
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun launchMemberDialer(context: Context, mobile: String) {
    if (mobile.isBlank()) return
    val cleaned = mobile.filter { it.isDigit() || it == '+' }
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch phone dialer", Toast.LENGTH_SHORT).show()
    }
}

private fun launchMemberWhatsApp(context: Context, mobile: String, name: String) {
    if (mobile.isBlank()) return
    val digits = mobile.filter { it.isDigit() }
    val waNumber = when {
        digits.length == 9 && (digits.startsWith("50") || digits.startsWith("52") || digits.startsWith("54") || digits.startsWith("55") || digits.startsWith("56") || digits.startsWith("58")) -> "971$digits"
        digits.length == 10 && (digits.startsWith("9") || digits.startsWith("8") || digits.startsWith("7") || digits.startsWith("6")) -> "91$digits"
        else -> digits
    }
    val message = "Assalamu Alaikum $name, greetings from Thajusunna Pilathara (TSS) Gulf Committee."
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$waNumber&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

private fun copyNumber(context: Context, mobile: String, name: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Contact of $name", mobile)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $mobile to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // ignore
    }
}
