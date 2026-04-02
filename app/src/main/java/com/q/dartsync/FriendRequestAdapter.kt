package com.q.dartsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendRequestAdapter(
    private val requests: List<FriendRequest>,
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvRequestName)
        val btnAccept: ImageButton = v.findViewById(R.id.btnAccept)
        val btnDecline: ImageButton = v.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false))

    override fun onBindViewHolder(h: ViewHolder, p: Int) {
        val req = requests[p]

        // --- 💎 KRİTİK GÜNCELLEME ---
        // Eğer senderNickname boşsa (eski veriler için) ID'nin ilk 5 karakterini gösterir.
        // Ama yeni isteklerde doğrudan ismi basar.
        if (req.senderNickname.isNotEmpty()) {
            h.tvName.text = "${req.senderNickname} sana istek attı"
        } else {
            h.tvName.text = "Yeni İstek (ID: ${req.senderId.take(5)}...)"
        }

        h.btnAccept.setOnClickListener { onAccept(req) }
        h.btnDecline.setOnClickListener { onDecline(req) }
    }

    override fun getItemCount() = requests.size
}