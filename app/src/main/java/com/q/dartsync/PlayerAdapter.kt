package com.q.dartsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlayerAdapter(
    private val playerList: List<String>,
    private val onItemClick: (String) -> Unit, // Tıklama (Detay için)
    private val onItemLongClick: (String) -> Unit // 🔥 Yeni: Uzun Tıklama (Silme için)
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlayerNameItem)
        val tvInitials: TextView = view.findViewById(R.id.tvPlayerInitials)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val playerName = playerList[position]
        holder.tvName.text = playerName

        // Görsellik: İsmin baş harfini büyük olarak ayarla
        holder.tvInitials.text = playerName.take(1).uppercase()

        // 1. Kısa Tıklama: Oyuncu detaylarına (Pasta Grafik) gider
        holder.itemView.setOnClickListener {
            onItemClick(playerName)
        }

        // 2. Uzun Tıklama: Silme uyarısını (Dialog) tetikler
        holder.itemView.setOnLongClickListener {
            onItemLongClick(playerName)
            true // Tıklama olayının burada bittiğini sisteme bildirir
        }
    }

    override fun getItemCount(): Int = playerList.size
}