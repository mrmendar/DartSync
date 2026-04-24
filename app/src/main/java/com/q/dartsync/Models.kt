package com.q.dartsync

import com.google.firebase.Timestamp

// --- 👤 KULLANICI VE SOSYAL MODELLER ---
data class UserProfile(
    val uid: String = "",
    val nickname: String = "",
    val searchName: String = "",
    val email: String = "",
    val targetLevel: Int = 120,
    val friendsCount: Int = 0,
    val createdAt: Timestamp? = null
)

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val receiverId: String = "",
    val status: String = "pending",
    val timestamp: Timestamp? = null
)

data class FriendItem(
    val uid: String = "",
    val nickname: String = "",
    val targetLevel: Int = 120
)

data class GameInvite(
    val inviteId: String = "",
    val hostId: String = "",
    val hostNickname: String = "",
    val guestId: String = "",
    val roomCode: String = "",
    val gameMode: String = "501",
    val status: String = "pending",
    val timestamp: Timestamp? = null
)

// --- 🎮 OYUN VE ODA MODELLERİ ---
data class GameRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val guestId: String = "",
    val hostNickname: String = "",
    val guestNickname: String = "",
    val hostScore: Int = 501,
    val guestScore: Int = 501,
    val currentTurn: String = "",
    val lastShot: Int = 0,
    val status: String = "playing",
    val gameMode: String = "501",
    val winnerId: String = "",
    val timestamp: Timestamp? = null
)

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

// --- 🏆 TURNUVA MODELLERİ (Esnek Grup & Beraberlik Destekli) ---

enum class BracketType { WINNERS, LOSERS, FINAL, GROUP }
enum class TournamentType { SINGLE_ELIMINATION, DOUBLE_ELIMINATION, GROUP_STAGE }

data class TournamentMatch(
    val matchId: String = "",
    val tournamentId: String = "",
    val player1: String = "",
    val player2: String = "",
    val player1Id: String? = null,
    val player2Id: String? = null,
    val winner: String? = null,
    val winnerId: String? = null,
    val loserId: String? = null,
    val round: Int = 1,
    val bracketType: BracketType = BracketType.WINNERS,
    val nextMatchId: String? = null,
    val loserTargetMatchId: String? = null,
    val status: String = "pending", // pending, finished

    // 🔥 SKOR VE FORMAT BİLGİLERİ (Beraberlik Destekli)
    val p1Score: Int = 0,
    val p2Score: Int = 0,
    val isDraw: Boolean = false,    // 🔥 1-1 biten maçlar için
    val bestOf: Int = 3,            // 1, 3, 5 (BO) veya 2 (Fixed Legs)
    val groupId: String? = null     // Grup aşamasıysa grup ID'si
)

data class TournamentSession(
    val tournamentId: String = "",
    val name: String = "",
    val type: TournamentType = TournamentType.SINGLE_ELIMINATION,
    val adminId: String = "",
    val participants: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Timestamp = Timestamp.now(),

    // 🔥 ORGANİZATÖR AYARLARI
    val legs: Int = 3,                 // BO1, BO2 (Draw), BO3, BO5
    val groupCount: Int = 0,           // Kaç gruba bölünecek
    val qualifiersPerGroup: Int = 0,   // Gruptan kaç kişi çıkacak

    // 🔥 PUANLAMA SİSTEMİ (Örn: Galibiyet 3, Beraberlik 1)
    val pointsForWin: Int = 3,
    val pointsForDraw: Int = 1,
    val pointsForLoss: Int = 0
)

// 🔥 GRUP PUAN DURUMU (Tabloyu oluşturmak için)
data class GroupStanding(
    val playerId: String = "",
    val playerNickname: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val legsWon: Int = 0,
    val legsLost: Int = 0,
    val points: Int = 0
)