package com.q.dartsync

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    // 📡 Online/Sıra Değişkenleri
    private var isOnline: Boolean = false
    private var roomCode: String = ""
    private var myRole: String = ""
    private var myUid: String = ""
    private var currentTurn: String = ""
    private var dartsThrown = 0

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

        // 2. Hedef Listesi (20-10 ve Özel Alanlar)
        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "Double", "Triple", "Bull", "House")
        targetList = labels.map { DartTarget(it) }

        setupRecyclerView()

        if (isOnline) {
            if (myRole == "HOST") createCricketRoom() // Host ise odayı oluştur
            setupOnlineMode()
        }

        // İsimleri yerleştir
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
                    // 🔥 SIRA KONTROLÜ: Sıra sende değilse işlem yapma
                    if (currentTurn != myUid) {
                        Toast.makeText(this, "Sıra rakipte, lütfen bekleyin.", Toast.LENGTH_SHORT).show()
                        return@DartAdapter
                    }

                    dartsThrown++
                    syncHitToFirebase(label, playerIndex, newHits)
                }
            }
        )
        rv.adapter = adapter
    }

    // 🔥 Odayı Firestore'da ilk kez oluşturma (Host için)
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

            // 🔥 SENKRONİZASYON: Firebase'deki hits haritasını listeye işle
            val remoteHits = snapshot.get("hits") as? Map<String, Any>
            remoteHits?.forEach { (label, players) ->
                val playerMap = players as? Map<String, Any>
                val target = targetList.find { it.label == label }
                target?.apply {
                    player1Hits = (playerMap?.get("p1") as? Long)?.toInt() ?: 0
                    player2Hits = (playerMap?.get("p2") as? Long)?.toInt() ?: 0
                }
            }

            // İsim Senkronu
            if (!etP1.isFocused) etP1.setText(snapshot.getString("hostNickname") ?: "Oyuncu 1")
            if (!etP2.isFocused) etP2.setText(snapshot.getString("guestNickname") ?: "Oyuncu 2")

            updateTurnUI()
            adapter.notifyDataSetChanged() // Görseli anında yenile
        }
    }

    private fun syncHitToFirebase(label: String, playerIndex: Int, newHits: Int) {
        val roomRef = db.collection("rooms").document(roomCode)
        val pKey = if (playerIndex == 1) "p1" else "p2"

        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val hostId = snapshot.getString("hostId") ?: ""
            val guestId = snapshot.getString("guestId") ?: ""

            // "hits.20.p1" gibi nested path kullanarak veriyi güncelle
            transaction.update(roomRef, "hits.$label.$pKey", newHits)

            // 3 ok bittiyse sırayı karşıya ver
            if (dartsThrown >= 3) {
                val nextTurn = if (myUid == hostId) guestId else hostId
                transaction.update(roomRef, "currentTurn", nextTurn)
                dartsThrown = 0
            }
            null
        }
    }

    private fun updateTurnUI() {
        val isMyTurn = currentTurn == myUid
        tvStatus.text = if (isMyTurn) "SENİN SIRAN 🎯" else "RAKİP ATIŞI..."
        tvStatus.setTextColor(if (isMyTurn) Color.GREEN else Color.RED)

        // 🔥 FİZİKSEL KİLİT: Sıra sende değilse işaretleme yapılamaz
        adapter.isReadOnly = !isMyTurn
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

            // Sadece Host odayı silebilir
            if (isOnline && myRole == "HOST") {
                db.collection("rooms").document(roomCode).delete()
            }

            finish()
        }
    }
}