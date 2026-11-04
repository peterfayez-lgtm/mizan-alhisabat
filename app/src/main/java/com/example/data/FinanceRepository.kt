package com.example.data

import android.content.Context
import com.example.R
import com.example.data.local.LocalAssetHoldingEntity
import com.example.data.local.LocalCommitmentEntity
import com.example.data.local.LocalCustomCategoryEntity
import com.example.data.local.LocalFinanceDao
import com.example.data.local.LocalFinanceDatabase
import com.example.data.local.LocalSavingsGoalEntity
import com.example.data.local.toLocalEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val localDao: LocalFinanceDao? = null,
    private val appContext: Context? = null
) {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localDao = LocalFinanceDatabase.getInstance(context.applicationContext).financeDao(),
        appContext = context.applicationContext
    )

    private val auth = Firebase.auth

    companion object {
        const val OFFLINE_USER_ID = "offline_local_user"

        fun loadAppSettingsFromContext(context: Context): AppSettings {
            val prefs = context.applicationContext.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE)
            val alreadyMigratedCallMeBot = prefs.getBoolean("callmebot_3591043_activated_v1", false)
            if (!alreadyMigratedCallMeBot) {
                prefs.edit()
                    .putString("whatsapp_phone_number", "201021992233")
                    .putString("whatsapp_callmebot_api_key", "3591043")
                    .putBoolean("whatsapp_midnight_auto_enabled", true)
                    .putBoolean("callmebot_3591043_activated_v1", true)
                    .apply()
            }
            val savedPhone = prefs.getString("whatsapp_phone_number", "201021992233")?.takeIf { it.isNotBlank() } ?: "201021992233"
            val savedApiKey = prefs.getString("whatsapp_callmebot_api_key", "3591043")?.takeIf { it.isNotBlank() } ?: "3591043"

            return AppSettings(
                accountTitle = prefs.getString("account_title", "حسابي المالي الشخصي") ?: "حسابي المالي الشخصي",
                currencySymbol = prefs.getString("currency_symbol", "ج.م") ?: "ج.م",
                salaryDayOfMonth = prefs.getInt("salary_day_of_month", 1).coerceIn(1, 28),
                luxurySafeThresholdPercent = prefs.getInt("luxury_safe_threshold_pct", 30).coerceIn(15, 50),
                defaultSpiritualGivingMode = prefs.getInt("default_spiritual_mode", 1).coerceIn(1, 2),
                useGrossIncomeForOrthodoxTithe = prefs.getBoolean("use_gross_orthodox_tithe", true),
                autoMaskPrivacyOnLaunch = prefs.getBoolean("auto_mask_privacy_on_launch", false),
                enableSmartBudgetAlerts = prefs.getBoolean("enable_smart_budget_alerts", true),
                designPresetId = prefs.getString("design_preset_id_v3", AppDesignPreset.WARM_OASIS.id)
                    ?: AppDesignPreset.WARM_OASIS.id,
                dailySpendMode = prefs.getInt("daily_spend_mode", 0).coerceIn(0, 3),
                customDailySafeLimit = (prefs.getFloat("custom_daily_safe_limit", 0f)).toDouble().coerceAtLeast(0.0),
                customRemainingDaysOverride = prefs.getInt("custom_remaining_days_override", 0).coerceIn(0, 60),
                whatsappPhoneNumber = savedPhone,
                whatsappMidnightAutoEnabled = prefs.getBoolean("whatsapp_midnight_auto_enabled", true),
                whatsappReportHour = prefs.getInt("whatsapp_report_hour", 0).coerceIn(0, 23),
                whatsappReportMinute = prefs.getInt("whatsapp_report_minute", 0).coerceIn(0, 59),
                whatsappCallMeBotApiKey = savedApiKey,
                whatsappWebhookUrl = prefs.getString("whatsapp_webhook_url", "") ?: "",
                whatsappMetaAccessToken = prefs.getString("whatsapp_meta_access_token", "") ?: "",
                whatsappMetaPhoneNumberId = prefs.getString("whatsapp_meta_phone_number_id", "") ?: "",
                whatsappMetaWabaId = prefs.getString("whatsapp_meta_waba_id", "") ?: "",
                telegramBotEnabled = prefs.getBoolean("telegram_bot_enabled", false),
                telegramBotToken = prefs.getString("telegram_bot_token", "") ?: "",
                telegramChatId = prefs.getString("telegram_chat_id", "") ?: "",
                autoCheckUpdatesOnLaunch = prefs.getBoolean("auto_check_updates_on_launch", true),
                updateManifestUrl = prefs.getString("update_manifest_url", "")?.takeIf { it.isNotBlank() }
                    ?: "https://api.github.com/repos/peterfayez-lgtm/mizan-alhisabat/releases/latest",
                publishedLatestVersionCode = prefs.getInt("published_latest_version_code", 3).coerceAtLeast(1),
                publishedLatestVersionName = prefs.getString("published_latest_version_name", "2.1.0") ?: "2.1.0",
                publishedApkDownloadUrl = prefs.getString("published_apk_download_url", "")?.takeIf { it.isNotBlank() }
                    ?: "https://github.com/peterfayez-lgtm/mizan-alhisabat/releases/latest/download/Mizan-AlHisabat-Latest.apk",
                publishedReleaseNotesAr = prefs.getString(
                    "published_release_notes_ar",
                    "• تفعيل الإرسال التلقائي الأساسي عبر CallMeBot للواتساب وإضافة بوت تليجرام.\n• ربط التحديث السحابي التلقائي بمستودع peterfayez-lgtm/mizan-alhisabat على GitHub Releases.\n• حاسبة سريعة لإضافة المعاملات في 3 ثوانٍ مع الجمع والطرح الفوري."
                ) ?: "",
                publishedIsMandatoryUpdate = prefs.getBoolean("published_is_mandatory_update", false),
                simpleMinimalistUiMode = prefs.getBoolean("simple_minimalist_ui_mode", true),
                autoCaptureBankNotifications = prefs.getBoolean("auto_capture_bank_notifications", true),
                googleSheetsWebAppUrl = prefs.getString("google_sheets_web_app_url", "") ?: "",
                autoSyncGoogleSheetsOnNewTx = prefs.getBoolean("auto_sync_google_sheets_on_new_tx", false)
            )
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCloudAuthenticated(): Boolean = auth.currentUser != null

    fun observeTransactions(userId: String = requireUserId()): Flow<List<FinancialTransaction>> {
        if (localDao != null) {
            return localDao.observeAllTransactions().map { list ->
                list.map { it.toDomain() }
            }
        }
        val currentUid = auth.currentUser?.uid
        if (currentUid == null || currentUid != userId) {
            return flow {
                throw FirebaseFirestoreException(
                    "User must be authenticated and match userId: $userId",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }
        }
        return flow {
            val path = "users/$userId/transactions"
            emitAll(
                db.collection("users").document(userId).collection("transactions")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            FinancialTransaction::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).map { tx ->
                            tx.copy(title = CryptoSecurityManager.decryptText(tx.title))
                        }.sortedByDescending { it.txDateEpochMs }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeBudgets(userId: String = requireUserId()): Flow<List<CategoryBudget>> {
        if (localDao != null) {
            return localDao.observeAllBudgets().map { list ->
                list.map { it.toDomain() }
            }
        }
        val currentUid = auth.currentUser?.uid
        if (currentUid == null || currentUid != userId) {
            return flow {
                throw FirebaseFirestoreException(
                    "User must be authenticated and match userId: $userId",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }
        }
        return flow {
            val path = "users/$userId/budgets"
            emitAll(
                db.collection("users").document(userId).collection("budgets")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CategoryBudget::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeTemplates(userId: String = requireUserId()): Flow<List<QuickTemplate>> {
        if (localDao != null) {
            return localDao.observeAllTemplates().map { list ->
                list.map { it.toDomain() }
            }
        }
        return flow {
            val path = "users/$userId/templates"
            emitAll(
                db.collection("users").document(userId).collection("templates")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            QuickTemplate::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).map { tpl ->
                            tpl.copy(title = CryptoSecurityManager.decryptText(tpl.title))
                        }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    fun observeCustomCategories(): Flow<List<LocalCustomCategoryEntity>> {
        return localDao?.observeCustomCategories() ?: flow { emit(emptyList()) }
    }

    suspend fun addCustomCategory(name: String, type: String): Result<String> {
        val cleanName = name.trim().take(45)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("اسم البند لا يمكن أن يكون فارغاً"))
        }
        val normalizedType = if (type == "income") "income" else "expense"
        val key = "${normalizedType}_${cleanName}"
        return try {
            localDao?.insertCustomCategory(
                LocalCustomCategoryEntity(
                    key = key,
                    name = cleanName,
                    type = normalizedType
                )
            )
            // If it's a new expense category, also ensure a default budget exists so it shows in Budgets & Forecast
            if (normalizedType == "expense") {
                val safeBudgetId = "budget_custom_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                val effectiveUserId = auth.currentUser?.uid ?: OFFLINE_USER_ID
                val newBudget = CategoryBudget(
                    id = safeBudgetId,
                    userId = effectiveUserId,
                    category = cleanName,
                    monthlyLimit = 1500.0
                )
                localDao?.insertBudget(newBudget.toLocalEntity(effectiveUserId))
                val cloudUid = auth.currentUser?.uid
                if (cloudUid != null) {
                    val docRef = db.collection("users").document(cloudUid).collection("budgets").document(safeBudgetId)
                    try {
                        docRef.set(newBudget.toCreatePayload(cloudUid)).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.WRITE, docRef.path)
                    }
                }
            }
            Result.success(cleanName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransaction(tx: FinancialTransaction): Result<String> {
        val docId = tx.id.takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
            ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(20)}"

        // 1. Offline-first encrypted Room persistence when localDao is present
        if (localDao != null) {
            val effectiveUid = auth.currentUser?.uid ?: tx.userId.ifBlank { OFFLINE_USER_ID }
            val normalizedTx = tx.copy(
                id = docId,
                userId = effectiveUid,
                txDateEpochMs = if (tx.txDateEpochMs > 0L) tx.txDateEpochMs else System.currentTimeMillis()
            )
            localDao.insertTransaction(normalizedTx.toLocalEntity(effectiveUid))
            appContext?.let { MizanHomeWidgetProvider.updateAllWidgets(it) }

            // 2. If user opted into Cloud Sync (signed in with Google), also sync encrypted payload to Firestore
            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                val docRef = db.collection("users").document(cloudUid).collection("transactions").document(docId)
                try {
                    val encryptedCloudTx = normalizedTx.copy(
                        title = CryptoSecurityManager.encryptText(normalizedTx.title).take(120)
                    )
                    docRef.set(encryptedCloudTx.toCreatePayload(cloudUid)).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, docRef.path)
                }
            }
            return Result.success(docId)
        }

        // Direct Firestore path (used by Firestore rules tests when localDao == null)
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(docId)
        return try {
            docRef.set(tx.toCreatePayload(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(txId: String): Result<Unit> {
        if (localDao != null) {
            localDao.deleteTransactionById(txId)
            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                val docRef = db.collection("users").document(cloudUid).collection("transactions").document(txId)
                try {
                    docRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, docRef.path)
                }
            }
            return Result.success(Unit)
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTransactionById(targetUserId: String, txId: String): Result<FinancialTransaction?> {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null || currentUid != targetUserId) {
            throw FirebaseFirestoreException(
                "Cross-user access denied for user: $targetUserId",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
        val docRef = db.collection("users").document(targetUserId).collection("transactions").document(txId)
        return try {
            val snap = docRef.get().await()
            val tx = snap.toObject(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { it.copy(title = CryptoSecurityManager.decryptText(it.title)) }
            Result.success(tx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertBudget(category: String, monthlyLimit: Double, existingBudgetId: String? = null): Result<String> {
        val safeId = existingBudgetId?.takeIf { it.isNotBlank() }
            ?: "budget_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        if (localDao != null) {
            val effectiveUid = auth.currentUser?.uid ?: OFFLINE_USER_ID
            val budget = CategoryBudget(
                id = safeId,
                userId = effectiveUid,
                category = category.trim().take(60),
                monthlyLimit = monthlyLimit.coerceAtLeast(0.0)
            )
            localDao.insertBudget(budget.toLocalEntity(effectiveUid))

            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                val docRef = db.collection("users").document(cloudUid).collection("budgets").document(safeId)
                try {
                    val snap = docRef.get().await()
                    if (snap.exists()) {
                        docRef.update(
                            mapOf(
                                "category" to category.trim().take(60),
                                "monthlyLimit" to monthlyLimit.coerceAtLeast(0.0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else {
                        docRef.set(budget.toCreatePayload(cloudUid)).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, docRef.path)
                }
            }
            return Result.success(safeId)
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("budgets").document(safeId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "category" to category.trim().take(60),
                        "monthlyLimit" to monthlyLimit.coerceAtLeast(0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val budget = CategoryBudget(id = safeId, userId = uid, category = category, monthlyLimit = monthlyLimit)
                docRef.set(budget.toCreatePayload(uid)).await()
            }
            Result.success(safeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addQuickTemplate(template: QuickTemplate): Result<String> {
        val docId = template.id.takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
            ?: "tpl_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        if (localDao != null) {
            val effectiveUid = auth.currentUser?.uid ?: OFFLINE_USER_ID
            val normalizedTpl = template.copy(id = docId, userId = effectiveUid)
            localDao.insertTemplate(normalizedTpl.toLocalEntity(effectiveUid))

            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                val docRef = db.collection("users").document(cloudUid).collection("templates").document(docId)
                try {
                    val encryptedTpl = normalizedTpl.copy(
                        title = CryptoSecurityManager.encryptText(normalizedTpl.title).take(80)
                    )
                    docRef.set(encryptedTpl.toCreatePayload(cloudUid)).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, docRef.path)
                }
            }
            return Result.success(docId)
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("templates").document(docId)
        return try {
            docRef.set(template.toCreatePayload(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuickTemplate(templateId: String): Result<Unit> {
        if (localDao != null) {
            localDao.deleteTemplateById(templateId)
            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                val docRef = db.collection("users").document(cloudUid).collection("templates").document(templateId)
                try {
                    docRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, docRef.path)
                }
            }
            return Result.success(Unit)
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("templates").document(templateId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCommitments(): Flow<List<LocalCommitmentEntity>> {
        return localDao?.observeCommitments() ?: flow { emit(DefaultFinanceCatalog.buildSeedCommitments()) }
    }

    suspend fun upsertCommitment(entity: LocalCommitmentEntity) {
        localDao?.insertCommitment(entity)
    }

    suspend fun deleteCommitment(id: String) {
        localDao?.deleteCommitmentById(id)
    }

    fun observeSavingsGoals(): Flow<List<LocalSavingsGoalEntity>> {
        return localDao?.observeSavingsGoals() ?: flow { emit(DefaultFinanceCatalog.buildSeedSavingsGoals()) }
    }

    suspend fun upsertSavingsGoal(entity: LocalSavingsGoalEntity) {
        localDao?.insertSavingsGoal(entity)
    }

    suspend fun deleteSavingsGoal(id: String) {
        localDao?.deleteSavingsGoalById(id)
    }

    fun observeAssetHoldings(): Flow<List<LocalAssetHoldingEntity>> {
        return localDao?.observeAssetHoldings() ?: flow { emit(DefaultFinanceCatalog.buildSeedAssetHoldings()) }
    }

    suspend fun upsertAssetHolding(entity: LocalAssetHoldingEntity) {
        localDao?.insertAssetHolding(entity)
    }

    suspend fun deleteCustomCategory(name: String, type: String) {
        val normalizedType = if (type == "income") "income" else "expense"
        localDao?.deleteCustomCategory(name.trim(), normalizedType)
    }

    fun loadAppSettings(): AppSettings {
        val ctx = appContext ?: return AppSettings()
        return loadAppSettingsFromContext(ctx)
    }

    fun saveAppSettings(settings: AppSettings) {
        val prefs = appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE) ?: return
        prefs.edit()
            .putString("account_title", settings.accountTitle.trim().ifBlank { "حسابي المالي الشخصي" })
            .putString("currency_symbol", settings.currencySymbol.trim().ifBlank { "ج.م" })
            .putInt("salary_day_of_month", settings.salaryDayOfMonth.coerceIn(1, 28))
            .putInt("luxury_safe_threshold_pct", settings.luxurySafeThresholdPercent.coerceIn(15, 50))
            .putInt("default_spiritual_mode", settings.defaultSpiritualGivingMode.coerceIn(1, 2))
            .putBoolean("use_gross_orthodox_tithe", settings.useGrossIncomeForOrthodoxTithe)
            .putBoolean("auto_mask_privacy_on_launch", settings.autoMaskPrivacyOnLaunch)
            .putBoolean("enable_smart_budget_alerts", settings.enableSmartBudgetAlerts)
            .putString("design_preset_id", settings.designPresetId)
            .putString("design_preset_id_v3", settings.designPresetId)
            .putInt("daily_spend_mode", settings.dailySpendMode.coerceIn(0, 3))
            .putFloat("custom_daily_safe_limit", settings.customDailySafeLimit.toFloat().coerceAtLeast(0f))
            .putInt("custom_remaining_days_override", settings.customRemainingDaysOverride.coerceIn(0, 60))
            .putString("whatsapp_phone_number", settings.whatsappPhoneNumber.trim())
            .putBoolean("whatsapp_midnight_auto_enabled", settings.whatsappMidnightAutoEnabled)
            .putInt("whatsapp_report_hour", settings.whatsappReportHour.coerceIn(0, 23))
            .putInt("whatsapp_report_minute", settings.whatsappReportMinute.coerceIn(0, 59))
            .putString("whatsapp_callmebot_api_key", settings.whatsappCallMeBotApiKey.trim())
            .putString("whatsapp_webhook_url", settings.whatsappWebhookUrl.trim())
            .putString("whatsapp_meta_access_token", settings.whatsappMetaAccessToken.trim())
            .putString("whatsapp_meta_phone_number_id", settings.whatsappMetaPhoneNumberId.trim())
            .putString("whatsapp_meta_waba_id", settings.whatsappMetaWabaId.trim())
            .putBoolean("telegram_bot_enabled", settings.telegramBotEnabled)
            .putString("telegram_bot_token", settings.telegramBotToken.trim())
            .putString("telegram_chat_id", settings.telegramChatId.trim())
            .putBoolean("auto_check_updates_on_launch", settings.autoCheckUpdatesOnLaunch)
            .putString("update_manifest_url", settings.updateManifestUrl.trim())
            .putInt("published_latest_version_code", settings.publishedLatestVersionCode.coerceAtLeast(1))
            .putString("published_latest_version_name", settings.publishedLatestVersionName.trim().ifBlank { "2.0.0" })
            .putString("published_apk_download_url", settings.publishedApkDownloadUrl.trim())
            .putString("published_release_notes_ar", settings.publishedReleaseNotesAr.trim())
            .putBoolean("published_is_mandatory_update", settings.publishedIsMandatoryUpdate)
            .putBoolean("simple_minimalist_ui_mode", settings.simpleMinimalistUiMode)
            .putBoolean("auto_capture_bank_notifications", settings.autoCaptureBankNotifications)
            .putString("google_sheets_web_app_url", settings.googleSheetsWebAppUrl.trim())
            .putBoolean("auto_sync_google_sheets_on_new_tx", settings.autoSyncGoogleSheetsOnNewTx)
            .apply()
        appContext?.let { ctx ->
            WhatsAppMidnightScheduler.scheduleOrCancelDailyMidnightReport(ctx, settings)
            MizanHomeWidgetProvider.updateAllWidgets(ctx)
        }
    }

    fun isPinLockEnabled(): Boolean {
        val prefs = appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE) ?: return false
        return !prefs.getString("encrypted_app_pin", null).isNullOrBlank()
    }

    fun verifyPin(inputPin: String): Boolean {
        val prefs = appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE) ?: return true
        val storedEncrypted = prefs.getString("encrypted_app_pin", null) ?: return true
        val decrypted = CryptoSecurityManager.decryptText(storedEncrypted)
        return decrypted == inputPin.trim()
    }

    fun setOrClearPin(newPin: String?) {
        val prefs = appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE) ?: return
        if (newPin.isNullOrBlank()) {
            prefs.edit().remove("encrypted_app_pin").apply()
        } else {
            val encrypted = CryptoSecurityManager.encryptText(newPin.trim())
            prefs.edit().putString("encrypted_app_pin", encrypted).apply()
        }
    }

    suspend fun zeroAllCategoryBudgets() {
        val effectiveUid = auth.currentUser?.uid ?: OFFLINE_USER_ID
        if (localDao != null) {
            val zeroedSeed = DefaultFinanceCatalog.buildSeedBudgets().map {
                it.copy(monthlyLimit = 0.0).toLocalEntity(effectiveUid)
            }
            localDao.insertAllBudgets(zeroedSeed)
            localDao.zeroAllBudgets()
        }
    }

    suspend fun setTotalMonthlyBudgetDistribute(totalMonthlyBudget: Double) {
        val effectiveUid = auth.currentUser?.uid ?: OFFLINE_USER_ID
        val cleanTotal = totalMonthlyBudget.coerceAtLeast(0.0)
        val seedBudgets = DefaultFinanceCatalog.buildSeedBudgets()
        val seedTotal = seedBudgets.sumOf { it.monthlyLimit }.coerceAtLeast(1.0)
        val distributed = seedBudgets.map { b ->
            val share = if (cleanTotal == 0.0) 0.0 else kotlin.math.round((b.monthlyLimit / seedTotal) * cleanTotal)
            b.copy(monthlyLimit = share).toLocalEntity(effectiveUid)
        }
        localDao?.insertAllBudgets(distributed)
    }

    suspend fun clearAllTransactionsAndStartFresh() {
        appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("user_cleared_seed_data", true)
            ?.putBoolean("user_cleared_budgets_v2", true)
            ?.putFloat("custom_daily_safe_limit", 0f)
            ?.apply()

        if (localDao != null) {
            localDao.clearAllTransactions()
            localDao.clearAllCommitments()
            localDao.clearAllSavingsGoals()
            localDao.zeroAllAssetHoldings()
            val effectiveUid = auth.currentUser?.uid ?: OFFLINE_USER_ID
            val zeroedSeed = DefaultFinanceCatalog.buildSeedBudgets().map {
                it.copy(monthlyLimit = 0.0).toLocalEntity(effectiveUid)
            }
            localDao.insertAllBudgets(zeroedSeed)
            localDao.zeroAllBudgets()
        }

        val cloudUid = auth.currentUser?.uid
        if (cloudUid != null) {
            try {
                val snap = db.collection("users").document(cloudUid).collection("transactions").get().await()
                val batch = db.batch()
                snap.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$cloudUid/transactions")
            }
        }
    }

    suspend fun ensureSeedDataIfEmpty(userId: String) {
        val prefs = appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE)
        val userCleared = prefs?.getBoolean("user_cleared_seed_data", false) ?: false
        if (userCleared) {
            val budgetsZeroedV2 = prefs?.getBoolean("user_cleared_budgets_v2", false) ?: false
            if (!budgetsZeroedV2 && localDao != null && localDao.countTransactions() == 0) {
                zeroAllCategoryBudgets()
                localDao.zeroAllAssetHoldings()
                prefs?.edit()?.putBoolean("user_cleared_budgets_v2", true)?.apply()
            }
            return
        }

        if (localDao != null) {
            if (localDao.countTransactions() == 0) {
                populateRichSeedData(userId)
            } else if (localDao.countCommitments() == 0) {
                localDao.insertAllCommitments(DefaultFinanceCatalog.buildSeedCommitments())
                localDao.insertAllSavingsGoals(DefaultFinanceCatalog.buildSeedSavingsGoals())
                localDao.insertAllAssetHoldings(DefaultFinanceCatalog.buildSeedAssetHoldings())
            }
            return
        }

        val txPath = "users/$userId/transactions"
        try {
            val existingTx = db.collection("users").document(userId).collection("transactions").limit(1).get().await()
            if (existingTx.isEmpty) {
                populateRichSeedData(userId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, txPath)
        }
    }

    suspend fun populateRichSeedData(userId: String = auth.currentUser?.uid ?: OFFLINE_USER_ID) {
        appContext?.getSharedPreferences("mizan_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("user_cleared_seed_data", false)
            ?.putBoolean("user_cleared_budgets_v2", false)
            ?.apply()

        val seedBudgets = DefaultFinanceCatalog.buildSeedBudgets()
        val seedTemplates = DefaultFinanceCatalog.buildSeedTemplates()
        val seedTransactions = DefaultFinanceCatalog.buildSeedTransactions()

        if (localDao != null) {
            localDao.insertAllBudgets(seedBudgets.map { it.toLocalEntity(userId) })
            localDao.insertAllTemplates(seedTemplates.map { it.toLocalEntity(userId) })
            localDao.insertAllTransactions(seedTransactions.map { it.toLocalEntity(userId) })
            localDao.insertAllCommitments(DefaultFinanceCatalog.buildSeedCommitments())
            localDao.insertAllSavingsGoals(DefaultFinanceCatalog.buildSeedSavingsGoals())
            localDao.insertAllAssetHoldings(DefaultFinanceCatalog.buildSeedAssetHoldings())

            val cloudUid = auth.currentUser?.uid
            if (cloudUid != null) {
                syncSeedToCloud(cloudUid, seedBudgets, seedTemplates, seedTransactions)
            }
            return
        }

        syncSeedToCloud(userId, seedBudgets, seedTemplates, seedTransactions)
    }

    private suspend fun syncSeedToCloud(
        userId: String,
        seedBudgets: List<CategoryBudget>,
        seedTemplates: List<QuickTemplate>,
        seedTransactions: List<FinancialTransaction>
    ) {
        val batch = db.batch()
        val userDoc = db.collection("users").document(userId)

        seedBudgets.forEach { budget ->
            val ref = userDoc.collection("budgets").document(budget.id)
            batch.set(ref, budget.toCreatePayload(userId))
        }

        seedTemplates.forEach { tpl ->
            val ref = userDoc.collection("templates").document(tpl.id)
            val encryptedTpl = tpl.copy(title = CryptoSecurityManager.encryptText(tpl.title).take(80))
            batch.set(ref, encryptedTpl.toCreatePayload(userId))
        }

        seedTransactions.forEach { tx ->
            val ref = userDoc.collection("transactions").document(tx.id)
            val encryptedTx = tx.copy(title = CryptoSecurityManager.encryptText(tx.title).take(120))
            batch.set(ref, encryptedTx.toCreatePayload(userId))
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/seedBatch")
            if (localDao == null) throw e
        }
    }
}
