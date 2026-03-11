package com.q.dartsync

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Veri Kaynağını Hazırla
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "D", "T", "B", "H")
        val gameTargets = labels.map { DartTarget(it) }

        // 2. UI Bileşenlerini Tanımla
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDart)
        val btnReset = findViewById<ImageButton>(R.id.btnReset)
        val editPlayer1Name = findViewById<EditText>(R.id.editPlayer1Name)
        val editPlayer2Name = findViewById<EditText>(R.id.editPlayer2Name)

        // 3. Adapter Kurulumu (Kazanma mantığıyla birlikte tek seferde)
        val adapter = DartAdapter(gameTargets) { playerIndex ->
            // Birisi kazandığında çalışacak blok
            val winnerName = if (playerIndex == 1) {
                editPlayer1Name.text.toString().ifEmpty { "Oyuncu 1" }
            } else {
                editPlayer2Name.text.toString().ifEmpty { "Oyuncu 2" }
            }

            // Oyun bitti uyarısı
            AlertDialog.Builder(this)
                .setTitle("Oyun Tamamlandı! 🎯")
                .setMessage("Tebrikler $winnerName, tüm tahtayı kapattın!")
                .setCancelable(false) // Yanlışlıkla ekrana basıp kapatılmasın diye
                .setPositiveButton("Kaydet ve Bitir") { _, _ ->
                    // Faz 2'de buraya veritabanı kayıt kodlarını ekleyeceğiz imat
                    finish()
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