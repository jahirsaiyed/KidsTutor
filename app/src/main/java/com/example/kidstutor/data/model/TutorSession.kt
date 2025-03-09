package com.example.kidstutor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.kidstutor.util.Converters
import java.time.LocalDateTime

@Entity(tableName = "tutor_sessions")
@TypeConverters(Converters::class)
data class TutorSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topic: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastAccessedAt: LocalDateTime = LocalDateTime.now(),
    val language: String = "en",
    val thumbnailUrl: String? = null,
    val content: String? = null,
    val imageUrls: List<String>? = null,
    val youtubeLinks: List<String>? = null
) 