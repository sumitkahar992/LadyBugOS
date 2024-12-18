package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Note
import javax.inject.Inject

class UpdateNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(notes: List<Note>) {
        repository.updateNotes(notes)
    }
} 