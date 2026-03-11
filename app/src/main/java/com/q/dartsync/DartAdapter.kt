package com.q.dartsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onWin parametresi ekledik: Kazananın ismini Activity'ye fırlatacak
class DartAdapter(
    private val targetList: List<DartTarget>,
    private val onWin: (Int) -> Unit
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
        holder.textViewLabel.text = currentTarget.label

        updateUI(holder.imageViewHit1, currentTarget.player1Hits)
        updateUI(holder.imageViewHit2, currentTarget.player2Hits)

        // Oyuncu 1 Etkileşimleri
        holder.player1Cell.setOnClickListener {
            currentTarget.player1Hits = if (currentTarget.player1Hits >= 3) 0 else currentTarget.player1Hits + 1
            notifyItemChanged(position)
            checkWinner(1)
        }
        holder.player1Cell.setOnLongClickListener {
            currentTarget.player1Hits = if (currentTarget.player1Hits == 4) 0 else 4
            notifyItemChanged(position)
            checkWinner(1)
            true
        }

        // Oyuncu 2 Etkileşimleri
        holder.player2Cell.setOnClickListener {
            currentTarget.player2Hits = if (currentTarget.player2Hits >= 3) 0 else currentTarget.player2Hits + 1
            notifyItemChanged(position)
            checkWinner(2)
        }
        holder.player2Cell.setOnLongClickListener {
            currentTarget.player2Hits = if (currentTarget.player2Hits == 4) 0 else 4
            notifyItemChanged(position)
            checkWinner(2)
            true
        }
    }

    // KAZANMA KONTROLÜ
    private fun checkWinner(playerIndex: Int) {
        val isWinner = targetList.all {
            val hits = if (playerIndex == 1) it.player1Hits else it.player2Hits
            hits == 3 || hits == 4 // Senin istediğin o kritik mantık burada
        }

        if (isWinner) {
            onWin(playerIndex)
        }
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