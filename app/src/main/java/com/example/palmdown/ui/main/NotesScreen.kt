package com.example.palmdown.ui.notes

import android.content.Intent
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import com.example.palmdown.ui.editor.EditorActivity
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

    LifecycleResumeEffect(Unit) {
        refreshNotes()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = bgDark,
            floatingActionButton = {
                if (previewState == null) {
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
                        Text(
                            text = "Your Space",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 34.sp,
                            color = textPrimary,
                            letterSpacing = (-0.5).sp
                        )
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
                    }

                    AnimatedVisibility(
                        visible = searchExpanded,
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
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
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
                                        onClick = {
                                            val intent = Intent(context, EditorActivity::class.java).apply {
                                                putExtra("NOTE_ID", note.id)
                                                putExtra("NOTE_TITLE", note.title)
                                                putExtra("NOTE_CONTENT", note.content)
                                            }
                                            context.startActivity(intent)
                                        },
                                        onLongClick = { bounds ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewState = NotePreviewState(note, bounds)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleStyleNoteCard(
    note: Notes,
    cardColor: Color,
    textColor: Color,
    dateColor: Color,
    accentColor: Color,
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
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onGloballyPositioned { coordinates ->
                itemBounds = coordinates.boundsInRoot()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
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
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = if (note.title.isBlank()) "New Entry" else note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
                Text(
                    text = if (note.content.isBlank()) "No additional text" else note.content.replace("\n", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = dateColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
    val screenHeight = configuration.screenHeightDp.dp

    val initialTop = with(density) { state.initialBounds.top.toDp() }
    val targetTop = 48.dp
    val currentTop = androidx.compose.ui.unit.lerp(initialTop, targetTop, animProgress.value)

    val menuHeight = 300.dp
    val spacerHeight = 16.dp
    val bottomMargin = 32.dp

    val maxCardHeight = screenHeight - targetTop - menuHeight - spacerHeight - bottomMargin

    val initialHeight = with(density) { state.initialBounds.height.toDp() }
    val targetHeight = maxCardHeight.coerceAtLeast(initialHeight)
    val currentHeight = androidx.compose.ui.unit.lerp(initialHeight, targetHeight, animProgress.value)

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
                .width(with(density) { state.initialBounds.width.toDp() })
                .align(Alignment.TopCenter)
                .offset(y = currentTop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardColor)
                    .clickable { startDismiss { onEdit() } }
                    .padding(20.dp)
            ) {
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
                        Text(
                            text = state.note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.9f),
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .weight(1f)
                        )
                    }
                }
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