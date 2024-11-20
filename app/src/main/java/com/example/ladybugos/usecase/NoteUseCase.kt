package com.example.ladybugos.usecase

import com.example.ladybugos.model.Note
import com.example.ladybugos.notification.NotificationHelper
import com.example.ladybugos.repository.NoteRepository
import com.example.ladybugos.widget.CoroutineDispatchers
import com.example.ladybugos.widget.DefaultCoroutineDispatchers
import com.example.ladybugos.widget.WidgetUpdater
import kotlinx.coroutines.withContext


class UpdateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val notificationHelper: NotificationHelper,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    suspend operator fun invoke(
        note: Note,
        tagIds: List<Long>,
        updateReminder: Boolean = false
    ) = withContext(dispatchers.io) {
        // Update note and tags in repository
        noteRepository.updateNoteWithTags(note, tagIds)

        // Handle reminder if needed
        if (updateReminder) {
            note.reminderDate?.let { reminderDate ->
                val currentTime = System.currentTimeMillis()
                if (reminderDate > currentTime) {
                    notificationHelper.scheduleNotification(note)
                } else {
                    notificationHelper.cancelNotification(note.id)
                }
            } ?: notificationHelper.cancelNotification(note.id)
        }

        // Update widgets
        widgetUpdater.updateSingleWidget(note)
    }
}


class BatchUpdateNoteUseCase(
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    suspend operator fun invoke(
        notes: List<Note>
    ) = withContext(dispatchers.io) {
        // Update notes in repository
        noteRepository.updateNotes(notes)

        // Update only relevant widgets
        widgetUpdater.updateWidgetsForNotes(notes)
    }
}

class DeleteNoteUseCase(
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val notificationHelper: NotificationHelper,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    suspend operator fun invoke(note: Note) = withContext(dispatchers.io) {
        // Delete note from repository
        noteRepository.deleteNote(note)

        // Cancel any existing notifications
        notificationHelper.cancelNotification(note.id)

        // Update widgets if note was in trash
        if (note.isTrashed) {
//            widgetUpdater.handleNoteDeleted()
        }
    }
}

class EmptyTrashUseCase(
    private val noteRepository: NoteRepository,
    private val widgetUpdater: WidgetUpdater,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) {
    suspend operator fun invoke() = withContext(dispatchers.io) {
        // Empty trash in repository
        noteRepository.emptyTrashWithTags()

        // Update widgets after emptying trash
//        widgetUpdater.handleNoteDeleted()
    }
}


/*
class NotesUseCasesImpl(private val repository: NoteRepository) : NotesUseCasesInterface {
    override fun getAllNotes(): Flow<List<Note>> = repository.getAllNotes

    override suspend fun addNote(note: Note, tagIds: List<Long>): Long =
        repository.insertNote(note, tagIds)

    override suspend fun updateNote(note: Note, tagIds: List<Long>) =
        repository.updateNote(note, tagIds)

    override suspend fun updateNotes(notes: List<Note>) = repository.updateNotes(notes)

    override suspend fun deleteNote(note: Note) = repository.delete(note)

    override fun getNoteById(id: Long): Flow<Note?> = repository.getNoteById(id)

    override fun searchNotes(query: String): Flow<List<Note>> = repository.searchNotes(query)

    override suspend fun emptyTrash() = repository.emptyTrash()

}

interface NotesUseCasesInterface {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun addNote(note: Note, tagIds: List<Long>): Long
    suspend fun updateNote(note: Note, tagIds: List<Long>)
    suspend fun updateNotes(notes: List<Note>)
    suspend fun deleteNote(note: Note)
    fun getNoteById(id: Long): Flow<Note?>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun emptyTrash()

}
*/

