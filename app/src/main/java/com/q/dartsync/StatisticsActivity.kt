package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var dao: GameResultDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        dao = AppDatabase.getDatabase(this).gameResultDao()
        loadData()
    }

    private fun loadData() {
        val tvTotalGames = findViewById<TextView>(R.id.tvTotalGames)
        val tvTopPlayer = findViewById<TextView>(R.id.tvTopPlayer)
        val rvPlayers = findViewById<RecyclerView>(R.id.rvPlayers)

        rvPlayers.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val totalMatches = dao.getTotalMatches()
            val topPlayer = dao.getMostActivePlayer() ?: "Henüz Veri Yok"

            tvTotalGames.text = totalMatches.toString()
            tvTopPlayer.text = topPlayer

            val players = dao.getAllUniquePlayers()

            val adapter = PlayerAdapter(players,
                onItemClick = { clickedName ->
                    val intent = Intent(this@StatisticsActivity, PlayerDetailActivity::class.java)
                    intent.putExtra("PLAYER_NAME", clickedName)
                    startActivity(intent)
                },
                onItemLongClick = { nameToDelete ->
                    showDeleteConfirmDialog(nameToDelete)
                }
            )
            rvPlayers.adapter = adapter
        }
    }

    private fun showDeleteConfirmDialog(playerName: String) {
        AlertDialog.Builder(this)
            .setTitle("Kayıt Silme 🗑️")
            .setMessage("$playerName isimli oyuncunun tüm kayıtlarını silmek istediğine emin misin imat?")
            .setPositiveButton("Evet, Sil") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteByPlayerName(playerName)
                    Toast.makeText(this@StatisticsActivity, "Kayıtlar silindi", Toast.LENGTH_SHORT).show()
                    loadData() // Sayfayı yenile
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}