package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String, // Category name
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double, // Cost per period
    val category: String,
    val billingCycle: String, // "MONTHLY" or "ANNUALLY"
    val nextBillingDate: Long,
    val isActive: Boolean = true,
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0
)

@Entity(tableName = "quick_templates")
data class QuickTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String, // Category name
    val note: String,
    val usageCount: Int = 0,
    val currency: String = "CNY",
    val exchangeRate: Double = 1.0
)

// --- DAOs ---

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)
}

@Dao
interface QuickTemplateDao {
    @Query("SELECT * FROM quick_templates ORDER BY usageCount DESC, id ASC")
    fun getAllTemplates(): Flow<List<QuickTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuickTemplate)

    @Update
    suspend fun updateTemplate(template: QuickTemplate)

    @Delete
    suspend fun deleteTemplate(template: QuickTemplate)

    @Query("DELETE FROM quick_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
}

// --- AppDatabase ---

@Database(entities = [Transaction::class, Subscription::class, QuickTemplate::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun quickTemplateDao(): QuickTemplateDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Unified Repository Pattern ---

class LedgerRepository(private val db: AppDatabase) {
    private val transactionDao = db.transactionDao()
    private val subscriptionDao = db.subscriptionDao()
    private val quickTemplateDao = db.quickTemplateDao()

    // Transaction Queries
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    // Subscription Queries
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    suspend fun insertSubscription(subscription: Subscription) {
        subscriptionDao.insertSubscription(subscription)
    }

    suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun deleteSubscriptionById(id: Long) {
        subscriptionDao.deleteSubscriptionById(id)
    }

    // Quick Template Queries
    val allTemplates: Flow<List<QuickTemplate>> = quickTemplateDao.getAllTemplates()

    suspend fun insertTemplate(template: QuickTemplate) {
        quickTemplateDao.insertTemplate(template)
    }

    suspend fun updateTemplate(template: QuickTemplate) {
        quickTemplateDao.updateTemplate(template)
    }

    suspend fun deleteTemplate(template: QuickTemplate) {
        quickTemplateDao.deleteTemplate(template)
    }

    suspend fun deleteTemplateById(id: Long) {
        quickTemplateDao.deleteTemplateById(id)
    }
}
