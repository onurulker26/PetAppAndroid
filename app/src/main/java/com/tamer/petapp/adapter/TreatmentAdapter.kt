package com.tamer.petapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemAppointmentBinding
import com.tamer.petapp.databinding.ItemDocumentBinding
import com.tamer.petapp.databinding.ItemMedicationBinding
import com.tamer.petapp.databinding.ItemTreatmentBinding
import com.tamer.petapp.model.Medication
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Treatment
import com.tamer.petapp.model.TreatmentDocument
import com.tamer.petapp.model.TreatmentStatus
import com.tamer.petapp.model.VetAppointment
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ana tedavi listesini gösteren adapter
 */
class TreatmentAdapter(
    private val onItemClick: (Treatment) -> Unit,
    private val onEditClick: (Treatment) -> Unit,
    private val onDeleteClick: (Treatment) -> Unit
) : ListAdapter<Treatment, TreatmentAdapter.TreatmentViewHolder>(TreatmentDiffCallback()) {

    private val TAG = "TreatmentAdapter"
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Evcil hayvan isimlerini çözümlemek için kullanılacak map
    private var petsMap: Map<String, Pet> = emptyMap()
    
    // Alternatif constructor - eski TreatmentsAdapter uyumluluğu için
    constructor(
        treatments: List<Treatment>,
        petsMap: Map<String, Pet>,
        onItemClick: (Treatment) -> Unit,
        onEditClick: (Treatment) -> Unit,
        onDeleteClick: (Treatment) -> Unit
    ) : this(onItemClick, onEditClick, onDeleteClick) {
        this.petsMap = petsMap
        submitList(treatments)
    }
    
    fun updatePetsMap(newPetsMap: Map<String, Pet>) {
        petsMap = newPetsMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreatmentViewHolder {
        val binding = ItemTreatmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TreatmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TreatmentViewHolder, position: Int) {
        val treatment = getItem(position)
        holder.bind(treatment)
    }

    inner class TreatmentViewHolder(private val binding: ItemTreatmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.btnMenu.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(it, getItem(position))
                }
            }
        }
        
        private fun showPopupMenu(view: View, treatment: Treatment) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.menu_treatment_item)
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(treatment)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(treatment)
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }

        fun bind(treatment: Treatment) {
            binding.tvTreatmentName.text = treatment.name
            
            // Evcil hayvan adını göster
            val pet = petsMap[treatment.petId]
            binding.tvPetName.text = pet?.name ?: "Bilinmeyen Hayvan"
            
            // Başlangıç ve bitiş tarihlerini ayarla
            binding.tvStartDate.text = dateFormat.format(treatment.startDate)
            binding.tvEndDate.text = treatment.endDate?.let { dateFormat.format(it) } ?: "-"
            
            // İlaç, randevu ve belge sayılarını ayarla
            binding.tvMedicationCount.text = "${treatment.medications.size} ilaç"
            binding.tvAppointmentCount.text = "${treatment.vetAppointments.size} randevu"
            
            // Belge sayısını kontrol edip göster
            if (treatment.hasDocuments == true) {
                binding.tvDocumentCount.text = "Belgeler yükleniyor..."
                
                firestore.collection("users")
                    .document(auth.currentUser?.uid ?: "")
                    .collection("pets")
                    .document(treatment.petId)
                    .collection("treatments")
                    .document(treatment.id)
                    .collection("documents")
                    .get()
                    .addOnSuccessListener { documentsSnapshot ->
                        // Test belgesi filtreleniyor
                        val realDocuments = documentsSnapshot.documents.filter { 
                            !(it.id == "test_doc" || it.getBoolean("isTest") == true) 
                        }
                        
                        val count = realDocuments.size
                        val countText = when {
                            count == 0 -> "Belge yok"
                            count == 1 -> "1 adet belge var"
                            else -> "$count adet belge var"
                        }
                        
                        binding.tvDocumentCount.text = countText
                    }
                    .addOnFailureListener {
                        binding.tvDocumentCount.text = "Belge sayısı alınamadı"
                    }
            } else {
                binding.tvDocumentCount.text = "Belge yok"
            }
            
            // Tedavi durumunu ayarla
            val context = binding.root.context
            when (treatment.status) {
                TreatmentStatus.ACTIVE -> {
                    binding.chipStatus.text = context.getString(R.string.active)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.primary)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                TreatmentStatus.COMPLETED -> {
                    binding.chipStatus.text = context.getString(R.string.completed)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.success)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                TreatmentStatus.CANCELLED -> {
                    binding.chipStatus.text = context.getString(R.string.cancelled)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.error)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
            }
        }
    }
    
    private class TreatmentDiffCallback : DiffUtil.ItemCallback<Treatment>() {
        override fun areItemsTheSame(oldItem: Treatment, newItem: Treatment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Treatment, newItem: Treatment): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Tedavi detaylarında ilaçları göstermek için adapter
 */
class MedicationAdapter(
    private val medications: List<Medication>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        holder.bind(medication)
    }

    override fun getItemCount(): Int = medications.size

    inner class MedicationViewHolder(private val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }

        fun bind(medication: Medication) {
            binding.tvMedicationName.text = medication.name
            binding.tvDosage.text = medication.dosage
            binding.tvFrequency.text = medication.frequency
            
            if (medication.startDate != null) {
                binding.tvStartDate.text = dateFormat.format(medication.startDate!!)
            }
            
            if (medication.endDate != null) {
                binding.tvEndDate.text = dateFormat.format(medication.endDate!!)
            } else {
                binding.tvEndDate.text = "-"
            }
            
            // Hatırlatma saati
            if (medication.reminderTime.isNotEmpty()) {
                binding.tvReminderTime.text = medication.reminderTime
                binding.tvReminderTime.visibility = View.VISIBLE
                binding.labelReminderTime.visibility = View.VISIBLE
            } else {
                binding.tvReminderTime.visibility = View.GONE
                binding.labelReminderTime.visibility = View.GONE
            }
            
            // Notlar
            if (medication.notes.isNotEmpty()) {
                binding.tvNotes.text = medication.notes
                binding.tvNotes.visibility = View.VISIBLE
                binding.labelNotes.visibility = View.VISIBLE
            } else {
                binding.tvNotes.visibility = View.GONE
                binding.labelNotes.visibility = View.GONE
            }
        }
    }
}

/**
 * Tedavi detaylarında randevuları göstermek için adapter
 */
class AppointmentAdapter(
    private val appointments: List<VetAppointment>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount(): Int = appointments.size

    inner class AppointmentViewHolder(private val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }

        fun bind(appointment: VetAppointment) {
            binding.tvClinicName.text = appointment.clinicName
            binding.tvDate.text = dateFormat.format(appointment.date)
            
            // Durum chip
            when (appointment.status) {
                com.tamer.petapp.model.AppointmentStatus.SCHEDULED -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.scheduled)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.primary)
                }
                com.tamer.petapp.model.AppointmentStatus.COMPLETED -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.completed)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.success)
                }
                com.tamer.petapp.model.AppointmentStatus.CANCELLED -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.cancelled)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.error)
                }
            }
            
            // Notlar
            if (appointment.notes.isNotEmpty()) {
                binding.tvNotes.text = appointment.notes
                binding.tvNotes.visibility = View.VISIBLE
                binding.labelNotes.visibility = View.VISIBLE
            } else {
                binding.tvNotes.visibility = View.GONE
                binding.labelNotes.visibility = View.GONE
            }
        }
    }
}

/**
 * Tedavi detaylarında belgeleri göstermek için adapter
 */
class DocumentAdapter(
    private val documents: List<TreatmentDocument>,
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (TreatmentDocument) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]
        holder.bind(document)
    }

    override fun getItemCount(): Int = documents.size

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(documents[position])
                }
            }
            
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }

        fun bind(document: TreatmentDocument) {
            binding.tvDocumentName.text = document.name
            binding.tvDocumentType.text = document.type
            binding.tvUploadDate.text = dateFormat.format(document.uploadDate)
            
            // Dosya türüne göre icon atanabilir
            when {
                document.type.contains("pdf", ignoreCase = true) -> {
                    binding.ivDocumentIcon.setImageResource(R.drawable.ic_document)
                }
                document.type.contains("image", ignoreCase = true) -> {
                    binding.ivDocumentIcon.setImageResource(R.drawable.ic_image)
                }
                else -> {
                    binding.ivDocumentIcon.setImageResource(R.drawable.ic_document)
                }
            }
        }
    }
} 