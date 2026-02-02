package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
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
    var searchExpanded by remember { mutableStateOf(false) }

    var focusedNewsId by remember { mutableStateOf<String?>(null) }

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
        Column(modifier = Modifier.fillMaxSize()) {
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
                    IconButton(
                        onClick = { viewModel.refresh(context, forceRefresh = true) },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = searchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.Gray
                        )
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
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                errorMessage != null -> Text(
                    text = errorMessage ?: "Error while loading news",
                    color = MaterialTheme.colorScheme.error
                )

                filteredNews.isEmpty() -> EmptyNewsTutorial()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 0.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredNews) { news ->
                        NewsListItem(
                            news = news,
                            isFocused = focusedNewsId == news.id,
                            dimmed = focusedNewsId != null && focusedNewsId != news.id,
                            onLongPressActivated = { focusedNewsId = news.id },
                            onMenuDismiss = { focusedNewsId = null }
                        )
                        Divider(color = Color(0xFFCCCCCC), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyNewsTutorial() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No news found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Try refreshing or changing your search.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun NewsListItem(
    news: News,
    isFocused: Boolean,
    dimmed: Boolean,
    onLongPressActivated: () -> Unit,
    onMenuDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var menuExpanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var fixedMenuOffset by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val alpha by animateFloatAsState(
        targetValue = when {
            dimmed -> 0.35f
            isPressed && !isFocused -> 0.6f
            else -> 1f
        },
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f),
        label = "pop"
    )

    val safeContent =
        news.content.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .onGloballyPositioned { coords ->
                if (fixedMenuOffset == null) {
                    val windowOffset = coords.localToWindow(Offset.Zero)
                    pressOffset = windowOffset
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (fixedMenuOffset == null) {
                            pressOffset = pressOffset + change.position
                        }
                    }
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (news.url.isBlank()) return@combinedClickable
                    val finalUrl =
                        if (news.url.startsWith("http")) news.url else "https://${news.url}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                },
                onLongClick = {
                    onLongPressActivated()
                    fixedMenuOffset = pressOffset
                    menuExpanded = true
                }
            )
    ) {
        Column {
            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp
            )

            if (safeContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(safeContent, style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    DateUtils.formatDate(news.date),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 14.sp
                )
                Text(news.date?.let { timeFormat.format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 14.sp)
                val country = formatCountrySingle(news.country)
                if (country.isNotBlank()) Text(
                    country,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // DropdownMenu fisso appena compare
        fixedMenuOffset?.let { offset ->
            Box {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        fixedMenuOffset = null
                        onMenuDismiss()
                    },
                    offset = DpOffset(
                        x = with(density) { offset.x.toDp() },
                        y = with(density) { offset.y.toDp() }),
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .widthIn(min = 180.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Create note from this news", fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            fixedMenuOffset = null
                            onMenuDismiss()
                        }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.25.dp)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Put news in archive", fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            fixedMenuOffset = null
                            onMenuDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun formatCountrySingle(raw: String): String {
    if (raw.isBlank()) return ""
    val cleaned = raw.removePrefix("[").removeSuffix("]").replace("\"", "")
    val first = cleaned.split(",").firstOrNull()?.trim() ?: return ""
    val lower = first.lowercase(Locale.getDefault())
    val lowercaseWords = setOf("of", "and", "the")
    return lower.split(" ")
        .joinToString(" ") { word -> if (word in lowercaseWords) word else word.replaceFirstChar { it.uppercase() } }
}