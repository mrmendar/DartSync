package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit



class TournamentBracketActivity : AppCompatActivity() {

    private lateinit var bracketContainer: LinearLayout
    private lateinit var viewKonfetti: KonfettiView
    private var selectedMode: String = "501"

    private val allRounds = mutableListOf<MutableList<TournamentMatch>>()
    private var lastClickedRoundIndex = -1
    private var lastClickedMatchIndex = -1

    // 🔥 MAÇ SONUCU ALICI
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val winnerName = result.data?.getStringExtra("WINNER_NAME")
            if (winnerName != null && lastClickedRoundIndex != -1) {
                allRounds[lastClickedRoundIndex][lastClickedMatchIndex].winner = winnerName
                generateNextRoundsFrom(lastClickedRoundIndex)
                renderBracket()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_bracket)

        bracketContainer = findViewById(R.id.bracketContainer)
        viewKonfetti = findViewById(R.id.viewKonfetti)

        val rawNames = intent.getStringArrayListExtra("NAMES") ?: arrayListOf()
        val cleanNames = rawNames.filter { it.isNotBlank() }.distinct()
        selectedMode = intent.getStringExtra("SELECTED_MODE") ?: "501"

        val firstRoundMatches = createInitialMatches(cleanNames).toMutableList()
        allRounds.clear()
        allRounds.add(firstRoundMatches)

        generateNextRoundsFrom(0)
        renderBracket()
    }

    private fun createInitialMatches(participants: List<String>): List<TournamentMatch> {
        val n = participants.size
        val nextPowerOfTwo = Math.pow(2.0, Math.ceil(Math.log(n.toDouble()) / Math.log(2.0))).toInt()
        val byeCount = nextPowerOfTwo - n
        val shuffledNames = participants.shuffled()
        val matches = mutableListOf<TournamentMatch>()

        var currentIndex = 0
        for (i in 0 until byeCount) {
            matches.add(TournamentMatch(shuffledNames[currentIndex], "BYE", shuffledNames[currentIndex], 1, true))
            currentIndex++
        }
        while (currentIndex < n) {
            matches.add(TournamentMatch(shuffledNames[currentIndex], shuffledNames[currentIndex + 1], null, 1))
            currentIndex += 2
        }
        return matches
    }

    private fun generateNextRoundsFrom(currentRoundIndex: Int) {
        val currentRound = allRounds[currentRoundIndex]

        if (currentRound.size > 1) {
            val winners = currentRound.map { it.winner ?: "Bekleniyor..." }
            val nextMatches = mutableListOf<TournamentMatch>()

            var i = 0
            while (i < winners.size) {
                if (i + 1 < winners.size) {
                    nextMatches.add(TournamentMatch(winners[i], winners[i + 1], null, currentRoundIndex + 2))
                    i += 2
                } else {
                    nextMatches.add(TournamentMatch(winners[i], "BYE", winners[i], currentRoundIndex + 2, true))
                    i += 1
                }
            }

            if (allRounds.size > currentRoundIndex + 1) {
                allRounds[currentRoundIndex + 1] = nextMatches
                generateNextRoundsFrom(currentRoundIndex + 1)
            } else {
                allRounds.add(nextMatches)
            }
        } else if (currentRound.size == 1 && currentRound[0].winner != null) {
            // 🎉 ŞAMPİYON BELLİ OLDU!
            triggerChampionCelebration(currentRound[0].winner!!)
        }
    }

    private fun triggerChampionCelebration(winner: String) {
        // 1. Konfeti Partisi Ayarları
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x00D1FF, 0x00D26A),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 5, TimeUnit.SECONDS).perSecond(100)
        )
        viewKonfetti.start(party)

        // 2. Diyalog Göster
        AlertDialog.Builder(this)
            .setTitle("🏆 TURNUVA ŞAMPİYONU 🏆")
            .setMessage("Tebrikler $winner!\nDartSync Turnuvasını kazandın.")
            .setPositiveButton("Kupayı Al") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun renderBracket() {
        bracketContainer.removeAllViews()
        val matchHeight = 200
        val baseMargin = 40

        allRounds.forEachIndexed { rIndex, roundMatches ->
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP
                setPadding(60, 0, 60, 0)
            }

            // --- 📐 SİMETRİK AĞAÇ MATEMATİĞİ ---
            val initialTopMargin = ((Math.pow(2.0, rIndex.toDouble()) - 1) * (matchHeight / 2 + baseMargin)).toInt()
            val verticalMargin = (Math.pow(2.0, rIndex.toDouble()) * baseMargin).toInt()

            roundMatches.forEachIndexed { mIndex, match ->
                val matchView = layoutInflater.inflate(R.layout.item_match_node, column, false)
                val tvTop = matchView.findViewById<TextView>(R.id.tvPlayerTop)
                val tvBottom = matchView.findViewById<TextView>(R.id.tvPlayerBottom)

                tvTop.text = match.player1
                tvBottom.text = match.player2

                if (match.player2 == "BYE") {
                    tvBottom.text = "--- BYE ---"
                    tvBottom.setTextColor(Color.GRAY)
                    matchView.alpha = 0.6f
                }

                if (match.winner != null) {
                    if (match.winner == match.player1) tvTop.setTextColor(Color.parseColor("#00D26A"))
                    else tvBottom.setTextColor(Color.parseColor("#00D26A"))
                }

                matchView.setOnClickListener {
                    if (match.winner == null && match.player1 != "Bekleniyor..." &&
                        match.player2 != "Bekleniyor..." && match.player2 != "BYE") {

                        lastClickedRoundIndex = rIndex
                        lastClickedMatchIndex = mIndex

                        val target = if (selectedMode.uppercase() == "CRICKET") CricketActivity::class.java else MainActivity::class.java
                        val intent = Intent(this, target).apply {
                            putExtra("P1_NAME", match.player1)
                            putExtra("P2_NAME", match.player2)
                            putExtra("IS_TOURNAMENT", true)
                        }
                        getResult.launch(intent)
                    }
                }

                val params = LinearLayout.LayoutParams(550, matchHeight)
                val top = if (mIndex == 0) initialTopMargin else (verticalMargin * 2)
                params.setMargins(0, top, 0, 0)
                column.addView(matchView, params)
            }
            bracketContainer.addView(column)
        }
    }
}