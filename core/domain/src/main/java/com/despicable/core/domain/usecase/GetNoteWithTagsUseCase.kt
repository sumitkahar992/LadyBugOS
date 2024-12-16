package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.NoteWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteWithTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    operator fun invoke(id: Long): Flow<NoteWithTags?> =
        repository.getNoteWithTagsById(id)
} 