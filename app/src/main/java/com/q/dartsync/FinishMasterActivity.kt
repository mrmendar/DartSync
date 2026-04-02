package com.q.dartsync

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class FinishMasterActivity : AppCompatActivity() {

    // --- 🎮 TEMEL OYUN DEĞİŞKENLERİ ---
    private var targetLevel = 120
    private var currentRemaining = 120
    private var dartsThrownInLevel = 0
    private var currentInput = ""
    private var currentBackgroundColor = Color.parseColor("#121212")

    // --- 📡 ONLINE KONTROL DEĞİŞKENLERİ ---
    private var isOnline = false
    private var roomCode = ""
    private var myRole = ""
    private var opponentUid = ""
    private var currentTurn = ""
    private var myUid = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // --- 📊 İSTATİSTİK VE TAKİP ---
    private var startTimeMillis: Long = 0
    private var totalDartsInSession = 0
    private var sessionLogs = ""
    private var isSessionSaved = false

    private val validSingleDartScores = setOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 24, 25, 26, 27, 28, 30, 32, 33, 34, 36, 38, 39, 40,
        42, 45, 48, 50, 51, 54, 57, 60
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish_master)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        myUid = auth.currentUser?.uid ?: ""

        isOnline = intent.getBooleanExtra("IS_ONLINE", false)
        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        myRole = intent.getStringExtra("ROLE") ?: "HOST"
        opponentUid = intent.getStringExtra("OPPONENT_UID") ?: ""

        startTimeMillis = System.currentTimeMillis()

        findViewById<ImageButton>(R.id.btnExit)?.setOnClickListener { onBackPressed() }

        setupKeypad()
        updateUI()

        if (isOnline) {
            if (myRole == "HOST") createCoopRoom()
            listenRoomUpdates()
        }
    }

    // --- 📡 CO-OP (ORTAKLAŞA) MANTIK ---

    private fun createCoopRoom() {
        val newRoom = hashMapOf(
            "roomCode" to roomCode,
            "hostId" to myUid,
            "guestId" to opponentUid,
            "sharedScore" to targetLevel,
            "sharedDarts" to 0,
            "targetLevel" to targetLevel,
            "currentTurn" to myUid,
            "gameMode" to "FinishMaster",
            "status" to "playing"
        )
        db.collection("rooms").document(roomCode).set(newRoom)
    }

    private fun listenRoomUpdates() {
        db.collection("rooms").document(roomCode).addSnapshotListener { snapshot, _ ->
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

            currentRemaining = snapshot.getLong("sharedScore")?.toInt() ?: 120
            dartsThrownInLevel = snapshot.getLong("sharedDarts")?.toInt() ?: 0
            targetLevel = snapshot.getLong("targetLevel")?.toInt() ?: 120
            currentTurn = snapshot.getString("currentTurn") ?: ""

            updateUI()
        }
    }

    /**
     * 🔥 TIKANMA ÖNLEYİCİ SENKRONİZASYON (Transaction sürümü)
     */
    private fun syncCoopState(nextRemaining: Int, nextDarts: Int, nextLevel: Int, switchTurn: Boolean) {
        if (!isOnline) return

        val roomRef = db.collection("rooms").document(roomCode)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val hostId = snapshot.getString("hostId") ?: ""
            val guestId = snapshot.getString("guestId") ?: ""

            val updates = hashMapOf<String, Any>(
                "sharedScore" to nextRemaining,
                "sharedDarts" to nextDarts,
                "targetLevel" to nextLevel
            )

            if (switchTurn) {
                // Yerel veriye güvenmek yerine Firestore'daki IDs'lere bakarak sırayı devret
                updates["currentTurn"] = if (myUid == hostId) guestId else hostId
            }

            transaction.update(roomRef, updates)
            null
        }.addOnFailureListener {
            Toast.makeText(this, "Senkronizasyon hatası imat!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 🎯 OYUN MANTIK FONKSİYONLARI ---

    private fun processDart(score: Int) {
        if (isOnline && currentTurn != myUid) {
            Toast.makeText(this, "Rakibin atışını bekle imat!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!validSingleDartScores.contains(score)) {
            triggerHaptic(android.view.HapticFeedbackConstants.LONG_PRESS)
            return
        }

        triggerHaptic(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

        dartsThrownInLevel++
        totalDartsInSession++
        val nextScore = currentRemaining - score

        when {
            // 🏆 BAŞARI
            nextScore == 0 -> {
                animateBackground(Color.parseColor("#122D12"))
                savePBForLevel(targetLevel, dartsThrownInLevel)
                logLevelResult(targetLevel, dartsThrownInLevel.toString())
                Toast.makeText(this, "TEBRİKLER!", Toast.LENGTH_SHORT).show()

                val nextTarget = targetLevel + 1
                if (nextTarget > 170) {
                    finishSessionAndSave()
                } else {
                    if (isOnline) {
                        syncCoopState(nextTarget, 0, nextTarget, true)
                    } else {
                        targetLevel = nextTarget
                        resetLevel()
                    }
                }
            }

            // ❌ BUST VEYA 9 OK
            nextScore < 0 || nextScore == 1 || dartsThrownInLevel >= 9 -> {
                animateBackground(Color.parseColor("#121212"))
                triggerHaptic(android.view.HapticFeedbackConstants.REJECT)
                logLevelResult(targetLevel, if (dartsThrownInLevel >= 9) "FAIL" else "BUST")
                Toast.makeText(this, if (dartsThrownInLevel >= 9) "9 OK DOLDU!" else "BUST!", Toast.LENGTH_SHORT).show()

                if (isOnline) {
                    syncCoopState(targetLevel, 0, targetLevel, true)
                } else {
                    resetLevel()
                }
            }

            // 🎯 DEVAM
            else -> {
                currentRemaining = nextScore
                if (isOnline) {
                    val shouldSwitch = (dartsThrownInLevel % 3 == 0)
                    syncCoopState(currentRemaining, dartsThrownInLevel, targetLevel, shouldSwitch)
                } else {
                    updateUI()
                }
            }
        }
    }

    /**
     * 📊 UI GÜNCELLEME (Out'lar/Öneriler Geri Geldi)
     */
    private fun updateUI() {
        val tvTarget = findViewById<TextView>(R.id.tvTargetLevel)
        val tvRemaining = findViewById<TextView>(R.id.tvCurrentScore)
        val tvDarts = findViewById<TextView>(R.id.tvDartsRemaining)
        val tvStatus = findViewById<TextView>(R.id.tvCheckoutHint)
        val tvPB = findViewById<TextView>(R.id.tvPersonalBest)

        tvTarget.text = targetLevel.toString()
        tvRemaining.text = currentRemaining.toString()
        tvPB.text = "PB: ${getPBForLevel(targetLevel)} Ok"

        val remainingTotalDarts = 9 - dartsThrownInLevel
        tvDarts.text = remainingTotalDarts.toString()

        val dartsLeftInTurn = 3 - (dartsThrownInLevel % 3)
        val suggestion = CheckoutHelper.getSuggestion(currentRemaining, dartsLeftInTurn)

        if (isOnline) {
            val isMyTurn = currentTurn == myUid
            if (isMyTurn) {
                tvStatus.text = "SIRA SENDE 🎯\nÖneri: $suggestion"
                tvStatus.setTextColor(Color.GREEN)
                toggleKeypad(true)
            } else {
                tvStatus.text = "RAKİP ATIŞI..."
                tvStatus.setTextColor(Color.RED)
                toggleKeypad(false)
            }
        } else {
            tvStatus.text = suggestion
            tvStatus.setTextColor(Color.WHITE)
        }

        tvDarts.setTextColor(if (remainingTotalDarts <= 3) Color.RED else Color.parseColor("#FFD700"))
    }

    // --- 📂 VERİ VE KAYIT YARDIMCILARI (PB, Room DB vb. Aynen Korundu) ---

    private fun getPBForLevel(level: Int): Int {
        val prefs = getSharedPreferences("DartStats", MODE_PRIVATE)
        return prefs.getInt("PB_$level", 0)
    }

    private fun savePBForLevel(level: Int, darts: Int) {
        val prefs = getSharedPreferences("DartStats", MODE_PRIVATE)
        val currentPB = getPBForLevel(level)
        if (currentPB == 0 || darts < currentPB) {
            prefs.edit().putInt("PB_$level", darts).apply()
            Toast.makeText(this, "YENİ REKOR! 🏆", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logLevelResult(level: Int, result: String) {
        sessionLogs += "$level:$result, "
    }

    private fun finishSessionAndSave() {
        if (isSessionSaved) return
        isSessionSaved = true
        val sessionDuration = System.currentTimeMillis() - startTimeMillis
        val uniqueId = IdManager.getGuestId(this)

        lifecycleScope.launch {
            val dbRoom = AppDatabase.getDatabase(this@FinishMasterActivity)
            val result = GameResult(
                userId = uniqueId,
                player1Name = "Sen",
                player2Name = if (isOnline) "Ortak Maç" else "120 Antrenmanı",
                date = System.currentTimeMillis(),
                winnerName = "N/A",
                p1Snapshot = "0",
                p2Snapshot = "0",
                totalDarts = totalDartsInSession,
                isFinishMasterMode = true,
                finishedLevels = sessionLogs.removeSuffix(", "),
                sessionDurationMillis = sessionDuration,
                highestLevelReached = if (currentRemaining == 0) targetLevel else targetLevel - 1
            )
            dbRoom.gameResultDao().insertGame(result)
            finish()
        }
    }

    // --- 🛠️ KEYPAD VE DİĞER YARDIMCILAR ---

    private fun toggleKeypad(enabled: Boolean) {
        val keypadIds = intArrayOf(R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnClear, R.id.btnEnterScore)
        for (id in keypadIds) {
            findViewById<View>(id)?.apply {
                isEnabled = enabled
                alpha = if (enabled) 1.0f else 0.5f
            }
        }
    }

    private fun setupKeypad() {
        val tvInput = findViewById<TextView>(R.id.tvInput)
        for (i in 0..9) {
            val resId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(resId)?.setOnClickListener {
                if (currentInput.length < 3) {
                    currentInput += i.toString()
                    tvInput.text = currentInput
                }
            }
        }
        findViewById<Button>(R.id.btnClear)?.setOnClickListener { currentInput = ""; tvInput.text = "0" }
        findViewById<Button>(R.id.btnEnterScore)?.setOnClickListener {
            val score = currentInput.toIntOrNull() ?: 0
            processDart(score)
            currentInput = ""; tvInput.text = "0"
        }
    }

    private fun triggerHaptic(type: Int) { window.decorView.performHapticFeedback(type) }

    private fun animateBackground(toColor: Int) {
        val rootLayout = findViewById<ConstraintLayout>(R.id.mainLayout) ?: return
        ValueAnimator.ofObject(ArgbEvaluator(), currentBackgroundColor, toColor).apply {
            duration = 500
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                rootLayout.setBackgroundColor(color)
                currentBackgroundColor = color
            }
            start()
        }
    }

    private fun resetLevel() { currentRemaining = targetLevel; dartsThrownInLevel = 0; updateUI() }

    override fun onBackPressed() {
        if (isOnline) {
            AlertDialog.Builder(this)
                .setTitle("Maçtan Ayrıl")
                .setMessage("Online maçı bitirmek istiyor musun?")
                .setPositiveButton("Kaydet ve Çık") { _, _ -> finishSessionAndSave() }
                .setNegativeButton("Devam Et", null)
                .setNeutralButton("Kaydetmeden Çık") { _, _ -> finish() }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("120 Oyununu Bitir")
                .setMessage("İlerlemeyi kaydetmek istiyor musun?")
                .setPositiveButton("Kaydet ve Çık") { _, _ -> finishSessionAndSave() }
                .setNegativeButton("Devam Et", null)
                .setNeutralButton("Kaydetmeden Çık") { _, _ -> finish() }
                .show()
        }
    }
}