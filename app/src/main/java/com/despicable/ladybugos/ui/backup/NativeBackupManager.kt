package com.despicable.ladybugos.ui.backup

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Keep
@Serializable
data class BackupData(
    val notes: List<com.despicable.database.model.NoteEntity>,
    val tags: List<com.despicable.database.model.TagEntity>,
    val noteTagCrossRefs: List<com.despicable.database.model.NoteTagCrossRef>,
)

class NativeBackupManager(
    private val context: Context,
    private val database: com.despicable.database.model.NoteDatabase
) {
    private val bufferSize = 8192 // 8KB buffer size
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun createBackup(destinationUri: Uri): Flow<BackupResults> = channelFlow {
        send(BackupResults.Progress(0))
        delay(1000) // Initial delay

        try {
            withContext(Dispatchers.IO) {
                // Collect all data
                val notes = database.noteDao().getAllNotesStream().firstOrNull() ?: emptyList()
                send(BackupResults.Progress(25))
                delay(1000)

                val tags = database.tagDao().getAllTags().firstOrNull() ?: emptyList()
                send(BackupResults.Progress(50))
                delay(1000)

                val crossRefs =
                    database.noteTagCrossRefDao().getAllCrossRefs().firstOrNull() ?: emptyList()
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

    fun restoreBackup(backupUri: Uri): Flow<BackupResults> = channelFlow {
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
                    database.withTransaction {
                        database.noteDao().mergeBackupData(
                            notes = data.notes,
                            tags = data.tags,
                            crossRefs = data.noteTagCrossRefs
                        )
                    }

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
}

