package com.despicable.core.domain.usecase

import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.model.Note
import com.despicable.core.model.NoteWithTags
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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





