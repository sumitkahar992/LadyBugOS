package com.despicable.core.data.model

import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.model.Checklist

fun ChecklistEntity.toDomain(): Checklist =
    Checklist(
        id = id,
        noteId = noteId,
        content = content,
        isChecked = isChecked,
        position = position
    )

fun Checklist.toEntity(): ChecklistEntity =
    ChecklistEntity(
        id = id,
        noteId = noteId,
        content = content,
        isChecked = isChecked,
        position = position
    )

fun List<ChecklistEntity>.toDomainList(): List<Checklist> =
    map { it.toDomain() }

fun List<Checklist>.toEntityList(): List<ChecklistEntity> =
    map { it.toEntity() }