package com.example.palmdown.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLogoutClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var keywordInput by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Impostazioni",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- KEYWORDS ----
        OutlinedTextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
            label = { Text("Parola chiave") },
            trailingIcon = {
                IconButton(onClick = {
                    val text = keywordInput.text.trim()
                    if (text.isNotEmpty()) {
                        viewModel.addKeyword(text)
                        keywordInput = TextFieldValue("")
                    }
                }) {
                    Text("+", fontSize = 20.sp)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.keywords) { keyword ->
                KeywordPill(
                    keyword = keyword,
                    onRemove = { viewModel.removeKeyword(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- LANGUAGES ----
        Text(
            text = "Lingue (max 5)",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LanguageSelector(
            selectedLanguages = settings.languages,
            onToggle = { viewModel.toggleLanguage(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- NOTIFICATIONS ----
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Abilita notifiche",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = settings.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
        }

        if (settings.notificationsEnabled) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Notifiche al giorno")

            Slider(
                value = settings.notificationsPerDay.toFloat(),
                onValueChange = { viewModel.setNotificationsPerDay(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3
            )

            Text(
                text = "${settings.notificationsPerDay} al giorno",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ---- LOGOUT ----
        Text(
            text = "Logout",
            color = Color.Red,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogoutClick() }
                .padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun KeywordPill(
    keyword: String,
    onRemove: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.LightGray, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = keyword)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "✕",
            modifier = Modifier.clickable { onRemove(keyword) },
            color = Color.DarkGray
        )
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguages: List<String>,
    onToggle: (String) -> Unit
) {
    val languages = listOf(
        "it" to "Italiano",
        "en" to "English",
        "fr" to "Francese",
        "de" to "Tedesco",
        "es" to "Spagnolo",
        "af" to "Afrikaans"
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(languages) { (code, name) ->
            val selected = selectedLanguages.contains(code)

            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggle(code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = name,
                    color = if (selected) Color.White else Color.Black
                )
            }
        }
    }
}
