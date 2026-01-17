package com.example.palmdown.worker

import android.content.Context
import androidx.work.*
import com.example.palmdown.repository.SettingsRepository
import com.example.palmdown.workers.NewsBackgroundWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NewsWorkerScheduler {
    fun scheduleDailyNews(context: Context) {
        val settingsRepository = SettingsRepository()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val settings = settingsRepository.getSettings() ?: return@launch
            if (!settings.notificationsEnabled) return@launch

            val notificationsPerDay = settings.notificationsPerDay.coerceIn(1, 10)

            val now = Calendar.getInstance()
            val start = now.apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val end = now.apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val interval = (end - start) / notificationsPerDay

            repeat(notificationsPerDay) { i ->
                val delayMillis = start + i * interval - System.currentTimeMillis()
                val work = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                    .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }

}
