package com.despicable.core.domain.usecase

/*

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import com.despicable.core.model.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}

class EmptyTrashWithTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke() {
        repository.emptyTrashWithTags()
    }
}

class GetAllNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> = repository.getAllNotes()
}


class GetAllNotesTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteWithTags>> = repository.getAllNotesWithTags()
}

class GetAllTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
}

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: Long): Flow<Note?> = repository.getNoteById(id)
}

class GetNoteWithTagsByIdUseCase @Inject constructor(
    private val repository: NoteRepository
){
    operator fun invoke(noteId: Long): Flow<NoteWithTags?> {
        return repository.getNoteWithTagsById(noteId)
    }

}

class GetNoteWithTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: Long): Flow<NoteWithTags?> =
        repository.getNoteWithTagsById(id)
}

class GetUpcomingRemindersUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteWithTags>> = repository.getUpcomingReminders()
}


class InsertNoteWithTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note, tagIds: List<Long>): Long =
        repository.insertNoteWithTags(note, tagIds)
}

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note, tagIds: List<Long>): Long =
        if (note.id == 0L) {
            repository.insertNoteWithTags(note, tagIds)
        } else {
            repository.updateNoteWithTags(note, tagIds)
            note.id
        }
}

class GetInsertTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag): Long = repository.insertTag(tag)
}


class GetUpdateTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag) = repository.updateTag(tag)
}


class GetDeleteTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tag: Tag) = repository.deleteTag(tag)
}

class UpdateNoteReminderUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, reminderDate: Long?) {
        repository.updateNoteReminder(noteId, reminderDate)
    }
}

class UpdateNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(notes: List<Note>) {
        repository.updateNotes(notes)
    }
}

class UpdateNoteTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note, tagIds: List<Long>) {
        repository.updateNoteWithTags(note, tagIds)
    }
}


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



