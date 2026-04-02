package com.q.dartsync

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Game501Activity : AppCompatActivity() {

    private var p1Score = 501
    private var p2Score = 501
    private var currentPlayer = 1
    private var currentInput = ""

    // 🔥 PPD Takibi için değişken
    private var totalDartsThrown = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val p1NameFromIntent = intent.getStringExtra("P1_NAME") ?: "Oyuncu 1"
        val p2NameFromIntent = intent.getStringExtra("P2_NAME") ?: "Oyuncu 2"
        val startScore = intent.getIntExtra("START_SCORE", 501)

        p1Score = startScore
        p2Score = startScore

        val editP1Name = findViewById<EditText>(R.id.editPlayer1Name)
        val editP2Name = findViewById<EditText>(R.id.editPlayer2Name)
        val tvP1Score = findViewById<TextView>(R.id.tvP1Remaining)
        val tvP2Score = findViewById<TextView>(R.id.tvP2Remaining)
        val tvInput = findViewById<TextView>(R.id.tvCurrentInput)
        val tvHint = findViewById<TextView>(R.id.tvCheckoutHint)

        editP1Name.setText(p1NameFromIntent)
        editP2Name.setText(p2NameFromIntent)

        updateUI(tvP1Score, tvP2Score, tvHint)

        // Sayı Butonlarını Bağlama
        for (i in 0..9) {
            val btnId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(btnId)?.setOnClickListener {
                if (currentInput.length < 3) {
                    currentInput += (it as Button).text
                    tvInput.text = currentInput
                }
            }
        }

        findViewById<Button>(R.id.btnClear)?.setOnClickListener {
            currentInput = ""
            tvInput.text = "0"
        }

        findViewById<Button>(R.id.btnEnterScore)?.setOnClickListener {
            val score = currentInput.toIntOrNull() ?: 0
            if (score > 180) {
                Toast.makeText(this, "Geçersiz Skor! (Maks 180)", Toast.LENGTH_SHORT).show()
            } else if (currentInput.isNotEmpty()) {
                // 🔥 Her geçerli girişte ok sayısını 3 artırıyoruz
                totalDartsThrown += 3

                processScore(score, editP1Name.text.toString(), editP2Name.text.toString())
                currentInput = ""
                tvInput.text = "0"
                updateUI(tvP1Score, tvP2Score, tvHint)
            }
        }
    }

    private fun processScore(score: Int, p1Name: String, p2Name: String) {
        val current = if (currentPlayer == 1) p1Score else p2Score
        val next = current - score

        when {
            next == 0 -> {
                if (currentPlayer == 1) p1Score = 0 else p2Score = 0
                handleWin(if (currentPlayer == 1) p1Name else p2Name, p1Name, p2Name)
            }
            next < 0 || next == 1 -> {
                Toast.makeText(this, "BUST! Sıra rakibe geçti.", Toast.LENGTH_SHORT).show()
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
            else -> {
                if (currentPlayer == 1) p1Score = next else p2Score = next
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
        }
    }

    private fun updateUI(tv1: TextView, tv2: TextView, hint: TextView) {
        tv1.text = p1Score.toString()
        tv2.text = p2Score.toString()

        val currentRem = if (currentPlayer == 1) p1Score else p2Score

        // Sırası geleni parlat ve zeki tavsiyeyi ver
        if (currentPlayer == 1) {
            tv1.setTextColor(Color.parseColor("#00D1FF"))
            tv2.setTextColor(Color.WHITE)
        } else {
            tv1.setTextColor(Color.WHITE)
            tv2.setTextColor(Color.parseColor("#00D26A"))
        }

        // 🔥 Atış Rotası Algoritmasını (CheckoutHelper) Çalıştır
        hint.text = CheckoutHelper.getSuggestion(currentRem)

        if (currentRem <= 170) {
            hint.setTextColor(Color.parseColor("#FFD700")) // Bitiriş bölgesi altın sarısı
        } else {
            hint.setTextColor(Color.parseColor("#A0A0A0"))
        }
    }

    private fun handleWin(winnerName: String, p1Name: String, p2Name: String) {
        AlertDialog.Builder(this)
            .setTitle("ŞAMPİYON BELLİ OLDU! 🏆")
            .setMessage("Tebrikler $winnerName!\nToplam $totalDartsThrown ok ile bitirdin.")
            .setCancelable(false)
            .setPositiveButton("Sonucu Kaydet ve Bitir") { _, _ ->

                val gameResult = GameResult(
                    player1Name = p1Name.ifEmpty { "Oyuncu 1" },
                    player2Name = p2Name.ifEmpty { "Oyuncu 2" },
                    date = System.currentTimeMillis(),
                    p1Snapshot = p1Score.toString(),
                    p2Snapshot = p2Score.toString(),
                    winnerName = winnerName,
                    totalDarts = totalDartsThrown,
                    userId = IdManager.getGuestId(this)
                )

                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@Game501Activity)
                    db.gameResultDao().insertGame(gameResult)
                    finish()
                }
            }
            .show()
    }
}