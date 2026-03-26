package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class CricketActivity : AppCompatActivity() {

    private lateinit var targetList: List<DartTarget>
    private lateinit var adapter: DartAdapter
    private lateinit var etP1: EditText
    private lateinit var etP2: EditText

    // 🌐 Online Mod Değişkenleri
    private var isOnline: Boolean = false
    private var roomCode: String = ""
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cricket)

        // 1. Intent Verilerini Al
        isOnline = intent.getBooleanExtra("IS_ONLINE", false)
        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""

        etP1 = findViewById(R.id.etP1Name)
        etP2 = findViewById(R.id.etP2Name)

        // 2. Firebase Kurulumu (Eğer Online ise)
        if (isOnline) {
            // Toast ile hangi odada olduğunu gösterelim
            Toast.makeText(this, "Oda: $roomCode - Online Bağlantı Hazır!", Toast.LENGTH_LONG).show()
            dbRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomCode)
            setupFirebaseSync()
        }

        // 3. İsimleri Yerleştir
        val p1FromIntent = intent.getStringExtra("P1_NAME") ?: "Oyuncu 1"
        val p2FromIntent = intent.getStringExtra("P2_NAME") ?: "Oyuncu 2"
        etP1.setText(p1FromIntent)
        etP2.setText(p2FromIntent)

        // 🔥 Online modda isimleri de senkronize et (Bir taraf değiştirince diğerinde değişsin)
        setupNameSync()

        // 4. Hedef Listesi (20 -> 10 ve D, T, B, H)
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "Double", "Triple", "Bull", "House")
        targetList = labels.map { DartTarget(it) }

        // 5. RecyclerView Ayarları
        val rv = findViewById<RecyclerView>(R.id.rvCricketBoard)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = DartAdapter(
            targetList = targetList,
            isReadOnly = false,
            onWin = { playerIndex ->
                val winnerName = if (playerIndex == 1) etP1.text.toString() else etP2.text.toString()
                showWinDialog(winnerName)
            },
            // 🔥 ADAPTER'A DOKUNUŞ: Tıklama olduğunda Firebase'e gönder (Eğer Online ise)
            onHit = { label, playerIndex, newHits ->
                if (isOnline) {
                    val pKey = if (playerIndex == 1) "p1Hits" else "p2Hits"
                    dbRef.child("scores").child(label).child(pKey).setValue(newHits)
                }
            }
        )
        rv.adapter = adapter
    }

    private fun setupFirebaseSync() {
        // Firebase'deki skor değişikliklerini dinle
        dbRef.child("scores").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                for (target in targetList) {
                    val targetSnap = snapshot.child(target.label)
                    target.player1Hits = targetSnap.child("p1Hits").getValue(Int::class.java) ?: 0
                    target.player2Hits = targetSnap.child("p2Hits").getValue(Int::class.java) ?: 0
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupNameSync() {
        if (!isOnline) return

        // 1. Firebase'den isimleri oku
        dbRef.child("names").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val n1 = snapshot.child("p1Name").getValue(String::class.java)
                val n2 = snapshot.child("p2Name").getValue(String::class.java)

                // Eğer odak EditText'te değilse (kullanıcı yazmıyorsa) ismi güncelle
                if (!etP1.isFocused && n1 != null) etP1.setText(n1)
                if (!etP2.isFocused && n2 != null) etP2.setText(n2)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. İsim değiştiğinde Firebase'e yaz
        etP1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (etP1.isFocused) dbRef.child("names").child("p1Name").setValue(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etP2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (etP2.isFocused) dbRef.child("names").child("p2Name").setValue(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showWinDialog(winnerName: String) {
        AlertDialog.Builder(this)
            .setTitle("TEBRİKLER! 🏆")
            .setMessage("Kazanan: $winnerName\nMaç kaydedilsin mi?")
            .setPositiveButton("Kaydet ve Bitir") { _, _ ->
                if (isOnline) dbRef.removeValue() // Oyun bittiyse odayı temizle (opsiyonel)
                saveGame(winnerName)
            }
            .setNegativeButton("Kapat", null)
            .setCancelable(false)
            .show()
    }

    private fun saveGame(winnerName: String) {
        lifecycleScope.launch {
            val currentP1 = etP1.text.toString().ifEmpty { "Oyuncu 1" }
            val currentP2 = etP2.text.toString().ifEmpty { "Oyuncu 2" }

            val p1Snapshot = targetList.joinToString(",") { it.player1Hits.toString() }
            val p2Snapshot = targetList.joinToString(",") { it.player2Hits.toString() }

            val result = GameResult(
                player1Name = currentP1,
                player2Name = currentP2,
                winnerName = winnerName,
                p1Snapshot = p1Snapshot,
                p2Snapshot = p2Snapshot,
                date = System.currentTimeMillis(),
                totalDarts = 0
            )

            AppDatabase.getDatabase(this@CricketActivity).gameResultDao().insertGame(result)

            val resultIntent = Intent().apply { putExtra("WINNER_NAME", winnerName) }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}