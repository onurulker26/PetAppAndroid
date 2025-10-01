package com.tamer.petapp.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemPetBinding
import com.tamer.petapp.model.Pet
import java.text.SimpleDateFormat
import java.util.*

/**
 * PetApp uygulaması için evcil hayvan liste adaptörü
 * PetAdapter ve PetsAdapter sınıfları birleştirilmiştir
 */
class PetAdapter(
    private val onItemClick: (Pet) -> Unit,
    private val onEditClick: (Pet) -> Unit,
    private val onDeleteClick: (Pet) -> Unit
) : ListAdapter<Pet, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    private val TAG = "PetAdapter"
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Alternatif liste modu için constructor
    constructor(
        petsList: List<Pet>,
        onItemClick: (Pet) -> Unit
    ) : this(onItemClick, { }, { }) {
        submitList(petsList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = getItem(position)
        if (pet != null) {
            holder.bind(pet)
        } else {
            Log.e(TAG, "Null pet at position $position")
        }
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d(TAG, "PetAdapter item count: $count")
        return count
    }

    fun getItemAtPosition(position: Int): Pet? {
        return if (position >= 0 && position < itemCount) {
            getItem(position)
        } else {
            null
        }
    }

    inner class PetViewHolder(
        val binding: ItemPetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDeleteConfirmationDialog(binding.root.context, getItem(position))
                }
            }
        }

        fun bind(pet: Pet) {
            try {
                Log.d(TAG, "Binding pet: ${pet.name}, ID: ${pet.id}, Type: ${pet.type}")
                
                binding.apply {
                    tvPetName.text = pet.name
                    tvPetType.text = pet.type
                    tvPetBreed.text = pet.breed
                    tvPetAge.text = calculateAge(pet.birthDate)
                }
                
                // Resim yükleme işlemi
                loadPetImage(pet)
            } catch (e: Exception) {
                Log.e(TAG, "Pet binding error: ${e.message}", e)
            }
        }
        
        private fun loadPetImage(pet: Pet) {
            try {
                // Önce base64Image'ı kontrol et
                if (!pet.base64Image.isNullOrEmpty()) {
                    Log.d(TAG, "Base64 resmi mevcut, uzunluk: ${pet.base64Image.length}")
                    try {
                        // Base64 kodunu çöz
                        val imageBytes = Base64.decode(pet.base64Image, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        
                        // ImageView'a ayarla
                        binding.ivPetImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e(TAG, "Base64 resim yükleme hatası: ${e.message}", e)
                        // Hata durumunda varsayılan resmi göster
                        binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
                    }
                }
                // Sonra imageUrl'i kontrol et (geriye dönük uyumluluk)
                else if (!pet.imageUrl.isNullOrEmpty()) {
                    Log.d(TAG, "Fotoğraf URL: ${pet.imageUrl}")
                    try {
                        Glide.with(binding.root.context)
                            .load(pet.imageUrl)
                            .placeholder(R.drawable.ic_pet_placeholder)
                            .error(R.drawable.ic_pet_placeholder)
                            .centerCrop()
                            .into(binding.ivPetImage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Fotoğraf yüklenirken hata: ${e.message}", e)
                        binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
                    }
                } else {
                    Log.d(TAG, "Fotoğraf bulunamadı, placeholder gösteriliyor")
                    binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPetImage genel hata: ${e.message}", e)
                binding.ivPetImage.setImageResource(R.drawable.ic_pet_placeholder)
            }
        }

        private fun calculateAge(birthDate: Long): String {
            return try {
                if (birthDate <= 0) return "Yaş bilgisi yok"
                
                val today = Calendar.getInstance()
                val birthCal = Calendar.getInstance().apply { timeInMillis = birthDate }
                
                var years = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                val months = today.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)
                
                // Ay farkı negatifse veya aynı ayda doğum günü henüz gelmediyse yılı bir azalt
                if (months < 0 || (months == 0 && today.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH))) {
                    years--
                }
                
                when {
                    years > 0 -> "$years yaşında"
                    else -> {
                        val monthAge = today.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH) + 
                                (today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12
                        "$monthAge aylık"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Yaş hesaplama hatası: ${e.message}", e)
                "Yaş hesaplanamadı"
            }
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, pet: Pet) {
        AlertDialog.Builder(context)
            .setTitle("Evcil Hayvan Sil")
            .setMessage("${pet.name} isimli evcil hayvanınızı silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ -> onDeleteClick(pet) }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Debug için base64 resim kontrolü
    fun logImageDetails() {
        for (i in 0 until itemCount) {
            val pet = getItem(i)
            if (pet.base64Image?.isNotEmpty() == true) {
                Log.d(TAG, "Evcil hayvan '${pet.name}' - Base64 resim var: ${pet.base64Image.take(50)}...")
            } else if (pet.imageUrl?.isNotEmpty() == true) {
                Log.d(TAG, "Evcil hayvan '${pet.name}' - URL var: ${pet.imageUrl}")
            } else {
                Log.d(TAG, "Evcil hayvan '${pet.name}' - Resim yok")
            }
        }
    }
    
    private class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem == newItem
        }
    }
} 