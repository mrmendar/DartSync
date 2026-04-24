package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 📡 Davet dinleyicisi yönetimi
    private var inviteListener: ListenerRegistration? = null
    private val sessionStartTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
        fetchUserData()
    }

    // --- 📡 LİFECYCLE VE DİNLENME YÖNETİMİ ---

    override fun onStart() {
        super.onStart()
        listenForGameInvites()
    }

    override fun onStop() {
        super.onStop()
        inviteListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        updateOnlineStatus(true)
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(status: Boolean) {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("users").document(myUid).update("isOnline", status)
    }

    // --- 🎮 DAVET SİSTEMİ (Online Oyun Buradan Yönetilir) ---

    private fun listenForGameInvites() {
        val myUid = auth.currentUser?.uid ?: return
        inviteListener?.remove()

        inviteListener = db.collection("gameInvites")
            .whereEqualTo("guestId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (isFinishing || isDestroyed) return@addSnapshotListener

                snapshots?.let {
                    for (dc in it.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val doc = dc.document
                            val inviteTime = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0

                            if (inviteTime > (sessionStartTime - 5000)) {
                                val hostName = doc.getString("hostNickname") ?: "Bir arkadaşın"
                                val roomCode = doc.getString("roomCode") ?: ""
                                val gameMode = doc.getString("gameMode") ?: "501"
                                val inviteId = doc.id

                                showInviteDialog(hostName, roomCode, gameMode, inviteId)
                            }
                        }
                    }
                }
            }
    }

    private fun showInviteDialog(hostName: String, roomCode: String, gameMode: String, inviteId: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("🎮 Meydan Okuma!")
            .setMessage("$hostName seni $gameMode maçına bekliyor. Katılmak ister misin?")
            .setCancelable(false)
            .setPositiveButton("KABUL ET") { _, _ -> acceptInvite(inviteId, roomCode, gameMode) }
            .setNegativeButton("REDDET") { _, _ -> db.collection("gameInvites").document(inviteId).delete() }
            .show()
    }

    private fun acceptInvite(inviteId: String, roomCode: String, gameMode: String) {
        db.collection("gameInvites").document(inviteId).update("status", "accepted")

        val targetActivity = when (gameMode) {
            "Cricket" -> CricketActivity::class.java
            "FinishMaster" -> FinishMasterActivity::class.java
            else -> MainActivity::class.java
        }

        val intent = Intent(this, targetActivity)
        intent.putExtra("IS_ONLINE", true)
        intent.putExtra("ROOM_CODE", roomCode)
        intent.putExtra("ROLE", "GUEST")
        startActivity(intent)
    }

    // --- 🛠️ UI KURULUMU ---

    private fun setupUI() {
        val cardStartGame = findViewById<CardView>(R.id.cardStartGame)
        val cardHistory = findViewById<CardView>(R.id.cardHistory)
        val cardStats = findViewById<CardView>(R.id.cardStats)
        val cardFriends = findViewById<CardView>(R.id.cardFriends)
        val cardSettings = findViewById<CardView>(R.id.cardSettings)
        val btnSignOut = findViewById<TextView>(R.id.btnSignOut)
        val cardTournament = findViewById<androidx.cardview.widget.CardView>(R.id.cardTournament)

        cardTournament.setOnClickListener {
            val intent = Intent(this, TournamentConfigActivity::class.java)
            startActivity(intent)
        }

        cardStartGame.setOnClickListener { showGameModeDialog() }
        cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        cardStats.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        cardFriends.setOnClickListener { startActivity(Intent(this, SocialActivity::class.java)) }

        cardSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        btnSignOut.setOnClickListener {
            updateOnlineStatus(false)
            inviteListener?.remove()
            auth.signOut()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchUserData() {
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
            if (!(isFinishing || isDestroyed)) {
                tvUsername.text = "${doc.getString("nickname") ?: "Dartçı"}!"
            }
        }
    }

    // --- 🎯 OYUN MODU VE HIZLI BAŞLATMA ---

    private fun showGameModeDialog() {
        val modeDialog = BottomSheetDialog(this)
        val modeView = layoutInflater.inflate(R.layout.dialog_game_modes, null)

        modeView.apply {
            // 501 Modu: Direkt Yerel Maç Başlatır
            findViewById<android.view.View>(R.id.mode501)?.setOnClickListener {
                modeDialog.dismiss()
                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
            }

            // Cricket Modu: Direkt Yerel Maç Başlatır
            findViewById<android.view.View>(R.id.modeCricket)?.setOnClickListener {
                modeDialog.dismiss()
                startActivity(Intent(this@HomeActivity, CricketActivity::class.java))
            }

            // Turnuva Modu
            findViewById<android.view.View>(R.id.modeTournament)?.setOnClickListener {
                modeDialog.dismiss()
                startActivity(Intent(this@HomeActivity, TournamentSetupActivity::class.java))
            }


            // Finish Master Modu
            findViewById<android.view.View>(R.id.modeFinishMaster)?.setOnClickListener {
                modeDialog.dismiss()
                startActivity(Intent(this@HomeActivity, FinishMasterActivity::class.java))
            }
        }
        modeDialog.setContentView(modeView)
        modeDialog.show()
    }

    // NOT: showConnectionChoice ve showRoomDialog fonksiyonları
    // "Oyuna Başla" akışından kaldırılmıştır çünkü Online oyun "Arkadaşlar" sekmesinden yönetilmektedir.
}