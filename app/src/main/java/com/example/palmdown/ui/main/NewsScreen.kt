package com.example.palmdown.ui.main

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.example.palmdown.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        NewsScreenTracker.isNewsScreenVisible = true
        onDispose { NewsScreenTracker.isNewsScreenVisible = false }
    }

    val newsList by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeFilter by viewModel.filters.collectAsState()

    LaunchedEffect(categories, activeFilter.category) {
        if (activeFilter.category != "all" && activeFilter.category.isNotBlank()) {
            if (categories.none { it.equals(activeFilter.category, ignoreCase = true) }) {
                viewModel.updateCategory("all")
            }
        }
    }

    var searchExpanded by remember { mutableStateOf(false) }
    var favoritesFirst by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    // Selection Mode States
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNewsIds by remember { mutableStateOf(setOf<String>()) }

    val cyanAccent = Color(0xFF00E5FF)
    val cardDark = Color(0xFF1E1E1E)
    val textPrimary = Color.White
    val accentPurple = Color(0xFF632F96)
    val destructiveRed = Color(0xFFFF453A)

    // Handle Back Press to exit selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedNewsIds = emptySet()
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

    val allSelectedAreFavorites by remember {
        derivedStateOf {
            val selectedItems = newsList.filter { it.id in selectedNewsIds }
            selectedItems.isNotEmpty() && selectedItems.all { it.isFavorite }
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
                            text = if (isSelectionMode) "${selectedNewsIds.size} Selected" else "Your News",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isSelectionMode) FontFamily.SansSerif else FontFamily.Serif,
                            fontSize = 32.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectionMode) {
                                IconButton(onClick = {
                                    isSelectionMode = false
                                    selectedNewsIds = emptySet()
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Selection",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { favoritesFirst = !favoritesFirst }) {
                                    Icon(
                                        imageVector = if (favoritesFirst) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = if (favoritesFirst) cyanAccent else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                IconButton(onClick = { searchExpanded = !searchExpanded }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showTopMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showTopMenu,
                                        onDismissRequest = { showTopMenu = false },
                                        offset = DpOffset(x = 12.dp, y = 0.dp),
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        border = null
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = cardDark,
                                            shadowElevation = 8.dp,
                                            border = BorderStroke(0.5.dp, Color(0xFF333333)),
                                            modifier = Modifier.widthIn(min = 220.dp)
                                        ) {
                                            Column {
                                                MenuOptionItem(
                                                    text = "Refresh",
                                                    icon = Icons.Default.Refresh,
                                                    textColor = textPrimary,
                                                    iconColor = accentPurple,
                                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                                    onClick = {
                                                        scope.launch {
                                                            showTopMenu = false
                                                            viewModel.refresh(context, forceRefresh = true)
                                                        }
                                                    }
                                                )

                                                Divider(color = Color(0xFF444444), thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = "Select News",
                                                    icon = Icons.Default.CheckCircle,
                                                    textColor = textPrimary,
                                                    iconColor = accentPurple,
                                                    shape = RoundedCornerShape(0.dp),
                                                    onClick = {
                                                        scope.launch {
                                                            delay(150)
                                                            isSelectionMode = true
                                                            showTopMenu = false
                                                        }
                                                    }
                                                )

                                                Divider(color = Color(0xFF444444), thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = "View Archive",
                                                    icon = Icons.Default.Archive,
                                                    textColor = textPrimary,
                                                    iconColor = accentPurple,
                                                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                                    onClick = {
                                                        scope.launch {
                                                            delay(150)
                                                            showTopMenu = false
                                                            try {
                                                                val intent = Intent(context, Class.forName("com.example.palmdown.ui.archive.NewsArchiveActivity"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = searchExpanded && !isSelectionMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PremiumSearchBar(
                            value = activeFilter.query,
                            onValueChange = viewModel::updateQuery
                        )
                    }

                    if (sortedCategories.size > 1 && !isSelectionMode) {
                        Spacer(Modifier.height(16.dp))

                        CategoryRowDark(
                            categories = sortedCategories,
                            selected = activeFilter.category.ifBlank { "all" },
                            onSelected = viewModel::updateCategory
                        )
                    }
                }
            }

            when {
                isLoading && filteredNews.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF632F96))
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Error while loading news",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                filteredNews.isEmpty() -> EmptyNewsTutorial()
                else -> {
                    val pullRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refresh(context, forceRefresh = true) },
                        modifier = Modifier.fillMaxSize(),
                        state = pullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullRefreshState,
                                isRefreshing = isLoading,
                                modifier = Modifier.align(Alignment.TopCenter),
                                containerColor = Color.White,
                                color = accentPurple
                            )
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isSelectionMode) 12.dp else 24.dp),
                        ) {
                            items(filteredNews) { news ->
                                NewsListItem(
                                    news = news.copy(
                                        title = news.title?.let { if (it.length > 128) it.take(128) + "…" else it } ?: "",
                                        content = news.content?.let { if (it.length > 192) it.take(192) + "…" else it } ?: ""
                                    ),
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedNewsIds.contains(news.id),
                                    onLongPressActivated = {
                                        if (!isSelectionMode) {
                                            // Handle context menu inside NewsListItem
                                        } else {
                                            isSelectionMode = true
                                            selectedNewsIds = selectedNewsIds + news.id
                                        }
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(news.id) },
                                    onArchive = { viewModel.archiveNews(news.id) },
                                    onDelete = { viewModel.deleteNews(news.id) },
                                    onSelectionToggle = {
                                        selectedNewsIds = if (selectedNewsIds.contains(news.id)) {
                                            selectedNewsIds - news.id
                                        } else {
                                            selectedNewsIds + news.id
                                        }
                                    }
                                )

                                if (!isSelectionMode) {
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
        }

        // Bottom Action Bar for Selection Mode
        AnimatedVisibility(
            visible = isSelectionMode && selectedNewsIds.isNotEmpty(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color(0xFF1E1E1E),
                contentColor = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Action
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            FooterActionButton(
                                text = if (allSelectedAreFavorites) "Unfavorite" else "Favorite",
                                icon = if (allSelectedAreFavorites) Icons.Outlined.StarBorder else Icons.Filled.Star,
                                color = cyanAccent,
                                onClick = {
                                    scope.launch {
                                        val targetState = !allSelectedAreFavorites
                                        val itemsToUpdate = newsList.filter { it.id in selectedNewsIds }
                                        itemsToUpdate.forEach { news ->
                                            if (news.isFavorite != targetState) {
                                                viewModel.toggleFavorite(news.id)
                                            }
                                        }
                                        isSelectionMode = false
                                        selectedNewsIds = emptySet()
                                    }
                                }
                            )
                        }

                        // Archive Action
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            FooterActionButton(
                                text = "Archive",
                                icon = Icons.Default.Archive,
                                color = accentPurple,
                                onClick = {
                                    scope.launch {
                                        selectedNewsIds.forEach { id -> viewModel.archiveNews(id) }
                                        isSelectionMode = false
                                        selectedNewsIds = emptySet()
                                    }
                                }
                            )
                        }

                        // Delete Action
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            FooterActionButton(
                                text = "Delete",
                                icon = Icons.Default.Delete,
                                color = destructiveRed,
                                onClick = {
                                    scope.launch {
                                        selectedNewsIds.forEach { id -> viewModel.deleteNews(id) }
                                        isSelectionMode = false
                                        selectedNewsIds = emptySet()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Safe area padding
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
    return lower.split(" ").joinToString(" ") { word -> if (word in lowercaseWords) word else word.replaceFirstChar { it.uppercase() } }
}

@Composable
fun MenuOptionItem(
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FooterActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by animateFloatAsState(targetValue = if (isPressed) 0.15f else 0f, label = "glow")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(color.copy(alpha = glowAlpha))
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp
        )
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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPressActivated: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onSelectionToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var menuExpanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var fixedMenuOffset by remember { mutableStateOf<Offset?>(null) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val alpha by animateFloatAsState(targetValue = if (isPressed && !menuExpanded) 0.8f else 1f, label = "alpha")
    val scale by animateFloatAsState(targetValue = if (isPressed && !menuExpanded) 0.98f else 1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f), label = "pop")

    val safeContent = news.content.takeUnless { it.isBlank() || it.equals("null", true) } ?: ""
    val cyanAccent = Color(0xFF00E5FF)
    val selectionPurple = Color(0xFF632F96)
    val textBlack = Color(0xFF1E1E1E)
    val accentPurple = Color(0xFF632F96)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .onGloballyPositioned { coords ->
                if (fixedMenuOffset == null) pressOffset = coords.localToWindow(Offset.Zero)
            }
            .animateContentSize()
            .then(
                if (isSelectionMode) {
                    Modifier.background(
                        if (isSelected) Color(0xFFE8E0F2) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectionToggle() }
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (news.url.isBlank()) return@combinedClickable
                            val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                fixedMenuOffset = pressOffset
                                menuExpanded = true
                                onLongPressActivated()
                            } else {
                                onSelectionToggle()
                            }
                        }
                    )
                }
            )
            .padding(if (isSelectionMode) 12.dp else 0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Selection Checkbox
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) selectionPurple else Color.Gray,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {

                // Image
                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .clickable { onToggleFavorite() }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (news.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (news.isFavorite) cyanAccent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = news.title,
                    fontSize = if (isSelectionMode) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    lineHeight = if (isSelectionMode) 24.sp else 28.sp,
                    color = Color.Black,
                    maxLines = if (isSelectionMode) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )

                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
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
                }
            }
        }

        // Custom Context Menu (White Background, No preview)
        fixedMenuOffset?.let { offset ->
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                    fixedMenuOffset = null
                },
                offset = DpOffset(x = with(density) { offset.x.toDp() }, y = with(density) { offset.y.toDp() }),
                modifier = Modifier
                    .background(Color.White)
                    .widthIn(min = 200.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        DropdownMenuItem(
                            text = { Text("Open in Browser", color = textBlack, fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                            },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = textBlack) }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Link", color = textBlack, fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("News Link", news.url)
                                clipboard.setPrimaryClip(clip)
                            },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = textBlack) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (news.isFavorite) "Unfavorite" else "Favorite", color = textBlack, fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                onToggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (news.isFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (news.isFavorite) textBlack else cyanAccent
                                )
                            }
                        )
                        Divider(thickness = 0.5.dp, color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Share", color = textBlack, fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${news.title}\n\n${news.url}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share News"))
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = textBlack) }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive", color = textBlack, fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                onArchive()
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = textBlack) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFFF453A), fontSize = 16.sp) },
                            onClick = {
                                menuExpanded = false
                                fixedMenuOffset = null
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A)) }
                        )
                    }
                }
            }
        }
    }
}