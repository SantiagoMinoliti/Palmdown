package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.palmdown.model.News
import com.example.palmdown.ui.archive.NotesArchiveActivity
import com.example.palmdown.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        NewsScreenTracker.isNewsScreenVisible = true
        onDispose { NewsScreenTracker.isNewsScreenVisible = false }
    }

    val newsList by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeFilter by viewModel.filters.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var favoritesFirst by remember { mutableStateOf(false) }
    var focusedNewsId by remember { mutableStateOf<String?>(null) }

    val cyanAccent = Color(0xFF00E5FF)

    val filteredNews by remember {
        derivedStateOf {
            val visible = newsList.filter { !it.isArchived }

            val query = activeFilter.query
            val filtered = if (query.isBlank()) visible else visible.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.content?.contains(query, ignoreCase = true) == true)
            }

            if (favoritesFirst) {
                filtered.sortedWith(
                    compareByDescending<News> { it.isFavorite }
                        .thenByDescending { it.date }
                )
            } else {
                filtered
            }
        }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF121212),
            Color(0xFF1E1B2E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your News",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 32.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { favoritesFirst = !favoritesFirst }) {
                                Icon(
                                    imageVector = if (favoritesFirst) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Toggle Favorites",
                                    tint = if (favoritesFirst) cyanAccent else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = { searchExpanded = !searchExpanded }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            // Main page menu (3 dots)
                            var pageMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { pageMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = pageMenuExpanded,
                                    onDismissRequest = { pageMenuExpanded = false },
                                    offset = DpOffset(x = 0.dp, y = 8.dp),
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF1E1B2E),
                                        shadowElevation = 8.dp,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF333333)),
                                        modifier = Modifier.widthIn(min = 200.dp)
                                    ) {
                                        Column {
                                            NewsMenuOptionItem(
                                                text = "View Archive",
                                                icon = Icons.Default.Archive,
                                                textColor = Color.White,
                                                iconColor = cyanAccent,
                                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                                onClick = {
                                                    pageMenuExpanded = false
                                                    val intent = Intent(context, NotesArchiveActivity::class.java)
                                                    context.startActivity(intent)
                                                }
                                            )
                                            Divider(color = Color(0xFF444444), thickness = 0.5.dp)
                                            NewsMenuOptionItem(
                                                text = "Refresh",
                                                icon = Icons.Default.Refresh,
                                                textColor = Color.White,
                                                iconColor = cyanAccent,
                                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                                onClick = {
                                                    pageMenuExpanded = false
                                                    viewModel.refresh(context, forceRefresh = true)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = searchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PremiumSearchBar(
                            value = activeFilter.query,
                            onValueChange = viewModel::updateQuery
                        )
                    }

                    val sortedCategories = remember(categories) {
                        val list = mutableListOf<String>()
                        list.add("all")
                        if (categories.any { it.equals("top", true) }) list.add("top")
                        categories.filter { !it.equals("top", true) }
                            .map { it.lowercase(Locale.getDefault()) }
                            .distinct()
                            .sorted()
                            .forEach { list.add(it) }
                        list
                    }

                    Spacer(Modifier.height(16.dp))

                    CategoryRowDark(
                        categories = sortedCategories,
                        selected = activeFilter.category.ifBlank { "all" },
                        onSelected = viewModel::updateCategory
                    )
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF632F96))
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Error while loading news",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                filteredNews.isEmpty() -> EmptyNewsTutorial()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(filteredNews) { news ->
                        NewsListItem(
                            news = news.copy(
                                title = news.title?.let { if (it.length > 128) it.take(128) + "…" else it } ?: "",
                                content = news.content?.let { if (it.length > 192) it.take(192) + "…" else it } ?: ""
                            ),
                            isFocused = focusedNewsId == news.id,
                            dimmed = focusedNewsId != null && focusedNewsId != news.id,
                            onLongPressActivated = { focusedNewsId = news.id },
                            onMenuDismiss = { focusedNewsId = null },
                            onToggleFavorite = { viewModel.toggleFavorite(news.id) },
                            onArchive = { viewModel.archiveNews(news.id) },
                            onDelete = { viewModel.deleteNews(news.id) }
                        )
                        Divider(
                            color = Color(0xFFE0E0E0),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSearchBar(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Search through Your News",
                        color = Color(0xFF8E8E93),
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(Color(0xFF632F96))
                )
            }

            if (value.isNotBlank()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Clear",
                        tint = Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRowDark(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = category.equals(selected, true)
            val displayName = category.replaceFirstChar { it.uppercaseChar() }

            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelected(category) }
                    .background(
                        if (isSelected) Color(0xFF632F96) else Color(0xFF2C2C2E)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFFAAAAAA)
                )
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
        Icon(
            imageVector = Icons.Default.Newspaper,
            contentDescription = null,
            tint = Color(0xFFDDDDDD),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No news found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try refreshing or changing your filters.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsListItem(
    news: News,
    isFocused: Boolean,
    dimmed: Boolean,
    onLongPressActivated: () -> Unit,
    onMenuDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var menuExpanded by remember { mutableStateOf(false) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val alpha by animateFloatAsState(targetValue = if (dimmed) 0.35f else if (isPressed && !isFocused) 0.6f else 1f, label = "alpha")
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f), label = "pop")

    val safeContent = news.content.takeUnless { it.isBlank() || it.equals("null", true) } ?: ""
    val cyanAccent = Color(0xFF00E5FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        tapOffset = offset
                        menuExpanded = true
                        onLongPressActivated()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onTap = {
                        if (news.url.isNotBlank()) {
                            val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                        }
                    }
                )
            }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (news.imageUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(news.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                }

                // Favorite Star
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable { onToggleFavorite() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (news.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (news.isFavorite) cyanAccent else Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(2.dp, CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = news.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                lineHeight = 28.sp,
                color = Color.Black
            )

            if (safeContent.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = safeContent,
                    fontSize = 16.sp,
                    color = Color(0xFF444444),
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val country = formatCountrySingle(news.country)
                    if (country.isNotBlank()) {
                        Text(
                            text = country.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF632F96)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(3.dp).background(Color.Gray, CircleShape))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(DateUtils.formatDate(news.date), fontSize = 12.sp, color = Color.Gray)
                }
                Text(news.date?.let { timeFormat.format(it) } ?: "", fontSize = 12.sp, color = Color.Gray)
            }
        }

        // Popup Menu under the finger
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
                onMenuDismiss()
            },
            offset = DpOffset(
                x = with(density) { tapOffset.x.toDp() },
                y = with(density) { tapOffset.y.toDp() }
            ),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEEEEEE))
            ) {
                Column {
                    NewsMenuOptionItem(
                        text = "Share",
                        icon = Icons.Default.Share,
                        textColor = Color.Black,
                        iconColor = Color.Black,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        onClick = {
                            menuExpanded = false
                            onMenuDismiss()
                            onShare()
                        }
                    )
                    Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                    NewsMenuOptionItem(
                        text = "Archive",
                        icon = Icons.Default.Archive,
                        textColor = Color.Black,
                        iconColor = Color.Black,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        onClick = {
                            menuExpanded = false
                            onMenuDismiss()
                            onArchive()
                        }
                    )
                    Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                    NewsMenuOptionItem(
                        text = "Delete",
                        icon = Icons.Default.Delete,
                        textColor = Color(0xFFFF453A),
                        iconColor = Color(0xFFFF453A),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        onClick = {
                            menuExpanded = false
                            onMenuDismiss()
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NewsMenuOptionItem(
    text: String,
    icon: ImageVector,
    textColor: Color,
    iconColor: Color,
    shape: Shape,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
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

private fun onShare() {}