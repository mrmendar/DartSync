package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class TournamentConfigActivity : AppCompatActivity() {

    private lateinit var adapter: ParticipantInputAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_config)

        // --- UI Bileşenleri ---
        val rgType = findViewById<RadioGroup>(R.id.rgTournamentType)
        val layoutGroupSettings = findViewById<LinearLayout>(R.id.layoutGroupSettings)
        val etGroupCount = findViewById<EditText>(R.id.etGroupCount)
        val etQualifiers = findViewById<EditText>(R.id.etQualifiersPerGroup)
        val rgLegs = findViewById<RadioGroup>(R.id.rgLegs)
        val rvParticipants = findViewById<RecyclerView>(R.id.rvParticipants)
        val btnGenerate = findViewById<Button>(R.id.btnGenerateTournament)
        val etParticipantCount = findViewById<EditText>(R.id.etParticipantCount)
        val btnUpdateList = findViewById<Button>(R.id.btnUpdateList)

        // 1. Dinamik UI: Grup Aşaması seçilirse ayarlar belirsin
        rgType.setOnCheckedChangeListener { _, checkedId ->
            layoutGroupSettings.visibility = if (checkedId == R.id.rbGroup) View.VISIBLE else View.GONE
        }

        // 2. Katılımcı Listesi Kurulumu (Başlangıçta 8 kişi)
        adapter = ParticipantInputAdapter(8)
        rvParticipants.layoutManager = LinearLayoutManager(this)
        rvParticipants.adapter = adapter

        // Liste sayısını güncelleme
        btnUpdateList.setOnClickListener {
            val count = etParticipantCount.text.toString().toIntOrNull() ?: 8
            if (count in 2..128) {
                adapter.updateCount(count)
            } else {
                Toast.makeText(this, "Lütfen 2 ile 128 arası bir sayı gir!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. TURNUVAYI OLUŞTUR VE KAYDET
        btnGenerate.setOnClickListener {
            val participants = adapter.getEnteredNames()

            // Temel Kontrol
            if (participants.size < 2) {
                Toast.makeText(this, "En az 2 katılımcı gerekli kral!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tournamentId = UUID.randomUUID().toString()

            // Sistem Seçimi (Double Elimination veya Group Stage)
            val selectedType = if (rgType.checkedRadioButtonId == R.id.rbDouble)
                TournamentType.DOUBLE_ELIMINATION else TournamentType.GROUP_STAGE

            // 🔥 MAÇ FORMATI (Beraberlik Sistemi Dahil)
            val legs = when(rgLegs.checkedRadioButtonId) {
                R.id.rbBo1 -> 1
                R.id.rbBo2 -> 2 // 🔥 Senin istediğin "Beraberlik / 2 Legs" sistemi
                R.id.rbBo5 -> 5
                else -> 3       // Varsayılan BO3
            }

            // Grup Ayarları (Sadece Grup Aşaması seçiliyse anlamlı)
            val groupCount = etGroupCount.text.toString().toIntOrNull() ?: 0
            val qualifiers = etQualifiers.text.toString().toIntOrNull() ?: 0

            // Validasyon: 5 grup varken 30 kişi girmek lazım vb.
            if (selectedType == TournamentType.GROUP_STAGE && groupCount > 0) {
                if (participants.size < groupCount) {
                    Toast.makeText(this, "Katılımcı sayısı grup sayısından az olamaz!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // --- 🏗️ SESSION PAKETLEME ---
            val session = TournamentSession(
                tournamentId = tournamentId,
                name = if (selectedType == TournamentType.GROUP_STAGE) "Grup Turnuvası" else "Çift Eleme Turnuvası",
                type = selectedType,
                participants = participants,
                status = "active",
                legs = legs, // BO1, BO2, BO3, BO5
                groupCount = groupCount,
                qualifiersPerGroup = qualifiers,
                pointsForWin = 3, // Standart dart ligi puanlaması
                pointsForDraw = 1, // Beraberlikte 1 puan
                pointsForLoss = 0
            )

            // Firestore'a kaydet ve ağaç/puan ekranına git
            saveTournament(session)
        }
    }

    private fun saveTournament(session: TournamentSession) {
        val batch = db.batch()
        val tournamentRef = db.collection("tournaments").document(session.tournamentId)

        batch.set(tournamentRef, session)

        // NOT: Maçların kura çekimi ve oluşturulması (TournamentLogic)
        // BracketActivity açıldığında oradaki ilk dinleme anında tetiklenecek.

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Turnuva Kuralları Kaydedildi! 🎯", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, TournamentBracketActivity::class.java).apply {
                putExtra("TOURNAMENT_ID", session.tournamentId)
                putExtra("SYSTEM_TYPE", session.type.name) // Grup mu eleme mi bilelim
            }
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}