package com.example.kidstutor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kidstutor.data.model.TutorSession
import com.example.kidstutor.ui.components.SessionCard
import com.example.kidstutor.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSessionClick: (TutorSession) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isGeneratingContent by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf("") }

    val sessions by viewModel.sessions.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kids Tutor") },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, "Search sessions")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewSessionDialog = true }) {
                Icon(Icons.Default.Add, "Create new session")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchSessions(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search topics...") },
                    singleLine = true
                )
            }

            when {
                isGeneratingContent -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Creating a personalized tutorial for '$newTopicName'...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                uiState is MainViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState is MainViewModel.UiState.Success -> {
                    if (sessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sessions yet. Create one to get started!",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sessions) { session ->
                                SessionCard(
                                    session = session,
                                    onSessionClick = onSessionClick,
                                    onDeleteClick = { viewModel.deleteSession(it) }
                                )
                            }
                        }
                    }
                }
                uiState is MainViewModel.UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as MainViewModel.UiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (showNewSessionDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showNewSessionDialog = false
                    newTopicName = ""
                },
                title = { Text("Create New Session") },
                text = {
                    Column {
                        Text(
                            "Enter a topic to create a personalized tutorial using AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = newTopicName,
                            onValueChange = { newTopicName = it },
                            label = { Text("Topic Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTopicName.isNotBlank()) {
                                showNewSessionDialog = false
                                isGeneratingContent = true
                                coroutineScope.launch {
                                    viewModel.createSession(newTopicName)
                                    isGeneratingContent = false
                                    newTopicName = ""
                                }
                            }
                        },
                        enabled = newTopicName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showNewSessionDialog = false
                            newTopicName = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 