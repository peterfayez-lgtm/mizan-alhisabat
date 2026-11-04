package com.example.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.local.LocalFinanceDatabase
import com.example.data.local.toLocalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * قارئ إشعارات البنوك وإنستاباي وفودافون كاش الذكي (Smart Financial Notification Listener).
 * يلتقط إشعارات المعاملات المالية الواردة من تطبيقات البنوك، InstaPay، فودافون/اتصالات/أورنج كاش،
 * Telda، فوري، والرسائل النصية البنكية، ويحللها تلقائياً على الهاتف دون إرسال أي بيانات للخارج.
 */
class MizanNotificationListenerService : NotificationListenerService() {

    private val financialPackagesKeywords = listOf(
        "instapay",
        "vodafone",
        "cib",
        "nbe",
        "alahly",
        "banquemisr",
        "qnb",
        "alexbank",
        "fawry",
        "telda",
        "klivvr",
        "nexta",
        "etisalat",
        "orange",
        "we.pay",
        "messaging",
        "mms"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName?.lowercase() ?: return
        // Ignore our own notifications
        if (pkg == applicationContext.packageName.lowercase()) return

        val settings = FinanceRepository.loadAppSettingsFromContext(applicationContext)
        if (!settings.autoCaptureBankNotifications) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combinedText = "$title\n${bigText.ifBlank { text }}".trim()
        if (combinedText.isBlank()) return

        // Check if package or content matches banking / InstaPay / e-wallet patterns
        val isFinancialPkg = financialPackagesKeywords.any { pkg.contains(it) }
        val hasFinancialKeywords = combinedText.contains("ج.م") ||
            combinedText.contains("EGP", ignoreCase = true) ||
            combinedText.contains("إنستاباي") ||
            combinedText.contains("InstaPay", ignoreCase = true) ||
            combinedText.contains("فودافون كاش") ||
            combinedText.contains("تم خصم") ||
            combinedText.contains("تم تحويل") ||
            combinedText.contains("تم استلام") ||
            combinedText.contains("تم إيداع")

        if (!isFinancialPkg && !hasFinancialKeywords) return

        val parsed = FreeIntegrationsHelper.parseBankOrInstaPayMessage(combinedText) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LocalFinanceDatabase.getInstance(applicationContext).financeDao()
                val newTx = FinancialTransaction(
                    id = "auto_notif_${System.currentTimeMillis()}",
                    userId = FinanceRepository.OFFLINE_USER_ID,
                    title = parsed.title,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = parsed.category,
                    priority = parsed.priority,
                    recurrence = "variable",
                    wallet = parsed.wallet,
                    txDateEpochMs = System.currentTimeMillis()
                )
                dao.insertTransaction(newTx.toLocalEntity(FinanceRepository.OFFLINE_USER_ID))
                MizanHomeWidgetProvider.updateAllWidgets(applicationContext)

                val sign = if (parsed.type == "income") "+" else "-"
                FreeIntegrationsHelper.sendSmartLocalNotification(
                    context = applicationContext,
                    title = "⚡ تم تسجيل معاملة تلقائياً (${parsed.detectedSourceAr})",
                    message = "${parsed.title}: $sign${parsed.amount.toInt()} ${settings.currencySymbol} في الفئة «${parsed.category}» (${DefaultFinanceCatalog.walletLabelAr(parsed.wallet)})"
                )
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            return try {
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ) ?: return false
                val cn = ComponentName(context, MizanNotificationListenerService::class.java)
                flat.contains(cn.flattenToString())
            } catch (_: Exception) {
                false
            }
        }

        fun openNotificationListenerSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }
}
