package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String, // "Food", "Entertainment", "Transport", "Shopping", "Housing", "Salary", "Bonus", "Others"
    val timestamp: Long,
    val notes: String = "",
    val tags: String = "",
    val account: String = "默认账户",
    val isReimbursable: Boolean = false,
    val reimbursementStatus: String = "NONE" // "NONE", "PENDING", "REIMBURSED"
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val cycle: String, // "MONTHLY" or "YEARLY"
    val category: String,
    val nextBillingDate: Long,
    val notes: String = ""
)

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Int)
}

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isExpense: Boolean = true
)

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY id DESC")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(customCategory: CustomCategory)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCustomCategory(id: Int)
}

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val id: String, // "total" or "category_[categoryId]"
    val amount: Double
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)
}

@Database(entities = [Transaction::class, Subscription::class, CustomCategory::class, Budget::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val subscriptionDao: SubscriptionDao,
    private val customCategoryDao: CustomCategoryDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    val allCustomCategories: Flow<List<CustomCategory>> = customCategoryDao.getAllCustomCategories()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insertSubscription(subscription: Subscription) {
        subscriptionDao.insertSubscription(subscription)
    }

    suspend fun deleteSubscriptionById(id: Int) {
        subscriptionDao.deleteSubscriptionById(id)
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun getCount(): Int {
        return transactionDao.getCount()
    }

    suspend fun insertCustomCategory(customCategory: CustomCategory) {
        customCategoryDao.insertCustomCategory(customCategory)
    }

    suspend fun deleteCustomCategoryById(id: Int) {
        customCategoryDao.deleteCustomCategory(id)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(id: String) {
        budgetDao.deleteBudget(id)
    }
}
