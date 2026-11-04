package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class MonthlyBarPoint(
    val monthLabelAr: String,
    val yearMonthKey: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val essentialExpense: Double,
    val luxuryExpense: Double
)

data class DashboardMetrics(
    val currentMonthIncome: Double,
    val currentMonthExpense: Double,
    val netBalance: Double,
    val totalMonthlyBudget: Double,
    val remainingBudget: Double,
    val remainingDaysInMonth: Int,
    val elapsedDaysInMonth: Int,
    val totalDaysInMonth: Int,
    val dailySafeToSpend: Double,
    val essentialSpent: Double,
    val luxurySpent: Double,
    val essentialRatio: Float,
    val luxuryRatio: Float,
    val isLuxuryWarning: Boolean, // > 30%
    val categoryTotals: Map<String, Double>,
    val walletBalances: Map<String, Double>,
    val lastSixMonthsBars: List<MonthlyBarPoint>,
    val dailyLimitSourceLabelAr: String = "تلقائي ذكي"
)

object FinanceAnalyticsEngine {

    private val arabicMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun yearMonthKey(epochMs: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun formatDateAr(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun computeDashboardMetrics(
        transactions: List<FinancialTransaction>,
        budgets: List<CategoryBudget>,
        nowEpochMs: Long = System.currentTimeMillis(),
        appSettings: AppSettings = AppSettings()
    ): DashboardMetrics {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowEpochMs
        val currentKey = yearMonthKey(nowEpochMs)
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val elapsedDaysInMonth = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        val remainingDaysInMonth = when {
            appSettings.customRemainingDaysOverride > 0 -> appSettings.customRemainingDaysOverride.coerceIn(1, 60)
            appSettings.salaryDayOfMonth > 1 -> {
                val todayDay = cal.get(Calendar.DAY_OF_MONTH)
                val salDay = appSettings.salaryDayOfMonth.coerceIn(1, 28)
                if (todayDay < salDay) {
                    (salDay - todayDay).coerceAtLeast(1)
                } else {
                    (totalDaysInMonth - todayDay + salDay).coerceAtLeast(1)
                }
            }
            else -> max(1, totalDaysInMonth - elapsedDaysInMonth + 1)
        }

        val currentMonthTx = transactions.filter { yearMonthKey(it.txDateEpochMs) == currentKey }
        val currentMonthIncome = currentMonthTx.filter { it.type == "income" }.sumOf { it.amount }
        val currentMonthExpenses = currentMonthTx.filter { it.type == "expense" }
        val currentMonthExpense = currentMonthExpenses.sumOf { it.amount }

        val essentialSpent = currentMonthExpenses.filter { it.priority == "essential" }.sumOf { it.amount }
        val luxurySpent = currentMonthExpenses.filter { it.priority == "luxury" }.sumOf { it.amount }

        val essentialRatio = if (currentMonthExpense > 0) (essentialSpent / currentMonthExpense).toFloat() else 0.7f
        val luxuryRatio = if (currentMonthExpense > 0) (luxurySpent / currentMonthExpense).toFloat() else 0.3f
        val luxuryThresholdRatio = (appSettings.luxurySafeThresholdPercent.coerceIn(15, 50) / 100f)
        val isLuxuryWarning = currentMonthExpense > 0 && luxuryRatio > luxuryThresholdRatio

        val walletBalances = listOf("cash", "bank", "ewallet").associateWith { walletKey ->
            val inc = transactions.filter { it.wallet == walletKey && it.type == "income" }.sumOf { it.amount }
            val exp = transactions.filter { it.wallet == walletKey && it.type == "expense" }.sumOf { it.amount }
            val transferOut = transactions.filter { it.wallet == walletKey && it.type == "transfer" }.sumOf { it.amount }
            val transferIn = transactions.filter { it.recurrence == walletKey && it.type == "transfer" }.sumOf { it.amount }
            inc - exp - transferOut + transferIn
        }
        val totalWalletNetBalance = walletBalances.values.sum()

        val todayStr = formatDateAr(nowEpochMs)
        val todaySpent = currentMonthExpenses
            .filter { formatDateAr(it.txDateEpochMs) == todayStr }
            .sumOf { it.amount }

        val budgetsSum = budgets.sumOf { it.monthlyLimit }

        val (totalMonthlyBudget, remainingBudget, dailySafeToSpend, sourceLabel) = when (appSettings.dailySpendMode) {
            1 -> {
                // Mode 1: Based on actual net balance in wallets (صافي السيولة الفعلية ÷ الأيام المتبقية)
                val avail = max(0.0, totalWalletNetBalance)
                val totalRef = max(avail, currentMonthIncome)
                val daily = avail / remainingDaysInMonth.toDouble()
                Quad(totalRef, avail, daily, "حسب صافي الرصيد الفعلي")
            }
            2 -> {
                // Mode 2: Manual fixed daily limit set by user (حد يومي يدوي ثابت)
                val customDaily = appSettings.customDailySafeLimit.coerceAtLeast(0.0)
                val remToday = max(0.0, customDaily - todaySpent)
                val totalRef = customDaily * totalDaysInMonth
                val remMonth = customDaily * remainingDaysInMonth
                Quad(totalRef, remMonth, remToday, "حد يومي يدوي (${customDaily.toInt()} ج.م)")
            }
            3 -> {
                // Mode 3: Always based on Category Budgets sum (حسب سقف تبويب الميزانيات دائماً)
                val totalRef = budgetsSum
                val rem = max(0.0, totalRef - currentMonthExpense)
                val daily = rem / remainingDaysInMonth.toDouble()
                Quad(totalRef, rem, daily, "حسب سقف الميزانيات (${totalRef.toInt()} ج.م)")
            }
            else -> {
                // Mode 0 (Default Smart Mode):
                // If user has 0 transactions and customDailySafeLimit is 0, don't show leftover 18,700 seed budget!
                if (transactions.isEmpty() && appSettings.customDailySafeLimit <= 0.0) {
                    Quad(0.0, 0.0, 0.0, "مصفّر (أضف دخلاً أو حدد ميزانيتك)")
                } else if (appSettings.customDailySafeLimit > 0.0) {
                    val customDaily = appSettings.customDailySafeLimit
                    val remToday = max(0.0, customDaily - todaySpent)
                    Quad(customDaily * totalDaysInMonth, customDaily * remainingDaysInMonth, remToday, "حد يومي مخصص (${customDaily.toInt()} ج.م)")
                } else {
                    val totalRef = if (budgetsSum > 0.0) budgetsSum else currentMonthIncome * 0.80
                    val rem = max(0.0, totalRef - currentMonthExpense)
                    val daily = rem / remainingDaysInMonth.toDouble()
                    Quad(totalRef, rem, daily, "تلقائي ذكي من الميزانية والدخل")
                }
            }
        }

        val categoryTotals = currentMonthExpenses
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        // Last 6 months bar points
        val sixMonths = (5 downTo 0).map { offset ->
            val c = Calendar.getInstance()
            c.timeInMillis = nowEpochMs
            c.add(Calendar.MONTH, -offset)
            val key = yearMonthKey(c.timeInMillis)
            val label = arabicMonths[c.get(Calendar.MONTH)]
            val monthTxs = transactions.filter { yearMonthKey(it.txDateEpochMs) == key }
            val inc = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
            val expList = monthTxs.filter { it.type == "expense" }
            val exp = expList.sumOf { it.amount }
            val ess = expList.filter { it.priority == "essential" }.sumOf { it.amount }
            val lux = expList.filter { it.priority == "luxury" }.sumOf { it.amount }
            MonthlyBarPoint(
                monthLabelAr = label,
                yearMonthKey = key,
                totalIncome = inc,
                totalExpense = exp,
                essentialExpense = ess,
                luxuryExpense = lux
            )
        }

        return DashboardMetrics(
            currentMonthIncome = currentMonthIncome,
            currentMonthExpense = currentMonthExpense,
            netBalance = currentMonthIncome - currentMonthExpense,
            totalMonthlyBudget = totalMonthlyBudget,
            remainingBudget = remainingBudget,
            remainingDaysInMonth = remainingDaysInMonth,
            elapsedDaysInMonth = elapsedDaysInMonth,
            totalDaysInMonth = totalDaysInMonth,
            dailySafeToSpend = dailySafeToSpend,
            essentialSpent = essentialSpent,
            luxurySpent = luxurySpent,
            essentialRatio = essentialRatio,
            luxuryRatio = luxuryRatio,
            isLuxuryWarning = isLuxuryWarning,
            categoryTotals = categoryTotals,
            walletBalances = walletBalances,
            lastSixMonthsBars = sixMonths,
            dailyLimitSourceLabelAr = sourceLabel
        )
    }

    private data class Quad(
        val totalBudget: Double,
        val remainingBudget: Double,
        val dailySafe: Double,
        val label: String
    )

    fun computeBurnRates(
        transactions: List<FinancialTransaction>,
        budgets: List<CategoryBudget>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<BurnRateStatus> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowEpochMs
        val currentKey = yearMonthKey(nowEpochMs)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val elapsedDays = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val monthElapsedRatio = elapsedDays.toDouble() / totalDays.toDouble()

        val currentExpenses = transactions.filter {
            it.type == "expense" && yearMonthKey(it.txDateEpochMs) == currentKey
        }

        return budgets.map { budget ->
            val spent = currentExpenses.filter { it.category == budget.category }.sumOf { it.amount }
            val limit = max(1.0, budget.monthlyLimit)
            val spentRatio = spent / limit
            val dailyVelocity = spent / elapsedDays.toDouble()
            val remainingAmount = max(0.0, limit - spent)
            val daysUntilDepleted = if (dailyVelocity > 0.0) {
                (remainingAmount / dailyVelocity).roundToInt()
            } else {
                totalDays - elapsedDays
            }

            // Burn rate alert if spentRatio exceeds monthElapsedRatio by >= 15% and spentRatio >= 35%
            val isBurningFast = spentRatio > (monthElapsedRatio + 0.15) && spentRatio >= 0.35
            val statusLevel = when {
                spentRatio >= 1.0 -> "exceeded"
                spentRatio >= 0.80 -> "warning"
                else -> "safe"
            }

            BurnRateStatus(
                category = budget.category,
                spent = spent,
                limit = budget.monthlyLimit,
                spentRatio = spentRatio,
                monthElapsedRatio = monthElapsedRatio,
                isBurningFast = isBurningFast,
                estimatedDaysUntilDepleted = daysUntilDepleted,
                statusLevel = statusLevel
            )
        }.sortedByDescending { it.spentRatio }
    }

    fun computeForecastTable(
        transactions: List<FinancialTransaction>,
        budgets: List<CategoryBudget>,
        mode: ForecastMode,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<CategoryForecastRow> {
        fun keyForMonthsAgo(monthsAgo: Int): String {
            val c = Calendar.getInstance()
            c.timeInMillis = nowEpochMs
            c.add(Calendar.MONTH, -monthsAgo)
            return yearMonthKey(c.timeInMillis)
        }

        val keyM3 = keyForMonthsAgo(3)
        val keyM2 = keyForMonthsAgo(2)
        val keyM1 = keyForMonthsAgo(1)
        val keyM0 = keyForMonthsAgo(0)

        val expenses = transactions.filter { it.type == "expense" }
        val allCategories = (DefaultFinanceCatalog.expenseCategories + budgets.map { it.category } + expenses.map { it.category }).distinct()

        return allCategories.map { cat ->
            val m3 = expenses.filter { it.category == cat && yearMonthKey(it.txDateEpochMs) == keyM3 }.sumOf { it.amount }
            val m2 = expenses.filter { it.category == cat && yearMonthKey(it.txDateEpochMs) == keyM2 }.sumOf { it.amount }
            val m1 = expenses.filter { it.category == cat && yearMonthKey(it.txDateEpochMs) == keyM1 }.sumOf { it.amount }
            val m0 = expenses.filter { it.category == cat && yearMonthKey(it.txDateEpochMs) == keyM0 }.sumOf { it.amount }

            // If historical months are empty for a newly added custom category, fallback to current month
            val effectiveM1 = if (m1 > 0.0) m1 else m0
            val effectiveM2 = if (m2 > 0.0) m2 else effectiveM1
            val effectiveM3 = if (m3 > 0.0) m3 else effectiveM2

            val forecast = when (mode) {
                ForecastMode.ONE_MONTH -> effectiveM1
                ForecastMode.TWO_MONTHS_AVG -> (effectiveM1 + effectiveM2) / 2.0
                ForecastMode.THREE_MONTHS_AVG -> (effectiveM1 + effectiveM2 + effectiveM3) / 3.0
                ForecastMode.WEIGHTED_SMART -> (effectiveM1 * 0.50) + (effectiveM2 * 0.30) + (effectiveM3 * 0.20)
            }

            val budgetLimit = budgets.firstOrNull { it.category == cat }?.monthlyLimit ?: 0.0

            CategoryForecastRow(
                category = cat,
                monthMinus3 = m3,
                monthMinus2 = m2,
                monthMinus1 = m1,
                currentMonthSoFar = m0,
                forecastedNextMonth = forecast,
                budgetLimit = budgetLimit
            )
        }
    }
}
