package com.example.budgetzen.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.*
import com.example.budgetzen.data.ExpenseDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun MonthlyBarChart() {
    val context = LocalContext.current
    val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
    val allExpenses by dao.getAllExpenses().collectAsState(initial = emptyList())

    // Formatter pour les dates ISO dans la BDD
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 12 derniers mois (du plus ancien au plus récent)
    val now = LocalDate.now()
    val last12 = (5 downTo 0).map { now.minusMonths(it.toLong()) }

    // Calcul total par mois
    val monthTotals = last12.map { date ->
        val year = date.year
        val month = date.monthValue
        val total = allExpenses
            .mapNotNull {
                try {
                    val parsed = LocalDate.parse(it.date, formatter)
                    if (parsed.year == year && parsed.monthValue == month) it.amount else 0.0
                } catch (_: Exception) {
                    0.0
                }
            }
            .sum()
        date to total
    }

    // couleurs pour les barres
    val colors = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFE91E63),
        Color(0xFF9C27B0)
    )

    // Construire la liste de BarData
    val barChartList: List<BarData> = monthTotals.mapIndexed { index, (date, total) ->
        BarData(
            point = Point(index.toFloat(), total.toFloat(), description = "test"),         // axe (x=index, y=value)
            label = monthToShortName(date.monthValue),               // étiquette X
            color = colors[index % colors.size],                     // couleur de la barre
            //gradientColorList = emptyList(),                         // optionnel : gradient
            description = "${monthToShortName(date.monthValue)} : ${"%.2f".format(total)}€"
        )
    }

    // si toutes les barres valent 0 -> message
    if (barChartList.all { it.point.y == 0f }) {
        Text(
            "Pas encore de dépenses sur les 12 derniers mois.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Config X axis
    val xAxis = AxisData.Builder()
        .steps(barChartList.size - 1)
        .startPadding(8.dp)
        .endPadding(8.dp)
        //.axisStepSize(8.dp)
        .bottomPadding(20.dp)
        .startDrawPadding(16.dp)
        .axisLabelAngle(0f)
        .labelData { index -> barChartList[index].label ?: "" }
        .build()

    // Trouver le max des valeurs
    val maxValue = barChartList.maxOfOrNull { it.point.y } ?: 0f
    roundToNice(maxValue) // on arrondit pour l'échelle max de l'axe Y du graphe

    // Step Y agréable
    val yStep = computeNiceStep(maxValue)

    // Nombre de graduations
    var steps = (maxValue / yStep).toInt().coerceAtLeast(1)

    // Config Y axis
    val yAxis = AxisData.Builder()
        .steps(5)
        .startPadding(8.dp)
        .endPadding(8.dp)
        .labelData { valueIndex ->
            val y = valueIndex * (maxValue/5)
            "%.0f".format(y)
        }
        .build()

    // Construire BarChartData
    val barChartData = BarChartData(
        chartData = barChartList,
        xAxisData = xAxis,
        yAxisData = yAxis,
        //horizontalExtraSpace = 32.dp,
        //paddingEnd = 16.dp,
        //paddingTop = 12.dp,
        //tapPadding = 12.dp,
        showXAxis = true,
        showYAxis = true,
        backgroundColor = Color.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(horizontal = 0.dp),
            //.padding(start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BarChart(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, end = 4.dp),
            barChartData = barChartData,
        )
    }
}

fun monthToShortName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Fév"
        3 -> "Mar"
        4 -> "Avr"
        5 -> "Mai"
        6 -> "Juin"
        7 -> "Juil"
        8 -> "Août"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Déc"
        else -> "?"
    }
}

// Calcul d'un step automatiquement
fun computeNiceStep(max: Float): Float {
    if (max <= 10) return 2f
    if (max <= 50) return 5f
    if (max <= 100) return 10f
    if (max <= 200) return 20f
    if (max <= 500) return 50f
    if (max <= 1000) return 100f
    if (max <= 2000) return 200f
    return max / 10f
}

fun roundToNice(max: Float): Float {
    if (max <= 0f) return 10f

    // Trouver 10^n (puissance de 10 inférieure)
    val magnitude = 10f.pow(floor(log10(max)))

    // Diviser par magnitude pour ramener dans une zone simple
    val normalized = max / magnitude

    // Arrondir au multiple le plus proche : 1 décimale
    val rounded = (normalized * 10).roundToInt() / 10f

    // Reconstituer l'échelle
    return rounded * magnitude
}
