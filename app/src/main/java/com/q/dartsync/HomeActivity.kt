package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        // 🎯 2. OYUNA BAŞLA (Önce Mod Seçimi)
        btnStartGame.setOnClickListener {
            val modeDialog = BottomSheetDialog(this)
            val modeView = layoutInflater.inflate(R.layout.dialog_game_modes, null)

            // 501 / 301 Modu
            modeView.findViewById<Button>(R.id.mode501).setOnClickListener {
                modeDialog.dismiss()
                showConnectionChoice(MainActivity::class.java)
            }

            // Kriket Modu
            modeView.findViewById<Button>(R.id.modeCricket).setOnClickListener {
                modeDialog.dismiss()
                showConnectionChoice(CricketActivity::class.java)
            }

            // Turnuva Modu (Turnuvalar şimdilik yerel devam ediyor)
            modeView.findViewById<Button>(R.id.modeTournament).setOnClickListener {
                modeDialog.dismiss()
                val intent = Intent(this, TournamentSetupActivity::class.java)
                startActivity(intent)
            }

            modeDialog.setContentView(modeView)
            modeDialog.show()
        }

        // 📚 GEÇMİŞ KAYITLAR
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // 📊 İSTATİSTİKLER
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }

    // 🌐 BAĞLANTI TÜRÜ SEÇİMİ (Yerel mi Online mı?)
    private fun showConnectionChoice(targetActivity: Class<*>) {
        val choiceDialog = BottomSheetDialog(this)
        val choiceView = layoutInflater.inflate(R.layout.dialog_connection_choice, null)

        // YEREL OYUN: Tek cihaz, internete gitme
        choiceView.findViewById<Button>(R.id.btnLocalGame).setOnClickListener {
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", false)
            startActivity(intent)
            choiceDialog.dismiss()
        }

        // ONLINE OYUN: Firebase oda sistemine yönlendir
        choiceView.findViewById<Button>(R.id.btnOnlineGame).setOnClickListener {
            choiceDialog.dismiss()
            showRoomDialog(targetActivity)
        }

        choiceDialog.setContentView(choiceView)
        choiceDialog.show()
    }

    // 🏠 ODA YÖNETİMİ (Kur veya Katıl)
    private fun showRoomDialog(targetActivity: Class<*>) {
        val roomDialog = BottomSheetDialog(this)
        val roomView = layoutInflater.inflate(R.layout.dialog_join_room, null)

        // Odaya Katıl
        roomView.findViewById<Button>(R.id.btnJoinRoom).setOnClickListener {
            val code = roomView.findViewById<EditText>(R.id.etRoomCode).text.toString()
            if (code.isNotEmpty()) {
                val intent = Intent(this, targetActivity)
                intent.putExtra("IS_ONLINE", true)
                intent.putExtra("ROOM_CODE", code)
                startActivity(intent)
                roomDialog.dismiss()
            } else {
                Toast.makeText(this, "Lütfen bir oda kodu gir imat!", Toast.LENGTH_SHORT).show()
            }
        }

        // Yeni Oda Kur
        roomView.findViewById<Button>(R.id.btnCreateRoom).setOnClickListener {
            val randomCode = (1000..9999).random().toString() // Rastgele 4 haneli oda kodu
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", true)
            intent.putExtra("ROOM_CODE", randomCode)
            startActivity(intent)
            roomDialog.dismiss()
        }

        roomDialog.setContentView(roomView)
        roomDialog.show()
    }
}