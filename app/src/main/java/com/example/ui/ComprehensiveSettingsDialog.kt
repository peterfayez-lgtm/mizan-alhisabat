package com.example.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.BurnRateStatus
import com.example.data.CategoryBudget
import com.example.data.CategoryForecastRow
import com.example.data.CsvSyncHelper
import com.example.data.DashboardMetrics
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinancialTransaction
import com.example.data.ForecastMode
import com.example.data.FreeIntegrationsHelper
import com.example.data.QuickTemplate
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import com.example.ui.theme.DangerRose
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComprehensiveSettingsDialog(
    appSettings: AppSettings,
    isDarkTheme: Boolean,
    isPrivacyMasked: Boolean,
    isPinConfigured: Boolean,
    isCloudSignedIn: Boolean,
    cloudEmail: String?,
    transactions: List<FinancialTransaction>,
    budgets: List<CategoryBudget>,
    templates: List<QuickTemplate>,
    commitments: List<LocalCommitmentEntity>,
    savingsGoals: List<LocalSavingsGoalEntity>,
    assetHoldings: List<LocalAssetHoldingEntity>,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    metrics: DashboardMetrics,
    burnRates: List<BurnRateStatus>,
    forecastRows: List<CategoryForecastRow>,
    forecastMode: ForecastMode,
    onUpdateAppSettings: (AppSettings) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onTogglePrivacyMask: () -> Unit,
    onOpenSecurityAndPinDialog: () -> Unit,
    onAddCustomCategory: (name: String, type: String) -> Unit,
    onDeleteCustomCategory: (name: String, type: String) -> Unit,
    onImportBackupOrCsvContent: (String) -> Unit,
    onRefreshLiveRates: () -> Unit,
    onOpenUpdateCenter: () -> Unit = {},
    onClearAllData: () -> Unit,
    onResetSeedData: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 0 = عام والمالية, 1 = الاستيراد والتصدير والنسخ الاحتياطي, 2 = الفئات والأدوات وإدارة البيانات
    var selectedSection by remember { mutableIntStateOf(0) }

    // Editable state for General & Financial Preferences
    var accountTitle by remember(appSettings) { mutableStateOf(appSettings.accountTitle) }
    var currencySymbol by remember(appSettings) { mutableStateOf(appSettings.currencySymbol) }
    var salaryDayText by remember(appSettings) { mutableStateOf(appSettings.salaryDayOfMonth.toString()) }
    var luxurySafeSlider by remember(appSettings) { mutableFloatStateOf(appSettings.luxurySafeThresholdPercent.toFloat()) }
    var defaultSpiritualMode by remember(appSettings) { mutableIntStateOf(appSettings.defaultSpiritualGivingMode) }
    var useGrossOrthodoxTithe by remember(appSettings) { mutableStateOf(appSettings.useGrossIncomeForOrthodoxTithe) }
    var autoMaskOnLaunch by remember(appSettings) { mutableStateOf(appSettings.autoMaskPrivacyOnLaunch) }
    var enableSmartAlerts by remember(appSettings) { mutableStateOf(appSettings.enableSmartBudgetAlerts) }

    // Custom categories management state
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("expense") }

    // Paste Backup / CSV state
    var showPasteBackupDialog by remember { mutableStateOf(false) }
    var confirmClearAllDialog by remember { mutableStateOf(false) }

    // File Picker for importing Full Backup (.mizan / .json) or CSV (.csv)
    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    onImportBackupOrCsvContent(content)
                }
            } catch (_: Exception) {
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            FreeIntegrationsHelper.sendSmartLocalNotification(
                context = context,
                title = "⚙️ ميزان المالي: التنبيهات الذكية مفعّلة",
                message = "المسموح صرفه اليوم: ${metrics.dailySafeToSpend.toInt()} $currencySymbol • المتبقي من الميزانية: ${metrics.remainingBudget.toInt()} $currencySymbol"
            )
        }
    }

    fun buildCurrentSettingsObject(): AppSettings = AppSettings(
        accountTitle = accountTitle.trim().ifBlank { "حسابي المالي الشخصي" },
        currencySymbol = currencySymbol,
        salaryDayOfMonth = (salaryDayText.toIntOrNull() ?: 1).coerceIn(1, 28),
        luxurySafeThresholdPercent = luxurySafeSlider.toInt().coerceIn(15, 50),
        defaultSpiritualGivingMode = defaultSpiritualMode,
        useGrossIncomeForOrthodoxTithe = useGrossOrthodoxTithe,
        autoMaskPrivacyOnLaunch = autoMaskOnLaunch,
        enableSmartBudgetAlerts = enableSmartAlerts
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "قائمة الإعدادات الشاملة ومركز البيانات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تحكم كامل في الهوية المالية، الاستيراد والتصدير، الفئات، والأمان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Selector Tabs
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        label = { Text("⚙️ عام والمالية") },
                        modifier = Modifier.testTag("settings_tab_general")
                    )
                    FilterChip(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        label = { Text("💾 الاستيراد والتصدير") },
                        modifier = Modifier.testTag("settings_tab_import_export")
                    )
                    FilterChip(
                        selected = selectedSection == 2,
                        onClick = { selectedSection = 2 },
                        label = { Text("🗂️ الفئات والبيانات") },
                        modifier = Modifier.testTag("settings_tab_categories_data")
                    )
                }

                HorizontalDivider()

                when (selectedSection) {
                    0 -> {
                        // =========================================================================
                        // القسم ١: الإعدادات العامة والتفضيلات المالية والمظهر
                        // =========================================================================
                        Text(
                            text = "1. الهوية والتفضيلات المالية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = accountTitle,
                            onValueChange = { accountTitle = it },
                            label = { Text("اسم الحساب / دفتر الميزانية") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "العملة الأساسية للبرنامج:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "ج.م" to "جنيه مصري (ج.م)",
                                "ر.س" to "ريال سعودي (ر.س)",
                                "د.إ" to "درهم إماراتي (د.إ)",
                                "$" to "دولار ($)",
                                "€" to "يورو (€)",
                                "د.ك" to "دينار كويتي (د.ك)"
                            ).forEach { (sym, label) ->
                                FilterChip(
                                    selected = currencySymbol == sym,
                                    onClick = { currencySymbol = sym },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = salaryDayText,
                            onValueChange = { salaryDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("يوم بداية الشهر المالي / يوم قبض الراتب (1 - 28)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "الحد الآمن لنسبة الرفاهيات: ${luxurySafeSlider.toInt()}% (تنبيه عند تجاوزها)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = luxurySafeSlider,
                            onValueChange = { luxurySafeSlider = it },
                            valueRange = 15f..50f,
                            steps = 6
                        )

                        HorizontalDivider()

                        Text(
                            text = "2. النظام الافتراضي لحاسبة الزكاة والعشور",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = defaultSpiritualMode == 1,
                                onClick = { defaultSpiritualMode = 1 },
                                label = { Text("١. الزكاة والصدقات (2.5%)") }
                            )
                            FilterChip(
                                selected = defaultSpiritualMode == 2,
                                onClick = { defaultSpiritualMode = 2 },
                                label = { Text("٢. العشور الأرثوذكسية (10%)") }
                            )
                        }

                        if (defaultSpiritualMode == 2) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = useGrossOrthodoxTithe,
                                    onClick = { useGrossOrthodoxTithe = true },
                                    label = { Text("10% من إجمالي الدخل الكلي") }
                                )
                                FilterChip(
                                    selected = !useGrossOrthodoxTithe,
                                    onClick = { useGrossOrthodoxTithe = false },
                                    label = { Text("10% من صافي الدخل بعد الضروريات") }
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "3. إعدادات المظهر والخصوصية والأمان",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الوضع الليلي المريح للعين (Dark Mode)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isDarkTheme, onCheckedChange = { onToggleDarkTheme() })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إخفاء الأرقام الآن (وضع الخصوصية الفوري 👁️)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isPrivacyMasked, onCheckedChange = { onTogglePrivacyMask() })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إخفاء الأرقام تلقائياً عند كل تشغيل للتطبيق", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = autoMaskOnLaunch, onCheckedChange = { autoMaskOnLaunch = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل تنبيهات تجاوز سقف الميزانية الفورية", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = enableSmartAlerts, onCheckedChange = { enableSmartAlerts = it })
                        }

                        FilledTonalButton(
                            onClick = onOpenSecurityAndPinDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_pin_and_cloud_from_settings")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isPinConfigured) "🔒 إدارة الرمز السري (PIN مفعّل) والمزامنة السحابية"
                                else "🔓 إعداد رمز قفل سري (PIN) والمزامنة السحابية"
                            )
                        }
                    }

                    1 -> {
                        // =========================================================================
                        // القسم ٢: مركز الاستيراد والتصدير والنسخ الاحتياطي الشامل
                        // =========================================================================
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SafeGreen.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "💾 مركز الاستيراد والتصدير والنسخ الاحتياطي الشامل",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeGreen
                                )
                                Text(
                                    text = "يمكنك تصدير واستيراد كل بياناتك (المعاملات، الميزانيات، الجمعيات، الأقساط، الحصالة، الذهب، الفئات المخصصة، والإعدادات) في ملف واحد مشفر (AES-256-GCM) أو بصيغة Excel CSV أو PDF.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Text(
                            text = "أولاً: النسخ الاحتياطي الشامل للنظام (كل البيانات + الإعدادات)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                val currentSettings = buildCurrentSettingsObject()
                                onUpdateAppSettings(currentSettings)
                                val encryptedFull = CsvSyncHelper.buildFullSystemEncryptedBackup(
                                    transactions = transactions,
                                    budgets = budgets,
                                    templates = templates,
                                    commitments = commitments,
                                    savingsGoals = savingsGoals,
                                    assetHoldings = assetHoldings,
                                    expenseCategories = expenseCategories,
                                    incomeCategories = incomeCategories,
                                    appSettings = currentSettings,
                                    encryptPayload = true
                                )
                                CsvSyncHelper.shareCsvText(
                                    context = context,
                                    fileName = "mizan_full_backup_encrypted.mizan",
                                    csvContent = encryptedFull
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_export_full_encrypted_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔒 تصدير نسخة احتياطية شاملة مشفرة (كل البيانات)")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { backupFilePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_import_file_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد من ملف")
                            }

                            FilledTonalButton(
                                onClick = { showPasteBackupDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_import_paste_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد باللصق")
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "ثانياً: تصدير واستيراد ملفات الإكسيل (Excel CSV) و PDF",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        FilledTonalButton(
                            onClick = {
                                val csv = CsvSyncHelper.buildTransactionsCsv(transactions)
                                CsvSyncHelper.shareCsvText(context, "mizan_transactions.csv", csv)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📊 تصدير سجل المعاملات لبرنامج الإكسيل (Excel CSV)")
                        }

                        FilledTonalButton(
                            onClick = {
                                val execCsv = CsvSyncHelper.buildExecutiveReportCsv(
                                    metrics = metrics,
                                    burnRates = burnRates,
                                    forecastRows = forecastRows,
                                    forecastMode = forecastMode
                                )
                                CsvSyncHelper.shareCsvText(context, "mizan_executive_report.csv", execCsv)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📈 تصدير التقرير التنفيذي والتنبؤات (Excel CSV)")
                        }

                        OutlinedButton(
                            onClick = {
                                FreeIntegrationsHelper.generateAndShareOfficialPdfReport(
                                    context = context,
                                    metrics = metrics,
                                    burnRates = burnRates,
                                    transactions = transactions
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📄 تصدير كشف حساب رسمي للطباعة والمشاركة (PDF)")
                        }
                    }

                    2 -> {
                        // =========================================================================
                        // القسم ٣: إدارة الفئات المخصصة، الأدوات الذكية، وإعادة ضبط البيانات
                        // =========================================================================
                        Text(
                            text = "1. إدارة الفئات والبنود المخصصة (+)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newCategoryType == "expense",
                                onClick = { newCategoryType = "expense" },
                                label = { Text("بند مصروفات") }
                            )
                            FilterChip(
                                selected = newCategoryType == "income",
                                onClick = { newCategoryType = "income" },
                                label = { Text("بند إيرادات") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = {
                                    Text(
                                        if (newCategoryType == "income") "اسم بند الإيراد الجديد..."
                                        else "اسم بند المصروف الجديد..."
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = newCategoryName.trim()
                                    if (clean.isNotBlank()) {
                                        onAddCustomCategory(clean, newCategoryType)
                                        newCategoryName = ""
                                    }
                                },
                                enabled = newCategoryName.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة")
                            }
                        }

                        val customExpenseOnly = expenseCategories.filter { it !in DefaultFinanceCatalog.expenseCategories }
                        val customIncomeOnly = incomeCategories.filter { it !in DefaultFinanceCatalog.incomeCategories }

                        if (customExpenseOnly.isNotEmpty() || customIncomeOnly.isNotEmpty()) {
                            Text(
                                text = "البنود المخصصة التي أضفتها (اضغط سلة الحذف لإزالتها):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            customExpenseOnly.forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• $cat (مصروف)", style = MaterialTheme.typography.bodySmall)
                                    IconButton(
                                        onClick = { onDeleteCustomCategory(cat, "expense") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف البند",
                                            tint = DangerRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            customIncomeOnly.forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• $cat (إيراد)", style = MaterialTheme.typography.bodySmall)
                                    IconButton(
                                        onClick = { onDeleteCustomCategory(cat, "income") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف البند",
                                            tint = DangerRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "2. التحديث اللحظي للعملات والتنبيهات الذكية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FilledTonalButton(
                            onClick = onRefreshLiveRates,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🌐 تحديث أسعار العملات الأجنبية والذهب تلقائياً الآن")
                        }

                        FilledTonalButton(
                            onClick = {
                                val sent = FreeIntegrationsHelper.sendSmartLocalNotification(
                                    context = context,
                                    title = "⚙️ ميزان المالي: الحد اليومي الآمن ${metrics.dailySafeToSpend.toInt()} $currencySymbol",
                                    message = "المتبقي من ميزانية الشهر: ${metrics.remainingBudget.toInt()} $currencySymbol • صافي الرصيد: ${metrics.netBalance.toInt()} $currencySymbol"
                                )
                                if (!sent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔔 اختبار وإرسال إشعار تنبيه ذكي على الهاتف")
                        }

                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onOpenUpdateCenter()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_open_update_center_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔄 مركز التحديثات المستقبلية وإرسال تحديث لكل الهواتف")
                        }

                        HorizontalDivider()

                        Text(
                            text = "3. إدارة قاعدة البيانات وإعادة الضبط",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRose
                        )

                        OutlinedButton(
                            onClick = onResetSeedData,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة البيانات التجريبية (للتجربة والمحاكاة - 4 شهور)")
                        }

                        Button(
                            onClick = { confirmClearAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_clear_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🗑️ مسح جميع البيانات والبدء من الصفر (0 ج.م)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateAppSettings(buildCurrentSettingsObject())
                    onDismiss()
                },
                modifier = Modifier.testTag("save_all_settings_button")
            ) {
                Text("حفظ الإعدادات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    if (showPasteBackupDialog) {
        var pastedContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteBackupDialog = false },
            title = { Text("استعادة نسخة احتياطية شاملة أو CSV", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق هنا كود النسخة الاحتياطية المشفرة (.mizan) أو محتوى ملف CSV ليتم فك تشفيره واستعادة جميع معاملاتك وإعداداتك فوراً:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pastedContent,
                        onValueChange = { pastedContent = it },
                        label = { Text("محتوى النسخة الاحتياطية المشفرة أو CSV") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedContent.isNotBlank()) {
                            onImportBackupOrCsvContent(pastedContent)
                            showPasteBackupDialog = false
                        }
                    },
                    enabled = pastedContent.isNotBlank()
                ) {
                    Text("استعادة ودمج الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteBackupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (confirmClearAllDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearAllDialog = false },
            title = { Text("تأكيد تصفير جميع البيانات (0 ج.م)", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من مسح جميع المعاملات والجمعيات والأهداف الحالية والبدء بصفحة بيضاء (0 ج.م)؟ يمكنك أخذ نسخة احتياطية أولاً من تبويب الاستيراد والتصدير.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        confirmClearAllDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRose)
                ) {
                    Text("نعم، تصفير البدء من الصفر")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAllDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }
}
