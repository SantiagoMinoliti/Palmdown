package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.palmdown.model.News
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

private const val TAG = "NewsDebug"

@Composable
fun NewsScreen() {

    val repository = remember { NewsRepository() }
    val scope = rememberCoroutineScope()

    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "➡️ LaunchedEffect: richiesta news (coroutines)")
        try {
            val result = repository.getAllNews()
            Log.d(TAG, "✅ News ricevute: ${result.size}")
            result.forEach { news ->
                Log.d(TAG, "📥 News ID=${news.id}, url='${news.url}'")
            }
            newsList = result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore Firestore", e)
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "News",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()

            errorMessage != null -> Text(
                text = "Errore nel caricamento news",
                color = MaterialTheme.colorScheme.error
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsList) { news ->
                    NewsCard(news)
                }
            }
        }
    }
}

@Composable
private fun NewsCard(news: News) {

    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d(TAG, "👉 Click su news ID=${news.id}, url=${news.url}")

                if (news.url.isBlank()) {
                    Log.w(TAG, "⚠️ URL vuoto, click ignorato")
                    return@clickable
                }

                val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                Log.d(TAG, "🌍 URL finale: $finalUrl")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = news.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DateUtils.formatDate(news.date),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = news.date?.let { timeFormat.format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = news.country,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}