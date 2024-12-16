package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Note
import javax.inject.Inject

class UpdateNotesUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(notes: List<Note>) {
        repository.updateNotes(notes)
    }
} 