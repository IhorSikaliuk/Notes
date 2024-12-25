package com.example.notes

import java.util.Date

// Note Data Model
data class Note(
    val title: String = "",
    val modifiedAt: Date = Date(),
    val id: String = "",
    val records: List<Record> = emptyList()
)

// Record data class
data class Record(
    val type: String = "text", // "text" or "checkbox"
    val content: String = "",
    val isChecked: Boolean? = null,
    val order: Int = 0,
)