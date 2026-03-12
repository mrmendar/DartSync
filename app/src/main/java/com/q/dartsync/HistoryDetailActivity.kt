package com.q.dartsync

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail)

        // 1. Intent üzerinden kaydedilen verileri çekiyoruz
        val p1Name = intent.getStringExtra("p1Name") ?: "Oyuncu 1"
        val p2Name = intent.getStringExtra("p2Name") ?: "Oyuncu 2"
        val p1Snapshot = intent.getStringExtra("p1Snapshot") ?: ""
        val p2Snapshot = intent.getStringExtra("p2Snapshot") ?: ""

        // 2. İsimleri arayüzdeki TextView'lara yazıyoruz
        findViewById<TextView>(R.id.tvPlayer1Name).text = p1Name
        findViewById<TextView>(R.id.tvPlayer2Name).text = p2Name

        // 3. Zaman Makinesi Mantığı: String'den Liste'ye geri dönüş
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "D", "T", "B", "H")

        // Virgülle ayrılmış metni parçalayıp sayıya çeviriyoruz
        val p1HitsList = p1Snapshot.split(",").map { it.toIntOrNull() ?: 0 }
        val p2HitsList = p2Snapshot.split(",").map { it.toIntOrNull() ?: 0 }

        // DartTarget objelerini eski vuruş sayılarıyla dolduruyoruz
        val gameTargets = labels.mapIndexed { index, label ->
            DartTarget(label).apply {
                player1Hits = p1HitsList.getOrElse(index) { 0 }
                player2Hits = p2HitsList.getOrElse(index) { 0 }
            }
        }

        // 4. RecyclerView Yapılandırması
        val rv = findViewById<RecyclerView>(R.id.recyclerViewDart)
        rv.layoutManager = LinearLayoutManager(this)

        // KRİTİK NOKTA: isReadOnly = true diyerek tıklamaları kilitliyoruz.
        // Artık bu ekranda hiçbir şeye tıklandığında skor değişmez.
        rv.adapter = DartAdapter(
            targetList = gameTargets,
            isReadOnly = true,
            onWin = { /* Geçmiş ekranda kazanma kontrolüne gerek yok */ }
        )
    }
}