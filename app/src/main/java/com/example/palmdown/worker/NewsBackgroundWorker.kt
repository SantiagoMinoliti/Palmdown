package com.example.palmdown.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.palmdown.R
import com.example.palmdown.model.News
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.repository.SettingsRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewsBackgroundWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val settingsRepository = SettingsRepository()
    private val newsRepository = NewsRepository()

    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettings() ?: return Result.success()
        if (!settings.notificationsEnabled) return Result.success()

        val keywords = settings.keywords
        val languages = settings.languages

        val query = keywords.joinToString(" OR ")
        val languageParam = languages.joinToString(",") { it.lowercase() }

        val url = buildString {
            append("https://newsdata.io/api/1/latest?apikey=pub_c5ddd94b11714cc7a3398b4a60d44702")
            if (query.isNotBlank()) append("&q=$query")
            if (languageParam.isNotBlank()) append("&language=$languageParam")
        }

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Result.retry()

        val body = response.body?.string() ?: return Result.success()
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return Result.success()
        if (results.length() == 0) return Result.success()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val newNews = mutableListOf<News>()

        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)

            val pubDateRaw = item.optString("pubDate")
            val parsedDate: Date? = try {
                if (pubDateRaw.isNotBlank()) dateFormat.parse(pubDateRaw) else null
            } catch (e: Exception) {
                null
            }

            val news = News(
                title = item.optString("title"),
                content = item.optString("description"),
                url = item.optString("link"),
                date = parsedDate,
                country = item.optString("country")
            )

            val inserted = newsRepository.saveNewsIfNotExists(news)
            if (inserted) newNews.add(news)
        }

        if (newNews.isNotEmpty()) {
            showNotification(newNews.first())
        }

        return Result.success()
    }

    private fun showNotification(news: News) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val channelId = "news_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "News updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(news.title)
            .setContentText(news.content.take(120))
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
