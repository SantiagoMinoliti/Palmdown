package com.example.palmdown.worker

import android.content.Context
import com.example.palmdown.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MockWorkerScheduler {

    fun scheduleMockNews(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            repeat(5) { i ->
                val work = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                    .setInitialDelay((i * 1L), TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }
}
