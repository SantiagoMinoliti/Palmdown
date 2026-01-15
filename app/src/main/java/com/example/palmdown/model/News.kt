package com.example.palmdown.model

import java.util.Date

data class News(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Date? = null,
    val country: String = "",
    val url: String = ""
)

