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

    // 🔥 Davet dinleyicisini kontrol altında tutmak için değişken
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

    // --- 📡 LİFECYCLE YÖNETİMİ (Crash Önleyici) ---

    override fun onStart() {
        super.onStart()
        // Uygulama ekrana geldiğinde dinlemeye başla
        listenForGameInvites()
    }

    override fun onStop() {
        super.onStop()
        // 🔥 KRİTİK: Uygulama durduğunda (arka plana geçtiğinde) dinlemeyi kes.
        // Böylece BadTokenException (çökme) riski ortadan kalkar.
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

    // --- 🎮 DAVET SİSTEMİ ---

    private fun listenForGameInvites() {
        val myUid = auth.currentUser?.uid ?: return

        // Önceki listener varsa temizle
        inviteListener?.remove()

        inviteListener = db.collection("gameInvites")
            .whereEqualTo("guestId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                // 🔥 GÜVENLİK: Activity ölme aşamasındaysa hiçbir UI işlemi yapma
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
        // 🔥 GÜVENLİK KONTROLÜ: Activity pencere eklemeye uygun mu?
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("🎮 Meydan Okuma!")
            .setMessage("$hostName seni $gameMode maçına bekliyor. Katılmak ister misin?")
            .setCancelable(false)
            .setPositiveButton("KABUL ET") { _, _ ->
                acceptInvite(inviteId, roomCode, gameMode)
            }
            .setNegativeButton("REDDET") { _, _ ->
                db.collection("gameInvites").document(inviteId).delete()
            }
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

    // --- 🛠️ DİĞER UI İŞLEMLERİ ---

    private fun setupUI() {
        val cardStartGame = findViewById<CardView>(R.id.cardStartGame)
        val cardHistory = findViewById<CardView>(R.id.cardHistory)
        val cardStats = findViewById<CardView>(R.id.cardStats)
        val cardFriends = findViewById<CardView>(R.id.cardFriends)
        val cardSettings = findViewById<CardView>(R.id.cardSettings)
        val btnSignOut = findViewById<TextView>(R.id.btnSignOut)

        cardStartGame.setOnClickListener { showGameModeDialog() }
        cardHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        cardStats.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        cardFriends.setOnClickListener { startActivity(Intent(this, SocialActivity::class.java)) }
        cardSettings.setOnClickListener { Toast.makeText(this, "Ayarlar yakında!", Toast.LENGTH_SHORT).show() }

        btnSignOut.setOnClickListener {
            updateOnlineStatus(false)
            inviteListener?.remove() // Dinleyiciyi kapat
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

    private fun showGameModeDialog() {
        val modeDialog = BottomSheetDialog(this)
        val modeView = layoutInflater.inflate(R.layout.dialog_game_modes, null)

        modeView.findViewById<android.view.View>(R.id.mode501)?.setOnClickListener {
            modeDialog.dismiss()
            showConnectionChoice(MainActivity::class.java)
        }
        modeView.findViewById<android.view.View>(R.id.modeCricket)?.setOnClickListener {
            modeDialog.dismiss()
            showConnectionChoice(CricketActivity::class.java)
        }
        modeView.findViewById<android.view.View>(R.id.modeTournament)?.setOnClickListener {
            modeDialog.dismiss()
            startActivity(Intent(this, TournamentSetupActivity::class.java))
        }
        modeView.findViewById<android.view.View>(R.id.modeFinishMaster)?.setOnClickListener {
            modeDialog.dismiss()
            startActivity(Intent(this, FinishMasterActivity::class.java))
        }
        modeDialog.setContentView(modeView)
        modeDialog.show()
    }

    private fun showConnectionChoice(targetActivity: Class<*>) {
        val choiceDialog = BottomSheetDialog(this)
        val choiceView = layoutInflater.inflate(R.layout.dialog_connection_choice, null)

        choiceView.findViewById<CardView>(R.id.btnLocalGame)?.setOnClickListener {
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", false)
            startActivity(intent)
            choiceDialog.dismiss()
        }
        choiceView.findViewById<CardView>(R.id.btnOnlineGame)?.setOnClickListener {
            choiceDialog.dismiss()
            showRoomDialog(targetActivity)
        }
        choiceDialog.setContentView(choiceView)
        choiceDialog.show()
    }

    private fun showRoomDialog(targetActivity: Class<*>) {
        val roomDialog = BottomSheetDialog(this)
        val roomView = layoutInflater.inflate(R.layout.dialog_join_room, null)

        roomView.findViewById<CardView>(R.id.btnJoinRoom)?.setOnClickListener {
            val etCode = roomView.findViewById<EditText>(R.id.etRoomCode)
            val code = etCode.text.toString()
            if (code.isNotEmpty()) {
                val intent = Intent(this, targetActivity)
                intent.putExtra("IS_ONLINE", true)
                intent.putExtra("ROOM_CODE", code)
                intent.putExtra("ROLE", "GUEST")
                startActivity(intent)
                roomDialog.dismiss()
            }
        }

        roomView.findViewById<CardView>(R.id.btnCreateRoom)?.setOnClickListener {
            val randomCode = (1000..9999).random().toString()
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", true)
            intent.putExtra("ROOM_CODE", randomCode)
            intent.putExtra("ROLE", "HOST")
            startActivity(intent)
            roomDialog.dismiss()
        }
        roomDialog.setContentView(roomView)
        roomDialog.show()
    }
}