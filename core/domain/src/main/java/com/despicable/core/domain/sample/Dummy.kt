package com.despicable.core.domain.sample

import android.content.Context
import com.despicable.core.database.model.TagEntity
import com.despicable.core.domain.usecase.GetAllNotesUseCase
import com.despicable.core.domain.usecase.GetAllTagsUseCase
import com.despicable.core.domain.usecase.GetInsertTagsUseCase
import com.despicable.core.domain.usecase.InsertNoteWithTagsUseCase
import com.despicable.core.model.Note
import com.despicable.core.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.random.Random

class LoadSampleDataUseCase(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val insertTagsUseCase: GetInsertTagsUseCase,
    private val insertNoteWithTagsUseCase: InsertNoteWithTagsUseCase,
    private val assetLoader: AssetLoader
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            if (getAllNotesUseCase().first().isEmpty()
                && getAllTagsUseCase().first().isEmpty()
            ) {

                val sampleTags = createSampleTags()
                sampleTags.forEach { tag ->
                    insertTagsUseCase(tag)
                }

                val sampleNotes = getSampleNotes()
//                    .map { it.copy(lightColor = colorPalette.random().toArgb()) }
                sampleNotes.forEach { note ->
                    val randomTags = sampleTags.shuffled().take(Random.nextInt(1, 4))
                    insertNoteWithTagsUseCase(note, randomTags.map { it.id })
                }
            }
        }
    }

    private fun createSampleTags(): List<Tag> {
        return listOf(
            Tag(1, "Work"),
            Tag(2, "Personal"),
            Tag(3, "Ideas"),
            Tag(4, "Todo"),
            Tag(5, "Important"),
            Tag(6, "Project"),
            Tag(7, "Meeting"),
            Tag(8, "Family"),
            Tag(9, "Travel"),
            Tag(10, "Shopping"),
            Tag(11, "Health"),
            Tag(12, "Finance"),
            Tag(13, "Education"),
            Tag(14, "Hobby")
        )
    }

    private suspend fun getSampleNotes(): List<Note> {
        val json = assetLoader.loadTextAsset("noteee.json")
        return Json.decodeFromString<List<Note>>(json)
    }
}

interface AssetLoader {
    suspend fun loadTextAsset(fileName: String): String
}

class AssetLoaderImpl(private val context: Context) : AssetLoader {
    override suspend fun loadTextAsset(fileName: String): String = withContext(Dispatchers.IO) {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}


// Titles of varying lengths for the notes
val smallTitles = listOf("Note", "Todo", "Idea", "Reminder", "Task")
val mediumTitles =
    listOf("Meeting Notes", "Shopping List", "Travel Plans", "Study Schedule", "Fitness Goals")
val largeTitles = listOf(
    "Long-Term Project Overview",
    "Quarterly Business Strategy Discussion",
    "Research on Market Trends for New Product Launch",
    "Comprehensive Plan for Website Redesign and Development",
    "Detailed Steps for Personal Finance Management"
)

// Contents of varying lengths for the notes
val smallContents = listOf("Buy milk", "Call mom", "Water the plants", "Email boss", "Book flight")
val mediumContents = listOf(
    "Prepare presentation for Monday's meeting",
    "Buy groceries: milk, eggs, bread, and cheese",
    "Outline for the next blog post on technology trends",
    "Plan vacation to Italy: book hotel and tours",
    "Study chapters 1-5 for next week's test",
    """
        Day 1-3: Tokyo
        - Arrive at Narita International Airport
        - Check-in at Hotel Sunroute Plaza Shinjuku
        - Visit Senso-ji Temple in Asakusa
        - Explore Akihabara for electronics and anime culture
        - Dinner at Robot Restaurant
        - Day trip to DisneySea
        - Shibuya Crossing and shopping
        - Teamlab Borderless digital art museum

        Day 4-5: Hakone
        - Take Shinkansen to Odawara, then bus to Hakone
        - Stay at traditional ryokan with onsen (hot springs)
        - Hakone Ropeway for Mt. Fuji views
        - Lake Ashi cruise
        - Hakone Open-Air Museum

        Day 6-8: Kyoto
        - Travel to Kyoto by Shinkansen
        - Check-in at Kyoto Century Hotel
        - Visit Kinkaku-ji (Golden Pavilion)
        - Explore Arashiyama Bamboo Grove
        - Fushimi Inari Shrine (1000 torii gates)
        - Day trip to Nara (Todaiji Temple, deer park)
        - Gion district for geisha spotting
        - Tea ceremony experience

        Day 9-10: Hiroshima & Miyajima
        - Travel to Hiroshima by Shinkansen
        - Visit Peace Memorial Park and Museum
        - Hiroshima Castle
        - Day trip to Miyajima Island
        - See the floating torii gate of Itsukushima Shrine
        - Mt. Misen ropeway for panoramic views

        Remember to pack:
        - Passport
        - JR Pass
        - Comfortable walking shoes
        - Adapter for electronics
        """
)
val largeContents = listOf(
    "Project description: Develop a mobile application for personal task management. Features include adding, editing, and deleting tasks, organizing them by priority and deadline, and setting reminders.",
    "Business strategy for Q4: Focus on increasing sales in European markets, launch the new product line by November, and optimize the marketing campaigns for Black Friday.",
    "Research: Analyze consumer behavior trends in the last 5 years for digital products, identify new market opportunities, and develop actionable insights for the product team.",
    "Website redesign plan: Improve the user experience by simplifying navigation, increasing mobile responsiveness, and integrating modern design elements.",
    "Personal finance plan: Set a monthly budget, track expenses in real-time, save for emergencies, and plan for retirement by investing in low-risk mutual funds."
)

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

/*suspend fun generateDummyData(noteRepository: com.despicable.core.data.repository.NoteRepository) {
    val random = Random(System.currentTimeMillis())

    // Fetch existing tags from the database
    // Step 1: Insert tags if they don't exist in the database
    val existingTags = noteRepository.getAllTags().firstOrNull()

    val tagIds = if (existingTags.isNullOrEmpty()) {
        // Insert tags
        tags.forEach {
            noteRepository.insertTag(it)
        }
        // Fetch inserted tags' IDs
        noteRepository.getAllTags().first().map { it.id }
    } else {
        // Tags already exist, use the existing tag IDs
        Timber.tag("DEBUG").d("Predefined tags already exist.")
        existingTags.map { it.id }
    }

    if (tagIds.isEmpty()) {
        throw Exception("No tags found in the database! Please insert tags first.")
    }

    val notes = List(100) {
        val (title, content) = when (random.nextInt(3)) {
            0 -> Pair(smallTitles.random(), smallContents.random()) // Small note
            1 -> Pair(mediumTitles.random(), mediumContents.random()) // Medium note
            else -> Pair(largeTitles.random(), largeContents.random()) // Large note
        }

        val isPinned = random.nextBoolean()
        val isArchived = false
        val isTrashed = false
        val lightColor = colorPalette.random().toArgb() // Random color from the palette
        val updateDate = generateRandomDate()

        // Shuffle and pick random tags only if this is the first launch (tags were just inserted)
        val randomTags = if (existingTags.isNullOrEmpty()) {
            tagIds.shuffled().take(random.nextInt(1, 4)) // Shuffle if this is the first time
        } else {
            tagIds.take(random.nextInt(0, 4)) // Don't shuffle, just take a few tags
        }
//        Timber.tag("DEBUG").d("[randomTags]=[$randomTags]")

        NoteEntity(
            title = title,
            content = content,
            updateDate = updateDate,
            lightColor = lightColor,
            isPinned = isPinned,
            isArchived = isArchived,
            isTrashed = isTrashed,
            isDone = false
        ) to randomTags
    }

    // Batch insert notes with tags
    runBlocking {
        notes.forEach { (note, tags) ->
            noteRepository.insertNoteWithTags(note, tags)
        }
    }

    println("100 dummy notes have been inserted successfully!")
}

fun generateRandomDate(): String {
    val year = Random.nextInt(2021, 2024)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, 29) // To simplify, we assume each month has 28 days
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}*/
