package com.example.budgetzen.util

import com.example.budgetzen.data.Expense
import java.lang.Exception
import java.util.*

// Calcule la somme des dépenses d'une liste pour le mois actuel
fun calculateMonthTotal(expenses: List<Expense>, referenceCalendar: Calendar = Calendar.getInstance()): Double {
    val currentMonth = referenceCalendar.get(Calendar.MONTH) + 1
    val currentYear = referenceCalendar.get(Calendar.YEAR)

    return expenses.asSequence().mapNotNull { expense ->
        try {
            // On attend le format ISO yyyy-MM-dd dans la BDD
            val parts = expense.date.split("-")
            if (parts.size != 3) return@mapNotNull null
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            if (year == currentYear && month == currentMonth) expense.amount else null
        } catch (e: Exception) {
            null
        }
    }.sum()
}
