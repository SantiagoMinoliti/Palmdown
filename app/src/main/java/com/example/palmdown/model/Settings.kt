package com.example.palmdown.model

data class Settings(
    val keywords: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val notificationsPerDay: Int = 1
)
