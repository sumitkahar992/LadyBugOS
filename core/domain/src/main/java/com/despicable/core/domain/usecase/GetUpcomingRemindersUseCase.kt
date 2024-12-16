package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import com.despicable.model.NoteWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUpcomingRemindersUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    operator fun invoke(): Flow<List<NoteWithTags>> = repository.getUpcomingReminders()
} 