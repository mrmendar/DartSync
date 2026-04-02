package com.q.dartsync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val player1Name: String,
    val player2Name: String,
    val date: Long,
    val p1Snapshot: String,
    val p2Snapshot: String,
    val winnerName: String,
    val totalDarts: Int,

    // 🔥 Antrenman Modu (Finish Master) İçin Yeni Alanlar
    val isFinishMasterMode: Boolean = false,
    val finishedLevels: String = "",       // Örn: "120:3, 121:BUST, 122:6"
    val sessionDurationMillis: Long = 0,   // Antrenman ne kadar sürdü?
    val highestLevelReached: Int = 120     // Kullanıcının ulaştığı son seviye
)