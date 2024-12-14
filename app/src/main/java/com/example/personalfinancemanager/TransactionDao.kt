package com.example.personalfinancemanager

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>> // Change this to Flow

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 0")
    suspend fun getTotalIncome(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 1")
    suspend fun getTotalExpenses(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 1 AND category = :category")
    suspend fun getExpensesByCategory(category: String): Double?

    @Query("SELECT * FROM transactions WHERE category = :category")
    suspend fun getTransactionsByCategory(category: String): List<Transaction>
}
