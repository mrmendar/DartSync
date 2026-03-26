package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Ana Butonları Tanımlıyoruz
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnStats = findViewById<Button>(R.id.btnStats)

        // 🎯 2. "OYUNA BAŞLA" Butonuna Basınca Açılan Alt Menü (Bottom Sheet)
        btnStartGame.setOnClickListener {
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_game_modes, null)

            // Menü içindeki mod butonlarını bağlıyoruz
            val mode501 = view.findViewById<Button>(R.id.mode501)
            val modeCricket = view.findViewById<Button>(R.id.modeCricket)
            val modeTournament = view.findViewById<Button>(R.id.modeTournament)

            // 501 / 301 Modu
            mode501.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
                bottomSheet.dismiss()
            }

            // Kriket Modu
            modeCricket.setOnClickListener {
                startActivity(Intent(this, CricketActivity::class.java))
                bottomSheet.dismiss()
            }

            // Turnuva Modu
            modeTournament.setOnClickListener {
                startActivity(Intent(this, TournamentSetupActivity::class.java))
                bottomSheet.dismiss()
            }

            bottomSheet.setContentView(view)
            bottomSheet.show()
        }

        // 📚 3. GEÇMİŞ KAYITLAR - Doğrudan yönlendirme
        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // 📊 4. İSTATİSTİKLER - Doğrudan yönlendirme
        btnStats.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }
}