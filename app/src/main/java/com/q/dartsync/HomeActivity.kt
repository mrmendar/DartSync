package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    // 🔥 Firebase Tanımlamaları
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Firebase Başlatma
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Görünümleri Tanımlıyoruz
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnStats = findViewById<Button>(R.id.btnStats)
        val btnLogout = findViewById<Button>(R.id.btnLogout) // 🚪 XML'e eklediğin buton ID'si
        val tvWelcome = findViewById<TextView>(R.id.tvTitle) // Senin ekranındaki "Hoş geldin" alanı

        // ✨ 2. USER VERİSİNİ ÇEKME
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isAnonymous) {
                // Misafir girişi yapıldıysa
                tvWelcome.text = "Hoş geldin, Misafir! 🎯"
            } else {
                // Kayıtlı kullanıcı ise Firestore'dan nickname çek
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val nickname = document.getString("nickname")
                            tvWelcome.text = "Hoş geldin, $nickname!"
                        } else {
                            tvWelcome.text = "Hoş geldin!"
                        }
                    }
                    .addOnFailureListener {
                        tvWelcome.text = "Hoş geldin!"
                    }
            }
        }

        // 🚪 3. ÇIKIŞ YAP (Oturumu kapatıp giriş ekranına döner)
        btnLogout.setOnClickListener {
            auth.signOut() // Firebase oturumunu kapat
            val intent = Intent(this, AuthActivity::class.java)
            // Geri tuşuna basınca tekrar ana sayfaya gelmesini engellemek için:
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Çıkış yapıldı, görüşürüz imat!", Toast.LENGTH_SHORT).show()
        }

        // 🎯 4. OYUNA BAŞLA (Mod Seçimi)
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

            // Turnuva Modu
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

        // YEREL OYUN
        choiceView.findViewById<Button>(R.id.btnLocalGame).setOnClickListener {
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", false)
            startActivity(intent)
            choiceDialog.dismiss()
        }

        // ONLINE OYUN
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
            val randomCode = (1000..9999).random().toString()
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