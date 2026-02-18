package com.example.expenceflow.di

import android.content.Context
import androidx.room.Room
import com.example.expenceflow.data.dao.BudgetDao
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
            .fallbackToDestructiveMigration()
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
}
