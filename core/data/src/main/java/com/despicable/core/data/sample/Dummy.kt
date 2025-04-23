package com.despicable.core.data.sample

import android.content.Context
import com.despicable.core.data.model.toDomain
import com.despicable.core.data.repository.NoteRepository
import com.despicable.core.database.model.NoteEntity
import com.despicable.core.database.model.TagEntity
import com.despicable.core.model.NoteContent
import com.despicable.core.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

class LoadSampleDataUseCase(
    private val repo: NoteRepository,
    private val assetLoader: AssetLoader
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            if (repo.getAllNotes().first().isEmpty()
                && repo.getAllTags().first().isEmpty()
            ) {

                val sampleTags = createSampleTags()
                sampleTags.forEach { tag ->
                    repo.insertTag(tag.toDomain())
                }

                val sampleNotes = getSampleNotes()
                    .map {
                        it.copy(
                            // Use color index instead of ARGB values for better theme compatibility
                            lightColor = Random.nextInt(0, 18), // 0-9 for our 10 note colors
                            updateDate = generateRandomDate(),
                            creationDate = generateRandomDate(),
                            reminderDate = generateRandomDate(),
                            pinnedDate = generateRandomDate()

                        )
                    }
                sampleNotes.forEach { note ->
                    val randomTags = sampleTags.shuffled().take(Random.nextInt(1, 4))

                    val checklistItems = if (note.noteType == NoteType.CHECKLIST) {
                        val content = note.content as NoteContent.ChecklistItems
                        content.items
                    } else {
                        emptyList()
                    }

                    repo.insertNoteWithTagsChecklist(
                        note.toDomain(),
                        randomTags.map { it.id },
                        checklistItems,
                        emptyList()
                    )
                }
            }
        }
    }

    private fun createSampleTags(): List<TagEntity> {
        return listOf(
            TagEntity(1, "Work"),
            TagEntity(2, "Personal"),
            TagEntity(3, "Ideas"),
            TagEntity(4, "Todo"),
            TagEntity(5, "Important"),
            TagEntity(6, "Project"),
            TagEntity(7, "Meeting"),
            TagEntity(8, "Family"),
            TagEntity(9, "Travel"),
            TagEntity(10, "Shopping"),
            TagEntity(11, "Health"),
            TagEntity(12, "Finance"),
            TagEntity(13, "Education"),
            TagEntity(14, "CODEE"),
            TagEntity(15, "WORKEE"),
            TagEntity(16, "JOBEE"),
            TagEntity(17, "SLEEPY"),
            TagEntity(18, "NO-FAP"),
            TagEntity(19, "DEVIL"),
            TagEntity(20, "ANGEL"),
            TagEntity(21, "HINDU"),
            TagEntity(22, "SANATANI"),
            TagEntity(23, "SHIVA"),
            TagEntity(24, "KRISHNA"),
        )
    }

    private suspend fun getSampleNotes(): List<NoteEntity> {
//        val json = assetLoader.loadTextAsset("noteee.json")
        val json = assetLoader.loadTextAsset("codeee.json")
        return Json.decodeFromString<List<NoteEntity>>(json)
    }
}

// Replace string date generation with timestamp generation
fun generateRandomDate(): Instant {
    val currentInstant = Clock.System.now()
    val randomDaysAgo = Random.nextInt(0, 365)
    return currentInstant - randomDaysAgo.days
}

interface AssetLoader {
    suspend fun loadTextAsset(fileName: String): String
}

class AssetLoaderImpl(private val context: Context) : AssetLoader {
    override suspend fun loadTextAsset(fileName: String): String = withContext(Dispatchers.IO) {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}


// 14 predefined tag headers
val tags = listOf(
    TagEntity(1, "Work"),
    TagEntity(2, "Personal"),
    TagEntity(3, "Ideas"),
    TagEntity(4, "Todo"),
    TagEntity(5, "Important"),
    TagEntity(6, "Project"),
    TagEntity(7, "Meeting"),
    TagEntity(8, "Family"),
    TagEntity(9, "Travel"),
    TagEntity(10, "Shopping"),
    TagEntity(11, "Health"),
    TagEntity(12, "Finance"),
    TagEntity(13, "Education"),
    TagEntity(14, "Hobby")
)
