package com.q.dartsync

import android.content.Intent
import android.os.Bundle
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

    // 🔥 Maç bittiğinde kazananı getiren akıllı launcher
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val winnerName = result.data?.getStringExtra("WINNER_NAME")

            if (winnerName != null && lastPlayedMatchIndex != -1) {
                // 1. Kazananı ilgili maçın içine yaz
                currentMatches[lastPlayedMatchIndex].winner = winnerName

                // 2. Ekranı güncelle
                adapter.notifyDataSetChanged()

                // 3. Round bitti mi kontrol et (Eğer tüm maçlar bittiyse bir sonraki tura geçebiliriz)
                checkRoundCompletion()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_bracket)

        // Intent'ten isimleri al
        val names = intent.getStringArrayListExtra("NAMES") ?: arrayListOf()

        // İlk tur eşleşmelerini oluştur (MutableList yapıyoruz ki değiştirebilelim)
        currentMatches = TournamentLogic.createMatches(names).toMutableList()

        val rv = findViewById<RecyclerView>(R.id.rvTournamentBracket)
        rv.layoutManager = LinearLayoutManager(this)

        // 🔥 Artık adapter hem maçı hem de pozisyonunu döndürüyor
        adapter = MatchAdapter(currentMatches) { match, position ->
            lastPlayedMatchIndex = position // Hangi maça tıklandığını kaydet

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("P1_NAME", match.player1)
                putExtra("P2_NAME", match.player2)
                putExtra("IS_TOURNAMENT", true)
            }
            getResult.launch(intent)
        }
        rv.adapter = adapter
    }

    // TournamentBracketActivity.kt içinde checkRoundCompletion fonksiyonunu güncelle ve butonun click listener'ını ekle

    private fun checkRoundCompletion() {
        val btnNext = findViewById<Button>(R.id.btnNextRound)
        val allFinished = currentMatches.all { it.winner != null }

        if (allFinished) {
            if (currentMatches.size == 1) {
                // Şampiyon belli oldu!
                val winner = currentMatches[0].winner
                Toast.makeText(this, "🏆 ŞAMPİYON: $winner 🏆", Toast.LENGTH_LONG).show()
                btnNext.text = "TURNUVAYI BİTİR"
                btnNext.visibility = android.view.View.VISIBLE
                btnNext.setOnClickListener { finish() }
            } else {
                // Tur bitti, sonraki tura geçiş butonu
                btnNext.visibility = android.view.View.VISIBLE
                btnNext.setOnClickListener {
                    startNextRound()
                }
            }
        }
    }

    private fun startNextRound() {
        // 1. Mevcut turdaki kazananları topla
        val winners = currentMatches.mapNotNull { it.winner }

        // 2. Yeni tur eşleşmelerini oluştur (Kura çekmeden, kazanan sırasına göre)
        // Not: Burada tekrar karıştırmak istemeyebilirsin, o yüzden createMatches yerine
        // daha basit bir eşleşme mantığı kullanacağız.

        val nextRoundMatches = mutableListOf<TournamentMatch>()
        for (i in 0 until winners.size step 2) {
            if (i + 1 < winners.size) {
                nextRoundMatches.add(TournamentMatch(winners[i], winners[i+1]))
            } else {
                // Tek kalan olursa yine BYE
                nextRoundMatches.add(TournamentMatch(winners[i], "BYE", winner = winners[i]))
            }
        }

        // 3. Listeyi ve ekranı güncelle
        currentMatches.clear()
        currentMatches.addAll(nextRoundMatches)
        adapter.notifyDataSetChanged()

        // 4. Butonu tekrar gizle
        findViewById<Button>(R.id.btnNextRound).visibility = android.view.View.GONE

        Toast.makeText(this, "Yeni Tur Hazır! Başarılar.", Toast.LENGTH_SHORT).show()
    }
}