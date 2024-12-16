package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.Tag
import javax.inject.Inject


class GetInsertTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(tag: Tag): Long = repository.insertTag(tag)
}


class GetUpdateTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(tag: Tag) = repository.updateTag(tag)
}


class GetDeleteTagsUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(tag: Tag) = repository.deleteTag(tag)
}

