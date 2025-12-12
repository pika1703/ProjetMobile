package com.example.budgetzen

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetzen.chart.ExpensePieChart
import com.example.budgetzen.chart.MonthlyBarChart
import com.example.budgetzen.data.Expense
import com.example.budgetzen.data.ExpenseDatabase
import com.example.budgetzen.data.BudgetRepository
import com.example.budgetzen.ui.theme.BudgetZenTheme
import com.example.budgetzen.util.calculateMonthTotal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetZenTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // ---- Gestion de l'état de la navigation ----
    // `selectedTab` contrôle la navigation principale via la BottomNavigationBar.
    var selectedTab by remember { mutableStateOf(0) }
    // `showAllExpenses` est un état secondaire pour afficher un écran de détail
    // par-dessus l'écran d'accueil, sans changer l'onglet sélectionné.
    var showAllExpenses by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_budgetzen_logo_no_bg),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 8.dp)
                        )
                        Text("BudgetZen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD0BCFF),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!showAllExpenses) { // pas de BottomBar sur la page "Voir plus"
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
                        label = { Text("Accueil") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Ajouter") },
                        label = { Text("Ajouter") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Bilan") },
                        label = { Text("Bilan") }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Le `Box` contient le contenu principal de l'écran.
        // `innerPadding` est fourni par le Scaffold pour éviter que le contenu
        // ne soit caché par les Top/Bottom bars.
        Box(modifier = Modifier.padding(innerPadding)) {
            // ---- Logique de routage (Navigation) ----
            // Un `when` contrôle quel écran est affiché en fonction de l'état.
            when {
                showAllExpenses -> AllExpensesScreen(onBackClicked = { showAllExpenses = false })
                selectedTab == 0 -> HomeScreen(onSeeMoreClicked = { showAllExpenses = true })
                selectedTab == 1 -> AddExpenseScreen()
                selectedTab == 2 -> SummaryScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    onSeeMoreClicked: () -> Unit // callback pour le bouton "Voir plus"
) {
    val context = LocalContext.current
    val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
    val allExpenses by dao.getAllExpenses().collectAsState(initial = emptyList())
    val lastExpenses by dao.getLastExpenses(3).collectAsState(initial = emptyList())

    // Calcul du total du mois actuel
    val monthTotal = calculateMonthTotal(allExpenses)

    val budgetRepo = remember { BudgetRepository(context) }
    val budget by budgetRepo.budgetFlow.collectAsState(initial = 0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Budget : pour afficher et modifier le budget
        HomeBudgetSection(
            monthTotal = monthTotal,
            budgetValue = budget,
            onBudgetChanged = { newBudget ->
                CoroutineScope(Dispatchers.IO).launch {
                    budgetRepo.saveBudget(newBudget)
                }
            }
        )

        Text(
            "Dernières dépenses",
            style = MaterialTheme.typography.headlineSmall
        )

        if (lastExpenses.isEmpty()) {
            Text("Aucune dépense enregistrée pour le moment.")
        } else {
            // Affichage des dernières dépenses dans des cartes.
            lastExpenses.forEach { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = expense.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Text(
                                text = " - ${expense.amount}€",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            "${expense.category} • ${expense.date.toDisplayDate()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        // Le bouton "Voir plus" déclenche le callback qui changera l'état `showAllExpenses`.
        Button(
            onClick = onSeeMoreClicked,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Voir plus")
        }

        // Graphique circulaire du mois
        ExpensePieChart()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen() {
    // ---- Ecran pour l'ajout de dépense ----
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") } // Date au format YYYY-MM-DD pour la BDD
    var displayDate by remember { mutableStateOf("") } // Date au format DD/MM/YYYY pour l'affichage
    var expanded by remember { mutableStateOf(false) } // État pour le menu déroulant
    var showDatePicker by remember { mutableStateOf(false) } // État pour afficher/cacher le DatePickerDialog

    val categories = listOf("Alimentation", "Transport","Logement", "Loisirs", "Autres")

    val datePickerState = rememberDatePickerState()

    val context = LocalContext.current
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val dao = db.expenseDao()
    val scope = rememberCoroutineScope()

    // `SnackbarHostState` pour afficher des messages temporaires (ex: "Dépense enregistrée !").
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ajouter une dépense", style = MaterialTheme.typography.headlineSmall)

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                // Formattage de la date pour la base de données et pour l'affichage.
                                val formatter = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                )
                                selectedDate = formatter.format(Date(millis))

                                val frFormatter = SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                )
                                displayDate = frFormatter.format(Date(millis))
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Annuler")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

                // Champ Montant
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) amount = newValue
                    },
                    label = { Text("Montant (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("€") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                // Catégorie : Menu déroulant
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Nom de la dépense
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la dépense") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Sélecteur de date
                ClickableDateField(
                    value = displayDate,
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                // Bouton Enregistrer
                Button(
                    onClick = {
                        if (amount.isNotBlank() && selectedDate.isNotBlank()) {
                            // Cas du champ nom non rempli
                            val finalName = if (name.isBlank()) "Dépense" else name

                            val expense = Expense(
                                amount = amount.toDouble(),
                                category = category,
                                name = finalName,
                                date = selectedDate
                            )
                            scope.launch() {
                                dao.insertExpense(expense)
                                launch {
                                    snackbarHostState.showSnackbar("Dépense enregistrée !")
                                }
                                // Réinitialiser les champs
                                amount = ""
                                name = ""
                                selectedDate = ""
                                displayDate = ""
                                category = "Alimentation"
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Merci de remplir tous les champs obligatoires.")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Enregistrer")
                }
            }
        }
    }


@Composable
fun SummaryScreen() {
    val context = LocalContext.current
    val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
    val allExpenses by dao.getAllExpenses().collectAsState(initial = emptyList())

    val totalSpent = allExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TotalSpentCard(total = totalSpent)

        Text(
            "Dépenses les 6 derniers mois",
            style = MaterialTheme.typography.headlineSmall
        )

        MonthlyBarChart()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
    val allExpenses by dao.getAllExpenses().collectAsState(initial = emptyList())

    // États pour suppression et édition
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toutes les dépenses") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (allExpenses.isEmpty()) {
                Text("Aucune dépense enregistrée.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allExpenses, key = { it.id }) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nom
                                        Text(
                                            text = expense.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        // Montant
                                        Text(
                                            text = " - ${expense.amount}€",
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        "${expense.category} • ${expense.date.toDisplayDate()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row {
                                    // Bouton modifier
                                    IconButton(onClick = { expenseToEdit = expense }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Modifier",
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                    // Bouton supprimer
                                    IconButton(onClick = { expenseToDelete = expense }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue de suppression
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.deleteExpense(expenseToDelete!!)
                        launch{
                            snackbarHostState.showSnackbar("Dépense supprimée")
                        }
                        expenseToDelete = null
                    }
                }) { Text("Supprimer", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Annuler") }
            },
            title = { Text("Supprimer la dépense") },
            text = { Text("Voulez-vous vraiment supprimer cette dépense ?") }
        )
    }

    // Dialogue d’édition
    if (expenseToEdit != null) {
        EditExpenseDialog(
            expense = expenseToEdit!!,
            onDismiss = { expenseToEdit = null },
            onSave = { updated ->
                scope.launch {
                    dao.updateExpense(updated)
                    launch{
                        snackbarHostState.showSnackbar("Dépense modifiée")
                    }
                    expenseToEdit = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var category by remember { mutableStateOf(expense.category) }
    var name by remember { mutableStateOf(expense.name) }
    var date by remember { mutableStateOf(expense.date) }
    var displayDate by remember { mutableStateOf(expense.date.toDisplayDate())}
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = listOf("Alimentation", "Transport", "Logement","Loisirs", "Autres")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && amount.isNotBlank() && date.isNotBlank()) {
                    onSave(expense.copy(
                        name = name,
                        amount = amount.toDouble(),
                        category = category,
                        date = date
                    ))
                }
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        title = { Text("Modifier la dépense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Catégorie
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    category = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Sélecteur de date
                ClickableDateField(
                    value = displayDate,
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                /*
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_today),
                                contentDescription = "Choisir une date"
                            )
                        }
                    }
                )*/
                if (showDatePicker) {
                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val month = (m + 1).toString().padStart(2, '0')
                            val day = d.toString().padStart(2, '0')
                            date = "$y-$month-$day"
                            displayDate = "$day/$month/$y"
                        },
                        year, month, day
                    ).show()
                    showDatePicker = false
                }
            }
        }
    )
}

fun String.toDisplayDate(): String {
    return try {
        val parts = this.split("-") // yyyy-MM-dd
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]
            "$day/$month/$year"
        } else this
    } catch (_: Exception) {
        this
    }
}

@Composable
fun HomeBudgetSection(
    monthTotal: Double, // somme dépenses du mois
    onBudgetChanged: (Double) -> Unit,
    budgetValue: Double
) {
    Column(Modifier.padding(8.dp)) {

        Text(
            "Budget du mois",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        var text by remember { mutableStateOf("") }

        LaunchedEffect(budgetValue) {
            if (budgetValue != 0.0) {
                text = budgetValue.toString()
            }
        }

        TextField(
            value = text,
            onValueChange = {
                text = it
                it.toDoubleOrNull()?.let { value ->
                    onBudgetChanged(value)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            placeholder = { Text("Entrez votre budget (€)") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        val remaining = budgetValue - monthTotal

        Text(
            "Solde restant : ${"%.2f".format(remaining)} €",
            style = MaterialTheme.typography.titleMedium,
            color = if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
        )
    }
}

@Composable
fun TotalSpentCard(
    total: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F0FF)   // bleu clair
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // Icône
            Icon(
                painter = painterResource(id = R.drawable.ic_money),
                contentDescription = "Total dépensé",
                tint = Color(0xFF1A73E8),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Texte
            Column {
                Text(
                    text = "Total dépensé",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "%.2f €".format(total),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A73E8)
                )
            }
        }
    }
}

@Composable
fun ClickableDateField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayValue = value.ifBlank { "Date" }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayValue,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )

            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_today),
                contentDescription = "Choisir une date"
            )
        }
    }
}