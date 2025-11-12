package com.example.budgetzen.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.example.budgetzen.data.ExpenseDatabase
import java.util.Calendar
@Composable
fun ExpensePieChart() {
    val context = LocalContext.current
    val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
    val allExpenses by dao.getAllExpenses().collectAsState(initial = emptyList())

    // Obtenir le mois et l’année actuels
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // Filtrer les dépenses du mois en cours
    val monthExpenses = allExpenses.filter {
        try {
            val parts = it.date.split("/") // Format: "jour/mois/année"
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            month == currentMonth && year == currentYear
        } catch (_: Exception) {
            false
        }
    }

    // Regrouper par catégorie
    val grouped = monthExpenses.groupBy { it.category }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    // Génération des couleurs automatiques
    val colors = listOf(
        Color(0xFF4CAF50), // Vert - Alimentation
        Color(0xFFFF9800), // Orange - Transport
        Color(0xFF2196F3), // Bleu - Logement
        Color(0xFFE91E63), // Rose - Loisirs
        Color(0xFF9C27B0), // Violet - Autres
    )

    // Préparer les slices
    val slices = grouped.entries.mapIndexed { index, (category, total) ->
        PieChartData.Slice(
            label = category,
            value = total.toFloat(),
            color = colors[index % colors.size],
            sliceDescription = { "$category : ${"%.2f".format(total)}€" }
        )
    }


    // Si aucune dépense, on affiche un message
    if (slices.isEmpty()) {
        Text(
            "Aucune dépense enregistrée ce mois-ci.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Configuration du DonutChart
    val pieChartData = PieChartData(
        slices = slices,
        plotType = PlotType.Donut
    )
    val pieChartConfig = PieChartConfig(
        showSliceLabels = true,
        isAnimationEnable = true,
        labelColor = Color.Black,
        activeSliceAlpha = 0.8f,
        strokeWidth = 100.0f,
        backgroundColor = Color.Transparent,
        labelFontSize = 12.sp,
    )

    // Affichage
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Répartition des dépenses du mois ${currentMonth}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ){
            PieChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f),
                pieChartData = pieChartData,
                pieChartConfig = pieChartConfig
            )

            // Texte centré au milieu du donut
            val totalMonth = monthExpenses.sumOf { it.amount }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "${"%.2f".format(totalMonth)}€",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        slices.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(slice.color, shape = RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Texte de la catégorie
                Text(
                    text = "${slice.label} : ${"%.2f".format(slice.value)}€",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}