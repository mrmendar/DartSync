package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TournamentSetupActivity : AppCompatActivity() {

    private lateinit var adapter: ParticipantInputAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_setup)

        val etCount = findViewById<EditText>(R.id.etParticipantCount)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateList)
        val btnStart = findViewById<Button>(R.id.btnStartTournament)
        val rvInputs = findViewById<RecyclerView>(R.id.rvParticipantInputs)

        // 1. Başlangıç Kurulumu (Varsayılan 4 Kişi)
        adapter = ParticipantInputAdapter(4)
        rvInputs.layoutManager = LinearLayoutManager(this)
        rvInputs.adapter = adapter

        // 2. Sayı Güncelleme Butonu
        btnUpdate.setOnClickListener {
            val count = etCount.text.toString().toIntOrNull() ?: 0
            if (count in 2..32) {
                adapter.updateCount(count)
            } else {
                Toast.makeText(this, "Lütfen 2-32 arası bir sayı gir imat!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Turnuvayı Başlatma ve Kura Çekme Butonu
        btnStart.setOnClickListener {
            val names = adapter.getEnteredNames()
            val countString = etCount.text.toString()

            // İsimler eksik mi kontrol et
            if (countString.isEmpty() || names.size < (countString.toIntOrNull() ?: 0)) {
                Toast.makeText(this, "Lütfen tüm isimleri doldur imat!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 EKRAN GEÇİŞİ VE VERİ TAŞIMA
            // Algoritma TournamentBracketActivity içinde de çalıştırılabilir
            // ama biz isim listesini temiz bir şekilde gönderiyoruz.
            val intent = Intent(this, TournamentBracketActivity::class.java).apply {
                putStringArrayListExtra("NAMES", ArrayList(names))
            }

            startActivity(intent)

            // Bilgilendirme mesajı
            Toast.makeText(this, "${names.size} kişilik turnuva başlıyor!", Toast.LENGTH_SHORT).show()
        }
    }
}