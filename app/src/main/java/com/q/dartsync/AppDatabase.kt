package com.q.dartsync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameResult::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dartsync_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}