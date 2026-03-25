package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // 1. UI Bileşenlerini Bağla

        val editP1Name = findViewById<EditText>(R.id.editPlayer1Name)
        val editP2Name = findViewById<EditText>(R.id.editPlayer2Name)
        val tvP1Score = findViewById<TextView>(R.id.tvP1Remaining)
        val tvP2Score = findViewById<TextView>(R.id.tvP2Remaining)
        val tvInputDisplay = findViewById<TextView>(R.id.tvCurrentInput)
        val tvHint = findViewById<TextView>(R.id.tvCheckoutHint)
        val btnReset = findViewById<ImageButton>(R.id.btnReset)
        val btnEnter = findViewById<Button>(R.id.btnEnterScore)

        val p1FromIntent = intent.getStringExtra("P1_NAME")
        val p2FromIntent = intent.getStringExtra("P2_NAME")

        if (!p1FromIntent.isNullOrEmpty()) {
            editP1Name.setText(p1FromIntent)
        }
        if (!p2FromIntent.isNullOrEmpty()) {
            editP2Name.setText(p2FromIntent)
        }
        // 2. Siyah Ekran Önlemi: Ekranın çizilmesi için 100ms bekle ve sonra Dialog'u aç
        window.decorView.postDelayed({
            if (!isFinishing) {
                showStartDialog(editP1Name, editP2Name, tvP1Score, tvP2Score, tvHint)
            }
        }, 100)

        // 3. Tuş Takımı Bağlantısı (Daha Güvenli Versiyon)
        setupKeypad(tvInputDisplay)

        // 4. Skor Onaylama
        btnEnter?.setOnClickListener {
            val inputVal = currentInput.toIntOrNull() ?: 0
            if (inputVal > 180) {
                Toast.makeText(this, "Geçersiz Skor! (Max 180)", Toast.LENGTH_SHORT).show()
                clearInput(tvInputDisplay)
            } else if (currentInput.isNotEmpty()) {
                totalDartsThrown += 3
                processTurn(inputVal, editP1Name.text.toString(), editP2Name.text.toString())
                clearInput(tvInputDisplay)
                updateUI(tvP1Score, tvP2Score, tvHint)
            }
        }

        btnReset?.setOnClickListener {
            showStartDialog(editP1Name, editP2Name, tvP1Score, tvP2Score, tvHint)
        }
    }

    private fun showStartDialog(e1: EditText, e2: EditText, t1: TextView, t2: TextView, hint: TextView) {
        val modes = arrayOf("301 Modu", "501 Modu")
        AlertDialog.Builder(this)
            .setTitle("Oyun Modu Seç")
            .setItems(modes) { _, which ->
                startingScore = if (which == 0) 301 else 501
                resetGame(t1, t2, hint)
            }
            .setCancelable(false)
            .show()
    }

    private fun setupKeypad(display: TextView) {
        // Buton ID'lerini tek tek kontrol ederek bağlayalım (Çökmeyi önler)
        for (i in 0..9) {
            val resId = resources.getIdentifier("btn$i", "id", packageName)
            if (resId != 0) {
                findViewById<Button>(resId)?.setOnClickListener {
                    if (currentInput.length < 3) {
                        currentInput += i.toString()
                        display.text = currentInput
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnClear)?.setOnClickListener {
            clearInput(display)
        }
    }

    private fun clearInput(display: TextView) {
        currentInput = ""
        display.text = "0"
    }

    private fun processTurn(score: Int, p1Name: String, p2Name: String) {
        val current = if (currentPlayer == 1) p1Remaining else p2Remaining
        val next = current - score

        when {
            next == 0 -> {
                if (currentPlayer == 1) p1Remaining = 0 else p2Remaining = 0
                showWinDialog(p1Name, p2Name)
            }
            next < 0 || next == 1 -> {
                Toast.makeText(this, "BUST! (Sıra Değişti)", Toast.LENGTH_SHORT).show()
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
            else -> {
                if (currentPlayer == 1) p1Remaining = next else p2Remaining = next
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
        }
    }

    private fun updateUI(t1: TextView, t2: TextView, hint: TextView) {
        t1.text = p1Remaining.toString()
        t2.text = p2Remaining.toString()

        // 1. Sıradaki oyuncuyu renklendir
        if (currentPlayer == 1) {
            t1.setTextColor(Color.parseColor("#00D1FF")) // Neon Mavi
            t2.setTextColor(Color.WHITE)
        } else {
            t1.setTextColor(Color.WHITE)
            t2.setTextColor(Color.parseColor("#00D26A")) // Neon Yeşil
        }

        // 2. 🔥 Zeki Algoritmayı Çalıştır
        val currentScore = if (currentPlayer == 1) p1Remaining else p2Remaining

        // Algoritmamızdan rotayı alıp ekrana yazdırıyoruz
        hint.text = CheckoutHelper.getSuggestion(currentScore)

        // Eğer bitiriş bölgesindeyse metni biraz parlatabiliriz
        if (currentScore <= 170) {
            hint.setTextColor(Color.parseColor("#FFD700")) // Altın Sarısı
        } else {
            hint.setTextColor(Color.parseColor("#A0A0A0")) // Gri
        }
    }

    private fun getCheckoutPath(score: Int): String {
        return when (score) {
            170 -> "🎯 T20 + T20 + Bull"
            160 -> "🎯 T20 + T20 + D20"
            100 -> "🎯 T20 + D20"
            in 2..158 -> "Bitiriş Bölgesindesin!"
            else -> "Sayı düşmeye devam..."
        }
    }

    private fun resetGame(t1: TextView, t2: TextView, hint: TextView) {
        p1Remaining = startingScore
        p2Remaining = startingScore
      totalDartsThrown = 0
        currentPlayer = 1
        updateUI(t1, t2, hint)
    }

    private fun showWinDialog(p1Name: String, p2Name: String) {
        val winner = if (p1Remaining == 0) p1Name.ifEmpty { "Oyuncu 1" } else p2Name.ifEmpty { "Oyuncu 2" }
        AlertDialog.Builder(this)
            .setTitle("MAÇ BİTTİ! 🏆")
            .setMessage("Kazanan: $winner")
            .setPositiveButton("Kaydet") { _, _ -> saveToDb(p1Name, p2Name, winner) }
            .setCancelable(false)
            .show()
    }

    private fun saveToDb(p1: String, p2: String, winner: String) {
        lifecycleScope.launch {
            val result = GameResult(
                player1Name = p1.ifEmpty { "Oyuncu 1" },
                player2Name = p2.ifEmpty { "Oyuncu 2" },
                date = System.currentTimeMillis(),
                p1Snapshot = p1Remaining.toString(),
                p2Snapshot = p2Remaining.toString(),
                winnerName = winner,
                totalDarts = totalDartsThrown,



            )
            AppDatabase.getDatabase(this@MainActivity).gameResultDao().insertGame(result)
            finish()
        }
        val resultIntent = Intent()
        resultIntent.putExtra("WINNER_NAME", winner)
        setResult(RESULT_OK, resultIntent) // "İşlem tamam, işte sonuç" diyoruz

        finish() // Maç bitti, turnuva ağacına geri dön
    }
}