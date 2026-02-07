package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var focusedNewsId by remember { mutableStateOf<String?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val cyanAccent = Color(0xFF00E5FF)
    val cardDark = Color(0xFF1E1E1E)
    val textPrimary = Color.White
    val accentPurple = Color(0xFF632F96)

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

                    if (sortedCategories.size > 1) {
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

    var menuExpanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var fixedMenuOffset by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
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
            .onGloballyPositioned { coords -> if (fixedMenuOffset == null) pressOffset = coords.localToWindow(Offset.Zero) }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (news.url.isBlank()) return@combinedClickable
                    val finalUrl = if (news.url.startsWith("http")) news.url else "https://${news.url}"
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

        fixedMenuOffset?.let { offset ->
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                    fixedMenuOffset = null
                    onMenuDismiss()
                },
                offset = DpOffset(x = with(density) { offset.x.toDp() }, y = with(density) { offset.y.toDp() }),
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Share", fontSize = 14.sp) },
                    onClick = {
                        menuExpanded = false; fixedMenuOffset = null; onMenuDismiss()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${news.title}\n\n${news.url}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share News"))
                    },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Archive", fontSize = 14.sp) },
                    onClick = {
                        menuExpanded = false; fixedMenuOffset = null; onMenuDismiss()
                        onArchive()
                    },
                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 14.sp, color = Color(0xFFFF453A)) },
                    onClick = {
                        menuExpanded = false; fixedMenuOffset = null; onMenuDismiss()
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp)) }
                )
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