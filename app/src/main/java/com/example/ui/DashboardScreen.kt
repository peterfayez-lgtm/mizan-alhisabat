package com.example.ui

import android.Manifest
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppDesignPreset
import com.example.data.BurnRateStatus
import com.example.data.DashboardMetrics
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinancialTransaction
import com.example.data.FreeIntegrationsHelper
import com.example.data.MonthlyBarPoint
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import com.example.ui.theme.DangerRose
import com.example.ui.theme.EssentialBlue
import com.example.ui.theme.LuxuryPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DonutSliceItem(
    val category: String,
    val amount: Double,
    val percentage: Int,
    val sweepFraction: Float,
    val color: Color,
    val txCount: Int
)

private val ExpenseDonutPalette = listOf(
    Color(0xFFF2992E), // كهرماني دافئ (Spend Orange from reference)
    Color(0xFF5B8DEF), // أزرق ملكي هادئ (Royal Blue from reference)
    Color(0xFF2B3A4A), // كحلي داكن أنيق (Deep Slate Navy from reference)
    Color(0xFF8C6239), // برونزي بيج دافئ (Warm Bronze)
    Color(0xFFD94F4F)  // مرجاني هادئ
)

private val IncomeDonutPalette = listOf(
    Color(0xFF5B8DEF), // أزرق ملكي هادئ (Earning Blue from reference)
    Color(0xFF2E9E6B), // أخضر زمردي (Safe Emerald)
    Color(0xFFF2992E), // كهرماني ذهبي (Warm Amber)
    Color(0xFF2B3A4A), // كحلي داكن أنيق (Deep Slate Navy)
    Color(0xFF8C6239)  // برونزي دافئ
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    transactions: List<FinancialTransaction> = emptyList(),
    burnRates: List<BurnRateStatus> = emptyList(),
    savingsGoals: List<LocalSavingsGoalEntity> = emptyList(),
    commitments: List<LocalCommitmentEntity> = emptyList(),
    isPrivacyMasked: Boolean = false,
    designPreset: AppDesignPreset = AppDesignPreset.WARM_OASIS,
    simpleMinimalistUiMode: Boolean = true,
    whatsappPhoneNumber: String = "",
    whatsappMidnightAutoEnabled: Boolean = false,
    whatsappScheduleLabelAr: String = "١٢:٠٠ منتصف الليل يومياً",
    hasAvailableUpdate: Boolean = false,
    availableUpdateVersionName: String = "",
    onToggleMinimalistUiMode: () -> Unit = {},
    onSelectDesignPreset: (AppDesignPreset) -> Unit = {},
    onOpenDesignStudio: () -> Unit = {},
    onOpenDailyLimitControl: () -> Unit = {},
    onOpenWhatsAppDailyReport: () -> Unit = {},
    onOpenUpdateCenter: () -> Unit = {},
    onOpenQuickCalculator: () -> Unit = {},
    onOpenSmartIntegrationsHub: () -> Unit = {},
    onNavigateToPersonalDebts: () -> Unit = {},
    onPayCommitmentInstallment: (LocalCommitmentEntity) -> Unit = {},
    onQuickNavigateToTransactions: () -> Unit,
    onResetSeedData: () -> Unit,
    onClearAllData: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showMoreInsights by remember { mutableStateOf(false) }

    // 0 = أكثر البنود مصروفاً (Expenses), 1 = أكثر البنود إيراداً (Income)
    var selectedDonutMode by remember { mutableIntStateOf(0) }

    val expenseSlices = remember(transactions) {
        computeCategorySlices(
            transactions = transactions,
            targetType = "expense",
            palette = ExpenseDonutPalette
        )
    }

    val incomeSlices = remember(transactions) {
        computeCategorySlices(
            transactions = transactions,
            targetType = "income",
            palette = IncomeDonutPalette
        )
    }

    val activeSlices = if (selectedDonutMode == 0) expenseSlices else incomeSlices

    val healthReport = remember(metrics, burnRates, savingsGoals) {
        FreeIntegrationsHelper.computeFinancialHealthReport(
            metrics = metrics,
            burnRates = burnRates,
            savingsGoals = savingsGoals
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            FreeIntegrationsHelper.sendSmartLocalNotification(
                context = context,
                title = "🛡️ ميزان المالي: الحد اليومي الآمن اليوم ${metrics.dailySafeToSpend.toInt()} ج.م",
                message = "تقييم صحتك المالية: ${healthReport.scoreOutOf100}/100 (${healthReport.gradeTitleAr}). المتبقي من ميزانية الشهر ${metrics.remainingBudget.toInt()} ج.م."
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. بطاقة الرصيد الإجمالي الفاخرة (Hero Balance Card - Warm White & Beige)
        HarmoniousHeroBalanceCard(
            metrics = metrics,
            isPrivacyMasked = isPrivacyMasked,
            onOpenDailyLimitControl = onOpenDailyLimitControl
        )

        // 2. بطاقة الداش بورد الدائرية الجديدة (Donut Chart Card) لأكثر البنود مصروفاً وأكثرها إيراداً
        DonutAnalyticsDashboardCard(
            metrics = metrics,
            selectedMode = selectedDonutMode,
            onSelectMode = { selectedDonutMode = it },
            slices = activeSlices,
            isPrivacyMasked = isPrivacyMasked
        )

        // 3. قسم أعلى البنود والتزامات وفواتير الشهر (على غرار Unpaid Bill في التصميم المرفق)
        CategoriesAndBillsTabbedSection(
            selectedDonutMode = selectedDonutMode,
            activeSlices = activeSlices,
            commitments = commitments,
            isPrivacyMasked = isPrivacyMasked,
            onPayCommitmentInstallment = onPayCommitmentInstallment,
            onNavigateToAllCommitments = onNavigateToPersonalDebts
        )

        // 4. صف الإجراءات الذكية السريعة (4 أزرار متناسقة ومرتبة)
        QuickActionsFourGrid(
            whatsappConfigured = whatsappPhoneNumber.isNotBlank() && whatsappMidnightAutoEnabled,
            onOpenQuickCalculator = onOpenQuickCalculator,
            onOpenWhatsAppDailyReport = onOpenWhatsAppDailyReport,
            onOpenSmartIntegrationsHub = onOpenSmartIntegrationsHub,
            onNavigateToPersonalDebts = onNavigateToPersonalDebts
        )

        // 5. توزيع السيولة عبر المحافظ (كاش، بنك، محفظة إلكترونية) بتصميم بيج وأبيض هادئ
        CalmWalletsRow(
            walletBalances = metrics.walletBalances,
            isPrivacyMasked = isPrivacyMasked
        )

        // 6. أحدث المعاملات المالية (آخر 5 معاملات مع زر عرض الكل)
        RecentTransactionsCleanCard(
            transactions = transactions.take(5),
            isPrivacyMasked = isPrivacyMasked,
            onViewAllClick = onQuickNavigateToTransactions
        )

        // 7. قسم قابل للطي للتحليلات الإضافية وإدارة البيانات (مغلق افتراضياً لمنع أي ازدحام)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreInsights = !showMoreInsights }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .testTag("toggle_minimalist_ui_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مؤشر الصحة المالية والأدوات الإضافية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "التقييم المالي (${healthReport.scoreOutOf100}/100) • المظهر • إدارة البيانات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showMoreInsights) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showMoreInsights) "طي التفاصيل" else "عرض التفاصيل",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showMoreInsights) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // مؤشر الصحة المالية المبسط
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("financial_health_score_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الصحة المالية: ${healthReport.scoreOutOf100}/100 (${healthReport.gradeTitleAr})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = healthReport.actionableRecommendations.firstOrNull() ?: "حالتك المالية متزنة ومستقرة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "إشعار ذكي",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                FreeIntegrationsHelper.sendSmartLocalNotification(
                                                    context = context,
                                                    title = "🛡️ ميزان المالي: الحد اليومي الآمن اليوم ${metrics.dailySafeToSpend.toInt()} ج.م",
                                                    message = "تقييم صحتك المالية: ${healthReport.scoreOutOf100}/100 (${healthReport.gradeTitleAr})."
                                                )
                                            }
                                        }
                                        .padding(4.dp)
                                        .testTag("send_smart_notification_button")
                                )
                            }
                        }

                        // مقارنة الأساسي والكمالي
                        EssentialVsLuxuryCard(metrics = metrics)

                        // الرسم البياني لـ 6 شهور
                        SixMonthsBarChartCard(bars = metrics.lastSixMonthsBars)

                        // أزرار المظهر والتحديثات وإدارة البيانات التجريبية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenDesignStudio,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_design_studio_from_banner_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الألوان والمظهر", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenUpdateCenter,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_update_center_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (hasAvailableUpdate) "تحديث v$availableUpdateVersionName" else "مركز التحديثات",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClearAllData,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_all_data_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تصفير البيانات", style = MaterialTheme.typography.labelSmall, color = DangerRose)
                            }
                            OutlinedButton(
                                onClick = onResetSeedData,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_seed_data_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بيانات تجريبية", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // مساحة سفلية إضافية لعدم تغطية الزر العائم (FAB) لآخر عنصر
        Spacer(modifier = Modifier.height(72.dp))
    }
}

/**
 * بطاقة الداش بورد الدائرية (Donut Chart Dashboard Card) المستوحاة من الصورة المرفقة:
 * - مفتاح تبديل أنيق بين «أكثر البنود مصروفاً» و«أكثر البنود إيراداً»
 * - حلقة دائرية مقسمة مع النسب المئوية مكتوبة بداخل كل قطاع
 * - الرصيد المتاح في المنتصف
 * - ٣ أعمدة سفلية: الإيرادات (Earning) | المصروفات (Spend) | المتاح (Available)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonutAnalyticsDashboardCard(
    metrics: DashboardMetrics,
    selectedMode: Int,
    onSelectMode: (Int) -> Unit,
    slices: List<DonutSliceItem>,
    isPrivacyMasked: Boolean
) {
    val earningBlue = Color(0xFF5B8DEF)
    val spendOrange = Color(0xFFF2992E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("donut_analytics_dashboard_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. مفتاح التبديل العلوي داخل البطاقة بين المصروفات والإيرادات
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isExpense = selectedMode == 0
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(0) }
                            .testTag("donut_mode_expense_tab"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExpense) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isExpense) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpense) spendOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أكثر البنود مصروفاً",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(1) }
                            .testTag("donut_mode_income_tab"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isExpense) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (!isExpense) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (!isExpense) earningBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أكثر البنود إيراداً",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. الحلقة الدائرية المقسمة (Donut Chart) مع النسب المئوية داخل كل قطاع
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 44.dp.toPx()
                    val diameter = size.minDimension - strokeWidthPx
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    val radius = diameter / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    if (slices.isEmpty()) {
                        drawArc(
                            color = Color(0xFFE8DFD1),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                        )
                    } else {
                        val labelPaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = Paint.Align.CENTER
                            textSize = 13.dp.toPx()
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }

                        var currentStartAngle = -90f
                        slices.forEach { slice ->
                            val sweepAngle = (slice.sweepFraction * 360f).coerceAtLeast(1f)
                            drawArc(
                                color = slice.color,
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                            )

                            // كتابة النسبة المئوية في منتصف القطاع إذا كان القطاع كبيراً بما يكفي
                            if (slice.percentage >= 8) {
                                val midAngleDeg = currentStartAngle + (sweepAngle / 2f)
                                val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                                val textX = center.x + (radius * cos(midAngleRad)).toFloat()
                                val fontMetrics = labelPaint.fontMetrics
                                val textOffsetY = (fontMetrics.descent + fontMetrics.ascent) / 2f
                                val textY = center.y + (radius * sin(midAngleRad)).toFloat() - textOffsetY

                                drawContext.canvas.nativeCanvas.drawText(
                                    "${slice.percentage}%",
                                    textX,
                                    textY,
                                    labelPaint
                                )
                            }

                            currentStartAngle += sweepAngle
                        }
                    }
                }

                // أيقونة خفيفة في قلب الدائرة الفارغة
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (selectedMode == 0) "المصروفات" else "الإيرادات",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. وسيلة إيضاح سريعة (Legend Chips) لربط كل لون باسم البند ونسبته
            if (slices.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEach { slice ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Text(
                                    text = "${slice.category} (${slice.percentage}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. الرصيد المتاح في المنتصف كما في الصورة تماماً
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatCurrency(metrics.netBalance, isPrivacyMasked),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الرصيد المتاح",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // 5. الأعمدة الثلاثة السفلية: الإيرادات (Earning) | المصروفات (Spend) | المتاح (Available)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutSummaryColumn(
                    labelAr = "الإيرادات",
                    amountText = formatCurrency(metrics.currentMonthIncome, isPrivacyMasked),
                    valueColor = earningBlue,
                    modifier = Modifier.weight(1f)
                )
                DonutSummaryColumn(
                    labelAr = "المصروفات",
                    amountText = formatCurrency(metrics.currentMonthExpense, isPrivacyMasked),
                    valueColor = spendOrange,
                    modifier = Modifier.weight(1f)
                )
                DonutSummaryColumn(
                    labelAr = "المتاح",
                    amountText = formatCurrency(metrics.netBalance, isPrivacyMasked),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DonutSummaryColumn(
    labelAr: String,
    amountText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = labelAr,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * القسم التفاعلي أسفل البطاقة الدائرية على غرار قسم (Unpaid Bill: All | Upcoming | Previous) في الصورة المرفقة:
 * يتيح التبديل بين:
 * - أعلى البنود (مصروفات أو إيرادات حسب اختيار الحلقة الدائرية)
 * - التزامات وفواتير قادمة (Upcoming) مع زر برتقالي «ادفع الآن» (Pay Now)
 * - كل الالتزامات (All)
 * - المدفوعة (Previous)
 */
@Composable
private fun CategoriesAndBillsTabbedSection(
    selectedDonutMode: Int,
    activeSlices: List<DonutSliceItem>,
    commitments: List<LocalCommitmentEntity>,
    isPrivacyMasked: Boolean,
    onPayCommitmentInstallment: (LocalCommitmentEntity) -> Unit,
    onNavigateToAllCommitments: () -> Unit
) {
    // 0 = أعلى البنود, 1 = الكل (All), 2 = القادمة (Upcoming), 3 = المدفوعة (Previous)
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val payNowOrange = Color(0xFFF2992E)

    val tabs = listOf(
        Triple(0, if (selectedDonutMode == 0) "أعلى المصروفات" else "أعلى الإيرادات", "tab_top_categories"),
        Triple(1, "كل الفواتير", "tab_bills_all"),
        Triple(2, "القادمة", "tab_bills_upcoming"),
        Triple(3, "المدفوعة", "tab_bills_previous")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("categories_and_bills_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // عنوان القسم الرئيسي
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedSubTab == 0) {
                    if (selectedDonutMode == 0) "تفاصيل أكثر البنود مصروفاً" else "تفاصيل أكثر البنود إيراداً"
                } else {
                    "الفواتير والالتزامات المستحقة"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (selectedSubTab != 0) {
                TextButton(onClick = onNavigateToAllCommitments) {
                    Text(
                        text = "إدارة الالتزامات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // شريط التبويبات السفلي بخط كهرماني تحت التبويب النشط (كما في الصورة All | Upcoming | Previous)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (index, title, tag) ->
                    val isSelected = selectedSubTab == index
                    Column(
                        modifier = Modifier
                            .clickable { selectedSubTab = index }
                            .padding(vertical = 4.dp)
                            .testTag(tag),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) payNowOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) payNowOrange else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }

        // محتوى التبويب المختار
        if (selectedSubTab == 0) {
            if (activeSlices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedDonutMode == 0) {
                            "لا توجد مصروفات مسجلة حتى الآن لعرض أعلى البنود."
                        } else {
                            "لا توجد إيرادات مسجلة حتى الآن لعرض أعلى البنود."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeSlices.forEach { slice ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // أيقونة مربعة ملونة ناعمة الحواف كما في الصورة
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(slice.color.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = slice.color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = slice.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${slice.txCount} معاملة • ${formatCurrency(slice.amount, isPrivacyMasked)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // كبسولة النسبة المئوية البارزة على غرار الزر في الصورة
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = slice.color
                                ) {
                                    Text(
                                        text = "${slice.percentage}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // عرض الفواتير والالتزامات (All / Upcoming / Previous) مع زر "ادفع الآن" (Pay Now)
            val filteredCommitments = remember(commitments, selectedSubTab) {
                when (selectedSubTab) {
                    2 -> commitments.filter { it.paidInstallmentsCount < it.totalInstallmentsCount }
                    3 -> commitments.filter { it.paidInstallmentsCount >= it.totalInstallmentsCount || it.paidInstallmentsCount > 0 }
                    else -> commitments
                }
            }

            if (filteredCommitments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لا توجد التزامات أو فواتير في هذا التبويب حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = onNavigateToAllCommitments,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إضافة التزام أو فاتورة جديدة", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredCommitments.take(5).forEachIndexed { idx, cmt ->
                        val isCompleted = cmt.paidInstallmentsCount >= cmt.totalInstallmentsCount
                        val iconColor = ExpenseDonutPalette[idx % ExpenseDonutPalette.size]

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(iconColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.EventRepeat,
                                            contentDescription = null,
                                            tint = if (isCompleted) SafeGreen else iconColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = cmt.decryptedTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isCompleted) {
                                                "مكتمل السداد (${cmt.paidInstallmentsCount}/${cmt.totalInstallmentsCount})"
                                            } else {
                                                "بانتظار السداد • يوم ${cmt.dueDayOfMonth} • ${formatCurrency(cmt.monthlyInstallment, isPrivacyMasked)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (!isCompleted) {
                                    Button(
                                        onClick = { onPayCommitmentInstallment(cmt) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = payNowOrange,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "ادفع الآن",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "تم السداد",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SafeGreen,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
}

private fun computeCategorySlices(
    transactions: List<FinancialTransaction>,
    targetType: String,
    palette: List<Color>
): List<DonutSliceItem> {
    val filtered = transactions.filter { it.type == targetType && it.amount > 0.0 }
    if (filtered.isEmpty()) return emptyList()

    val totalAmount = filtered.sumOf { it.amount }.coerceAtLeast(1.0)
    val grouped = filtered
        .groupBy { it.category.ifBlank { "عام" } }
        .map { (cat, list) ->
            Triple(cat, list.sumOf { it.amount }, list.size)
        }
        .sortedByDescending { it.second }

    val topItems = if (grouped.size <= 4) {
        grouped
    } else {
        val firstThree = grouped.take(3)
        val remainingAmount = grouped.drop(3).sumOf { it.second }
        val remainingCount = grouped.drop(3).sumOf { it.third }
        firstThree + Triple("أخرى", remainingAmount, remainingCount)
    }

    val rawPercentages = topItems.map { (_, amt, _) ->
        ((amt / totalAmount) * 100.0).roundToInt().coerceAtLeast(1)
    }.toMutableList()

    // ضبط المجموع ليكون 100% بالضبط كما في الصورة المرفقة
    val sumPct = rawPercentages.sum()
    if (sumPct != 100 && rawPercentages.isNotEmpty()) {
        rawPercentages[0] = (rawPercentages[0] + (100 - sumPct)).coerceAtLeast(1)
    }

    return topItems.mapIndexed { index, (cat, amt, count) ->
        DonutSliceItem(
            category = cat,
            amount = amt,
            percentage = rawPercentages[index],
            sweepFraction = (amt / totalAmount).toFloat(),
            color = palette[index % palette.size],
            txCount = count
        )
    }
}

@Composable
private fun HarmoniousHeroBalanceCard(
    metrics: DashboardMetrics,
    isPrivacyMasked: Boolean,
    onOpenDailyLimitControl: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safe_to_spend_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // السطر العلوي: العنوان + عدد الأيام المتبقية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "الرصيد الإجمالي الصافي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "باقي ${metrics.remainingDaysInMonth} يوم للراتب",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // الرصيد الإجمالي بخط كبير وواضح
            Text(
                text = formatCurrency(metrics.netBalance, isPrivacyMasked),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // كبسولتا الدخل والمصروفات بتصميم متناظر أنيق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // كبسولة الدخل
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SafeGreen.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إجمالي الدخل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(metrics.currentMonthIncome, isPrivacyMasked),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // كبسولة المصروفات
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DangerRose.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = DangerRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "المصروفات",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(metrics.currentMonthExpense, isPrivacyMasked),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRose,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // شريط الحد اليومي الآمن للصرف (مدمج بأناقة داخل البطاقة مع إمكانية التعديل بضغطة)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenDailyLimitControl() }
                    .testTag("open_daily_limit_control_button"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "الحد اليومي الآمن للصرف:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${formatCurrency(metrics.dailySafeToSpend, isPrivacyMasked)} / يوم",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "تعديل الحد اليومي",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsFourGrid(
    whatsappConfigured: Boolean,
    onOpenQuickCalculator: () -> Unit,
    onOpenWhatsAppDailyReport: () -> Unit,
    onOpenSmartIntegrationsHub: () -> Unit,
    onNavigateToPersonalDebts: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionTile(
            title = "حاسبة سريعة",
            icon = Icons.Default.Calculate,
            accentColor = MaterialTheme.colorScheme.primary,
            tag = "open_quick_calculator_button",
            onClick = onOpenQuickCalculator,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            title = if (whatsappConfigured) "واتساب ✓" else "تقرير واتساب",
            icon = Icons.Default.NotificationsActive,
            accentColor = SafeGreen,
            tag = "open_whatsapp_daily_report_button",
            onClick = onOpenWhatsAppDailyReport,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            title = "الربط والبنوك",
            icon = Icons.Default.Hub,
            accentColor = MaterialTheme.colorScheme.secondary,
            tag = "open_smart_integrations_hub_button",
            onClick = onOpenSmartIntegrationsHub,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            title = "الجمعيات والديون",
            icon = Icons.Default.Savings,
            accentColor = MaterialTheme.colorScheme.tertiary,
            tag = "open_personal_debts_shortcut_button",
            onClick = onNavigateToPersonalDebts,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalmWalletsRow(
    walletBalances: Map<String, Double>,
    isPrivacyMasked: Boolean
) {
    val cash = walletBalances["cash"] ?: 0.0
    val bank = walletBalances["bank"] ?: 0.0
    val ewallet = walletBalances["ewallet"] ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "توزيع السيولة حسب المحفظة",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WalletMiniCard(
                label = "كاش نقدي",
                amount = cash,
                icon = Icons.Default.Payments,
                isPrivacyMasked = isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "حساب بنكي",
                amount = bank,
                icon = Icons.Default.AccountBalance,
                isPrivacyMasked = isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "محفظة إلكترونية",
                amount = ewallet,
                icon = Icons.Default.PhoneAndroid,
                isPrivacyMasked = isPrivacyMasked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WalletMiniCard(
    label: String,
    amount: Double,
    icon: ImageVector,
    isPrivacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatCurrency(amount, isPrivacyMasked),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentTransactionsCleanCard(
    transactions: List<FinancialTransaction>,
    isPrivacyMasked: Boolean,
    onViewAllClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("ar")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث المعاملات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.testTag("go_to_transactions_button")
                ) {
                    Text(
                        text = "عرض الكل",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "لا توجد معاملات مسجلة بعد • اضغط على زر (+ إضافة معاملة) بالأسفل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                transactions.forEachIndexed { index, tx ->
                    val isIncome = tx.type == "income"
                    val amountColor = if (isIncome) SafeGreen else DangerRose
                    val sign = if (isIncome) "+" else "-"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = amountColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.category} • ${DefaultFinanceCatalog.walletLabelAr(tx.wallet)} • ${dateFormat.format(Date(tx.txDateEpochMs))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = "$sign ${formatCurrency(tx.amount, isPrivacyMasked)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }

                    if (index < transactions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EssentialVsLuxuryCard(metrics: DashboardMetrics) {
    val total = (metrics.essentialSpent + metrics.luxurySpent).coerceAtLeast(1.0)
    val essentialPct = ((metrics.essentialSpent / total) * 100).toInt()
    val luxuryPct = ((metrics.luxurySpent / total) * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("essential_vs_luxury_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "توزيع الإنفاق: أساسي ($essentialPct%) مقابل كمالي ($luxuryPct%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { (essentialPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EssentialBlue,
                trackColor = LuxuryPurple.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun SixMonthsBarChartCard(bars: List<MonthlyBarPoint>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("six_months_chart_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ملخص الدخل والمصروفات لآخر ٦ أشهر",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxVal = remember(bars) {
                bars.maxOfOrNull { maxOf(it.totalIncome, it.totalExpense) }?.coerceAtLeast(1.0) ?: 1.0
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (bars.isEmpty()) return@Canvas
                val groupWidth = size.width / bars.size
                val barWidth = (groupWidth * 0.28f).coerceAtLeast(6f)

                bars.forEachIndexed { idx, point ->
                    val centerX = idx * groupWidth + groupWidth / 2f
                    val incHeight = ((point.totalIncome / maxVal).toFloat() * (size.height - 16f)).coerceAtLeast(4f)
                    val expHeight = ((point.totalExpense / maxVal).toFloat() * (size.height - 16f)).coerceAtLeast(4f)

                    drawRoundRect(
                        color = SafeGreen,
                        topLeft = Offset(centerX - barWidth - 2f, size.height - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = DangerRose,
                        topLeft = Offset(centerX + 2f, size.height - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

fun formatCurrency(amount: Double, isPrivacyMasked: Boolean = false): String {
    if (isPrivacyMasked) return "•••• ج.م"
    return String.format(Locale.US, "%,.0f ج.م", amount)
}
