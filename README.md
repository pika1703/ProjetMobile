# ProjetMobile
Groupe 4 : Romain LE SOURD, Aboubacar DIABY

# BudgetZen - Gestionnaire de Dépenses

BudgetZen est une application Android native développée en Kotlin avec Jetpack Compose. Elle permet aux utilisateurs de suivre leurs dépenses, de les catégoriser et de visualiser des résumés mensuels et annuels pour mieux maîtriser leur budget.

## Structure du Projet

Le projet s'articule autour d'une architecture simple et moderne, utilisant une Activity unique (MainActivity) qui gère la navigation et l'affichage des différents écrans (Composable).

- **MainActivity.kt**: C'est le cœur de l'interface utilisateur.
  - **MainScreen**: Le composant principal qui utilise un Scaffold pour organiser l'écran avec une TopAppBar et une NavigationBar. Il gère la navigation entre les trois onglets principaux.
  - **Navigation par État (State-Driven)**: La navigation n'utilise pas un NavController complexe. Elle est pilotée par un état remember (selectedTab) qui détermine quel écran afficher (HomeScreen, AddExpenseScreen, ou SummaryScreen). Une seconde variable d'état (showAllExpenses) permet d'afficher l'écran de détail des dépenses (AllExpensesScreen) par-dessus l'écran principal, une approche simple et efficace.
  - **HomeScreen**: L'écran d'accueil. Il affiche un résumé du budget, la liste des dernières dépenses, et un graphique circulaire (ExpensePieChart) de la répartition des dépenses.
  - **AddExpenseScreen**: L'écran d'ajout de dépense, construit avec des composants Material 3 permet d'ajouter une dépense avec un montant, une catégorie, un nom (facultatif) et une date.
  - **SummaryScreen**: L'écran de bilan, qui contient le graphique à barres des dépenses mensuelles (MonthlyBarChart) et le total des dépenses.
  - **AllExpensesScreen**: Un écran qui affiche la liste complète de toutes les dépenses, avec des options de modification et de suppression.

- **com.example.budgetzen.data**: Ce package gère toute la logique de persistance des données.
  - **Expense.kt**: La classe de données (@Entity) qui définit la structure d'une dépense dans la base de données Room.
  - **ExpenseDao.kt**: L'interface d'accès aux données (DAO). Elle définit les méthodes pour interagir avec la base de données (insérer, récupérer, supprimer) en utilisant des Flow pour rendre l'interface utilisateur réactive.
  - **ExpenseDatabase.kt**: La classe de base de données Room qui hérite de RoomDatabase et implémente un pattern Singleton pour garantir une seule instance dans l'application.
  - **BudgetDataStore.kt**: Un référentiel qui gère la persistance du budget mensuel de l'utilisateur. Il utilise Jetpack DataStore pour stocker cette valeur de manière asynchrone et sécurisée.

- **com.example.budgetzen.chart**: Ce package contient les composants graphiques personnalisés.
  - **ExpensePieChart.kt**: Un Composable qui affiche un graphique circulaire (camembert) représentant la répartition des dépenses par catégorie pour le mois en cours. Il utilise la bibliothèque YCharts.
  - **MonthlyBarChart.kt**: Affiche un graphique à barres montrant le total des dépenses pour chaque mois des 6 derniers mois. Il utilise aussi la bibliothèque YCharts.
