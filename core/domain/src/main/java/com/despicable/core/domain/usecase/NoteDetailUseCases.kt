package com.despicable.core.domain.usecase

import javax.inject.Inject


data class NoteDetailUseCases @Inject constructor(
    val getNoteByIdUseCase: GetNoteByIdUseCase,
    val getNoteWithTags: GetNoteWithTagsUseCase,
    val saveNote: SaveNoteUseCase,
    val updateNoteReminder: UpdateNoteReminderUseCase,
    val deleteNote: DeleteNoteUseCase,
    val getAllTags: GetAllTagsUseCase,
    val updateNoteWithTags: UpdateNoteTagsUseCase,
    val updateNotes: UpdateNotesUseCase,
    val insertNoteWithTags: InsertNoteWithTagsUseCase,
    val getNoteWithTagsById: GetNoteWithTagsByIdUseCase

)