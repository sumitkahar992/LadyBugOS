package com.despicable.core.data.repository

import android.content.Context
import android.net.Uri
import com.despicable.core.database.NoteDatabase
import com.despicable.core.database.dao.ChecklistDao
import com.despicable.core.database.dao.NoteDao
import com.despicable.core.database.dao.TagDao
import com.despicable.core.database.model.BackupData
import com.despicable.core.database.model.BackupResults
import com.despicable.core.database.model.ChecklistEntity
import com.despicable.core.database.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val context: Context,
    private val database: NoteDatabase,
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistDao
) : BackupRepository {

    private val bufferSize = 16384 // 16KB buffer for better performance
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true // More resilient to schema changes
    }

    override fun createBackup(destinationUri: Uri): Flow<BackupResults> = channelFlow {
        send(BackupResults.Progress(0))

        try {
            withContext(Dispatchers.IO) {
                // Collect ALL notes including trashed and archived
                val notes = noteDao.getAllNotesForBackup().first()
                send(BackupResults.Progress(25))

                val tags = tagDao.getAllTags().first()
                send(BackupResults.Progress(40))

                val crossRefs = noteDao.getAllCrossRefs().first()
                send(BackupResults.Progress(60))

                // Get checklist items for ALL notes
                val checklistItems = checklistDao.getAllChecklistItems().first()
                send(BackupResults.Progress(75))

                // Create backup data
                val backupData = BackupData(
                    notes = notes,
                    tags = tags,
                    noteTagCrossRefs = crossRefs,
                    checklistItems = checklistItems
                )

                // Serialize to JSON
                val backupJson = json.encodeToString(backupData)
                send(BackupResults.Progress(85))

                // Write to zip file
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(outputStream.buffered(bufferSize)).use { zipOut ->
                        // Write backup data
                        zipOut.putNextEntry(ZipEntry("backup.json"))
                        zipOut.write(backupJson.toByteArray())
                        zipOut.closeEntry()

                        // Backup media files if any
                        val mediaDir = context.getExternalFilesDir(null)
                        if (mediaDir?.exists() == true) {
                            backupDirectory(mediaDir, "media", zipOut)
                        }
                    }
                } ?: throw IOException("Could not open output stream")

                send(BackupResults.Progress(100))

                send(
                    BackupResults.Success(
                        notesCount = notes.size,
                        tagsCount = tags.size,
                        operation = BackupResults.Success.Operation.BACKUP
                    )
                )
            }
        } catch (e: Exception) {
            send(BackupResults.Error(e.message ?: "Unknown error occurred during backup"))
        }
    }

    override fun restoreBackup(backupUri: Uri): Flow<BackupResults> = channelFlow {
        try {
            send(BackupResults.Progress(0))

            // Validate backup first
            if (!isValidBackup(backupUri)) {
                send(BackupResults.Error("Invalid or corrupted backup file"))
                return@channelFlow
            }

            withContext(Dispatchers.IO) {
                var backupData: BackupData? = null

                // Extract backup data
                context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                    ZipInputStream(inputStream.buffered(bufferSize)).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        while (entry != null) {
                            when {
                                entry.name == "backup.json" -> {
                                    // Read and parse backup data
                                    val jsonBytes = zipIn.readBytes()
                                    backupData = json.decodeFromString<BackupData>(
                                        jsonBytes.toString(Charsets.UTF_8)
                                    )
                                    send(BackupResults.Progress(40))
                                }

                                entry.name.startsWith("media/") -> {
                                    // Restore media files
                                    val mediaFile = File(
                                        context.getExternalFilesDir(null),
                                        entry.name.substringAfter("media/")
                                    )
                                    restoreFile(zipIn, mediaFile)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                } ?: throw IOException("Could not open input stream")

                send(BackupResults.Progress(60))

                // Restore data to database
                backupData?.let { data ->
                    // Use the enhanced mergeBackupData function
                    noteDao.mergeBackupData(
                        notes = data.notes,
                        tags = data.tags,
                        crossRefs = data.noteTagCrossRefs,
                        database = database
                    )

                    // Handle checklist items separately if they exist
                    data.checklistItems?.let { checklistItems ->
                        restoreChecklistItems(checklistItems, data.notes)
                    }

                    send(BackupResults.Progress(100))

                    send(
                        BackupResults.Success(
                            notesCount = data.notes.size,
                            tagsCount = data.tags.size,
                            operation = BackupResults.Success.Operation.RESTORE
                        )
                    )
                } ?: throw IOException("No backup data found")
            }
        } catch (e: Exception) {
            send(BackupResults.Error(e.message ?: "Unknown error occurred during restore"))
        }
    }

    override suspend fun isValidBackup(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "backup.json") {
                            return@withContext true
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun restoreChecklistItems(
        checklistItems: List<ChecklistEntity>,
        notes: List<NoteEntity>
    ) {
        // Get existing notes to map old IDs to new IDs
        val existingNotes = noteDao.getAllNotesForBackup().first()
        val noteIdMapping = mutableMapOf<Long, Long>()

        notes.forEach { backupNote ->
            val matchingNote = existingNotes.find {
                it.title == backupNote.title &&
                        it.content == backupNote.content &&
                        it.updateDate == backupNote.updateDate
            }
            if (matchingNote != null) {
                noteIdMapping[backupNote.id] = matchingNote.id
            }
        }

        // Process checklist items with the correct note IDs
        val processedItems = checklistItems.mapNotNull { item ->
            val newNoteId = noteIdMapping[item.noteId] ?: return@mapNotNull null
            item.copy(id = 0, noteId = newNoteId)
        }

        // Insert in batches for better performance
        if (processedItems.isNotEmpty()) {
            checklistDao.insertChecklistItems(processedItems)
        }
    }

    private fun backupDirectory(directory: File, basePath: String, zipOut: ZipOutputStream) {
        directory.listFiles()?.forEach { file ->
            val entryPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
            if (file.isDirectory) {
                backupDirectory(file, entryPath, zipOut)
            } else {
                addFileToZip(file, entryPath, zipOut)
            }
        }
    }

    private fun addFileToZip(file: File, entryPath: String, zipOut: ZipOutputStream) {
        if (!file.exists()) return

        FileInputStream(file).use { fileIn ->
            zipOut.putNextEntry(ZipEntry(entryPath))
            val buffer = ByteArray(bufferSize)
            var len: Int
            while (fileIn.read(buffer).also { len = it } > 0) {
                zipOut.write(buffer, 0, len)
            }
            zipOut.closeEntry()
        }
    }

    private fun restoreFile(zipIn: ZipInputStream, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fileOut ->
            val buffer = ByteArray(bufferSize)
            var len: Int
            while (zipIn.read(buffer).also { len = it } > 0) {
                fileOut.write(buffer, 0, len)
            }
        }
    }
}


/*
class BackupRepositoryImpl @Inject constructor(
    private val context: Context,
    private val database: NoteDatabase,
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val noteTagCrossRefDao: NoteTagCrossRefDao,
) : BackupRepository {

    private val bufferSize = 8192 // 8KB buffer size
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun createBackup(destinationUri: Uri): Flow<BackupResults> = channelFlow {
        send(BackupResults.Progress(0))
        delay(1000) // Initial delay

        try {
            withContext(Dispatchers.IO) {
                // Collect all data
                val notes = noteDao.getAllNotesStream().firstOrNull() ?: emptyList()
                send(BackupResults.Progress(25))
                delay(1000)

                val tags = tagDao.getAllTags().firstOrNull() ?: emptyList()
                send(BackupResults.Progress(50))
                delay(1000)

                val crossRefs =
                    noteTagCrossRefDao.getAllCrossRefs().firstOrNull() ?: emptyList()
                send(BackupResults.Progress(75))
                delay(800)

                val backupData = BackupData(notes, tags, crossRefs)
                val backupJson = json.encodeToString(backupData)

                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(outputStream.buffered(bufferSize)).use { zipOut ->
                        // Write backup data
                        zipOut.putNextEntry(ZipEntry("backup.json"))
                        zipOut.write(backupJson.toByteArray())
                        zipOut.closeEntry()

                        // Backup media files if any
                        val mediaDir = context.getExternalFilesDir(null)
                        if (mediaDir?.exists() == true) {
                            backupDirectory(mediaDir, "media", zipOut)
                        }
                    }
                } ?: throw IOException("Could not open output stream")

                send(BackupResults.Progress(100))
                delay(200)

                send(
                    BackupResults.Success(
                        notesCount = notes.size,
                        tagsCount = tags.size,
                        operation = BackupResults.Success.Operation.BACKUP
                    )
                )
            }
        } catch (e: Exception) {
            send(BackupResults.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override fun restoreBackup(backupUri: Uri): Flow<BackupResults> = channelFlow {
        try {
            send(BackupResults.Progress(0))
            delay(1000)

            withContext(Dispatchers.IO) {
                var backupData: BackupData? = null

                context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                    ZipInputStream(inputStream.buffered(bufferSize)).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        while (entry != null) {
                            when {
                                entry.name == "backup.json" -> {
                                    // Read and parse backup data
                                    val jsonBytes = zipIn.readBytes()
                                    backupData = json.decodeFromString<BackupData>(
                                        jsonBytes.toString(Charsets.UTF_8)
                                    )
                                    send(BackupResults.Progress(50))
                                    delay(1500)
                                }

                                entry.name.startsWith("media/") -> {
                                    // Restore media files
                                    val mediaFile = File(
                                        context.getExternalFilesDir(null),
                                        entry.name.substringAfter("media/")
                                    )
                                    restoreFile(zipIn, mediaFile)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                } ?: throw IOException("Could not open input stream")

                // Restore data to database
                backupData?.let { data ->
                    noteDao.mergeBackupData(
                        notes = data.notes,
                        tags = data.tags,
                        crossRefs = data.noteTagCrossRefs,
                        database = database
                    )


                    send(BackupResults.Progress(100))
                    delay(500)

                    send(
                        BackupResults.Success(
                            notesCount = data.notes.size,
                            tagsCount = data.tags.size,
                            operation = BackupResults.Success.Operation.RESTORE
                        )
                    )
                } ?: throw IOException("No backup data found")
            }
        } catch (e: Exception) {
            send(BackupResults.Error(e.message ?: "Unknown error occurred"))
        }
    }

    private fun backupDirectory(directory: File, basePath: String, zipOut: ZipOutputStream) {
        directory.listFiles()?.forEach { file ->
            val entryPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
            if (file.isDirectory) {
                backupDirectory(file, entryPath, zipOut)
            } else {
                addFileToZip(file, entryPath, zipOut)
            }
        }
    }

    private fun addFileToZip(file: File, entryPath: String, zipOut: ZipOutputStream) {
        if (!file.exists()) return

        FileInputStream(file).use { fileIn ->
            zipOut.putNextEntry(ZipEntry(entryPath))
            fileIn.copyTo(zipOut, bufferSize)
            zipOut.closeEntry()
        }
    }

    private fun restoreFile(zipIn: ZipInputStream, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fileOut ->
            zipIn.copyTo(fileOut, bufferSize)
        }
    }
}*/
