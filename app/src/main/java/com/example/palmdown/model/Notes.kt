package com.example.palmdown.model
import java.util.Date

data class Notes (
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Long = Date().time,
    val archived: Boolean = false,
    val pinned: Boolean = false
)