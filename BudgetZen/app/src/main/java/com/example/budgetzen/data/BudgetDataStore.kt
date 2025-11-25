package com.example.budgetzen.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.budgetDataStore by preferencesDataStore("budget_prefs")

class BudgetRepository(private val context: Context) {

    companion object {
        private val BUDGET_KEY = doublePreferencesKey("monthly_budget")
    }

    val budgetFlow: Flow<Double> = context.budgetDataStore.data.map { prefs ->
        prefs[BUDGET_KEY] ?: 0.0
    }

    suspend fun saveBudget(budget: Double) {
        context.budgetDataStore.edit { prefs ->
            prefs[BUDGET_KEY] = budget
        }
    }
}
