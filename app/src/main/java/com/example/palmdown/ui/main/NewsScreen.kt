package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.palmdown.model.News
import com.example.palmdown.ui.archive.NewsArchiveActivity
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
        onDispose { NewsScreenTracker.isNewsScreenVisible = false }
    }

    val newsList by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    val filteredNews = remember(newsList, searchQuery.text) {
        if (searchQuery.text.isBlank()) newsList
        else newsList.filter {
            it.title.contains(searchQuery.text, ignoreCase = true) ||
                    it.content.contains(searchQuery.text, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadNews() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFAFAFA))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "News",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.refresh(context, forceRefresh = true) }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archived", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                                onClick = {
                                    menuExpanded = false
                                    context.startActivity(Intent(context, NewsArchiveActivity::class.java))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom search bar
            AnimatedVisibility(visible = searchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                errorMessage != null -> Text(
                    text = errorMessage ?: "Error while loading news",
                    color = MaterialTheme.colorScheme.error
                )
                filteredNews.isEmpty() -> EmptyNewsTutorial()
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(filteredNews) { news ->
                        NewsListItem(news)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFCCCCCC), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNewsTutorial() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "No news found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Try refreshing or changing your search.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}

@Composable
private fun NewsListItem(news: News) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val safeContent = news.content.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: ""

    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            if (news.url.isBlank()) return@clickable
            val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
        }
        .padding(vertical = 8.dp)
    ) {
        Text(
            text = news.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp
        )
        if (safeContent.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(safeContent, style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp)) // space after content
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateUtils.formatDate(news.date), style = MaterialTheme.typography.labelSmall, fontSize = 14.sp)
            Text(news.date?.let { timeFormat.format(it) } ?: "", style = MaterialTheme.typography.labelSmall, fontSize = 14.sp)
            val country = formatCountrySingle(news.country)
            if (country.isNotBlank()) Text(country, style = MaterialTheme.typography.labelSmall, fontSize = 14.sp)
        }
    }
}

private fun formatCountrySingle(raw: String): String {
    if (raw.isBlank()) return ""
    val cleaned = raw.removePrefix("[").removeSuffix("]").replace("\"", "")
    val first = cleaned.split(",").firstOrNull()?.trim() ?: return ""
    val lower = first.lowercase(Locale.getDefault())
    val lowercaseWords = setOf("of", "and", "the")
    return lower.split(" ").joinToString(" ") { word -> if (word in lowercaseWords) word else word.replaceFirstChar { it.uppercase() } }
}
