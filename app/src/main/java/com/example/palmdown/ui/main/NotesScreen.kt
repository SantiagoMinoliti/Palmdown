package com.example.palmdown.ui.notes

import android.content.Intent
import android.webkit.WebView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import com.example.palmdown.ui.archive.NotesArchiveActivity
import com.example.palmdown.ui.editor.EditorActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class NotePreviewState(
    val note: Notes,
    val initialBounds: Rect
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val repository = remember { NotesRepository() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var allNotes by remember { mutableStateOf(listOf<Notes>()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var searchExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<String>()) }

    var previewState by remember { mutableStateOf<NotePreviewState?>(null) }

    val bgDark = Color(0xFF121212)
    val cardDark = Color(0xFF1C1C1E)
    val accentPurple = Color(0xFF632F96)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFF8E8E93)
    val searchBarBg = Color(0xFF2C2C2E)
    val destructiveRed = Color(0xFFFF453A)
    val warningOrange = Color(0xFFFF9F0A)

    val filteredNotes by remember {
        derivedStateOf {
            val visibleNotes = allNotes.filter { !it.archived }

            val searched = if (searchQuery.isBlank()) visibleNotes
            else visibleNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
            searched.sortedWith(
                compareByDescending<Notes> { it.pinned }
                    .thenByDescending { it.date }
            )
        }
    }

    val allSelectedArePinned by remember {
        derivedStateOf {
            val selectedObjects = allNotes.filter { it.id in selectedNotes }
            selectedObjects.isNotEmpty() && selectedObjects.all { it.pinned }
        }
    }

    fun refreshNotes() {
        isLoading = true
        scope.launch {
            try {
                allNotes = repository.getAllNotes()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleSelection(noteId: String) {
        selectedNotes = if (selectedNotes.contains(noteId)) {
            selectedNotes - noteId
        } else {
            selectedNotes + noteId
        }
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedNotes = emptySet()
    }

    BackHandler(enabled = isSelectionMode) {
        clearSelection()
    }

    LifecycleResumeEffect(Unit) {
        refreshNotes()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = bgDark,
            floatingActionButton = {
                if (previewState == null && !isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, EditorActivity::class.java)
                            context.startActivity(intent)
                        },
                        containerColor = accentPurple,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isSelectionMode && selectedNotes.isNotEmpty(),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    Surface(
                        color = bgDark,
                        contentColor = textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    FooterActionButton(
                                        text = if (allSelectedArePinned) "Unpin" else "Pin",
                                        color = accentPurple,
                                        onClick = {
                                            scope.launch {
                                                val targetState = !allSelectedArePinned
                                                allNotes.filter { it.id in selectedNotes }.forEach { note ->
                                                    repository.saveNote(note.copy(pinned = targetState))
                                                }
                                                refreshNotes()
                                                clearSelection()
                                            }
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FooterActionButton(
                                        text = "Archive",
                                        color = accentPurple,
                                        onClick = {
                                            scope.launch {
                                                allNotes.filter { it.id in selectedNotes }.forEach { note ->
                                                    repository.saveNote(note.copy(archived = true))
                                                }
                                                refreshNotes()
                                                clearSelection()
                                            }
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    FooterActionButton(
                                        text = "Delete",
                                        color = destructiveRed,
                                        onClick = {
                                            scope.launch {
                                                selectedNotes.forEach { id ->
                                                    repository.deleteNote(id)
                                                }
                                                refreshNotes()
                                                clearSelection()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgDark)
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasSelection = isSelectionMode && selectedNotes.isNotEmpty()

                        Text(
                            text = if (hasSelection) "${selectedNotes.size} Selected" else "Your Space",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (hasSelection) FontFamily.SansSerif else FontFamily.Serif,
                            fontSize = 34.sp,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Row {
                            if (!isSelectionMode) {
                                IconButton(
                                    onClick = {
                                        searchExpanded = !searchExpanded
                                        if (!searchExpanded) searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = accentPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = accentPurple,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
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
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF333333)),
                                            modifier = Modifier.widthIn(min = 220.dp)
                                        ) {
                                            Column {
                                                MenuOptionItem(
                                                    text = "Select Notes",
                                                    icon = Icons.Default.CheckCircle,
                                                    textColor = textPrimary,
                                                    iconColor = accentPurple,
                                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                                    onClick = {
                                                        scope.launch {
                                                            delay(150)
                                                            isSelectionMode = true
                                                            menuExpanded = false
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
                                                            menuExpanded = false
                                                            val intent = Intent(context, NotesArchiveActivity::class.java)
                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                IconButton(onClick = { clearSelection() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Selection",
                                        tint = accentPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = searchExpanded && !isSelectionMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(searchBarBg)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = textPrimary,
                                        fontFamily = FontFamily.SansSerif
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    cursorBrush = SolidColor(accentPurple),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search entries...",
                                                color = textSecondary,
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentPurple, strokeWidth = 2.dp)
                    }
                } else if (filteredNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "No notes",
                            color = textSecondary.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = previewState == null
                    ) {
                        items(items = filteredNotes, key = { it.id }) { note ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart && !isSelectionMode) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            val updatedNote = note.copy(archived = true)
                                            repository.saveNote(updatedNote)
                                            refreshNotes()
                                        }
                                        true
                                    } else false
                                }
                            )

                            val isBeingPreviewed = previewState?.note?.id == note.id

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                gesturesEnabled = !isSelectionMode,
                                backgroundContent = {
                                    val density = LocalDensity.current
                                    val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                                    val threshold = with(density) { 90.dp.toPx() }
                                    val alpha = (abs(offset) / threshold).coerceIn(0f, 1f)
                                    val interpolatedColor = warningOrange.copy(alpha = alpha)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(interpolatedColor)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (alpha > 0.05f) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = "Archive",
                                                tint = Color.White.copy(alpha = alpha),
                                                modifier = Modifier.scale(0.8f + (alpha * 0.2f))
                                            )
                                        }
                                    }
                                }
                            ) {
                                Box(modifier = Modifier.alpha(if (isBeingPreviewed) 0f else 1f)) {
                                    AppleStyleNoteCard(
                                        note = note,
                                        cardColor = cardDark,
                                        textColor = textPrimary,
                                        dateColor = textSecondary,
                                        accentColor = accentPurple,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = selectedNotes.contains(note.id),
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleSelection(note.id)
                                            } else {
                                                val intent = Intent(context, EditorActivity::class.java).apply {
                                                    putExtra("NOTE_ID", note.id)
                                                    putExtra("NOTE_TITLE", note.title)
                                                    putExtra("NOTE_CONTENT", note.content)
                                                }
                                                context.startActivity(intent)
                                            }
                                        },
                                        onLongClick = { bounds ->
                                            if (!isSelectionMode) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                previewState = NotePreviewState(note, bounds)
                                            } else {
                                                toggleSelection(note.id)
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

        if (previewState != null) {
            val state = previewState!!
            PreviewOverlay(
                state = state,
                cardColor = cardDark,
                textColor = textPrimary,
                secondaryColor = textSecondary,
                destructiveColor = destructiveRed,
                accentColor = accentPurple,
                onDismiss = { previewState = null },
                onEdit = {
                    previewState = null
                    val intent = Intent(context, EditorActivity::class.java).apply {
                        putExtra("NOTE_ID", state.note.id)
                        putExtra("NOTE_TITLE", state.note.title)
                        putExtra("NOTE_CONTENT", state.note.content)
                    }
                    context.startActivity(intent)
                },
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${state.note.title}\n\n${state.note.content}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                    previewState = null
                },
                onArchive = {
                    scope.launch {
                        val updatedNote = state.note.copy(archived = true)
                        repository.saveNote(updatedNote)
                        refreshNotes()
                    }
                    previewState = null
                },
                onPin = {
                    scope.launch {
                        val updatedNote = state.note.copy(pinned = !state.note.pinned)
                        repository.saveNote(updatedNote)
                        refreshNotes()
                    }
                    previewState = null
                },
                onDelete = {
                    scope.launch {
                        repository.deleteNote(state.note.id)
                        refreshNotes()
                    }
                    previewState = null
                }
            )
        }
    }
}

@Composable
fun FooterActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by animateFloatAsState(targetValue = if (isPressed) 0.3f else 0f, label = "glow")

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = glowAlpha),
                                color.copy(alpha = 0f)
                            ),
                            center = center,
                            radius = size.width / 1.4f
                        )
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp
        )
    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleStyleNoteCard(
    note: Notes,
    cardColor: Color,
    textColor: Color,
    dateColor: Color,
    accentColor: Color,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var itemBounds by remember { mutableStateOf(Rect.Zero) }

    val dateTimeStr = remember(note.date) {
        val dateObj = Date(note.date)
        val isToday = android.text.format.DateUtils.isToday(note.date)
        val format = if (isToday) "HH:mm" else "MMM dd"
        SimpleDateFormat(format, Locale.getDefault()).format(dateObj)
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "scale"
    )

    val finalCardColor = if (isSelected) Color(0xFF38383A) else cardColor

    val titleHex = String.format("#%06X", (0xFFFFFF and textColor.toArgb()))
    val contentHex = String.format("#%06X", (0xFFFFFF and dateColor.toArgb()))

    val titleHtml = remember(note.title, textColor) {
        """
        <html>
        <head>
        <style>
            body { 
                color: $titleHex; 
                background-color: transparent; 
                margin: 0; 
                padding: 0; 
                font-family: sans-serif; 
                font-size: 17px; 
                font-weight: bold;
                white-space: nowrap; 
                overflow: hidden; 
                text-overflow: ellipsis;
                display: block;
                width: 100vw;
            }
        </style>
        </head>
        <body>${if (note.title.isBlank()) "New Entry" else note.title}</body>
        </html>
        """.trimIndent()
    }

    val contentHtml = remember(note.content, dateColor) {
        """
        <html>
        <head>
        <style>
            body { 
                color: $contentHex; 
                background-color: transparent; 
                margin: 0; 
                padding: 0; 
                font-family: sans-serif; 
                font-size: 14px;
                white-space: nowrap; 
                overflow: hidden; 
                text-overflow: ellipsis;
                opacity: 0.6;
                display: block;
                width: 100vw;
            }
        </style>
        </head>
        <body>${if (note.content.isBlank()) "No additional text" else note.content.replace("\n", " ")}</body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onGloballyPositioned { coordinates ->
                itemBounds = coordinates.boundsInRoot()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(finalCardColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Content Layer (WebViews are here, but won't catch clicks)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = false
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false
                                setOnTouchListener { _, _ -> false }
                                isFocusable = false
                                isClickable = false
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, titleHtml, "text/html", "utf-8", null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                    )

                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateTimeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = dateColor.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = false
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false
                                setOnTouchListener { _, _ -> false }
                                isFocusable = false
                                isClickable = false
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, contentHtml, "text/html", "utf-8", null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                    )
                }
            }
        }

        // Overlay Layer (Invisible, but captures all interactions)
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = {
                        isPressed = false
                        onLongClick(itemBounds)
                    }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                isPressed = true
                            } else if (change != null && !change.pressed) {
                                isPressed = false
                            }
                        }
                    }
                }
        )
    }
}

@Composable
fun PreviewOverlay(
    state: NotePreviewState,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
    destructiveColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onArchive: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val animProgress = remember { Animatable(0f) }

    fun startDismiss(action: () -> Unit = {}) {
        scope.launch {
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            )
            action()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
        )
    }

    val dateTimeStr = remember(state.note.date) {
        val dateObj = Date(state.note.date)
        val format = "dd MMMM yyyy, HH:mm"
        SimpleDateFormat(format, Locale.getDefault()).format(dateObj)
    }

    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val initialTopPx = state.initialBounds.top
    val targetTopPx = with(density) { 48.dp.toPx() }
    val currentTopPx = lerp(initialTopPx, targetTopPx, animProgress.value)

    val initialLeftPx = state.initialBounds.left
    val initialWidthPx = state.initialBounds.width
    val targetLeftPx = (screenWidthPx - initialWidthPx) / 2f
    val currentLeftPx = lerp(initialLeftPx, targetLeftPx, animProgress.value)

    val menuHeight = 300.dp
    val spacerHeight = 16.dp
    val bottomMargin = 32.dp

    val maxCardHeightPx = screenHeightPx - targetTopPx - with(density) { (menuHeight + spacerHeight + bottomMargin).toPx() }

    val initialHeightPx = state.initialBounds.height
    val targetHeightPx = maxCardHeightPx.coerceAtLeast(initialHeightPx)
    val currentHeightPx = lerp(initialHeightPx, targetHeightPx, animProgress.value)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * animProgress.value))
            .pointerInput(Unit) {
                detectTapGestures { startDismiss() }
            }
    ) {
        Column(
            modifier = Modifier
                .width(with(density) { initialWidthPx.toDp() })
                .align(Alignment.TopStart)
                .offset { IntOffset(currentLeftPx.roundToInt(), currentTopPx.roundToInt()) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { currentHeightPx.toDp() })
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardColor)
                    .padding(20.dp)
            ) {
                // Content with WebView
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (state.note.title.isBlank()) "New Entry" else state.note.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.note.pinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = accentColor,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dateTimeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryColor,
                        fontSize = 13.sp
                    )

                    val contentAlpha = animProgress.value.coerceIn(0f, 1f)
                    if (contentAlpha > 0.05f) {
                        Spacer(modifier = Modifier.height(12.dp))

                        val hexColor = String.format("#%06X", (0xFFFFFF and textColor.toArgb()))
                        val htmlContent = """
                            <html>
                            <head>
                            <style>
                                body { color: $hexColor; background-color: transparent; margin: 0; padding: 0; font-family: sans-serif; font-size: 16px; }
                                a { color: #632F96; }
                            </style>
                            </head>
                            <body>
                                ${state.note.content}
                            </body>
                            </html>
                        """.trimIndent()

                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    settings.javaScriptEnabled = false
                                    settings.loadWithOverviewMode = true
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                            },
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }

                // Overlay for click capture on Preview
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { startDismiss { onEdit() } }
                )
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally)
                    .alpha(animProgress.value)
                    .scale(0.8f + (0.2f * animProgress.value))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardColor)
                ) {
                    ActionMenuItem(
                        label = if (state.note.pinned) "Unpin" else "Pin",
                        icon = if (state.note.pinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                        textColor = textColor,
                        onClick = { startDismiss { onPin() } }
                    )
                    Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                    ActionMenuItem(
                        label = "Share",
                        icon = Icons.Default.Share,
                        textColor = textColor,
                        onClick = { startDismiss { onShare() } }
                    )
                    Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                    ActionMenuItem(
                        label = "Archive",
                        icon = Icons.Default.Archive,
                        textColor = textColor,
                        onClick = { startDismiss { onArchive() } }
                    )
                    Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                    ActionMenuItem(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        textColor = destructiveColor,
                        iconTint = destructiveColor,
                        onClick = { startDismiss { onDelete() } },
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    label: String,
    icon: ImageVector,
    textColor: Color,
    iconTint: Color = textColor,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}