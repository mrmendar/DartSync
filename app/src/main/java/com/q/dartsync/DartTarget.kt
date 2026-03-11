package com.q.dartsync

data class DartTarget(
    val label: String,
    var player1Hits: Int = 0, // 0: boş, 1: /, 2: X, 3: ⭕, 4: Noktalı O
    var player2Hits: Int = 0
)