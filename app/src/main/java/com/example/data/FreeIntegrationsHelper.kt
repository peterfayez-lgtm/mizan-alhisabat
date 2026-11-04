package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ZakatAndTithesSummary(
    // 1. الزكاة والصدقات (نصاب 85 جرام ذهب عيار 21 - 2.5%)
    val gold21GramRateEgp: Double,
    val nisabThresholdEgp: Double,
    val liquidWalletsBalanceEgp: Double,
    val goldAndForeignAssetsEgp: Double,
    val receivableDebtsEgp: Double,
    val deductibleDebtsDueEgp: Double,
    val netZakatableWealthEgp: Double,
    val isNisabReached: Boolean,
    val annualZakat2Point5Percent: Double,
    val monthlyZakatDistribution: Double,
    val paidZakatAndSadaqahThisMonth: Double,
    // 2. العشور والعطايا (المسيحية الأرثوذكسية - 10% من الدخل)
    val grossMonthlyIncomeEgp: Double,
    val essentialMonthlyExpensesEgp: Double,
    val netMonthlyIncomeAfterEssentialsEgp: Double,
    val orthodoxTitheGross10Percent: Double,
    val orthodoxTitheNet10Percent: Double,
    val paidTithesAndOfferingsThisMonth: Double,
    val remainingGrossTitheThisMonth: Double,
    val remainingNetTitheThisMonth: Double
)

data class ParsedBankSms(
    val title: String,
    val amount: Double,
    val type: String, // "expense" | "income" | "transfer"
    val category: String,
    val wallet: String, // "bank" | "ewallet" | "cash"
    val targetWallet: String = "bank",
    val priority: String = "essential",
    val detectedSourceAr: String
)

data class FinancialHealthReport(
    val scoreOutOf100: Int,
    val gradeTitleAr: String,
    val savingsRatePercent: Int,
    val luxuryRatioPercent: Int,
    val emergencyFundCoveragePercent: Int,
    val fastBurningCategoriesCount: Int,
    val actionableRecommendations: List<String>
)

object FreeIntegrationsHelper {

    private const val NOTIFICATION_CHANNEL_ID = "mizan_smart_alerts"

    /**
     * 1. حاسبة الزكاة والعشور الذكية:
     * - الخيار رقم 1: الزكاة والصدقات (نصاب 85 جرام ذهب عيار 21، ربع العشر 2.5% من صافي الوعاء الزكوي).
     * - الخيار رقم 2: العشور والعطايا في الكنيسة المسيحية الأرثوذكسية (10% من إجمالي الدخل الكلي أو صافي الدخل).
     */
    fun computeZakatAndTithesSummary(
        metrics: DashboardMetrics,
        transactions: List<FinancialTransaction>,
        commitments: List<LocalCommitmentEntity>,
        savingsGoals: List<LocalSavingsGoalEntity>,
        assetHoldings: List<LocalAssetHoldingEntity>
    ): ZakatAndTithesSummary {
        val currentMonthKey = FinanceAnalyticsEngine.yearMonthKey(System.currentTimeMillis())
        val thisMonthTxs = transactions.filter {
            FinanceAnalyticsEngine.yearMonthKey(it.txDateEpochMs) == currentMonthKey
        }

        // Gold 21K rate from user's portfolio or fallback 3750 EGP
        val gold21Rate = assetHoldings
            .firstOrNull { it.assetCode.equals("GOLD_21", ignoreCase = true) }
            ?.unitRateInEgp
            ?.takeIf { it > 0.0 }
            ?: 3750.0

        // Nisab = 85 grams of 21K gold
        val nisabThreshold = 85.0 * gold21Rate

        val liquidWallets = max(0.0, metrics.netBalance)
        val goalsSaved = savingsGoals.sumOf { it.currentSavedAmount }
        val assetsTotal = assetHoldings.sumOf { it.quantity * it.unitRateInEgp }

        val receivableDebts = commitments
            .filter { it.type == "debt_receivable" && it.paidInstallmentsCount < it.totalInstallmentsCount }
            .sumOf { (it.totalInstallmentsCount - it.paidInstallmentsCount) * it.monthlyInstallment }

        val owedShortTermDebts = commitments
            .filter { it.type != "debt_receivable" && it.paidInstallmentsCount < it.totalInstallmentsCount }
            .sumOf { it.monthlyInstallment }

        val netZakatableWealth = max(
            0.0,
            liquidWallets + goalsSaved + assetsTotal + receivableDebts - owedShortTermDebts
        )
        val isNisabReached = netZakatableWealth >= nisabThreshold
        val annualZakat = if (isNisabReached) netZakatableWealth * 0.025 else 0.0
        val monthlyZakat = annualZakat / 12.0

        val paidZakatThisMonth = thisMonthTxs
            .filter {
                it.type == "expense" && (
                    it.category.contains("زكاة") ||
                        it.category.contains("صدقة") ||
                        it.category.contains("صدقات") ||
                        it.title.contains("زكاة") ||
                        it.title.contains("صدقة")
                    )
            }
            .sumOf { it.amount }

        // 2. العشور في الكنيسة المسيحية الأرثوذكسية (10% من الدخل)
        val grossIncome = max(0.0, metrics.currentMonthIncome)
        val essentialExpenses = max(0.0, metrics.essentialSpent)
        val netIncomeAfterEssentials = max(0.0, grossIncome - essentialExpenses)

        val grossTithe10 = grossIncome * 0.10
        val netTithe10 = netIncomeAfterEssentials * 0.10

        val paidTithesThisMonth = thisMonthTxs
            .filter {
                it.type == "expense" && (
                    it.category.contains("عشور") ||
                        it.category.contains("عطايا") ||
                        it.category.contains("إخوة الرب") ||
                        it.category.contains("بكور") ||
                        it.title.contains("عشور") ||
                        it.title.contains("عطايا") ||
                        it.title.contains("عطية") ||
                        it.title.contains("إخوة الرب") ||
                        it.title.contains("بكور")
                    )
            }
            .sumOf { it.amount }

        return ZakatAndTithesSummary(
            gold21GramRateEgp = gold21Rate,
            nisabThresholdEgp = nisabThreshold,
            liquidWalletsBalanceEgp = liquidWallets + goalsSaved,
            goldAndForeignAssetsEgp = assetsTotal,
            receivableDebtsEgp = receivableDebts,
            deductibleDebtsDueEgp = owedShortTermDebts,
            netZakatableWealthEgp = netZakatableWealth,
            isNisabReached = isNisabReached,
            annualZakat2Point5Percent = annualZakat,
            monthlyZakatDistribution = monthlyZakat,
            paidZakatAndSadaqahThisMonth = paidZakatThisMonth,
            grossMonthlyIncomeEgp = grossIncome,
            essentialMonthlyExpensesEgp = essentialExpenses,
            netMonthlyIncomeAfterEssentialsEgp = netIncomeAfterEssentials,
            orthodoxTitheGross10Percent = grossTithe10,
            orthodoxTitheNet10Percent = netTithe10,
            paidTithesAndOfferingsThisMonth = paidTithesThisMonth,
            remainingGrossTitheThisMonth = max(0.0, grossTithe10 - paidTithesThisMonth),
            remainingNetTitheThisMonth = max(0.0, netTithe10 - paidTithesThisMonth)
        )
    }

    /**
     * 2. جلب أسعار صرف العملات والذهب الحية مجاناً عبر الإنترنت (Open Exchange Rates API)
     * مع دعم عدم الاتصال بالإنترنت (Offline Fallback).
     */
    suspend fun fetchLiveRatesInEgp(): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val rates = json.getJSONObject("rates")
            val usdToEgp = rates.optDouble("EGP", 49.60)
            val usdToSar = rates.optDouble("SAR", 3.75).coerceAtLeast(0.01)
            val usdToEur = rates.optDouble("EUR", 0.92).coerceAtLeast(0.01)
            val usdToAed = rates.optDouble("AED", 3.67).coerceAtLeast(0.01)

            val sarToEgp = (usdToEgp / usdToSar * 100.0).roundToInt() / 100.0
            val eurToEgp = (usdToEgp / usdToEur * 100.0).roundToInt() / 100.0
            val aedToEgp = (usdToEgp / usdToAed * 100.0).roundToInt() / 100.0

            // Approximate international gold ounce to 24K & 21K gram in EGP
            val gold24GramEgp = (usdToEgp * 86.5).roundToInt().toDouble()
            val gold21GramEgp = (gold24GramEgp * (21.0 / 24.0)).roundToInt().toDouble()

            Result.success(
                mapOf(
                    "USD" to ((usdToEgp * 100.0).roundToInt() / 100.0),
                    "SAR" to sarToEgp,
                    "EUR" to eurToEgp,
                    "AED" to aedToEgp,
                    "GOLD_24" to gold24GramEgp,
                    "GOLD_21" to gold21GramEgp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 3. الربط المجاني مع تقويم الهاتف وتقويم جوجل (Google Calendar / Android Calendar Intent)
     * لتذكير المستخدم بموعد قسط الجمعية أو القسط الشهري تلقائياً كل شهر.
     */
    fun openCalendarReminder(
        context: Context,
        title: String,
        monthlyAmount: Double,
        dueDayOfMonth: Int
    ) {
        try {
            val now = Calendar.getInstance()
            val eventStart = Calendar.getInstance().apply {
                val safeDay = dueDayOfMonth.coerceIn(1, 28)
                set(Calendar.DAY_OF_MONTH, safeDay)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) {
                    add(Calendar.MONTH, 1)
                }
            }
            val eventEnd = (eventStart.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "موعد سداد: $title (${monthlyAmount.toInt()} ج.م)")
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "تذكير تلقائي من تطبيق ميزان المالي الذكي لسداد القسط/الجمعية الشهرية بقيمة ${monthlyAmount.toInt()} ج.م."
                )
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventStart.timeInMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventEnd.timeInMillis)
                putExtra(CalendarContract.Events.RRULE, "FREQ=MONTHLY;BYMONTHDAY=${dueDayOfMonth.coerceIn(1, 28)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * 4. محلل رسائل البنوك وإنستاباي وفودافون كاش الذكي (بدون صلاحية قراءة الرسائل)
     */
    fun parseBankOrInstaPayMessage(rawText: String): ParsedBankSms? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        // Normalize Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩) to Western digits
        val normalized = buildString {
            for (ch in text) {
                when (ch) {
                    in '٠'..'٩' -> append((ch - '٠'))
                    '٫' -> append('.')
                    '،' -> append(',')
                    else -> append(ch)
                }
            }
        }

        // Extract amount (look for numbers before/after EGP, ج.م, جنيه, بمبلغ, مبلغ, GBP, USD)
        val amountRegex = Regex(
            """(?:بمبلغ|مبلغ|قيمة|EGP|ج\.م|جنيه|تم خصم|تم تحويل|تم استلام|تم إيداع|Paid|Amount|debited|credited)\s*[:\-]?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)|([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:EGP|ج\.م|جنيه)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(normalized)
        val rawAmountStr = match?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
            ?: Regex("""\b([0-9]{2,7}(?:\.[0-9]{1,2})?)\b""").find(normalized)?.groupValues?.getOrNull(1)
            ?: return null

        val amount = rawAmountStr.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val lower = normalized.lowercase(Locale.ROOT)

        // Detect source & wallet
        val isInstaPay = lower.contains("instapay") || normalized.contains("إنستاباي") || normalized.contains("انستاباي")
        val isVodafoneCash = lower.contains("vodafone") || normalized.contains("فودافون كاش") || normalized.contains("اتصالات كاش") || normalized.contains("أورنج كاش") || normalized.contains("محفظة")
        val wallet = when {
            isVodafoneCash -> "ewallet"
            isInstaPay -> "bank"
            else -> "bank"
        }
        val sourceAr = when {
            isInstaPay -> "InstaPay (إنستاباي)"
            isVodafoneCash -> "محفظة إلكترونية (فودافون/اتصالات كاش)"
            else -> "إشعار بنكي / بطاقة"
        }

        // Detect transaction type
        val isIncome = normalized.contains("تم استلام") ||
            normalized.contains("تم إيداع") ||
            normalized.contains("إضافة مبلغ") ||
            normalized.contains("وارد") ||
             normalized.contains("مرتب") ||
            lower.contains("received") ||
            lower.contains("credited")

        val type = if (isIncome) "income" else "expense"

        // Detect category & merchant
        val category = when {
            isIncome -> "عمل حر ومشاريع"
            normalized.contains("سوبر ماركت") || normalized.contains("كارفور") || normalized.contains("طلبات") || normalized.contains("كازيون") -> "طعام ومشتريات"
            normalized.contains("أوبر") || normalized.contains("كريم") || normalized.contains("بنزين") || normalized.contains("وقود") -> "مواصلات ووقود"
            normalized.contains("مطعم") || normalized.contains("كافيه") || normalized.contains("قهوة") -> "مطاعم وكافيهات"
            normalized.contains("كهرباء") || normalized.contains("إنترنت") || normalized.contains("فوري") || normalized.contains("فاتورة") -> "سكن وفواتير"
            normalized.contains("صيدلية") || normalized.contains("مستشفى") || normalized.contains("عيادة") -> "صحة وعلاج"
            else -> "تسوق وترفيه"
        }

        val title = when {
            isInstaPay && isIncome -> "تحويل وارد عبر InstaPay"
            isInstaPay -> "تحويل صادر عبر InstaPay"
            isVodafoneCash && isIncome -> "إيداع وارد بالمحفظة الإلكترونية"
            isVodafoneCash -> "معاملة محفظة إلكترونية"
            isIncome -> "إيداع بنكي وارد"
            else -> "معاملة بطاقة / بنك ($category)"
        }

        return ParsedBankSms(
            title = title,
            amount = amount,
            type = type,
            category = category,
            wallet = wallet,
            priority = if (category in listOf("مطاعم وكافيهات", "تسوق وترفيه")) "luxury" else "essential",
            detectedSourceAr = sourceAr
        )
    }

    /**
     * 5. مؤشر الصحة المالية الذكي (Financial Health Score 0–100)
     */
    fun computeFinancialHealthReport(
        metrics: DashboardMetrics,
        burnRates: List<BurnRateStatus>,
        savingsGoals: List<LocalSavingsGoalEntity>
    ): FinancialHealthReport {
        val income = metrics.currentMonthIncome.coerceAtLeast(1.0)
        val expense = metrics.currentMonthExpense
        val savingsRate = ((income - expense) / income).coerceIn(-0.5, 1.0)
        val savingsRatePercent = (savingsRate * 100).roundToInt().coerceAtLeast(0)

        // 1. Savings score (up to 35 pts — 20%+ savings gets full 35 pts)
        val savingsPoints = ((savingsRate / 0.20) * 35.0).roundToInt().coerceIn(0, 35)

        // 2. Luxury control score (up to 25 pts — luxury <= 30% gets full points)
        val luxuryRatio = metrics.luxuryRatio.toDouble()
        val luxuryPoints = when {
            luxuryRatio <= 0.20 -> 25
            luxuryRatio <= 0.30 -> 20
            luxuryRatio <= 0.40 -> 12
            else -> 5
        }

        // 3. Burn-rate & budget discipline score (up to 25 pts)
        val fastBurningCount = burnRates.count { it.isBurningFast || it.statusLevel == "exceeded" }
        val budgetPoints = (25 - fastBurningCount * 7).coerceIn(0, 25)

        // 4. Emergency fund & savings goals progress (up to 15 pts)
        val totalTarget = savingsGoals.sumOf { it.targetAmount }.coerceAtLeast(1.0)
        val totalSaved = savingsGoals.sumOf { it.currentSavedAmount }
        val goalsCoverageRatio = (totalSaved / totalTarget).coerceIn(0.0, 1.0)
        val emergencyPoints = (goalsCoverageRatio * 15.0).roundToInt().coerceIn(0, 15)

        val totalScore = (savingsPoints + luxuryPoints + budgetPoints + emergencyPoints).coerceIn(10, 100)

        val gradeAr = when {
            totalScore >= 85 -> "ممتاز — إدارة مالية احترافية 🌟"
            totalScore >= 70 -> "جيد جداً — توازن مالي مستقر ✅"
            totalScore >= 55 -> "متوسط — يحتاج ضبط بعض الفئات ⚡"
            else -> "يحتاج تدخل — راجع سرعة الصرف والرفاهيات ⚠️"
        }

        val tips = mutableListOf<String>()
        if (savingsRatePercent < 15) {
            tips.add("ارفع نسبة الادخار الشهري إلى 20% من الدخل لتعزيز الأمان المالي.")
        } else {
            tips.add("معدل ادخارك الشهري الحالي ($savingsRatePercent%) ممتاز ويحقق نمواً مستداماً.")
        }
        if (metrics.isLuxuryWarning) {
            tips.add("الرفاهيات تجاوزت 30% من مصروفاتك؛ جرّب محاكي التوفير لخفضها.")
        }
        if (fastBurningCount > 0) {
            tips.add("يوجد $fastBurningCount فئة تحترق ميزانيتها أسرع من أيام الشهر.")
        }

        return FinancialHealthReport(
            scoreOutOf100 = totalScore,
            gradeTitleAr = gradeAr,
            savingsRatePercent = savingsRatePercent,
            luxuryRatioPercent = (metrics.luxuryRatio * 100).roundToInt(),
            emergencyFundCoveragePercent = (goalsCoverageRatio * 100).roundToInt(),
            fastBurningCategoriesCount = fastBurningCount,
            actionableRecommendations = tips
        )
    }

    /**
     * 6. مولد التقارير وكشف الحساب الرسمي بصيغة PDF (Native Android PdfDocument)
     */
    fun generateAndShareOfficialPdfReport(
        context: Context,
        metrics: DashboardMetrics,
        burnRates: List<BurnRateStatus>,
        transactions: List<FinancialTransaction>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(6, 95, 70)
                textSize = 20f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 12f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1.5f
            }

            var y = 48f
            canvas.drawText("ميزان المالي الذكي - كشف حساب وتقرير مالي رسمي", 40f, y, titlePaint)
            y += 24f
            canvas.drawText(
                "تاريخ التقرير: ${FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())}",
                40f,
                y,
                bodyPaint
            )
            y += 16f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 28f

            canvas.drawText("أولاً: ملخص الأداء المالي الشهري", 40f, y, headerPaint)
            y += 22f
            canvas.drawText("• إجمالي دخل الشهر: ${metrics.currentMonthIncome.toInt()} ج.م", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText("• إجمالي مصروفات الشهر: ${metrics.currentMonthExpense.toInt()} ج.م", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText("• صافي الرصيد الكلي: ${metrics.netBalance.toInt()} ج.م", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText("• الحد اليومي الآمن للصرف: ${metrics.dailySafeToSpend.toInt()} ج.م / يوم", 50f, y, bodyPaint)
            y += 18f
            canvas.drawText(
                "• نسبة الضروريات: ${(metrics.essentialRatio * 100).toInt()}% | نسبة الرفاهيات: ${(metrics.luxuryRatio * 100).toInt()}%",
                50f,
                y,
                bodyPaint
            )
            y += 22f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 26f

            canvas.drawText("ثانياً: موقف الميزانيات وسرعة الحرق", 40f, y, headerPaint)
            y += 20f
            burnRates.take(7).forEach { br ->
                val pct = (br.spentRatio * 100).toInt()
                canvas.drawText(
                    "- ${br.category}: المنصرف ${br.spent.toInt()} ج.م من ${br.limit.toInt()} ج.م ($pct%)",
                    50f,
                    y,
                    bodyPaint
                )
                y += 18f
            }

            y += 12f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 26f

            canvas.drawText("ثالثاً: أحدث المعاملات المسجلة", 40f, y, headerPaint)
            y += 20f
            transactions.take(10).forEach { tx ->
                if (y < 790f) {
                    val sign = if (tx.type == "income") "+" else "-"
                    canvas.drawText(
                        "• ${FinanceAnalyticsEngine.formatDateAr(tx.txDateEpochMs)} | ${tx.title} (${tx.category}): $sign${tx.amount.toInt()} ج.م",
                        50f,
                        y,
                        bodyPaint
                    )
                    y += 17f
                }
            }

            pdfDocument.finishPage(page)
            val outFile = File(context.cacheDir, "mizan_official_statement.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Share human-readable executive summary + file location
            val summaryText = buildString {
                appendLine("📄 كشف حساب رسمي - تطبيق ميزان المالي الذكي")
                appendLine("التاريخ: ${FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())}")
                appendLine("──────────────────────")
                appendLine("• إجمالي الدخل: ${metrics.currentMonthIncome.toInt()} ج.م")
                appendLine("• إجمالي المنصرف: ${metrics.currentMonthExpense.toInt()} ج.م")
                appendLine("• صافي الرصيد: ${metrics.netBalance.toInt()} ج.م")
                appendLine("• المسموح صرفه يومياً: ${metrics.dailySafeToSpend.toInt()} ج.م")
                appendLine("• الضروريات: ${(metrics.essentialRatio * 100).toInt()}% | الرفاهيات: ${(metrics.luxuryRatio * 100).toInt()}%")
                appendLine("──────────────────────")
                appendLine("أحدث المعاملات:")
                transactions.take(8).forEach { tx ->
                    val s = if (tx.type == "income") "+" else "-"
                    appendLine("- ${tx.title} (${tx.category}): $s${tx.amount.toInt()} ج.م")
                }
                appendLine("──────────────────────")
                appendLine("تم توليد ملف PDF الرسمي بنجاح (${outFile.name}).")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "كشف حساب PDF - ميزان المالي الذكي")
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب الرسمي PDF"))
            outFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 7. التنبيهات الذكية المحلية على الهاتف (Smart Local Notifications)
     */
    fun sendSmartLocalNotification(
        context: Context,
        title: String,
        message: String
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "تنبيهات ميزان المالي الذكية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات الحد اليومي الآمن ومواعيد الأقساط وسرعة الحرق"
                }
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 8. تنسيق رقم هاتف الواتساب دولياً (يدعم الأرقام المصرية تلقائياً مثل 010... -> 2010...)
     */
    fun normalizeWhatsAppPhoneNumber(rawPhone: String): String {
        val digitsOnly = buildString {
            for (ch in rawPhone.trim()) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in '٠'..'٩' -> append((ch - '٠'))
                }
            }
        }
        if (digitsOnly.isEmpty()) return ""
        return when {
            digitsOnly.startsWith("00") -> digitsOnly.drop(2)
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "2$digitsOnly"
            digitsOnly.startsWith("1") && digitsOnly.length == 10 -> "20$digitsOnly"
            else -> digitsOnly
        }
    }

    /**
     * 9. بناء ملخص جميع معاملات اليوم بتفاصيلها الكاملة للإرسال عبر الواتساب عند الساعة 12 ليلاً
     */
    fun buildDailyTransactionsWhatsAppSummary(
        transactions: List<FinancialTransaction>,
        metrics: DashboardMetrics,
        currencySymbol: String = "ج.م",
        includeClosingDayIfMidnight: Boolean = true
    ): String {
        val nowCal = Calendar.getInstance()
        val todayStartCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = todayStartCal.timeInMillis
        val todayEndMs = todayStartMs + 24L * 60L * 60L * 1000L

        var targetStartMs = todayStartMs
        var targetEndMs = todayEndMs
        var targetDateLabel = FinanceAnalyticsEngine.formatDateAr(nowCal.timeInMillis)

        val todayTxs = transactions.filter { it.txDateEpochMs in todayStartMs until todayEndMs }

        // إذا انطلق التقرير عند الساعة 12:00 منتصف الليل (أول ساعة من اليوم الجديد) نرسل ملخص اليوم المنقضي بالكامل
        val dayTxs = if (todayTxs.isEmpty() && includeClosingDayIfMidnight && nowCal.get(Calendar.HOUR_OF_DAY) <= 1) {
            val yesterdayStartMs = todayStartMs - 24L * 60L * 60L * 1000L
            val yTxs = transactions.filter { it.txDateEpochMs in yesterdayStartMs until todayStartMs }
            if (yTxs.isNotEmpty()) {
                targetStartMs = yesterdayStartMs
                targetEndMs = todayStartMs
                targetDateLabel = FinanceAnalyticsEngine.formatDateAr(yesterdayStartMs)
                yTxs
            } else {
                todayTxs
            }
        } else {
            todayTxs
        }

        val sortedDayTxs = dayTxs.sortedBy { it.txDateEpochMs }
        val dayIncome = sortedDayTxs.filter { it.type == "income" }.sumOf { it.amount }
        val dayExpense = sortedDayTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val dayTransfers = sortedDayTxs.filter { it.type == "transfer" }.sumOf { it.amount }
        val timeFmt = SimpleDateFormat("hh:mm a", Locale("ar"))

        return buildString {
            appendLine("📊 *ملخص معاملات اليوم — ميزان المالي الذكي*")
            appendLine("📅 التاريخ: *$targetDateLabel*")
            appendLine("----------------")
            if (sortedDayTxs.isEmpty()) {
                appendLine("ℹ️ لم يتم تسجيل أي معاملات مالية خلال هذا اليوم.")
            } else {
                appendLine("🧾 *تفاصيل معاملات اليوم (${sortedDayTxs.size} معاملة):*")
                sortedDayTxs.forEachIndexed { idx, tx ->
                    val sign = when (tx.type) {
                        "income" -> "🟢 +"
                        "transfer" -> "🔄 "
                        else -> "🔴 -"
                    }
                    val walletAr = DefaultFinanceCatalog.walletLabelAr(tx.wallet)
                    val timeStr = timeFmt.format(Date(tx.txDateEpochMs))
                    appendLine(
                        "${idx + 1}. $sign*${tx.amount.toInt()} $currencySymbol* — ${tx.title} (${tx.category} • $walletAr • $timeStr)"
                    )
                }
            }
            appendLine("----------------")
            appendLine("📈 *إجمالي دخل اليوم:* +${dayIncome.toInt()} $currencySymbol")
            appendLine("📉 *إجمالي منصرف اليوم:* -${dayExpense.toInt()} $currencySymbol")
            if (dayTransfers > 0.0) {
                appendLine("🔄 *تحويلات بين المحافظ:* ${dayTransfers.toInt()} $currencySymbol")
            }
            val netDay = dayIncome - dayExpense
            val netDaySign = if (netDay >= 0) "+" else ""
            appendLine("⚖️ *صافي حركة اليوم:* $netDaySign${netDay.toInt()} $currencySymbol")
            appendLine("----------------")
            appendLine("💼 *أرصدة المحافظ الحالية:*")
            appendLine("• صافي الرصيد الكلي: *${metrics.netBalance.toInt()} $currencySymbol*")
            appendLine(
                "• كاش: ${(metrics.walletBalances["cash"] ?: 0.0).toInt()} $currencySymbol | بنك: ${(metrics.walletBalances["bank"] ?: 0.0).toInt()} $currencySymbol | محفظة: ${(metrics.walletBalances["ewallet"] ?: 0.0).toInt()} $currencySymbol"
            )
            appendLine("🎯 *الحد اليومي الآمن للصرف غداً:* *${metrics.dailySafeToSpend.toInt()} $currencySymbol*")
            appendLine("----------------")
            appendLine("✅ _تم التوليد تلقائياً بواسطة تطبيق ميزان المالي_")
        }
    }

    /**
     * 10. فتح محادثة واتساب مباشرة إلى الرقم المحدد مع ملء ملخص معاملات اليوم تلقائياً
     */
    fun openDirectWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        return try {
            val cleanPhone = normalizeWhatsAppPhoneNumber(phoneNumber)
            val encodedText = URLEncoder.encode(message, "UTF-8")
            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
            }
            val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(directIntent)
            true
        } catch (_: Exception) {
            try {
                val fallbackShare = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackShare, "إرسال ملخص اليوم عبر واتساب"))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 11. إرسال ملخص المعاملات تلقائياً في الخلفية عبر خدمة CallMeBot الرسمية المفعلة (الأساسية والوحيدة للواتساب)
     * Phone: 201021992233 | API Key: 3591043
     */
    suspend fun sendBackgroundWhatsAppReport(
        phoneNumber: String,
        callMeBotApiKey: String = "3591043",
        webhookUrl: String = "",
        metaAccessToken: String = "",
        metaPhoneNumberId: String = "",
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPhone = normalizeWhatsAppPhoneNumber(phoneNumber.ifBlank { "201021992233" })
        val cleanKey = callMeBotApiKey.trim().ifBlank { "3591043" }

        if (cleanPhone.isBlank() || cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("يرجى التأكد من إدخال رقم الواتساب (201021992233) وكود تفعيل CallMeBot (3591043).")
            )
        }

        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val encodedKey = URLEncoder.encode(cleanKey, "UTF-8")
            val apiUrl = URL(
                "https://api.callmebot.com/whatsapp.php?phone=$cleanPhone&text=$encodedMsg&apikey=$encodedKey"
            )
            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "*/*")
                connectTimeout = 15000
                readTimeout = 15000
            }
            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
            }

            if (code in 200..299) {
                val lowerBody = responseBody.lowercase(Locale.ROOT)
                if (lowerBody.contains("apikey is invalid") || lowerBody.contains("not activated")) {
                    Result.failure(
                        IllegalStateException("⚠️ كود التفعيل ($cleanKey) غير متطابق مع الرقم (+$cleanPhone) في CallMeBot: $responseBody")
                    )
                } else {
                    Result.success("✅ تم إرسال التقرير اليومي بنجاح عبر CallMeBot (كود $cleanKey) إلى واتساب +$cleanPhone!")
                }
            } else {
                Result.failure(
                    IllegalStateException("رد خادم CallMeBot (HTTP $code): ${responseBody.take(200)}")
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("تعذر الاتصال بخدمة CallMeBot: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}")
            )
        }
    }

    /**
     * 11-B. إرسال نفس التقرير اليومي عبر بوت تليجرام (Telegram Bot API) تلقائياً أو يدوياً
     */
    suspend fun sendTelegramBotReport(
        botToken: String,
        chatId: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()

        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("يرجى إدخال رمز بوت التليجرام (Bot Token) ومعرف المحادثة (Chat ID) أولاً.")
            )
        }

        try {
            val apiUrl = URL("https://api.telegram.org/bot$cleanToken/sendMessage")
            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 15000
                readTimeout = 15000
            }

            // تحويل تنسيق الخط العريض من واتساب (*نص*) إلى نص نظيف واضح في تليجرام
            val cleanTelegramText = message.replace("*", "")
            val payload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", cleanTelegramText)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
            }

            if (code in 200..299) {
                Result.success("✅ تم إرسال التقرير اليومي بنجاح عبر بوت تليجرام إلى المحادثة ($cleanChatId)!")
            } else {
                val errDesc = try {
                    JSONObject(responseBody).optString("description", responseBody)
                } catch (_: Exception) {
                    responseBody
                }
                Result.failure(IllegalStateException("⚠️ خطأ من تليجرام: $errDesc"))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("تعذر الاتصال بخوادم تليجرام: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}")
            )
        }
    }

    /**
     * 11-C. جلب الـ Chat ID تلقائياً من آخر رسالة أرسلها المستخدم للبوت عبر (getUpdates)
     */
    suspend fun fetchTelegramChatIdFromUpdates(
        botToken: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("يرجى لصق رمز البوت (Bot Token) أولاً ثم إرسال رسالة /start للبوت في تليجرام.")
            )
        }

        try {
            val apiUrl = URL("https://api.telegram.org/bot$cleanToken/getUpdates")
            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
            }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
            }

            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("تأكد من صحة Bot Token (HTTP $code)"))
            }

            val json = JSONObject(body)
            val resultArr = json.optJSONArray("result")
            if (resultArr == null || resultArr.length() == 0) {
                return@withContext Result.failure(
                    IllegalStateException("لم يتم العثور على رسائل بعد؛ افتح البوت في تليجرام واضغط Start أو أرسل له أي كلمة ثم جرب مرة أخرى.")
                )
            }

            val lastUpdate = resultArr.getJSONObject(resultArr.length() - 1)
            val msgObj = lastUpdate.optJSONObject("message")
                ?: lastUpdate.optJSONObject("edited_message")
                ?: lastUpdate.optJSONObject("channel_post")
            val chatObj = msgObj?.optJSONObject("chat")
                ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة بيانات المحادثة من البوت."))

            val chatId = chatObj.optLong("id", 0L).toString()
            val name = listOfNotNull(
                chatObj.optString("first_name").takeIf { it.isNotBlank() },
                chatObj.optString("username").takeIf { it.isNotBlank() }?.let { "@$it" }
            ).joinToString(" ").ifBlank { "محادثة تليجرام" }

            Result.success(chatId to name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 11-D. مشاركة التقرير مباشرة عبر تطبيق تليجرام المثبت على الهاتف بضغطة واحدة
     */
    fun openDirectTelegramShare(
        context: Context,
        message: String
    ): Boolean {
        return try {
            val cleanText = message.replace("*", "")
            val encodedText = URLEncoder.encode(cleanText, "UTF-8")
            val tgUri = Uri.parse("https://t.me/share/url?url=%20&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * إرسال قالب رسمي مجاني معتمد من Meta (hello_world) للتحقق الفوري من صحة الـ Token و Phone Number ID
     */
    suspend fun sendMetaWhatsAppTemplate(
        phoneNumber: String,
        metaAccessToken: String,
        metaPhoneNumberId: String,
        templateName: String = "hello_world",
        languageCode: String = "en_US",
        bodyParameters: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPhone = normalizeWhatsAppPhoneNumber(phoneNumber)
        val cleanMetaToken = metaAccessToken.trim()
        val cleanMetaPhoneId = metaPhoneNumberId.trim()

        if (cleanMetaToken.isBlank() || cleanMetaPhoneId.isBlank() || cleanPhone.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("يرجى التأكد من كتابة رقم الهاتف والـ Access Token و Phone Number ID")
            )
        }

        try {
            val graphUrl = URL("https://graph.facebook.com/v21.0/$cleanMetaPhoneId/messages")
            val conn = (graphUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $cleanMetaToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val templateObj = JSONObject().apply {
                put("name", templateName)
                put("language", JSONObject().apply {
                    put("code", languageCode)
                })
                if (bodyParameters.isNotEmpty()) {
                    val components = org.json.JSONArray().apply {
                        val bodyComp = JSONObject().apply {
                            put("type", "body")
                            val params = org.json.JSONArray().apply {
                                bodyParameters.forEach { p ->
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", p)
                                    })
                                }
                            }
                            put("parameters", params)
                        }
                        put(bodyComp)
                    }
                    put("components", components)
                }
            }

            val payload = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("recipient_type", "individual")
                put("to", cleanPhone)
                put("type", "template")
                put("template", templateObj)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            }

            if (responseCode in 200..299) {
                Result.success("✅ نجح إرسال قالب الاختبار الرسمي ($templateName) من Meta إلى +$cleanPhone! الاتصال والمفاتيح تعمل 100%.")
            } else {
                val errorMsg = try {
                    val root = JSONObject(responseBody).optJSONObject("error")
                    val msg = root?.optString("message", "") ?: responseBody
                    val code = root?.optInt("code", 0) ?: 0
                    "فشل إرسال القالب من Meta (كود $code): $msg"
                } catch (_: Exception) {
                    "HTTP $responseCode: $responseBody"
                }
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 12. إشعار الساعة 12:00 منتصف الليل التفاعلي الذي يفتح محادثة الواتساب على الرقم المرتبط بضغطة واحدة
     */
    fun sendMidnightWhatsAppNotification(
        context: Context,
        phoneNumber: String,
        summaryText: String,
        autoSentInBackground: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "تنبيهات ميزان المالي الذكية",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات ملخص الواتساب اليومي الساعة 12 ليلاً والحد اليومي"
                }
                manager.createNotificationChannel(channel)
            }

            val cleanPhone = normalizeWhatsAppPhoneNumber(phoneNumber)
            val encodedText = URLEncoder.encode(summaryText, "UTF-8")
            val whatsappUri = if (cleanPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
            }
            val openWhatsAppIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9012,
                openWhatsAppIntent,
                pendingIntentFlags
            )

            val title = if (autoSentInBackground) {
                "✅ تم إرسال ملخص معاملات اليوم على واتساب (+$cleanPhone)"
            } else {
                "🕛 ملخص معاملات اليوم جاهز للإرسال على واتساب (+$cleanPhone)"
            }
            val shortSubtitle = if (autoSentInBackground) {
                "تم الإرسال التلقائي الساعة 12 ليلاً بنجاح. اضغط لعرض التقرير في واتساب."
            } else {
                "اضغط هنا لفتح محادثة واتساب مع رقمك وإرسال تقرير معاملات اليوم فوراً."
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(shortSubtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_send,
                    "إرسال عبر واتساب الآن",
                    pendingIntent
                )
                .setAutoCancel(true)
                .build()

            manager.notify(9012, notification)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 13. بوابة الفتح السريع لتطبيقات الدفع والتحويل المجانية:
     * (InstaPay • فودافون كاش *9# • اتصالات كاش *777# • أورنج كاش #7115# • فوري • Telda)
     */
    fun openPaymentAppOrDialer(
        context: Context,
        paymentKey: String,
        amount: Double = 0.0,
        recipientPhoneOrIpa: String = ""
    ): Boolean {
        return try {
            // نسخ الرقم والمبلغ للحافظة ليسهل لصقها فوراً داخل تطبيق الدفع
            if (recipientPhoneOrIpa.isNotBlank() || amount > 0.0) {
                val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clipValue = recipientPhoneOrIpa.ifBlank { amount.toInt().toString() }
                clipBoard?.setPrimaryClip(android.content.ClipData.newPlainText("mizan_payment", clipValue))
            }

            when (paymentKey) {
                "instapay" -> {
                    val launch = context.packageManager.getLaunchIntentForPackage("com.egyptianbanks.instapay")
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launch)
                    } else {
                        val storeIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.egyptianbanks.instapay")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(storeIntent)
                    }
                    true
                }
                "vodafone_cash" -> {
                    val cleanDigits = recipientPhoneOrIpa.filter { it.isDigit() }
                    val ussd = if (cleanDigits.length == 11 && amount > 0.0) {
                        "tel:*9*7*$cleanDigits*${amount.toInt()}%23"
                    } else {
                        "tel:*9%23"
                    }
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(ussd)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                    true
                }
                "etisalat_cash" -> {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*777%23")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                    true
                }
                "orange_cash" -> {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:%237115%23")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                    true
                }
                "fawry" -> {
                    val launch = context.packageManager.getLaunchIntentForPackage("com.fawry.myfawry")
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launch)
                    } else {
                        val storeIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.fawry.myfawry")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(storeIntent)
                    }
                    true
                }
                "telda" -> {
                    val launch = context.packageManager.getLaunchIntentForPackage("com.telda.app")
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launch)
                    } else {
                        val storeIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.telda.app")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(storeIntent)
                    }
                    true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 14. إرسال رسالة تذكير ودي أو إشعار سداد لدين شخصي («ليا بره» أو «عليّا لناس») عبر واتساب
     */
    fun sendPoliteDebtWhatsAppReminder(
        context: Context,
        commitment: LocalCommitmentEntity,
        recipientPhone: String = "",
        paymentAccountHint: String = "",
        currencySymbol: String = "ج.م"
    ): Boolean {
        val remainingInstallments = (commitment.totalInstallmentsCount - commitment.paidInstallmentsCount).coerceAtLeast(1)
        val remainingTotal = (remainingInstallments * commitment.monthlyInstallment).toInt()
        val currentInstallment = commitment.monthlyInstallment.toInt()
        val title = commitment.decryptedTitle

        val message = if (commitment.type == "debt_receivable") {
            buildString {
                appendLine("أهلاً بك يا غالي 🌹")
                appendLine("أتمنى تكون بخير وصحة دائماً.")
                appendLine("رسالة تذكير ودية بخصوص: *$title*")
                appendLine("• قيمة الدفعة الحالية: *$currentInstallment $currencySymbol*")
                appendLine("• إجمالي المبلغ المتبقي: *$remainingTotal $currencySymbol*")
                appendLine("• موعد الاستحقاق الشهري: يوم *${commitment.dueDayOfMonth}* من الشهر")
                if (paymentAccountHint.isNotBlank()) {
                    appendLine("• التحويل متاح عبر (InstaPay / المحفظة): *$paymentAccountHint*")
                }
                appendLine("شكراً جزيلاً لك ودمت بكل خير 🙏")
            }
        } else {
            buildString {
                appendLine("أهلاً بك يا غالي 🌹")
                appendLine("بخصوص الالتزام/الدين المسجل: *$title*")
                appendLine("• قيمة الدفعة المقررة: *$currentInstallment $currencySymbol*")
                appendLine("• إجمالي المتبقي بعد هذه الدفعة: *${(remainingTotal - currentInstallment).coerceAtLeast(0)} $currencySymbol*")
                appendLine("• موعد السداد: يوم *${commitment.dueDayOfMonth}* من الشهر")
                appendLine("تحياتي وتقديري لك 🙏")
            }
        }

        return openDirectWhatsAppMessage(
            context = context,
            phoneNumber = recipientPhone,
            message = message
        )
    }

    /**
     * 15. الربط المجاني مع مهام جوجل وملاحظات جوجل (Google Tasks / Google Keep)
     * لتصدير قائمة مهام الفواتير والأقساط والديون المستحقة هذا الشهر بضغطة واحدة.
     */
    fun shareMonthlyChecklistToTasksOrKeep(
        context: Context,
        commitments: List<LocalCommitmentEntity>,
        burnRates: List<BurnRateStatus>,
        metrics: DashboardMetrics,
        currencySymbol: String = "ج.م"
    ): Boolean {
        return try {
            val activeCommitments = commitments.filter { it.paidInstallmentsCount < it.totalInstallmentsCount }
            val checklistText = buildString {
                appendLine("✅ قائمة مهام والتزامات الشهر — ميزان المالي الذكي")
                appendLine("📅 التاريخ: ${FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())}")
                appendLine("──────────────────────")
                appendLine("🎯 الحد اليومي الآمن للصرف: ${metrics.dailySafeToSpend.toInt()} $currencySymbol / يوم")
                appendLine("💼 صافي السيولة المتاحة: ${metrics.netBalance.toInt()} $currencySymbol")
                appendLine("──────────────────────")
                if (activeCommitments.isEmpty()) {
                    appendLine("☑️ لا توجد أقساط أو ديون متأخرة حالياً.")
                } else {
                    appendLine("📌 الأقساط والجمعيات والديون المطلوب متابعتها:")
                    activeCommitments.forEach { cmt ->
                        val kind = when (cmt.type) {
                            "debt_receivable" -> "🟢 تحصيل دين لي"
                            "debt_owed" -> "🔴 سداد دين عليّ"
                            "rosca" -> "🏦 قسط جمعية"
                            else -> "📅 قسط التزام"
                        }
                        appendLine("☐ [$kind] ${cmt.decryptedTitle}: ${cmt.monthlyInstallment.toInt()} $currencySymbol (يوم ${cmt.dueDayOfMonth})")
                    }
                }
                val warnings = burnRates.filter { it.isBurningFast || it.statusLevel != "safe" }
                if (warnings.isNotEmpty()) {
                    appendLine("──────────────────────")
                    appendLine("⚠️ فئات تحتاج ترشيد هذا الشهر:")
                    warnings.forEach { b ->
                        appendLine("☐ ضبط صرف «${b.category}» (${b.spent.toInt()} من ${b.limit.toInt()} $currencySymbol)")
                    }
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "قائمة التزامات ومهام الشهر المالية")
                putExtra(Intent.EXTRA_TEXT, checklistText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "إرسال القائمة إلى Google Tasks / Keep / الملاحظات"))
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 16. الربط السحابي المجاني 100% مع جداول بيانات جوجل (Google Sheets Live Sync)
     * يرسل جميع المعاملات والميزانيات والالتزامات والملخص المالي إلى جدول Google Sheet الخاص بالمستخدم.
     */
    suspend fun syncAllDataToGoogleSheetsWebApp(
        webAppUrl: String,
        transactions: List<FinancialTransaction>,
        budgets: List<CategoryBudget>,
        commitments: List<LocalCommitmentEntity>,
        savingsGoals: List<LocalSavingsGoalEntity>,
        metrics: DashboardMetrics
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanUrl = webAppUrl.trim()
        if (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://")) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رابط Google Apps Script Web App صحيح يبدأ بـ https://"))
        }

        try {
            val payload = JSONObject().apply {
                put("app", "MizanSmartFinance")
                put("syncedAt", FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis()))
                put(
                    "summary",
                    JSONObject().apply {
                        put("netBalance", metrics.netBalance)
                        put("currentMonthIncome", metrics.currentMonthIncome)
                        put("currentMonthExpense", metrics.currentMonthExpense)
                        put("dailySafeToSpend", metrics.dailySafeToSpend)
                        put("remainingBudget", metrics.remainingBudget)
                    }
                )
                val txArray = org.json.JSONArray()
                transactions.take(300).forEach { tx ->
                    txArray.put(
                        JSONObject().apply {
                            put("id", tx.id)
                            put("date", FinanceAnalyticsEngine.formatDateAr(tx.txDateEpochMs))
                            put("title", tx.title)
                            put("amount", tx.amount)
                            put("type", tx.type)
                            put("category", tx.category)
                            put("priority", tx.priority)
                            put("wallet", DefaultFinanceCatalog.walletLabelAr(tx.wallet))
                        }
                    )
                }
                put("transactions", txArray)

                val budgetArray = org.json.JSONArray()
                budgets.forEach { b ->
                    budgetArray.put(
                        JSONObject().apply {
                            put("category", b.category)
                            put("monthlyLimit", b.monthlyLimit)
                            put("spent", metrics.categoryTotals[b.category] ?: 0.0)
                        }
                    )
                }
                put("budgets", budgetArray)

                val cmtArray = org.json.JSONArray()
                commitments.forEach { c ->
                    cmtArray.put(
                        JSONObject().apply {
                            put("title", c.decryptedTitle)
                            put("type", c.type)
                            put("monthlyInstallment", c.monthlyInstallment)
                            put("paidCount", c.paidInstallmentsCount)
                            put("totalCount", c.totalInstallmentsCount)
                            put("dueDay", c.dueDayOfMonth)
                        }
                    )
                }
                put("commitments", cmtArray)

                val goalsArray = org.json.JSONArray()
                savingsGoals.forEach { g ->
                    goalsArray.put(
                        JSONObject().apply {
                            put("title", g.decryptedTitle)
                            put("saved", g.currentSavedAmount)
                            put("target", g.targetAmount)
                        }
                    )
                }
                put("savingsGoals", goalsArray)
            }

            val conn = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 12000
                readTimeout = 12000
            }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code in 200..399) {
                Result.success("☁️ تمت مزامنة ${transactions.size} معاملة وجميع الميزانيات والالتزامات مع Google Sheets بنجاح!")
            } else {
                Result.failure(IllegalStateException("استجابة الخادم HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * كود Google Apps Script الجاهز للنسخ بضغطة واحدة لربط أي ملف Google Sheet مجاناً.
     */
    fun getReadyGoogleAppsScriptCode(): String = """
function doPost(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var data = JSON.parse(e.postData.contents);
  
  // 1. ورقة المعاملات
  var txSheet = ss.getSheetByName("المعاملات") || ss.insertSheet("المعاملات");
  txSheet.clearContents();
  txSheet.appendRow(["التاريخ", "الوصف", "المبلغ", "النوع", "الفئة", "الأولوية", "المحفظة"]);
  (data.transactions || []).forEach(function(tx) {
    txSheet.appendRow([tx.date, tx.title, tx.amount, tx.type, tx.category, tx.priority, tx.wallet]);
  });

  // 2. ورقة الميزانيات
  var bSheet = ss.getSheetByName("الميزانيات") || ss.insertSheet("الميزانيات");
  bSheet.clearContents();
  bSheet.appendRow(["الفئة", "المنصرف الفعلي", "سقف الميزانية"]);
  (data.budgets || []).forEach(function(b) {
    bSheet.appendRow([b.category, b.spent, b.monthlyLimit]);
  });

  // 3. ورقة الديون والجمعيات
  var cSheet = ss.getSheetByName("الديون_والجمعيات") || ss.insertSheet("الديون_والجمعيات");
  cSheet.clearContents();
  cSheet.appendRow(["العنوان", "النوع", "القسط الشهري", "المدفوع", "الإجمالي", "يوم الاستحقاق"]);
  (data.commitments || []).forEach(function(c) {
    cSheet.appendRow([c.title, c.type, c.monthlyInstallment, c.paidCount, c.totalCount, c.dueDay]);
  });

  return ContentService.createTextOutput(JSON.stringify({status: "ok"}))
    .setMimeType(ContentService.MimeType.JSON);
}
""".trimIndent()
}

