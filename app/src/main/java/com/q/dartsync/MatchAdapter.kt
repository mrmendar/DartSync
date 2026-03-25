package com.q.dartsync

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MatchAdapter(
    private val matches: List<TournamentMatch>,
    private val onMatchClick: (TournamentMatch, Int) -> Unit // 🔥 Artık pozisyonu (Int) da döndürüyor
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val p1: TextView = view.findViewById(R.id.tvPlayer1)
        val p2: TextView = view.findViewById(R.id.tvPlayer2)
        val vs: TextView = view.findViewById(R.id.tvVs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.p1.text = match.player1
        holder.p2.text = match.player2

        // Varsayılan renkleri temizle (RecyclerView hücrelerini tekrar kullandığı için önemli)
        holder.p1.setTextColor(Color.WHITE)
        holder.p2.setTextColor(Color.WHITE)

        when {
            // 1. Durum: Maç bittiyse (Kazanan belliyse)
            match.winner != null -> {
                holder.vs.text = "BİTTİ"
                holder.vs.setTextColor(Color.GRAY)
                holder.itemView.isEnabled = false

                // Kazananı yeşil yaparak vurgula
                if (match.winner == match.player1) {
                    holder.p1.setTextColor(Color.parseColor("#00D26A"))
                } else if (match.winner == match.player2) {
                    holder.p2.setTextColor(Color.parseColor("#00D26A"))
                }
            }

            // 2. Durum: Rakip BYE ise (Otomatik tur atlama)
            match.player2 == "BYE" -> {
                holder.vs.text = "TUR ATLADI"
                holder.vs.setTextColor(Color.parseColor("#00D26A"))
                holder.p1.setTextColor(Color.parseColor("#00D26A"))
                holder.itemView.isEnabled = false
            }

            // 3. Durum: Maç oynanmaya hazırsa
            else -> {
                holder.vs.text = "VS"
                holder.vs.setTextColor(Color.parseColor("#00D1FF"))
                holder.itemView.isEnabled = true
                holder.itemView.setOnClickListener {
                    onMatchClick(match, position) // Pozisyon bilgisini geri gönderiyoruz
                }
            }
        }
    }

    override fun getItemCount() = matches.size
}