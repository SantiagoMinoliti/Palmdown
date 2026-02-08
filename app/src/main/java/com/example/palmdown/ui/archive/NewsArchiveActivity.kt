package com.example.palmdown.ui.archive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palmdown.model.News
import com.example.palmdown.model.NewsFilter
import com.example.palmdown.repository.NewsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class NewsArchiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = NewsRepository()
        setContent {
            ArchiveScreen(repository = repository, onBack = { finish() })
        }
    }
}

@Composable
fun ArchiveScreen(repository: NewsRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNewsIds by remember { mutableStateOf(setOf<String>()) }
    var showTopMenu by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    val cyanAccent = Color(0xFF00E5FF)
    val darkBackground = Color(0xFF121212)
    val cardColor = Color(0xFF1E1E1E)
    val textPrimary = Color.White
    val destructiveRed = Color(0xFFFF453A)

    LaunchedEffect(Unit) {
        isLoading = true
        val allNews = repository.getAllNews(NewsFilter())
        newsList = allNews.filter { it.isArchived }
        isLoading = false
    }

    val filteredList by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) newsList
            else newsList.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedNewsIds = emptySet()
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(bottom = 8.dp) // Ridotto padding inferiore del box header
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp) // Ridotto padding interno
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Ridotto spazio sotto il titolo
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (isSelectionMode) { isSelectionMode = false; selectedNewsIds = emptySet() } else onBack() }) {
                                Icon(
                                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = cyanAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSelectionMode) "${selectedNewsIds.size} Selected" else "Archive",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }

                        if (!isSelectionMode) {
                            Row {
                                IconButton(onClick = { searchExpanded = !searchExpanded }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showTopMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showTopMenu,
                                        onDismissRequest = { showTopMenu = false },
                                        offset = DpOffset(x = 0.dp, y = 0.dp),
                                        containerColor = cardColor,
                                        border = BorderStroke(0.5.dp, Color(0xFF333333))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Select Items", color = textPrimary) },
                                            onClick = {
                                                showTopMenu = false
                                                isSelectionMode = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.CheckCircle, null, tint = cyanAccent)
                                            }
                                        )
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
                        ArchiveSearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            accentColor = cyanAccent
                        )
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = cyanAccent)
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results found" else "Archive is empty",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { news ->
                        ArchiveListItem(
                            news = news,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedNewsIds.contains(news.id),
                            accentColor = cyanAccent,
                            onLongPress = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedNewsIds = selectedNewsIds + news.id
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedNewsIds = if (selectedNewsIds.contains(news.id)) {
                                        selectedNewsIds - news.id
                                    } else {
                                        selectedNewsIds + news.id
                                    }
                                }
                            },
                            onUnarchive = {
                                scope.launch {
                                    repository.updateNews(news.copy(isArchived = false))
                                    newsList = newsList.filter { it.id != news.id }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    // Local removal only for UI, Repo doesn't have delete yet but we simulate
                                    newsList = newsList.filter { it.id != news.id }
                                }
                            }
                        )
                    }
                }
            }
        }

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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionFooterButton(
                            text = "Unarchive",
                            icon = Icons.Default.Restore,
                            color = cyanAccent,
                            onClick = {
                                scope.launch {
                                    val toRestore = newsList.filter { it.id in selectedNewsIds }
                                    toRestore.forEach {
                                        repository.updateNews(it.copy(isArchived = false))
                                    }
                                    newsList = newsList.filter { it.id !in selectedNewsIds }
                                    isSelectionMode = false
                                    selectedNewsIds = emptySet()
                                }
                            }
                        )
                        ActionFooterButton(
                            text = "Delete",
                            icon = Icons.Default.Delete,
                            color = destructiveRed,
                            onClick = {
                                scope.launch {
                                    // Simulate delete
                                    newsList = newsList.filter { it.id !in selectedNewsIds }
                                    isSelectionMode = false
                                    selectedNewsIds = emptySet()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveListItem(
    news: News,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)
    val backgroundColor = if (isSelected) Color(0xFF1A3F45) else Color(0xFF252525)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier.padding(end = 16.dp).size(20.dp)
                )
            }

            Text(
                text = news.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ArchiveSearchBar(value: String, onValueChange: (String) -> Unit, accentColor: Color) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(accentColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("Search in Archive...", color = Color.Gray)
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onValueChange("") }
                    )
                }
            }
        }
    )
}

@Composable
fun ActionFooterButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}