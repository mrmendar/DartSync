package com.q.dartsync

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class TournamentAdapter(
    private var matches: List<TournamentMatch> = emptyList(),
    private val onMatchClick: (TournamentMatch) -> Unit
) : RecyclerView.Adapter<TournamentAdapter.MatchViewHolder>() {

    fun updateMatches(newMatches: List<TournamentMatch>) {
        this.matches = newMatches
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.bind(match, onMatchClick)
    }

    override fun getItemCount(): Int = matches.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlayer1 = itemView.findViewById<TextView>(R.id.tvPlayer1)
        private val tvPlayer2 = itemView.findViewById<TextView>(R.id.tvPlayer2)
        private val tvRoundInfo = itemView.findViewById<TextView>(R.id.tvRoundInfo)
        private val tvBracketType = itemView.findViewById<TextView>(R.id.tvBracketType)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvMatchStatus)
        private val tvVs = itemView.findViewById<TextView>(R.id.tvVs)

        fun bind(match: TournamentMatch, onMatchClick: (TournamentMatch) -> Unit) {
            // Oyuncu İsimleri
            tvPlayer1.text = if (match.player1.isEmpty() || match.player1 == "TBD") "Bekleniyor..." else match.player1
            tvPlayer2.text = if (match.player2.isEmpty() || match.player2 == "TBD") "Bekleniyor..." else match.player2

            // Tur Bilgisi
            tvRoundInfo.text = "ROUND ${match.round}"

            // Tablo Tipi (Winners/Losers)
            tvBracketType.text = match.bracketType.name
            if (match.bracketType == BracketType.LOSERS) {
                tvBracketType.setTextColor(Color.parseColor("#FFA500"))
            } else {
                tvBracketType.setTextColor(Color.parseColor("#8B8B9E"))
            }

            resetStyles()

            // Maç Durumu Kontrolü
            if (match.status == "finished") {
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "🏆 KAZANAN: ${match.winner ?: "Belli değil"}"
                tvVs.text = "BİTTİ"

                // Kazananı parlat, kaybedeni soluklaştır
                if (match.winnerId == match.player1Id) {
                    tvPlayer1.setTextColor(Color.parseColor("#4CAF50"))
                    tvPlayer2.setTextColor(Color.parseColor("#55FFFFFF"))
                } else {
                    tvPlayer2.setTextColor(Color.parseColor("#4CAF50"))
                    tvPlayer1.setTextColor(Color.parseColor("#55FFFFFF"))
                }
            } else {
                tvStatus.visibility = View.GONE
                tvVs.text = " vs "
            }

            // --- 🔒 TIKLAMA KİLİDİ ---
            itemView.setOnClickListener {
                if (match.status == "finished") {
                    // Maç zaten bittiyse sadece bilgi ver
                    Toast.makeText(itemView.context, "Bu maç zaten tamamlandı!", Toast.LENGTH_SHORT).show()
                } else if (isMatchPlayable(match)) {
                    // Sadece bitmemiş ve oyuncuları belli maçları başlat
                    onMatchClick(match)
                } else {
                    // Oyuncular henüz belli değilse (TBD) bilgi ver
                    Toast.makeText(itemView.context, "Eşleşme henüz hazır değil!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun isMatchPlayable(match: TournamentMatch): Boolean {
            // Oynanabilirlik Şartları:
            // 1. Durumu bitmiş olmayacak
            // 2. Oyuncular boş, BYE veya Bekleniyor olmayacak
            return match.status != "finished" &&
                    match.player1.isNotEmpty() && match.player2.isNotEmpty() &&
                    match.player1 != "BYE" && match.player2 != "BYE" &&
                    match.player1 != "Bekleniyor..." && match.player2 != "Bekleniyor..." &&
                    match.player1 != "TBD" && match.player2 != "TBD"
        }

        private fun resetStyles() {
            tvPlayer1.setTextColor(Color.WHITE)
            tvPlayer2.setTextColor(Color.WHITE)
        }
    }
}