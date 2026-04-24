package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var totalDartsThrown = 0
    private var startingScore = 501
    private var p1Remaining = 501
    private var p2Remaining = 501
    private var currentPlayer = 1
    private var currentInput = ""
    private var dartsInTurn = 0

    // 🔥 TURNUVA VE BEST OF 3 DEĞİŞKENLERİ
    private var p1Legs = 0
    private var p2Legs = 0
    private val targetLegs = 2 // 2 olan kazanır (Best of 3)
    private var isTournament = false
    private var matchId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editP1Name = findViewById<EditText>(R.id.editPlayer1Name)
        val editP2Name = findViewById<EditText>(R.id.editPlayer2Name)
        val tvP1Score = findViewById<TextView>(R.id.tvP1Remaining)
        val tvP2Score = findViewById<TextView>(R.id.tvP2Remaining)
        val tvInputDisplay = findViewById<TextView>(R.id.tvCurrentInput)
        val tvHint = findViewById<TextView>(R.id.tvCheckoutHint)
        val btnReset = findViewById<ImageButton>(R.id.btnReset)
        val btnEnter = findViewById<Button>(R.id.btnEnterScore)

        // 🔥 Turnuva Bilgilerini Al
        isTournament = intent.getBooleanExtra("IS_TOURNAMENT", false)
        matchId = intent.getStringExtra("MATCH_ID")
        val p1FromIntent = intent.getStringExtra("P1_NAME")
        val p2FromIntent = intent.getStringExtra("P2_NAME")

        if (!p1FromIntent.isNullOrEmpty()) editP1Name.setText(p1FromIntent)
        if (!p2FromIntent.isNullOrEmpty()) editP2Name.setText(p2FromIntent)

        window.decorView.postDelayed({
            if (!isFinishing) {
                showStartDialog(editP1Name, editP2Name, tvP1Score, tvP2Score, tvHint)
            }
        }, 100)

        setupKeypad(tvInputDisplay)

        btnEnter?.setOnClickListener {
            val score = currentInput.toIntOrNull() ?: 0
            if (score > 60) {
                Toast.makeText(this, "Geçersiz Skor! Tek ok max 60.", Toast.LENGTH_SHORT).show()
                clearInput(tvInputDisplay)
            } else if (currentInput.isNotEmpty()) {
                processSingleDart(score, editP1Name.text.toString(), editP2Name.text.toString(), tvP1Score, tvP2Score, tvHint)
                clearInput(tvInputDisplay)
                updateUI(tvP1Score, tvP2Score, tvHint)
            }
        }

        btnReset?.setOnClickListener {
            showStartDialog(editP1Name, editP2Name, tvP1Score, tvP2Score, tvHint)
        }
    }

    private fun processSingleDart(score: Int, p1Name: String, p2Name: String, t1: TextView, t2: TextView, hint: TextView) {
        val currentScore = if (currentPlayer == 1) p1Remaining else p2Remaining
        val nextScore = currentScore - score

        when {
            // 🏆 LEG BİTTİ (KAZANMA)
            nextScore == 0 -> {
                if (currentPlayer == 1) p1Remaining = 0 else p2Remaining = 0
                totalDartsThrown += (dartsInTurn + 1)
                handleLegWin(p1Name, p2Name, t1, t2, hint)
            }

            // ❌ BUST
            nextScore < 0 || nextScore == 1 -> {
                Toast.makeText(this, "BUST! Sıra değişti.", Toast.LENGTH_SHORT).show()
                totalDartsThrown += 3
                switchPlayer()
            }

            // ✅ GEÇERLİ ATIŞ
            else -> {
                if (currentPlayer == 1) p1Remaining = nextScore else p2Remaining = nextScore
                dartsInTurn++
                if (dartsInTurn == 3) {
                    totalDartsThrown += 3
                    switchPlayer()
                }
            }
        }
    }

    private fun handleLegWin(p1Name: String, p2Name: String, t1: TextView, t2: TextView, hint: TextView) {
        if (currentPlayer == 1) p1Legs++ else p2Legs++

        // MAÇ BİTTİ Mİ? (Bir taraf 2 oldu mu?)
        if (p1Legs >= targetLegs || p2Legs >= targetLegs) {
            showWinDialog(p1Name, p2Name)
        } else {
            // SADECE LEG BİTTİ, SKORLARI SIFIRLA
            Toast.makeText(this, "Leg Kazanıldı! Skor: $p1Legs - $p2Legs", Toast.LENGTH_LONG).show()
            resetForNextLeg(t1, t2, hint)
        }
    }

    private fun resetForNextLeg(t1: TextView, t2: TextView, hint: TextView) {
        p1Remaining = startingScore
        p2Remaining = startingScore
        dartsInTurn = 0
        currentPlayer = if ((p1Legs + p2Legs) % 2 == 0) 1 else 2 // Leg başlangıç sırası değişsin
        updateUI(t1, t2, hint)
    }

    private fun switchPlayer() {
        dartsInTurn = 0
        currentPlayer = if (currentPlayer == 1) 2 else 1
    }

    private fun updateUI(t1: TextView, t2: TextView, hint: TextView) {
        t1.text = p1Remaining.toString()
        t2.text = p2Remaining.toString()

        if (currentPlayer == 1) {
            t1.setTextColor(Color.parseColor("#00D1FF"))
            t2.setTextColor(Color.WHITE)
        } else {
            t1.setTextColor(Color.WHITE)
            t2.setTextColor(Color.parseColor("#00D26A"))
        }

        // Skor Tablosuna Leg Bilgisini Yazdır
        val p1NameView = findViewById<EditText>(R.id.editPlayer1Name)
        val p2NameView = findViewById<EditText>(R.id.editPlayer2Name)
        p1NameView.setHint("P1 (Legs: $p1Legs)")
        p2NameView.setHint("P2 (Legs: $p2Legs)")

        val currentRem = if (currentPlayer == 1) p1Remaining else p2Remaining
        hint.text = CheckoutHelper.getSuggestion(currentRem, 3 - dartsInTurn)
    }

    private fun setupKeypad(display: TextView) {
        for (i in 0..9) {
            val resId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(resId)?.setOnClickListener {
                if (currentInput.length < 3) {
                    currentInput += i.toString()
                    display.text = currentInput
                }
            }
        }
        findViewById<Button>(R.id.btnClear)?.setOnClickListener { clearInput(display) }
    }

    private fun clearInput(display: TextView) {
        currentInput = ""
        display.text = "0"
    }

    private fun showStartDialog(e1: EditText, e2: EditText, t1: TextView, t2: TextView, hint: TextView) {
        val modes = arrayOf("301 Modu", "501 Modu")
        AlertDialog.Builder(this)
            .setTitle("Oyun Modu Seç")
            .setItems(modes) { _, which ->
                startingScore = if (which == 0) 301 else 501
                p1Legs = 0
                p2Legs = 0
                resetGame(t1, t2, hint)
            }
            .setCancelable(false)
            .show()
    }

    private fun resetGame(t1: TextView, t2: TextView, hint: TextView) {
        p1Remaining = startingScore
        p2Remaining = startingScore
        totalDartsThrown = 0
        dartsInTurn = 0
        currentPlayer = 1
        updateUI(t1, t2, hint)
    }

    private fun showWinDialog(p1Name: String, p2Name: String) {
        val winner = if (p1Legs > p2Legs) p1Name.ifEmpty { "Oyuncu 1" } else p2Name.ifEmpty { "Oyuncu 2" }
        AlertDialog.Builder(this)
            .setTitle("MAÇ BİTTİ! 🏆")
            .setMessage("Kazanan: $winner\nSkor: $p1Legs - $p2Legs\nToplam $totalDartsThrown ok.")
            .setPositiveButton("Sonucu Onayla") { _, _ -> saveToDb(p1Name, p2Name, winner) }
            .setCancelable(false)
            .show()
    }

    private fun saveToDb(p1: String, p2: String, winner: String) {
        lifecycleScope.launch {
            // Room DB Kaydı (İstatistikler için)
            val result = GameResult(
                player1Name = p1.ifEmpty { "Oyuncu 1" },
                player2Name = p2.ifEmpty { "Oyuncu 2" },
                date = System.currentTimeMillis(),
                p1Snapshot = p1Legs.toString(),
                p2Snapshot = p2Legs.toString(),
                winnerName = winner,
                totalDarts = totalDartsThrown,
                userId = IdManager.getGuestId(this@MainActivity)
            )
            AppDatabase.getDatabase(this@MainActivity).gameResultDao().insertGame(result)

            // 🔥 TURNUVA AĞACINA VERİ GÖNDERME
            val resultIntent = Intent()
            resultIntent.putExtra("WINNER_NAME", winner)
            resultIntent.putExtra("WINNER_ID", winner) // ID sistemi isim üzerine kuruluysa
            resultIntent.putExtra("MATCH_ID", matchId)
            resultIntent.putExtra("P1_SCORE", p1Legs)
            resultIntent.putExtra("P2_SCORE", p2Legs)

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}