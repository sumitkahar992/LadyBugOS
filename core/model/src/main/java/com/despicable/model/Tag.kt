package com.despicable.model

import androidx.annotation.Keep

@Keep
data class Tag(
    val id: Long = 0,
    val name: String,
)
