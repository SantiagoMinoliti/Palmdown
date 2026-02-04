package com.example.palmdown.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        onDispose { NewsScreenTracker.isNewsScreenVisible = false }
    }

    val newsList by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeFilter by viewModel.filters.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var focusedNewsId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFFAFAFA)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFF3ECFA))))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "News",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 28.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { searchExpanded = !searchExpanded }) {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                            IconButton(
                                onClick = { viewModel.refresh(context, forceRefresh = true) },
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        }
                    }

                    AnimatedVisibility(visible = searchExpanded) {
                        ModernSearchBar(
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

                    CategoryRow(
                        categories = sortedCategories,
                        selected = activeFilter.category.ifBlank { "all" },
                        onSelected = viewModel::updateCategory
                    )

                    Divider(color = Color(0xFF632F96), thickness = 1.dp)
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                errorMessage != null -> Text(
                    text = errorMessage ?: "Error while loading news",
                    color = MaterialTheme.colorScheme.error
                )
                newsList.isEmpty() -> EmptyNewsTutorial()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsList) { news ->
                        NewsListItem(
                            news = news.copy(
                                title = news.title?.let { if (it.length > 128) it.take(128) + "…" else it } ?: "",
                                content = news.content?.let { if (it.length > 192) it.take(192) + "…" else it } ?: ""
                            ),
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
    private fun ModernSearchBar(value: String, onValueChange: (String) -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(32.dp)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F7FB))), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFBDB6D5), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF7A75A1))
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isBlank()) {
                            Text("Search news...", color = Color(0xFFAAA7C3), fontSize = 14.sp)
                        }
                        inner()
                    }
                )
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryRow(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(top = 6.dp, bottom = 4.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category.equals(selected, true)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onSelected(category) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = category.replaceFirstChar { it.uppercaseChar() },
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF632F96) else Color.Gray
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(24.dp)
                            .background(if (isSelected) Color(0xFF632F96) else Color.Transparent)
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
            Text(
                text = "No news found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Try refreshing or changing your filters.",
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

        val safeContent = news.content.takeUnless { it.isBlank() || it.equals("null", true) } ?: ""

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
                .scale(scale)
                .onGloballyPositioned { coords ->
                    if (fixedMenuOffset == null) pressOffset = coords.localToWindow(Offset.Zero)
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (fixedMenuOffset == null) pressOffset += change.position
                        }
                    }
                }
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
                Text(news.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                if (safeContent.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(safeContent, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(DateUtils.formatDate(news.date), fontSize = 14.sp)
                    Text(news.date?.let { timeFormat.format(it) } ?: "", fontSize = 14.sp)
                }
                Spacer(Modifier.height(24.dp))
            }

            fixedMenuOffset?.let { offset ->
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        fixedMenuOffset = null
                        onMenuDismiss()
                    },
                    offset = DpOffset(x = with(density) { offset.x.toDp() }, y = with(density) { offset.y.toDp() })
                ) {
                    DropdownMenuItem(
                        text = { Text("Create note from this news", fontSize = 13.sp) },
                        onClick = { onMenuDismiss(); menuExpanded = false; fixedMenuOffset = null }
                    )
                    DropdownMenuItem(
                        text = { Text("Put news in archive", fontSize = 13.sp) },
                        onClick = { onMenuDismiss(); menuExpanded = false; fixedMenuOffset = null }
                    )
                }
            }
        }
    }