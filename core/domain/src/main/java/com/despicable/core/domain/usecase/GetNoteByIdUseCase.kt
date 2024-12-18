package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: Long): Flow<Note?> = repository.getNoteById(id)
}