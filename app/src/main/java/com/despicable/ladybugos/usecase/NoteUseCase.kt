package com.despicable.ladybugos.usecase

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

