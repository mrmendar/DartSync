package com.q.dartsync

// Bu artık senin projenin ortak "Maç" veri modeli.
// Her yerden buna erişebilirsin.
data class TournamentMatch(
    val player1: String,
    val player2: String,
    var winner: String? = null,
    var round: Int = 1,
    var isBye: Boolean = false
)