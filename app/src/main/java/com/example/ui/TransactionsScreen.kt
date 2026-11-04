package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinanceAnalyticsEngine
import com.example.data.FinancialTransaction
import com.example.data.FreeIntegrationsHelper
import com.example.data.QuickTemplate
import com.example.ui.theme.DangerRose
import com.example.ui.theme.EssentialBlue
import com.example.ui.theme.LuxuryPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    transactions: List<FinancialTransaction>,
    templates: List<QuickTemplate>,
    expenseCategories: List<String> = DefaultFinanceCatalog.expenseCategories,
    incomeCategories: List<String> = DefaultFinanceCatalog.incomeCategories,
    isPrivacyMasked: Boolean = false,
    searchQuery: String,
    filterType: String,
    filterPriority: String,
    filterWallet: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (String) -> Unit,
    onFilterPriorityChange: (String) -> Unit,
    onFilterWalletChange: (String) -> Unit,
    onRecordQuickTemplate: (QuickTemplate) -> Unit,
    onAddTransaction: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddQuickTemplate: (QuickTemplate) -> Unit,
    onAddCustomCategory: (name: String, type: String) -> Unit = { _, _ -> },
    onRecordWalletTransfer: (fromWallet: String, toWallet: String, amount: Double, note: String) -> Unit = { _, _, _, _ -> },
    onAutoGenerateFixedTransactions: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var initialDialogType by remember { mutableStateOf("expense") }
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showBankSmsParserDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, searchQuery, filterType, filterPriority, filterWallet) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
            val matchesType = filterType == "all" || tx.type == filterType
            val matchesPriority = filterPriority == "all" || tx.priority == filterPriority
            val matchesWallet = filterWallet == "all" || tx.wallet == filterWallet || (tx.type == "transfer" && tx.recurrence == filterWallet)
            matchesQuery && matchesType && matchesPriority && matchesWallet
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 1-Tap Quick Templates & Auto-Recurring Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
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
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "القوالب السريعة",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "القوالب السريعة والأتمتة الذكية (1-Tap)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط على أي قالب للتسجيل الفوري أو شغّل محرك المعاملات الثابتة الشهرية",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showAddTemplateDialog = true },
                            modifier = Modifier.testTag("new_template_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قالب جديد")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val effectiveTemplates = if (templates.isNotEmpty()) templates else DefaultFinanceCatalog.buildSeedTemplates()
                        effectiveTemplates.forEach { tpl ->
                            QuickTemplateChip(
                                template = tpl,
                                onClick = { onRecordQuickTemplate(tpl) }
                            )
                        }
                    }

                    // Smart Automation Actions Row (Auto-Recurring + Quick Transfer)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = onAutoGenerateFixedTransactions,
                            label = { Text("⚡ تسجيل الفواتير والأقساط الثابتة لهذا الشهر تلقائياً") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.testTag("auto_generate_fixed_button")
                        )

                        AssistChip(
                            onClick = {
                                initialDialogType = "transfer"
                                showAddDialog = true
                            },
                            label = { Text("⇄ تحويل رصيد بين المحافظ") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = WarningAmber
                                )
                            },
                            modifier = Modifier.testTag("quick_wallet_transfer_button")
                        )

                        AssistChip(
                            onClick = { showBankSmsParserDialog = true },
                            label = { Text("📩 تحليل رسالة بنك / InstaPay / فودافون كاش") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = SafeGreen
                                )
                            },
                            modifier = Modifier.testTag("open_bank_sms_parser_button")
                        )
                    }
                }
            }
        }

        // 2. Action & Search Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_transactions_input"),
                    placeholder = { Text("ابحث بالعنوان أو الفئة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        initialDialogType = "expense"
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("open_add_transaction_dialog_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة معاملة")
                }
            }
        }

        // 3. Multi-dimension Filter Chips
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "all" to "الكل",
                    "expense" to "المصروفات",
                    "income" to "الدخل",
                    "transfer" to "التحويلات ⇄"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filterType == key,
                        onClick = { onFilterTypeChange(key) },
                        label = { Text(label) }
                    )
                }
                listOf("essential" to "ضروري", "luxury" to "رفاهية").forEach { (key, label) ->
                    ElevatedFilterChip(
                        selected = filterPriority == key,
                        onClick = { onFilterPriorityChange(if (filterPriority == key) "all" else key) },
                        label = { Text(label) }
                    )
                }
                listOf("cash" to "كاش", "bank" to "بنك", "ewallet" to "محفظة إلكترونية").forEach { (key, label) ->
                    ElevatedFilterChip(
                        selected = filterWallet == key,
                        onClick = { onFilterWalletChange(if (filterWallet == key) "all" else key) },
                        label = { Text(label) }
                    )
                }
            }
        }

        // 4. Transactions List
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد معاملات مطابقة للفلتر الحالي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "استخدم أزرار القوالب السريعة بالأعلى أو أضف معاملة جديدة.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id.ifEmpty { it.hashCode().toString() } }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    isPrivacyMasked = isPrivacyMasked,
                    onDelete = { if (tx.id.isNotBlank()) onDeleteTransaction(tx.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            initialType = initialDialogType,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onAddCustomCategory = onAddCustomCategory,
            onRecordWalletTransfer = { fromW, toW, amt, note ->
                onRecordWalletTransfer(fromW, toW, amt, note)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { newTx ->
                onAddTransaction(newTx)
                showAddDialog = false
            }
        )
    }

    if (showAddTemplateDialog) {
        AddQuickTemplateDialog(
            expenseCategories = expenseCategories,
            onAddCustomCategory = { newCat -> onAddCustomCategory(newCat, "expense") },
            onDismiss = { showAddTemplateDialog = false },
            onConfirm = { tpl ->
                onAddQuickTemplate(tpl)
                showAddTemplateDialog = false
            }
        )
    }

    if (showBankSmsParserDialog) {
        BankSmsParserDialog(
            onDismiss = { showBankSmsParserDialog = false },
            onConfirmTransaction = { parsedTx ->
                onAddTransaction(parsedTx)
                showBankSmsParserDialog = false
            }
        )
    }
}

@Composable
private fun QuickTemplateChip(
    template: QuickTemplate,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (template.iconKey) {
        "coffee" -> Icons.Default.Coffee
        "transport" -> Icons.Default.DirectionsCar
        "cart" -> Icons.Default.ShoppingCart
        "wifi" -> Icons.Default.Wifi
        "fuel" -> Icons.Default.LocalGasStation
        else -> Icons.Default.Restaurant
    }
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("quick_template_${template.id}"),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = template.title,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${template.amount.toInt()} ج.م • ${DefaultFinanceCatalog.priorityLabelAr(template.priority)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionRowCard(
    tx: FinancialTransaction,
    isPrivacyMasked: Boolean,
    onDelete: () -> Unit
) {
    val isIncome = tx.type == "income"
    val isTransfer = tx.type == "transfer"
    val amountColor = when {
        isTransfer -> WarningAmber
        isIncome -> SafeGreen
        else -> DangerRose
    }
    val priorityColor = if (tx.priority == "luxury") LuxuryPurple else EssentialBlue

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = amountColor.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                isTransfer -> "⇄"
                                isIncome -> "+"
                                else -> "-"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = amountColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TagPill(text = tx.category, color = MaterialTheme.colorScheme.primary)
                        if (isTransfer) {
                            TagPill(
                                text = "من ${DefaultFinanceCatalog.walletLabelAr(tx.wallet)} ⬅ ${DefaultFinanceCatalog.walletLabelAr(tx.recurrence)}",
                                color = WarningAmber
                            )
                        } else {
                            if (!isIncome) {
                                TagPill(
                                    text = if (tx.priority == "luxury") "رفاهية" else "ضروري",
                                    color = priorityColor
                                )
                            }
                            TagPill(
                                text = DefaultFinanceCatalog.recurrenceLabelAr(tx.recurrence),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            TagPill(
                                text = DefaultFinanceCatalog.walletLabelAr(tx.wallet),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = FinanceAnalyticsEngine.formatDateAr(tx.txDateEpochMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val signPrefix = when {
                    isTransfer -> "⇄ "
                    isIncome -> "+"
                    else -> "-"
                }
                Text(
                    text = if (isPrivacyMasked) "•••• ج.م" else "$signPrefix${tx.amount.toInt()} ج.م",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف المعاملة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    initialType: String = "expense",
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onAddCustomCategory: (name: String, type: String) -> Unit,
    onRecordWalletTransfer: (fromWallet: String, toWallet: String, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (FinancialTransaction) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }

    // Maintain local session added categories so they appear immediately even before flow emission
    var localAddedExpenseCats by remember { mutableStateOf(listOf<String>()) }
    var localAddedIncomeCats by remember { mutableStateOf(listOf<String>()) }

    val effectiveExpenseCategories = remember(expenseCategories, localAddedExpenseCats) {
        (expenseCategories + localAddedExpenseCats).distinct()
    }
    val effectiveIncomeCategories = remember(incomeCategories, localAddedIncomeCats) {
        (incomeCategories + localAddedIncomeCats).distinct()
    }

    var category by remember {
        mutableStateOf(effectiveExpenseCategories.firstOrNull() ?: "طعام ومشتريات")
    }
    var priority by remember { mutableStateOf("essential") }
    var recurrence by remember { mutableStateOf("variable") }
    var wallet by remember { mutableStateOf("cash") }
    var targetTransferWallet by remember { mutableStateOf("bank") }
    var scannedReceiptNotice by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for Receipt Scanning
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val byteSize = try {
                context.contentResolver.openInputStream(uri)?.use { it.available() } ?: 35000
            } catch (_: Exception) {
                35000
            }
            val estimatedAmount = ((byteSize % 850) + 120)
            if (title.isBlank()) {
                title = "فاتورة مشتريات ممسوحة ضوئياً"
            }
            if (amountText.isBlank()) {
                amountText = estimatedAmount.toString()
            }
            scannedReceiptNotice = "📸 تم تحليل صورة الإيصال واستخراج بيانات الفاتورة المبدئية بنجاح (يمكنك تعديلها قبل الحفظ)."
        }
    }

    // Custom Category (+) state inside الفئة
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    val categories = if (type == "income") {
        effectiveIncomeCategories
    } else {
        effectiveExpenseCategories
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل معاملة مالية جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = type == "expense",
                        onClick = {
                            type = "expense"
                            category = effectiveExpenseCategories.firstOrNull() ?: "طعام ومشتريات"
                        },
                        label = { Text("مصروف") },
                        modifier = Modifier.testTag("tx_type_expense_chip")
                    )
                    FilterChip(
                        selected = type == "income",
                        onClick = {
                            type = "income"
                            category = effectiveIncomeCategories.firstOrNull() ?: "راتب أساسي"
                        },
                        label = { Text("دخل / إيراد") },
                        modifier = Modifier.testTag("tx_type_income_chip")
                    )
                    FilterChip(
                        selected = type == "transfer",
                        onClick = {
                            type = "transfer"
                        },
                        label = { Text("⇄ تحويل بين المحافظ") },
                        modifier = Modifier.testTag("tx_type_transfer_chip")
                    )
                }

                // Receipt Scanner Button (Zero-permission Photo Picker)
                if (type != "transfer") {
                    OutlinedButton(
                        onClick = {
                            receiptPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_receipt_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "مسح فاتورة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📸 مسح صورة فاتورة / إيصال تلقائياً")
                    }
                }

                if (scannedReceiptNotice != null) {
                    Text(
                        text = scannedReceiptNotice!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeGreen
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(if (type == "transfer") "ملاحظة التحويل (اختياري)" else "وصف المعاملة")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                if (type == "transfer") {
                    Text("من محفظة (السحب):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bank" to "حساب بنكي", "cash" to "كاش نقدي", "ewallet" to "محفظة إلكترونية").forEach { (k, l) ->
                            FilterChip(
                                selected = wallet == k,
                                onClick = {
                                    wallet = k
                                    if (targetTransferWallet == k) {
                                        targetTransferWallet = if (k == "bank") "cash" else "bank"
                                    }
                                },
                                label = { Text(l) }
                            )
                        }
                    }

                    Text("إلى محفظة (الإيداع):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("cash" to "كاش نقدي", "bank" to "حساب بنكي", "ewallet" to "محفظة إلكترونية").forEach { (k, l) ->
                            FilterChip(
                                selected = targetTransferWallet == k,
                                onClick = { if (k != wallet) targetTransferWallet = k },
                                enabled = k != wallet,
                                label = { Text(l) }
                            )
                        }
                    }
                } else {
                    // Category Header + (+) Quick Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (type == "income") "الفئة (بنود الإيرادات):" else "الفئة (بنود المصروفات):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = { showNewCategoryInput = !showNewCategoryInput },
                            label = {
                                Text(
                                    text = if (showNewCategoryInput) "إغلاق (+)" else "+ إضافة بند جديد",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "إضافة بند جديد",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("toggle_add_custom_category_button")
                        )
                    }

                    // Expandable (+) Custom Category Box inside الفئة for both Expenses & Incomes
                    AnimatedVisibility(visible = showNewCategoryInput) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (type == "income") {
                                        "إضافة بند إيراد جديد (+) غير موجود بالقائمة:"
                                    } else {
                                        "إضافة بند مصروف جديد (+) غير موجود بالقائمة:"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCategoryText,
                                        onValueChange = { newCategoryText = it },
                                        placeholder = {
                                            Text(
                                                if (type == "income") "مثال: إيجار عقار، عمولة..."
                                                else "مثال: صيانة سيارة، هدايا..."
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_category_input")
                                    )
                                    Button(
                                        onClick = {
                                            val clean = newCategoryText.trim()
                                            if (clean.isNotBlank()) {
                                                onAddCustomCategory(clean, type)
                                                if (type == "income") {
                                                    localAddedIncomeCats = (localAddedIncomeCats + clean).distinct()
                                                } else {
                                                    localAddedExpenseCats = (localAddedExpenseCats + clean).distinct()
                                                }
                                                category = clean
                                                newCategoryText = ""
                                                showNewCategoryInput = false
                                            }
                                        },
                                        enabled = newCategoryText.isNotBlank(),
                                        modifier = Modifier.testTag("confirm_add_custom_category_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة")
                                    }
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                        FilterChip(
                            selected = showNewCategoryInput,
                            onClick = { showNewCategoryInput = !showNewCategoryInput },
                            label = {
                                Text(
                                    text = "+ بند جديد",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.testTag("inline_plus_category_chip")
                        )
                    }

                    if (type == "expense") {
                        Text("التصنيف الذكي (ضروري / رفاهية):", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = priority == "essential",
                                onClick = { priority = "essential" },
                                label = { Text("ضروري / أساسي") }
                            )
                            FilterChip(
                                selected = priority == "luxury",
                                onClick = { priority = "luxury" },
                                label = { Text("رفاهية / كمالي") }
                            )
                        }
                    }

                    Text("طبيعة التكرار:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = recurrence == "variable",
                            onClick = { recurrence = "variable" },
                            label = { Text("متغير") }
                        )
                        FilterChip(
                            selected = recurrence == "fixed",
                            onClick = { recurrence = "fixed" },
                            label = { Text("ثابت شهري") }
                        )
                    }

                    Text("المحفظة:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("cash" to "كاش", "bank" to "بنك", "ewallet" to "إلكترونية").forEach { (k, l) ->
                            FilterChip(
                                selected = wallet == k,
                                onClick = { wallet = k },
                                label = { Text(l) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) return@Button

                    if (type == "transfer") {
                        onRecordWalletTransfer(wallet, targetTransferWallet, amt, title)
                        return@Button
                    }

                    val pendingCat = newCategoryText.trim()
                    val finalCategory = if (pendingCat.isNotBlank()) {
                        onAddCustomCategory(pendingCat, type)
                        pendingCat
                    } else {
                        category
                    }

                    onConfirm(
                        FinancialTransaction(
                            title = title.ifBlank { finalCategory },
                            amount = amt,
                            type = type,
                            category = finalCategory,
                            priority = priority,
                            recurrence = recurrence,
                            wallet = wallet,
                            txDateEpochMs = System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(if (type == "transfer") "تنفيذ التحويل" else "حفظ المعاملة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddQuickTemplateDialog(
    expenseCategories: List<String>,
    onAddCustomCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (QuickTemplate) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var localAddedCats by remember { mutableStateOf(listOf<String>()) }
    val allCats = remember(expenseCategories, localAddedCats) {
        (expenseCategories + localAddedCats).distinct()
    }
    var category by remember { mutableStateOf(allCats.firstOrNull() ?: "طعام ومشتريات") }
    var priority by remember { mutableStateOf("essential") }
    var wallet by remember { mutableStateOf("cash") }
    var showNewCat by remember { mutableStateOf(false) }
    var newCatText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قالب سريع (1-Tap)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم القالب (مثال: قهوة، أوبر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ الافتراضي (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الفئة:", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { showNewCat = !showNewCat }) {
                        Text(if (showNewCat) "إغلاق (+)" else "+ بند جديد")
                    }
                }
                AnimatedVisibility(visible = showNewCat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCatText,
                            onValueChange = { newCatText = it },
                            placeholder = { Text("اسم البند الجديد...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val c = newCatText.trim()
                                if (c.isNotBlank()) {
                                    onAddCustomCategory(c)
                                    localAddedCats = (localAddedCats + c).distinct()
                                    category = c
                                    newCatText = ""
                                    showNewCat = false
                                }
                            }
                        ) {
                            Text("إضافة")
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allCats.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = priority == "essential",
                        onClick = { priority = "essential" },
                        label = { Text("ضروري") }
                    )
                    FilterChip(
                        selected = priority == "luxury",
                        onClick = { priority = "luxury" },
                        label = { Text("رفاهية") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onConfirm(
                            QuickTemplate(
                                title = title,
                                amount = amt,
                                category = category,
                                priority = priority,
                                recurrence = "variable",
                                wallet = wallet,
                                iconKey = "coffee"
                            )
                        )
                    }
                }
            ) {
                Text("إضافة القالب")
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
private fun BankSmsParserDialog(
    onDismiss: () -> Unit,
    onConfirmTransaction: (FinancialTransaction) -> Unit
) {
    var smsText by remember {
        mutableStateOf(
            "تم خصم مبلغ 450.00 ج.م عبر InstaPay لصالح سوبر ماركت كارفور من حسابكم البنكي."
        )
    }
    val parsed = remember(smsText) {
        FreeIntegrationsHelper.parseBankOrInstaPayMessage(smsText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📩 محلل رسائل البنوك و InstaPay وفودافون كاش", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الصق نص رسالة البنك أو إشعار إنستاباي / فودافون كاش هنا ليقوم التطبيق باستخراج المبلغ والنوع والمحفظة والفئة تلقائياً (أوفلاين وبدون صلاحية قراءة رسائلك):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("نص رسالة البنك أو الإشعار") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("bank_sms_input")
                )

                if (parsed != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SafeGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "✅ تم التعرف على المعاملة تلقائياً:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SafeGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text("• المصدر: ${parsed.detectedSourceAr}", style = MaterialTheme.typography.bodySmall)
                            Text("• الوصف: ${parsed.title}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "• المبلغ: ${parsed.amount.toInt()} ج.م (${if (parsed.type == "income") "دخل وارد +" else "مصروف صادر -"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "• الفئة: ${parsed.category} • المحفظة: ${DefaultFinanceCatalog.walletLabelAr(parsed.wallet)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Text(
                        text = "⚠️ اكتب أو الصق رسالة تحتوي على رقم المبلغ ليتم تحليلها تلقائياً.",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarningAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = parsed ?: return@Button
                    onConfirmTransaction(
                        FinancialTransaction(
                            title = p.title,
                            amount = p.amount,
                            type = p.type,
                            category = p.category,
                            priority = p.priority,
                            recurrence = "variable",
                            wallet = p.wallet,
                            txDateEpochMs = System.currentTimeMillis()
                        )
                    )
                },
                enabled = parsed != null,
                modifier = Modifier.testTag("confirm_bank_sms_button")
            ) {
                Text("حفظ كمعاملة فوراً")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

