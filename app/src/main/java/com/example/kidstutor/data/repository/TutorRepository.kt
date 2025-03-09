package com.example.kidstutor.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.kidstutor.data.dao.TutorSessionDao
import com.example.kidstutor.data.model.TutorSession
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import com.example.kidstutor.BuildConfig

data class TopicContent(
    val content: String,
    val imageUrls: List<String>,
    val youtubeLinks: List<String>
)

class TutorRepository(
    private val tutorSessionDao: TutorSessionDao,
    context: Context
) {
    private var generativeModel: GenerativeModel? = null
    private var generativeVisionModel: GenerativeModel? = null

    init {
        initializeModels()
    }

    private fun initializeModels() {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            throw Exception("Gemini API key not found. Please add it to local.properties")
        }

        try {
            generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
            generativeVisionModel = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        } catch (e: Exception) {
            // Log initialization error
            e.printStackTrace()
        }
    }

    private fun ensureModelsInitialized() {
        if (generativeModel == null || generativeVisionModel == null) {
            initializeModels()
        }
    }

    // Database operations
    fun getAllSessions(): Flow<List<TutorSession>> = tutorSessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): TutorSession? = tutorSessionDao.getSessionById(id)

    suspend fun createSession(topic: String): Long {
        val session = TutorSession(topic = topic)
        return tutorSessionDao.insertSession(session)
    }

    suspend fun updateSession(session: TutorSession) = tutorSessionDao.updateSession(session)

    suspend fun deleteSession(session: TutorSession) = tutorSessionDao.deleteSession(session)

    fun searchSessions(query: String): Flow<List<TutorSession>> = tutorSessionDao.searchSessions(query)

    private fun parseAIResponse(response: String): TopicContent {
        val imageUrlRegex = """https?://[^\s<>"]+?(?:\.[^\s<>"]+)+""".toRegex()
        val youtubeRegex = """(?:https?://)?(?:www\.)?(?:youtube\.com/watch\?v=|youtu\.be/)([^&\s]+)""".toRegex()

        val imageUrls = imageUrlRegex.findAll(response)
            .map { it.value }
            .filter { it.matches(""".*\.(jpg|jpeg|png|gif)$""".toRegex(RegexOption.IGNORE_CASE)) }
            .toList()
            .take(3) // Limit to 3 images to reduce memory usage

        val youtubeLinks = youtubeRegex.findAll(response)
            .map { "https://www.youtube.com/watch?v=${it.groupValues[1]}" }
            .toList()
            .take(2) // Limit to 2 videos to reduce memory usage

        // Remove URLs from the content
        val cleanContent = response.replace(imageUrlRegex, "")
            .replace(youtubeRegex, "")
            .replace("""\n\s*\n+""".toRegex(), "\n\n")
            .trim()

        return TopicContent(cleanContent, imageUrls, youtubeLinks)
    }

    // Gemini AI operations
    suspend fun generateTopicContent(topic: String, language: String = "en"): TopicContent {
        ensureModelsInitialized()
        
        val model = generativeModel ?: throw Exception("AI model not initialized")
        
        val prompt = """
            Create a brief educational tutorial about '$topic' suitable for children. 
            Keep the response concise and focused.
            
            Include:
            1. A short introduction (2-3 sentences)
            2. 3 key points about the topic
            3. 2 fun facts
            4. A simple activity or question
            5. 1-2 relevant image URLs (ending in .jpg, .jpeg, .png, or .gif)
            6. 1 YouTube video link related to the topic
            
            Please ensure the content is:
            1. Age-appropriate and simple
            2. Engaging but brief
            3. Clear and focused
            
            Please provide the response in $language language.
            Keep the total response under 500 words.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            return parseAIResponse(response.text ?: "Sorry, I couldn't generate content for this topic.")
        } catch (e: Exception) {
            // If there's an error, try reinitializing the models and retry once
            initializeModels()
            val retryModel = generativeModel ?: throw Exception("Failed to initialize AI model")
            val response = retryModel.generateContent(prompt)
            return parseAIResponse(response.text ?: "Sorry, I couldn't generate content for this topic.")
        }
    }

    suspend fun answerQuestion(question: String, context: String, language: String = "en"): String {
        ensureModelsInitialized()
        
        val model = generativeModel ?: throw Exception("AI model not initialized")
        
        val prompt = """
            Context: ${context.take(500)} // Limit context size
            
            Question: $question
            
            Please provide a brief, child-friendly answer in 2-3 sentences.
            Use simple language and keep it focused.
            
            Provide the response in $language language.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text ?: "Sorry, I couldn't answer this question."
        } catch (e: Exception) {
            "Sorry, I couldn't answer this question right now. Please try again."
        }
    }

    suspend fun explainImage(image: Bitmap, language: String = "en"): String {
        ensureModelsInitialized()
        
        val model = generativeVisionModel ?: throw Exception("AI vision model not initialized")
        
        val prompt = """
            Please give a very brief, child-friendly description of this image.
            Keep it to 2-3 sentences.
            Use simple language.
            Provide the description in $language language.
        """.trimIndent()

        return try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: "Sorry, I couldn't explain this image."
        } catch (e: Exception) {
            "Sorry, I couldn't explain this image right now. Please try again."
        }
    }
} 