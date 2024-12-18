package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.NoteWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class GetNoteWithTagsByIdUseCase @Inject constructor(
    private val repository: NoteRepository
){
    operator fun invoke(noteId: Long): Flow<NoteWithTags?> {
        return repository.getNoteWithTagsById(noteId)
    }

}