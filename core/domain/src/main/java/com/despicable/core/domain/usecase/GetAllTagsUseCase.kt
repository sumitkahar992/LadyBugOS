package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
} 