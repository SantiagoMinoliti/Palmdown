package com.example.palmdown.ui.notes

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import com.example.palmdown.ui.editor.EditorActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

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

    val bgDark = Color(0xFF121212)
    val cardDark = Color(0xFF1C1C1E)
    val accentPurple = Color(0xFF632F96)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFF8E8E93)
    val searchBarBg = Color(0xFF2C2C2E)
    val destructiveRed = Color(0xFFFF453A)

    val filteredNotes by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) allNotes
            else allNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun refreshNotes() {
        isLoading = true
        scope.launch {
            try {
                allNotes = repository.getAllNotes().sortedByDescending { it.date }
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

    Scaffold(
        containerColor = bgDark,
        floatingActionButton = {
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
                    // Spacer aumentato per distanziare la barra dal titolo
                    Column {
                        Spacer(modifier = Modifier.height(32.dp))
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = filteredNotes, key = { it.id }) { note ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        repository.deleteNote(note.id)
                                        refreshNotes()
                                    }
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val density = LocalDensity.current
                                // Calcolo l'offset reale per un feedback immediato
                                val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                                val threshold = with(density) { 90.dp.toPx() } // Rosso pieno a 90dp di swipe
                                val alpha = (abs(offset) / threshold).coerceIn(0f, 1f)

                                val interpolatedColor = destructiveRed.copy(alpha = alpha)

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
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White.copy(alpha = alpha),
                                            modifier = Modifier.scale(0.8f + (alpha * 0.2f))
                                        )
                                    }
                                }
                            }
                        ) {
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
                                onDelete = {
                                    scope.launch {
                                        repository.deleteNote(note.id)
                                        refreshNotes()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleStyleNoteCard(
    note: Notes,
    cardColor: Color,
    textColor: Color,
    dateColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

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
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            pressOffset = change.position
                        }
                    }
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    menuExpanded = true
                }
            )
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = if (note.title.isBlank()) "New Entry" else note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
                isPressed = false
            },
            offset = DpOffset(x = with(density) { pressOffset.x.toDp() }, y = 0.dp),
            modifier = Modifier
                .background(Color(0xFF2C2C2E))
                .clip(RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Edit Note", color = Color.White) },
                onClick = {
                    menuExpanded = false
                    isPressed = false
                    onClick()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor) }
            )
            DropdownMenuItem(
                text = { Text("Share", color = Color.White) },
                onClick = {
                    menuExpanded = false
                    isPressed = false
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = accentColor) }
            )
            Divider(color = Color(0xFF3A3A3C), thickness = 0.5.dp)
            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFFF453A)) },
                onClick = {
                    menuExpanded = false
                    isPressed = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A)) }
            )
        }
    }
}