package ch.zu.chrimametro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import ch.zu.chrimametro.navigation.BottomNavigationBar
import ch.zu.chrimametro.navigation.Screen
import ch.zu.chrimametro.ui.budget.BudgetViewModel
import ch.zu.chrimametro.ui.cashflow.CashFlowScreen
import ch.zu.chrimametro.ui.earning.EarningsScreen
import ch.zu.chrimametro.ui.earning.EarningsViewModel
import ch.zu.chrimametro.ui.expense.ExpensesScreen
import ch.zu.chrimametro.ui.expense.MainViewmodel
import ch.zu.chrimametro.ui.graphs.GraphViewModel
import ch.zu.chrimametro.ui.monthbudget.MonthBudgetViewModel
import ch.zu.chrimametro.ui.split.SplitViewModel
import ch.zu.chrimametro.ui.theme.ChrimametroTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewmodel by viewModels()
    private val budgetViewmodel: BudgetViewModel by viewModels()
    private val monthBudgetViewModel: MonthBudgetViewModel by viewModels()
    private val pieChartViewModel: GraphViewModel by viewModels()
    private val earningsViewModel: EarningsViewModel by viewModels()
    private val splitViewModel: SplitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChrimametroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(bottomBar = { BottomNavigationBar(navController = navController) }) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.HomeScreen.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.HomeScreen.route) {
                                ExpensesScreen(viewModel = viewModel)
                            }

                            composable(Screen.EarnScreen.route) {
                                EarningsScreen(earningsViewModel)
                            }
                            composable(Screen.CashFlowScreen.route) {
                                CashFlowScreen(viewModel)
                            }
                        }
                    }

                }

            }
        }

        val workRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
            DataSyncWorker::class.java,
            repeatInterval = 1, // Specify the repeat interval in hours
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()
        // Enqueue the work request with WorkManager
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}
