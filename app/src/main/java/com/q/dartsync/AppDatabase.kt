package com.q.dartsync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Version 1'den 2'ye yükseltildi. Yeni "winnerName" kolonu için bu şart.
@Database(entities = [GameResult::class], version = 3,exportSchema = false)
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
                )
                    /* * Mühendislik Notu: Aşağıdaki satır, şema değiştiğinde (Migration)
                     * hata vermek yerine eski tabloyu silip yeni yapıya göre sıfırdan kurar.
                     * Geliştirme aşamasında hayat kurtarır.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}