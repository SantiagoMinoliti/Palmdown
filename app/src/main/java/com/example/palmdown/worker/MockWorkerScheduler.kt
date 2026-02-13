package com.example.palmdown.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MockWorkerScheduler {

    fun scheduleDailyNews(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        repeat(5) { i ->
            val workRequest = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                .setConstraints(constraints)
                .setInitialDelay(i.toLong(), TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10,
                    TimeUnit.SECONDS
                )
                .addTag("mock_news_worker_$i")
                .build()

            workManager.enqueue(workRequest)
        }
    }
}