package com.example.kidstutor.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kidstutor.data.dao.TutorSessionDao
import com.example.kidstutor.data.model.TutorSession
import com.example.kidstutor.util.Converters

@Database(entities = [TutorSession::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TutorDatabase : RoomDatabase() {
    abstract fun tutorSessionDao(): TutorSessionDao

    companion object {
        @Volatile
        private var INSTANCE: TutorDatabase? = null

        fun getDatabase(context: Context): TutorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TutorDatabase::class.java,
                    "tutor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 