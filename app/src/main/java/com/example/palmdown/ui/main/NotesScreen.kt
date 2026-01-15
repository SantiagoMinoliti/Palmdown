package com.example.palmdown.ui.notes

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import com.example.palmdown.ui.editor.EditorActivity
import kotlinx.coroutines.launch

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
                val fetchedNotes = repository.getAllNotes()
                Log.d("NotesScreen", "Note recuperate: ${fetchedNotes.size}")
                notesList = fetchedNotes
            } catch (e: Exception) {
                Log.e("NotesScreen", "Errore nel recupero: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        refreshNotes()
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Le tue Note",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { refreshNotes() }) {
                    Text("🔄")
                }

                Button(onClick = {
                    val intent = Intent(context, EditorActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text("+")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notesList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna nota trovata. Verifica Firebase!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notesList) { note ->
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

@Composable
fun NoteCard(note: Notes, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (note.title.isEmpty()) "(Senza Titolo)" else note.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (note.content.isEmpty()) "Nessun contenuto" else note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}