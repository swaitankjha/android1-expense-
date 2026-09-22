package com.example.expenceflow.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expenceflow.data.dao.AccountDao
import com.example.expenceflow.data.dao.BudgetDao
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.dao.TransactionDao
import com.example.expenceflow.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create accounts table
            db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)")
            
            // 2. Insert default Personal account
            db.execSQL("INSERT INTO accounts (id, name, createdAt, isArchived) VALUES (1, 'Personal', ${System.currentTimeMillis()}, 0)")

            // 3. Create pending_transactions table
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `merchant` TEXT NOT NULL, `category` TEXT NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `referenceId` TEXT, `accountSuffix` TEXT, `source` TEXT NOT NULL, `status` TEXT NOT NULL, `detectedAt` INTEGER NOT NULL)")

            // 4. Add accountId to transactions
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add unique index to pending_transactions to prevent duplicates at the database level
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_transactions_amount_merchant_date` ON `pending_transactions` (`amount`, `merchant`, `date`)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext appContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "expense_flow_db"
        )
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(
        appDatabase: AppDatabase
    ): TransactionDao = appDatabase.transactionDao()

    @Provides
    @Singleton
    fun provideBudgetDao(
        appDatabase: AppDatabase
    ): BudgetDao = appDatabase.budgetDao()

    @Provides
    @Singleton
    fun provideAccountDao(
        appDatabase: AppDatabase
    ): AccountDao = appDatabase.accountDao()

    @Provides
    @Singleton
    fun providePendingTransactionDao(
        appDatabase: AppDatabase
    ): PendingTransactionDao = appDatabase.pendingTransactionDao()
}
