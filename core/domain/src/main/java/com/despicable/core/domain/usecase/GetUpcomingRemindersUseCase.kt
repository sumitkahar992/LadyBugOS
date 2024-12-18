package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.NoteWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUpcomingRemindersUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteWithTags>> = repository.getUpcomingReminders()
} 