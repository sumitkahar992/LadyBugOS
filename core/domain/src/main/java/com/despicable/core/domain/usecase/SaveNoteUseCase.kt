package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Note
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(note: Note, tagIds: List<Long>): Long =
        if (note.id == 0L) {
            repository.insertNoteWithTags(note, tagIds)
        } else {
            repository.updateNoteWithTags(note, tagIds)
            note.id
        }
} 