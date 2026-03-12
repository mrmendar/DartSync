package com.q.dartsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history) // Bu layout'u basitçe bir RecyclerView olarak oluşturmalısın

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@HistoryActivity)
            val games = db.gameResultDao().getAllGames()
            rv.adapter = HistoryAdapter(games)
        }
    }
}