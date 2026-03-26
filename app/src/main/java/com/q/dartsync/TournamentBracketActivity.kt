package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TournamentBracketActivity : AppCompatActivity() {

    private lateinit var currentMatches: MutableList<TournamentMatch>
    private lateinit var adapter: MatchAdapter
    private var lastPlayedMatchIndex: Int = -1
    private var selectedMode: String = "501" // 🔥 Seçilen modu tutmak için değişken

    // Maç bittiğinde kazananı getiren akıllı launcher
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val winnerName = result.data?.getStringExtra("WINNER_NAME")

            if (winnerName != null && lastPlayedMatchIndex != -1) {
                // 1. Kazananı ilgili maçın içine yaz
                currentMatches[lastPlayedMatchIndex].winner = winnerName

                // 2. Ekranı güncelle
                adapter.notifyDataSetChanged()

                // 3. Round bitti mi kontrol et
                checkRoundCompletion()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_bracket)

        // 1. Intent'ten isimleri VE seçilen modu al
        val names = intent.getStringArrayListExtra("NAMES") ?: arrayListOf()
        selectedMode = intent.getStringExtra("SELECTED_MODE") ?: "501" // 🔥 Kurulumdan gelen seçim

        // İlk tur eşleşmelerini oluştur
        currentMatches = TournamentLogic.createMatches(names).toMutableList()

        val rv = findViewById<RecyclerView>(R.id.rvTournamentBracket)
        rv.layoutManager = LinearLayoutManager(this)

        // 2. 🔥 AKILLI ADAPTER: Seçilen moda göre doğru sayfayı açar
        adapter = MatchAdapter(currentMatches) { match, position ->
            lastPlayedMatchIndex = position

            // Hangi sayfaya gideceğimizi belirliyoruz
            val targetActivity = if (selectedMode == "CRICKET") {
                CricketActivity::class.java
            } else {
                MainActivity::class.java
            }

            val intent = Intent(this, targetActivity).apply {
                putExtra("P1_NAME", match.player1)
                putExtra("P2_NAME", match.player2)
                putExtra("IS_TOURNAMENT", true)
            }
            getResult.launch(intent)
        }
        rv.adapter = adapter
    }

    private fun checkRoundCompletion() {
        val btnNext = findViewById<Button>(R.id.btnNextRound)
        val allFinished = currentMatches.all { it.winner != null }

        if (allFinished) {
            if (currentMatches.size == 1) {
                // Şampiyon belli oldu!
                val winner = currentMatches[0].winner
                Toast.makeText(this, "🏆 ŞAMPİYON: $winner 🏆", Toast.LENGTH_LONG).show()
                btnNext.text = "TURNUVAYI BİTİR"
                btnNext.visibility = View.VISIBLE
                btnNext.setOnClickListener { finish() }
            } else {
                // Tur bitti, sonraki tura geçiş butonu
                btnNext.visibility = View.VISIBLE
                btnNext.setOnClickListener {
                    startNextRound()
                }
            }
        }
    }

    private fun startNextRound() {
        // 1. Mevcut turdaki kazanan isimlerini topla
        val winners = currentMatches.mapNotNull { it.winner }

        // 2. Yeni tur listesini hazırla
        val nextRoundMatches = mutableListOf<TournamentMatch>()

        for (i in 0 until winners.size step 2) {
            if (i + 1 < winners.size) {
                nextRoundMatches.add(TournamentMatch(
                    player1 = winners[i],
                    player2 = winners[i + 1],
                    winner = null,
                    round = 2
                ))
            } else {
                nextRoundMatches.add(TournamentMatch(
                    player1 = winners[i],
                    player2 = "BYE",
                    winner = winners[i],
                    round = 2
                ))
            }
        }

        // 3. Mevcut listeyi boşalt ve yenisini yükle
        currentMatches.clear()
        currentMatches.addAll(nextRoundMatches)

        // 4. Ekranı güncelle
        adapter.notifyDataSetChanged()

        findViewById<Button>(R.id.btnNextRound).visibility = View.GONE
        Toast.makeText(this, "Sonraki Tur Hazır!", Toast.LENGTH_SHORT).show()
    }
}