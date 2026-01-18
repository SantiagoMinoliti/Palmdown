package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.palmdown.model.News
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "NewsDebug"

@Composable
fun NewsScreen() {

    val repository = remember { NewsRepository() }

    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect: richiesta news (coroutines)")
        try {
            val result = repository.getAllNews()
            Log.d(TAG, "News ricevute: ${result.size}")
            result.forEach { news ->
                Log.d(TAG, "📥 News ID=${news.id}, url='${news.url}'")
            }
            newsList = result
        } catch (e: Exception) {
            Log.e(TAG, "Errore Firestore", e)
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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

    // Normalizzazione content: null o "null" -> stringa vuota
    val safeContent = news.content
        .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d(TAG, "Click su news ID=${news.id}, url=${news.url}")

                if (news.url.isBlank()) {
                    Log.w(TAG, "URL vuoto, click ignorato")
                    return@clickable
                }

                val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                Log.d(TAG, "URL finale: $finalUrl")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium
            )

            if (safeContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = safeContent,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

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

                val country = formatCountrySingle(news.country)
                if (country.isNotBlank()) {
                    Text(
                        text = country,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatCountrySingle(raw: String): String {
    if (raw.isBlank()) return ""

    val cleaned = raw
        .removePrefix("[")
        .removeSuffix("]")
        .replace("\"", "")

    val first = cleaned.split(",").firstOrNull()?.trim() ?: return ""

    val lower = first.lowercase(Locale.getDefault())

    val lowercaseWords = setOf("of", "and", "the")

    return lower.split(" ")
        .joinToString(" ") { word ->
            if (word in lowercaseWords) word
            else word.replaceFirstChar { it.uppercase() }
        }
}
