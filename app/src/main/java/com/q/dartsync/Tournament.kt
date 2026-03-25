package com.q.dartsync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tournamentName: String,
    val participantNames: String, // Virgülle ayrılmış isimler: "Tuna, Kerem, Ali, Veli"
    val status: String = "Active", // Active, Finished
    val winnerName: String? = null
)

