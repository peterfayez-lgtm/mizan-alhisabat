package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.BurnRateStatus
import com.example.data.DashboardMetrics
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinancialTransaction
import com.example.data.FreeIntegrationsHelper
import com.example.data.MizanHomeWidgetProvider
import com.example.data.MizanNotificationListenerService
import com.example.data.local.LocalCommitmentEntity
import com.example.ui.theme.DangerRose
import com.example.ui.theme.EssentialBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

/**
 * تقييم تعبير حسابي بسيط مثل "150+75-20" داخل الآلة الحاسبة السريعة.
 */
fun evaluateSimpleMathExpression(expr: String): Double {
    val clean = expr.replace(" ", "").trim()
    if (clean.isEmpty()) return 0.0
    return try {
        var total = 0.0
        var currentNumber = StringBuilder()
        var currentOp = '+'
        for (i in clean.indices) {
            val ch = clean[i]
            if (ch in '0'..'9' || ch == '.') {
                currentNumber.append(ch)
            } else if (ch == '+' || ch == '-') {
                val num = currentNumber.toString().toDoubleOrNull() ?: 0.0
                total = if (currentOp == '+') total + num else total - num
                currentOp = ch
                currentNumber = StringBuilder()
            }
        }
        val lastNum = currentNumber.toString().toDoubleOrNull() ?: 0.0
        total = if (currentOp == '+') total + lastNum else total - lastNum
        total.coerceAtLeast(0.0)
    } catch (_: Exception) {
        0.0
    }
}

/**
 * 1. نافذة الآلة الحاسبة البنكية السريعة لتسجيل المعاملة في 3 ثوانٍ (Fast 3-Second Keypad Entry)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCalculatorAddSheetDialog(
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    currencySymbol: String = "ج.م",
    onAddTransaction: (FinancialTransaction) -> Unit,
    onRecordWalletTransfer: (fromWallet: String, toWallet: String, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") } // "expense" | "income" | "transfer"
    var selectedCategory by remember {
        mutableStateOf(expenseCategories.firstOrNull() ?: "طعام ومشتريات")
    }
    var selectedWallet by remember { mutableStateOf("cash") }
    var targetTransferWallet by remember { mutableStateOf("bank") }
    var priority by remember { mutableStateOf("essential") }
    var noteTitle by remember { mutableStateOf("") }

    val evaluatedAmount = remember(expression) {
        evaluateSimpleMathExpression(expression)
    }
    val activeCategories = if (type == "income") incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "⚡ تسجيل سريع بالحاسبة (٣ ثوانٍ)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اكتب المبلغ أو اجمع رقمين واختر الفئة والمحفظة فوراً",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Type Switcher Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("expense", "🔴 مصروف", DangerRose),
                        Triple("income", "🟢 دخل", SafeGreen),
                        Triple("transfer", "⇄ تحويل", WarningAmber)
                    ).forEach { (key, label, color) ->
                        val selected = type == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    type = key
                                    if (key == "income") {
                                        selectedCategory = incomeCategories.firstOrNull() ?: "راتب أساسي"
                                    } else if (key == "expense") {
                                        selectedCategory = expenseCategories.firstOrNull() ?: "طعام ومشتريات"
                                    }
                                }
                                .testTag("calc_type_$key"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (selected) color else Color.Transparent)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                // 2. Digital Amount Display Screen
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (expression.contains("+") || expression.contains("-")) {
                            Text(
                                text = expression,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (type == "transfer") {
                                    "من ${DefaultFinanceCatalog.walletLabelAr(selectedWallet)} ⬅ ${DefaultFinanceCatalog.walletLabelAr(targetTransferWallet)}"
                                } else {
                                    selectedCategory
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${evaluatedAmount.toInt()} $currencySymbol",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (type) {
                                    "income" -> SafeGreen
                                    "transfer" -> WarningAmber
                                    else -> DangerRose
                                }
                            )
                        }
                    }
                }

                // 3. Quick Preset Chips (50, 100, 200, 500, 1000)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50, 100, 200, 500, 1000, 2000).forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    expression = if (expression.isBlank()) {
                                        preset.toString()
                                    } else {
                                        "$expression+$preset"
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "+$preset",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 4. Compact Calculator Keypad Grid
                val keypadRows = listOf(
                    listOf("7", "8", "9", "+"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "⌫"),
                    listOf("C", "0", "00", ".")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    keypadRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (key) {
                                                "C" -> expression = ""
                                                "⌫" -> if (expression.isNotEmpty()) {
                                                    expression = expression.dropLast(1)
                                                }
                                                else -> expression += key
                                            }
                                        }
                                        .testTag("calc_key_$key"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = when (key) {
                                        "C" -> DangerRose.copy(alpha = 0.18f)
                                        "+", "-", "⌫" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (key == "C") DangerRose else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Category & Wallet Selection
                if (type != "transfer") {
                    Text(
                        text = "الفئة:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "cash" to "💵 كاش",
                        "bank" to "🏦 بنك",
                        "ewallet" to "📱 محفظة"
                    ).forEach { (wKey, wLabel) ->
                        FilterChip(
                            selected = selectedWallet == wKey,
                            onClick = {
                                selectedWallet = wKey
                                if (targetTransferWallet == wKey) {
                                    targetTransferWallet = if (wKey == "bank") "cash" else "bank"
                                }
                            },
                            label = { Text(wLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (type == "expense") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = priority == "essential",
                            onClick = { priority = "essential" },
                            label = { Text("🛡️ ضروري / أساسي") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = priority == "luxury",
                            onClick = { priority = "luxury" },
                            label = { Text("✨ رفاهية / كمالي") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("وصف مختصر (اختياري — يُملأ تلقائياً باسم الفئة)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (evaluatedAmount <= 0.0) return@Button
                    if (type == "transfer") {
                        onRecordWalletTransfer(
                            selectedWallet,
                            targetTransferWallet,
                            evaluatedAmount,
                            noteTitle
                        )
                    } else {
                        onAddTransaction(
                            FinancialTransaction(
                                title = noteTitle.trim().ifBlank { selectedCategory },
                                amount = evaluatedAmount,
                                type = type,
                                category = selectedCategory,
                                priority = if (type == "income") "essential" else priority,
                                recurrence = "variable",
                                wallet = selectedWallet,
                                txDateEpochMs = System.currentTimeMillis()
                            )
                        )
                    }
                    onDismiss()
                },
                enabled = evaluatedAmount > 0.0,
                modifier = Modifier.testTag("calc_confirm_save_button")
            ) {
                Text("حفظ فوراً (${evaluatedAmount.toInt()} $currencySymbol) ✓", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * 2. مركز البرامج المجانية والربط الذكي الشامل (Smart Payment Apps, Notification Reader, Widget & Google Sheets Hub)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartPaymentAndIntegrationsHubDialog(
    appSettings: AppSettings,
    metrics: DashboardMetrics,
    burnRates: List<BurnRateStatus>,
    commitments: List<LocalCommitmentEntity>,
    onAddTransaction: (FinancialTransaction) -> Unit,
    onToggleAutoCaptureNotifications: (Boolean) -> Unit,
    onSaveGoogleSheetsSettings: (url: String, autoSync: Boolean) -> Unit,
    onSyncGoogleSheetsNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = تطبيقات الدفع، 1 = قارئ الإشعارات والويدجت، 2 = Google Sheets & Tasks

    // State for Tab 0 (Payment Apps)
    var transferAmountText by remember { mutableStateOf("") }
    var recipientPhoneOrIpa by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var recordInMizanToo by remember { mutableStateOf(true) }

    // State for Tab 1 (SMS & Notification Reader + Home Widget)
    var manualSmsText by remember {
        mutableStateOf("تم خصم مبلغ 350.00 ج.م عبر InstaPay لصالح سوبر ماركت من حسابكم البنكي.")
    }
    var widgetFeedback by remember { mutableStateOf<String?>(null) }
    val isListenerGranted = remember {
        MizanNotificationListenerService.isNotificationListenerEnabled(context)
    }

    // State for Tab 2 (Google Sheets & Tasks)
    var sheetsUrlInput by remember(appSettings.googleSheetsWebAppUrl) {
        mutableStateOf(appSettings.googleSheetsWebAppUrl)
    }
    var autoSyncSheets by remember(appSettings.autoSyncGoogleSheetsOnNewTx) {
        mutableStateOf(appSettings.autoSyncGoogleSheetsOnNewTx)
    }
    var scriptCopiedNotice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🔗 مركز الربط الذكي والبرامج المجانية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "تطبيقات الدفع (InstaPay/كاش) • قارئ الإشعارات والويدجت • Google Sheets & Tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top 3 Sub-Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "⚡ الدفع والتحويل",
                        1 to "📩 الإشعارات والويدجت",
                        2 to "☁️ Google Sheets"
                    ).forEach { (idx, title) ->
                        val selected = activeTab == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeTab = idx }
                                .testTag("smart_hub_tab_$idx"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                when (activeTab) {
                    0 -> {
                        // TAB 0: INSTAPAY / VODAFONE CASH / FAWRY / TELDA QUICK LAUNCHER
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🚀 نفّذ التحويل عبر تطبيقك البنكي وسجّله في ميزان بضغطة واحدة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = transferAmountText,
                                    onValueChange = { transferAmountText = it },
                                    label = { Text("المبلغ المراد تحويله/دفعه (اختياري)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = recipientPhoneOrIpa,
                                    onValueChange = { recipientPhoneOrIpa = it },
                                    label = { Text("رقم الهاتف (010...) أو عنوان InstaPay (ينسخ تلقائياً)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = transferNote,
                                    onValueChange = { transferNote = it },
                                    label = { Text("وصف المعاملة في سجل ميزان (اختياري)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "تسجيل المعاملة تلقائياً في ميزان عند فتح تطبيق الدفع",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = recordInMizanToo,
                                        onCheckedChange = { recordInMizanToo = it }
                                    )
                                }

                                fun triggerPaymentApp(key: String, labelAr: String, walletType: String) {
                                    val amt = transferAmountText.toDoubleOrNull() ?: 0.0
                                    if (recordInMizanToo && amt > 0.0) {
                                        onAddTransaction(
                                            FinancialTransaction(
                                                title = transferNote.trim().ifBlank { "تحويل/دفع عبر $labelAr" },
                                                amount = amt,
                                                type = "expense",
                                                category = "سكن وفواتير",
                                                priority = "essential",
                                                recurrence = "variable",
                                                wallet = walletType,
                                                txDateEpochMs = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    FreeIntegrationsHelper.openPaymentAppOrDialer(
                                        context = context,
                                        paymentKey = key,
                                        amount = amt,
                                        recipientPhoneOrIpa = recipientPhoneOrIpa
                                    )
                                }

                                Text(
                                    text = "اضغط لفتح التطبيق أو كود التحويل المباشر:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { triggerPaymentApp("instapay", "InstaPay", "bank") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                        modifier = Modifier.testTag("launch_instapay_button")
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح InstaPay")
                                    }

                                    Button(
                                        onClick = { triggerPaymentApp("vodafone_cash", "فودافون كاش", "ewallet") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        modifier = Modifier.testTag("launch_vodafone_cash_button")
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فودافون كاش (*9*7#)")
                                    }

                                    FilledTonalButton(
                                        onClick = { triggerPaymentApp("etisalat_cash", "اتصالات كاش", "ewallet") }
                                    ) {
                                        Text("اتصالات كاش (*777#)")
                                    }

                                    FilledTonalButton(
                                        onClick = { triggerPaymentApp("orange_cash", "أورنج كاش", "ewallet") }
                                    ) {
                                        Text("أورنج كاش (#7115#)")
                                    }

                                    FilledTonalButton(
                                        onClick = { triggerPaymentApp("fawry", "myFawry", "bank") }
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تطبيق فوري (myFawry)")
                                    }

                                    FilledTonalButton(
                                        onClick = { triggerPaymentApp("telda", "Telda", "bank") }
                                    ) {
                                        Text("تطبيق Telda")
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: SMART BANK NOTIFICATION LISTENER + HOME SCREEN WIDGET
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = SafeGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "1. قارئ إشعارات البنوك و InstaPay التلقائي",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يلتقط إشعارات InstaPay، البنك الأهلي، CIB، بنك مصر، وفودافون كاش ويسجلها تلقائياً 100% أوفلاين.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = appSettings.autoCaptureBankNotifications,
                                        onCheckedChange = onToggleAutoCaptureNotifications
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        MizanNotificationListenerService.openNotificationListenerSettings(context)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isListenerGranted) {
                                            "✅ صلاحية قراءة الإشعارات مفعّلة (اضغط للمراجعة)"
                                        } else {
                                            "🔓 منح صلاحية قراءة إشعارات البنوك من إعدادات أندرويد"
                                        }
                                    )
                                }

                                // Instant Clipboard Paste & Parse
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "تحليل فوري لرسالة من الحافظة (Clipboard):",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clipText = cb?.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!clipText.isNullOrBlank()) {
                                                manualSmsText = clipText
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("لصق من الحافظة")
                                    }
                                }

                                OutlinedTextField(
                                    value = manualSmsText,
                                    onValueChange = { manualSmsText = it },
                                    label = { Text("نص رسالة البنك / InstaPay / فودافون كاش") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val parsedSms = remember(manualSmsText) {
                                    FreeIntegrationsHelper.parseBankOrInstaPayMessage(manualSmsText)
                                }
                                if (parsedSms != null) {
                                    Button(
                                        onClick = {
                                            onAddTransaction(
                                                FinancialTransaction(
                                                    title = parsedSms.title,
                                                    amount = parsedSms.amount,
                                                    type = parsedSms.type,
                                                    category = parsedSms.category,
                                                    priority = parsedSms.priority,
                                                    recurrence = "variable",
                                                    wallet = parsedSms.wallet,
                                                    txDateEpochMs = System.currentTimeMillis()
                                                )
                                            )
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "تسجيل (${parsedSms.title} • ${parsedSms.amount.toInt()} ج.م) الآن ✓",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // HOME SCREEN WIDGET CARD
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "2. ويدجت الشاشة الرئيسية للهاتف (Home Screen Widget)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يعرض «المسموح صرفه اليوم» و«صافي الرصيد» وزر «+ معاملة سريعة» على شاشة هاتفك الخارجية.",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        MizanHomeWidgetProvider.updateAllWidgets(context)
                                        val pinned = MizanHomeWidgetProvider.requestPinHomeWidget(context)
                                        widgetFeedback = if (pinned) {
                                            "✅ تم إرسال طلب تثبيت الويدجت إلى شاشة الهاتف الرئيسية وتحديث الأرقام!"
                                        } else {
                                            "✅ تم تحديث بيانات الويدجت! يمكنك أيضاً إضافته بالضغط مطولاً على شاشة الهاتف الرئيسية واختيار (Widgets -> ميزان المالي)."
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pin_home_widget_button")
                                ) {
                                    Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📌 تثبيت / تحديث ويدجت الشاشة الرئيسية الآن")
                                }

                                if (widgetFeedback != null) {
                                    Text(
                                        text = widgetFeedback!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: GOOGLE SHEETS LIVE SYNC & GOOGLE TASKS / KEEP CHECKLIST
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = SafeGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "1. المزامنة السحابية المباشرة مع Google Sheets (مجاناً 100%)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "اربط البرنامج بجدول بيانات جوجل في حسابك ليتم تحديث أوراق (المعاملات، الميزانيات، الديون) تلقائياً.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = sheetsUrlInput,
                                    onValueChange = { sheetsUrlInput = it },
                                    label = { Text("رابط Google Apps Script Web App (https://script.google.com/...)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("google_sheets_url_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مزامنة تلقائية مع Google Sheets عند تسجيل معاملة",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = autoSyncSheets,
                                        onCheckedChange = { autoSyncSheets = it }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onSaveGoogleSheetsSettings(sheetsUrlInput, autoSyncSheets)
                                            if (sheetsUrlInput.isNotBlank()) {
                                                onSyncGoogleSheetsNow()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sync_google_sheets_now_button")
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حفظ ومزامنة الآن")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cb?.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "GoogleAppsScript",
                                                    FreeIntegrationsHelper.getReadyGoogleAppsScriptCode()
                                                )
                                            )
                                            scriptCopiedNotice = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ كود الاسكربت")
                                    }
                                }

                                AnimatedVisibility(visible = scriptCopiedNotice) {
                                    Text(
                                        text = "✅ تم نسخ كود Google Apps Script! افتح أي ملف Google Sheet -> Extensions -> Apps Script والصق الكود ثم اضغط Deploy -> Web App.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // GOOGLE TASKS & GOOGLE KEEP CHECKLIST INTEGRATION
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EssentialBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "2. تصدير قائمة مهام الشهر إلى Google Tasks / Keep",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يرسل قائمة بجميع الأقساط والجمعيات والديون المستحقة هذا الشهر كقائمة مهام (Checklist).",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        FreeIntegrationsHelper.shareMonthlyChecklistToTasksOrKeep(
                                            context = context,
                                            commitments = commitments,
                                            burnRates = burnRates,
                                            metrics = metrics,
                                            currencySymbol = appSettings.currencySymbol
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("share_to_google_tasks_button")
                                ) {
                                    Text("📝 إرسال التزامات ومهام الشهر إلى Google Tasks / Keep")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}
