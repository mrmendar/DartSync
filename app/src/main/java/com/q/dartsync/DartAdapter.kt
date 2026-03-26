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
    private val isReadOnly: Boolean = false,
    private val onWin: (Int) -> Unit = {},
    // Hibrit mod için callback (Hangi sayı, Hangi oyuncu, Yeni vuruş sayısı)
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
        // Orta sütundaki tam kelimeleri (Double, Triple...) kısaltmalara (D, T...) dönüştür
        val uiLabel = when (currentTarget.label) {
            "Double" -> "D"
            "Triple" -> "T"
            "Bull" -> "B"
            "House" -> "H"
            else -> currentTarget.label // Sayılar aynı kalır (20, 19...)
        }
        holder.textViewLabel.text = uiLabel

        // İkonları güncelle
        updateUI(holder.imageViewHit1, currentTarget.player1Hits)
        updateUI(holder.imageViewHit2, currentTarget.player2Hits)

        // --- 🛡️ 2. RENK HATASI DÜZELTMESİ (P1 vs P2) ---
        // Kapandığında (>3 vuruş) her iki hücrenin de aynı, parlak koyu yeşil saydamlığıyla vurgulanmasını sağla
        if (currentTarget.player1Hits >= 3) {
            // Sol taraftaki "çok siyah"laşma giderildi, koyu tema dostu parlak yeşil saydamlığı (#1A00D26A) uygulandı
            holder.player1Cell.setBackgroundColor(Color.parseColor("#1A00D26A"))
        } else {
            holder.player1Cell.setBackgroundResource(R.drawable.bg_cell)
        }

        if (currentTarget.player2Hits >= 3) {
            // Sağ taraftaki açık yeşil yerine P1 ile aynı tutarlı koyu yeşil uygulandı
            holder.player2Cell.setBackgroundColor(Color.parseColor("#1A00D26A"))
        } else {
            holder.player2Cell.setBackgroundResource(R.drawable.bg_cell)
        }

        // --- TIKLAMA VE SENKRONİZASYON MANTIĞI ---
        if (!isReadOnly) {
            // Oyuncu 1 Hücresi (P1)
            holder.player1Cell.setOnClickListener {
                currentTarget.player1Hits = if (currentTarget.player1Hits >= 3) 0 else currentTarget.player1Hits + 1
                notifyItemChanged(position)

                // 🔥 Senkronizasyon callback'ini tam kelime (target.label) ile tetikle (veri kaybı yok!)
                onHit(currentTarget.label, 1, currentTarget.player1Hits)
                checkWinner(1)
            }
            // Oyuncu 1 Uzun Basma (Direkt Nokta Atışı)
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
            holder.player2Cell.setOnLongClickListener {
                currentTarget.player2Hits = if (currentTarget.player2Hits == 4) 0 else 4
                notifyItemChanged(position)

                onHit(currentTarget.label, 2, currentTarget.player2Hits)
                checkWinner(2)
                true
            }
        } else {
            // Eğer salt okunur moddaysa tıklama olaylarını temizle
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