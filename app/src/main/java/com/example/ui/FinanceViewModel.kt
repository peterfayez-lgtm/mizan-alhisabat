package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDesignPreset
import com.example.data.AppSettings
import com.example.data.AppUpdateManager
import com.example.data.AppUpdateManifest
import com.example.data.CategoryBudget
import com.example.data.CryptoSecurityManager
import com.example.data.CsvSyncHelper
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinanceAnalyticsEngine
import com.example.data.FinanceRepository
import com.example.data.FinancialTransaction
import com.example.data.ForecastMode
import com.example.data.QuickTemplate
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalSavingsGoalEntity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String
) : ViewModel() {

    val transactionsState: StateFlow<UiState<List<FinancialTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<FinancialTransaction>, UiState<List<FinancialTransaction>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing transactions", error)
                emit(UiState.Error(error.message ?: "تعذر تحميل سجل المعاملات"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val budgetsState: StateFlow<UiState<List<CategoryBudget>>> =
        repository.observeBudgets(currentUserId)
            .map<List<CategoryBudget>, UiState<List<CategoryBudget>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing budgets", error)
                emit(UiState.Error(error.message ?: "تعذر تحميل الميزانيات"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val templatesState: StateFlow<UiState<List<QuickTemplate>>> =
        repository.observeTemplates(currentUserId)
            .map<List<QuickTemplate>, UiState<List<QuickTemplate>>> { UiState.Success(it) }
            .catch { error ->
                Log.w(TAG, "Error observing templates", error)
                emit(UiState.Error(error.message ?: "تعذر تحميل القوالب السريعة"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val expenseCategoriesState: StateFlow<List<String>> =
        repository.observeCustomCategories()
            .map { customList ->
                val customExpense = customList
                    .filter { it.type == "expense" }
                    .map { it.name }
                (DefaultFinanceCatalog.expenseCategories + customExpense).distinct()
            }
            .catch { emit(DefaultFinanceCatalog.expenseCategories) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DefaultFinanceCatalog.expenseCategories
            )

    val incomeCategoriesState: StateFlow<List<String>> =
        repository.observeCustomCategories()
            .map { customList ->
                val customIncome = customList
                    .filter { it.type == "income" }
                    .map { it.name }
                (DefaultFinanceCatalog.incomeCategories + customIncome).distinct()
            }
            .catch { emit(DefaultFinanceCatalog.incomeCategories) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DefaultFinanceCatalog.incomeCategories
            )

    val commitmentsState: StateFlow<List<LocalCommitmentEntity>> =
        repository.observeCommitments()
            .catch { emit(DefaultFinanceCatalog.buildSeedCommitments()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DefaultFinanceCatalog.buildSeedCommitments()
            )

    val savingsGoalsState: StateFlow<List<LocalSavingsGoalEntity>> =
        repository.observeSavingsGoals()
            .catch { emit(DefaultFinanceCatalog.buildSeedSavingsGoals()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DefaultFinanceCatalog.buildSeedSavingsGoals()
            )

    val assetHoldingsState: StateFlow<List<LocalAssetHoldingEntity>> =
        repository.observeAssetHoldings()
            .catch { emit(DefaultFinanceCatalog.buildSeedAssetHoldings()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DefaultFinanceCatalog.buildSeedAssetHoldings()
            )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _appSettings = MutableStateFlow(repository.loadAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Bank-Grade Security: 1-Tap Privacy Blur/Mask Mode + App PIN Lock
    private val _isPrivacyMasked = MutableStateFlow(_appSettings.value.autoMaskPrivacyOnLaunch)
    val isPrivacyMasked: StateFlow<Boolean> = _isPrivacyMasked.asStateFlow()

    private val _isPinConfigured = MutableStateFlow(repository.isPinLockEnabled())
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    private val _isPinLocked = MutableStateFlow(repository.isPinLockEnabled())
    val isPinLocked: StateFlow<Boolean> = _isPinLocked.asStateFlow()

    // Filters for Tab 2 (Transactions)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("all") // "all", "expense", "income", "transfer"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterPriority = MutableStateFlow("all") // "all", "essential", "luxury"
    val filterPriority: StateFlow<String> = _filterPriority.asStateFlow()

    private val _filterWallet = MutableStateFlow("all") // "all", "cash", "bank", "ewallet"
    val filterWallet: StateFlow<String> = _filterWallet.asStateFlow()

    // Forecast & What-If Simulator state for Tab 5
    private val _forecastMode = MutableStateFlow(ForecastMode.WEIGHTED_SMART)
    val forecastMode: StateFlow<ForecastMode> = _forecastMode.asStateFlow()

    private val _luxuryCutPercent = MutableStateFlow(25f) // 0f..50f
    val luxuryCutPercent: StateFlow<Float> = _luxuryCutPercent.asStateFlow()

    private val _selectedSimCategory = MutableStateFlow("مطاعم وكافيهات")
    val selectedSimCategory: StateFlow<String> = _selectedSimCategory.asStateFlow()

    private val _categoryCutPercent = MutableStateFlow(20f) // 0f..50f
    val categoryCutPercent: StateFlow<Float> = _categoryCutPercent.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isAlertDanger = MutableStateFlow(false)
    val isAlertDanger: StateFlow<Boolean> = _isAlertDanger.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateManifest?>(null)
    val availableUpdate: StateFlow<AppUpdateManifest?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            repository.saveAppSettings(_appSettings.value)
            repository.ensureSeedDataIfEmpty(currentUserId)
            if (_appSettings.value.autoCheckUpdatesOnLaunch) {
                checkForAppUpdates(manualTrigger = false)
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun selectDesignPreset(preset: AppDesignPreset) {
        val updated = _appSettings.value.copy(designPresetId = preset.id)
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        _bannerMessage.value = "🎨 تم تفعيل ديزاين الواجهة: «${preset.titleAr}» بنجاح!"
    }

    fun togglePrivacyMask() {
        _isPrivacyMasked.value = !_isPrivacyMasked.value
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = repository.verifyPin(pin)
        if (ok) {
            _isPinLocked.value = false
        }
        return ok
    }

    fun configureAppPin(newPin: String?) {
        repository.setOrClearPin(newPin)
        val enabled = repository.isPinLockEnabled()
        _isPinConfigured.value = enabled
        _isPinLocked.value = false
        _isAlertDanger.value = false
        _bannerMessage.value = if (enabled) {
            "🔒 تم تفعيل قفل التطبيق بالرمز السري (PIN) المشفر بنجاح."
        } else {
            "تم إلغاء قفل الرمز السري (PIN)."
        }
    }

    fun lockAppNow() {
        if (_isPinConfigured.value) {
            _isPinLocked.value = true
        }
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun updateFilterType(type: String) {
        _filterType.value = type
    }

    fun updateFilterPriority(priority: String) {
        _filterPriority.value = priority
    }

    fun updateFilterWallet(wallet: String) {
        _filterWallet.value = wallet
    }

    fun setForecastMode(mode: ForecastMode) {
        _forecastMode.value = mode
    }

    fun setLuxuryCutPercent(pct: Float) {
        _luxuryCutPercent.value = pct.coerceIn(0f, 50f)
    }

    fun setSelectedSimCategory(category: String) {
        _selectedSimCategory.value = category
    }

    fun setCategoryCutPercent(pct: Float) {
        _categoryCutPercent.value = pct.coerceIn(0f, 50f)
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun updateAppSettings(newSettings: AppSettings) {
        repository.saveAppSettings(newSettings)
        _appSettings.value = newSettings
        _isAlertDanger.value = false
        _bannerMessage.value = "⚙️ تم حفظ إعدادات البرنامج بنجاح."
    }

    fun addCustomCategory(name: String, type: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val res = repository.addCustomCategory(trimmed, type)
            if (res.isSuccess) {
                val typeLabel = if (type == "income") "الإيرادات" else "المصروفات"
                _isAlertDanger.value = false
                _bannerMessage.value = "✅ تمت إضافة البند الجديد «$trimmed» إلى فئات $typeLabel بنجاح."
            }
        }
    }

    fun deleteCustomCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(name, type)
            _isAlertDanger.value = false
            _bannerMessage.value = "🗑️ تم حذف البند المخصص «${name.trim()}»."
        }
    }

    fun recordQuickTemplate(template: QuickTemplate) {
        val tx = FinancialTransaction(
            userId = currentUserId,
            title = template.title,
            amount = template.amount,
            type = "expense",
            category = template.category,
            priority = template.priority,
            recurrence = template.recurrence,
            wallet = template.wallet,
            txDateEpochMs = System.currentTimeMillis()
        )
        addTransactionWithInstantCheck(tx)
    }

    /**
     * Wallet-to-Wallet Transfer (التحويل بين المحافظ دون التأثير على إجمالي المصروفات)
     */
    fun recordWalletTransfer(fromWallet: String, toWallet: String, amount: Double, note: String) {
        if (amount <= 0.0 || fromWallet == toWallet) return
        val fromAr = DefaultFinanceCatalog.walletLabelAr(fromWallet)
        val toAr = DefaultFinanceCatalog.walletLabelAr(toWallet)
        val title = note.trim().ifBlank { "تحويل من $fromAr إلى $toAr" }
        val tx = FinancialTransaction(
            userId = currentUserId,
            title = title,
            amount = amount,
            type = "transfer",
            category = "تحويل بين المحافظ",
            priority = "essential",
            recurrence = toWallet, // Destination wallet stored in recurrence when type == "transfer"
            wallet = fromWallet,
            txDateEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val res = repository.addTransaction(tx)
            if (res.isSuccess) {
                _isAlertDanger.value = false
                _bannerMessage.value = "⇄ تم تحويل ${amount.toInt()} ج.م من «$fromAr» إلى «$toAr» بنجاح."
            }
        }
    }

    /**
     * Auto-Recurring Monthly Fixed Transactions Engine (توليد المعاملات الثابتة الشهرية تلقائياً)
     */
    fun autoGenerateMonthlyFixedTransactions() {
        viewModelScope.launch {
            val currentTxList = (transactionsState.value as? UiState.Success)?.data ?: emptyList()
            val currentTemplates = (templatesState.value as? UiState.Success)?.data ?: DefaultFinanceCatalog.buildSeedTemplates()
            val currentMonthKey = FinanceAnalyticsEngine.yearMonthKey(System.currentTimeMillis())

            val thisMonthTitles = currentTxList
                .filter { FinanceAnalyticsEngine.yearMonthKey(it.txDateEpochMs) == currentMonthKey }
                .map { it.title }
                .toSet()

            // Collect fixed templates and historical fixed transactions not yet recorded this month
            val fixedTemplates = currentTemplates.filter { it.recurrence == "fixed" }
            var addedCount = 0

            for (tpl in fixedTemplates) {
                if (tpl.title !in thisMonthTitles) {
                    repository.addTransaction(
                        FinancialTransaction(
                            userId = currentUserId,
                            title = tpl.title,
                            amount = tpl.amount,
                            type = "expense",
                            category = tpl.category,
                            priority = tpl.priority,
                            recurrence = "fixed",
                            wallet = tpl.wallet,
                            txDateEpochMs = System.currentTimeMillis()
                        )
                    )
                    addedCount++
                }
            }

            // Also check active ROSCAs/Installments due this month
            val commitments = commitmentsState.value
            for (cmt in commitments) {
                val cmtTitle = "قسط/دفعة: ${cmt.decryptedTitle}"
                if (cmt.paidInstallmentsCount < cmt.totalInstallmentsCount && cmtTitle !in thisMonthTitles) {
                    val txType = if (cmt.type == "debt_receivable") "income" else "expense"
                    repository.addTransaction(
                        FinancialTransaction(
                            userId = currentUserId,
                            title = cmtTitle,
                            amount = cmt.monthlyInstallment,
                            type = txType,
                            category = if (cmt.type == "rosca") "جمعية شهرية" else "أقساط والتزامات",
                            priority = "essential",
                            recurrence = "fixed",
                            wallet = cmt.wallet,
                            txDateEpochMs = System.currentTimeMillis()
                        )
                    )
                    repository.upsertCommitment(
                        cmt.copy(paidInstallmentsCount = (cmt.paidInstallmentsCount + 1).coerceAtMost(cmt.totalInstallmentsCount))
                    )
                    addedCount++
                }
            }

            _isAlertDanger.value = false
            _bannerMessage.value = if (addedCount > 0) {
                "⚡ تم تسجيل $addedCount معاملة/قسط شهري ثابت لهذا الشهر تلقائياً بنجاح!"
            } else {
                "✅ جميع المعاملات والأقساط الشهرية الثابتة لهذا الشهر مسجلة بالفعل."
            }
        }
    }

    fun addTransactionWithInstantCheck(tx: FinancialTransaction) {
        viewModelScope.launch {
            val currentTxList = (transactionsState.value as? UiState.Success)?.data ?: emptyList()
            val currentBudgets = (budgetsState.value as? UiState.Success)?.data ?: DefaultFinanceCatalog.buildSeedBudgets()

            val result = repository.addTransaction(tx)
            if (result.isSuccess) {
                if (tx.type == "expense") {
                    val currentMonthKey = FinanceAnalyticsEngine.yearMonthKey(System.currentTimeMillis())
                    val previousCatSpent = currentTxList
                        .filter { it.type == "expense" && it.category == tx.category && FinanceAnalyticsEngine.yearMonthKey(it.txDateEpochMs) == currentMonthKey }
                        .sumOf { it.amount }
                    val newCatSpent = previousCatSpent + tx.amount
                    val budgetLimit = currentBudgets.firstOrNull { it.category == tx.category }?.monthlyLimit ?: 0.0

                    if (budgetLimit > 0.0 && newCatSpent >= budgetLimit) {
                        _isAlertDanger.value = true
                        _bannerMessage.value = "⚠️ تنبيه فوري: تجاوزت سقف ميزانية «${tx.category}»! (${newCatSpent.toInt()} من ${budgetLimit.toInt()} ج.م)"
                    } else if (budgetLimit > 0.0 && newCatSpent >= budgetLimit * 0.80) {
                        _isAlertDanger.value = false
                        _bannerMessage.value = "⚡ تنبيه: وصلت إلى ${(newCatSpent / budgetLimit * 100).toInt()}% من ميزانية «${tx.category}»."
                    } else {
                        _isAlertDanger.value = false
                        _bannerMessage.value = "✅ تم تسجيل «${tx.title}» (${tx.amount.toInt()} ج.م) ومشفرة محلياً بنجاح."
                    }
                } else if (tx.type == "transfer") {
                    _isAlertDanger.value = false
                    _bannerMessage.value = "⇄ تم تسجيل التحويل «${tx.title}» (${tx.amount.toInt()} ج.م) بنجاح."
                } else {
                    _isAlertDanger.value = false
                    _bannerMessage.value = "✅ تم إضافة الدخل «${tx.title}» (+${tx.amount.toInt()} ج.م) ومشفرة محلياً بنجاح."
                }
            } else {
                _isAlertDanger.value = true
                _bannerMessage.value = "تعذر حفظ المعاملة: ${result.exceptionOrNull()?.localizedMessage ?: "خطأ في الحفظ"}"
            }
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
            _isAlertDanger.value = false
            _bannerMessage.value = "تم حذف المعاملة من السجل."
        }
    }

    fun updateCategoryBudget(category: String, newLimit: Double) {
        viewModelScope.launch {
            val currentBudgets = (budgetsState.value as? UiState.Success)?.data ?: emptyList()
            val existing = currentBudgets.firstOrNull { it.category == category }
            val res = repository.upsertBudget(category, newLimit, existing?.id)
            if (res.isSuccess) {
                _isAlertDanger.value = false
                _bannerMessage.value = "✅ تم تحديث سقف ميزانية «$category» إلى ${newLimit.toInt()} ج.م"
            }
        }
    }

    fun addQuickTemplate(template: QuickTemplate) {
        viewModelScope.launch {
            val res = repository.addQuickTemplate(template)
            if (res.isSuccess) {
                _isAlertDanger.value = false
                _bannerMessage.value = "✅ تمت إضافة القالب السريع «${template.title}»"
            }
        }
    }

    fun deleteQuickTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteQuickTemplate(templateId)
        }
    }

    // Commitments (ROSCAs / الجمعيات، الأقساط، والديون)
    fun addCommitment(
        title: String,
        type: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        totalInstallmentsCount: Int,
        dueDayOfMonth: Int,
        payoutMonthIndex: Int,
        wallet: String
    ) {
        viewModelScope.launch {
            val entity = LocalCommitmentEntity(
                id = "cmt_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                encryptedTitle = CryptoSecurityManager.encryptText(title.trim()),
                type = type,
                totalAmount = totalAmount,
                monthlyInstallment = monthlyInstallment,
                paidInstallmentsCount = 0,
                totalInstallmentsCount = totalInstallmentsCount.coerceAtLeast(1),
                dueDayOfMonth = dueDayOfMonth.coerceIn(1, 28),
                payoutMonthIndex = payoutMonthIndex,
                wallet = wallet
            )
            repository.upsertCommitment(entity)
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تمت إضافة «${title.trim()}» إلى سجل الالتزامات والجمعيات."
        }
    }

    fun payCommitmentInstallment(commitment: LocalCommitmentEntity) {
        if (commitment.paidInstallmentsCount >= commitment.totalInstallmentsCount) return
        viewModelScope.launch {
            val newPaid = commitment.paidInstallmentsCount + 1
            repository.upsertCommitment(commitment.copy(paidInstallmentsCount = newPaid))
            val isReceivable = commitment.type == "debt_receivable"
            val txTitle = if (isReceivable) {
                "تحصيل دفعة: ${commitment.decryptedTitle} ($newPaid/${commitment.totalInstallmentsCount})"
            } else {
                "سداد قسط: ${commitment.decryptedTitle} ($newPaid/${commitment.totalInstallmentsCount})"
            }
            repository.addTransaction(
                FinancialTransaction(
                    userId = currentUserId,
                    title = txTitle,
                    amount = commitment.monthlyInstallment,
                    type = if (isReceivable) "income" else "expense",
                    category = when (commitment.type) {
                        "rosca" -> "جمعية شهرية"
                        "installment" -> "أقساط مشتريات"
                        else -> "ديون والتزامات"
                    },
                    priority = "essential",
                    recurrence = "fixed",
                    wallet = commitment.wallet,
                    txDateEpochMs = System.currentTimeMillis()
                )
            )
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم تسجيل دفعة «${commitment.decryptedTitle}» ($newPaid من ${commitment.totalInstallmentsCount}) بنجاح."
        }
    }

    fun deleteCommitment(id: String) {
        viewModelScope.launch {
            repository.deleteCommitment(id)
        }
    }

    // Smart Savings Goals (الحصالة الذكية وأهداف الادخار)
    fun addSavingsGoal(title: String, targetAmount: Double, initialSaved: Double) {
        viewModelScope.launch {
            val entity = LocalSavingsGoalEntity(
                id = "goal_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                encryptedTitle = CryptoSecurityManager.encryptText(title.trim()),
                targetAmount = targetAmount.coerceAtLeast(1.0),
                currentSavedAmount = initialSaved.coerceAtLeast(0.0),
                categoryIcon = "savings"
            )
            repository.upsertSavingsGoal(entity)
            _isAlertDanger.value = false
            _bannerMessage.value = "🎯 تمت إضافة هدف الادخار «${title.trim()}» بنجاح."
        }
    }

    fun depositIntoSavingsGoal(goal: LocalSavingsGoalEntity, amount: Double, recordAsExpenseFromWallet: String? = null) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val updated = goal.copy(currentSavedAmount = goal.currentSavedAmount + amount)
            repository.upsertSavingsGoal(updated)
            if (!recordAsExpenseFromWallet.isNullOrBlank()) {
                repository.addTransaction(
                    FinancialTransaction(
                        userId = currentUserId,
                        title = "إيداع ادخار: ${goal.decryptedTitle}",
                        amount = amount,
                        type = "expense",
                        category = "ادخار واستثمار",
                        priority = "essential",
                        recurrence = "variable",
                        wallet = recordAsExpenseFromWallet,
                        txDateEpochMs = System.currentTimeMillis()
                    )
                )
            }
            _isAlertDanger.value = false
            _bannerMessage.value = "💰 تم إيداع ${amount.toInt()} ج.م في «${goal.decryptedTitle}» بنجاح!"
        }
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
        }
    }

    // Multi-Currency & Gold Holdings
    fun upsertAssetHolding(assetCode: String, labelAr: String, unitAr: String, quantity: Double, unitRateInEgp: Double) {
        viewModelScope.launch {
            repository.upsertAssetHolding(
                LocalAssetHoldingEntity(
                    assetCode = assetCode.trim().uppercase(),
                    labelAr = labelAr.trim(),
                    unitAr = unitAr.trim(),
                    quantity = quantity.coerceAtLeast(0.0),
                    unitRateInEgp = unitRateInEgp.coerceAtLeast(0.0)
                )
            )
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم تحديث رصيد وسعر «$labelAr» بنجاح."
        }
    }

    fun refreshLiveCurrencyAndGoldRates() {
        viewModelScope.launch {
            val res = com.example.data.FreeIntegrationsHelper.fetchLiveRatesInEgp()
            val liveMap = res.getOrNull()
            if (liveMap != null) {
                val currentHoldings = assetHoldingsState.value
                for (asset in currentHoldings) {
                    val newRate = liveMap[asset.assetCode.uppercase()]
                    if (newRate != null && newRate > 0.0) {
                        repository.upsertAssetHolding(asset.copy(unitRateInEgp = newRate))
                    }
                }
                _isAlertDanger.value = false
                _bannerMessage.value = "🌐 تم تحديث أسعار العملات الأجنبية والذهب تلقائياً من السوق اللحظي بنجاح!"
            } else {
                _isAlertDanger.value = false
                _bannerMessage.value = "ℹ️ التطبيق يعمل الآن أوفلاين — يتم استخدام آخر أسعار صرف وذهب محفوظة محلياً."
            }
        }
    }

    fun recordSpiritualGiving(title: String, amount: Double, category: String, wallet: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.addCustomCategory(category, "expense")
            repository.addTransaction(
                FinancialTransaction(
                    userId = currentUserId,
                    title = title.trim(),
                    amount = amount,
                    type = "expense",
                    category = category,
                    priority = "essential",
                    recurrence = "variable",
                    wallet = wallet,
                    txDateEpochMs = System.currentTimeMillis()
                )
            )
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم تسجيل «${title.trim()}» بقيمة ${amount.toInt()} ج.م ضمن بند «$category» بنجاح."
        }
    }

    fun resetAndPopulateSeedData() {
        viewModelScope.launch {
            try {
                repository.populateRichSeedData(currentUserId)
                _isAlertDanger.value = false
                _bannerMessage.value = "✅ تمت استعادة وتحديث البيانات التجريبية لـ 4 شهور بنجاح."
            } catch (e: Exception) {
                _isAlertDanger.value = true
                _bannerMessage.value = "تعذر تحديث البيانات التجريبية: ${e.localizedMessage}"
            }
        }
    }

    fun clearAllDataAndStartFresh() {
        viewModelScope.launch {
            try {
                repository.clearAllTransactionsAndStartFresh()
                val updatedSettings = _appSettings.value.copy(
                    dailySpendMode = 0,
                    customDailySafeLimit = 0.0
                )
                repository.saveAppSettings(updatedSettings)
                _appSettings.value = updatedSettings
                _isAlertDanger.value = false
                _bannerMessage.value = "✅ تم مسح جميع البيانات التجريبية وتصفير الرصيد وحد الصرف اليومي والميزانيات إلى (0 ج.م) بالكامل!"
            } catch (e: Exception) {
                _isAlertDanger.value = true
                _bannerMessage.value = "تعذر مسح البيانات: ${e.localizedMessage}"
            }
        }
    }

    fun zeroAllCategoryBudgets() {
        viewModelScope.launch {
            repository.zeroAllCategoryBudgets()
            val updated = _appSettings.value.copy(
                dailySpendMode = 0,
                customDailySafeLimit = 0.0
            )
            repository.saveAppSettings(updated)
            _appSettings.value = updated
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم تصفير حدود الميزانيات الافتراضية (الـ 18,700 ج.م) وحد الصرف اليومي إلى 0 ج.م بنجاح."
        }
    }

    fun setTotalMonthlyBudgetAndDistribute(totalMonthlyBudget: Double, remainingDaysOverride: Int = _appSettings.value.customRemainingDaysOverride) {
        viewModelScope.launch {
            val cleanTotal = totalMonthlyBudget.coerceAtLeast(0.0)
            repository.setTotalMonthlyBudgetDistribute(cleanTotal)
            val updated = _appSettings.value.copy(
                dailySpendMode = if (cleanTotal > 0.0) 3 else 0,
                customDailySafeLimit = 0.0,
                customRemainingDaysOverride = remainingDaysOverride.coerceIn(0, 60)
            )
            repository.saveAppSettings(updated)
            _appSettings.value = updated
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم ضبط إجمالي ميزانية الشهر إلى ${cleanTotal.toInt()} ج.م وتوزيعها على الفئات وحساب الحد اليومي تلقائياً."
        }
    }

    fun updateDailySpendControl(
        mode: Int,
        customDailyLimit: Double,
        customRemainingDaysOverride: Int
    ) {
        val updated = _appSettings.value.copy(
            dailySpendMode = mode.coerceIn(0, 3),
            customDailySafeLimit = customDailyLimit.coerceAtLeast(0.0),
            customRemainingDaysOverride = customRemainingDaysOverride.coerceIn(0, 60)
        )
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        _bannerMessage.value = "✅ تم حفظ إعدادات التحكم في حد الصرف اليومي بنجاح!"
    }

    fun updateWhatsAppDailyReportSettings(
        phoneNumber: String,
        midnightAutoEnabled: Boolean,
        reportHour: Int,
        reportMinute: Int,
        callMeBotApiKey: String,
        webhookUrl: String = "",
        metaAccessToken: String = "",
        metaPhoneNumberId: String = "",
        metaWabaId: String = "",
        telegramBotEnabled: Boolean = _appSettings.value.telegramBotEnabled,
        telegramBotToken: String = _appSettings.value.telegramBotToken,
        telegramChatId: String = _appSettings.value.telegramChatId
    ) {
        val cleanPhone = com.example.data.FreeIntegrationsHelper.normalizeWhatsAppPhoneNumber(
            phoneNumber.ifBlank { "201021992233" }
        )
        val cleanApiKey = callMeBotApiKey.trim().ifBlank { "3591043" }
        val updated = _appSettings.value.copy(
            whatsappPhoneNumber = cleanPhone,
            whatsappMidnightAutoEnabled = midnightAutoEnabled,
            whatsappReportHour = reportHour.coerceIn(0, 23),
            whatsappReportMinute = reportMinute.coerceIn(0, 59),
            whatsappCallMeBotApiKey = cleanApiKey,
            whatsappWebhookUrl = "",
            whatsappMetaAccessToken = "",
            whatsappMetaPhoneNumberId = "",
            whatsappMetaWabaId = "",
            telegramBotEnabled = telegramBotEnabled,
            telegramBotToken = telegramBotToken.trim(),
            telegramChatId = telegramChatId.trim()
        )
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        val scheduleLabel = com.example.data.WhatsAppMidnightScheduler.formatScheduleTimeAr(
            updated.whatsappReportHour,
            updated.whatsappReportMinute
        )
        val tgSuffix = if (updated.telegramBotEnabled && updated.telegramBotToken.isNotBlank()) " + بوت تليجرام" else ""
        _bannerMessage.value = if (updated.whatsappMidnightAutoEnabled || updated.telegramBotEnabled) {
            "✅ تم تفعيل إرسال التقرير اليومي عبر CallMeBot (+$cleanPhone • كود $cleanApiKey)$tgSuffix في موعد ($scheduleLabel) بنجاح!"
        } else {
            "✅ تم حفظ إعدادات واتساب وتليجرام بنجاح."
        }
    }

    fun testSendBackgroundWhatsAppNow(summaryMessage: String) {
        val settings = _appSettings.value
        viewModelScope.launch {
            val res = com.example.data.FreeIntegrationsHelper.sendBackgroundWhatsAppReport(
                phoneNumber = settings.whatsappPhoneNumber.ifBlank { "201021992233" },
                callMeBotApiKey = settings.whatsappCallMeBotApiKey.ifBlank { "3591043" },
                message = summaryMessage
            )
            res.onSuccess { msg ->
                _isAlertDanger.value = false
                _bannerMessage.value = msg
            }.onFailure { err ->
                _isAlertDanger.value = true
                _bannerMessage.value = "⚠️ تعذر الإرسال عبر CallMeBot: ${err.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}"
            }
        }
    }

    fun checkForAppUpdates(manualTrigger: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val settings = _appSettings.value
            val result = AppUpdateManager.checkForUpdates(settings)
            _isCheckingUpdate.value = false

            result.onSuccess { manifest ->
                val currentCode = AppUpdateManager.currentVersionCode
                if (manifest.latestVersionCode > currentCode) {
                    _availableUpdate.value = manifest
                    if (manualTrigger) {
                        _isAlertDanger.value = false
                        _bannerMessage.value = "🚀 يتوفر تحديث جديد للبرنامج (الإصدار v${manifest.latestVersionName} - Build #${manifest.latestVersionCode})! يمكنك تحميله وتثبيته مع حفظ جميع بياناتك."
                    }
                } else {
                    _availableUpdate.value = null
                    if (manualTrigger) {
                        _isAlertDanger.value = false
                        _bannerMessage.value = "✅ أنت تستخدم أحدث إصدار من البرنامج حالياً (الإصدار v${AppUpdateManager.currentVersionName} - Build #$currentCode)."
                    }
                }
            }.onFailure { err ->
                if (manualTrigger) {
                    _isAlertDanger.value = false
                    _bannerMessage.value = "ℹ️ تعذر الاتصال برابط التحديث السحابي (${err.localizedMessage ?: "تأكد من الرابط"}) — الإصدار الحالي المثبت هو v${AppUpdateManager.currentVersionName}."
                }
            }
        }
    }

    fun saveUpdateCenterSettings(
        autoCheckOnLaunch: Boolean,
        updateManifestUrl: String,
        publishedVersionCode: Int,
        publishedVersionName: String,
        publishedApkUrl: String,
        publishedReleaseNotes: String,
        publishedIsMandatory: Boolean
    ) {
        val updated = _appSettings.value.copy(
            autoCheckUpdatesOnLaunch = autoCheckOnLaunch,
            updateManifestUrl = updateManifestUrl.trim(),
            publishedLatestVersionCode = publishedVersionCode.coerceAtLeast(1),
            publishedLatestVersionName = publishedVersionName.trim().ifBlank { AppUpdateManager.currentVersionName },
            publishedApkDownloadUrl = publishedApkUrl.trim(),
            publishedReleaseNotesAr = publishedReleaseNotes.trim(),
            publishedIsMandatoryUpdate = publishedIsMandatory
        )
        repository.saveAppSettings(updated)
        _appSettings.value = updated

        if (updated.publishedLatestVersionCode > AppUpdateManager.currentVersionCode) {
            _availableUpdate.value = AppUpdateManifest(
                latestVersionCode = updated.publishedLatestVersionCode,
                latestVersionName = updated.publishedLatestVersionName,
                apkDownloadUrl = updated.publishedApkDownloadUrl,
                releaseNotesAr = updated.publishedReleaseNotesAr,
                isMandatoryUpdate = updated.publishedIsMandatoryUpdate,
                publishedDateAr = FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())
            )
            _isAlertDanger.value = false
            _bannerMessage.value = "🚀 تم حفظ وتفعيل إشعار التحديث الجديد (الإصدار v${updated.publishedLatestVersionName}) بنجاح!"
        } else {
            _availableUpdate.value = null
            _isAlertDanger.value = false
            _bannerMessage.value = "✅ تم حفظ إعدادات مركز التحديثات بنجاح."
        }
    }

    fun simulateNewUpdatePreview(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        isMandatory: Boolean
    ) {
        val simCode = versionCode.coerceAtLeast(AppUpdateManager.currentVersionCode + 1)
        val simName = versionName.trim().ifBlank { "$simCode.0.0" }
        val manifest = AppUpdateManifest(
            latestVersionCode = simCode,
            latestVersionName = simName,
            apkDownloadUrl = apkUrl.trim(),
            releaseNotesAr = releaseNotes.trim().ifBlank {
                "• إضافة مزايا ذكية جديدة وتحسين سرعة الأداء.\n• تثبيت التحديث يحفظ 100% من معاملاتك وأرصدتك تلقائياً."
            },
            isMandatoryUpdate = isMandatory,
            publishedDateAr = FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())
        )
        _availableUpdate.value = manifest
        _isAlertDanger.value = false
        _bannerMessage.value = "🚀 معاينة حية: هكذا يظهر إشعار التحديث الجديد (v$simName) لجميع المستخدمين أعلى الشاشة!"
    }

    fun dismissAvailableUpdateBanner() {
        _availableUpdate.value = null
    }

    fun importCsvContent(csvOrBackupContent: String) {
        viewModelScope.launch {
            val fullRestore = CsvSyncHelper.parseFullSystemRestorePayload(csvOrBackupContent)
            if (fullRestore == null) {
                _isAlertDanger.value = true
                _bannerMessage.value = "لم يتم العثور على بيانات صالحة داخل الملف أو النص المستورد."
                return@launch
            }

            fullRestore.restoredSettings?.let { s ->
                repository.saveAppSettings(s)
                _appSettings.value = s
            }
            for (cat in fullRestore.customExpenseCategories) {
                repository.addCustomCategory(cat, "expense")
            }
            for (cat in fullRestore.customIncomeCategories) {
                repository.addCustomCategory(cat, "income")
            }
            for (b in fullRestore.budgets) {
                repository.upsertBudget(b.category, b.monthlyLimit, b.id.takeIf { it.isNotBlank() })
            }
            for (tpl in fullRestore.templates) {
                repository.addQuickTemplate(tpl.copy(userId = currentUserId))
            }
            for (cmt in fullRestore.commitments) {
                repository.upsertCommitment(cmt)
            }
            for (goal in fullRestore.savingsGoals) {
                repository.upsertSavingsGoal(goal)
            }
            for (asset in fullRestore.assetHoldings) {
                repository.upsertAssetHolding(asset)
            }

            var txCount = 0
            for (tx in fullRestore.transactions) {
                val r = repository.addTransaction(tx.copy(userId = currentUserId))
                if (r.isSuccess) txCount++
            }

            _isAlertDanger.value = false
            _bannerMessage.value = if (fullRestore.commitments.isNotEmpty() || fullRestore.savingsGoals.isNotEmpty() || fullRestore.restoredSettings != null) {
                "✅ تمت استعادة النسخة الاحتياطية الشاملة بنجاح ($txCount معاملة + الميزانيات + الجمعيات + الأهداف + الإعدادات)!"
            } else {
                "✅ تم استيراد ودمج $txCount معاملة بنجاح!"
            }
        }
    }

    fun toggleSimpleMinimalistUiMode() {
        val nextMode = !_appSettings.value.simpleMinimalistUiMode
        val updated = _appSettings.value.copy(simpleMinimalistUiMode = nextMode)
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        _bannerMessage.value = if (nextMode) {
            "✨ تم تفعيل «الواجهة البنكية الاحترافية البسيطة» (Minimalist Pro Mode) بنجاح."
        } else {
            "📊 تم التبديل إلى «وضع العرض التفصيلي الكامل» للداش بورد."
        }
    }

    fun toggleAutoCaptureBankNotifications(enabled: Boolean) {
        val updated = _appSettings.value.copy(autoCaptureBankNotifications = enabled)
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        _bannerMessage.value = if (enabled) {
            "📩 تم تفعيل قارئ إشعارات البنوك و InstaPay وفودافون كاش التلقائي."
        } else {
            "تم إيقاف القراءة التلقائية لإشعارات البنوك."
        }
    }

    fun saveGoogleSheetsSettings(webAppUrl: String, autoSyncOnNewTx: Boolean) {
        val updated = _appSettings.value.copy(
            googleSheetsWebAppUrl = webAppUrl.trim(),
            autoSyncGoogleSheetsOnNewTx = autoSyncOnNewTx
        )
        repository.saveAppSettings(updated)
        _appSettings.value = updated
        _isAlertDanger.value = false
        _bannerMessage.value = "☁️ تم حفظ إعدادات الربط المجاني مع Google Sheets بنجاح."
    }

    fun syncAllDataToGoogleSheetsNow() {
        val url = _appSettings.value.googleSheetsWebAppUrl.trim()
        if (url.isBlank()) {
            _isAlertDanger.value = true
            _bannerMessage.value = "⚠️ يرجى لصق رابط Google Apps Script Web App أولاً لإتمام المزامنة السحابية."
            return
        }
        viewModelScope.launch {
            val txs = (transactionsState.value as? UiState.Success)?.data ?: DefaultFinanceCatalog.buildSeedTransactions()
            val budgets = (budgetsState.value as? UiState.Success)?.data ?: DefaultFinanceCatalog.buildSeedBudgets()
            val metrics = FinanceAnalyticsEngine.computeDashboardMetrics(
                transactions = txs,
                budgets = budgets,
                appSettings = _appSettings.value
            )
            val res = com.example.data.FreeIntegrationsHelper.syncAllDataToGoogleSheetsWebApp(
                webAppUrl = url,
                transactions = txs,
                budgets = budgets,
                commitments = commitmentsState.value,
                savingsGoals = savingsGoalsState.value,
                metrics = metrics
            )
            res.onSuccess { msg ->
                _isAlertDanger.value = false
                _bannerMessage.value = msg
            }.onFailure { err ->
                _isAlertDanger.value = true
                _bannerMessage.value = "⚠️ تعذرت المزامنة مع Google Sheets (${err.localizedMessage ?: "تحقق من الرابط والإنترنت"})."
            }
        }
    }

    private companion object {
        const val TAG = "FinanceViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

