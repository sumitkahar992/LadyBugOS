package com.despicable.feature.home.di


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.despicable.core.designsystem.theme.LadyBugOSTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val notes = remember { getSampleNotes() }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Search bar height
    val searchBarHeight = 56.dp
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalSearchBarHeight = searchBarHeight + statusBarHeight + 16.dp // 16.dp for padding
    val totalSearchBarHeightPx = with(LocalDensity.current) { totalSearchBarHeight.toPx() }

    // Track search bar offset
    var searchBarOffsetY by remember { mutableFloatStateOf(0f) }

    // Smooth animation for search bar
    val animatedSearchBarOffsetY by animateFloatAsState(
        targetValue = searchBarOffsetY,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f
        ),
        label = "searchBarOffset"
    )

    // Create nested scroll connection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = searchBarOffsetY + delta
                searchBarOffsetY = newOffset.coerceIn(-totalSearchBarHeightPx, 0f)
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y == 0f && available.y > 0) {
                    searchBarOffsetY = 0f
                }
                return Offset.Zero
            }
        }
    }

    // Filter notes
    val filteredNotes = remember(searchQuery, notes) {
        if (searchQuery.isEmpty()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent()
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main content with notes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                // Notes Grid/List
                if (isGridView) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = totalSearchBarHeight + 8.dp,
                            bottom = 80.dp
                        ),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredNotes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(note = note)
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(1),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = totalSearchBarHeight + 8.dp,
                            bottom = 80.dp
                        ),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredNotes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(note = note)
                        }
                    }
                }

                // Search Bar Container with proper background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = animatedSearchBarOffsetY
                        }
                ) {
                    // Background that extends behind status bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalSearchBarHeight)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    )

                    // Google Keep Style Search Bar
                    GoogleKeepSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onGridViewToggle = { isGridView = !isGridView },
                        isGridView = isGridView,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }

            // FAB Overlay Background (Full Screen)
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isFabExpanded = false
                        }
                )
            }

            // Multi-option FAB
            ExpandableFAB(
                isExpanded = isFabExpanded,
                onExpandToggle = { isFabExpanded = !isFabExpanded },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .zIndex(2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleKeepSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onGridViewToggle: () -> Unit,
    isGridView: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Icon
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search your notes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    singleLine = true
                )
            }

            // Clear button
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Grid/List View Toggle
            IconButton(
                onClick = onGridViewToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isGridView) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                    contentDescription = if (isGridView) "List view" else "Grid view",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Account Avatar
            IconButton(
                onClick = { /* Open account menu */ },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "A",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableFAB(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(300),
        label = "rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FAB Options
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + expandVertically(
                animationSpec = tween(250),
                expandFrom = Alignment.Bottom
            ),
            exit = fadeOut(tween(200)) + shrinkVertically(
                animationSpec = tween(250),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FabOption(
                    icon = Icons.Outlined.CheckBox,
                    label = "List",
                    onClick = {
                        onExpandToggle()
                        /* Create list note */
                    }
                )
                FabOption(
                    icon = Icons.Outlined.Brush,
                    label = "Drawing",
                    onClick = {
                        onExpandToggle()
                        /* Create drawing note */
                    }
                )
                FabOption(
                    icon = Icons.Outlined.Mic,
                    label = "Audio",
                    onClick = {
                        onExpandToggle()
                        /* Create audio note */
                    }
                )
                FabOption(
                    icon = Icons.Outlined.Image,
                    label = "Image",
                    onClick = {
                        onExpandToggle()
                        /* Create image note */
                    }
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = {
                if (isExpanded) {
                    // Create text note when expanded and clicked
                    onExpandToggle()
                } else {
                    onExpandToggle()
                }
            },
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isExpanded) 8.dp else 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                if (isExpanded) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = if (isExpanded) "New text note" else "Create note",
                tint = if (isExpanded)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun FabOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surface,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 3.dp
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ... Rest of the code (DrawerContent, DrawerItem, NoteCard, TagChip, formatTimestamp, getSampleNotes) remains the same ...

@Composable
fun DrawerContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        // Header
        Text(
            text = "Google Keep",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Items
        DrawerItem(
            icon = Icons.Outlined.Lightbulb,
            label = "Notes",
            selected = true,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.Outlined.Notifications,
            label = "Reminders",
            selected = false,
            onClick = { }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Labels section
        Text(
            text = "LABELS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.Label,
            label = "Personal",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.Label,
            label = "Work",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.Label,
            label = "Ideas",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.Outlined.Add,
            label = "Create new label",
            selected = false,
            onClick = { }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        DrawerItem(
            icon = Icons.Outlined.Archive,
            label = "Archive",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.Outlined.Delete,
            label = "Trash",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            selected = false,
            onClick = { }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            label = "Help & feedback",
            selected = false,
            onClick = { }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { /* Open note detail/edit */ },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = note.color.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Title
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Content preview
            Text(
                text = note.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 10,
                lineHeight = 18.sp
            )

            // Tags
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    note.tags.take(3).forEach { tag ->
                        TagChip(tag = tag)
                    }
                }
            }

            // Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTimestamp(note.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatTimestamp(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val hours = ChronoUnit.HOURS.between(timestamp, now)
    val days = ChronoUnit.DAYS.between(timestamp, now)

    return when {
        hours < 1 -> "Just now"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> timestamp.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}



fun getSampleNotes(): List<Note> {
    return listOf(
        Note(
            id = 1,
            title = "Grocery list",
            content = "Milk, Eggs, Bread, Butter",
            timestamp = LocalDateTime.now().minusHours(2),
            tags = listOf("shopping"),
            color = Color(0xFFFFEB3B)
        ),
        Note(
            id = 2,
            title = "Project Ideas",
            content = "1. AI Assistant app\n2. Reminder app with location based alerts",
            timestamp = LocalDateTime.now().minusDays(1),
            tags = listOf("work", "ideas"),
            color = Color(0xFF90CAF9)
        ),
        Note(
            id = 3,
            title = "Meeting Notes",
            content = "Discuss product roadmap and deadlines",
            timestamp = LocalDateTime.now().minusDays(2),
            tags = listOf("work"),
            color = Color(0xFFC8E6C9)
        ),
        Note(
            id = 4,
            title = "Books to Read",
            content = "• Atomic Habits\n• Deep Work\n• The Pragmatic Programmer\n• Clean Code",
            timestamp = LocalDateTime.now().minusHours(5),
            tags = listOf("personal", "reading"),
            color = Color(0xFFFFCDD2)
        ),
        Note(
            id = 5,
            title = "Workout Plan",
            content = "Monday: Chest & Triceps\nTuesday: Back & Biceps\nWednesday: Rest\nThursday: Legs\nFriday: Shoulders",
            timestamp = LocalDateTime.now().minusDays(3),
            tags = listOf("fitness"),
            color = Color(0xFFE1BEE7)
        ),
        Note(
            id = 6,
            title = "Quick Note",
            content = "Call dentist tomorrow",
            timestamp = LocalDateTime.now().minusMinutes(30),
            tags = listOf(),
            color = Color(0xFFFFF9C4)
        ),
        Note(
            id = 7,
            title = "Travel Plans",
            content = "Book flight to Paris next month",
            timestamp = LocalDateTime.now().minusWeeks(1),
            tags = listOf("travel"),
            color = Color(0xFFAED581)
        ),
        Note(
            id = 8,
            title = "Birthday Party",
            content = "Invite friends for a surprise party in June",
            timestamp = LocalDateTime.now().plusMonths(1),
            tags = listOf("birthday"),
            color = Color(0xFFF48FB1)
        ),
        Note(
            id = 9,
            title = "Recipe Idea",
            content = "Spaghetti Carbonara Recipe:\nIngredients: Spaghetti, Eggs, Parmesan Cheese, Bacon\nInstructions: Cook spaghetti al dente, fry bacon until crispy, whisk eggs with grated cheese.",
            timestamp = LocalDateTime.now().minusDays(4),
            tags = listOf("food"),
            color = Color(0xFFBA68C8)
        ),
        Note(
            id = 10,
            title = "Movie Night",
            content = "Watch 'The Shawshank Redemption' tonight!",
            timestamp = LocalDateTime.now().minusHours(1),
            tags = listOf("entertainment"),
            color = Color(0xFFDCE775)
        ),
        Note(
            id = 11,
            title = "Todo List",
            content = "- [x] Finish project report\n- [ ] Call client\n- [ ] Buy groceries",
            timestamp = LocalDateTime.now().minusDays(5),
            tags = listOf("todo"),
            color = Color(0xFF9FA8DA)
        ),
        Note(
            id = 12,
            title = "Weather Forecast",
            content = "Today: Sunny, High 75°F\nTomorrow: Partly Cloudy, High 78°F",
            timestamp = LocalDateTime.now().minusHours(3),
            tags = listOf("weather"),
            color = Color(0xFFE0E0E0)
        ),
        Note(
            id = 13,
            title = "Shopping List",
            content = "Apples, Bananas, Milk, Bread",
            timestamp = LocalDateTime.now().minusDays(6),
            tags = listOf("shopping"),
            color = Color(0xFFFFEB3B)
        ),
        Note(
            id = 14,
            title = "Ideas for New App",
            content = "1. Social media platform\n2. Learning app\n3. Travel planning app",
            timestamp = LocalDateTime.now().minusDays(7),
            tags = listOf("ideas"),
            color = Color(0xFF90CAF9)
        ),
        Note(
            id = 15,
            title = "Personal Goals",
            content = "Learn a new language\nImprove coding skills\nRead more books",
            timestamp = LocalDateTime.now().minusDays(8),
            tags = listOf("goals"),
            color = Color(0xFFC8E6C9)
        ),
        Note(
            id = 16,
            title = "Cooking Recipes",
            content = "• Chicken Alfredo\n• Grilled Salmon\n• Vegetable Stir Fry",
            timestamp = LocalDateTime.now().minusHours(4),
            tags = listOf("cooking"),
            color = Color(0xFFFFCDD2)
        ),
        Note(
            id = 17,
            title = "Fitness Routine",
            content = "Monday: Cardio\nTuesday: Strength Training\nWednesday: Rest\nThursday: Yoga\nFriday: HIIT Workout",
            timestamp = LocalDateTime.now().minusDays(9),
            tags = listOf("fitness"),
            color = Color(0xFFE1BEE7)
        ),
        Note(
            id = 18,
            title = "Daily Journal",
            content = "Today was a good day!\nI accomplished my goals and had some quality time with family.",
            timestamp = LocalDateTime.now().minusDays(10),
            tags = listOf("journal"),
            color = Color(0xFFFFF9C4)
        ),

        )
}

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: LocalDateTime,
    val tags: List<String>,
    val color: Color
)

@Preview
@Composable
fun PreviewNotesScreen() {
    LadyBugOSTheme {
        NotesScreen()
    }
}