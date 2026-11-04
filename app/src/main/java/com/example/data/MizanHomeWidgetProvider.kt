package com.example.data

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalFinanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ويدجت الشاشة الرئيسية الذكي للهاتف (Android Home Screen AppWidget).
 * يعرض صافي السيولة الكلية، المسموح صرفه اليوم بأمان، وأرصدة المحافظ الـ 3،
 * مع أزرار سريعة لتسجيل معاملة جديدة أو فتح ملخص الواتساب اليومي بضغطة واحدة من خارج التطبيق.
 */
class MizanHomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "com.example.EXTRA_WIDGET_ACTION"
        const val ACTION_QUICK_ADD_TX = "QUICK_ADD_TX"
        const val ACTION_OPEN_WHATSAPP_REPORT = "OPEN_WHATSAPP_REPORT"

        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(appCtx)
                    val component = ComponentName(appCtx, MizanHomeWidgetProvider::class.java)
                    val ids = manager.getAppWidgetIds(component)
                    if (ids.isEmpty()) return@launch

                    val dao = LocalFinanceDatabase.getInstance(appCtx).financeDao()
                    val settings = FinanceRepository.loadAppSettingsFromContext(appCtx)
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

                    val currency = settings.currencySymbol
                    val cashBal = (metrics.walletBalances["cash"] ?: 0.0).toInt()
                    val bankBal = (metrics.walletBalances["bank"] ?: 0.0).toInt()
                    val ewalletBal = (metrics.walletBalances["ewallet"] ?: 0.0).toInt()

                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }

                    // 1. Open main app intent
                    val openAppIntent = Intent(appCtx, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val openAppPending = PendingIntent.getActivity(appCtx, 7001, openAppIntent, flags)

                    // 2. Quick Add Transaction intent
                    val quickAddIntent = Intent(appCtx, MainActivity::class.java).apply {
                        putExtra(EXTRA_WIDGET_ACTION, ACTION_QUICK_ADD_TX)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val quickAddPending = PendingIntent.getActivity(appCtx, 7002, quickAddIntent, flags)

                    // 3. WhatsApp Daily Report intent
                    val whatsappIntent = Intent(appCtx, MainActivity::class.java).apply {
                        putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_WHATSAPP_REPORT)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val whatsappPending = PendingIntent.getActivity(appCtx, 7003, whatsappIntent, flags)

                    for (widgetId in ids) {
                        val views = RemoteViews(appCtx.packageName, R.layout.mizan_home_widget)
                        views.setTextViewText(
                            R.id.widget_net_balance,
                            "الرصيد: ${metrics.netBalance.toInt()} $currency"
                        )
                        views.setTextViewText(
                            R.id.widget_daily_safe_value,
                            "${metrics.dailySafeToSpend.toInt()} $currency / يومياً"
                        )
                        views.setTextViewText(
                            R.id.widget_wallets_summary,
                            "كاش: $cashBal • بنك: $bankBal • محفظة: $ewalletBal $currency"
                        )

                        views.setOnClickPendingIntent(R.id.widget_root_container, openAppPending)
                        views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPending)
                        views.setOnClickPendingIntent(R.id.widget_btn_whatsapp, whatsappPending)

                        manager.updateAppWidget(widgetId, views)
                    }
                } catch (_: Exception) {
                }
            }
        }

        /**
         * يطلب تثبيت ويدجت الشاشة الرئيسية مباشرة على شاشة الهاتف (Android 8.0+).
         */
        fun requestPinHomeWidget(context: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val provider = ComponentName(context, MizanHomeWidgetProvider::class.java)
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                        return true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}
