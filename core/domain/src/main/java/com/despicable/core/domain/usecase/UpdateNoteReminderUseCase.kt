package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepositoryInterface
import javax.inject.Inject

class UpdateNoteReminderUseCase @Inject constructor(
    private val repository: NoteRepositoryInterface
) {
    suspend operator fun invoke(noteId: Long, reminderDate: Long?) {
        repository.updateNoteReminder(noteId, reminderDate)
    }
} 