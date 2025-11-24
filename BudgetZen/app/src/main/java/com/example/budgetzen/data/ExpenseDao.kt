package com.example.budgetzen.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // Récupérer les dernières dépenses selon la valeur de limit (pour la page d'accueil)
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    fun getLastExpenses(limit: Int): Flow<List<Expense>>

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

}

