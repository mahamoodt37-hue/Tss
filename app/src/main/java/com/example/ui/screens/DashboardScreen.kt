package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.formatCurrency
import com.example.ui.theme.EmeraldForest
import com.example.ui.theme.EmeraldMidnight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintAccent
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusUnpaid
import com.example.ui.theme.TealDark
import com.example.ui.viewmodel.CurrencyBalance
import com.example.ui.viewmodel.MONTHS
import com.example.ui.viewmodel.MonthCollectionStats
import com.example.ui.viewmodel.TssUiState

@Composable
fun DashboardScreen(
    uiState: TssUiState,
    onOpenSheetDialog: () -> Unit,
    onExportCsv: () -> Unit,
    onInitiateTransfer: (holder: String, currency: String, amount: Double) -> Unit,
    onNavigateToTab: (String) -> Unit = {},
    onAddNewMember: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val monthName = MONTHS.getOrElse(uiState.selectedMonthIndex) { "NOV" }
    val cur = uiState.selectedCurrency

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Welfare Treasury Visual Banner
        item {
            HeroTreasuryBanner(
                currentMonth = monthName,
                modifier = Modifier.testTag("banner_hero_treasury")
            )
        }

        // 2. Grand Treasury Vault Card (Selected & Dual Currency)
        item {
            GrandTreasuryCard(
                currency = cur,
                aedBalance = uiState.aedBalance,
                inrBalance = uiState.inrBalance,
                onQuickTransfer = { holder, c, amt ->
                    onInitiateTransfer(holder, c, amt)
                },
                modifier = Modifier.testTag("card_grand_treasury")
            )
        }

        // 3. Quick Action Buttons Grid
        item {
            QuickActionsGrid(
                onAddMember = onAddNewMember,
                onTransfers = { onNavigateToTab("transfers") },
                onExtra = { onNavigateToTab("extra") },
                onExport = onExportCsv,
                modifier = Modifier.testTag("grid_quick_actions")
            )
        }

        // 4. Monthly Collection Performance & Progress Gauge Card
        item {
            EnhancedCollectionProgressCard(
                monthName = monthName,
                currency = cur,
                stats = uiState.monthStats,
                onViewMembers = { onNavigateToTab("members") },
                modifier = Modifier.testTag("card_month_collection")
            )
        }

        // 5. Cash in Field (with Collectors) Section
        item {
            val holders = if (cur == "AED") uiState.aedBalance.holders else uiState.inrBalance.holders
            EnhancedCashWithOthersCard(
                currency = cur,
                holders = holders,
                onTransferClick = { holder, amount ->
                    onInitiateTransfer(holder, cur, amount)
                },
                modifier = Modifier.testTag("card_cash_with_others")
            )
        }

        // 6. Cloud Backup & Google Sheets Integration Card
        item {
            CloudSyncStatusCard(
                isLinked = uiState.googleSheetUrl.isNotBlank(),
                onOpenSheetDialog = onOpenSheetDialog,
                onExportCsv = onExportCsv,
                modifier = Modifier.testTag("card_cloud_sync_status")
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 1. Hero Treasury Banner with Architectural Artwork & Glowing Badges
 */
@Composable
fun HeroTreasuryBanner(
    currentMonth: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            // Background Artwork
            Image(
                painter = painterResource(id = R.drawable.img_tss_banner_1794280541542),
                contentDescription = "TSS Community Treasury Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                EmeraldMidnight.copy(alpha = 0.5f),
                                EmeraldMidnight.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Content inside the hero banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = GoldAccent.copy(alpha = 0.25f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldAccent, GoldGlow)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(GoldGlow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Collection Cycle: $currentMonth 2026",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Pilathra Welfare Treasury",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Collective monthly giving supporting members and community",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * 2. Grand Treasury Vault Card (Displays Total, Main Account, Cash with Collectors)
 */
@Composable
fun GrandTreasuryCard(
    currency: String,
    aedBalance: CurrencyBalance,
    inrBalance: CurrencyBalance,
    onQuickTransfer: (String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBalance = if (currency == "AED") aedBalance else inrBalance
    val otherBalance = if (currency == "AED") inrBalance else aedBalance
    val otherCurrency = if (currency == "AED") "INR" else "AED"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = EmeraldMidnight),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(TealDark, GoldAccent.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(EmeraldForest.copy(alpha = 0.8f), EmeraldMidnight),
                        radius = 800f
                    )
                )
                .padding(18.dp)
        ) {
            // Top Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = GoldGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Total Treasury Balance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$currency Funds",
                            fontSize = 10.sp,
                            color = MintAccent
                        )
                    }
                }

                // Secondary Currency pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$otherCurrency: ${formatCurrency(otherCurrency, otherBalance.total)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Massive Total Currency display
            Text(
                text = formatCurrency(currency, activeBalance.total),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Two Split Glassmorphic Cards (Main Account vs Cash with Others)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Account
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MintAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Main Vault",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(currency, activeBalance.mainAccount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Cash with Others
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeBalance.cashWithOthers > 0) GoldAccent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (activeBalance.cashWithOthers > 0) Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.4f), GoldAccent)) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = GoldGlow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "With Collectors",
                                fontSize = 11.sp,
                                color = GoldLight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(currency, activeBalance.cashWithOthers),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGlow
                        )
                    }
                }
            }

            // Quick Handover indicator if cash is outside
            if (activeBalance.cashWithOthers > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val firstHolder = activeBalance.holders.entries.firstOrNull { it.value > 0 }
                if (firstHolder != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onQuickTransfer(firstHolder.key, currency, firstHolder.value)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${firstHolder.key} has ${formatCurrency(currency, firstHolder.value)}",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Deposit Now →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGlow
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Quick Action Shortcuts Grid
 */
@Composable
fun QuickActionsGrid(
    onAddMember: () -> Unit,
    onTransfers: () -> Unit,
    onExtra: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Quick Operations",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    label = "Add Member",
                    tint = TealDark,
                    bgColor = TealDark.copy(alpha = 0.12f),
                    onClick = onAddMember,
                    tag = "quick_action_add_member"
                )

                QuickActionButton(
                    icon = Icons.Default.SwapHoriz,
                    label = "Handover",
                    tint = GoldAccent,
                    bgColor = GoldAccent.copy(alpha = 0.15f),
                    onClick = onTransfers,
                    tag = "quick_action_handover"
                )

                QuickActionButton(
                    icon = Icons.Default.VolunteerActivism,
                    label = "Special Fund",
                    tint = Color(0xFF8B5CF6),
                    bgColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    onClick = onExtra,
                    tag = "quick_action_special_fund"
                )

                QuickActionButton(
                    icon = Icons.Default.FileDownload,
                    label = "Export CSV",
                    tint = Color(0xFF2563EB),
                    bgColor = Color(0xFF2563EB).copy(alpha = 0.12f),
                    onClick = onExport,
                    tag = "quick_action_export_csv"
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 4. Enhanced Monthly Collection Progress Card
 */
@Composable
fun EnhancedCollectionProgressCard(
    monthName: String,
    currency: String,
    stats: MonthCollectionStats,
    onViewMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = stats.totalDueMembers.coerceAtLeast(1).toFloat()
    val paidFraction = (stats.paidCount / total).coerceIn(0f, 1f)
    val inProgressFraction = (stats.inProgressCount / total).coerceIn(0f, 1f)
    val pendingFraction = (stats.pendingCount / total).coerceIn(0f, 1f)
    val unpaidFraction = (stats.unpaidCount / total).coerceIn(0f, 1f)

    val percentCollected = if (stats.totalExpected > 0) {
        ((stats.totalCollected / stats.totalExpected) * 100).toInt()
    } else 0

    val remainingAmount = (stats.totalExpected - stats.totalCollected).coerceAtLeast(0.0)

    val animPaid by animateFloatAsState(targetValue = paidFraction, animationSpec = tween(600), label = "paid")
    val animProgress by animateFloatAsState(targetValue = inProgressFraction, animationSpec = tween(600), label = "progress")
    val animPending by animateFloatAsState(targetValue = pendingFraction, animationSpec = tween(600), label = "pending")
    val animUnpaid by animateFloatAsState(targetValue = unpaidFraction, animationSpec = tween(600), label = "unpaid")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with percentage chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$monthName 2026 Collection Progress",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stats.totalDueMembers} members scheduled in $currency",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (percentCollected >= 80) StatusPaid.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$percentCollected% Collected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentCollected >= 80) StatusPaid else GoldAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (animPaid > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(animPaid.coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(StatusPaid)
                        )
                    }
                    if (animProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(animProgress.coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(StatusInProgress)
                        )
                    }
                    if (animPending > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(animPending.coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(StatusPending)
                        )
                    }
                    if (animUnpaid > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(animUnpaid.coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(StatusUnpaid)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge Tiles Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMetricTile(
                    label = "Paid",
                    count = stats.paidCount,
                    color = StatusPaid,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )

                StatusMetricTile(
                    label = "In Progress",
                    count = stats.inProgressCount,
                    color = StatusInProgress,
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier.weight(1f)
                )

                StatusMetricTile(
                    label = "Pending",
                    count = stats.pendingCount,
                    color = StatusPending,
                    icon = Icons.Default.Pending,
                    modifier = Modifier.weight(1f)
                )

                StatusMetricTile(
                    label = "Unpaid",
                    count = stats.unpaidCount,
                    color = StatusUnpaid,
                    icon = Icons.Default.Error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Collected so far",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(currency, stats.totalCollected),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusPaid
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Goal: ${formatCurrency(currency, stats.totalExpected)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Remaining: ${formatCurrency(currency, remainingAmount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remainingAmount > 0) GoldAccent else StatusPaid
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onViewMembers,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_view_due_members")
            ) {
                Text("Manage $monthName Members (${stats.totalDueMembers})", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatusMetricTile(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = count.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 5. Cash with Others (Field Collectors)
 */
@Composable
fun EnhancedCashWithOthersCard(
    currency: String,
    holders: Map<String, Double>,
    onTransferClick: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cash with Collectors ($currency)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (holders.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = GoldAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${holders.size} active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (holders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusPaid,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All collected cash has been deposited to the Main Vault!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                holders.forEach { (holder, amount) ->
                    val initials = holder.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                        .ifBlank { "T" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = holder,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatCurrency(currency, amount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }

                        Button(
                            onClick = { onTransferClick(holder, amount) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("button_transfer_${holder.replace(" ", "_")}")
                        ) {
                            Text("Handover", fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

/**
 * 6. Cloud Backup & Sheet Integration Card
 */
@Composable
fun CloudSyncStatusCard(
    isLinked: Boolean,
    onOpenSheetDialog: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cloud Sync & Official Reports",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isLinked) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = StatusPaidBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Sync ON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusPaid)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenSheetDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_link_sheet_dashboard")
                ) {
                    Icon(
                        imageVector = if (isLinked) Icons.Default.CloudDone else Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLinked) "Sheet Settings" else "Link Google Sheet", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_export_csv_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Backup all member statuses, payments, and balances securely. Share reports directly to WhatsApp, Drive, or print.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
