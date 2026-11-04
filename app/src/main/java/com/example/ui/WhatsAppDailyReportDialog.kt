package com.example.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppSettings
import com.example.data.DashboardMetrics
import com.example.data.FinancialTransaction
import com.example.data.FreeIntegrationsHelper
import com.example.data.WhatsAppMidnightScheduler
import com.example.ui.theme.SafeGreen
import kotlinx.coroutines.launch

private val WhatsAppGreen = Color(0xFF25D366)
private val TelegramBlue = Color(0xFF229ED9)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppDailyReportDialog(
    appSettings: AppSettings,
    transactions: List<FinancialTransaction>,
    metrics: DashboardMetrics,
    onSaveWhatsAppSettings: (
        phoneNumber: String,
        midnightAutoEnabled: Boolean,
        reportHour: Int,
        reportMinute: Int,
        callMeBotApiKey: String,
        webhookUrl: String,
        metaAccessToken: String,
        metaPhoneNumberId: String,
        metaWabaId: String,
        telegramBotEnabled: Boolean,
        telegramBotToken: String,
        telegramChatId: String
    ) -> Unit,
    onTestBackgroundSend: (summaryMessage: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember(appSettings.whatsappPhoneNumber) {
        mutableStateOf(appSettings.whatsappPhoneNumber.ifBlank { "201021992233" })
    }
    var callMeBotKeyInput by remember(appSettings.whatsappCallMeBotApiKey) {
        mutableStateOf(appSettings.whatsappCallMeBotApiKey.ifBlank { "3591043" })
    }
    var autoWhatsAppEnabled by remember(appSettings.whatsappMidnightAutoEnabled) {
        mutableStateOf(appSettings.whatsappMidnightAutoEnabled)
    }

    var selectedHour by remember(appSettings.whatsappReportHour) {
        mutableIntStateOf(appSettings.whatsappReportHour)
    }
    var selectedMinute by remember(appSettings.whatsappReportMinute) {
        mutableIntStateOf(appSettings.whatsappReportMinute)
    }
    var hourTextInput by remember(selectedHour) {
        mutableStateOf(selectedHour.toString().padStart(2, '0'))
    }
    var minuteTextInput by remember(selectedMinute) {
        mutableStateOf(selectedMinute.toString().padStart(2, '0'))
    }

    var telegramEnabled by remember(appSettings.telegramBotEnabled) {
        mutableStateOf(appSettings.telegramBotEnabled)
    }
    var telegramTokenInput by remember(appSettings.telegramBotToken) {
        mutableStateOf(appSettings.telegramBotToken)
    }
    var telegramChatIdInput by remember(appSettings.telegramChatId) {
        mutableStateOf(appSettings.telegramChatId)
    }

    var isTestingCallMeBot by remember { mutableStateOf(false) }
    var isTestingTelegram by remember { mutableStateOf(false) }
    var isFetchingTelegramChatId by remember { mutableStateOf(false) }
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isStatusSuccess by remember { mutableStateOf(false) }

    val liveSummaryPreview = remember(transactions, metrics, appSettings.currencySymbol) {
        FreeIntegrationsHelper.buildDailyTransactionsWhatsAppSummary(
            transactions = transactions,
            metrics = metrics,
            currencySymbol = appSettings.currencySymbol,
            includeClosingDayIfMidnight = false
        )
    }

    val normalizedPhonePreview = remember(phoneInput) {
        FreeIntegrationsHelper.normalizeWhatsAppPhoneNumber(phoneInput.ifBlank { "201021992233" })
    }

    fun persistCurrentSettings() {
        onSaveWhatsAppSettings(
            phoneInput.ifBlank { "201021992233" },
            autoWhatsAppEnabled,
            selectedHour.coerceIn(0, 23),
            selectedMinute.coerceIn(0, 59),
            callMeBotKeyInput.ifBlank { "3591043" },
            "",
            "",
            "",
            "",
            telegramEnabled,
            telegramTokenInput,
            telegramChatIdInput
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        FreeIntegrationsHelper.sendMidnightWhatsAppNotification(
            context = context,
            phoneNumber = normalizedPhonePreview,
            summaryText = liveSummaryPreview,
            autoSentInBackground = false
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("whatsapp_daily_report_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = WhatsAppGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "التقرير اليومي التلقائي (واتساب + تليجرام)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إرسال تلقائي عبر CallMeBot المفعّل وبوت تليجرام بالموعد الذي تحدده",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // 1. Primary Activated CallMeBot WhatsApp Card (201021992233 / 3591043)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WhatsAppGreen.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = WhatsAppGreen
                                )
                                Column {
                                    Text(
                                        text = "١. واتساب الأساسي (CallMeBot Activated)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsAppGreen
                                    )
                                    Text(
                                        text = "مفعّل كطريقة أساسية وحيدة للإرسال التلقائي للواتساب",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = autoWhatsAppEnabled,
                                onCheckedChange = { autoWhatsAppEnabled = it },
                                modifier = Modifier.testTag("whatsapp_auto_midnight_switch")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SafeGreen.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ CallMeBot API Activated for +$normalizedPhonePreview • API Key: $callMeBotKeyInput",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("رقم الواتساب المفعّل") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("whatsapp_phone_input")
                            )

                            OutlinedTextField(
                                value = callMeBotKeyInput,
                                onValueChange = { callMeBotKeyInput = it },
                                label = { Text("كود CallMeBot") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("whatsapp_apikey_input")
                            )
                        }

                        Button(
                            onClick = {
                                persistCurrentSettings()
                                isTestingCallMeBot = true
                                statusFeedbackMessage = null
                                coroutineScope.launch {
                                    val res = FreeIntegrationsHelper.sendBackgroundWhatsAppReport(
                                        phoneNumber = phoneInput.ifBlank { "201021992233" },
                                        callMeBotApiKey = callMeBotKeyInput.ifBlank { "3591043" },
                                        message = liveSummaryPreview
                                    )
                                    isTestingCallMeBot = false
                                    res.onSuccess { msg ->
                                        isStatusSuccess = true
                                        statusFeedbackMessage = msg
                                    }.onFailure { err ->
                                        isStatusSuccess = false
                                        statusFeedbackMessage = err.localizedMessage ?: "تعذر الإرسال عبر CallMeBot"
                                    }
                                }
                            },
                            enabled = !isTestingCallMeBot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_callmebot_now_button")
                        ) {
                            if (isTestingCallMeBot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارٍ إرسال التقرير عبر CallMeBot...")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🚀 إرسال التقرير فوراً عبر CallMeBot (${callMeBotKeyInput.ifBlank { "3591043" }})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Manual Schedule Time Picker Card (تحديد ميعاد إرسال التقرير يدوياً بالدقيقة والساعة)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "٢. تحديد ميعاد إرسال التقرير يدوياً",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اختر الساعة والدقيقة التي ينطلق فيها تقرير واتساب وتليجرام يومياً",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // عرض الموعد الحالي بخط واضح + زر فتح ساعة النظام اليدوية
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "الموعد المجدول حالياً:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = WhatsAppMidnightScheduler.formatScheduleTimeAr(selectedHour, selectedMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                selectedHour = hourOfDay.coerceIn(0, 23)
                                                selectedMinute = minute.coerceIn(0, 59)
                                                hourTextInput = selectedHour.toString().padStart(2, '0')
                                                minuteTextInput = selectedMinute.toString().padStart(2, '0')
                                            },
                                            selectedHour,
                                            selectedMinute,
                                            false
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("open_manual_time_picker_button")
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختيار من الساعة", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // التحكم اليدوي المباشر بالساعة والدقيقة (+/- أو كتابة الرقم مباشرة)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // حقل الساعة (0..23)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "الساعة (0 - 23)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedHour = (selectedHour - 1 + 24) % 24
                                                hourTextInput = selectedHour.toString().padStart(2, '0')
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص الساعة", modifier = Modifier.size(16.dp))
                                        }
                                        OutlinedTextField(
                                            value = hourTextInput,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(2)
                                                hourTextInput = digits
                                                digits.toIntOrNull()?.let { h ->
                                                    if (h in 0..23) selectedHour = h
                                                }
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(64.dp)
                                                .testTag("manual_hour_input")
                                        )
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedHour = (selectedHour + 1) % 24
                                                hourTextInput = selectedHour.toString().padStart(2, '0')
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة الساعة", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            // حقل الدقيقة (0..59)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "الدقيقة (0 - 59)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedMinute = (selectedMinute - 5 + 60) % 60
                                                minuteTextInput = selectedMinute.toString().padStart(2, '0')
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص الدقيقة", modifier = Modifier.size(16.dp))
                                        }
                                        OutlinedTextField(
                                            value = minuteTextInput,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { it.isDigit() }.take(2)
                                                minuteTextInput = digits
                                                digits.toIntOrNull()?.let { m ->
                                                    if (m in 0..59) selectedMinute = m
                                                }
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(64.dp)
                                                .testTag("manual_minute_input")
                                        )
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedMinute = (selectedMinute + 5) % 60
                                                minuteTextInput = selectedMinute.toString().padStart(2, '0')
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة الدقيقة", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // اختصارات سريعة للأوقات الأكثر استخداماً
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(0, 0, "🕛 ١٢:٠٠ منتصف الليل"),
                                Triple(23, 0, "🕚 ١١:٠٠ مساءً"),
                                Triple(22, 0, "🕙 ١٠:٠٠ مساءً"),
                                Triple(21, 0, "🕘 ٩:٠٠ مساءً"),
                                Triple(8, 0, "🕗 ٨:٠٠ صباحاً")
                            ).forEach { (h, m, label) ->
                                FilterChip(
                                    selected = selectedHour == h && selectedMinute == m,
                                    onClick = {
                                        selectedHour = h
                                        selectedMinute = m
                                        hourTextInput = h.toString().padStart(2, '0')
                                        minuteTextInput = m.toString().padStart(2, '0')
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // 3. Telegram Bot Integration Card (إرسال نفس التقرير عبر بوت تليجرام)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TelegramBlue.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = TelegramBlue
                                )
                                Column {
                                    Text(
                                        text = "٣. إرسال نفس التقرير عبر بوت تليجرام",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TelegramBlue
                                    )
                                    Text(
                                        text = "يرسل نفس ملخص اليوم تلقائياً على تليجرام في الموعد المحدد أعلاه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = telegramEnabled,
                                onCheckedChange = { telegramEnabled = it },
                                modifier = Modifier.testTag("telegram_bot_enable_switch")
                            )
                        }

                        Text(
                            text = "💡 خطوات سريعة (دقيقة واحدة فقط):\n" +
                                "١. افتح @BotFather في تليجرام وأرسل /newbot لتحصل على (Bot Token).\n" +
                                "٢. افتح البوت الجديد واضغط Start ثم اضغط زر «جلب Chat ID تلقائياً» بالأسفل.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = telegramTokenInput,
                            onValueChange = {
                                telegramTokenInput = it
                                if (it.isNotBlank() && !telegramEnabled) telegramEnabled = true
                            },
                            label = { Text("رمز بوت تليجرام (Telegram Bot Token)") },
                            placeholder = { Text("مثال: 7123456789:AAHxyz...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("telegram_bot_token_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = telegramChatIdInput,
                                onValueChange = { telegramChatIdInput = it },
                                label = { Text("معرف المحادثة (Chat ID)") },
                                placeholder = { Text("مثال: 123456789") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("telegram_chat_id_input")
                            )

                            OutlinedButton(
                                onClick = {
                                    isFetchingTelegramChatId = true
                                    statusFeedbackMessage = null
                                    coroutineScope.launch {
                                        val res = FreeIntegrationsHelper.fetchTelegramChatIdFromUpdates(telegramTokenInput)
                                        isFetchingTelegramChatId = false
                                        res.onSuccess { (detectedId, chatName) ->
                                            telegramChatIdInput = detectedId
                                            telegramEnabled = true
                                            isStatusSuccess = true
                                            statusFeedbackMessage = "✅ تم اكتشاف معرف المحادثة تلقائياً: $detectedId ($chatName)"
                                        }.onFailure { err ->
                                            isStatusSuccess = false
                                            statusFeedbackMessage = err.localizedMessage ?: "تعذر جلب Chat ID"
                                        }
                                    }
                                },
                                enabled = !isFetchingTelegramChatId,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("fetch_telegram_chat_id_button")
                            ) {
                                if (isFetchingTelegramChatId) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("جلب تلقائي", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    persistCurrentSettings()
                                    isTestingTelegram = true
                                    statusFeedbackMessage = null
                                    coroutineScope.launch {
                                        val res = FreeIntegrationsHelper.sendTelegramBotReport(
                                            botToken = telegramTokenInput,
                                            chatId = telegramChatIdInput,
                                            message = liveSummaryPreview
                                        )
                                        isTestingTelegram = false
                                        res.onSuccess { msg ->
                                            isStatusSuccess = true
                                            statusFeedbackMessage = msg
                                        }.onFailure { err ->
                                            isStatusSuccess = false
                                            statusFeedbackMessage = err.localizedMessage ?: "تعذر الإرسال عبر بوت تليجرام"
                                        }
                                    }
                                },
                                enabled = !isTestingTelegram,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelegramBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_telegram_bot_send_button")
                            ) {
                                if (isTestingTelegram) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جارٍ الإرسال...")
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال عبر بوت تليجرام الآن", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    FreeIntegrationsHelper.openDirectTelegramShare(
                                        context = context,
                                        message = liveSummaryPreview
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("share_direct_telegram_button")
                            ) {
                                Text("مشاركة بتليجرام", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Live Feedback Banner (for CallMeBot or Telegram Bot test results)
                if (statusFeedbackMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isStatusSuccess) SafeGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, if (isStatusSuccess) SafeGreen else MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isStatusSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isStatusSuccess) SafeGreen else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = statusFeedbackMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isStatusSuccess) SafeGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 4. Live Preview of Today's Report
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📋 معاينة حية لرسالة التقرير الموحد (واتساب + تليجرام):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = liveSummaryPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 5. Bottom Actions: Direct WhatsApp Send + Notification Test + Save Schedule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            persistCurrentSettings()
                            FreeIntegrationsHelper.openDirectWhatsAppMessage(
                                context = context,
                                phoneNumber = phoneInput.ifBlank { "201021992233" },
                                message = liveSummaryPreview
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_whatsapp_summary_now_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح المحادثة مباشرة", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            persistCurrentSettings()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                FreeIntegrationsHelper.sendMidnightWhatsAppNotification(
                                    context = context,
                                    phoneNumber = normalizedPhonePreview,
                                    summaryText = liveSummaryPreview,
                                    autoSentInBackground = false
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تجربة الإشعار", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = {
                        persistCurrentSettings()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_whatsapp_settings_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ وتفعيل الموعد (${WhatsAppMidnightScheduler.formatScheduleTimeAr(selectedHour, selectedMinute)})",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
