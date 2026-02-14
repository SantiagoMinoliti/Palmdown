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
import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException
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

    companion object {
        const val KEY_FORCE_REFRESH = "force_refresh"
        private const val SAFE_BROWSING_API_KEY = "AIzaSyAfZHPatJzojUkwuV7XnoIYwM9HO8cLghA"
        private const val SAFE_BROWSING_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$SAFE_BROWSING_API_KEY"
        private const val TAG = "PalmDownWorker"
    }

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

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        var responseBody: String? = null
        var fetchSuccessful = false

        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    responseBody = response.body?.string()
                    fetchSuccessful = true
                    break
                } else {
                    response.close()
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, "DNS Error: Impossibile risolvere l'host. Se sei su emulatore, prova 'Wipe Data'. Error: ${e.message}")
            } catch (e: IOException) {
                Log.e(TAG, "Errore di rete generico: ${e.message}")
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

        // Usiamo un Set per tracciare i titoli già visti in questo batch
        val seenTitles = mutableSetOf<String>()

        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            val title = item.optString("title")

            // Controllo duplicati nel batch corrente (stessa notizia da fonti diverse)
            if (title.isNotBlank() && seenTitles.contains(title)) {
                Log.d(TAG, "News scartata (Titolo duplicato nel batch): $title")
                continue
            }
            seenTitles.add(title)

            val pubDate = item.optString("pubDate")
            val parsedPubDate = runCatching {
                if (pubDate.isNotBlank()) dateFormat.parse(pubDate) else null
            }.getOrNull()

            val newsUrl = item.optString("link")

            if (newsUrl.isBlank()) continue

            val news = News(
                title = title,
                content = item.optString("description"),
                url = newsUrl,
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

            // 1. Controllo validità link (HEAD/GET)
            if (!checkUrlHead(client, newsUrl)) {
                Log.w(TAG, "News scartata (Link non valido): $newsUrl")
                continue
            }

            // 2. Controllo Google Safe Browsing
            if (!checkSafeBrowsing(client, newsUrl)) {
                Log.w(TAG, "News scartata (Sito non sicuro): $newsUrl")
                continue
            }

            if (newsRepository.saveNewsIfNotExists(news)) {
                newNews.add(news)
                Log.d(TAG, "News salvata: $title")
            } else {
                Log.d(TAG, "News già presente nel DB: $title")
            }
        }

        if (newNews.isNotEmpty() && !NewsScreenTracker.isNewsScreenVisible && !forceRefresh) {
            showNotification(newNews.first())
        }

        return@withContext Result.success()
    }

    private fun checkUrlHead(client: OkHttpClient, url: String): Boolean {
        return try {
            var request = Request.Builder()
                .url(url)
                .head()
                .build()

            var response = client.newCall(request).execute()

            if (response.code == 405) {
                response.close()
                request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                response = client.newCall(request).execute()
            }

            val isSuccess = response.isSuccessful
            response.close()
            isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSafeBrowsing(client: OkHttpClient, url: String): Boolean {
        return try {
            val jsonBody = JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientId", "palmdown-android")
                    put("clientVersion", "1.0.0")
                })
                put("threatInfo", JSONObject().apply {
                    put("threatTypes", JSONArray().apply {
                        put("MALWARE")
                        put("SOCIAL_ENGINEERING")
                        put("UNWANTED_SOFTWARE")
                        put("POTENTIALLY_HARMFUL_APPLICATION")
                    })
                    put("platformTypes", JSONArray().put("ANDROID"))
                    put("threatEntryTypes", JSONArray().put("URL"))
                    put("threatEntries", JSONArray().put(JSONObject().put("url", url)))
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(SAFE_BROWSING_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return false
            }

            val responseString = response.body?.string() ?: "{}"
            response.close()

            val jsonResponse = JSONObject(responseString)
            !jsonResponse.has("matches")
        } catch (e: Exception) {
            Log.e(TAG, "Errore Safe Browsing: ${e.message}")
            false
        }
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

        var largeIcon: android.graphics.Bitmap? = null
        if (!news.sourceIcon.isNullOrBlank()) {
            try {
                val url = URL(news.sourceIcon)
                largeIcon = BitmapFactory.decodeStream(url.openStream())
            } catch (e: Exception) {
                // Ignora errori caricamento immagine
            }
        }

        if (largeIcon == null) {
            largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setLargeIcon(largeIcon)
            .setContentTitle(news.title)
            .setContentText(news.content.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(news.content))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}