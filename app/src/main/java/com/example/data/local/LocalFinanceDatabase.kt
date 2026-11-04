package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.CategoryBudget
import com.example.data.CryptoSecurityManager
import com.example.data.FinancialTransaction
import com.example.data.QuickTemplate
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val encryptedTitle: String,
    val amount: Double,
    val type: String, // "income" | "expense" | "transfer"
    val category: String,
    val priority: String, // "essential" | "luxury"
    val recurrence: String, // "fixed" | "variable" | target wallet when type == "transfer"
    val wallet: String, // "cash" | "bank" | "ewallet"
    val txDateEpochMs: Long
) {
    fun toDomain(userId: String = "offline_local_user"): FinancialTransaction = FinancialTransaction(
        id = id,
        userId = userId,
        title = CryptoSecurityManager.decryptText(encryptedTitle),
        amount = amount,
        type = type,
        category = category,
        priority = priority,
        recurrence = recurrence,
        wallet = wallet,
        txDateEpochMs = txDateEpochMs
    )

    companion object {
        fun fromDomain(tx: FinancialTransaction, ensuredId: String): LocalTransactionEntity =
            LocalTransactionEntity(
                id = ensuredId,
                encryptedTitle = CryptoSecurityManager.encryptText(tx.title),
                amount = tx.amount,
                type = tx.type,
                category = tx.category,
                priority = tx.priority,
                recurrence = tx.recurrence,
                wallet = tx.wallet,
                txDateEpochMs = if (tx.txDateEpochMs > 0L) tx.txDateEpochMs else System.currentTimeMillis()
            )
    }
}

@Entity(tableName = "local_budgets")
data class LocalBudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val monthlyLimit: Double
) {
    fun toDomain(userId: String = "offline_local_user"): CategoryBudget = CategoryBudget(
        id = id,
        userId = userId,
        category = category,
        monthlyLimit = monthlyLimit
    )
}

@Entity(tableName = "local_templates")
data class LocalTemplateEntity(
    @PrimaryKey val id: String,
    val encryptedTitle: String,
    val amount: Double,
    val category: String,
    val priority: String,
    val recurrence: String,
    val wallet: String,
    val iconKey: String
) {
    fun toDomain(userId: String = "offline_local_user"): QuickTemplate = QuickTemplate(
        id = id,
        userId = userId,
        title = CryptoSecurityManager.decryptText(encryptedTitle),
        amount = amount,
        category = category,
        priority = priority,
        recurrence = recurrence,
        wallet = wallet,
        iconKey = iconKey
    )
}

@Entity(tableName = "local_custom_categories")
data class LocalCustomCategoryEntity(
    @PrimaryKey val key: String, // "${type}_${name}"
    val name: String,
    val type: String, // "expense" | "income"
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Represents ROSCAs (الجمعيات الشهرية), Installments (الأقساط), and Debts (الديون لي / عليّ).
 */
@Entity(tableName = "local_commitments")
data class LocalCommitmentEntity(
    @PrimaryKey val id: String,
    val encryptedTitle: String,
    val type: String, // "rosca" (جمعية), "installment" (قسط), "debt_owed" (دين عليّ), "debt_receivable" (دين لي)
    val totalAmount: Double,
    val monthlyInstallment: Double,
    val paidInstallmentsCount: Int,
    val totalInstallmentsCount: Int,
    val dueDayOfMonth: Int, // 1..28
    val payoutMonthIndex: Int = 1, // For ROSCA: which month you collect the pot
    val wallet: String = "bank"
) {
    val decryptedTitle: String
        get() = CryptoSecurityManager.decryptText(encryptedTitle)
}

/**
 * Represents Smart Savings Goals (الحصالة الذكية وأهداف الادخار).
 */
@Entity(tableName = "local_savings_goals")
data class LocalSavingsGoalEntity(
    @PrimaryKey val id: String,
    val encryptedTitle: String,
    val targetAmount: Double,
    val currentSavedAmount: Double,
    val categoryIcon: String = "savings"
) {
    val decryptedTitle: String
        get() = CryptoSecurityManager.decryptText(encryptedTitle)
}

/**
 * Represents Multi-Currency & Gold Holdings (تعدد العملات والذهب).
 */
@Entity(tableName = "local_asset_holdings")
data class LocalAssetHoldingEntity(
    @PrimaryKey val assetCode: String, // "USD", "SAR", "EUR", "GOLD_21", "GOLD_24"
    val labelAr: String,
    val unitAr: String,
    val quantity: Double,
    val unitRateInEgp: Double
)

fun FinancialTransaction.toLocalEntity(userId: String = "offline_local_user"): LocalTransactionEntity =
    LocalTransactionEntity.fromDomain(this, this.id.ifBlank { "tx_${System.currentTimeMillis()}" })

fun CategoryBudget.toLocalEntity(userId: String = "offline_local_user"): LocalBudgetEntity =
    LocalBudgetEntity(
        id = this.id.ifBlank { "budget_${this.category.hashCode()}" },
        category = this.category,
        monthlyLimit = this.monthlyLimit
    )

fun QuickTemplate.toLocalEntity(userId: String = "offline_local_user"): LocalTemplateEntity =
    LocalTemplateEntity(
        id = this.id.ifBlank { "tpl_${this.title.hashCode()}" },
        encryptedTitle = CryptoSecurityManager.encryptText(this.title),
        amount = this.amount,
        category = this.category,
        priority = this.priority,
        recurrence = this.recurrence,
        wallet = this.wallet,
        iconKey = this.iconKey
    )

@Dao
interface LocalFinanceDao {

    @Query("SELECT * FROM local_transactions ORDER BY txDateEpochMs DESC")
    fun observeAllTransactions(): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM local_transactions ORDER BY txDateEpochMs DESC")
    suspend fun getAllTransactionsSnapshot(): List<LocalTransactionEntity>

    @Query("SELECT COUNT(*) FROM local_transactions")
    suspend fun countTransactions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(entities: List<LocalTransactionEntity>)

    @Query("DELETE FROM local_transactions WHERE id = :txId")
    suspend fun deleteTransactionById(txId: String)

    @Query("DELETE FROM local_transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM local_budgets")
    fun observeAllBudgets(): Flow<List<LocalBudgetEntity>>

    @Query("SELECT * FROM local_budgets")
    suspend fun getAllBudgetsSnapshot(): List<LocalBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(entity: LocalBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBudgets(entities: List<LocalBudgetEntity>)

    @Query("SELECT * FROM local_templates")
    fun observeAllTemplates(): Flow<List<LocalTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(entity: LocalTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTemplates(entities: List<LocalTemplateEntity>)

    @Query("DELETE FROM local_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    @Query("SELECT * FROM local_custom_categories ORDER BY createdAtEpochMs ASC")
    fun observeCustomCategories(): Flow<List<LocalCustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(entity: LocalCustomCategoryEntity)

    @Query("DELETE FROM local_custom_categories WHERE name = :name AND type = :type")
    suspend fun deleteCustomCategory(name: String, type: String)

    // Commitments (ROSCAs, Installments, Debts)
    @Query("SELECT * FROM local_commitments ORDER BY dueDayOfMonth ASC")
    fun observeCommitments(): Flow<List<LocalCommitmentEntity>>

    @Query("SELECT COUNT(*) FROM local_commitments")
    suspend fun countCommitments(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(entity: LocalCommitmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommitments(entities: List<LocalCommitmentEntity>)

    @Query("DELETE FROM local_commitments WHERE id = :id")
    suspend fun deleteCommitmentById(id: String)

    @Query("DELETE FROM local_commitments")
    suspend fun clearAllCommitments()

    // Savings Goals
    @Query("SELECT * FROM local_savings_goals")
    fun observeSavingsGoals(): Flow<List<LocalSavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(entity: LocalSavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSavingsGoals(entities: List<LocalSavingsGoalEntity>)

    @Query("DELETE FROM local_savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: String)

    @Query("DELETE FROM local_savings_goals")
    suspend fun clearAllSavingsGoals()

    // Multi-Currency & Gold Holdings
    @Query("SELECT * FROM local_asset_holdings")
    fun observeAssetHoldings(): Flow<List<LocalAssetHoldingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetHolding(entity: LocalAssetHoldingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAssetHoldings(entities: List<LocalAssetHoldingEntity>)

    @Query("UPDATE local_budgets SET monthlyLimit = 0.0")
    suspend fun zeroAllBudgets()

    @Query("UPDATE local_asset_holdings SET quantity = 0.0")
    suspend fun zeroAllAssetHoldings()
}

@Database(
    entities = [
        LocalTransactionEntity::class,
        LocalBudgetEntity::class,
        LocalTemplateEntity::class,
        LocalCustomCategoryEntity::class,
        LocalCommitmentEntity::class,
        LocalSavingsGoalEntity::class,
        LocalAssetHoldingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LocalFinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): LocalFinanceDao

    companion object {
        @Volatile
        private var INSTANCE: LocalFinanceDatabase? = null

        fun getInstance(context: Context): LocalFinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalFinanceDatabase::class.java,
                    "mizan_offline_encrypted.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
