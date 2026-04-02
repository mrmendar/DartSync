package com.q.dartsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendAdapter(
    private val friendList: List<FriendItem>,
    private val onChallenge: (FriendItem) -> Unit // 🔥 Meydan okuma için eklenen callback
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvFriendName)
        val tvLevel: TextView = view.findViewById(R.id.tvFriendLevel)
        val btnAction: ImageView = view.findViewById(R.id.btnAction) // Mavi ok ikonu
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendList[position]

        holder.tvName.text = friend.nickname
        holder.tvLevel.text = "Level: ${friend.targetLevel}"

        // 🔥 Mavi oka tıklandığında SocialActivity'deki fonksiyonu tetikle
        holder.btnAction.setOnClickListener {
            onChallenge(friend)
        }
    }

    override fun getItemCount() = friendList.size
}