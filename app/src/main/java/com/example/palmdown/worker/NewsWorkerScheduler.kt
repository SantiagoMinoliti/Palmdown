package com.example.palmdown.worker

import android.content.Context
import androidx.work.*
import com.example.palmdown.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NewsWorkerScheduler {
    private const val NEWS_WORK_PREFIX = "news_work_"

    fun scheduleDailyNews(context: Context) {
        val settingsRepository = SettingsRepository()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsRepository.getSettings() ?: return@launch
            if (!settings.notificationsEnabled) return@launch

            val notificationsPerDay = settings.notificationsPerDay.coerceIn(1, 10)

            val now = Calendar.getInstance()
            val todayKey = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR)

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startMillis = startCal.timeInMillis
            val endMillis = endCal.timeInMillis

            if (now.timeInMillis >= endMillis) return@launch

            val effectiveStart = maxOf(now.timeInMillis, startMillis)
            val windowMillis = endMillis - effectiveStart
            if (windowMillis <= 0) return@launch

            val interval = windowMillis / notificationsPerDay

            val workManager = WorkManager.getInstance(appContext)

            repeat(notificationsPerDay) { index ->
                val triggerAt = effectiveStart + index * interval
                if (triggerAt >= endMillis) return@repeat

                val delay = triggerAt - System.currentTimeMillis()
                if (delay <= 0) return@repeat

                val workName = "$NEWS_WORK_PREFIX$todayKey-$index"

                val workRequest = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(workName)
                    .build()

                workManager.enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
            }
        }
    }
}
