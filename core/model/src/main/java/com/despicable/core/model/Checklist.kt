package com.despicable.core.model

import kotlinx.serialization.Serializable


@Serializable
data class Checklist(
    val id: Long = 0,
    val noteId: Long,
    val content: String,
    val isChecked: Boolean = false,
    val position: Int,
)

