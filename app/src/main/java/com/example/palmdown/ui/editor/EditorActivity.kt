package com.example.palmdown.ui.editor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NotesRepository
import kotlinx.coroutines.launch

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val noteId = intent.getStringExtra("NOTE_ID") ?: ""
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: ""
        val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorEditScreen(
                        id = noteId,
                        initialTitle = noteTitle,
                        initialContent = noteContent,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorEditScreen(
    id: String,
    initialTitle: String,
    initialContent: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NotesRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (id.isEmpty()) "Nuova Nota" else "Modifica Nota",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Contenuto") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Button(
            onClick = {
                if (title.isBlank()) {
                    Toast.makeText(context, "Inserisci un titolo", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSaving = true
                scope.launch {
                    val noteToSave = Notes(
                        id = id,
                        title = title,
                        content = content,
                        date = System.currentTimeMillis()
                    )

                    val success = repository.saveNote(noteToSave)

                    isSaving = false
                    if (success) {
                        onBack()
                    } else {
                        Toast.makeText(context, "Errore durante il salvataggio", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Salva su Firebase")
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Annulla")
        }
    }
}