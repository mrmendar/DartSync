package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CricketActivity : AppCompatActivity() {

    private lateinit var targetList: List<DartTarget>
    private lateinit var adapter: DartAdapter
    private lateinit var etP1: EditText
    private lateinit var etP2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cricket)

        // 1. UI Bileşenlerini Tanımla
        etP1 = findViewById(R.id.etP1Name)
        etP2 = findViewById(R.id.etP2Name)

        // 2. İsimleri Intent'ten çek ve kutucuklara otomatik yaz
        val p1FromIntent = intent.getStringExtra("P1_NAME") ?: "Oyuncu 1"
        val p2FromIntent = intent.getStringExtra("P2_NAME") ?: "Oyuncu 2"

        etP1.setText(p1FromIntent)
        etP2.setText(p2FromIntent)

        // 3. Özel hedef listesi (20 -> 10 ve D, T, B, H)
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "Double", "Triple", "Bull", "House")
        targetList = labels.map { DartTarget(it) }

        // 4. RecyclerView Ayarları
        val rv = findViewById<RecyclerView>(R.id.rvCricketBoard)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = DartAdapter(
            targetList = targetList,
            isReadOnly = false,
            onWin = { playerIndex ->
                // 🔥 Kazananın kutusundaki güncel ismi çekiyoruz
                val winnerName = if (playerIndex == 1) etP1.text.toString() else etP2.text.toString()
                showWinDialog(winnerName)
            }
        )
        rv.adapter = adapter
    }

    private fun showWinDialog(winnerName: String) {
        AlertDialog.Builder(this)
            .setTitle("TEBRİKLER! 🏆")
            .setMessage("Kazanan: $winnerName\nMaç kaydedilsin mi?")
            .setPositiveButton("Kaydet ve Bitir") { _, _ -> saveGame(winnerName) }
            .setNegativeButton("Kapat", null)
            .setCancelable(false)
            .show()
    }

    private fun saveGame(winnerName: String) {
        lifecycleScope.launch {
            // İstatistiklere gidecek isimleri o anki kutucuklardan alıyoruz
            val currentP1 = etP1.text.toString().ifEmpty { "Oyuncu 1" }
            val currentP2 = etP2.text.toString().ifEmpty { "Oyuncu 2" }

            // Vuruşları metne dönüştür
            val p1Snapshot = targetList.joinToString(",") { it.player1Hits.toString() }
            val p2Snapshot = targetList.joinToString(",") { it.player2Hits.toString() }

            val result = GameResult(
                player1Name = currentP1,
                player2Name = currentP2,
                winnerName = winnerName,
                p1Snapshot = p1Snapshot,
                p2Snapshot = p2Snapshot,
                date = System.currentTimeMillis(),
                totalDarts = 0
            )

            // Veritabanına kaydet
            AppDatabase.getDatabase(this@CricketActivity).gameResultDao().insertGame(result)

            // Turnuva sonucunu paketle ve geri gönder
            val resultIntent = Intent()
            resultIntent.putExtra("WINNER_NAME", winnerName)
            setResult(RESULT_OK, resultIntent)

            finish()
        }
    }
}