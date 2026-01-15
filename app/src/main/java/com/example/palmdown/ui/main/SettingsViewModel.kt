package com.example.palmdown.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.palmdown.model.Settings

class SettingsViewModel : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    fun addKeyword(word: String) {
        if (word.isBlank()) return
        _settings.value = _settings.value.copy(
            keywords = _settings.value.keywords + word
        )
    }

    fun removeKeyword(word: String) {
        _settings.value = _settings.value.copy(
            keywords = _settings.value.keywords - word
        )
    }

    fun toggleLanguage(code: String) {
        val current = _settings.value.languages.toMutableList()

        if (current.contains(code)) {
            current.remove(code)
        } else if (current.size < 5) {
            current.add(code)
        }

        _settings.value = _settings.value.copy(languages = current)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun setNotificationsPerDay(count: Int) {
        _settings.value = _settings.value.copy(
            notificationsPerDay = count.coerceIn(1, 5)
        )
    }
}
