package com.q.dartsync

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParticipantInputAdapter(private var count: Int) :
    RecyclerView.Adapter<ParticipantInputAdapter.InputViewHolder>() {

    // 🔥 İsimleri güvenli bir şekilde tutan ve veri kaybetmeyen liste
    private var names = MutableList(count) { "" }

    class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val etName: EditText = view.findViewById(R.id.etParticipantNameInput)
        // XML'inde oyuncu numarası gösteren bir TextView varsa (opsiyonel)
        // val tvLabel: TextView? = view.findViewById(R.id.tvPlayerLabel)

        var currentWatcher: TextWatcher? = null // 🔥 Eski watcher'ı temizlemek için saklıyoruz
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant_input, parent, false)
        return InputViewHolder(view)
    }

    override fun onBindViewHolder(holder: InputViewHolder, position: Int) {
        // 1. Önce üzerindeki eski dinleyiciyi temizle (Crash önleyici hamle)
        holder.currentWatcher?.let { holder.etName.removeTextChangedListener(it) }

        holder.etName.hint = "${position + 1}. Oyuncu İsmi"

        // 2. İsmi set et (Watcher yokken yapıyoruz ki döngüye girmesin)
        holder.etName.setText(names[position])

        // 3. Yeni bir dinleyici oluştur
        val newWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 🔥 Sadece geçerli pozisyondaki veriyi güncelle
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION && currentPos < names.size) {
                    names[currentPos] = s.toString()
                }
            }
        }

        // 4. Yeni dinleyiciyi bağla ve değişkene ata
        holder.etName.addTextChangedListener(newWatcher)
        holder.currentWatcher = newWatcher
    }

    override fun getItemCount() = count

    // 🔥 İsimleri silmeden sayıyı güncelleyen "Senior" fonksiyonu
    fun updateCount(newCount: Int) {
        val oldNames = names.toList()
        names = MutableList(newCount) { index ->
            if (index < oldNames.size) oldNames[index] else ""
        }
        count = newCount
        notifyDataSetChanged()
    }

    fun getEnteredNames(): List<String> = names.filter { it.isNotBlank() }.map { it.trim() }
}