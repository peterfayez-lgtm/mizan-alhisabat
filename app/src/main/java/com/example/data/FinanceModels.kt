package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class FinancialTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "expense", // "income" | "expense"
    val category: String = "طعام ومشتريات",
    val priority: String = "essential", // "essential" | "luxury"
    val recurrence: String = "variable", // "fixed" | "variable"
    val wallet: String = "cash", // "cash" | "bank" | "ewallet"
    val txDateEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreatePayload(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().take(120).ifEmpty { "معاملة مالية" },
        "amount" to max(0.01, amount),
        "type" to if (type == "income") "income" else "expense",
        "category" to category.trim().take(60).ifEmpty { "عام" },
        "priority" to if (priority == "luxury") "luxury" else "essential",
        "recurrence" to if (recurrence == "fixed") "fixed" else "variable",
        "wallet" to when (wallet) {
            "bank", "ewallet" -> wallet
            else -> "cash"
        },
        "txDateEpochMs" to if (txDateEpochMs > 0L) txDateEpochMs else System.currentTimeMillis(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "",
    val monthlyLimit: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreatePayload(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "category" to category.trim().take(60),
        "monthlyLimit" to max(0.0, monthlyLimit),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class QuickTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val priority: String = "essential",
    val recurrence: String = "variable",
    val wallet: String = "cash",
    val iconKey: String = "coffee",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreatePayload(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().take(80),
        "amount" to max(0.01, amount),
        "category" to category.trim().take(60),
        "priority" to if (priority == "luxury") "luxury" else "essential",
        "recurrence" to if (recurrence == "fixed") "fixed" else "variable",
        "wallet" to when (wallet) {
            "bank", "ewallet" -> wallet
            else -> "cash"
        },
        "iconKey" to iconKey.trim().take(40).ifEmpty { "coffee" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class ForecastMode(val labelAr: String, val subtitleAr: String) {
    ONE_MONTH("شهر واحد", "على أساس منصرف آخر شهر كامل"),
    TWO_MONTHS_AVG("متوسط شهرين", "المتوسط الحسابي لآخر شهرين"),
    THREE_MONTHS_AVG("متوسط 3 شهور", "المتوسط الحسابي لآخر 3 شهور"),
    WEIGHTED_SMART("المتوسط المرجح الذكي", "وزن 50% للأخير، 30% للذي قبله، 20% للأقدم")
}

data class BurnRateStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val spentRatio: Double,
    val monthElapsedRatio: Double,
    val isBurningFast: Boolean,
    val estimatedDaysUntilDepleted: Int,
    val statusLevel: String // "safe" (<80%), "warning" (80..99%), "exceeded" (>=100%)
)

data class CategoryForecastRow(
    val category: String,
    val monthMinus3: Double,
    val monthMinus2: Double,
    val monthMinus1: Double,
    val currentMonthSoFar: Double,
    val forecastedNextMonth: Double,
    val budgetLimit: Double
)

enum class AppDesignPreset(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val layoutDescriptionAr: String
) {
    EMERALD_GOLD(
        id = "EMERALD_GOLD",
        titleAr = "١. الديزاين الملكي الزمردي والذهبي (Royal Emerald)",
        subtitleAr = "طابع البنوك الخاصة وإدارة الثروات الفاخرة",
        layoutDescriptionAr = "بطاقة هيرو متدرجة بالزمرد والذهب + 3 بطاقات مؤشرات بارزة"
    ),
    SAPPHIRE_FINTECH(
        id = "SAPPHIRE_FINTECH",
        titleAr = "٢. الديزاين البنكي السماوي الحديث (Neo-Bank Sapphire)",
        subtitleAr = "مستوحى من تطبيقات Revolut و Wise العصرية",
        layoutDescriptionAr = "تصميم البطاقة البنكية الذكية (Virtual Smart Card) مع شريحة رقمية وأزرار مدمجة"
    ),
    AMETHYST_GLASS(
        id = "AMETHYST_GLASS",
        titleAr = "٣. الديزاين الزجاجي البنفسجي والمرجاني (Cyber Amethyst)",
        subtitleAr = "مستوحى من لوحات التحليل الذكية والتقنية الحديثة",
        layoutDescriptionAr = "حلقة قياس دائرية مزدوجة (Dual Ring Gauge) للحد اليومي والصحة المالية"
    ),
    MINIMAL_EXECUTIVE(
        id = "MINIMAL_EXECUTIVE",
        titleAr = "٤. الديزاين التنفيذي الهندسي (Executive Slate & Copper)",
        subtitleAr = "مستوحى من شاشات Bloomberg التنفيذية عالية التباين",
        layoutDescriptionAr = "شبكة قيادة تنفيذية هندسية (6 مؤشرات رقمية مكثفة بحواف حادة)"
    ),
    WARM_OASIS(
        id = "WARM_OASIS",
        titleAr = "١. الديزاين الأبيض والبيج الهادئ (Warm White & Beige)",
        subtitleAr = "ألوان الأبيض العاجي والبيج الرملي الدافئ المريح للعين",
        layoutDescriptionAr = "بطاقة الرصيد الإجمالي الأنيقة + اختصارات ذكية + أحدث المعاملات"
    );

    companion object {
        fun fromId(id: String?): AppDesignPreset =
            entries.firstOrNull { it.id == id } ?: WARM_OASIS
    }
}

data class AppSettings(
    val accountTitle: String = "حسابي المالي الشخصي",
    val currencySymbol: String = "ج.م",
    val salaryDayOfMonth: Int = 1, // 1..28
    val luxurySafeThresholdPercent: Int = 30, // 15..50
    val defaultSpiritualGivingMode: Int = 1, // 1 = الزكاة والصدقات (2.5%), 2 = العشور الأرثوذكسية (10%)
    val useGrossIncomeForOrthodoxTithe: Boolean = true,
    val autoMaskPrivacyOnLaunch: Boolean = false,
    val enableSmartBudgetAlerts: Boolean = true,
    val designPresetId: String = AppDesignPreset.WARM_OASIS.id,
    val dailySpendMode: Int = 0, // 0 = تلقائي ذكي (0 عند التصفير)، 1 = صافي السيولة ÷ الأيام، 2 = حد يومي يدوي ثابت، 3 = إجمالي سقف الميزانيات ÷ الأيام
    val customDailySafeLimit: Double = 0.0,
    val customRemainingDaysOverride: Int = 0, // 0 = تلقائي، 1..31 = تحديد يدوي لعدد الأيام المتبقية
    val whatsappPhoneNumber: String = "201021992233", // رقم الواتساب المرتبط (مفعل افتراضياً)
    val whatsappMidnightAutoEnabled: Boolean = true, // تفعيل إرسال ملخص المعاملات يومياً تلقائياً
    val whatsappReportHour: Int = 0, // 0 = 12:00 منتصف الليل (قابل للتعديل اليدوي)
    val whatsappReportMinute: Int = 0,
    val whatsappCallMeBotApiKey: String = "3591043", // مفتاح CallMeBot الأساسي المفعل للرقم 201021992233
    val whatsappWebhookUrl: String = "",
    val whatsappMetaAccessToken: String = "",
    val whatsappMetaPhoneNumberId: String = "",
    val whatsappMetaWabaId: String = "",
    val telegramBotEnabled: Boolean = false, // تفعيل إرسال نفس التقرير اليومي عبر بوت تليجرام
    val telegramBotToken: String = "", // توكن بوت تليجرام من BotFather
    val telegramChatId: String = "", // معرف المحادثة (Chat ID) في تليجرام
    val autoCheckUpdatesOnLaunch: Boolean = true, // فحص التحديثات تلقائياً عند فتح البرنامج
    val updateManifestUrl: String = "https://api.github.com/repos/peterfayez-lgtm/mizan-alhisabat/releases/latest", // رابط GitHub Releases التلقائي لمستودعك
    val publishedLatestVersionCode: Int = 3, // كود أحدث إصدار منشور
    val publishedLatestVersionName: String = "2.1.0", // اسم أحدث إصدار منشور
    val publishedApkDownloadUrl: String = "https://github.com/peterfayez-lgtm/mizan-alhisabat/releases/latest/download/Mizan-AlHisabat-Latest.apk", // رابط تحميل ملف الـ APK المباشر من GitHub Releases
    val publishedReleaseNotesAr: String = "• تفعيل الإرسال التلقائي الأساسي عبر CallMeBot للواتساب وإضافة بوت تليجرام.\n• ربط التحديث السحابي التلقائي بمستودع peterfayez-lgtm/mizan-alhisabat على GitHub Releases.\n• حاسبة سريعة لإضافة المعاملات في 3 ثوانٍ مع الجمع والطرح الفوري.",
    val publishedIsMandatoryUpdate: Boolean = false, // هل التحديث إجباري أم اختياري
    val simpleMinimalistUiMode: Boolean = true, // الوضع البسيط الاحترافي (واجهة بنكية هادئة غير مزدحمة)
    val autoCaptureBankNotifications: Boolean = true, // قراءة وتحليل إشعارات البنوك وإنستاباي تلقائياً
    val googleSheetsWebAppUrl: String = "", // رابط Google Sheets Apps Script للمزامنة السحابية المجانية
    val autoSyncGoogleSheetsOnNewTx: Boolean = false // مزامنة تلقائية مع Google Sheets عند إضافة معاملة
)

data class AppUpdateManifest(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val releaseNotesAr: String,
    val isMandatoryUpdate: Boolean = false,
    val publishedDateAr: String = ""
)

object DefaultFinanceCatalog {
    val expenseCategories = listOf(
        "طعام ومشتريات",
        "سكن وفواتير",
        "مواصلات ووقود",
        "مطاعم وكافيهات",
        "تسوق وترفيه",
        "صحة وعلاج",
        "تعليم واشتراكات"
    )

    val incomeCategories = listOf(
        "راتب أساسي",
        "عمل حر ومشاريع",
        "عوائد استثمار",
        "مكافآت وحوافز"
    )

    fun walletLabelAr(wallet: String): String = when (wallet) {
        "bank" -> "حساب بنكي"
        "ewallet" -> "محفظة إلكترونية"
        else -> "كاش نقدي"
    }

    fun priorityLabelAr(priority: String): String =
        if (priority == "luxury") "رفاهية / كمالي" else "ضروري / أساسي"

    fun recurrenceLabelAr(recurrence: String): String =
        if (recurrence == "fixed") "ثابت شهري" else "متغير"

    fun buildSeedBudgets(): List<CategoryBudget> = listOf(
        CategoryBudget(id = "budget_food", category = "طعام ومشتريات", monthlyLimit = 4500.0),
        CategoryBudget(id = "budget_housing", category = "سكن وفواتير", monthlyLimit = 6000.0),
        CategoryBudget(id = "budget_transport", category = "مواصلات ووقود", monthlyLimit = 2000.0),
        CategoryBudget(id = "budget_Monitor", category = "مطاعم وكافيهات", monthlyLimit = 1800.0),
        CategoryBudget(id = "budget_shopping", category = "تسوق وترفيه", monthlyLimit = 2200.0),
        CategoryBudget(id = "budget_health", category = "صحة وعلاج", monthlyLimit = 1200.0),
        CategoryBudget(id = "budget_edu", category = "تعليم واشتراكات", monthlyLimit = 1000.0)
    )

    fun buildSeedTemplates(): List<QuickTemplate> = listOf(
        QuickTemplate(
            id = "tpl_coffee",
            title = "قهوة مختصة",
            amount = 65.0,
            category = "مطاعم وكافيهات",
            priority = "luxury",
            recurrence = "variable",
            wallet = "cash",
            iconKey = "coffee"
        ),
        QuickTemplate(
            id = "tpl_transport",
            title = "مشوار مواصلات / أوبر",
            amount = 85.0,
            category = "مواصلات ووقود",
            priority = "essential",
            recurrence = "variable",
            wallet = "ewallet",
            iconKey = "transport"
        ),
        QuickTemplate(
            id = "tpl_market",
            title = "طلبات سوبر ماركت",
            amount = 320.0,
            category = "طعام ومشتريات",
            priority = "essential",
            recurrence = "variable",
            wallet = "bank",
            iconKey = "cart"
        ),
        QuickTemplate(
            id = "tpl_internet",
            title = "فاتورة إنترنت منزلي",
            amount = 450.0,
            category = "سكن وفواتير",
            priority = "essential",
            recurrence = "fixed",
            wallet = "ewallet",
            iconKey = "wifi"
        ),
        QuickTemplate(
            id = "tpl_fuel",
            title = "تموين وقود السيارة",
            amount = 500.0,
            category = "مواصلات ووقود",
            priority = "essential",
            recurrence = "variable",
            wallet = "cash",
            iconKey = "fuel"
        ),
        QuickTemplate(
            id = "tpl_dinner",
            title = "عشاء خارجي",
            amount = 420.0,
            category = "مطاعم وكافيهات",
            priority = "luxury",
            recurrence = "variable",
            wallet = "bank",
            iconKey = "restaurant"
        )
    )

    fun buildSeedTransactions(nowEpochMs: Long = System.currentTimeMillis()): List<FinancialTransaction> {
        fun monthTimestamp(monthsAgo: Int, dayOfMonth: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = nowEpochMs
            cal.add(Calendar.MONTH, -monthsAgo)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val list = mutableListOf<FinancialTransaction>()
        // 3 past months + current month (monthsAgo = 3, 2, 1, 0)
        for (m in 3 downTo 0) {
            val inflationFactor = when (m) {
                3 -> 0.90
                2 -> 0.95
                1 -> 1.02
                else -> 1.0
            }
            // Monthly salary + freelance income
            list.add(
                FinancialTransaction(
                    id = "seed_inc_sal_$m",
                    title = "الراتب الشهري الأساسي",
                    amount = 22000.0,
                    type = "income",
                    category = "راتب أساسي",
                    priority = "essential",
                    recurrence = "fixed",
                    wallet = "bank",
                    txDateEpochMs = monthTimestamp(m, 1)
                )
            )
            list.add(
                FinancialTransaction(
                    id = "seed_inc_free_$m",
                    title = "مشروع تطوير أنظمة",
                    amount = (4500.0 * inflationFactor).roundToInt().toDouble(),
                    type = "income",
                    category = "عمل حر ومشاريع",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "ewallet",
                    txDateEpochMs = monthTimestamp(m, 10)
                )
            )
            // Housing & Bills
            list.add(
                FinancialTransaction(
                    id = "seed_exp_rent_$m",
                    title = "إيجار السكن وفواتير المرافق",
                    amount = (5200.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "سكن وفواتير",
                    priority = "essential",
                    recurrence = "fixed",
                    wallet = "bank",
                    txDateEpochMs = monthTimestamp(m, 2)
                )
            )
            // Groceries
            list.add(
                FinancialTransaction(
                    id = "seed_exp_groceries1_$m",
                    title = "خزين الشهر من السوبر ماركت",
                    amount = (2400.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "طعام ومشتريات",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "bank",
                    txDateEpochMs = monthTimestamp(m, 4)
                )
            )
            list.add(
                FinancialTransaction(
                    id = "seed_exp_groceries2_$m",
                    title = "خضروات وفاكهة ولحوم أسبوعية",
                    amount = (1450.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "طعام ومشتريات",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "cash",
                    txDateEpochMs = monthTimestamp(m, 12)
                )
            )
            // Transport
            list.add(
                FinancialTransaction(
                    id = "seed_exp_trans_$m",
                    title = "وقود السيارة ومواصلات",
                    amount = (1550.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "مواصلات ووقود",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "cash",
                    txDateEpochMs = monthTimestamp(m, 7)
                )
            )
            // Restaurants & Cafes (Luxury — intentionally higher in current month to trigger Burn Rate & 30% Luxury alerts!)
            val cafeAmount = if (m == 0) 1680.0 else (1350.0 * inflationFactor).roundToInt().toDouble()
            list.add(
                FinancialTransaction(
                    id = "seed_exp_cafe_$m",
                    title = "مطاعم وكافيهات وعزومات",
                    amount = cafeAmount,
                    type = "expense",
                    category = "مطاعم وكافيهات",
                    priority = "luxury",
                    recurrence = "variable",
                    wallet = "bank",
                    txDateEpochMs = monthTimestamp(m, 8)
                )
            )
            // Shopping & Entertainment (Luxury)
            val shopAmount = if (m == 0) 2150.0 else (1750.0 * inflationFactor).roundToInt().toDouble()
            list.add(
                FinancialTransaction(
                    id = "seed_exp_shop_$m",
                    title = "تسوق ملابس وترفيه عائلي",
                    amount = shopAmount,
                    type = "expense",
                    category = "تسوق وترفيه",
                    priority = "luxury",
                    recurrence = "variable",
                    wallet = "ewallet",
                    txDateEpochMs = monthTimestamp(m, 11)
                )
            )
            // Health
            list.add(
                FinancialTransaction(
                    id = "seed_exp_health_$m",
                    title = "صيدلية ومستلزمات طبية",
                    amount = (650.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "صحة وعلاج",
                    priority = "essential",
                    recurrence = "variable",
                    wallet = "cash",
                    txDateEpochMs = monthTimestamp(m, 9)
                )
            )
            // Education & Subscriptions
            list.add(
                FinancialTransaction(
                    id = "seed_exp_edu_$m",
                    title = "اشتراكات رقمية ودورات تدريبية",
                    amount = (780.0 * inflationFactor).roundToInt().toDouble(),
                    type = "expense",
                    category = "تعليم واشتراكات",
                    priority = "luxury",
                    recurrence = "fixed",
                    wallet = "ewallet",
                    txDateEpochMs = monthTimestamp(m, 5)
                )
            )
        }
        return list
    }

    fun buildSeedCommitments(): List<com.example.data.local.LocalCommitmentEntity> = listOf(
        com.example.data.local.LocalCommitmentEntity(
            id = "cmt_rosca_family",
            encryptedTitle = CryptoSecurityManager.encryptText("جمعية العائلة الشهرية"),
            type = "rosca",
            totalAmount = 30000.0,
            monthlyInstallment = 3000.0,
            paidInstallmentsCount = 4,
            totalInstallmentsCount = 10,
            dueDayOfMonth = 5,
            payoutMonthIndex = 7,
            wallet = "bank"
        ),
        com.example.data.local.LocalCommitmentEntity(
            id = "cmt_inst_laptop",
            encryptedTitle = CryptoSecurityManager.encryptText("قسط لابتوب عمل (بدون فوائد)"),
            type = "installment",
            totalAmount = 24000.0,
            monthlyInstallment = 2000.0,
            paidInstallmentsCount = 6,
            totalInstallmentsCount = 12,
            dueDayOfMonth = 10,
            payoutMonthIndex = 0,
            wallet = "bank"
        ),
        com.example.data.local.LocalCommitmentEntity(
            id = "cmt_debt_friend",
            encryptedTitle = CryptoSecurityManager.encryptText("سلفة لصديق (مستحقة لي)"),
            type = "debt_receivable",
            totalAmount = 4000.0,
            monthlyInstallment = 1000.0,
            paidInstallmentsCount = 2,
            totalInstallmentsCount = 4,
            dueDayOfMonth = 15,
            payoutMonthIndex = 0,
            wallet = "ewallet"
        )
    )

    fun buildSeedSavingsGoals(): List<com.example.data.local.LocalSavingsGoalEntity> = listOf(
        com.example.data.local.LocalSavingsGoalEntity(
            id = "goal_emergency",
            encryptedTitle = CryptoSecurityManager.encryptText("صندوق الطوارئ والأمان المالي (6 شهور)"),
            targetAmount = 50000.0,
            currentSavedAmount = 32500.0,
            categoryIcon = "shield"
        ),
        com.example.data.local.LocalSavingsGoalEntity(
            id = "goal_vacation",
            encryptedTitle = CryptoSecurityManager.encryptText("عطلة صيفية عائلية"),
            targetAmount = 15000.0,
            currentSavedAmount = 9000.0,
            categoryIcon = "flight"
        ),
        com.example.data.local.LocalSavingsGoalEntity(
            id = "goal_device",
            encryptedTitle = CryptoSecurityManager.encryptText("تحديث الهاتف والأجهزة الشخصية"),
            targetAmount = 25000.0,
            currentSavedAmount = 14000.0,
            categoryIcon = "devices"
        )
    )

    fun buildSeedAssetHoldings(): List<com.example.data.local.LocalAssetHoldingEntity> = listOf(
        com.example.data.local.LocalAssetHoldingEntity(
            assetCode = "GOLD_21",
            labelAr = "ذهب عيار 21 (مدخرات)",
            unitAr = "جرام",
            quantity = 15.0,
            unitRateInEgp = 3750.0
        ),
        com.example.data.local.LocalAssetHoldingEntity(
            assetCode = "GOLD_24",
            labelAr = "سبائك ذهب عيار 24",
            unitAr = "جرام",
            quantity = 5.0,
            unitRateInEgp = 4285.0
        ),
        com.example.data.local.LocalAssetHoldingEntity(
            assetCode = "USD",
            labelAr = "دولار أمريكي (USD)",
            unitAr = "$",
            quantity = 400.0,
            unitRateInEgp = 49.50
        ),
        com.example.data.local.LocalAssetHoldingEntity(
            assetCode = "SAR",
            labelAr = "ريال سعودي (SAR)",
            unitAr = "ر.س",
            quantity = 1000.0,
            unitRateInEgp = 13.20
        )
    )
}
