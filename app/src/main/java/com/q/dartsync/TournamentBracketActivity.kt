package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class TournamentBracketActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TournamentAdapter
    private lateinit var viewKonfetti: KonfettiView

    private var tournamentId: String = ""
    private var selectedMode: String = "501"
    private var systemType: String = "SINGLE_ELIMINATION"
    private var targetLegs: Int = 3

    private var allMatches = listOf<TournamentMatch>()
    private var currentBracketView = BracketType.WINNERS

    // 🔥 MAÇ SONUCUNU SKORLARLA BİRLİKTE ALAN MERKEZ
    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val winnerId = data?.getStringExtra("WINNER_ID")
            val winnerName = data?.getStringExtra("WINNER_NAME")
            val matchId = data?.getStringExtra("MATCH_ID")
            val p1Score = data?.getIntExtra("P1_SCORE", 0) ?: 0
            val p2Score = data?.getIntExtra("P2_SCORE", 0) ?: 0

            if (matchId != null) {
                processTournamentLogic(matchId, winnerId, winnerName ?: "Bilinmeyen", p1Score, p2Score)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_bracket)

        viewKonfetti = findViewById(R.id.viewKonfetti)
        db = FirebaseFirestore.getInstance()

        tournamentId = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        selectedMode = intent.getStringExtra("SELECTED_MODE") ?: "501"
        systemType = intent.getStringExtra("SYSTEM_TYPE") ?: "SINGLE_ELIMINATION"

        setupRecyclerView()
        setupTabs()
        listenToTournamentData()
    }

    private fun setupRecyclerView() {
        val rvMatches = findViewById<RecyclerView>(R.id.rvMatches)
        adapter = TournamentAdapter(emptyList()) { match ->
            launchMatch(match)
        }
        rvMatches.layoutManager = LinearLayoutManager(this)
        rvMatches.adapter = adapter
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutBrackets)
        tabLayout.removeAllTabs()

        // Turnuva tipine göre sekmeleri kuruyoruz
        if (systemType == "GROUP_STAGE") {
            tabLayout.addTab(tabLayout.newTab().setText("Puan Durumu"))
            tabLayout.addTab(tabLayout.newTab().setText("Grup Maçları"))
            currentBracketView = BracketType.GROUP
        } else {
            tabLayout.addTab(tabLayout.newTab().setText("Winners"))
            tabLayout.addTab(tabLayout.newTab().setText("Losers"))
            currentBracketView = BracketType.WINNERS
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (systemType == "GROUP_STAGE") {
                    // Şimdilik sadece maç listesini gösteriyoruz
                    currentBracketView = BracketType.GROUP
                } else {
                    currentBracketView = if (tab?.position == 0) BracketType.WINNERS else BracketType.LOSERS
                }
                filterAndDisplayMatches()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun listenToTournamentData() {
        if (tournamentId.isEmpty()) return

        db.collection("tournaments").document(tournamentId).addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                targetLegs = it.getLong("legs")?.toInt() ?: 3
            }
        }

        db.collection("tournaments").document(tournamentId)
            .collection("matches")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                allMatches = snapshots?.toObjects(TournamentMatch::class.java) ?: emptyList()
                checkTournamentWinner()
                filterAndDisplayMatches()
            }
    }

    private fun filterAndDisplayMatches() {
        val filteredList = allMatches.filter { it.bracketType == currentBracketView }
            .sortedBy { it.round }
        adapter.updateMatches(filteredList)
    }

    private fun launchMatch(match: TournamentMatch) {
        val target = if (selectedMode.uppercase() == "CRICKET") CricketActivity::class.java else MainActivity::class.java
        val intent = Intent(this, target).apply {
            putExtra("P1_NAME", match.player1)
            putExtra("P2_NAME", match.player2)
            putExtra("MATCH_ID", match.matchId)
            putExtra("IS_TOURNAMENT", true)
            putExtra("TARGET_LEGS", targetLegs)
        }
        getResult.launch(intent)
    }

    private fun processTournamentLogic(matchId: String, winnerId: String?, winnerName: String, p1Score: Int, p2Score: Int) {
        val currentMatch = allMatches.find { it.matchId == matchId } ?: return

        val isDraw = (p1Score == p2Score && p1Score > 0)
        val loserId = if (winnerId == currentMatch.player1Id) currentMatch.player2Id else currentMatch.player1Id
        val loserName = if (winnerName == currentMatch.player1) currentMatch.player2 else currentMatch.player1

        val batch = db.batch()
        val tournamentRef = db.collection("tournaments").document(tournamentId)

        val currentMatchRef = tournamentRef.collection("matches").document(matchId)
        batch.update(currentMatchRef, mapOf(
            "status" to "finished",
            "winner" to if (isDraw) "Berabere" else winnerName,
            "winnerId" to if (isDraw) null else winnerId,
            "p1Score" to p1Score,
            "p2Score" to p2Score,
            "isDraw" to isDraw,
            "loserId" to if (isDraw) null else loserId
        ))

        if (!isDraw && winnerId != null) {
            currentMatch.nextMatchId?.let { nextId ->
                val nextMatch = allMatches.find { it.matchId == nextId }
                if (nextMatch != null) {
                    val nextRef = tournamentRef.collection("matches").document(nextId)
                    val fieldName = if (nextMatch.player1.isEmpty() || nextMatch.player1 == "TBD" || nextMatch.player1.contains("Winner")) "player1" else "player2"
                    batch.update(nextRef, fieldName, winnerName)
                    batch.update(nextRef, if (fieldName == "player1") "player1Id" else "player2Id", winnerId)
                }
            }

            currentMatch.loserTargetMatchId?.let { lbId ->
                val lbMatch = allMatches.find { it.matchId == lbId }
                if (lbMatch != null) {
                    val lbRef = tournamentRef.collection("matches").document(lbId)
                    val fieldName = if (lbMatch.player1.contains("WB L-")) "player1" else "player2"
                    batch.update(lbRef, fieldName, loserName)
                    batch.update(lbRef, if (fieldName == "player1") "player1Id" else "player2Id", loserId)
                }
            }
        }

        batch.commit().addOnSuccessListener {
            val msg = if (isDraw) "Maç Berabere Bitti!" else "Sonuç: $p1Score - $p2Score"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkTournamentWinner() {
        if (allMatches.isEmpty() || systemType == "GROUP_STAGE") return

        val pendingMatches = allMatches.filter { it.status != "finished" }
        if (pendingMatches.isEmpty()) {
            val finalMatch = allMatches.maxByOrNull { it.round }
            finalMatch?.winner?.let {
                if (it != "Berabere" && it != "TBD") triggerChampionCelebration(it)
            }
        }
    }

    private fun triggerChampionCelebration(winner: String) {
        val party = Party(
            speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x00D1FF, 0x00D26A),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 5, TimeUnit.SECONDS).perSecond(100)
        )
        viewKonfetti.start(party)

        AlertDialog.Builder(this)
            .setTitle("🏆 TURNUVA ŞAMPİYONU 🏆")
            .setMessage("Tebrikler $winner!\nBu turnuvanın en büyüğü sensin.")
            .setPositiveButton("Kupayı Al") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}