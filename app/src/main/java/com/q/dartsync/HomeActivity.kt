package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnNewGame = findViewById<Button>(R.id.btnNewGame)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        // Yeni Oyun'a basınca MainActivity'ye (mevcut oyun ekranına) gider
        btnNewGame.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Kayıtlar'a basınca şimdilik sadece bir mesaj verelim (HistoryActivity'yi sonra yapacağız)
        btnHistory.setOnClickListener {
            Toast.makeText(this, "Kayıtlar yakında burada olacak!", Toast.LENGTH_SHORT).show()
        }
    }
}