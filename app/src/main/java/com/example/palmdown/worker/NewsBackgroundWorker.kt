package com.example.palmdown.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NewsBackgroundWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val settingsRepository = SettingsRepository()
    private val newsRepository = NewsRepository()

    /**
     * Esegue il lavoro in background.
     * Utilizziamo withContext(Dispatchers.IO) per spostare l'esecuzione su un thread
     * ottimizzato per operazioni di I/O (Rete, Database), evitando di bloccare.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)

        val settings = settingsRepository.getSettings() ?: return@withContext Result.success()
        if (!forceRefresh && !settings.notificationsEnabled) return@withContext Result.success()

        val query = settings.keywords.joinToString(" OR ")
        val languageParam = settings.languages.joinToString(",") { it.lowercase() }

        val url = buildString {
            append("https://newsdata.io/api/1/latest?apikey=pub_5c29853e6fe54ab9b1b0e4d69ae7fed6")
            if (query.isNotBlank()) append("&q=$query")
            if (languageParam.isNotBlank()) append("&language=$languageParam")
        }

        // Configurazione aggressiva del client con timeout più lunghi
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        var responseBody: String? = null
        var fetchSuccessful = false

        // *** STRATEGIA DI RETRY INTERNO ***
        // Proviamo a scaricare i dati fino a 3 volte prima di arrendersi al WorkManager.
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    responseBody = response.body?.string()
                    fetchSuccessful = true
                    break // Successo! Usciamo dal loop di retry
                } else {
                    response.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (attempt < maxRetries) {
                delay(attempt * 2000L)
            }
        }

        if (!fetchSuccessful || responseBody == null) {
            return@withContext Result.retry()
        }

        val results = try {
            JSONObject(responseBody).optJSONArray("results")
        } catch (e: Exception) {
            null
        } ?: return@withContext Result.success()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newNews = mutableListOf<News>()

        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)

            val pubDate = item.optString("pubDate")

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

        return@withContext Result.success()
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

        // Logica per l'icona grande (Large Icon) - Questa accetta i colori!
        var largeIcon: android.graphics.Bitmap? = null
        if (!news.sourceIcon.isNullOrBlank()) {
            try {
                // Proviamo a scaricare l'icona della fonte
                val url = URL(news.sourceIcon)
                largeIcon = BitmapFactory.decodeStream(url.openStream())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Se l'icona online fallisce (spesso sono SVG non supportati), usiamo l'icona dell'app come fallback.
        // BitmapFactory.decodeResource assicura che venga caricata come immagine, non come drawable xml.
        if (largeIcon == null) {
            largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            // SmallIcon: DEVE essere monocromatica/trasparente per Android moderno.
            // Se metti ic_launcher qui, Android la trasforma in un cerchio grigio/blu.
            // Usiamo l'icona di notifica standard che avevi.
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)

            // LargeIcon: Qui va l'immagine a colori (Fonte o App Icon)
            .setLargeIcon(largeIcon)

            .setContentTitle(news.title)
            .setContentText(news.content.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(news.content)) // Espande il testo se lungo
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