package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CricketActivity : AppCompatActivity() {

    private lateinit var targetList: List<DartTarget>
    private lateinit var adapter: DartAdapter
    private lateinit var etP1: EditText
    private lateinit var etP2: EditText
    private lateinit var tvStatus: TextView
    private lateinit var btnPassTurn: Button // 🔥 Sıra geçme butonu

    // 📡 Online/Sıra Değişkenleri
    private var isOnline: Boolean = false
    private var roomCode: String = ""
    private var myRole: String = ""
    private var myUid: String = ""
    private var currentTurn: String = ""

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cricket)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        myUid = auth.currentUser?.uid ?: ""

        // 1. Intent Verilerini Al
        isOnline = intent.getBooleanExtra("IS_ONLINE", false)
        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        myRole = intent.getStringExtra("ROLE") ?: "HOST"

        etP1 = findViewById(R.id.etP1Name)
        etP2 = findViewById(R.id.etP2Name)
        tvStatus = findViewById(R.id.tvStatus)
        btnPassTurn = findViewById(R.id.btnPassTurn) // 🔥 XML'deki ID ile eşleşmeli

        // 2. Hedef Listesi
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "Double", "Triple", "Bull", "House")
        targetList = labels.map { DartTarget(it) }

        setupRecyclerView()

        // 3. Sıra Geçme Butonu Dinleyicisi
        btnPassTurn.setOnClickListener {
            if (isOnline) {
                passTurnOnline()
            } else {
                // Yerel modda istersen sadece bir Toast gösterebilirsin
                Toast.makeText(this, "Sıra değişti", Toast.LENGTH_SHORT).show()
            }
        }

        if (isOnline) {
            if (myRole == "HOST") createCricketRoom()
            setupOnlineMode()
        }

        etP1.setText(intent.getStringExtra("P1_NAME") ?: "Oyuncu 1")
        etP2.setText(intent.getStringExtra("P2_NAME") ?: "Oyuncu 2")
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvCricketBoard)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = DartAdapter(
            targetList = targetList,
            isReadOnly = false,
            onWin = { playerIndex ->
                val winnerName = if (playerIndex == 1) etP1.text.toString() else etP2.text.toString()
                showWinDialog(winnerName)
            },
            onHit = { label, playerIndex, newHits ->
                if (isOnline) {
                    if (currentTurn != myUid) {
                        Toast.makeText(this, "Sıra rakipte, lütfen bekleyin.", Toast.LENGTH_SHORT).show()
                        return@DartAdapter
                    }
                    // Sadece vuruşu senkronize et, sırayı butonla geçeceğiz
                    syncHitToFirebase(label, playerIndex, newHits)
                }
            }
        )
        rv.adapter = adapter
    }

    private fun createCricketRoom() {
        val hitsMap = mutableMapOf<String, Map<String, Int>>()
        targetList.forEach { hitsMap[it.label] = mapOf("p1" to 0, "p2" to 0) }

        val newRoom = hashMapOf(
            "roomCode" to roomCode,
            "hostId" to myUid,
            "guestId" to (intent.getStringExtra("OPPONENT_UID") ?: ""),
            "hits" to hitsMap,
            "currentTurn" to myUid,
            "hostNickname" to (intent.getStringExtra("P1_NAME") ?: "Oyuncu 1"),
            "guestNickname" to (intent.getStringExtra("P2_NAME") ?: "Oyuncu 2"),
            "status" to "playing",
            "gameMode" to "Cricket"
        )
        db.collection("rooms").document(roomCode).set(newRoom)
    }

    private fun setupOnlineMode() {
        db.collection("rooms").document(roomCode).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            currentTurn = snapshot.getString("currentTurn") ?: ""

            val remoteHits = snapshot.get("hits") as? Map<String, Any>
            remoteHits?.forEach { (label, players) ->
                val playerMap = players as? Map<String, Any>
                val target = targetList.find { it.label == label }
                target?.apply {
                    player1Hits = (playerMap?.get("p1") as? Long)?.toInt() ?: 0
                    player2Hits = (playerMap?.get("p2") as? Long)?.toInt() ?: 0
                }
            }

            if (!etP1.isFocused) etP1.setText(snapshot.getString("hostNickname") ?: "Oyuncu 1")
            if (!etP2.isFocused) etP2.setText(snapshot.getString("guestNickname") ?: "Oyuncu 2")

            updateTurnUI()
            adapter.notifyDataSetChanged()
        }
    }

    private fun syncHitToFirebase(label: String, playerIndex: Int, newHits: Int) {
        val roomRef = db.collection("rooms").document(roomCode)
        val pKey = if (playerIndex == 1) "p1" else "p2"

        // 🔥 OK SINIRI KALDIRILDI: Sadece vuruş verisini güncelle
        roomRef.update("hits.$label.$pKey", newHits)
    }

    private fun passTurnOnline() {
        // Sıra sende değilse butona basılsa da işlem yapma
        if (currentTurn != myUid) return

        val roomRef = db.collection("rooms").document(roomCode)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val hostId = snapshot.getString("hostId") ?: ""
            val guestId = snapshot.getString("guestId") ?: ""

            // Sırayı karşı tarafa devret
            val nextTurn = if (myUid == hostId) guestId else hostId
            transaction.update(roomRef, "currentTurn", nextTurn)
            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Sıra rakibe geçti!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTurnUI() {
        val isMyTurn = currentTurn == myUid

        // Sıra sende değilse hem tahtayı kilitle hem butonu gizle
        adapter.isReadOnly = !isMyTurn
        btnPassTurn.visibility = if (isMyTurn) View.VISIBLE else View.GONE

        if (isMyTurn) {
            tvStatus.text = "SENİN SIRAN 🎯"
            tvStatus.setTextColor(Color.GREEN)
        } else {
            tvStatus.text = "RAKİP ATIŞI..."
            tvStatus.setTextColor(Color.RED)
        }
    }

    private fun showWinDialog(winnerName: String) {
        AlertDialog.Builder(this)
            .setTitle("OYUN BİTTİ! 🏆")
            .setMessage("Kazanan: $winnerName")
            .setPositiveButton("Kaydet ve Bitir") { _, _ -> saveGame(winnerName) }
            .setCancelable(false)
            .show()
    }

    private fun saveGame(winnerName: String) {
        lifecycleScope.launch {
            val p1Snapshot = targetList.joinToString(",") { it.player1Hits.toString() }
            val p2Snapshot = targetList.joinToString(",") { it.player2Hits.toString() }

            val result = GameResult(
                userId = IdManager.getGuestId(this@CricketActivity),
                player1Name = etP1.text.toString(),
                player2Name = etP2.text.toString(),
                winnerName = winnerName,
                p1Snapshot = p1Snapshot,
                p2Snapshot = p2Snapshot,
                date = System.currentTimeMillis(),
                totalDarts = 0
            )

            AppDatabase.getDatabase(this@CricketActivity).gameResultDao().insertGame(result)
            if (isOnline && myRole == "HOST") db.collection("rooms").document(roomCode).delete()
            finish()
        }
    }
}