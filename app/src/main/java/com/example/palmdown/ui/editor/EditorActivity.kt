package com.example.palmdown.ui.editor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = {
                                if (title.isBlank() && content.isBlank()) {
                                    onBack()
                                    return@TextButton
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
                                    if (success) onBack()
                                    else Toast.makeText(context, "Errore di salvataggio", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Title",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Content here...", color = Color.Gray.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = TextStyle(fontSize = 18.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
    }
}