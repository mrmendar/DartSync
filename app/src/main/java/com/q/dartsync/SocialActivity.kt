package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SocialActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var friendAdapter: FriendAdapter
    private val friendsList = mutableListOf<FriendItem>()

    private lateinit var requestAdapter: FriendRequestAdapter
    private val requestsList = mutableListOf<FriendRequest>()
    private var myNickname: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
        fetchMyInfo()
        loadFriends()
        listenForRequests()
    }

    private fun setupUI() {
        val etSearch = findViewById<EditText>(R.id.etSearchNickname)
        val btnSearch = findViewById<Button>(R.id.btnSearchUser)
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim().lowercase()
            if (query.isNotEmpty()) searchUser(query)
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriendsList)

        // Callback ile adapter'ı başlatıyoruz
        friendAdapter = FriendAdapter(friendsList) { friend ->
            showChallengeModeDialog(friend)
        }

        rvFriends.layoutManager = LinearLayoutManager(this)
        rvFriends.adapter = friendAdapter

        val rvRequests = findViewById<RecyclerView>(R.id.rvRequestsList)
        requestAdapter = FriendRequestAdapter(requestsList,
            onAccept = { request -> handleRequest(request, true) },
            onDecline = { request -> handleRequest(request, false) }
        )
        rvRequests?.layoutManager = LinearLayoutManager(this)
        rvRequests?.adapter = requestAdapter
    }

    /**
     * 🎮 MEYDAN OKUMA DİYALOĞU
     */
    private fun showChallengeModeDialog(friend: FriendItem) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_game_modes, null)

        val modeTournament = view.findViewById<View>(R.id.modeTournament)
        val mode501 = view.findViewById<View>(R.id.mode501)
        val modeCricket = view.findViewById<View>(R.id.modeCricket)
        val modeFinishMaster = view.findViewById<View>(R.id.modeFinishMaster)

        modeTournament?.visibility = View.GONE

        mode501?.setOnClickListener {
            dialog.dismiss()
            startOnlineChallenge(MainActivity::class.java, friend, "501")
        }

        modeCricket?.setOnClickListener {
            dialog.dismiss()
            startOnlineChallenge(CricketActivity::class.java, friend, "Cricket")
        }

        modeFinishMaster?.setOnClickListener {
            dialog.dismiss()
            startOnlineChallenge(FinishMasterActivity::class.java, friend, "FinishMaster")
        }

        dialog.setContentView(view)
        dialog.show()
    }

    /**
     * 🚀 ONLINE MAÇI BAŞLAT VE DAVET GÖNDER (GÜNCELLENDİ)
     */
    private fun startOnlineChallenge(targetActivity: Class<*>, friend: FriendItem, mode: String) {
        val myUid = auth.currentUser?.uid ?: return
        val randomRoomCode = (1000..9999).random().toString()

        // 🔥 GÜNCELLEME: Katı 'isOnline' kontrolü kaldırıldı.
        // Daveti doğrudan gönderiyoruz, Firebase senkronizasyon gecikmelerinden etkilenmiyoruz.
        val inviteData = hashMapOf(
            "hostId" to myUid,
            "hostNickname" to myNickname,
            "guestId" to friend.uid,
            "roomCode" to randomRoomCode,
            "gameMode" to mode,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("gameInvites").add(inviteData).addOnSuccessListener {
            // Davet başarıyla oluşturuldu, odaya HOST olarak gir
            val intent = Intent(this, targetActivity)
            intent.putExtra("IS_ONLINE", true)
            intent.putExtra("ROOM_CODE", randomRoomCode)
            intent.putExtra("OPPONENT_UID", friend.uid)
            intent.putExtra("ROLE", "HOST")

            startActivity(intent)

            Toast.makeText(this, "${friend.nickname} davet edildi. Bekleniyor...", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Davet gönderilemedi!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MEVCUT DİĞER FONKSİYONLAR ---

    private fun fetchMyInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener {
            myNickname = it.getString("nickname") ?: "İsimsiz"
        }
    }

    private fun loadFriends() {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("users").document(myUid).collection("friends")
            .addSnapshotListener { snapshots, _ ->
                friendsList.clear()
                snapshots?.let {
                    for (doc in it) friendsList.add(doc.toObject(FriendItem::class.java))
                }
                friendAdapter.notifyDataSetChanged()
            }
    }

    private fun listenForRequests() {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("friendRequests")
            .whereEqualTo("receiverId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, _ ->
                requestsList.clear()
                snapshots?.let {
                    for (doc in it) {
                        val req = doc.toObject(FriendRequest::class.java).copy(requestId = doc.id)
                        requestsList.add(req)
                    }
                }
                requestAdapter.notifyDataSetChanged()
                findViewById<TextView>(R.id.tvRequestsHeader)?.visibility =
                    if (requestsList.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun handleRequest(request: FriendRequest, accept: Boolean) {
        if (!accept) {
            db.collection("friendRequests").document(request.requestId).delete()
            return
        }
        val senderName = if (request.senderNickname.isNotEmpty()) request.senderNickname else "Bilinmeyen"
        executeFriendshipTransaction(request, senderName)
    }

    private fun executeFriendshipTransaction(request: FriendRequest, senderName: String) {
        val myUid = auth.currentUser?.uid ?: return
        db.runTransaction { transaction ->
            val myFriendRef = db.collection("users").document(myUid).collection("friends").document(request.senderId)
            transaction.set(myFriendRef, FriendItem(request.senderId, senderName, 120))

            val hisFriendRef = db.collection("users").document(request.senderId).collection("friends").document(myUid)
            transaction.set(hisFriendRef, FriendItem(myUid, myNickname, 120))

            transaction.delete(db.collection("friendRequests").document(request.requestId))
            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Artık arkadaşsınız!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchUser(nickname: String) {
        db.collection("users").whereEqualTo("searchName", nickname).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) Toast.makeText(this, "Bulunamadı!", Toast.LENGTH_SHORT).show()
                else for (doc in documents) {
                    val uid = doc.getString("uid") ?: ""
                    val name = doc.getString("nickname") ?: ""
                    if (uid != auth.currentUser?.uid) showAddFriendDialog(uid, name)
                }
            }
    }

    private fun showAddFriendDialog(targetUid: String, targetNickname: String) {
        AlertDialog.Builder(this)
            .setTitle("Arkadaş Ekle")
            .setMessage("$targetNickname adlı kullanıcıya istek gönderilsin mi?")
            .setPositiveButton("Gönder") { _, _ -> validateAndSendRequest(targetUid, targetNickname) }
            .setNegativeButton("İptal", null).show()
    }

    private fun validateAndSendRequest(targetUid: String, targetNickname: String) {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("users").document(myUid).collection("friends").document(targetUid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Toast.makeText(this, "Bu kullanıcıyla zaten arkadaşsın!", Toast.LENGTH_SHORT).show()
                } else {
                    checkPendingRequests(myUid, targetUid, targetNickname)
                }
            }
    }

    private fun checkPendingRequests(myUid: String, targetUid: String, targetNickname: String) {
        db.collection("friendRequests")
            .whereEqualTo("senderId", myUid)
            .whereEqualTo("receiverId", targetUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { sentDocs ->
                if (!sentDocs.isEmpty) {
                    Toast.makeText(this, "Zaten bekleyen bir isteğin var!", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("friendRequests")
                        .whereEqualTo("senderId", targetUid)
                        .whereEqualTo("receiverId", myUid)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener { receivedDocs ->
                            if (!receivedDocs.isEmpty) {
                                Toast.makeText(this, "$targetNickname sana zaten istek atmış!", Toast.LENGTH_LONG).show()
                            } else {
                                finalSendRequest(myUid, targetUid, targetNickname)
                            }
                        }
                }
            }
    }

    private fun finalSendRequest(myUid: String, targetUid: String, targetNickname: String) {
        val data = hashMapOf(
            "senderId" to myUid,
            "senderNickname" to myNickname,
            "receiverId" to targetUid,
            "status" to "pending",
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("friendRequests").add(data).addOnSuccessListener {
            Toast.makeText(this, "İstek gönderildi!", Toast.LENGTH_SHORT).show()
        }
    }
}