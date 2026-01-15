package com.example.palmdown.ui.notes

import android.content.Intent
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import com.example.palmdown.ui.editor.EditorActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val repository = remember { NotesRepository() }
    val scope = rememberCoroutineScope()

    var notesList by remember { mutableStateOf(listOf<Notes>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshNotes() {
        isLoading = true
        scope.launch {
            try {
                notesList = repository.getAllNotes()
            } catch (e: Exception) {
                Log.e("NotesScreen", "Error: ${e.message}")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, EditorActivity::class.java)
                    context.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else if (notesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = notesList, key = { it.id }) { note ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
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
                                val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                                        dismissState.progress > 0f

                                val color by animateColorAsState(
                                    if (isSwiping) Color(0xFFE57373) else Color.Transparent,
                                    label = "bgColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(20.dp, 2.dp).background(Color.White))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(modifier = Modifier.size(16.dp, 18.dp).background(Color.Transparent).padding(1.dp)) {
                                            Box(modifier = Modifier.fillMaxSize().background(Color.White))
                                        }
                                    }
                                }
                            }
                        ) {
                            NoteCard(note = note) {
                                val intent = Intent(context, EditorActivity::class.java).apply {
                                    putExtra("NOTE_ID", note.id)
                                    putExtra("NOTE_TITLE", note.title)
                                    putExtra("NOTE_CONTENT", note.content)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: Notes, onClick: () -> Unit) {
    val dateTimeData = remember(note) {
        val dateObj = Date(note.date)
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALY)
        Pair(dateFormatter.format(dateObj), timeFormatter.format(dateObj))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (note.title.isBlank()) "No title" else note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (note.title.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (note.content.isBlank()) "No content" else note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateTimeData.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
                Text(
                    text = dateTimeData.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )
            }
        }
    }
}