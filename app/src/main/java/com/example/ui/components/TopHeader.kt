package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldMidnight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.TealDark
import com.example.ui.viewmodel.MONTHS

@Composable
fun TopHeader(
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    selectedMonthIndex: Int,
    onMonthChange: (Int) -> Unit,
    onShareCsv: () -> Unit,
    onOpenSheetDialog: () -> Unit,
    isSheetLinked: Boolean,
    userEmail: String? = null,
    onSignInRequest: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var userMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldMidnight, TealDark)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Organization Emblem with luxury golden rim
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tss_emblem_circle),
                            contentDescription = "TSS Emblem",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Thajusunna Pilathara",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Monthly Fund Collection · 2026",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Viewer / Admin status pill
                    if (userEmail.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldAccent.copy(alpha = 0.2f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldAccent, GoldGlow))),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSignInRequest() }
                                .testTag("button_admin_login_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Admin Login",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Google Sheet sync button
                    IconButton(
                        onClick = onOpenSheetDialog,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("button_open_sheet")
                    ) {
                        Icon(
                            imageVector = if (isSheetLinked) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = "Google Sheet Sync",
                            tint = if (isSheetLinked) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share CSV button
                    IconButton(
                        onClick = onShareCsv,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("button_share_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export and Share CSV",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // User Profile / Sign Out button
                    Box {
                        IconButton(
                            onClick = { userMenuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("button_user_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Account",
                                tint = if (!userEmail.isNullOrBlank()) GoldAccent else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = userMenuExpanded,
                            onDismissRequest = { userMenuExpanded = false }
                        ) {
                            if (!userEmail.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Signed in as Admin",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                            Text(
                                                text = userEmail,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sign Out",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        onSignOut()
                                    },
                                    modifier = Modifier.testTag("button_sign_out")
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Viewer Mode (Live)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Real-time updates enabled",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = { },
                                    enabled = false
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Login,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Sign in as Admin",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        userMenuExpanded = false
                                        onSignInRequest()
                                    },
                                    modifier = Modifier.testTag("button_menu_admin_sign_in")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector Bar (Currency switcher + Month selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Currency Switcher
                CurrencySegmentedSwitch(
                    selectedCurrency = selectedCurrency,
                    onCurrencySelected = onCurrencyChange,
                    modifier = Modifier.testTag("currency_segmented_bar")
                )

                // Month Selector dropdown button
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { monthMenuExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("button_select_month"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${MONTHS.getOrElse(selectedMonthIndex) { "NOV" }} 2026",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        MONTHS.forEachIndexed { index, monthName ->
                            val isSelected = index == selectedMonthIndex
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$monthName 2026" + if (index == 10) " (Active)" else "",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onMonthChange(index)
                                    monthMenuExpanded = false
                                },
                                modifier = Modifier.testTag("month_option_$monthName")
                            )
                        }
                    }
                }
            }
        }
    }
}
