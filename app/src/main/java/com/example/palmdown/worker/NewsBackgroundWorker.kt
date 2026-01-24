package com.example.palmdown.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.example.palmdown.ui.main.NewsScreenTracker
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
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)

        val settings = settingsRepository.getSettings() ?: return Result.success()
        if (!forceRefresh && !settings.notificationsEnabled) return Result.success()

        val query = settings.keywords.joinToString(" OR ")
        val languageParam = settings.languages.joinToString(",") { it.lowercase() }

        val url = buildString {
            append("https://newsdata.io/api/1/latest?apikey=pub_5c29853e6fe54ab9b1b0e4d69ae7fed6")
            if (query.isNotBlank()) append("&q=$query")
            if (languageParam.isNotBlank()) append("&language=$languageParam")
        }

        val client = OkHttpClient()
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return Result.retry()

        val body = response.body?.string() ?: return Result.success()
        val results = JSONObject(body).optJSONArray("results") ?: return Result.success()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newNews = mutableListOf<News>()

        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)

            val pubDate = item.optString("pubDate")
            val fetchedAtRaw = item.optString("fetched_at")

            val parsedPubDate = runCatching {
                if (pubDate.isNotBlank()) dateFormat.parse(pubDate) else null
            }.getOrNull()


            val news = News(
                title = item.optString("title"),
                content = item.optString("description"),
                url = item.optString("link"),
                date = parsedPubDate,
                fetchedAt = Date(),
                country = item.optJSONArray("country")?.toString() ?: "",
                keywords = item.optJSONArray("keywords")?.let { arr ->
                    List(arr.length()) { idx -> arr.optString(idx) }
                } ?: emptyList(),
                creator = item.optJSONArray("creator")?.let { arr ->
                    List(arr.length()) { idx -> arr.optString(idx) }
                } ?: emptyList(),
                categories = item.optJSONArray("category")?.let { arr ->
                    List(arr.length()) { idx -> arr.optString(idx) }
                } ?: emptyList(),
                imageUrl = item.optString("image_url"),
                videoUrl = item.optString("video_url"),
                sourceId = item.optString("source_id"),
                sourceName = item.optString("source_name"),
                sourceIcon = item.optString("source_icon")
            )

            if (newsRepository.saveNewsIfNotExists(news)) {
                newNews.add(news)
            }
        }

        if (newNews.isNotEmpty() && !NewsScreenTracker.isNewsScreenVisible && !forceRefresh) {
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

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(news.title)
            .setContentText(news.content.take(120))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val KEY_FORCE_REFRESH = "force_refresh"
    }
}