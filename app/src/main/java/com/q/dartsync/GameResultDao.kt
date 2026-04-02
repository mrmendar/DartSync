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

    // --- GENEL İSTATİSTİKLER ---

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

    // --- OYUNCU BAZLI DETAYLI İSTATİSTİKLER ---

    // 3. Sistemdeki tüm benzersiz oyuncu isimlerini getirir (Alfabetik)
    @Query("""
        SELECT DISTINCT name FROM (
            SELECT player1Name AS name FROM game_results 
            UNION 
            SELECT player2Name AS name FROM game_results
        ) ORDER BY name ASC
    """)
    suspend fun getAllUniquePlayers(): List<String>

    // 4. Belirli bir oyuncunun kaç maç kazandığını sayar
    @Query("SELECT COUNT(*) FROM game_results WHERE winnerName = :playerName")
    suspend fun getWinCount(playerName: String): Int

    // 5. Belirli bir oyuncunun toplam kaç maçta yer aldığını sayar
    @Query("SELECT COUNT(*) FROM game_results WHERE player1Name = :playerName OR player2Name = :playerName")
    suspend fun getTotalMatchCount(playerName: String): Int


    // Tek bir maçı silmek için (ID üzerinden)
    @Query("DELETE FROM game_results WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Int)

    // Tüm geçmişi temizlemek istersen (Reset)
    @Query("DELETE FROM game_results")
    suspend fun deleteAllMatches()

    @Query("DELETE FROM game_results WHERE player1Name = :name OR player2Name = :name")
    suspend fun deleteByPlayerName(name: String)

    @Query("SELECT SUM(totalDarts) FROM game_results WHERE player1Name = :name OR player2Name = :name")
    suspend fun getTotalDartsByPlayer(name: String): Int

    @Query("SELECT SUM(CAST(p1Snapshot AS INTEGER)) FROM game_results WHERE player1Name = :name")
    suspend fun getSumOfRemainingP1(name: String): Int

    @Query("SELECT SUM(CAST(p2Snapshot AS INTEGER)) FROM game_results WHERE player2Name = :name")
    suspend fun getSumOfRemainingP2(name: String): Int


    @Query("SELECT * FROM game_results WHERE userId = :currentUserId")
    fun getMyResults(currentUserId: String): List<GameResult>

    @Query("SELECT * FROM game_results WHERE userId = :currentId ORDER BY date DESC")
    fun getMyStats(currentId: String): List<GameResult>

}