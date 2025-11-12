package com.example.budgetzen

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.budgetzen.chart.ExpensePieChart
import com.example.budgetzen.data.Expense
import com.example.budgetzen.data.ExpenseDatabase
import com.example.budgetzen.ui.theme.BudgetZenTheme
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
    var selectedTab by remember { mutableStateOf(0) }
    var showAllExpenses by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_budgetzen_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp)
                        )
                        Text("BudgetZen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
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
        Box(modifier = Modifier.padding(innerPadding)) {
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
    val lastExpenses by dao.getLastExpenses(3).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Dernières dépenses",
            style = MaterialTheme.typography.headlineSmall
        )

        if (lastExpenses.isEmpty()) {
            Text("Aucune dépense enregistrée pour le moment.")
        } else {
            lastExpenses.forEach { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${expense.name} - ${expense.amount}€",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${expense.category} • ${expense.date}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSeeMoreClicked,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Voir plus")
        }

        Spacer(modifier = Modifier.height(24.dp))


        ExpensePieChart()

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen() {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = listOf("Alimentation", "Transport","Logement", "Loisirs", "Autres")

    val datePickerState = rememberDatePickerState()

    val context = LocalContext.current
    val db = remember { ExpenseDatabase.getDatabase(context) }
    val dao = db.expenseDao()
    val scope = rememberCoroutineScope()

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
                                val formatter = SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                )
                                selectedDate = formatter.format(Date(millis))
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
/*
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajouter une dépense",
                    style = MaterialTheme.typography.headlineSmall
                )*/

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

                // Catégorie
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
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true,

                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_today),
                                contentDescription = "Choisir une date"
                            )
                        }
                    }
                )


                Button(
                    onClick = {
                        if (amount.isNotBlank() && name.isNotBlank() && selectedDate.isNotBlank()) {
                            val expense = Expense(
                                amount = amount.toDouble(),
                                category = category,
                                name = name,
                                date = selectedDate
                            )
                            scope.launch {
                                dao.insertExpense(expense)
                                snackbarHostState.showSnackbar("Dépense enregistrée !")
                                // Réinitialiser les champs
                                amount = ""
                                name = ""
                                selectedDate = ""
                                category = "Alimentation"
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Merci de remplir tous les champs.")
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
    ExpensePieChart()
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
                                Column {
                                    Text(
                                        "${expense.name} - ${expense.amount}€",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${expense.category} • ${expense.date}",
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
                        snackbarHostState.showSnackbar("🗑️ Dépense supprimée")
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
                    snackbarHostState.showSnackbar("Dépense modifiée")
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Montant (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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
                // Date
                OutlinedTextField(
                    value = date,
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
                )
                if (showDatePicker) {
                    val context = LocalContext.current
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = "$d/${m + 1}/$y" },
                        year, month, day
                    ).show()
                    showDatePicker = false
                }
            }
        }
    )
}

