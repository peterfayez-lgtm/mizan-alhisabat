package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppSettings
import com.example.data.DashboardMetrics
import com.example.ui.theme.DangerRose
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyLimitControlDialog(
    appSettings: AppSettings,
    metrics: DashboardMetrics,
    onSaveDailySpendControl: (mode: Int, customDailyLimit: Double, customRemainingDaysOverride: Int) -> Unit,
    onSetTotalMonthlyBudget: (totalMonthlyBudget: Double, remainingDaysOverride: Int) -> Unit,
    onZeroAllBudgets: () -> Unit,
    onNavigateToBudgetsTab: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember(appSettings.dailySpendMode) {
        mutableIntStateOf(appSettings.dailySpendMode)
    }
    var customDailyText by remember(appSettings.customDailySafeLimit) {
        mutableStateOf(
            if (appSettings.customDailySafeLimit > 0.0) appSettings.customDailySafeLimit.toInt().toString() else "250"
        )
    }
    var totalMonthlyBudgetText by remember(metrics.totalMonthlyBudget) {
        mutableStateOf(
            if (metrics.totalMonthlyBudget > 0.0) metrics.totalMonthlyBudget.toInt().toString() else "9000"
        )
    }
    var remainingDaysOverride by remember(appSettings.customRemainingDaysOverride) {
        mutableIntStateOf(appSettings.customRemainingDaysOverride)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("daily_limit_control_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "التحكم في حد الصرف اليومي والميزانية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الحالة الحالية: ${metrics.dailySafeToSpend.toInt()} ${appSettings.currencySymbol} / يومياً (${metrics.dailyLimitSourceLabelAr})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_daily_limit_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Explanation Card: Why 18,700 appeared and how it works
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "من أين يُحسب حد الصرف اليومي؟ ولماذا ظهر رقم 18,700؟",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "• رقم 18,700 ج.م كان مجموع حدود الفئات الـ 7 الافتراضية في تبويب «الميزانيات» (طعام 4500 + سكن 6000 + تسوق 2200 + مواصلات 2000 + مطاعم 1800 + صحة 1200 + تعليم 1000 = 18,700 ج.م).\n" +
                                "• لأن تاريخ اليوم هو 31 في الشهر (متبقي 1 يوم)، قُسِم المبلغ على 1 يوم.\n" +
                                "• يمكنك الآن تصفيره تماماً إلى (0 ج.م)، أو تحديد مبلغ يومي ثابت بنفسك، أو ربطه بصافي رصيدك الفعلي!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick 1-Tap Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onZeroAllBudgets()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("zero_all_budgets_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = DangerRose)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصفير الحد والميزانيات (0 ج.م)", color = DangerRose, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigateToBudgetsTab()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("go_to_budgets_tab_from_dialog")
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تبويب الميزانيات")
                    }
                }

                HorizontalDivider()

                // Mode Selection
                Text(
                    text = "١. اختر طريقة حساب «حد الصرف اليومي»:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val modeOptions = listOf(
                    Triple(
                        0,
                        "تلقائي ذكي (0 ج.م عند التصفير)",
                        "يعرض 0 ج.م طالما لم تسجل معاملات، وعند إضافة راتب أو ميزانية يقسم المتبقي على الأيام"
                    ),
                    Triple(
                        1,
                        "حسب صافي الرصيد الفعلي في المحافظ",
                        "يقسم السيولة الحقيقية المتبقية في محافظك (${metrics.netBalance.toInt()} ج.م) على الأيام المتبقية"
                    ),
                    Triple(
                        2,
                        "تحديد مبلغ صرف يومي ثابت يدوياً",
                        "تكتب بنفسك أقصى مبلغ مسموح صرفه في اليوم الواحد (مثلاً 250 ج.م / يومياً)"
                    ),
                    Triple(
                        3,
                        "حسب سقف تبويب «الميزانيات» الشهرية",
                        "يقسم المتبقي من إجمالي ميزانيات الفئات الشهرية على الأيام المتبقية"
                    )
                )

                modeOptions.forEach { (modeId, title, desc) ->
                    val isSelected = selectedMode == modeId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMode = modeId }
                            .testTag("daily_mode_option_$modeId"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMode = modeId }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Input for Mode 2: Custom Fixed Daily Limit
                if (selectedMode == 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "✍️ اكتب حد الصرف اليومي الثابت الذي تريده:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = customDailyText,
                                onValueChange = { customDailyText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("الحد اليومي (${appSettings.currencySymbol} / يومياً)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_daily_limit_input")
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(100, 200, 300, 500, 1000).forEach { presetAmount ->
                                    FilterChip(
                                        selected = customDailyText == presetAmount.toString(),
                                        onClick = { customDailyText = presetAmount.toString() },
                                        label = { Text("$presetAmount ${appSettings.currencySymbol}/يوم") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Input for Mode 3: Set Total Monthly Budget directly
                if (selectedMode == 3) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "📊 حدد إجمالي ميزانية الشهر بالكامل (توزع تلقائياً على الفئات):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = totalMonthlyBudgetText,
                                onValueChange = { totalMonthlyBudgetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("إجمالي ميزانية الشهر (${appSettings.currencySymbol})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("total_monthly_budget_input")
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0, 6000, 9000, 12000, 15000, 20000).forEach { presetTotal ->
                                    FilterChip(
                                        selected = totalMonthlyBudgetText == presetTotal.toString(),
                                        onClick = { totalMonthlyBudgetText = presetTotal.toString() },
                                        label = { Text("$presetTotal ${appSettings.currencySymbol}") }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Remaining Days Control (Prevents dividing whole budget by 1 day on the 31st unless desired)
                Text(
                    text = "٢. عدد الأيام المقسوم عليها الميزانية (الأيام المتبقية):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إذا كان اليوم هو آخر يوم في الشهر (يوم 31)، يقسم الوضع التلقائي المبلغ على (1 يوم). يمكنك اختيار القسمة على 30 يوماً أو دورة مخصصة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dayPresets = listOf(
                        0 to "تلقائي حسب الشهر",
                        30 to "30 يوم (شهر كامل)",
                        20 to "20 يوم",
                        15 to "15 يوم (نصف شهر)",
                        10 to "10 أيام",
                        7 to "7 أيام (أسبوع)"
                    )
                    dayPresets.forEach { (daysVal, label) ->
                        FilterChip(
                            selected = remainingDaysOverride == daysVal,
                            onClick = { remainingDaysOverride = daysVal },
                            label = { Text(label) },
                            modifier = Modifier.testTag("remaining_days_chip_$daysVal")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Save Button
                Button(
                    onClick = {
                        if (selectedMode == 3) {
                            val totalBudget = totalMonthlyBudgetText.toDoubleOrNull() ?: 0.0
                            onSetTotalMonthlyBudget(totalBudget, remainingDaysOverride)
                        } else {
                            val customDaily = if (selectedMode == 2) {
                                customDailyText.toDoubleOrNull() ?: 0.0
                            } else {
                                0.0
                            }
                            onSaveDailySpendControl(selectedMode, customDaily, remainingDaysOverride)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_daily_limit_control_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ وتطبيق إعدادات حد الصرف اليومي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
