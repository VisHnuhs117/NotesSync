package com.vishnuhs.notessync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotesApp()
            }
        }
    }
}

@Composable
fun NotesApp() {
    var showAddNoteScreen by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val viewModel: NotesViewModel = viewModel()

    when {
        showAddNoteScreen -> {
            AddNoteScreen(
                onNavigateBack = { showAddNoteScreen = false },
                onSaveNote = { title, content, category ->
                    viewModel.addNote(title, content, category)
                }
            )
        }

        noteToEdit != null -> {
            EditNoteScreen(
                note = noteToEdit!!,
                onNavigateBack = { noteToEdit = null },
                onUpdateNote = { title, content, category ->
                    viewModel.updateNote(noteToEdit!!.id, title, content, category)
                }
            )
        }

        else -> {
            NotesListScreen(
                viewModel = viewModel,
                onAddNote = { showAddNoteScreen = true },
                onEditNote = { note -> noteToEdit = note }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit
) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val syncStatus by viewModel.syncStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NotesSync") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search notes...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Category Filter Section
            item {
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" chip
                        item {
                            FilterChip(
                                onClick = { viewModel.updateSelectedCategory(null) },
                                label = { Text("All") },
                                selected = selectedCategory == null
                            )
                        }

                        // Category chips
                        items(Note.PREDEFINED_CATEGORIES) { category ->
                            FilterChip(
                                onClick = {
                                    viewModel.updateSelectedCategory(
                                        if (selectedCategory == category) null else category
                                    )
                                },
                                label = { Text(category) },
                                selected = selectedCategory == category
                            )
                        }
                    }
                }
            }

            // Sync controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )

                        // Just the sync button - clean and professional
                        Button(
                            onClick = { viewModel.syncAllNotes() }
                        ) {
                            Text("Sync")
                        }
                    }
                }
            }

            // Notes header
            item {
                val headerText = when {
                    searchQuery.isNotBlank() && selectedCategory != null ->
                        "\"$searchQuery\" in $selectedCategory (${notes.size})"
                    searchQuery.isNotBlank() ->
                        "Search: \"$searchQuery\" (${notes.size})"
                    selectedCategory != null ->
                        "$selectedCategory Notes (${notes.size})"
                    else ->
                        "All Notes (${notes.size})"
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Display notes
            if (notes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notes yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap the + button to create your first note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        searchQuery = searchQuery,
                        onDelete = { viewModel.deleteNote(note) },
                        onEdit = { onEditNote(note) }

                    )
                }
            }

            // Add some bottom padding for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Keep your existing NoteCard and getCategoryColor functions unchanged
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(note: Note, searchQuery: String, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = getCategoryColor(note.category),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }



                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Match found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Work" -> Color(0xFFFF5722)
        "Personal" -> Color(0xFF2196F3)
        "Ideas" -> Color(0xFFFF9800)
        "Important" -> Color(0xFFF44336)
        "To-Do" -> Color(0xFF4CAF50)
        "Shopping" -> Color(0xFF9C27B0)
        "Travel" -> Color(0xFF00BCD4)
        "Health" -> Color(0xFF8BC34A)
        "Finance" -> Color(0xFFFFC107)
        else -> Color(0xFF6200EE)
    }
}