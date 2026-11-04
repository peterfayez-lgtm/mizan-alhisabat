package com.example.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DashboardMetrics
import com.example.data.FinancialTransaction
import com.example.data.FreeIntegrationsHelper
import com.example.data.ZakatAndTithesSummary
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import com.example.ui.theme.DangerRose
import com.example.ui.theme.EssentialBlue
import com.example.ui.theme.LuxuryPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitmentsAndGoalsScreen(
    metrics: DashboardMetrics,
    transactions: List<FinancialTransaction>,
    commitments: List<LocalCommitmentEntity>,
    savingsGoals: List<LocalSavingsGoalEntity>,
    assetHoldings: List<LocalAssetHoldingEntity>,
    isPrivacyMasked: Boolean,
    onAddCommitment: (
        title: String,
        type: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        totalInstallmentsCount: Int,
        dueDayOfMonth: Int,
        payoutMonthIndex: Int,
        wallet: String
    ) -> Unit,
    onPayCommitmentInstallment: (LocalCommitmentEntity) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    onAddSavingsGoal: (title: String, targetAmount: Double, initialSaved: Double) -> Unit,
    onDepositIntoGoal: (goal: LocalSavingsGoalEntity, amount: Double, wallet: String?) -> Unit,
    onDeleteSavingsGoal: (String) -> Unit,
    onUpsertAssetHolding: (assetCode: String, labelAr: String, unitAr: String, quantity: Double, unitRateInEgp: Double) -> Unit,
    onRefreshLiveRates: () -> Unit = {},
    onRecordSpiritualGiving: (title: String, amount: Double, category: String, wallet: String) -> Unit = { _, _, _, _ -> },
    defaultSpiritualGivingMode: Int = 1,
    defaultUseGrossIncomeForTithe: Boolean = true
) {
    val context = LocalContext.current
    var showAddCommitmentDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var depositingGoal by remember { mutableStateOf<LocalSavingsGoalEntity?>(null) }
    var editingAsset by remember { mutableStateOf<LocalAssetHoldingEntity?>(null) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var commitmentFilterMode by remember { mutableStateOf("all") } // all, debt_receivable, debt_owed, rosca_installment

    val totalSavedInGoals = remember(savingsGoals) { savingsGoals.sumOf { it.currentSavedAmount } }
    val totalAssetsValueEgp = remember(assetHoldings) { assetHoldings.sumOf { it.quantity * it.unitRateInEgp } }
    val monthlyCommitmentsDue = remember(commitments) {
        commitments.filter { it.paidInstallmentsCount < it.totalInstallmentsCount && it.type != "debt_receivable" }
            .sumOf { it.monthlyInstallment }
    }
    val totalReceivableDebtsRemaining = remember(commitments) {
        commitments.filter { it.type == "debt_receivable" && it.paidInstallmentsCount < it.totalInstallmentsCount }
            .sumOf { (it.totalInstallmentsCount - it.paidInstallmentsCount) * it.monthlyInstallment }
    }
    val totalOwedDebtsRemaining = remember(commitments) {
        commitments.filter { it.type == "debt_owed" && it.paidInstallmentsCount < it.totalInstallmentsCount }
            .sumOf { (it.totalInstallmentsCount - it.paidInstallmentsCount) * it.monthlyInstallment }
    }
    val netPersonalDebtsPosition = totalReceivableDebtsRemaining - totalOwedDebtsRemaining
    val filteredCommitments = remember(commitments, commitmentFilterMode) {
        when (commitmentFilterMode) {
            "debt_receivable" -> commitments.filter { it.type == "debt_receivable" }
            "debt_owed" -> commitments.filter { it.type == "debt_owed" }
            "rosca_installment" -> commitments.filter { it.type == "rosca" || it.type == "installment" }
            else -> commitments
        }
    }

    val zakatAndTithesSummary = remember(metrics, transactions, commitments, savingsGoals, assetHoldings) {
        FreeIntegrationsHelper.computeZakatAndTithesSummary(
            metrics = metrics,
            transactions = transactions,
            commitments = commitments,
            savingsGoals = savingsGoals,
            assetHoldings = assetHoldings
        )
    }

    fun formatAmount(value: Double): String {
        return if (isPrivacyMasked) "••••• ج.م" else "${value.toInt()} ج.م"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("commitments_goals_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Summary Card (Total Wealth in Goals + Gold/Currency + Monthly Commitments)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "إدارة الجمعيات والأقساط والثروة والزكاة/العشور",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "تتبع الجمعيات الشهرية، الأقساط، الحصالة الذكية، الذهب والعملات، والزكاة أو العشور",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "مدخرات الأهداف",
                            value = formatAmount(totalSavedInGoals),
                            color = SafeGreen
                        )
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "الذهب والعملات",
                            value = formatAmount(totalAssetsValueEgp),
                            color = WarningAmber
                        )
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "أقساط وجمعيات الشهر",
                            value = formatAmount(monthlyCommitmentsDue),
                            color = DangerRose
                        )
                    }
                }
            }
        }

        // 2. Dropdown Card: Option 1 (الزكاة والصدقات) & Option 2 (العشور في المسيحية الأرثوذكسية 10%)
        item {
            ZakatAndOrthodoxTithesCard(
                summary = zakatAndTithesSummary,
                isPrivacyMasked = isPrivacyMasked,
                defaultSpiritualGivingMode = defaultSpiritualGivingMode,
                defaultUseGrossIncomeForTithe = defaultUseGrossIncomeForTithe,
                onRecordPayment = onRecordSpiritualGiving
            )
        }

        // 3. Section: ROSCAs, Installments & Personal Debts Ledger (الجمعيات الشهرية والأقساط ودفتر الديون: ليا وعليّا)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "الجمعيات الشهرية والأقساط ودفتر الديون (ليا وعليّا)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تتبع فلوسك اللي بره والديون والأقساط، وأرسل تذكيراً ودياً عبر واتساب بضغطة واحدة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { showAddCommitmentDialog = true },
                            modifier = Modifier.testTag("add_commitment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة التزام/دين")
                        }
                    }

                    // Personal Debts Summary Bar (ليا بره / عليّا / الصافي)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "🟢 فلوس ليا بره",
                            value = formatAmount(totalReceivableDebtsRemaining),
                            color = SafeGreen
                        )
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "🔴 ديون عليّا",
                            value = formatAmount(totalOwedDebtsRemaining),
                            color = DangerRose
                        )
                        MiniSummaryBox(
                            modifier = Modifier.weight(1f),
                            label = "⚖️ صافي الديون",
                            value = formatAmount(netPersonalDebtsPosition),
                            color = if (netPersonalDebtsPosition >= 0.0) SafeGreen else DangerRose
                        )
                    }

                    // Filter Chips for Personal Debts vs ROSCAs/Installments
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "all" to "الكل (${commitments.size})",
                            "debt_receivable" to "🟢 ليا بره",
                            "debt_owed" to "🔴 عليّا لناس",
                            "rosca_installment" to "🏦 جمعيات وأقساط"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = commitmentFilterMode == key,
                                onClick = { commitmentFilterMode = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (filteredCommitments.isEmpty()) {
                        Text(
                            text = "لا توجد عناصر في هذا التصنيف حالياً. اضغط «إضافة التزام/دين» لإضافة جمعية أو قسط أو سلفة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredCommitments.forEach { cmt ->
                            CommitmentItemCard(
                                commitment = cmt,
                                isPrivacyMasked = isPrivacyMasked,
                                onPayInstallment = { onPayCommitmentInstallment(cmt) },
                                onAddCalendarReminder = {
                                    FreeIntegrationsHelper.openCalendarReminder(
                                        context = context,
                                        title = cmt.decryptedTitle,
                                        monthlyAmount = cmt.monthlyInstallment,
                                        dueDayOfMonth = cmt.dueDayOfMonth
                                    )
                                },
                                onSendWhatsAppReminder = {
                                    FreeIntegrationsHelper.sendPoliteDebtWhatsAppReminder(
                                        context = context,
                                        commitment = cmt
                                    )
                                },
                                onDelete = { onDeleteCommitment(cmt.id) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Section: Smart Savings Goals (الحصالة الذكية وأهداف الادخار)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = SafeGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "الحصالة الذكية وأهداف الادخار",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حدد أهدافك المالية وتابع نسبة الإنجاز مع كل إيداع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showAddGoalDialog = true },
                            modifier = Modifier.testTag("add_savings_goal_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("هدف جديد")
                        }
                    }

                    if (savingsGoals.isEmpty()) {
                        Text(
                            text = "لا توجد أهداف ادخار حالياً. أضف هدفك الأول مثل «صندوق الطوارئ».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savingsGoals.forEach { goal ->
                            SavingsGoalCard(
                                goal = goal,
                                isPrivacyMasked = isPrivacyMasked,
                                onDepositClick = { depositingGoal = goal },
                                onDeleteClick = { onDeleteSavingsGoal(goal.id) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Section: Multi-Currency & Gold Portfolio + Live API Refresh
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "محفظة العملات الأجنبية والذهب",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "احسب القيمة اللحظية لمدخراتك أو حدّث الأسعار الحية مجاناً بضغطة واحدة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showAddAssetDialog = true },
                            modifier = Modifier.testTag("add_asset_holding_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عملة / أصل")
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshLiveRates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refresh_live_rates_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌐 تحديث أسعار العملات والذهب تلقائياً الآن (مجاناً)")
                    }

                    assetHoldings.forEach { asset ->
                        val totalEgp = asset.quantity * asset.unitRateInEgp
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = asset.labelAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPrivacyMasked) {
                                            "الكمية: ••• ${asset.unitAr} • السعر: ${asset.unitRateInEgp.toInt()} ج.م"
                                        } else {
                                            "الكمية: ${asset.quantity} ${asset.unitAr} × ${asset.unitRateInEgp.toInt()} ج.م"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatAmount(totalEgp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    FilledTonalIconButton(
                                        onClick = { editingAsset = asset },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الكمية أو سعر الصرف",
                                            modifier = Modifier.size(18.dp)
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

    if (showAddCommitmentDialog) {
        AddCommitmentDialog(
            onDismiss = { showAddCommitmentDialog = false },
            onConfirm = { title, type, totalAmt, monthlyAmt, count, dueDay, payoutMonth, wallet ->
                onAddCommitment(title, type, totalAmt, monthlyAmt, count, dueDay, payoutMonth, wallet)
                showAddCommitmentDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, target, initial ->
                onAddSavingsGoal(title, target, initial)
                showAddGoalDialog = false
            }
        )
    }

    if (depositingGoal != null) {
        DepositIntoGoalDialog(
            goal = depositingGoal!!,
            onDismiss = { depositingGoal = null },
            onConfirm = { amt, wallet ->
                onDepositIntoGoal(depositingGoal!!, amt, wallet)
                depositingGoal = null
            }
        )
    }

    if (editingAsset != null) {
        EditAssetHoldingDialog(
            initialAsset = editingAsset,
            onDismiss = { editingAsset = null },
            onConfirm = { code, label, unit, qty, rate ->
                onUpsertAssetHolding(code, label, unit, qty, rate)
                editingAsset = null
            }
        )
    }

    if (showAddAssetDialog) {
        EditAssetHoldingDialog(
            initialAsset = null,
            onDismiss = { showAddAssetDialog = false },
            onConfirm = { code, label, unit, qty, rate ->
                onUpsertAssetHolding(code, label, unit, qty, rate)
                showAddAssetDialog = false
            }
        )
    }
}

/**
 * بطاقة حاسبة الزكاة والعشور الذكية مع قائمة منسدلة (Dropdown):
 * ١. الزكاة والصدقات (نصاب 85 جرام ذهب عيار 21 - ربع العشر 2.5%)
 * ٢. العشور والعطايا في الديانة المسيحية الأرثوذكسية (10% من إجمالي الدخل أو صافي الدخل + البكور وإخوة الرب)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ZakatAndOrthodoxTithesCard(
    summary: ZakatAndTithesSummary,
    isPrivacyMasked: Boolean,
    defaultSpiritualGivingMode: Int = 1,
    defaultUseGrossIncomeForTithe: Boolean = true,
    onRecordPayment: (title: String, amount: Double, category: String, wallet: String) -> Unit
) {
    // 1 = الزكاة والصدقات, 2 = العشور والعطايا (المسيحية الأرثوذكسية)
    var selectedMode by remember(defaultSpiritualGivingMode) { mutableIntStateOf(defaultSpiritualGivingMode.coerceIn(1, 2)) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Inside Orthodox Tithes: true = 10% of Gross Total Income (الأصل الكنسي الأرثوذكسي), false = 10% of Net Income after essential expenses
    var useGrossIncomeForTithe by remember(defaultUseGrossIncomeForTithe) { mutableStateOf(defaultUseGrossIncomeForTithe) }
    var showPayDialog by remember { mutableStateOf(false) }

    val options = listOf(
        1 to "١. الزكاة والصدقات (نصاب الذهب 85 جرام - 2.5%)",
        2 to "٢. العشور والعطايا (المسيحية الأرثوذكسية - 10%)"
    )
    val currentLabel = options.first { it.first == selectedMode }.second

    fun fmt(amount: Double): String = if (isPrivacyMasked) "•••• ج.م" else "${amount.toInt()} ج.م"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zakat_and_tithes_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = "الزكاة والعشور",
                    tint = SafeGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "حاسبة الزكاة والعشور والعطايا الذكية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر من القائمة المنسدلة (١. الزكاة أو ٢. العشور الأرثوذكسية)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dropdown Selector (خانة منسدلة: ١. الزكاة / ٢. العشور)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spiritual_giving_dropdown_box")
            ) {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر نظام الحساب (الزكاة / العشور)") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("spiritual_giving_dropdown_field")
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    options.forEach { (modeId, labelText) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = labelText,
                                    fontWeight = if (selectedMode == modeId) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedMode = modeId
                                dropdownExpanded = false
                            },
                            modifier = Modifier.testTag("dropdown_option_$modeId")
                        )
                    }
                }
            }

            if (selectedMode == 1) {
                // =========================================================================
                // الخيار ١: الزكاة والصدقات (نصاب 85 جرام ذهب عيار 21 - ربع العشر 2.5%)
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "أولاً: حساب وعاء الزكاة الشرعي (نصاب 85 جرام ذهب عيار 21)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• قيمة نصاب الذهب الحالية (85 جرام × ${summary.gold21GramRateEgp.toInt()} ج.م): ${fmt(summary.nisabThresholdEgp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• السيولة والمدخرات النقدية: ${fmt(summary.liquidWalletsBalanceEgp)} | الذهب والعملات: ${fmt(summary.goldAndForeignAssetsEgp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• ديون مستحقة لي: ${fmt(summary.receivableDebtsEgp)} | يخصم التزامات حالّة: ${fmt(summary.deductibleDebtsDueEgp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "صافي الوعاء الزكوي: ${fmt(summary.netZakatableWealthEgp)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isNisabReached) SafeGreen else WarningAmber
                        )

                        if (summary.isNisabReached) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SafeGreen.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "✅ بلغ المال النصاب الشرعي (تجب فيه الزكاة عند حولان الحول بنسبة 2.5%)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SafeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• مقدار الزكاة السنوية (2.5%): ${fmt(summary.annualZakat2Point5Percent)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• القسط الشهري التقريبي لتوزيع الزكاة: ${fmt(summary.monthlyZakatDistribution)} / شهر",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• المخرج فعلياً هذا الشهر (زكاة وصدقات): ${fmt(summary.paidZakatAndSadaqahThisMonth)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            val diff = (summary.nisabThresholdEgp - summary.netZakatableWealthEgp).coerceAtLeast(0.0)
                            Text(
                                text = "ℹ️ لم يبلغ صافي المدخرات نصاب الزكاة بعد (متبقي ${fmt(diff)} لبلوغ النصاب)، ويمكنك إخراج الصدقات التطوعية في أي وقت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showPayDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("record_zakat_payment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل دفعة زكاة مال أو صدقة جارية")
                        }
                    }
                }
            } else {
                // =========================================================================
                // الخيار ٢: العشور والعطايا في الديانة المسيحية الأرثوذكسية (10%)
                // =========================================================================
                val targetTithe = if (useGrossIncomeForTithe) {
                    summary.orthodoxTitheGross10Percent
                } else {
                    summary.orthodoxTitheNet10Percent
                }
                val remainingTithe = if (useGrossIncomeForTithe) {
                    summary.remainingGrossTitheThisMonth
                } else {
                    summary.remainingNetTitheThisMonth
                }
                val progress = if (targetTithe > 0.0) {
                    (summary.paidTithesAndOfferingsThisMonth / targetTithe).toFloat().coerceIn(0f, 1f)
                } else 0f

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ثانياً: حساب العشور في الكنيسة المسيحية الأرثوذكسية (10%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "«هَاتُوا جَمِيعَ الْعُشُورِ إِلَى الْخَزْنَةِ لِيَكُونَ فِي بَيْتِي طَعَامٌ، وَجَرِّبُونِي بِهذَا قَالَ رَبُّ الْجُنُودِ» (ملاخي 3: 10). في التقليد الأرثوذكسي تُحسب العشور بنسبة 10% من إجمالي الدخل، ويُضاف إليها عطايا إخوة الرب والبكور والنذور بحسب محبتك.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = useGrossIncomeForTithe,
                                onClick = { useGrossIncomeForTithe = true },
                                label = { Text("10% من إجمالي الدخل الكلي (الأصل الأرثوذكسي)") }
                            )
                            FilterChip(
                                selected = !useGrossIncomeForTithe,
                                onClick = { useGrossIncomeForTithe = false },
                                label = { Text("10% من صافي الدخل بعد مصاريف العمل/الضروريات") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniSummaryBox(
                                modifier = Modifier.weight(1f),
                                label = if (useGrossIncomeForTithe) "إجمالي دخل الشهر" else "صافي الدخل",
                                value = fmt(
                                    if (useGrossIncomeForTithe) summary.grossMonthlyIncomeEgp
                                    else summary.netMonthlyIncomeAfterEssentialsEgp
                                ),
                                color = EssentialBlue
                            )
                            MiniSummaryBox(
                                modifier = Modifier.weight(1f),
                                label = "قيمة العشور (10%)",
                                value = fmt(targetTithe),
                                color = WarningAmber
                            )
                            MiniSummaryBox(
                                modifier = Modifier.weight(1f),
                                label = "المتبقي هذا الشهر",
                                value = fmt(remainingTithe),
                                color = if (remainingTithe <= 0.0) SafeGreen else DangerRose
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = SafeGreen
                        )

                        Text(
                            text = "• المسدد من العشور وعطايا إخوة الرب هذا الشهر: ${fmt(summary.paidTithesAndOfferingsThisMonth)} (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )

                        Text(
                            text = "💡 إرشاد أرثوذكسي: تشمل العطايا الكنسية أيضاً «البكور» (أول مرتب أو أول زيادة سنوية)، و«خدمة إخوة الرب» للفقراء والمحتاجين في الخفاء (متى 6: 3-4).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showPayDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("record_tithe_payment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل دفعة عشور (10%) / إخوة الرب / بكور")
                        }
                    }
                }
            }
        }
    }

    if (showPayDialog) {
        val defaultAmount = if (selectedMode == 1) {
            if (summary.monthlyZakatDistribution > 0.0) summary.monthlyZakatDistribution.toInt().toString() else "500"
        } else {
            val rem = if (useGrossIncomeForTithe) summary.remainingGrossTitheThisMonth else summary.remainingNetTitheThisMonth
            if (rem > 0.0) rem.toInt().toString() else summary.orthodoxTitheGross10Percent.toInt().coerceAtLeast(100).toString()
        }
        val defaultTitle = if (selectedMode == 1) "إخراج زكاة مال / صدقة" else "سداد عشور الشهر (10%) - إخوة الرب"
        val defaultCategory = if (selectedMode == 1) "زكاة وصدقات" else "عشور وعطايا"

        var titleText by remember { mutableStateOf(defaultTitle) }
        var amountText by remember { mutableStateOf(defaultAmount) }
        var wallet by remember { mutableStateOf("cash") }

        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = {
                Text(
                    if (selectedMode == 1) "تسجيل إخراج زكاة أو صدقة" else "تسجيل عشور / عطية إخوة الرب / بكور",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("الوصف (مثال: عشور الشهر، إخوة الرب، زكاة مال)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("المحفظة المستخدمة:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("cash" to "كاش", "bank" to "بنك", "ewallet" to "إلكترونية").forEach { (k, l) ->
                            FilterChip(
                                selected = wallet == k,
                                onClick = { wallet = k },
                                label = { Text(l) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onRecordPayment(titleText.ifBlank { defaultTitle }, amt, defaultCategory, wallet)
                            showPayDialog = false
                        }
                    }
                ) {
                    Text("حفظ وتسجيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun MiniSummaryBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommitmentItemCard(
    commitment: LocalCommitmentEntity,
    isPrivacyMasked: Boolean,
    onPayInstallment: () -> Unit,
    onAddCalendarReminder: () -> Unit,
    onSendWhatsAppReminder: () -> Unit = {},
    onDelete: () -> Unit
) {
    val progress = (commitment.paidInstallmentsCount.toFloat() / commitment.totalInstallmentsCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isCompleted = commitment.paidInstallmentsCount >= commitment.totalInstallmentsCount
    val typeBadgeLabel = when (commitment.type) {
        "rosca" -> "جمعية شهرية (تقبض في الشهر ${commitment.payoutMonthIndex})"
        "installment" -> "قسط شهري"
        "debt_receivable" -> "🟢 دين مستحق لي (ليا بره)"
        else -> "🔴 دين عليّ"
    }
    val badgeColor = when (commitment.type) {
        "rosca" -> LuxuryPurple
        "debt_receivable" -> SafeGreen
        else -> EssentialBlue
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commitment.decryptedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$typeBadgeLabel • يوم الاستحقاق: ${commitment.dueDayOfMonth} من كل شهر",
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف الالتزام"
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isCompleted) SafeGreen else badgeColor
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text(
                        text = if (isPrivacyMasked) {
                            "القسط: •••• ج.م من إجمالي ••••• ج.م"
                        } else {
                            "القسط: ${commitment.monthlyInstallment.toInt()} ج.م • الإجمالي: ${commitment.totalAmount.toInt()} ج.م"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "المدفوع: ${commitment.paidInstallmentsCount} من ${commitment.totalInstallmentsCount} دفعات (${(progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendWhatsAppReminder,
                        modifier = Modifier.testTag("whatsapp_reminder_${commitment.id}")
                    ) {
                        Text(if (commitment.type == "debt_receivable") "💬 تذكير ودي واتساب" else "💬 رسالة سداد واتساب")
                    }

                    OutlinedButton(
                        onClick = onAddCalendarReminder,
                        modifier = Modifier.testTag("calendar_reminder_${commitment.id}")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "تذكير بالتقويم", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقويم")
                    }

                    if (!isCompleted) {
                        FilledTonalButton(
                            onClick = onPayInstallment,
                            modifier = Modifier.testTag("pay_commitment_${commitment.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (commitment.type == "debt_receivable") "تحصيل دفعة" else "سداد قسط")
                        }
                    } else {
                        Text(
                            text = "✅ مكتمل",
                            style = MaterialTheme.typography.labelLarge,
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: LocalSavingsGoalEntity,
    isPrivacyMasked: Boolean,
    onDepositClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = (goal.currentSavedAmount / goal.targetAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val remaining = (goal.targetAmount - goal.currentSavedAmount).coerceAtLeast(0.0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.decryptedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPrivacyMasked) {
                            "المدخر: •••• ج.م من ••••• ج.م"
                        } else {
                            "المدخر: ${goal.currentSavedAmount.toInt()} ج.م من ${goal.targetAmount.toInt()} ج.م (متبقي ${remaining.toInt()} ج.م)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الهدف")
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = SafeGreen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نسبة الإنجاز: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = SafeGreen,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(onClick = onDepositClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إيداع في الحصالة")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCommitmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        totalInstallmentsCount: Int,
        dueDayOfMonth: Int,
        payoutMonthIndex: Int,
        wallet: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("rosca") }
    var monthlyText by remember { mutableStateOf("2000") }
    var monthsCountText by remember { mutableStateOf("10") }
    var dueDayText by remember { mutableStateOf("5") }
    var payoutMonthText by remember { mutableStateOf("6") }
    var wallet by remember { mutableStateOf("bank") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة جمعية أو قسط أو دين جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "rosca" to "جمعية شهرية",
                        "installment" to "قسط مشتريات",
                        "debt_owed" to "دين عليّ",
                        "debt_receivable" to "دين مستحق لي"
                    ).forEach { (k, label) ->
                        FilterChip(
                            selected = type == k,
                            onClick = { type = k },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("الاسم (مثال: جمعية أصدقاء العمل / قسط ثلاجة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("قيمة القسط / الدفعة الشهرية (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthsCountText,
                    onValueChange = { monthsCountText = it },
                    label = { Text("عدد الشهور / الأقساط الكلي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("يوم الاستحقاق (1-28)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (type == "rosca") {
                        OutlinedTextField(
                            value = payoutMonthText,
                            onValueChange = { payoutMonthText = it },
                            label = { Text("ترتيب شهر القبض") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("المحفظة المرتبطة بالسداد:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash" to "كاش", "bank" to "بنك", "ewallet" to "إلكترونية").forEach { (k, l) ->
                        FilterChip(
                            selected = wallet == k,
                            onClick = { wallet = k },
                            label = { Text(l) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monthly = monthlyText.toDoubleOrNull() ?: 0.0
                    val count = (monthsCountText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val dueDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 28)
                    val payoutMonth = (payoutMonthText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    if (title.isNotBlank() && monthly > 0.0) {
                        onConfirm(title, type, monthly * count, monthly, count, dueDay, payoutMonth, wallet)
                    }
                }
            ) {
                Text("حفظ الالتزام")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, initialSaved: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("20000") }
    var initialText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة هدف ادخار جديد (الحصالة الذكية)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الهدف (مثال: شراء سيارة، سفر، طوارئ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("المبلغ المستهدف (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = { Text("المبلغ المدخر حالياً كبداية (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val initial = initialText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && target > 0.0) {
                        onConfirm(title, target, initial)
                    }
                }
            ) {
                Text("إنشاء الهدف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun DepositIntoGoalDialog(
    goal: LocalSavingsGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, wallet: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("1000") }
    var deductWallet by remember { mutableStateOf<String?>("bank") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إيداع في «${goal.decryptedTitle}»", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ الإيداع (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("خصم المبلغ من محفظة:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "bank" to "بنك",
                        "cash" to "كاش",
                        "ewallet" to "إلكترونية",
                        null to "بدون خصم"
                    ).forEach { (k, label) ->
                        FilterChip(
                            selected = deductWallet == k,
                            onClick = { deductWallet = k },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(amt, deductWallet)
                    }
                }
            ) {
                Text("تأكيد الإيداع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun EditAssetHoldingDialog(
    initialAsset: LocalAssetHoldingEntity?,
    onDismiss: () -> Unit,
    onConfirm: (code: String, labelAr: String, unitAr: String, qty: Double, rate: Double) -> Unit
) {
    var code by remember { mutableStateOf(initialAsset?.assetCode ?: "EUR") }
    var label by remember { mutableStateOf(initialAsset?.labelAr ?: "يورو أوروبي (EUR)") }
    var unit by remember { mutableStateOf(initialAsset?.unitAr ?: "€") }
    var qtyText by remember { mutableStateOf((initialAsset?.quantity ?: 100.0).toString()) }
    var rateText by remember { mutableStateOf((initialAsset?.unitRateInEgp ?: 53.5).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialAsset == null) "إضافة عملة أجنبية أو ذهب" else "تحديث ${initialAsset.labelAr}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (initialAsset == null) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("رمز الأصل (مثال: EUR, AED, GOLD_18)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("الاسم بالعربية (مثال: درهم إماراتي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("الوحدة (مثال: جرام، $، €، د.إ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("الكمية المملوكة (${unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("سعر الوحدة الحالي بالجنيه (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toDoubleOrNull() ?: 0.0
                    val r = rateText.toDoubleOrNull() ?: 0.0
                    if (code.isNotBlank() && label.isNotBlank() && r > 0.0) {
                        onConfirm(code, label, unit, q, r)
                    }
                }
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
