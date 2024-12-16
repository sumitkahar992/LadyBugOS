package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Note
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}

class EmptyTrashWithTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke() {
        repository.emptyTrashWithTags()
    }
}


