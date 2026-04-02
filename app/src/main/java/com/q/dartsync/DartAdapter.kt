package com.q.dartsync

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DartAdapter(
    private val targetList: List<DartTarget>,
    var isReadOnly: Boolean = false, // 🔥 Değişiklik yapıldı: Artık Activity'den erişilebilir ve değiştirilebilir.
    private val onWin: (Int) -> Unit = {},
    private val onHit: (String, Int, Int) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<DartAdapter.DartViewHolder>() {

    class DartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewLabel: TextView = view.findViewById(R.id.textViewLabel)
        val imageViewHit1: ImageView = view.findViewById(R.id.imageViewHit1)
        val imageViewHit2: ImageView = view.findViewById(R.id.imageViewHit2)
        val player1Cell: View = view.findViewById(R.id.player1Cell)
        val player2Cell: View = view.findViewById(R.id.player2Cell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dart_target, parent, false)
        return DartViewHolder(view)
    }

    override fun onBindViewHolder(holder: DartViewHolder, position: Int) {
        val currentTarget = targetList[position]

        // --- 🎯 1. KISALTMALAR (UI Temsili) ---
        val uiLabel = when (currentTarget.label) {
            "Double" -> "D"
            "Triple" -> "T"
            "Bull" -> "B"
            "House" -> "H"
            else -> currentTarget.label
        }
        holder.textViewLabel.text = uiLabel

        // İkonları güncelle
        updateUI(holder.imageViewHit1, currentTarget.player1Hits)
        updateUI(holder.imageViewHit2, currentTarget.player2Hits)

        // --- 🛡️ 2. RENK AYARLARI (#1A00D26A) ---
        if (currentTarget.player1Hits >= 3) {
            holder.player1Cell.setBackgroundColor(Color.parseColor("#1A00D26A"))
        } else {
            holder.player1Cell.setBackgroundResource(R.drawable.bg_cell)
        }

        if (currentTarget.player2Hits >= 3) {
            holder.player2Cell.setBackgroundColor(Color.parseColor("#1A00D26A"))
        } else {
            holder.player2Cell.setBackgroundResource(R.drawable.bg_cell)
        }

        // --- ⚡ 3. TIKLAMA VE SENKRONİZASYON MANTIĞI ---
        if (!isReadOnly) {
            // Oyuncu 1 Hücresi (P1)
            holder.player1Cell.setOnClickListener {
                currentTarget.player1Hits = if (currentTarget.player1Hits >= 3) 0 else currentTarget.player1Hits + 1
                notifyItemChanged(position)
                onHit(currentTarget.label, 1, currentTarget.player1Hits)
                checkWinner(1)
            }
            // Oyuncu 1 Uzun Basma
            holder.player1Cell.setOnLongClickListener {
                currentTarget.player1Hits = if (currentTarget.player1Hits == 4) 0 else 4
                notifyItemChanged(position)
                onHit(currentTarget.label, 1, currentTarget.player1Hits)
                checkWinner(1)
                true
            }

            // Oyuncu 2 Hücresi (P2)
            holder.player2Cell.setOnClickListener {
                currentTarget.player2Hits = if (currentTarget.player2Hits >= 3) 0 else currentTarget.player2Hits + 1
                notifyItemChanged(position)
                onHit(currentTarget.label, 2, currentTarget.player2Hits)
                checkWinner(2)
            }
            // Oyuncu 2 Uzun Basma
            holder.player2Cell.setOnLongClickListener {
                currentTarget.player2Hits = if (currentTarget.player2Hits == 4) 0 else 4
                notifyItemChanged(position)
                onHit(currentTarget.label, 2, currentTarget.player2Hits)
                checkWinner(2)
                true
            }
        } else {
            // Salt okunur modda tıklama olaylarını temizle
            holder.player1Cell.setOnClickListener(null)
            holder.player1Cell.setOnLongClickListener(null)
            holder.player2Cell.setOnClickListener(null)
            holder.player2Cell.setOnLongClickListener(null)
        }
    }

    private fun checkWinner(playerIndex: Int) {
        val isWinner = targetList.all {
            val hits = if (playerIndex == 1) it.player1Hits else it.player2Hits
            hits >= 3
        }
        if (isWinner) onWin(playerIndex)
    }

    private fun updateUI(imageView: ImageView, hits: Int) {
        when (hits) {
            1 -> imageView.setImageResource(R.drawable.ic_dart_slash)
            2 -> imageView.setImageResource(R.drawable.ic_dart_cross)
            3 -> imageView.setImageResource(R.drawable.ic_dart_circled_cross)
            4 -> imageView.setImageResource(R.drawable.ic_dart_circle_dot)
            else -> imageView.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = targetList.size
}