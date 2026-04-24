package com.q.dartsync

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private lateinit var btnPassTurn: Button

    // 🔥 TURNUVA VE LEG SİSTEMİ DEĞİŞKENLERİ
    private var p1Legs = 0
    private var p2Legs = 0
    private var targetLegs = 3 // Varsayılan BO3
    private var isTournament = false
    private var matchId: String? = null

    private var vibrator: Vibrator? = null
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

        initVibrator()

        // 🔥 Turnuva Verilerini Yakala
        isTournament = intent.getBooleanExtra("IS_TOURNAMENT", false)
        matchId = intent.getStringExtra("MATCH_ID")
        targetLegs = intent.getIntExtra("TARGET_LEGS", 3) // Config'den gelen BO1, BO2, BO3 bilgisi

        isOnline = intent.getBooleanExtra("IS_ONLINE", false)
        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        myRole = intent.getStringExtra("ROLE") ?: "HOST"

        etP1 = findViewById(R.id.etP1Name)
        etP2 = findViewById(R.id.etP2Name)
        tvStatus = findViewById(R.id.tvStatus)
        btnPassTurn = findViewById(R.id.btnPassTurn)

        val labels = listOf("20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "Double", "Triple", "Bull", "House")
        targetList = labels.map { DartTarget(it) }

        setupRecyclerView()

        btnPassTurn.setOnClickListener {
            if (isOnline) passTurnOnline() else passTurnLocal()
        }

        if (isOnline) {
            if (myRole == "HOST") createCricketRoom()
            setupOnlineMode()
        }

        etP1.setText(intent.getStringExtra("P1_NAME") ?: "Oyuncu 1")
        etP2.setText(intent.getStringExtra("P2_NAME") ?: "Oyuncu 2")

        updateLegDisplay()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun triggerHapticFeedback() {
        val prefs = getSharedPreferences("DartSyncPrefs", Context.MODE_PRIVATE)
        val isVibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        if (isVibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvCricketBoard)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = DartAdapter(
            targetList = targetList,
            isReadOnly = false,
            onWin = { playerIndex ->
                handleLegWin(playerIndex)
            },
            onHit = { label, playerIndex, newHits ->
                if (isOnline) {
                    if (currentTurn != myUid) {
                        Toast.makeText(this, "Sıra rakipte!", Toast.LENGTH_SHORT).show()
                        return@DartAdapter
                    }
                    triggerHapticFeedback()
                    syncHitToFirebase(label, playerIndex, newHits)
                } else {
                    triggerHapticFeedback()
                }
            }
        )
        rv.adapter = adapter
    }

    private fun handleLegWin(playerIndex: Int) {
        if (playerIndex == 1) p1Legs++ else p2Legs++
        updateLegDisplay()

        // 🔥 MAÇ BİTİŞ MANTIĞI (Best of vs Beraberlik)
        if (targetLegs == 2) {
            // BERABERLİK SİSTEMİ (Fixed 2 Legs)
            if ((p1Legs + p2Legs) >= 2) {
                val winnerName = when {
                    p1Legs > p2Legs -> etP1.text.toString()
                    p2Legs > p1Legs -> etP2.text.toString()
                    else -> "Berabere"
                }
                showWinDialog(winnerName)
            } else {
                Toast.makeText(this, "Sonraki Leg! Skor: $p1Legs - $p2Legs", Toast.LENGTH_SHORT).show()
                resetBoardForNextLeg()
            }
        } else {
            // STANDART BEST OF SİSTEMİ (1, 3, 5)
            val winThreshold = (targetLegs / 2) + 1
            if (p1Legs >= winThreshold || p2Legs >= winThreshold) {
                val winnerName = if (p1Legs >= winThreshold) etP1.text.toString() else etP2.text.toString()
                showWinDialog(winnerName)
            } else {
                Toast.makeText(this, "Leg Kazanıldı! Skor: $p1Legs - $p2Legs", Toast.LENGTH_SHORT).show()
                resetBoardForNextLeg()
            }
        }
    }

    private fun resetBoardForNextLeg() {
        targetList.forEach {
            it.player1Hits = 0
            it.player2Hits = 0
        }
        adapter.notifyDataSetChanged()
    }

    private fun updateLegDisplay() {
        val formatLabel = if (targetLegs == 2) "Fixed 2L" else "BO $targetLegs"
        tvStatus.text = "Skor: $p1Legs - $p2Legs ($formatLabel)"
    }

    private fun showWinDialog(winnerName: String) {
        val title = if (winnerName == "Berabere") "MAÇ SONUCU" else "OYUN BİTTİ! 🏆"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Sonuç: $winnerName\nFinal Skoru: $p1Legs - $p2Legs")
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

            // 🔥 TURNUVA AĞACINA VERİ GÖNDERME
            if (isTournament) {
                val resultIntent = Intent()
                resultIntent.putExtra("WINNER_NAME", winnerName)
                // Beraberlikse ID null gitsin ki bracket activity anlasın
                resultIntent.putExtra("WINNER_ID", if (winnerName == "Berabere") null else winnerName)
                resultIntent.putExtra("MATCH_ID", matchId)
                resultIntent.putExtra("P1_SCORE", p1Legs)
                resultIntent.putExtra("P2_SCORE", p2Legs)
                setResult(RESULT_OK, resultIntent)
            }

            if (isOnline && myRole == "HOST") db.collection("rooms").document(roomCode).delete()
            finish()
        }
    }

    // --- Online Odalar İçin Yardımcı Fonksiyonlar ---
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
        roomRef.update("hits.$label.$pKey", newHits)
    }

    private fun passTurnOnline() {
        if (currentTurn != myUid) return
        val roomRef = db.collection("rooms").document(roomCode)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val hostId = snapshot.getString("hostId") ?: ""
            val guestId = snapshot.getString("guestId") ?: ""
            val nextTurn = if (myUid == hostId) guestId else hostId
            transaction.update(roomRef, "currentTurn", nextTurn)
            null
        }
    }

    private fun passTurnLocal() {
        Toast.makeText(this, "Sıra değişti", Toast.LENGTH_SHORT).show()
    }

    private fun updateTurnUI() {
        val isMyTurn = currentTurn == myUid
        adapter.isReadOnly = !isMyTurn
        btnPassTurn.visibility = if (isMyTurn) View.VISIBLE else View.GONE
        tvStatus.text = if (isMyTurn) "SENİN SIRAN 🎯" else "RAKİP ATIŞI..."
        tvStatus.setTextColor(if (isMyTurn) Color.GREEN else Color.RED)
    }
}