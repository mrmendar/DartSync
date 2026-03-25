package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val btnTournament = findViewById<Button>(R.id.btnTournamentMode)

        btnTournament.setOnClickListener {
            // Turnuva kurulum ekranına geçiş yapıyoruz
            val intent = Intent(this, TournamentSetupActivity::class.java)
            startActivity(intent)
        }

        // Butonları Tanımlıyoruz
        val btnNewGame = findViewById<Button>(R.id.btnNewGame)
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnStatistics = findViewById<Button>(R.id.btnStatistics)

        // 🎯 Yeni Oyun Ekranına Git
        btnNewGame.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 📚 Geçmiş Kayıtlar Ekranına Git
        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // 📊 İstatistik Ekranına Git (Senin eklediğin kısım)
        btnStatistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }
}