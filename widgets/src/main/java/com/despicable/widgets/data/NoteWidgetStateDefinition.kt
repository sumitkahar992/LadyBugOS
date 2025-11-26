package com.despicable.widgets.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.despicable.widgets.model.WidgetNote
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// TODO
object NoteWidgetStateDefinition : GlanceStateDefinition<Map<Long, WidgetNote>> {

    private const val DATA_STORE_FILENAME = "note_widgets"
    private val Context.datastore by dataStore(DATA_STORE_FILENAME, NoteSerializer)

    override suspend fun getDataStore(
        context: Context,
        fileKey: String
    ): DataStore<Map<Long, WidgetNote>> = context.datastore

    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(DATA_STORE_FILENAME)


    suspend fun updateNote(context: Context, note: WidgetNote) {
        getDataStore(context, DATA_STORE_FILENAME).updateData { notes ->
            notes.toMutableMap().apply {
                this[note.id] = note
            }
        }
    }

    suspend fun getNoteById(context: Context, noteId: Long): WidgetNote? {
        return getDataStore(context, DATA_STORE_FILENAME).data.first()[noteId]
    }

    suspend fun deleteNote(context: Context, noteId: Long) {
        getDataStore(context, DATA_STORE_FILENAME).updateData { notes ->
            notes.toMutableMap().apply {
                remove(noteId)
            }
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    object NoteSerializer : Serializer<Map<Long, WidgetNote>> {

        override val defaultValue: Map<Long, WidgetNote>
            get() = emptyMap()

        override suspend fun readFrom(input: InputStream): Map<Long, WidgetNote> =
            Json.decodeFromStream(input)

        override suspend fun writeTo(t: Map<Long, WidgetNote>, output: OutputStream) =
            Json.encodeToStream(t, output)

    }

}