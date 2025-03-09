package com.example.kidstutor.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kidstutor.data.model.TutorSession
import com.example.kidstutor.data.repository.TutorRepository
import com.example.kidstutor.data.repository.TopicContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TutorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _sessions = MutableStateFlow<List<TutorSession>>(emptyList())
    val sessions: StateFlow<List<TutorSession>> = _sessions

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                repository.getAllSessions()
                    .catch { 
                        _uiState.value = UiState.Error(it.message ?: "Unknown error")
                    }
                    .collect { sessions ->
                        _sessions.value = sessions
                        _uiState.value = UiState.Success
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load sessions")
            }
        }
    }

    fun createSession(topic: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.createSession(topic)
                loadSessions()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create session")
            }
        }
    }

    fun deleteSession(session: TutorSession) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.deleteSession(session)
                loadSessions()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete session")
            }
        }
    }

    fun searchSessions(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.searchSessions(query)
                    .catch { 
                        _uiState.value = UiState.Error(it.message ?: "Search failed")
                    }
                    .collect { sessions ->
                        _sessions.value = sessions
                        _uiState.value = UiState.Success
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Search failed")
            }
        }
    }

    suspend fun generateTopicContent(topic: String, language: String = "en"): TopicContent {
        try {
            return repository.generateTopicContent(topic, language)
        } catch (e: Exception) {
            throw Exception("Failed to generate content: ${e.message}")
        }
    }

    suspend fun answerQuestion(question: String, context: String, language: String = "en"): String {
        try {
            return repository.answerQuestion(question, context, language)
        } catch (e: Exception) {
            throw Exception("Failed to answer question: ${e.message}")
        }
    }

    suspend fun explainImage(image: Bitmap, language: String = "en"): String {
        try {
            return repository.explainImage(image, language)
        } catch (e: Exception) {
            throw Exception("Failed to explain image: ${e.message}")
        }
    }

    fun updateSession(session: TutorSession) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.updateSession(session)
                loadSessions()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update session")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    class Factory(private val repository: TutorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 