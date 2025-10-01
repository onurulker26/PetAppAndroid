package com.tamer.petapp.vaccination

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemVaccinationBinding
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Vaccination
import com.tamer.petapp.model.VaccinationStatus
import java.text.SimpleDateFormat
import java.util.*

class VaccinationsAdapter(
    private val onVaccinationClick: (Vaccination) -> Unit,
    private val onEditClick: (Vaccination) -> Unit,
    private val onDeleteClick: (Vaccination) -> Unit
) : RecyclerView.Adapter<VaccinationsAdapter.VaccinationViewHolder>() {

    private val vaccinations = mutableListOf<Vaccination>()
    private val pets = mutableMapOf<String, Pet>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun updateVaccinations(newVaccinations: List<Vaccination>) {
        vaccinations.clear()
        vaccinations.addAll(newVaccinations)
        notifyDataSetChanged()
    }
    
    fun updatePets(newPets: Map<String, Pet>) {
        pets.clear()
        pets.putAll(newPets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccinationViewHolder {
        val binding = ItemVaccinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VaccinationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VaccinationViewHolder, position: Int) {
        holder.bind(vaccinations[position])
    }

    override fun getItemCount() = vaccinations.size

    inner class VaccinationViewHolder(
        private val binding: ItemVaccinationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(it, vaccinations[position])
                }
            }
        }

        private fun showPopupMenu(view: android.view.View, vaccination: Vaccination) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.menu_vaccination_options, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            onEditClick(vaccination)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(vaccination)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }

        fun bind(vaccination: Vaccination) {
            binding.apply {
                tvVaccinationName.text = vaccination.name
                tvDate.text = "Aşı Tarihi: ${dateFormat.format(vaccination.date)}"
                
                // Pet adını göster
                pets[vaccination.petId]?.let { pet ->
                    tvPetName.text = pet.name
                }

                // Sonraki aşı tarihini göster
                vaccination.nextDate?.let { nextDate ->
                    tvNextDate.text = "Sonraki: ${dateFormat.format(nextDate)}"
                }

                // Aşı durumunu göster
                tvStatus.text = when (vaccination.status) {
                    VaccinationStatus.COMPLETED -> "Tamamlandı"
                    VaccinationStatus.UPCOMING -> "Yaklaşıyor"
                    VaccinationStatus.OVERDUE -> "Gecikmiş"
                }
            }
        }
    }
} 