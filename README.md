# ProjetMobile
Groupe 4 : Romain LE SOURD, Aboubacar DIABY

# BudgetZen - Gestionnaire de Dépenses

BudgetZen est une application Android native développée en Kotlin avec Jetpack Compose. Elle permet aux utilisateurs de suivre leurs dépenses, de les catégoriser et de visualiser des résumés mensuels et annuels pour mieux maîtriser leur budget.

## Structure du Projet

Le projet s'articule autour d'une architecture simple et moderne, utilisant une Activity unique (```MainActivity```) qui gère la navigation et l'affichage des différents écrans (```Composable```).

- **MainActivity.kt**: C'est le cœur de l'interface utilisateur.
  - **MainScreen**: Le composant principal qui utilise un ```Scaffold``` pour organiser l'écran avec une ```TopAppBar``` et une ```NavigationBar```. Il gère la navigation entre les trois onglets principaux.
  - **Navigation par État (State-Driven)**: La navigation n'utilise pas un ```NavController``` complexe. Elle est pilotée par un état ```remember``` (```selectedTab```) qui détermine quel écran afficher (```HomeScreen```, ```AddExpenseScreen```, ou ```SummaryScreen```). Une seconde variable d'état (```showAllExpenses```) permet d'afficher l'écran de détail des dépenses (```AllExpensesScreen```) par-dessus l'écran principal, une approche simple et efficace.
  - **HomeScreen**: L'écran d'accueil. Il affiche un résumé du budget, la liste des dernières dépenses, et un graphique circulaire (```ExpensePieChart```) de la répartition des dépenses.
  - **AddExpenseScreen**: L'écran d'ajout de dépense, construit avec des composants Material 3 permet d'ajouter une dépense avec un montant, une catégorie, un nom (facultatif) et une date.
  - **SummaryScreen**: L'écran de bilan, qui contient le graphique à barres des dépenses mensuelles (```MonthlyBarChart```) et le total des dépenses.
  - **AllExpensesScreen**: Un écran qui affiche la liste complète de toutes les dépenses, avec des options de modification et de suppression.

- **com.example.budgetzen.data**: Ce package gère toute la logique de persistance des données.
  - **Expense.kt**: La classe de données (```@Entity```) qui définit la structure d'une dépense dans la base de données Room.
  - **ExpenseDao.kt**: L'interface d'accès aux données (DAO). Elle définit les méthodes pour interagir avec la base de données (insérer, récupérer, supprimer) en utilisant des ```Flow``` pour rendre l'interface utilisateur réactive.
  - **ExpenseDatabase.kt**: La classe de base de données Room qui hérite de ```RoomDatabase``` et implémente un pattern Singleton pour garantir une seule instance dans l'application.
  - **BudgetDataStore.kt**: Un référentiel qui gère la persistance du budget mensuel de l'utilisateur. Il utilise Jetpack DataStore pour stocker cette valeur de manière asynchrone et sécurisée.

- **com.example.budgetzen.chart**: Ce package contient les composants graphiques personnalisés.
  - **ExpensePieChart.kt**: Un Composable qui affiche un graphique circulaire (camembert) représentant la répartition des dépenses par catégorie pour le mois en cours. Il utilise la bibliothèque YCharts.
  - **MonthlyBarChart.kt**: Affiche un graphique à barres montrant le total des dépenses pour chaque mois des 6 derniers mois. Il utilise aussi la bibliothèque YCharts.

## Points Clés et Parties de Code Intéressantes
### 1. Navigation Déclarative Simple avec l'État de Compose
La navigation de l'application a été fait de manière simple. Plutôt que d'intégrer une bibliothèque de navigation complète, elle utilise l'état natif de Jetpack Compose.
```Kotlin
// Dans MainScreen()
var selectedTab by remember { mutableStateOf(0) }
var showAllExpenses by remember { mutableStateOf(false) }

// ...
Box(modifier = Modifier.padding(innerPadding)) {
    when {
        showAllExpenses -> AllExpensesScreen(onBackClicked = { showAllExpenses = false })
        selectedTab == 0 -> HomeScreen(onSeeMoreClicked = { showAllExpenses = true })
        selectedTab == 1 -> AddExpenseScreen()
        selectedTab == 2 -> SummaryScreen()
    }
}
```
### 2. Données Réactives avec Room et collectAsState
L'application suit les meilleures pratiques Android pour l'accès aux données. Le ExpenseDao expose les listes de dépenses via des Flow, et les Composable s'abonnent à ces flux.
```Kotlin
// Dans HomeScreen()
val dao = remember { ExpenseDatabase.getDatabase(context).expenseDao() }
val lastExpenses by dao.getLastExpenses(3).collectAsState(initial = emptyList())
```
Grâce à collectAsState, dès qu'une dépense est ajoutée, modifiée ou supprimée, la variable lastExpenses est automatiquement mise à jour. Jetpack Compose détecte ce changement et recompose l'interface utilisateur sans aucune intervention manuelle. C'est le pilier de la réactivité de l'application.

### 3. Gestion du Budget avec DataStore
Pour stocker le budget mensuel, le projet utilise Jetpack DataStore via la classe BudgetRepository.
```Kotlin
// Dans BudgetRepository.kt
val budgetFlow: Flow<Double> = context.dataStore.data
    .map { preferences ->
        preferences[BUDGET_KEY] ?: 0.0
    }
```
```DataStore``` est la solution moderne recommandée par Google pour remplacer les ```SharedPreferences```. Il est asynchrone (ne bloque pas le thread UI), gère les erreurs de manière robuste et s'intègre parfaitement avec les ```Flow``` de Kotlin, permettant une mise à jour transparente de l'interface lorsque le budget est modifié.

### 4. Graphiques Personnalisés avec YCharts
Pour offrir une représentation visuelle des données financières, l'application utilise la bibliothèque tierce ```co.yml:ycharts```. Cette section détaille l'implémentation des deux graphiques principaux : le graphique circulaire (```ExpensePieChart```) et le graphique à barres (```MonthlyBarChart```).

#### ExpensePieChart : Répartition des Dépenses par Catégorie
Ce composant offre un aperçu immédiat de la manière dont les dépenses du mois en cours sont réparties entre les différentes catégories.
Logique de fonctionnement :
- 1. **Collecte des Données** : Le Composable récupère la liste complète des dépenses depuis la base de données via un Flow Room.
- 2. **Filtrage et Groupement** : Il filtre les dépenses pour ne conserver que celles du mois actuel. Ensuite, il groupe ces dépenses par catégorie (```groupBy { it.category }```).
- 3. **Calcul des Totaux** : Pour chaque catégorie, il calcule le montant total des dépenses (```sumOf { it.amount }```).
- 4. **Transformation pour YCharts** : Les données calculées (catégorie et total) sont transformées en une liste de ```PieChartData.Slice```, qui est le modèle de données attendu par YCharts. Chaque Slice a une valeur, une couleur et une étiquette.
- 5. **Configuration du Graphique**: Le ```Composable``` ```PieChart``` est configuré pour définir son style (par exemple, ```PieChartType.Donut```) et la manière dont les données sont affichées.
```Kotlin
// Dans ExpensePieChart.kt

// 2. Filtrer et grouper les données
val categoryTotals = allExpenses
    .filter { /* Dépenses du mois en cours */ }
    .groupBy { it.category }
    .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

// 4. Transformer en PieChartData.Slice
val pieChartData = PieChartData(
    slices = categoryTotals.map { (category, total) ->
        PieChartData.Slice(
            label = category,
            value = total.toFloat(),
            color = // Logique pour assigner une couleur
        )
    },
    plotType = PlotType.Donut
)

// 5. Afficher le graphique
PieChart(
    modifier = Modifier.height(250.dp),
    pieChartData = pieChartData,
    pieChartConfig = PieChartConfig(...)
)
```

#### MonthlyBarChart : Historique des Dépenses Mensuelles
Ce graphique offre une vue d'ensemble de l'évolution des dépenses sur les 6 derniers mois, permettant à l'utilisateur d'identifier des tendances.
Logique de fonctionnement :
- 1. **Collecte des Données** : Similaire au ```PieChart```, il récupère toutes les dépenses de la base de données.
- 2. **Préparation des Périodes** : Le code génère une liste des 6 derniers mois.
- 3. **Calcul des Totaux par Mois** : Pour chacun de ces 6 mois, il filtre la liste complète des dépenses pour isoler celles correspondant au mois en question, puis calcule leur somme totale.
- 4. **Transformation pour YCharts** : Chaque couple (mois, total) est transformé en un objet ```BarData```, qui contient le point (```Point(x, y)```) et une étiquette. ```x``` est l'index du mois et ```y``` est le montant total.
- 5. **Configuration Dynamique des Axes** :
  - **Axe X (Mois)** : Les étiquettes de l'axe X sont générées pour afficher le nom abrégé de chaque mois (ex: "Jan.", "Fév.").
  - **Axe Y (Montant)** : L'axe Y est configuré de manière dynamique. Le code calcule la valeur maximale (```maxValue```) parmi les totaux mensuels et divise l'axe en un nombre fixe d'étapes (```steps```). Cela garantit que l'échelle du graphique est toujours appropriée, peu importe si les dépenses sont de 100€ ou 10 000€.
```Kotlin
// Dans MonthlyBarChart.kt

// 3. Calculer le total pour chaque mois
val monthTotals = last12Months.map { monthDate ->
    val total = allExpenses.filter { /* ... correspond au mois ... */ }.sumOf { it.amount }
    monthDate to total
}

// 4. Transformer en BarData
val barChartList: List<BarData> = monthTotals.mapIndexed { index, (date, total) ->
    BarData(
        point = Point(index.toFloat(), total.toFloat()),
        label = // ex: "J" pour Janvier
    )
}

// 5. Configurer l'axe Y dynamiquement
val maxValue = monthTotals.maxOfOrNull { it.second }?.toFloat() ?: 0f
val yAxisData = AxisData.Builder()
    .steps(5) // Divise l'axe en 5 segments
    .labelData { index ->
        val value = index * (maxValue / 5) // Calcule la valeur pour chaque libellé
        "%.0f".format(value)
    }
    .build()

// Afficher le graphique
BarChart(
    modifier = Modifier.height(300.dp),
    barChartData = BarChartData(bars = barChartList),
    xAxisData = xAxisData,
    yAxisData = yAxisData
)
```

## Dépendances Principales
- **Jetpack Compose** : Pour l'ensemble de l'interface utilisateur.
- **Room** : Pour la base de données locale (persistance des dépenses).
- **DataStore** : Pour la persistance des préférences utilisateur (budget).
- **YCharts** : Pour les graphiques personnalisés.
- **Lifecycle & ViewModel KTX** : Pour une gestion du cycle de vie des écrans conforme aux standards Android.
