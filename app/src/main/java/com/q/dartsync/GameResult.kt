package com.q.dartsync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val player1Name: String,
    val player2Name: String,
    val date: Long,
    val p1Snapshot: String,
    val p2Snapshot: String,
    val winnerName: String ,// Yeni eklenen alan: Kazananın ismi
    val totalDarts: Int
)