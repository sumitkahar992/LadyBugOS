package com.despicable.feature.home

import com.despicable.core.model.Checklist
import com.despicable.core.model.Note
import com.despicable.core.model.Tag

val sampleTags = listOf(
    Tag(1, "Work"),
    Tag(2, "Personal"),
    Tag(3, "Shopping"),
    Tag(4, "Ideas"),
    Tag(5, "Travel"),
    Tag(6, "Health"),
    Tag(7, "Books"),
    Tag(8, "Movies")
)

val sampleNotes = listOf(
    Note(
        id = 1,
        title = "Shopping List",
        content = "Need to buy groceries for the week",
        updateDate = "2025-01-20",
        lightColor = 0xFFE6F3FF.toInt(),
        isPinned = true,
        reminderDate = 1698105600000,
        pinnedDate = System.currentTimeMillis(),
        isChecklist = true
    ),
    Note(
        id = 2,
        title = "Project Ideas",
        content = "Mobile app concepts for next quarter",
        updateDate = "2025-01-19",
        lightColor = 0xFFFFF3E0.toInt(),
        isPinned = true,
        isChecklist = true
    ),
    Note(
        id = 3,
        title = "Travel Plans",
        content = "Places to visit in Europe",
        updateDate = "2025-01-18",
        lightColor = 0xFFE8F5E9.toInt(),
        isChecklist = true
    ),
    Note(
        id = 4,
        title = "Reading List",
        content = "Books to read this month",
        updateDate = "2025-01-17",
        lightColor = 0xFFFCE4EC.toInt(),
        isChecklist = true
    ),
    Note(
        id = 5,
        title = "Meeting Notes",
        content = "Discussion points from team sync",
        updateDate = "2025-01-16",
        lightColor = 0xFFF3E5F5.toInt()
    ),
    Note(
        id = 6,
        title = "Workout Routine",
        content = "Weekly exercise plan",
        updateDate = "2025-01-15",
        lightColor = 0xFFE1F5FE.toInt()
    ),
    Note(
        id = 7,
        title = "Recipe Ideas",
        content = "New dishes to try cooking",
        updateDate = "2025-01-14",
        lightColor = 0xFFFFF3E0.toInt()
    ),
    Note(
        id = 8,
        title = "Movie Watchlist",
        content = "Films recommended by friends",
        updateDate = "2025-01-13",
        lightColor = 0xFFE8EAF6.toInt()
    )
)

val sampleChecklists = listOf(
    // Shopping List checklist items
    listOf(
        Checklist(1, noteId = 1, "Milk", false, 0),
        Checklist(2, noteId = 1, "Eggs", true, 1),
        Checklist(3, noteId = 1, "Bread", false, 2),
        Checklist(4, noteId = 1, "Fruits", true, 3),
        Checklist(5, noteId = 1, "Vegetables", false, 4),
        Checklist(6, noteId = 1, "Cheese", true, 5),
        Checklist(7, noteId = 1, "Meat", false, 6),
        Checklist(8, noteId = 1, "Snacks", true, 7)
    ),

    listOf(
        Checklist(1, noteId = 1, "Milk", false, 0),
        Checklist(1, noteId = 1, "Milk", false, 0),
        Checklist(1, noteId = 1, "Milk", false, 0),

    ),

    // Project Ideas checklist items
    listOf(
        Checklist(9, noteId = 2, "Research market trends", true, 0),
        Checklist(10, noteId = 2, "Create wireframes", false, 1),
        Checklist(11, noteId = 2, "Define MVP features", true, 2),
        Checklist(12, noteId = 2, "Schedule team meetings", false, 3),
        Checklist(13, noteId = 2, "Set up development environment", true, 4),
        Checklist(14, noteId = 2, "Create project timeline", false, 5),
        Checklist(15, noteId = 2, "Assign team roles", true, 6),
        Checklist(16, noteId = 2, "Plan beta testing", false, 7)
    ),

    // Travel Plans checklist items
    listOf(
        Checklist(17, noteId = 3, "Book flights", true, 0),
        Checklist(18, noteId = 3, "Reserve hotels", false, 1),
        Checklist(19, noteId = 3, "Check visa requirements", true, 2),
        Checklist(20, noteId = 3, "Purchase travel insurance", false, 3),
        Checklist(21, noteId = 3, "Create itinerary", true, 4),
        Checklist(22, noteId = 3, "Exchange currency", false, 5),
        Checklist(23, noteId = 3, "Pack essentials", true, 6),
        Checklist(24, noteId = 3, "Download offline maps", false, 7)
    ),

    // Reading List checklist items
    listOf(
        Checklist(25, noteId = 4, "The Pragmatic Programmer", true, 0),
        Checklist(26, noteId = 4, "Clean Code", false, 1),
        Checklist(27, noteId = 4, "Design Patterns", true, 2),
        Checklist(28, noteId = 4, "Kotlin in Action", false, 3),
        Checklist(29, noteId = 4, "Android Programming", true, 4),
        Checklist(30, noteId = 4, "Effective Java", false, 5),
        Checklist(31, noteId = 4, "Head First Design Patterns", true, 6),
        Checklist(32, noteId = 4, "Clean Architecture", false, 7)
    )
)

/*
@Preview
@Composable
fun PreviewNoteContent() {
    val note = sampleNotes[2]  // Use first note
    val noteTags = listOf(sampleTags[0], sampleTags[1])  // Associate with first two tags
    val noteChecklist = sampleChecklists[3]  // Use first checklist

    val height = calculateNoteHeight(
        note = note,
        hasReminder = note.reminderDate != null,
        hasTags = true,
        gridLayout = GridLayout.TwoColumns,
        checklistItems = noteChecklist
    )


    LadyBugOSTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(4.dp)
        )
        {
            NoteContent(
                note = note,
                tags = noteTags,
                checkList = noteChecklist,
                gridLayout = GridLayout.TwoColumns,
                titleSize = 20.sp,
                contentSize = 16.sp
            )
        }
    }

}*/
