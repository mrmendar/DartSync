package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TournamentSetupActivity : AppCompatActivity() {

    private lateinit var adapter: ParticipantInputAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_setup)

        val etCount = findViewById<EditText>(R.id.etParticipantCount)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateList)
        val btnStart = findViewById<Button>(R.id.btnStartTournament)
        val rvInputs = findViewById<RecyclerView>(R.id.rvParticipantInputs)
        val rbCricket = findViewById<RadioButton>(R.id.rbCricket)

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

        // 3. Turnuvayı Başlatma Butonu (Animasyonlu)
        btnStart.setOnClickListener {
            val names = adapter.getEnteredNames()
            val countString = etCount.text.toString()
            val expectedCount = countString.toIntOrNull() ?: 0

            // İsimler eksik mi kontrol et
            if (countString.isEmpty() || names.size < expectedCount) {
                Toast.makeText(this, "Lütfen tüm isimleri doldur imat!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🎯 KURA ANİMASYONUNU BAŞLAT
            showShuffleAnimation(names) {
                // Bu kısım animasyon bittiğinde (2.5 sn sonra) çalışacak:

                // İsimleri karıştır (Gerçek kura çekimi)
                val shuffledNames = names.shuffled()

                // Seçilen oyun modunu tespit et
                val selectedMode = if (rbCricket.isChecked) "CRICKET" else "501"

                // EKRAN GEÇİŞİ
                val intent = Intent(this, TournamentBracketActivity::class.java).apply {
                    putStringArrayListExtra("NAMES", ArrayList(shuffledNames))
                    putExtra("SELECTED_MODE", selectedMode)
                }
                startActivity(intent)

                val modeText = if (selectedMode == "CRICKET") "Kriket" else "501"
                Toast.makeText(this, "$modeText modunda kuralar çekildi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🎰 KURA ANİMASYONU MOTORU
    private fun showShuffleAnimation(names: List<String>, onComplete: () -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_shuffle_animation, null)
        val tvName = view.findViewById<TextView>(R.id.tvShufflingName)

        dialog.setContentView(view)
        dialog.setCancelable(false) // Animasyon sırasında kapatılmasın
        dialog.show()

        lifecycleScope.launch {
            val duration = 2500L // Animasyon süresi
            val startTime = System.currentTimeMillis()
            var delayTime = 60L // Başlangıç hızı

            while (System.currentTimeMillis() - startTime < duration) {
                // Listeden rastgele bir isim göster
                tvName.text = names.random()

                // Hafif büyüme efekti
                tvName.scaleX = 1.1f
                tvName.scaleY = 1.1f

                delay(delayTime)

                tvName.animate().scaleX(1.0f).scaleY(1.0f).setDuration(delayTime).start()

                // Sona yaklaştıkça yavaşla (Dramatik etki)
                if (System.currentTimeMillis() - startTime > duration * 0.7) {
                    delayTime += 25L
                }
            }

            // Sonuç aşaması
            tvName.text = "Kuralar Hazır! ✅"
            tvName.setTextColor(Color.parseColor("#00D26A")) // Yeşil onay
            delay(1000)
            dialog.dismiss()
            onComplete() // Turnuva ağacına geçişi tetikle
        }
    }
}