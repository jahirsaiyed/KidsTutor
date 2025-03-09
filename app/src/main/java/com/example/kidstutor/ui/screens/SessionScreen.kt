package com.example.kidstutor.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.SUCCESS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.kidstutor.data.model.TutorSession
import com.example.kidstutor.data.repository.TopicContent
import com.example.kidstutor.ui.components.YouTubePlayer
import com.example.kidstutor.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun SessionScreen(
    session: TutorSession,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf(session.content ?: "") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(session.language) }
    var isSpeaking by remember { mutableStateOf(false) }
    var imageUrls by remember { mutableStateOf(session.imageUrls ?: emptyList()) }
    var youtubeLinks by remember { mutableStateOf(session.youtubeLinks ?: emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Initialize TextToSpeech
    val textToSpeech = remember {
        object {
            var instance: TextToSpeech? = null
        }
    }

    DisposableEffect(Unit) {
        textToSpeech.instance = TextToSpeech(context) { status ->
            if (status == SUCCESS) {
                textToSpeech.instance?.language = Locale.US
            }
        }
        
        onDispose {
            try {
                textToSpeech.instance?.stop()
                textToSpeech.instance?.shutdown()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    // Function to handle text-to-speech
    fun speakText(text: String) {
        try {
            textToSpeech.instance?.let { tts ->
                if (isSpeaking) {
                    tts.stop()
                    isSpeaking = false
                } else {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    isSpeaking = true
                }
            }
        } catch (e: Exception) {
            errorMessage = "Unable to use text-to-speech at the moment"
        }
    }

    // Function to safely generate content
    suspend fun generateContent() {
        try {
            isLoading = true
            errorMessage = null
            viewModel.generateTopicContent(session.topic, selectedLanguage).let { topicContent ->
                content = topicContent.content
                imageUrls = topicContent.imageUrls
                youtubeLinks = topicContent.youtubeLinks
                // Update session with new content
                viewModel.updateSession(
                    session.copy(
                        content = content,
                        imageUrls = imageUrls,
                        youtubeLinks = youtubeLinks
                    )
                )
            }
        } catch (e: Exception) {
            println("Error generating content: ${e}")
            errorMessage = "Unable to generate content. Please try again."
        } finally {
            isLoading = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            coroutineScope.launch {
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                    bitmap?.let {
                        answer = viewModel.explainImage(it, selectedLanguage)
                    }
                } catch (e: Exception) {
                    answer = "Failed to process image: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Load content if not already available
    LaunchedEffect(session) {
        if (content.isEmpty()) {
            generateContent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.topic) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Go back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                generateContent()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, "Search topic")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Generating content for ${session.topic}...")
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                errorMessage?.let { message ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                IconButton(onClick = { errorMessage = null }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Content section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Topic Content",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                IconButton(
                                    onClick = { speakText(content) }
                                ) {
                                    Icon(
                                        if (isSpeaking) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = if (isSpeaking) "Stop speaking" else "Start speaking"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = content)
                        }
                    }
                }

                // Images section
                if (imageUrls.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related Images",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    imageUrls.forEach { imageUrl ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                GlideImage(
                                    model = imageUrl,
                                    contentDescription = "Topic image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Videos section
                if (youtubeLinks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related Videos",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    youtubeLinks.forEach { videoUrl ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                YouTubePlayer(
                                    videoId = Uri.parse(videoUrl).getQueryParameter("v") ?: ""
                                )
                            }
                        }
                    }
                }

                // Question section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Ask a Question",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = question,
                                onValueChange = { question = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Your question") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isLoading = true
                                            answer = viewModel.answerQuestion(
                                                question,
                                                content,
                                                selectedLanguage
                                            )
                                            isLoading = false
                                            question = ""
                                        }
                                    },
                                    enabled = question.isNotBlank()
                                ) {
                                    Text("Ask")
                                }
                                
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") }
                                ) {
                                    Text("Upload Image")
                                }
                            }
                        }
                    }
                }

                // Answer section
                if (answer.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Answer",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    IconButton(
                                        onClick = { speakText(answer) }
                                    ) {
                                        Icon(
                                            if (isSpeaking) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = if (isSpeaking) "Stop speaking" else "Start speaking"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = answer)
                            }
                        }
                    }
                }
            }
        }
    }
} 