package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FinishMasterActivity : AppCompatActivity() {

    private var targetLevel = 120 // Başlangıç hedefi
    private var currentRemaining = 120
    private var dartsThrownInLevel = 0
    private var currentInput = ""

    // 📊 İstatistik ve Takip Değişkenleri
    private var startTimeMillis: Long = 0
    private var totalDartsInSession = 0
    private var sessionLogs = "" // "120:3, 121:BUST, 122:FAIL" formatı için
    private var isSessionSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish_master)

        // Oturumu başlat
        startTimeMillis = System.currentTimeMillis()

        updateUI()
        setupKeypad()
    }

    private fun processDart(score: Int) {
        if (score > 60) {
            Toast.makeText(this, "Geçersiz Skor! Tek ok max 60 olabilir.", Toast.LENGTH_SHORT).show()
            return
        }

        dartsThrownInLevel++
        totalDartsInSession++
        val nextScore = currentRemaining - score

        when {
            // 1. BAŞARI: Sayı tam 0 oldu
            nextScore == 0 -> {
                logLevelResult(targetLevel, dartsThrownInLevel.toString())
                Toast.makeText(this, "TEBRİKLER! Seviye Tamamlandı.", Toast.LENGTH_SHORT).show()

                targetLevel++
                if (targetLevel > 170) {
                    Toast.makeText(this, "170 Tamamlandı! 🏆", Toast.LENGTH_LONG).show()
                    finishSessionAndSave()
                } else {
                    resetLevel()
                }
            }

            // 2. HATA (BUST): Sayı 1 kaldı veya 0'ın altına düştü
            nextScore < 0 || nextScore == 1 -> {
                logLevelResult(targetLevel, "BUST")
                Toast.makeText(this, "BUST! Aynı seviyeden tekrar başlıyorsun.", Toast.LENGTH_SHORT).show()
                resetLevel()
            }

            // 3. BAŞARISIZ: 9 ok doldu ama sayı bitmedi
            dartsThrownInLevel >= 9 -> {
                logLevelResult(targetLevel, "FAIL")
                Toast.makeText(this, "9 Ok Hakkın Doldu! Tekrar dene.", Toast.LENGTH_LONG).show()
                resetLevel()
            }

            // 4. DEVAM: Sayı hala pozitif ve ok hakkı var
            else -> {
                currentRemaining = nextScore
                updateUI()
            }
        }
    }

    private fun resetLevel() {
        currentRemaining = targetLevel
        dartsThrownInLevel = 0
        updateUI()
    }

    private fun updateUI() {
        val tvTarget = findViewById<TextView>(R.id.tvTargetLevel)
        val tvRemaining = findViewById<TextView>(R.id.tvCurrentScore)
        val tvDarts = findViewById<TextView>(R.id.tvDartsRemaining)
        val tvHint = findViewById<TextView>(R.id.tvCheckoutHint)

        tvTarget.text = "HEDEF: $targetLevel"
        tvRemaining.text = currentRemaining.toString()
        tvDarts.text = "Kalan Ok: ${9 - dartsThrownInLevel}"

        // 🔥 Dinamik Checkout Rehberi
        // Kalan ok sayısını 3 ok üzerinden hesapla (3, 2, 1)
        val dartsLeftInTurn = 3 - (dartsThrownInLevel % 3)
        tvHint.text = CheckoutHelper.getSuggestion(currentRemaining, dartsLeftInTurn)

        // Görsel Uyarı: Son 3 oka girince renk kırmızıya döner
        if (9 - dartsThrownInLevel <= 3) {
            tvDarts.setTextColor(Color.RED)
        } else {
            tvDarts.setTextColor(Color.parseColor("#FFD700"))
        }
    }

    private fun logLevelResult(level: Int, result: String) {
        sessionLogs += "$level:$result, "
    }

    private fun finishSessionAndSave() {
        if (isSessionSaved) return
        isSessionSaved = true

        val sessionDuration = System.currentTimeMillis() - startTimeMillis

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@FinishMasterActivity)
            val result = GameResult(
                player1Name = "Antrenman",
                player2Name = "FinishMaster",
                date = System.currentTimeMillis(),
                winnerName = "N/A",
                p1Snapshot = "0",
                p2Snapshot = "0",
                totalDarts = totalDartsInSession,

                // 🔥 Yeni İstatistik Verileri
                isFinishMasterMode = true,
                finishedLevels = sessionLogs.removeSuffix(", "),
                sessionDurationMillis = sessionDuration,
                highestLevelReached = if (currentRemaining == 0) targetLevel else targetLevel - 1
            )

            db.gameResultDao().insertGame(result)
            finish()
        }
    }

    override fun onBackPressed() {
        // Kullanıcı 170'e gelmeden çıkmak isterse ilerlemeyi kaydetmek için soralım
        AlertDialog.Builder(this)
            .setTitle("Antrenmanı Bitir")
            .setMessage("Şu ana kadarki ilerlemeni kaydetmek istiyor musun?")
            .setPositiveButton("Kaydet ve Çık") { _, _ -> finishSessionAndSave() }
            .setNegativeButton("Devam Et", null)
            .setNeutralButton("Kaydetmeden Çık") { _, _ -> super.onBackPressed() }
            .show()
    }

    private fun setupKeypad() {
        val tvInput = findViewById<TextView>(R.id.tvInput)

        // 0'dan 9'a kadar olan butonları döngüyle bağla
        for (i in 0..9) {
            val resId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(resId)?.setOnClickListener {
                if (currentInput.length < 3) {
                    currentInput += i.toString()
                    tvInput.text = currentInput
                }
            }
        }

        // Silme butonu
        findViewById<Button>(R.id.btnClear)?.setOnClickListener {
            currentInput = ""
            tvInput.text = "0"
        }

        // Onaylama butonu
        findViewById<Button>(R.id.btnEnterScore)?.setOnClickListener {
            val score = currentInput.toIntOrNull() ?: 0
            processDart(score) // Skor işleme fonksiyonun
            currentInput = ""
            tvInput.text = "0"
        }
    }
}