package com.tamer.petapp.adapter

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemVetClinicBinding
import com.tamer.petapp.model.VetClinic

// Klinik etkileşimlerini yönetmek için arayüz
interface VetClinicClickListener {
    fun onClinicSelected(clinic: VetClinic)
    fun onDirectionsRequested(clinic: VetClinic)
    fun onCallRequested(clinic: VetClinic)
}

class VetClinicAdapter(
    private val vetClinics: MutableList<VetClinic> = mutableListOf(),
    private val onClinicClicked: (VetClinic) -> Unit,
    private val onDirectionsClicked: (VetClinic) -> Unit,
    private val onCallClicked: (VetClinic) -> Unit
) : RecyclerView.Adapter<VetClinicAdapter.VetClinicViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun updateClinics(newClinics: List<VetClinic>) {
        vetClinics.clear()
        vetClinics.addAll(newClinics)
        notifyDataSetChanged()
    }

    fun selectClinic(clinic: VetClinic) {
        val newPosition = vetClinics.indexOfFirst { it.id == clinic.id }
        val oldPosition = selectedPosition
        
        if (newPosition != oldPosition) {
            selectedPosition = newPosition
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }
            if (newPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(newPosition)
            }
        }
    }

    fun setSelectedClinic(clinicId: String?) {
        val oldPosition = selectedPosition
        selectedPosition = if (clinicId == null) {
            RecyclerView.NO_POSITION
        } else {
            vetClinics.indexOfFirst { it.id == clinicId }
        }
        
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition)
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition)
        }
    }

    fun submitList(clinics: List<VetClinic>) {
        updateClinics(clinics)
    }

    fun getSelectedClinic(): VetClinic? {
        return if (selectedPosition != RecyclerView.NO_POSITION && 
                  selectedPosition < vetClinics.size) {
            vetClinics[selectedPosition]
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VetClinicViewHolder {
        val binding = ItemVetClinicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VetClinicViewHolder(binding, object : VetClinicClickListener {
            override fun onClinicSelected(clinic: VetClinic) {
                onClinicClicked(clinic)
            }

            override fun onDirectionsRequested(clinic: VetClinic) {
                onDirectionsClicked(clinic)
            }

            override fun onCallRequested(clinic: VetClinic) {
                onCallClicked(clinic)
            }
        })
    }

    override fun onBindViewHolder(holder: VetClinicViewHolder, position: Int) {
        val clinic = vetClinics[position]
        holder.bind(clinic, position == selectedPosition)
    }

    override fun getItemCount(): Int = vetClinics.size

    class VetClinicViewHolder(
        private val binding: ItemVetClinicBinding,
        private val clickListener: VetClinicClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(clinic: VetClinic, isSelected: Boolean) {
            binding.apply {
                // Seçili öğe görünümünü ayarla
                if (isSelected) {
                    // Seçili öğe için daha belirgin stil
                    viewSelectedIndicator.visibility = View.VISIBLE
                    root.setBackgroundResource(R.color.selected_item_background)
                    tvClinicName.setTextColor(Color.parseColor("#4CAF50"))
                    tvClinicName.setTypeface(tvClinicName.typeface, Typeface.BOLD)
                    tvClinicName.textSize = 17f
                    root.elevation = 8f
                } else {
                    // Normal öğeler için varsayılan stil
                    viewSelectedIndicator.visibility = View.INVISIBLE
                    root.setBackgroundColor(Color.WHITE)
                    tvClinicName.setTextColor(Color.parseColor("#212121"))
                    tvClinicName.setTypeface(tvClinicName.typeface, Typeface.BOLD)
                    tvClinicName.textSize = 16f
                    root.elevation = 2f
                }
                
                // Temel bilgilerin ayarlanması
                tvClinicName.text = clinic.name
                tvClinicAddress.text = clinic.address
                
                // Mesafe formatını ayarla
                val distance = clinic.distance.toInt()
                tvClinicDistance.text = if (distance < 1000) {
                    "$distance m"
                } else {
                    String.format("%.1f km", distance / 1000.0)
                }
                
                // Değerlendirme bilgisini ayarla
                if (clinic.rating > 0) {
                    tvClinicRating.text = String.format("%.1f ★", clinic.rating)
                } else {
                    tvClinicRating.text = "Değerlendirilmemiş"
                }
                
                // Çalışma saatleri bilgisini ayarla
                if (clinic.isOpen) {
                    tvOpeningHours.text = "Şu anda açık"
                    tvOpeningHours.setTextColor(Color.parseColor("#4CAF50")) // Yeşil
                } else {
                    tvOpeningHours.text = clinic.openingHours
                    tvOpeningHours.setTextColor(Color.parseColor("#F44336")) // Kırmızı
                }
                
                // Tıklama olaylarını ayarla
                root.setOnClickListener {
                    clickListener.onClinicSelected(clinic)
                }
                
                // Yol tarifi butonu tıklama olayı
                btnDirections.setOnClickListener {
                    clickListener.onDirectionsRequested(clinic)
                }
                
                // Arama butonu tıklama olayı
                btnCall.setOnClickListener {
                    clickListener.onCallRequested(clinic)
                }
            }
        }
    }
} 