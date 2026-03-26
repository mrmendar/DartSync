package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class TournamentBracketActivity : AppCompatActivity() {

    private var selectedMode: String = "501"
    private var lastPlayedMatchId: Int = -1 // 1: Match1, 2: Match2, 3: Final

    // Kazananların isimlerini tutmak için (Final maçını kurabilmek adına)
    private var winner1: String? = null
    private var winner2: String? = null

    // 🔥 AKILLI LAUNCHER: Maç bittiğinde kazananı alır ve ağaca yerleştirir
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val winnerName = result.data?.getStringExtra("WINNER_NAME")
            if (winnerName != null) {
                updateBracketAfterMatch(winnerName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_bracket)

        // 1. Verileri Al
        val names = intent.getStringArrayListExtra("NAMES") ?: arrayListOf()
        selectedMode = intent.getStringExtra("SELECTED_MODE") ?: "501"

        if (names.size < 4) {
            Toast.makeText(this, "Hata: En az 4 oyuncu gerekli!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Yarı Final 1 (match1)
        setupMatchView(findViewById(R.id.match1), names[0], names[1], 1)

        // 3. Yarı Final 2 (match2)
        setupMatchView(findViewById(R.id.match2), names[2], names[3], 2)

        // 4. Final Maçı (Başta boş)
        updateFinalView()
    }

    private fun setupMatchView(matchView: View, p1: String, p2: String, matchId: Int) {
        val tvTop = matchView.findViewById<TextView>(R.id.tvPlayerTop)
        val tvBottom = matchView.findViewById<TextView>(R.id.tvPlayerBottom)

        tvTop.text = p1
        tvBottom.text = p2

        matchView.setOnClickListener {
            if (p1 == "Bekleniyor..." || p2 == "Bekleniyor...") {
                Toast.makeText(this, "Önce yarı finaller bitmeli!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lastPlayedMatchId = matchId
            val targetActivity = if (selectedMode == "CRICKET") CricketActivity::class.java else MainActivity::class.java

            val intent = Intent(this, targetActivity).apply {
                putExtra("P1_NAME", p1)
                putExtra("P2_NAME", p2)
                putExtra("IS_TOURNAMENT", true)
            }
            getResult.launch(intent)
        }
    }

    private fun updateBracketAfterMatch(winnerName: String) {
        when (lastPlayedMatchId) {
            1 -> {
                winner1 = winnerName
                Toast.makeText(this, "1. Finalist: $winnerName", Toast.LENGTH_SHORT).show()
            }
            2 -> {
                winner2 = winnerName
                Toast.makeText(this, "2. Finalist: $winnerName", Toast.LENGTH_SHORT).show()
            }
            3 -> {
                // ŞAMPİYON BELLİ OLDU
                showChampionDialog(winnerName)
            }
        }
        updateFinalView()
    }

    private fun updateFinalView() {
        val finalMatchView = findViewById<View>(R.id.matchFinal)
        val p1 = winner1 ?: "Bekleniyor..."
        val p2 = winner2 ?: "Bekleniyor..."

        setupMatchView(finalMatchView, p1, p2, 3)

        // Finalistlerin ismini yeşil yapalım (Vurgu)
        if (winner1 != null) finalMatchView.findViewById<TextView>(R.id.tvPlayerTop).setTextColor(android.graphics.Color.parseColor("#00D26A"))
        if (winner2 != null) finalMatchView.findViewById<TextView>(R.id.tvPlayerBottom).setTextColor(android.graphics.Color.parseColor("#00D26A"))
    }

    private fun showChampionDialog(winnerName: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🏆 TURNUVA ŞAMPİYONU 🏆")
            .setMessage("Tebrikler $winnerName! DartSync Turnuvasını kazandın.")
            .setPositiveButton("Bitir") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}