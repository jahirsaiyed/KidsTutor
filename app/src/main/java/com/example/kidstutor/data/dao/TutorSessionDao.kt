package com.example.kidstutor.data.dao

import androidx.room.*
import com.example.kidstutor.data.model.TutorSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TutorSessionDao {
    @Query("SELECT * FROM tutor_sessions ORDER BY lastAccessedAt DESC")
    fun getAllSessions(): Flow<List<TutorSession>>

    @Query("SELECT * FROM tutor_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): TutorSession?

    @Insert
    suspend fun insertSession(session: TutorSession): Long

    @Update
    suspend fun updateSession(session: TutorSession)

    @Delete
    suspend fun deleteSession(session: TutorSession)

    @Query("SELECT * FROM tutor_sessions WHERE topic LIKE '%' || :searchQuery || '%'")
    fun searchSessions(searchQuery: String): Flow<List<TutorSession>>
} 