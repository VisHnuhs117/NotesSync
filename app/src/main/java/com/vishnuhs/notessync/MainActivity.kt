package com.vishnuhs.notessync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val viewModel: NotesViewModel = viewModel()
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val syncStatus by viewModel.syncStatus.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedNoteCategory by remember { mutableStateOf("General") }
    var showCategoryDropdown by remember { mutableStateOf("") }

    // Single LazyColumn for everything - this fixes the scroll issue
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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

                // Category chips in horizontal scroll
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

        // Add Note Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Add New Note",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category selection for new note
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown.isNotEmpty(),
                        onExpandedChange = {
                            showCategoryDropdown = if (showCategoryDropdown.isEmpty()) "open" else ""
                        }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedNoteCategory,
                            onValueChange = {},
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = showCategoryDropdown.isNotEmpty()
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown.isNotEmpty(),
                            onDismissRequest = { showCategoryDropdown = "" }
                        ) {
                            Note.PREDEFINED_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedNoteCategory = category
                                        showCategoryDropdown = ""
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content input
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                viewModel.addNote(title, content, selectedNoteCategory)
                                title = ""
                                content = ""
                                selectedNoteCategory = "General"
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Note")
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncAllNotes() }
                        ) {
                            Text("Sync")
                        }

                        Button(
                            onClick = { viewModel.testFirebaseConnection() }
                        ) {
                            Text("Test")
                        }
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
        items(notes) { note ->
            NoteCard(
                note = note,
                searchQuery = searchQuery,
                onDelete = { viewModel.deleteNote(note) }
            )
        }

        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(note: Note, searchQuery: String, onDelete: () -> Unit) {
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

                    // Category badge
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

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
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

// Helper function to get category colors
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