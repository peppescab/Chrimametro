package ch.zu.chrimametro

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.zu.chrimametro.ui.MainWidget

class DataSyncWorker(
    val context: Context,
    val params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Fetch data or do some work and then update all instance of your widget
        MainWidget().updateAll(context)
        return Result.success()
    }
}