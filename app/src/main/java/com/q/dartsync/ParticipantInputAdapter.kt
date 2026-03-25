package com.q.dartsync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView

class ParticipantInputAdapter(private var count: Int) :
    RecyclerView.Adapter<ParticipantInputAdapter.InputViewHolder>() {

    // İsimleri tutacağımız liste
    private val names = MutableList(count) { "" }

    class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val etName: EditText = view.findViewById(R.id.etParticipantNameInput)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputViewHolder {
        // item_participant_input.xml adında içinde sadece bir EditText olan basit bir layout oluşturmalısın
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant_input, parent, false)
        return InputViewHolder(view)
    }

    override fun onBindViewHolder(holder: InputViewHolder, position: Int) {
        holder.etName.hint = "${position + 1}. Oyuncu İsmi"
        holder.etName.setText(names[position])

        // Yazılan ismi anlık olarak listede güncelle
        holder.etName.addTextChangedListener {
            names[position] = it.toString()
        }
    }

    override fun getItemCount() = count

    fun updateCount(newCount: Int) {
        count = newCount
        names.clear()
        repeat(newCount) { names.add("") }
        notifyDataSetChanged()
    }

    fun getEnteredNames(): List<String> = names.filter { it.isNotBlank() }
}