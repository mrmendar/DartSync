package com.q.dartsync

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Veri Kaynağını Hazırla (20'den H'ye kadar olan hedefler)
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "D", "T", "B", "H")
        val gameTargets = labels.map { DartTarget(it) }

        // 2. UI Bileşenlerini Tanımla
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDart)
        val btnReset = findViewById<ImageButton>(R.id.btnReset)
        val editPlayer1Name = findViewById<EditText>(R.id.editPlayer1Name)
        val editPlayer2Name = findViewById<EditText>(R.id.editPlayer2Name)

        // 3. Adapter Kurulumu ve Kazanma Mantığı
        val adapter = DartAdapter(gameTargets, isReadOnly = false) { playerIndex ->
            // Kazanma anında isimleri al
            val p1Name = editPlayer1Name.text.toString().ifEmpty { "Oyuncu 1" }
            val p2Name = editPlayer2Name.text.toString().ifEmpty { "Oyuncu 2" }
            val winnerName = if (playerIndex == 1) p1Name else p2Name

            // Oyun bitti diyaloğunu göster
            AlertDialog.Builder(this)
                .setTitle("Oyun Tamamlandı! 🎯")
                .setMessage("Tebrikler $winnerName, tüm tahtayı kapattın!")
                .setCancelable(false)
                .setPositiveButton("Kaydet ve Bitir") { _, _ ->

                    // --- VERİTABANI KAYIT SÜRECİ ---

                    // Tahtanın o anki halini virgülle ayrılmış String'e çeviriyoruz
                    val p1Snapshot = gameTargets.joinToString(",") { it.player1Hits.toString() }
                    val p2Snapshot = gameTargets.joinToString(",") { it.player2Hits.toString() }

                    val result = GameResult(
                        player1Name = p1Name,
                        player2Name = p2Name,
                        date = System.currentTimeMillis(),
                        p1Snapshot = p1Snapshot,
                        p2Snapshot = p2Snapshot
                    )

                    // Veritabanı işlemini arka planda (Coroutine) başlat
                    lifecycleScope.launch {
                        val db = AppDatabase.getDatabase(this@MainActivity)
                        db.gameResultDao().insertGame(result)

                        // İşlem bitince ana menüye dön
                        finish()
                    }
                }
                .setNegativeButton("Tahtayı İncele", null)
                .show()
        }

        // 4. RecyclerView Yapılandırması
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 5. Reset Butonu Mantığı
        btnReset.setOnClickListener {
            gameTargets.forEach {
                it.player1Hits = 0
                it.player2Hits = 0
            }
            adapter.notifyDataSetChanged()
        }
    }
}