package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.LocalFinanceDatabase
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبِل وجدولة التقرير اليومي التلقائي عبر CallMeBot للواتساب وعبر بوت تليجرام في الموعد المحدد يدوياً
 */
class WhatsAppMidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val settings = FinanceRepository.loadAppSettingsFromContext(appContext)

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (settings.whatsappMidnightAutoEnabled || settings.telegramBotEnabled) {
                WhatsAppMidnightScheduler.scheduleOrCancelDailyMidnightReport(appContext, settings)
            }
            return
        }

        val isForceTest = intent?.getBooleanExtra("force_test_now", false) == true
        if (!settings.whatsappMidnightAutoEnabled && !settings.telegramBotEnabled && !isForceTest) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LocalFinanceDatabase.getInstance(appContext).financeDao()
                val txEntities = dao.getAllTransactionsSnapshot()
                val budgetEntities = dao.getAllBudgetsSnapshot()

                val transactions = txEntities.map { it.toDomain() }
                val budgets = if (budgetEntities.isNotEmpty()) {
                    budgetEntities.map { it.toDomain() }
                } else {
                    DefaultFinanceCatalog.buildSeedBudgets()
                }

                val metrics = FinanceAnalyticsEngine.computeDashboardMetrics(
                    transactions = transactions,
                    budgets = budgets,
                    appSettings = settings
                )

                val summaryMessage = FreeIntegrationsHelper.buildDailyTransactionsWhatsAppSummary(
                    transactions = transactions,
                    metrics = metrics,
                    currencySymbol = settings.currencySymbol,
                    includeClosingDayIfMidnight = true
                )

                // 1. الإرسال التلقائي الأساسي عبر CallMeBot للواتساب (201021992233 / 3591043)
                var autoSentSuccess = false
                if (settings.whatsappMidnightAutoEnabled || isForceTest) {
                    val result = FreeIntegrationsHelper.sendBackgroundWhatsAppReport(
                        phoneNumber = settings.whatsappPhoneNumber.ifBlank { "201021992233" },
                        callMeBotApiKey = settings.whatsappCallMeBotApiKey.ifBlank { "3591043" },
                        message = summaryMessage
                    )
                    autoSentSuccess = result.isSuccess
                }

                // 2. الإرسال التلقائي لنفس التقرير عبر بوت تليجرام إذا كان مفعلاً
                if (settings.telegramBotToken.isNotBlank() && settings.telegramChatId.isNotBlank() &&
                    (settings.telegramBotEnabled || isForceTest)
                ) {
                    val tgRes = FreeIntegrationsHelper.sendTelegramBotReport(
                        botToken = settings.telegramBotToken,
                        chatId = settings.telegramChatId,
                        message = summaryMessage
                    )
                    if (tgRes.isSuccess) {
                        autoSentSuccess = true
                    }
                }

                // 3. إرسال إشعار تأكيدي على الهاتف
                FreeIntegrationsHelper.sendMidnightWhatsAppNotification(
                    context = appContext,
                    phoneNumber = settings.whatsappPhoneNumber.ifBlank { "201021992233" },
                    summaryText = summaryMessage,
                    autoSentInBackground = autoSentSuccess
                )

                // 4. إعادة جدولة التقرير لليوم القادم في نفس الموعد المحدد يدوياً
                if (settings.whatsappMidnightAutoEnabled || settings.telegramBotEnabled) {
                    WhatsAppMidnightScheduler.scheduleOrCancelDailyMidnightReport(appContext, settings)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object WhatsAppMidnightScheduler {

    private const val ALARM_REQUEST_CODE = 12000
    const val ACTION_MIDNIGHT_WHATSAPP_REPORT = "com.example.ACTION_MIDNIGHT_WHATSAPP_REPORT"

    fun scheduleOrCancelDailyMidnightReport(context: Context, settings: AppSettings) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, WhatsAppMidnightReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_WHATSAPP_REPORT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            flags
        )

        val hasWhatsAppTarget = settings.whatsappMidnightAutoEnabled && settings.whatsappPhoneNumber.isNotBlank()
        val hasTelegramTarget = settings.telegramBotEnabled && settings.telegramBotToken.isNotBlank() && settings.telegramChatId.isNotBlank()

        if (!hasWhatsAppTarget && !hasTelegramTarget) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.whatsappReportHour.coerceIn(0, 23))
            set(Calendar.MINUTE, settings.whatsappReportMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                nextRun.timeInMillis,
                2 * 60 * 1000L, // نافذة دقيقة دقيقتين في الموعد المحدد
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                nextRun.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    fun formatScheduleTimeAr(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val period = if (h < 12) "صباحاً" else "مساءً"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val minStr = m.toString().padStart(2, '0')
        return if (h == 0 && m == 0) {
            "١٢:٠٠ منتصف الليل يومياً"
        } else {
            "$displayHour:$minStr $period يومياً"
        }
    }
}
