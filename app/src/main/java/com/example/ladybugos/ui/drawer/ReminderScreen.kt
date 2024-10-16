package com.example.ladybugos.ui.drawer

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: NotesViewModel = koinViewModel(),
    drawerState: DrawerState? = null,
    onSearch: () -> Unit,
    selectedTheme: Theme,
    handleNoteClick: (Note) -> Unit,
    toggleSelection: (Note) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes = notes.filter { it.isArchived }
    val scope = rememberCoroutineScope()
    val selectedNotes by remember { mutableStateOf(setOf<Note>()) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()



    NoteAppScaffold(
        scrollBehavior = scrollBehavior,
        title = "Reminders",
        onMenuClick = { scope.launch { drawerState?.open() } },
        onSearch = onSearch,
        content = {
            if (archivedNotes.isEmpty()) {
                Text(
                    text = "Notes with upcoming reminder appear here",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize()
                )
            } else {
                NoteGrid(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    notes = archivedNotes,
                    selectedNotes = selectedNotes,
                    onNoteClick = { handleNoteClick(it) },
                    onNoteLongPress = { toggleSelection(it) },
                    theme = selectedTheme,
                    searchHeightPadding = 0.dp,
                    pinnedHeader = false
                )
            }
        },
    )
}*/