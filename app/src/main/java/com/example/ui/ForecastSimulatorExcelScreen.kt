package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BurnRateStatus
import com.example.data.CategoryForecastRow
import com.example.data.CsvSyncHelper
import com.example.data.DashboardMetrics
import com.example.data.FinancialTransaction
import com.example.data.ForecastMode
import com.example.ui.theme.DangerRose
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForecastSimulatorExcelScreen(
    transactions: List<FinancialTransaction>,
    metrics: DashboardMetrics,
    burnRates: List<BurnRateStatus>,
    forecastRows: List<CategoryForecastRow>,
    forecastMode: ForecastMode,
    luxuryCutPercent: Float,
    selectedSimCategory: String,
    categoryCutPercent: Float,
    commitments: List<com.example.data.local.LocalCommitmentEntity> = emptyList(),
    onSelectForecastMode: (ForecastMode) -> Unit,
    onLuxuryCutChange: (Float) -> Unit,
    onSelectSimCategory: (String) -> Unit,
    onCategoryCutChange: (Float) -> Unit,
    onImportCsvContent: (String) -> Unit,
    onOpenSmartIntegrationsHub: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPasteCsvDialog by remember { mutableStateOf(false) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    onImportCsvContent(content)
                }
            } catch (_: Exception) {
            }
        }
    }

    val totalForecastNextMonth = remember(forecastRows) {
        forecastRows.sumOf { it.forecastedNextMonth }
    }

    // What-If Simulator Calculations
    val luxuryMonthlySaved = (metrics.luxurySpent * (luxuryCutPercent / 100.0)).roundToInt()
    val selectedCategoryForecast = forecastRows.firstOrNull { it.category == selectedSimCategory }?.forecastedNextMonth
        ?: (metrics.categoryTotals[selectedSimCategory] ?: 0.0)
    val categoryMonthlySaved = (selectedCategoryForecast * (categoryCutPercent / 100.0)).roundToInt()
    val totalMonthlySaved = luxuryMonthlySaved + categoryMonthlySaved
    val totalThreeMonthsSaved = totalMonthlySaved * 3

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("forecast_simulator_excel_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Advanced 4-Mode Forecasting Selector & Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "محرك التنبؤات",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "محرك التنبؤات المالية المتقدم للشهر القادم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = forecastMode.subtitleAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ForecastMode.entries.forEach { mode ->
                            FilterChip(
                                selected = forecastMode == mode,
                                onClick = { onSelectForecastMode(mode) },
                                label = { Text(mode.labelAr) }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "إجمالي المصروفات المتوقعة للشهر القادم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "بناءً على نموذج: ${forecastMode.labelAr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = "${totalForecastNextMonth.toInt()} ج.م",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive What-If Savings Simulator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("what_if_simulator_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "محاكي التوفير",
                            tint = SafeGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "محاكي التوفير التفاعلي (What-If Simulator)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "1. تقليص الصرف على الرفاهيات بنسبة ${luxuryCutPercent.toInt()}% (يوفر $luxuryMonthlySaved ج.م/شهرياً)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = luxuryCutPercent,
                        onValueChange = onLuxuryCutChange,
                        valueRange = 0f..50f,
                        steps = 9
                    )

                    Text(
                        text = "2. اختر فئة معينة لتجربة تقليصها (${categoryCutPercent.toInt()}%):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        forecastRows.forEach { row ->
                            FilterChip(
                                selected = selectedSimCategory == row.category,
                                onClick = { onSelectSimCategory(row.category) },
                                label = { Text(row.category) }
                            )
                        }
                    }
                    Slider(
                        value = categoryCutPercent,
                        onValueChange = onCategoryCutChange,
                        valueRange = 0f..50f,
                        steps = 9
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SafeGreen.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "الوفر الشهري الفوري",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "$totalMonthlySaved ج.م / شهر",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "الوفر المتراكم خلال 3 شهور",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "$totalThreeMonthsSaved ج.م",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Excel / CSV Export & Import Synchronization Hub (with \uFEFF Arabic BOM) + Multi-Sheet .XLS + Interactive .HTML + Google Sheets/Tasks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TableView,
                            contentDescription = "مزامنة Excel و CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مركز مزامنة وتصدير Excel / HTML / Google Sheets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تصدير ملف Excel متعدد الأوراق (.XLS) أو تقرير متصفح تفاعلي (.HTML) أو المزامنة الحية مع Google Sheets",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Multi-Sheet Excel (.XLS) & Interactive HTML Report
                    Button(
                        onClick = {
                            CsvSyncHelper.exportAndShareMultiSheetExcelWorkbook(
                                context = context,
                                transactions = transactions,
                                burnRates = burnRates,
                                commitments = commitments,
                                savingsGoals = emptyList(),
                                assetHoldings = emptyList(),
                                forecastRows = forecastRows,
                                metrics = metrics
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_multisheet_excel_xls_button")
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📊 تصدير ملف إكسيل شامل متعدد الأوراق (.XLS — 4 شيتات)")
                    }

                    FilledTonalButton(
                        onClick = {
                            CsvSyncHelper.exportAndShareInteractiveHtmlReport(
                                context = context,
                                transactions = transactions,
                                burnRates = burnRates,
                                metrics = metrics
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_interactive_html_report_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌐 تصدير تقرير تفاعلي ملون للمتصفح والكمبيوتر (.HTML)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenSmartIntegrationsHub,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_google_sheets_sync_button")
                        ) {
                            Text("☁️ ربط Google Sheets")
                        }

                        FilledTonalButton(
                            onClick = {
                                com.example.data.FreeIntegrationsHelper.shareMonthlyChecklistToTasksOrKeep(
                                    context = context,
                                    commitments = commitments,
                                    burnRates = burnRates,
                                    metrics = metrics
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_google_tasks_keep_button")
                        ) {
                            Text("📝 مهام Google Keep/Tasks")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = CsvSyncHelper.buildTransactionsCsv(transactions)
                                CsvSyncHelper.shareCsvText(context, "mizan_transactions.csv", csv)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_transactions_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير CSV")
                        }

                        FilledTonalButton(
                            onClick = {
                                val csv = CsvSyncHelper.buildExecutiveReportCsv(
                                    metrics = metrics,
                                    burnRates = burnRates,
                                    forecastRows = forecastRows,
                                    forecastMode = forecastMode
                                )
                                CsvSyncHelper.shareCsvText(context, "mizan_executive_report.csv", csv)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_executive_csv_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تقرير تنفيذي CSV")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { csvPickerLauncher.launch("text/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_csv_file_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد ملف CSV")
                        }

                        FilledTonalButton(
                            onClick = { showPasteCsvDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paste_csv_button")
                        ) {
                            Text("لصق بيانات CSV / مشفرة")
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            val encrypted = CsvSyncHelper.buildEncryptedBackupPayload(transactions)
                            CsvSyncHelper.shareCsvText(context, "mizan_encrypted_backup.mizan", encrypted)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_encrypted_backup_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔒 تصدير نسخة احتياطية مشفرة بالكامل (AES-256-GCM)")
                    }

                    Button(
                        onClick = {
                            com.example.data.FreeIntegrationsHelper.generateAndShareOfficialPdfReport(
                                context = context,
                                metrics = metrics,
                                burnRates = burnRates,
                                transactions = transactions
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_official_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📄 تصدير ومشاركة كشف حساب رسمي (PDF) للطباعة")
                    }
                }
            }
        }

        // 4. Category Forecast Breakdown Table
        item {
            Text(
                text = "جدول التنبؤ التفصيلي لكل فئة (${forecastMode.labelAr})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(forecastRows, key = { it.category }) { row ->
            val exceedsBudget = row.budgetLimit > 0 && row.forecastedNextMonth > row.budgetLimit
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "التوقع: ${row.forecastedNextMonth.toInt()} ج.م",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (exceedsBudget) DangerRose else SafeGreen
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "قبل 3 شهور: ${row.monthMinus3.toInt()}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "قبل شهرين: ${row.monthMinus2.toInt()}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "الشهر الماضي: ${row.monthMinus1.toInt()}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "السقف: ${row.budgetLimit.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningAmber
                        )
                    }
                }
            }
        }
    }

    if (showPasteCsvDialog) {
        var csvText by remember {
            mutableStateOf(
                "المعرف,العنوان,المبلغ,النوع,الفئة,الأولوية,التكرار,المحفظة,التاريخ,الطابع_الزمني\n" +
                    "imp_1,مشتريات لابتوب وإكسسوارات,850,expense,تسوق وترفيه,luxury,variable,bank,2026-10-30,${System.currentTimeMillis()}"
            )
        }
        AlertDialog(
            onDismissRequest = { showPasteCsvDialog = false },
            title = { Text("استيراد ودمج بيانات CSV", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("محتوى CSV") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsvContent(csvText)
                        showPasteCsvDialog = false
                    }
                ) {
                    Text("دمج البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCsvDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
