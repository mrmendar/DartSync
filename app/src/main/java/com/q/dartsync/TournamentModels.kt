package com.q.dartsync

// Her bir eşleşmeyi temsil eder
data class TournamentMatch(
    val player1: String,
    val player2: String,
    var winner: String? = null,
    val round: Int = 1 // 1: Çeyrek Final, 2: Yarı Final vb.
)