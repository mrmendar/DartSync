package com.q.dartsync

import com.google.firebase.Timestamp

/**
 * Firestore 'users' koleksiyonu için veri modeli.
 */
data class UserProfile(
    val uid: String = "",
    val nickname: String = "",
    val searchName: String = "",
    val email: String = "",
    val targetLevel: Int = 120,
    val friendsCount: Int = 0,
    val createdAt: Timestamp? = null
)

/**
 * Firestore 'friendRequests' koleksiyonu için veri modeli.
 */
data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val receiverId: String = "",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Timestamp? = null
)

/**
 * Arkadaş listesinde görünecek özet bilgi modeli.
 */
data class FriendItem(
    val uid: String = "",
    val nickname: String = "",
    val targetLevel: Int = 120
)

/**
 * 🔥 YENİ: Firestore 'gameInvites' koleksiyonu için veri modeli.
 * HomeActivity'deki radarın (listener) yakaladığı davet paketidir.
 */
data class GameInvite(
    val inviteId: String = "",
    val hostId: String = "",
    val hostNickname: String = "",
    val guestId: String = "",
    val roomCode: String = "",
    val gameMode: String = "501",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Timestamp? = null
)

/**
 * 🔥 YENİ: Firestore 'rooms' koleksiyonu için ana veri modeli.
 * Oyunun o anki tüm skorlarını ve sırasını tutan "Kara Kutu".
 */
data class GameRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val hostNickname: String = "",
    val guestNickname: String = "",
    val hostScore: Int = 501,
    val guestScore: Int = 501,
    val currentTurn: String = "", // Sırası gelen oyuncunun UID'si
    val lastShot: Int = 0,
    val status: String = "playing", // playing, finished
    val gameMode: String = "501",
    val winnerId: String = "",
    val timestamp: Timestamp? = null
)

/**
 * Oyun sonuçlarını Firestore'a senkronize etmek için model.
 */
data class RemoteGameResult(
    val userId: String = "",
    val player1Name: String = "",
    val player2Name: String = "",
    val totalDarts: Int = 0,
    val highestLevelReached: Int = 0,
    val isFinishMasterMode: Boolean = true,
    val finishedLevels: String = "",
    val sessionDurationMillis: Long = 0,
    val timestamp: Timestamp? = null
)