package ch.zu.chrimametro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ch.zu.chrimametro.ui.theme.ChrimametroTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewmodel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChrimametroTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val myState by viewModel.myStateFlow.collectAsState(emptyList())
                    MainComposable(myState, viewModel)
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
