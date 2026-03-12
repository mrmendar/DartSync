package com.q.dartsync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameResultDao {
    @Insert
    suspend fun insertGame(gameResult: GameResult)

    @Query("SELECT * FROM game_results ORDER BY date DESC")
    suspend fun getAllGames(): List<GameResult>

    // --- İstatistik Sorguları ---

    // 1. Toplam maç sayısı
    @Query("SELECT COUNT(*) FROM game_results")
    suspend fun getTotalMatches(): Int

    // 2. En aktif oyuncu (En çok maçta yer alan isim)
    @Query("""
        SELECT name FROM (
            SELECT player1Name AS name FROM game_results 
            UNION ALL 
            SELECT player2Name AS name FROM game_results
        ) 
        GROUP BY name 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostActivePlayer(): String?
}