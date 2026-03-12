package com.q.dartsync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val player1Name: String,
    val player2Name: String,
    val date: Long, // Oyunun bittiği zaman (Timestamp)
    val p1Snapshot: String, // Örn: "3,1,0,2..." (Oyuncu 1'in vuruşları)
    val p2Snapshot: String  // Örn: "0,2,3,1..." (Oyuncu 2'in vuruşları)
)