package com.q.dartsync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 3 Notu:
 * - GameResult tablosuna 'winnerName' eklendi.
 * - Antrenman modu (Finish Master) için istatistik kolonları eklendi.
 */
@Database(entities = [GameResult::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameResultDao(): GameResultDao

    companion object {
        // @Volatile: INSTANCE değerinin tüm thread'lerde güncel kalmasını sağlar.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Eğer INSTANCE null değilse onu döndür, null ise yeni oluştur (Thread-safe Singleton)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dartsync_database"
                )
                    /**
                     * 🛠️ Mühendislik Notu:
                     * Geliştirme aşamasında şema değiştikçe manuel SQL Migration yazmak yerine
                     * eski tabloyu silip yenisini kurar. Production (Canlı) aşamasında bu satır
                     * kaldırılıp yerine gerçek Migration senaryoları yazılmalıdır.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}