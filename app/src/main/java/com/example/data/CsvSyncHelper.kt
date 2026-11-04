package com.example.data

import android.content.Context
import android.content.Intent
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class FullSystemRestoreResult(
    val transactions: List<FinancialTransaction>,
    val budgets: List<CategoryBudget>,
    val templates: List<QuickTemplate>,
    val commitments: List<LocalCommitmentEntity>,
    val savingsGoals: List<LocalSavingsGoalEntity>,
    val assetHoldings: List<LocalAssetHoldingEntity>,
    val customExpenseCategories: List<String>,
    val customIncomeCategories: List<String>,
    val restoredSettings: AppSettings?
)

object CsvSyncHelper {

    private const val UTF8_BOM = "\uFEFF"

    fun buildTransactionsCsv(transactions: List<FinancialTransaction>): String {
        val sb = StringBuilder(UTF8_BOM)
        sb.appendLine("المعرف,العنوان,المبلغ,النوع,الفئة,الأولوية,التكرار,المحفظة,التاريخ,الطابع_الزمني")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (tx in transactions) {
            val dateStr = sdf.format(Date(tx.txDateEpochMs))
            val safeTitle = tx.title.replace(",", " - ").replace("\n", " ")
            val safeCat = tx.category.replace(",", " - ").replace("\n", " ")
            sb.appendLine(
                "${tx.id},$safeTitle,${tx.amount},${tx.type},$safeCat,${tx.priority},${tx.recurrence},${tx.wallet},$dateStr,${tx.txDateEpochMs}"
            )
        }
        return sb.toString()
    }

    fun buildEncryptedBackupPayload(
        transactions: List<FinancialTransaction>,
        passphrase: String = ""
    ): String {
        val rawCsv = buildTransactionsCsv(transactions)
        return CryptoSecurityManager.encryptText(rawCsv)
    }

    /**
     * يبني نسخة احتياطية شاملة (Full System Backup) لكل بيانات البرنامج وإعداداته
     * مشفرة بمعيار AES-256-GCM لاستعادتها في أي وقت أو نقلها لجهاز آخر.
     */
    fun buildFullSystemEncryptedBackup(
        transactions: List<FinancialTransaction>,
        budgets: List<CategoryBudget>,
        templates: List<QuickTemplate>,
        commitments: List<LocalCommitmentEntity>,
        savingsGoals: List<LocalSavingsGoalEntity>,
        assetHoldings: List<LocalAssetHoldingEntity>,
        expenseCategories: List<String>,
        incomeCategories: List<String>,
        appSettings: AppSettings,
        encryptPayload: Boolean = true
    ): String {
        val root = JSONObject()
        root.put("backupVersion", 2)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        // 1. Settings
        val settingsObj = JSONObject().apply {
            put("accountTitle", appSettings.accountTitle)
            put("currencySymbol", appSettings.currencySymbol)
            put("salaryDayOfMonth", appSettings.salaryDayOfMonth)
            put("luxurySafeThresholdPercent", appSettings.luxurySafeThresholdPercent)
            put("defaultSpiritualGivingMode", appSettings.defaultSpiritualGivingMode)
            put("useGrossIncomeForOrthodoxTithe", appSettings.useGrossIncomeForOrthodoxTithe)
            put("autoMaskPrivacyOnLaunch", appSettings.autoMaskPrivacyOnLaunch)
            put("enableSmartBudgetAlerts", appSettings.enableSmartBudgetAlerts)
        }
        root.put("settings", settingsObj)

        // 2. Custom Categories
        val customExpArr = JSONArray()
        expenseCategories.filter { it !in DefaultFinanceCatalog.expenseCategories }.forEach { customExpArr.put(it) }
        root.put("customExpenseCategories", customExpArr)

        val customIncArr = JSONArray()
        incomeCategories.filter { it !in DefaultFinanceCatalog.incomeCategories }.forEach { customIncArr.put(it) }
        root.put("customIncomeCategories", customIncArr)

        // 3. Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            txArr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("type", tx.type)
                    put("category", tx.category)
                    put("priority", tx.priority)
                    put("recurrence", tx.recurrence)
                    put("wallet", tx.wallet)
                    put("txDateEpochMs", tx.txDateEpochMs)
                }
            )
        }
        root.put("transactions", txArr)

        // 4. Budgets
        val budgetArr = JSONArray()
        budgets.forEach { b ->
            budgetArr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("monthlyLimit", b.monthlyLimit)
                }
            )
        }
        root.put("budgets", budgetArr)

        // 5. Quick Templates
        val tplArr = JSONArray()
        templates.forEach { tpl ->
            tplArr.put(
                JSONObject().apply {
                    put("id", tpl.id)
                    put("title", tpl.title)
                    put("amount", tpl.amount)
                    put("category", tpl.category)
                    put("priority", tpl.priority)
                    put("recurrence", tpl.recurrence)
                    put("wallet", tpl.wallet)
                    put("iconKey", tpl.iconKey)
                }
            )
        }
        root.put("templates", tplArr)

        // 6. Commitments (ROSCAs, Installments, Debts)
        val cmtArr = JSONArray()
        commitments.forEach { cmt ->
            cmtArr.put(
                JSONObject().apply {
                    put("id", cmt.id)
                    put("title", cmt.decryptedTitle)
                    put("type", cmt.type)
                    put("totalAmount", cmt.totalAmount)
                    put("monthlyInstallment", cmt.monthlyInstallment)
                    put("paidInstallmentsCount", cmt.paidInstallmentsCount)
                    put("totalInstallmentsCount", cmt.totalInstallmentsCount)
                    put("dueDayOfMonth", cmt.dueDayOfMonth)
                    put("payoutMonthIndex", cmt.payoutMonthIndex)
                    put("wallet", cmt.wallet)
                }
            )
        }
        root.put("commitments", cmtArr)

        // 7. Savings Goals
        val goalsArr = JSONArray()
        savingsGoals.forEach { g ->
            goalsArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.decryptedTitle)
                    put("targetAmount", g.targetAmount)
                    put("currentSavedAmount", g.currentSavedAmount)
                    put("categoryIcon", g.categoryIcon)
                }
            )
        }
        root.put("savingsGoals", goalsArr)

        // 8. Asset Holdings (Gold & Foreign Currencies)
        val assetsArr = JSONArray()
        assetHoldings.forEach { a ->
            assetsArr.put(
                JSONObject().apply {
                    put("assetCode", a.assetCode)
                    put("labelAr", a.labelAr)
                    put("unitAr", a.unitAr)
                    put("quantity", a.quantity)
                    put("unitRateInEgp", a.unitRateInEgp)
                }
            )
        }
        root.put("assetHoldings", assetsArr)

        val jsonText = root.toString()
        return if (encryptPayload) CryptoSecurityManager.encryptText(jsonText) else jsonText
    }

    /**
     * يستعيد نسخة احتياطية شاملة (سواء كانت JSON مشفر أو غير مشفر أو ملف CSV للمعاملات).
     */
    fun parseFullSystemRestorePayload(rawOrEncryptedInput: String): FullSystemRestoreResult? {
        val trimmed = rawOrEncryptedInput.trim()
        if (trimmed.isBlank()) return null

        val decrypted = CryptoSecurityManager.decryptText(trimmed).removePrefix(UTF8_BOM).trim()

        if (decrypted.startsWith("{")) {
            return try {
                val root = JSONObject(decrypted)

                val restoredSettings = root.optJSONObject("settings")?.let { s ->
                    AppSettings(
                        accountTitle = s.optString("accountTitle", "حسابي المالي الشخصي"),
                        currencySymbol = s.optString("currencySymbol", "ج.م"),
                        salaryDayOfMonth = s.optInt("salaryDayOfMonth", 1).coerceIn(1, 28),
                        luxurySafeThresholdPercent = s.optInt("luxurySafeThresholdPercent", 30).coerceIn(15, 50),
                        defaultSpiritualGivingMode = s.optInt("defaultSpiritualGivingMode", 1).coerceIn(1, 2),
                        useGrossIncomeForOrthodoxTithe = s.optBoolean("useGrossIncomeForOrthodoxTithe", true),
                        autoMaskPrivacyOnLaunch = s.optBoolean("autoMaskPrivacyOnLaunch", false),
                        enableSmartBudgetAlerts = s.optBoolean("enableSmartBudgetAlerts", true)
                    )
                }

                val customExp = mutableListOf<String>()
                root.optJSONArray("customExpenseCategories")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val v = arr.optString(i).trim()
                        if (v.isNotBlank()) customExp.add(v)
                    }
                }

                val customInc = mutableListOf<String>()
                root.optJSONArray("customIncomeCategories")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val v = arr.optString(i).trim()
                        if (v.isNotBlank()) customInc.add(v)
                    }
                }

                val txs = mutableListOf<FinancialTransaction>()
                root.optJSONArray("transactions")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val amt = o.optDouble("amount", 0.0)
                        if (amt > 0.0) {
                            txs.add(
                                FinancialTransaction(
                                    id = o.optString("id", ""),
                                    title = o.optString("title", "معاملة مستعادة"),
                                    amount = amt,
                                    type = o.optString("type", "expense"),
                                    category = o.optString("category", "عام"),
                                    priority = o.optString("priority", "essential"),
                                    recurrence = o.optString("recurrence", "variable"),
                                    wallet = o.optString("wallet", "cash"),
                                    txDateEpochMs = o.optLong("txDateEpochMs", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }

                val budgets = mutableListOf<CategoryBudget>()
                root.optJSONArray("budgets")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val cat = o.optString("category", "").trim()
                        if (cat.isNotBlank()) {
                            budgets.add(
                                CategoryBudget(
                                    id = o.optString("id", ""),
                                    category = cat,
                                    monthlyLimit = o.optDouble("monthlyLimit", 1500.0)
                                )
                            )
                        }
                    }
                }

                val templates = mutableListOf<QuickTemplate>()
                root.optJSONArray("templates")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val title = o.optString("title", "").trim()
                        if (title.isNotBlank()) {
                            templates.add(
                                QuickTemplate(
                                    id = o.optString("id", ""),
                                    title = title,
                                    amount = o.optDouble("amount", 50.0),
                                    category = o.optString("category", "طعام ومشتريات"),
                                    priority = o.optString("priority", "essential"),
                                    recurrence = o.optString("recurrence", "variable"),
                                    wallet = o.optString("wallet", "cash"),
                                    iconKey = o.optString("iconKey", "coffee")
                                )
                            )
                        }
                    }
                }

                val commitments = mutableListOf<LocalCommitmentEntity>()
                root.optJSONArray("commitments")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val title = o.optString("title", "").trim()
                        if (title.isNotBlank()) {
                            commitments.add(
                                LocalCommitmentEntity(
                                    id = o.optString("id", "cmt_${System.currentTimeMillis()}_$i"),
                                    encryptedTitle = CryptoSecurityManager.encryptText(title),
                                    type = o.optString("type", "rosca"),
                                    totalAmount = o.optDouble("totalAmount", 10000.0),
                                    monthlyInstallment = o.optDouble("monthlyInstallment", 1000.0),
                                    paidInstallmentsCount = o.optInt("paidInstallmentsCount", 0),
                                    totalInstallmentsCount = o.optInt("totalInstallmentsCount", 10),
                                    dueDayOfMonth = o.optInt("dueDayOfMonth", 5),
                                    payoutMonthIndex = o.optInt("payoutMonthIndex", 1),
                                    wallet = o.optString("wallet", "bank")
                                )
                            )
                        }
                    }
                }

                val goals = mutableListOf<LocalSavingsGoalEntity>()
                root.optJSONArray("savingsGoals")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val title = o.optString("title", "").trim()
                        if (title.isNotBlank()) {
                            goals.add(
                                LocalSavingsGoalEntity(
                                    id = o.optString("id", "goal_${System.currentTimeMillis()}_$i"),
                                    encryptedTitle = CryptoSecurityManager.encryptText(title),
                                    targetAmount = o.optDouble("targetAmount", 10000.0),
                                    currentSavedAmount = o.optDouble("currentSavedAmount", 0.0),
                                    categoryIcon = o.optString("categoryIcon", "savings")
                                )
                            )
                        }
                    }
                }

                val assets = mutableListOf<LocalAssetHoldingEntity>()
                root.optJSONArray("assetHoldings")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val code = o.optString("assetCode", "").trim()
                        if (code.isNotBlank()) {
                            assets.add(
                                LocalAssetHoldingEntity(
                                    assetCode = code,
                                    labelAr = o.optString("labelAr", code),
                                    unitAr = o.optString("unitAr", ""),
                                    quantity = o.optDouble("quantity", 0.0),
                                    unitRateInEgp = o.optDouble("unitRateInEgp", 1.0)
                                )
                            )
                        }
                    }
                }

                FullSystemRestoreResult(
                    transactions = txs,
                    budgets = budgets,
                    templates = templates,
                    commitments = commitments,
                    savingsGoals = goals,
                    assetHoldings = assets,
                    customExpenseCategories = customExp,
                    customIncomeCategories = customInc,
                    restoredSettings = restoredSettings
                )
            } catch (_: Exception) {
                null
            }
        }

        // Fallback: parse as CSV transactions
        val csvTxs = parseImportedTransactionsCsv(decrypted)
        if (csvTxs.isEmpty()) return null
        return FullSystemRestoreResult(
            transactions = csvTxs,
            budgets = emptyList(),
            templates = emptyList(),
            commitments = emptyList(),
            savingsGoals = emptyList(),
            assetHoldings = emptyList(),
            customExpenseCategories = emptyList(),
            customIncomeCategories = emptyList(),
            restoredSettings = null
        )
    }

    fun buildExecutiveReportCsv(
        metrics: DashboardMetrics,
        burnRates: List<BurnRateStatus>,
        forecastRows: List<CategoryForecastRow>,
        forecastMode: ForecastMode
    ): String {
        val sb = StringBuilder(UTF8_BOM)
        sb.appendLine("=== التقرير التنفيذي المالي الشامل - ميزان المالي ===")
        sb.appendLine("البند,القيمة")
        sb.appendLine("إجمالي دخل الشهر الحالي,${metrics.currentMonthIncome.toInt()}")
        sb.appendLine("إجمالي مصروفات الشهر الحالي,${metrics.currentMonthExpense.toInt()}")
        sb.appendLine("صافي الرصيد الشهري,${metrics.netBalance.toInt()}")
        sb.appendLine("الميزانية الشهرية الكلية,${metrics.totalMonthlyBudget.toInt()}")
        sb.appendLine("المتبقي من الميزانية,${metrics.remainingBudget.toInt()}")
        sb.appendLine("الحد اليومي الآمن للصرف (Safe-to-Spend),${metrics.dailySafeToSpend.toInt()}")
        sb.appendLine("نسبة الضروريات,${(metrics.essentialRatio * 100).toInt()}%")
        sb.appendLine("نسبة الرفاهيات,${(metrics.luxuryRatio * 100).toInt()}%")
        sb.appendLine()
        sb.appendLine("=== تقرير الميزانيات ومؤشر سرعة الحرق (Burn Rate) ===")
        sb.appendLine("الفئة,المنصرف الفعلي,سقف الميزانية,نسبة الاستهلاك,الحالة,الأيام المتبقية قبل النفاد")
        for (b in burnRates) {
            val pct = (b.spentRatio * 100).toInt()
            val statusAr = when (b.statusLevel) {
                "exceeded" -> "تجاوز السقف"
                "warning" -> "قريب من النفاد"
                else -> if (b.isBurningFast) "سرعة حرق مرتفعة" else "آمن"
            }
            sb.appendLine("${b.category},${b.spent.toInt()},${b.limit.toInt()},$pct%,$statusAr,${b.estimatedDaysUntilDepleted}")
        }
        sb.appendLine()
        sb.appendLine("=== جدول التنبؤات المالية للشهر القادم (${forecastMode.labelAr}) ===")
        sb.appendLine("الفئة,قبل 3 شهور,قبل شهرين,الشهر الماضي,الشهر الحالي,توقع الشهر القادم,سقف الميزانية")
        for (row in forecastRows) {
            sb.appendLine(
                "${row.category},${row.monthMinus3.toInt()},${row.monthMinus2.toInt()},${row.monthMinus1.toInt()},${row.currentMonthSoFar.toInt()},${row.forecastedNextMonth.toInt()},${row.budgetLimit.toInt()}"
            )
        }
        return sb.toString()
    }

    fun parseImportedTransactionsCsv(
        csvOrEncryptedContent: String,
        passphrase: String = ""
    ): List<FinancialTransaction> {
        val trimmedInput = csvOrEncryptedContent.trim()
        val decryptedContent = CryptoSecurityManager.decryptText(trimmedInput)

        val cleaned = decryptedContent.removePrefix(UTF8_BOM).trim()
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return emptyList()

        val parsed = mutableListOf<FinancialTransaction>()
        for (i in 1 until lines.size) {
            val cols = lines[i].split(",")
            if (cols.size < 8) continue
            val id = cols[0].trim().takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) } ?: ""
            val title = cols[1].trim().ifEmpty { "معاملة مستوردة" }
            val amount = cols[2].trim().toDoubleOrNull() ?: continue
            if (amount <= 0.0) continue
            val type = when (cols[3].trim()) {
                "income" -> "income"
                "transfer" -> "transfer"
                else -> "expense"
            }
            val category = cols[4].trim().ifEmpty { "عام" }
            val priority = if (cols[5].trim() == "luxury") "luxury" else "essential"
            val recurrence = cols[6].trim().ifEmpty { "variable" }
            val wallet = when (cols[7].trim()) {
                "bank", "ewallet" -> cols[7].trim()
                else -> "cash"
            }
            val epochMs = if (cols.size >= 10) {
                cols[9].trim().toLongOrNull() ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
            parsed.add(
                FinancialTransaction(
                    id = id,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    priority = priority,
                    recurrence = recurrence,
                    wallet = wallet,
                    txDateEpochMs = epochMs
                )
            )
        }
        return parsed
    }

    fun shareCsvText(context: Context, fileName: String, csvContent: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            val chooser = Intent.createChooser(sendIntent, "مشاركة وتصدير البيانات ($fileName)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /**
     * يبني ملف Microsoft Excel حقيقي متعدد الأوراق (Multi-Sheet Excel XML Workbook .xls)
     * يضم 4 أوراق عمل منفصلة داخل ملف واحد مع دعم كامل للعربية واتجاه RTL:
     * 1. المعاملات
     * 2. الميزانيات وسرعة الحرق
     * 3. الديون والجمعيات والأهداف والذهب
     * 4. التنبؤات والملخص التنفيذي
     */
    fun buildMultiSheetExcelXmlWorkbook(
        transactions: List<FinancialTransaction>,
        burnRates: List<BurnRateStatus>,
        commitments: List<LocalCommitmentEntity>,
        savingsGoals: List<LocalSavingsGoalEntity>,
        assetHoldings: List<LocalAssetHoldingEntity>,
        forecastRows: List<CategoryForecastRow>,
        metrics: DashboardMetrics
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")

            // Sheet 1: المعاملات
            appendLine("""<Worksheet ss:Name="1_المعاملات"><Table>""")
            appendLine("""<Row><Cell><Data ss:Type="String">التاريخ</Data></Cell><Cell><Data ss:Type="String">الوصف</Data></Cell><Cell><Data ss:Type="String">المبلغ</Data></Cell><Cell><Data ss:Type="String">النوع</Data></Cell><Cell><Data ss:Type="String">الفئة</Data></Cell><Cell><Data ss:Type="String">الأولوية</Data></Cell><Cell><Data ss:Type="String">المحفظة</Data></Cell></Row>""")
            transactions.forEach { tx ->
                val dateStr = sdf.format(Date(tx.txDateEpochMs))
                val typeAr = when (tx.type) {
                    "income" -> "دخل"
                    "transfer" -> "تحويل"
                    else -> "مصروف"
                }
                appendLine(
                    """<Row><Cell><Data ss:Type="String">$dateStr</Data></Cell><Cell><Data ss:Type="String">${escapeXml(tx.title)}</Data></Cell><Cell><Data ss:Type="Number">${tx.amount}</Data></Cell><Cell><Data ss:Type="String">$typeAr</Data></Cell><Cell><Data ss:Type="String">${escapeXml(tx.category)}</Data></Cell><Cell><Data ss:Type="String">${DefaultFinanceCatalog.priorityLabelAr(tx.priority)}</Data></Cell><Cell><Data ss:Type="String">${DefaultFinanceCatalog.walletLabelAr(tx.wallet)}</Data></Cell></Row>"""
                )
            }
            appendLine("""</Table></Worksheet>""")

            // Sheet 2: الميزانيات وسرعة الحرق
            appendLine("""<Worksheet ss:Name="2_الميزانيات"><Table>""")
            appendLine("""<Row><Cell><Data ss:Type="String">الفئة</Data></Cell><Cell><Data ss:Type="String">المنصرف الفعلي</Data></Cell><Cell><Data ss:Type="String">سقف الميزانية</Data></Cell><Cell><Data ss:Type="String">نسبة الاستهلاك %</Data></Cell><Cell><Data ss:Type="String">الحالة</Data></Cell></Row>""")
            burnRates.forEach { b ->
                val pct = (b.spentRatio * 100).toInt()
                val statusAr = if (b.statusLevel == "exceeded") "تجاوز السقف" else if (b.isBurningFast) "سرعة حرق مرتفعة" else "آمن"
                appendLine(
                    """<Row><Cell><Data ss:Type="String">${escapeXml(b.category)}</Data></Cell><Cell><Data ss:Type="Number">${b.spent.toInt()}</Data></Cell><Cell><Data ss:Type="Number">${b.limit.toInt()}</Data></Cell><Cell><Data ss:Type="Number">$pct</Data></Cell><Cell><Data ss:Type="String">$statusAr</Data></Cell></Row>"""
                )
            }
            appendLine("""</Table></Worksheet>""")

            // Sheet 3: الديون والجمعيات والأهداف والذهب
            appendLine("""<Worksheet ss:Name="3_الديون_والأصول"><Table>""")
            appendLine("""<Row><Cell><Data ss:Type="String">البند / الاسم</Data></Cell><Cell><Data ss:Type="String">التصنيف</Data></Cell><Cell><Data ss:Type="String">القيمة الحالية / القسط</Data></Cell><Cell><Data ss:Type="String">الإجمالي المستهدف</Data></Cell></Row>""")
            commitments.forEach { c ->
                val kind = when (c.type) {
                    "debt_receivable" -> "دين مستحق لي (ليا بره)"
                    "debt_owed" -> "دين مستحق عليّ (عليّا)"
                    "rosca" -> "جمعية شهرية"
                    else -> "قسط شهري"
                }
                appendLine(
                    """<Row><Cell><Data ss:Type="String">${escapeXml(c.decryptedTitle)}</Data></Cell><Cell><Data ss:Type="String">$kind</Data></Cell><Cell><Data ss:Type="Number">${c.monthlyInstallment.toInt()}</Data></Cell><Cell><Data ss:Type="Number">${c.totalAmount.toInt()}</Data></Cell></Row>"""
                )
            }
            savingsGoals.forEach { g ->
                appendLine(
                    """<Row><Cell><Data ss:Type="String">${escapeXml(g.decryptedTitle)}</Data></Cell><Cell><Data ss:Type="String">هدف ادخار</Data></Cell><Cell><Data ss:Type="Number">${g.currentSavedAmount.toInt()}</Data></Cell><Cell><Data ss:Type="Number">${g.targetAmount.toInt()}</Data></Cell></Row>"""
                )
            }
            assetHoldings.forEach { a ->
                appendLine(
                    """<Row><Cell><Data ss:Type="String">${escapeXml(a.labelAr)}</Data></Cell><Cell><Data ss:Type="String">أصل / ذهب / عملة</Data></Cell><Cell><Data ss:Type="Number">${a.quantity}</Data></Cell><Cell><Data ss:Type="Number">${(a.quantity * a.unitRateInEgp).toInt()}</Data></Cell></Row>"""
                )
            }
            appendLine("""</Table></Worksheet>""")

            // Sheet 4: التنبؤات والملخص التنفيذي
            appendLine("""<Worksheet ss:Name="4_التنبؤات_والملخص"><Table>""")
            appendLine("""<Row><Cell><Data ss:Type="String">المؤشر المالي</Data></Cell><Cell><Data ss:Type="String">القيمة (ج.م)</Data></Cell></Row>""")
            appendLine("""<Row><Cell><Data ss:Type="String">صافي السيولة الكلية</Data></Cell><Cell><Data ss:Type="Number">${metrics.netBalance.toInt()}</Data></Cell></Row>""")
            appendLine("""<Row><Cell><Data ss:Type="String">إجمالي دخل الشهر</Data></Cell><Cell><Data ss:Type="Number">${metrics.currentMonthIncome.toInt()}</Data></Cell></Row>""")
            appendLine("""<Row><Cell><Data ss:Type="String">إجمالي منصرف الشهر</Data></Cell><Cell><Data ss:Type="Number">${metrics.currentMonthExpense.toInt()}</Data></Cell></Row>""")
            appendLine("""<Row><Cell><Data ss:Type="String">الحد اليومي الآمن للصرف</Data></Cell><Cell><Data ss:Type="Number">${metrics.dailySafeToSpend.toInt()}</Data></Cell></Row>""")
            forecastRows.forEach { f ->
                appendLine(
                    """<Row><Cell><Data ss:Type="String">توقع الشهر القادم - ${escapeXml(f.category)}</Data></Cell><Cell><Data ss:Type="Number">${f.forecastedNextMonth.toInt()}</Data></Cell></Row>"""
                )
            }
            appendLine("""</Table></Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

    fun exportAndShareMultiSheetExcelWorkbook(
        context: Context,
        transactions: List<FinancialTransaction>,
        burnRates: List<BurnRateStatus>,
        commitments: List<LocalCommitmentEntity>,
        savingsGoals: List<LocalSavingsGoalEntity>,
        assetHoldings: List<LocalAssetHoldingEntity>,
        forecastRows: List<CategoryForecastRow>,
        metrics: DashboardMetrics
    ) {
        val xmlWorkbook = buildMultiSheetExcelXmlWorkbook(
            transactions = transactions,
            burnRates = burnRates,
            commitments = commitments,
            savingsGoals = savingsGoals,
            assetHoldings = assetHoldings,
            forecastRows = forecastRows,
            metrics = metrics
        )
        try {
            val outFile = java.io.File(context.cacheDir, "mizan_full_workbook.xls")
            outFile.writeText(xmlWorkbook, Charsets.UTF_8)
        } catch (_: Exception) {
        }
        shareCsvText(context, "mizan_full_workbook.xls", xmlWorkbook)
    }

    /**
     * يصدر تقريراً تفاعلياً أنيقاً بصيغة HTML (Arabic RTL) يعمل على أي متصفح هاتف أو كمبيوتر.
     */
    fun exportAndShareInteractiveHtmlReport(
        context: Context,
        transactions: List<FinancialTransaction>,
        burnRates: List<BurnRateStatus>,
        metrics: DashboardMetrics
    ) {
        val html = buildString {
            appendLine("<!DOCTYPE html><html lang='ar' dir='rtl'><head><meta charset='UTF-8'>")
            appendLine("<title>تقرير ميزان المالي الذكي التفاعلي</title>")
            appendLine("<style>body{font-family:Tahoma,Arial,sans-serif;background:#0F172A;color:#F8FAFC;padding:24px} .card{background:#1E293B;border-radius:16px;padding:18px;margin-bottom:16px;border:1px solid #334155} h1,h2{color:#34D399;margin-top:0} table{width:100%;border-collapse:collapse;margin-top:10px} th,td{padding:10px;border-bottom:1px solid #334155;text-align:right} th{color:#FBBF24}</style>")
            appendLine("</head><body>")
            appendLine("<div class='card'><h1>🛡️ التقرير المالي التفاعلي — ميزان المالي الذكي</h1>")
            appendLine("<p>صافي السيولة: <b>${metrics.netBalance.toInt()} ج.م</b> | الحد اليومي الآمن: <b>${metrics.dailySafeToSpend.toInt()} ج.م/يوم</b> | دخل الشهر: <b>${metrics.currentMonthIncome.toInt()} ج.م</b> | منصرف الشهر: <b>${metrics.currentMonthExpense.toInt()} ج.م</b></p></div>")
            appendLine("<div class='card'><h2>📊 موقف الميزانيات</h2><table><tr><th>الفئة</th><th>المنصرف</th><th>السقف</th><th>النسبة</th></tr>")
            burnRates.forEach { b ->
                appendLine("<tr><td>${escapeXml(b.category)}</td><td>${b.spent.toInt()} ج.م</td><td>${b.limit.toInt()} ج.م</td><td>${(b.spentRatio * 100).toInt()}%</td></tr>")
            }
            appendLine("</table></div>")
            appendLine("<div class='card'><h2>🧾 أحدث المعاملات</h2><table><tr><th>الوصف</th><th>الفئة</th><th>المبلغ</th><th>المحفظة</th></tr>")
            transactions.take(30).forEach { tx ->
                val sign = if (tx.type == "income") "+" else "-"
                appendLine("<tr><td>${escapeXml(tx.title)}</td><td>${escapeXml(tx.category)}</td><td>$sign${tx.amount.toInt()} ج.م</td><td>${DefaultFinanceCatalog.walletLabelAr(tx.wallet)}</td></tr>")
            }
            appendLine("</table></div></body></html>")
        }
        shareCsvText(context, "mizan_interactive_report.html", html)
    }
}

