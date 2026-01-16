package com.example.palmdown.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palmdown.model.Settings
import com.example.palmdown.model.Language
import com.example.palmdown.repository.SettingsRepository
import com.example.palmdown.repository.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = SettingsRepository(),
    private val languageRepository: LanguageRepository = LanguageRepository()
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    private val _availableLanguages = MutableStateFlow<List<Language>>(emptyList())
    val availableLanguages: StateFlow<List<Language>> = _availableLanguages

    init {
        viewModelScope.launch {
            settingsRepository.getSettings()?.let { _settings.value = it }
        }

        viewModelScope.launch {
            _availableLanguages.value = languageRepository.getLanguages()
        }
    }

    private fun save() {
        viewModelScope.launch {
            settingsRepository.saveSettings(_settings.value)
        }
    }

    fun addKeyword(word: String) {
        val normalized = word.trim()
        if (normalized.isBlank()) return
        if (_settings.value.keywords.contains(normalized)) return

        _settings.value = _settings.value.copy(
            keywords = _settings.value.keywords + normalized
        )
        save()
    }

    fun removeKeyword(word: String) {
        _settings.value = _settings.value.copy(
            keywords = _settings.value.keywords - word
        )
        save()
    }

    fun toggleLanguage(code: String) {
        val current = _settings.value.languages.toMutableList()

        if (current.contains(code)) {
            current.remove(code)
        } else if (current.size < 5) {
            current.add(code)
        }

        _settings.value = _settings.value.copy(languages = current)
        save()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
        save()
    }

    fun setNotificationsPerDay(count: Int) {
        _settings.value = _settings.value.copy(
            notificationsPerDay = count.coerceIn(1, 10)
        )
        save()
    }
}