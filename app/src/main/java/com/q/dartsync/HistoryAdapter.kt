package com.q.dartsync

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val gameList: List<GameResult>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlayers: TextView = view.findViewById(R.id.tvPlayers)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val game = gameList[position]
        holder.tvPlayers.text = "${game.player1Name} vs ${game.player2Name}"

        // Tarihi okunabilir formata çeviriyoruz
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(game.date))

        // HistoryAdapter içindeki onBindViewHolder kısmına ekle:
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, HistoryDetailActivity::class.java).apply {
                putExtra("p1Name", game.player1Name)
                putExtra("p2Name", game.player2Name)
                putExtra("p1Snapshot", game.p1Snapshot)
                putExtra("p2Snapshot", game.p2Snapshot)
            }
            holder.itemView.context.startActivity(intent)
        }

    }


    override fun getItemCount(): Int = gameList.size
}