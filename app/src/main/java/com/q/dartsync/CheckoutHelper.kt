package com.q.dartsync

object CheckoutHelper {

    // Key: Kalan Skor
    // Value: Triple (3 ok rotası, 2 ok rotası (null ise default), 1 ok rotası)
    private val checkouts = mapOf(
        170 to Triple(listOf("T20", "T20", "DB"), null, "DB"),
        167 to Triple(listOf("T20", "T19", "DB"), null, "DB"),
        164 to Triple(listOf("T19", "T19", "DB"), null, "DB"),
        161 to Triple(listOf("T20", "T17", "DB"), null, "DB"),
        160 to Triple(listOf("T20", "T20", "D20"), null, "D20"),
        158 to Triple(listOf("T20", "T20", "D19"), null, "D19"),
        157 to Triple(listOf("T20", "T19", "D20"), null, "D20"),
        156 to Triple(listOf("T20", "T20", "D18"), null, "D18"),
        155 to Triple(listOf("T20", "T19", "D19"), null, "D19"),
        154 to Triple(listOf("T19", "T19", "D20"), null, "D20"),
        153 to Triple(listOf("T20", "T19", "D18"), null, "D18"),
        152 to Triple(listOf("T20", "T20", "D16"), null, "D16"),
        151 to Triple(listOf("T20", "T17", "D20"), null, "D20"),
        150 to Triple(listOf("T19", "T19", "D18"), null, "D18"),
        149 to Triple(listOf("T20", "T19", "D16"), null, "D16"),
        148 to Triple(listOf("T20", "T20", "D14"), null, "D14"),
        147 to Triple(listOf("T20", "T17", "D18"), null, "D18"),
        146 to Triple(listOf("T19", "T19", "D16"), null, "D16"),
        145 to Triple(listOf("T20", "T15", "D20"), null, "D20"),
        144 to Triple(listOf("T20", "T20", "D12"), null, "D12"),
        143 to Triple(listOf("T20", "T17", "D16"), null, "D16"),
        142 to Triple(listOf("T20", "T14", "D20"), null, "D20"),
        141 to Triple(listOf("T20", "T19", "D12"), null, "D12"),
        140 to Triple(listOf("T20", "T20", "D10"), null, "D10"),
        130 to Triple(listOf("T20", "T20", "D5"), null, "D5"),
        121 to Triple(listOf("T20", "T11", "D14"), null, "D14"),
        110 to Triple(listOf("T20", "10", "D20"), listOf("T20", "DB"), "DB"), // w/2 durumu
        107 to Triple(listOf("T19", "10", "D20"), listOf("T19", "DB"), "DB"), // w/2 durumu
        104 to Triple(listOf("T18", "10", "D20"), listOf("T18", "DB"), "DB"), // w/2 durumu
        101 to Triple(listOf("T17", "10", "D20"), listOf("T17", "DB"), "DB"), // w/2 durumu
        100 to Triple(listOf("T20", "D20"), null, "D20"),
        90 to Triple(listOf("T18", "D18"), listOf("T20", "D15"), "D18"),
        80 to Triple(listOf("T20", "D10"), null, "D10"),
        70 to Triple(listOf("T10", "D20"), listOf("SB", "D22"), "D20"),
        60 to Triple(listOf("20", "D20"), null, "D20"),
        50 to Triple(listOf("DB"), null, "DB"),
        40 to Triple(listOf("D20"), null, "D20")
    )

    /**
     * @param score Kalan sayı
     * @param dartsLeft Bu turda atılacak kaç ok kaldı (3, 2, 1)
     */
    fun getSuggestion(score: Int, dartsLeft: Int = 3): String {
        // "imat" ismi kaldırıldı, profesyonel bir dil kullanıldı.
        if (score > 170) return "Sayı düşmeye devam..."
        if (score == 1) return "BUST! (1 Sayısı Bitirilemez)"

        val entry = checkouts[score]

        return if (entry != null) {
            when (dartsLeft) {
                3 -> "🎯 " + entry.first.joinToString(" - ")
                2 -> {
                    val path2 = entry.second ?: entry.first.takeLast(2)
                    "🎯 " + path2.joinToString(" - ")
                }
                1 -> "🎯 " + entry.third
                else -> "Sıra değişiyor..."
            }
        } else {
            // Tabloda olmayan düşük sayılar için akıllı mantık
            generateSmartPath(score, dartsLeft)
        }
    }

    private fun generateSmartPath(score: Int, dartsLeft: Int): String {
        return when {
            score <= 40 && score % 2 == 0 -> {
                val dVal = score / 2
                "🎯 D$dVal"
            }
            score < 40 -> {
                val single = 1
                val remainder = score - single
                if (dartsLeft > 1) "🎯 $single - D${remainder / 2}" else "🎯 $single (D için)"
            }
            score in 41..99 -> {
                if (dartsLeft > 1) "🎯 Tekli atışla Double ayarla" else "🎯 Triple veya Double odaklan"
            }
            else -> "Sayı düşmeye devam..."
        }
    }
}