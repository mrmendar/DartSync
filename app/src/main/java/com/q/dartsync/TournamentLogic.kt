package com.q.dartsync

import java.util.UUID
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object TournamentLogic {

    /**
     * Katılımcı listesini alır ve seçilen türe göre tüm maç ağacını oluşturur.
     */
    fun buildTournament(participants: List<String>, type: TournamentType, tournamentId: String): List<TournamentMatch> {
        return when (type) {
            TournamentType.SINGLE_ELIMINATION -> createSingleElimination(participants, tournamentId)
            TournamentType.DOUBLE_ELIMINATION -> createDoubleElimination(participants, tournamentId)
            else -> createSingleElimination(participants, tournamentId)
        }
    }

    private fun createSingleElimination(participants: List<String>, tId: String): List<TournamentMatch> {
        val n = participants.size
        val nextPowerOfTwo = 2.0.pow(ceil(log2(n.toDouble()))).toInt()
        val byeCount = nextPowerOfTwo - n
        val shuffled = participants.shuffled()

        val matches = mutableListOf<TournamentMatch>()
        var currentIndex = 0

        // 1. Tur Maçlarını Oluştur
        // BYE geçecekler
        for (i in 0 until byeCount) {
            matches.add(TournamentMatch(
                matchId = UUID.randomUUID().toString(),
                tournamentId = tId,
                player1 = shuffled[currentIndex],
                player2 = "BYE",
                winner = shuffled[currentIndex],
                status = "finished",
                round = 1
            ))
            currentIndex++
        }

        // Normal maçlar
        while (currentIndex < n) {
            matches.add(TournamentMatch(
                matchId = UUID.randomUUID().toString(),
                tournamentId = tId,
                player1 = shuffled[currentIndex],
                player2 = shuffled[currentIndex + 1],
                round = 1
            ))
            currentIndex += 2
        }

        return matches
    }

    /**
     * 🔥 KRİTİK: Çift Eleme Sistemi
     * Winners Bracket (WB) ve Losers Bracket (LB) arasındaki düşme mantığını kurar.
     */
    private fun createDoubleElimination(participants: List<String>, tId: String): List<TournamentMatch> {
        val wbMatches = createSingleElimination(participants, tId)
        val allMatches = mutableListOf<TournamentMatch>()
        allMatches.addAll(wbMatches)

        // Losers Bracket 1. Tur (WB 1. Tur kaybedenleri için yer tutucu maçlar)
        // Basitlik adına WB maç sayısı kadar LB başlangıç noktası oluşturuyoruz
        wbMatches.forEachIndexed { index, wbMatch ->
            val lbMatchId = "LB_R1_$index"

            // Winners maçına kaybedenin nereye gideceğini işaretle
            val updatedWbMatch = wbMatch.copy(loserTargetMatchId = lbMatchId)
            allMatches[allMatches.indexOf(wbMatch)] = updatedWbMatch

            // Boş LB maçını oluştur
            allMatches.add(TournamentMatch(
                matchId = lbMatchId,
                tournamentId = tId,
                player1 = "WB L-${index+1}", // WB kaybedeni bekleniyor
                player2 = "TBD",
                round = 1,
                bracketType = BracketType.LOSERS
            ))
        }

        return allMatches
    }

    /**
     * Maç bittiğinde kazananı ve kaybedeni yeni maçlarına gönderir.
     */
    fun updateTournamentProgress(
        finishedMatch: TournamentMatch,
        winnerName: String,
        winnerId: String,
        loserId: String,
        allMatches: List<TournamentMatch>
    ): List<TournamentMatch> {
        val mutableList = allMatches.toMutableList()

        // 1. Kazananı üst tura taşı
        finishedMatch.nextMatchId?.let { nextId ->
            val index = mutableList.indexOfFirst { it.matchId == nextId }
            if (index != -1) {
                val nextMatch = mutableList[index]
                val updated = if (nextMatch.player1 == "TBD" || nextMatch.player1.isEmpty()) {
                    nextMatch.copy(player1 = winnerName, player1Id = winnerId)
                } else {
                    nextMatch.copy(player2 = winnerName, player2Id = winnerId)
                }
                mutableList[index] = updated
            }
        }

        // 2. WB ise kaybedeni LB'ye düşür
        if (finishedMatch.bracketType == BracketType.WINNERS) {
            finishedMatch.loserTargetMatchId?.let { lbId ->
                val index = mutableList.indexOfFirst { it.matchId == lbId }
                if (index != -1) {
                    val lbMatch = mutableList[index]
                    // Kaybeden bilgisini LB maçına yaz
                    val updated = if (lbMatch.player1.contains("WB L-")) {
                        lbMatch.copy(player1 = "Loser of ${finishedMatch.matchId}", player1Id = loserId)
                    } else {
                        lbMatch.copy(player2 = "Loser of ${finishedMatch.matchId}", player2Id = loserId)
                    }
                    mutableList[index] = updated
                }
            }
        }

        return mutableList
    }
}