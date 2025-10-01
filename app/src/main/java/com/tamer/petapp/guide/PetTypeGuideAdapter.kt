package com.tamer.petapp.guide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.R

class PetTypeGuideAdapter(
    private val petTypes: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PetTypeGuideAdapter.PetTypeViewHolder>() {

    class PetTypeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val petTypeText: TextView = view.findViewById(R.id.tvPetType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_type_guide, parent, false)
        return PetTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetTypeViewHolder, position: Int) {
        val petType = petTypes[position]
        holder.petTypeText.text = petType
        holder.itemView.setOnClickListener { onItemClick(petType) }
    }

    override fun getItemCount() = petTypes.size
} 