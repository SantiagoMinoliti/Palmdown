package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palmdown.model.News
import com.example.palmdown.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = viewModel()
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        NewsScreenTracker.isNewsScreenVisible = true
        onDispose {
            NewsScreenTracker.isNewsScreenVisible = false
        }
    }

    val newsList by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNews()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "News",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { viewModel.refresh(context, forceRefresh = true) },
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh news"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Error while loading news",
                    color = MaterialTheme.colorScheme.error
                )
            }

            newsList.isEmpty() -> {
                EmptyNewsTutorial()
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newsList) { news ->
                        NewsCard(news)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNewsTutorial() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No news yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Refresh the page to request them.\nGo to Settings to control them!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NewsCard(news: News) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm")

    val safeContent = news.content
        .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (news.url.isBlank()) return@clickable
                val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                )
            },
        shape = RoundedCornerShape(12.dp),
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
