package com.example.palmdown.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLogoutClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allLanguages by viewModel.availableLanguages.collectAsState()
    val focusManager = LocalFocusManager.current

    var keywordInput by remember { mutableStateOf("") }
    var languageSearch by remember { mutableStateOf("") }
    var languagesExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                languagesExpanded = false
                focusManager.clearFocus()
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }

            item {
                Text("Keywords", style = MaterialTheme.typography.titleMedium)

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
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    settings.keywords.forEach { keyword ->
                        Chip(label = keyword) { viewModel.removeKeyword(it) }
                    }
                }
            }

            item {
                Text("Languages (max 5)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = languageSearch,
                    onValueChange = {
                        languageSearch = it
                        languagesExpanded = settings.languages.size < 5
                    },
                    enabled = settings.languages.size < 5,
                    label = { Text("Search languages") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (languagesExpanded) {
                item {
                    LanguageDropdown(
                        search = languageSearch,
                        selected = settings.languages,
                        languages = allLanguages.associate { it.code to it.name },
                        onSelect = {
                            viewModel.toggleLanguage(it)
                            languageSearch = ""
                            languagesExpanded = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    settings.languages.forEach { code ->
                        val name = allLanguages.firstOrNull { it.code == code }?.name ?: code
                        Chip(label = name) {
                            viewModel.toggleLanguage(code)
                        }
                    }
                }
            }

            item { Divider() }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable notifications", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }

            if (settings.notificationsEnabled) {
                item {
                    Text("Notifications per day")
                    Slider(
                        value = settings.notificationsPerDay.toFloat(),
                        onValueChange = { viewModel.setNotificationsPerDay(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 9
                    )
                    Text("${settings.notificationsPerDay} per day", style = MaterialTheme.typography.labelMedium)
                }
            }

            item { Divider() }

            item {
                Text(
                    text = "Logout",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { onLogoutClick() }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    onRemove: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✕",
                color = androidx.compose.ui.graphics.Color(0xFF9F8FE0),
                modifier = Modifier.clickable { onRemove(label) }
            )
        }
    }
}

@Composable
private fun LanguageDropdown(
    search: String,
    selected: List<String>,
    languages: Map<String, String>,
    onSelect: (String) -> Unit
) {
    val filtered = languages
        .filter { it.value.contains(search, ignoreCase = true) }
        .filterNot { selected.contains(it.key) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        if (filtered.isEmpty()) {
            Text(
                text = "No language found",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            filtered.forEach { (code, name) ->
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
}
