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

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // 🔥 KRİTİK EKLEME: Uygulamanın açıldığı milisaniyeyi tutuyoruz.
    // Bu sayede uygulamayı açmadan önce atılmış davetleri filtreleyeceğiz.
    private val sessionStartTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
        fetchUserData()

        // Gelen oyun davetlerini dinle
        listenForGameInvites()
    }

    // --- 📡 ONLINE DURUM YÖNETİMİ ---

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

    // --- 🎮 DAVET SİSTEMİ (ZOMBİ KORUMALI) ---

    private fun listenForGameInvites() {
        val myUid = auth.currentUser?.uid ?: return

        db.collection("gameInvites")
            .whereEqualTo("guestId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                snapshots?.let {
                    for (dc in it.documentChanges) {
                        // Sadece yeni eklenen dökümanlara bak
                        if (dc.type == DocumentChange.Type.ADDED) {
                            val doc = dc.document

                            // 🔥 VERİ KAYBI ÖNLEYİCİ FİLTRE:
                            // Davetin atılma zamanını milisaniye cinsinden alıyoruz.
                            val inviteTime = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0

                            // Eğer davet, uygulama açıldıktan sonra (veya tam o sırada) geldiyse göster.
                            // 5000ms (5sn) pay bırakıyoruz ki sunucu gecikmeleri sorun yaratmasın.
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

    // --- 🛠️ DİĞER UI VE MOD İŞLEMLERİ (MEVCUT KODUN) ---

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
            tvUsername.text = "${doc.getString("nickname") ?: "Dartçı"}!"
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