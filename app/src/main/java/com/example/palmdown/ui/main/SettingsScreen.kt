package com.example.palmdown.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLogoutClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var keywordInput by rememberSaveable { mutableStateOf("") }
    var languageSearch by rememberSaveable { mutableStateOf("") }
    var languagesExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        Text(text = "Keywords", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
            label = { Text("Add keyword") },
            trailingIcon = {
                IconButton(onClick = {
                    val text = keywordInput.trim()
                    if (text.isNotEmpty()) {
                        viewModel.addKeyword(text)
                        keywordInput = ""
                    }
                }) {
                    Text("+", fontSize = 20.sp)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            settings.keywords.forEach { keyword ->
                AssistChip(
                    onClick = {},
                    label = { Text(keyword) },
                    trailingIcon = {
                        Text(
                            text = "✕",
                            modifier = Modifier.clickable { viewModel.removeKeyword(keyword) }
                        )
                    }
                )
            }
        }

        Text(text = "Languages (max 5)", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = languageSearch,
            onValueChange = {
                languageSearch = it
                languagesExpanded = true
            },
            label = { Text("Search languages") },
            modifier = Modifier.fillMaxWidth()
        )

        if (languagesExpanded) {
            LanguageDropdown(
                search = languageSearch,
                selected = settings.languages,
                onSelect = {
                    viewModel.toggleLanguage(it)
                    languageSearch = ""
                    languagesExpanded = false
                }
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            settings.languages.forEach { code ->
                AssistChip(
                    onClick = {},
                    label = { Text(languageMap[code] ?: code) },
                    trailingIcon = {
                        Text(
                            text = "✕",
                            modifier = Modifier.clickable { viewModel.toggleLanguage(code) }
                        )
                    }
                )
            }
        }

        Divider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Enable notifications", modifier = Modifier.weight(1f))
            Switch(
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled
            )
        }

        if (settings.notificationsEnabled) {
            Text(text = "Notifications per day")

            Slider(
                value = settings.notificationsPerDay.toFloat(),
                onValueChange = { viewModel.setNotificationsPerDay(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3
            )

            Text(text = "${settings.notificationsPerDay} per day")
        }

        Divider()

        Text(
            text = "Logout",
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            modifier = Modifier
                .clickable { onLogoutClick() }
                .padding(vertical = 12.dp)
        )
    }
}

private val languageMap = mapOf(
    "it" to "Italian",
    "en" to "English",
    "fr" to "French",
    "de" to "German",
    "es" to "Spanish",
    "af" to "Afrikaans",
    "pt" to "Portuguese",
    "nl" to "Dutch"
)

@Composable
private fun LanguageDropdown(
    search: String,
    selected: List<String>,
    onSelect: (String) -> Unit
) {
    val languages = languageMap.entries
        .filter { it.value.contains(search, ignoreCase = true) }
        .filterNot { selected.contains(it.key) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        languages.forEach { (code, name) ->
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(code) }
                    .padding(8.dp)
            )
        }
    }
}
