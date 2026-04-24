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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class TournamentSetupActivity : AppCompatActivity() {

    private lateinit var adapter: ParticipantInputAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_setup)

        db = FirebaseFirestore.getInstance()

        val etCount = findViewById<EditText>(R.id.etParticipantCount)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateList)
        val btnStart = findViewById<Button>(R.id.btnStartTournament)
        val rvInputs = findViewById<RecyclerView>(R.id.rvParticipantInputs)
        val rbCricket = findViewById<RadioButton>(R.id.rbCricket)

        adapter = ParticipantInputAdapter(4)
        rvInputs.layoutManager = LinearLayoutManager(this)
        rvInputs.adapter = adapter

        btnUpdate.setOnClickListener {
            val count = etCount.text.toString().toIntOrNull() ?: 0
            if (count in 2..32) {
                adapter.updateCount(count)
            } else {
                Toast.makeText(this, "Lütfen 2-32 arası bir sayı gir imat!", Toast.LENGTH_SHORT).show()
            }
        }

        btnStart.setOnClickListener {
            val names = adapter.getEnteredNames()
            val countString = etCount.text.toString()
            val expectedCount = countString.toIntOrNull() ?: 0

            if (countString.isEmpty() || names.size < expectedCount) {
                Toast.makeText(this, "Lütfen tüm isimleri doldur imat!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showShuffleAnimation(names) {
                val selectedMode = if (rbCricket.isChecked) "CRICKET" else "501"
                // 🎯 FIRESTORE KAYIT OPERASYONU BAŞLIYOR
                saveAndStartTournament(names.shuffled(), selectedMode)
            }
        }
    }

    private fun saveAndStartTournament(shuffledNames: List<String>, mode: String) {
        val tournamentId = UUID.randomUUID().toString()

        // 1. Maçları Double Elimination (Çift Eleme) mantığıyla oluştur
        val matches = TournamentLogic.buildTournament(shuffledNames, TournamentType.DOUBLE_ELIMINATION, tournamentId)

        // 2. Turnuva ana oturumunu oluştur
        val session = TournamentSession(
            tournamentId = tournamentId,
            name = "Akşam Turnuvası - $mode",
            type = TournamentType.DOUBLE_ELIMINATION,
            participants = shuffledNames
        )

        val batch = db.batch()
        val tournamentRef = db.collection("tournaments").document(tournamentId)

        // Ana dökümanı ekle
        batch.set(tournamentRef, session)

        // Tüm maçları alt koleksiyona ekle
        matches.forEach { match ->
            val matchRef = tournamentRef.collection("matches").document(match.matchId)
            batch.set(matchRef, match)
        }

        // 3. Buluta gönder
        batch.commit().addOnSuccessListener {
            val intent = Intent(this, TournamentBracketActivity::class.java).apply {
                putExtra("TOURNAMENT_ID", tournamentId)
                putExtra("SELECTED_MODE", mode)
            }
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showShuffleAnimation(names: List<String>, onComplete: () -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_shuffle_animation, null)
        val tvName = view.findViewById<TextView>(R.id.tvShufflingName)

        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.show()

        lifecycleScope.launch {
            val duration = 2500L
            val startTime = System.currentTimeMillis()
            var delayTime = 60L

            while (System.currentTimeMillis() - startTime < duration) {
                tvName.text = names.random()
                tvName.scaleX = 1.1f
                tvName.scaleY = 1.1f
                delay(delayTime)
                tvName.animate().scaleX(1.0f).scaleY(1.0f).setDuration(delayTime).start()
                if (System.currentTimeMillis() - startTime > duration * 0.7) {
                    delayTime += 25L
                }
            }

            tvName.text = "Kuralar Hazır! ✅"
            tvName.setTextColor(Color.parseColor("#00D26A"))
            delay(1000)
            dialog.dismiss()
            onComplete()
        }
    }
}