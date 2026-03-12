package com.q.dartsync

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Veritabanı işlemleri için Coroutine başlatıyoruz
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@StatisticsActivity)
            val dao = db.gameResultDao()

            // Verileri çek
            val totalMatches = dao.getTotalMatches()
            val mostActive = dao.getMostActivePlayer() ?: "Veri Yok"

            // Arayüze yaz
            findViewById<TextView>(R.id.tvTotalGames).text = totalMatches.toString()
            findViewById<TextView>(R.id.tvTopPlayer).text = mostActive
        }
    }
}