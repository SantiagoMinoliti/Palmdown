package com.example.palmdown.model

import java.util.Date

data class News(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Date? = null,
    val country: String = "",
    val url: String = "",

    val fetchedAt: Date? = null,

    val keywords: List<String> = emptyList(),
    val creator: List<String> = emptyList(),
    val categories: List<String> = emptyList(),

    val imageUrl: String = "",
    val videoUrl: String = "",

    val sourceId: String = "",
    val sourceName: String = "",
    val sourceIcon: String = ""
)
