package com.q.dartsync

object CheckoutHelper {

    fun getSuggestion(score: Int): String {
        return when (score) {
            170 -> "🎯 T20 - T20 - Bullseye"
            167 -> "🎯 T20 - T19 - Bullseye"
            164 -> "🎯 T20 - T18 - Bullseye"
            161 -> "🎯 T20 - T17 - Bullseye"
            160 -> "🎯 T20 - T20 - D20"
            158 -> "🎯 T20 - T20 - D19"
            150 -> "🎯 T20 - T18 - D18"
            140 -> "🎯 T20 - T20 - D10"
            121 -> "🎯 T20 - T15 - D8"
            100 -> "🎯 T20 - D20"
            90 -> "🎯 T18 - D18"
            80 -> "🎯 T20 - D10"
            70 -> "🎯 T10 - D20"
            60 -> "🎯 S20 - D20"
            50 -> "🎯 Bullseye"
            40 -> "🎯 D20 (Tops)"
            32 -> "🎯 D16"
            24 -> "🎯 D12"
            16 -> "🎯 D8"
            8  -> "🎯 D4"
            4  -> "🎯 D2"
            2  -> "🎯 D1"

            // Genel kural: 170 altı ama özel rotası yoksa
            in 101..169 -> "🎯 Triple bölgelerine odaklan"
            in 41..99 -> "🎯 Tekli atışla Double'a hazırlan"
            in 1..39 -> if (score % 2 == 0) "🎯 Double ${score / 2} dene" else "🎯 Tekli ${score - (score - 1)} atıp Double'a kal"

            else -> "Sayı düşmeye devam imat..."
        }
    }
}