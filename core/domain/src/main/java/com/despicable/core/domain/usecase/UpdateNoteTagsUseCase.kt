package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Note
import javax.inject.Inject

class UpdateNoteTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(note: Note, tagIds: List<Long>) {
        repository.updateNoteWithTags(note, tagIds)
    }
} 