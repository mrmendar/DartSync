package com.q.dartsync

import kotlin.math.log2
import kotlin.math.pow

object TournamentLogic {

    fun createMatches(participants: List<String>): List<TournamentMatch> {
        val n = participants.size
        if (n < 2) return emptyList()

        // 1. Katılımcı sayısından büyük veya eşit olan en küçük 2'nin kuvvetini bul (Örn: 5 için 8)
        val nextPowerOfTwo = 2.0.pow(kotlin.math.ceil(log2(n.toDouble()))).toInt()

        // 2. Kaç kişi BYE (maç yapmadan geçiş) alacak?
        val byeCount = nextPowerOfTwo - n

        val shuffledNames = participants.shuffled()
        val matches = mutableListOf<TournamentMatch>()

        // 3. BYE geçecek şanslı kişileri belirle (Listenin başındakiler)
        var currentIndex = 0
        for (i in 0 until byeCount) {
            matches.add(
                TournamentMatch(
                    player1 = shuffledNames[currentIndex],
                    player2 = "BYE", // Rakip yok, otomatik üst tur
                    winner = shuffledNames[currentIndex], // Zaten kazandı sayılıyor
                    round = 1
                )
            )
            currentIndex++
        }

        // 4. Kalan kişileri birbiriyle eşleştir
        while (currentIndex < n) {
            matches.add(
                TournamentMatch(
                    player1 = shuffledNames[currentIndex],
                    player2 = shuffledNames[currentIndex + 1],
                    round = 1
                )
            )
            currentIndex += 2
        }

        return matches
    }
}