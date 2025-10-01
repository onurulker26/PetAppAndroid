package com.tamer.petapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tamer.petapp.R
import com.tamer.petapp.model.Banner

class AdminBannerAdapter : ListAdapter<Banner, AdminBannerAdapter.BannerViewHolder>(BANNER_COMPARATOR) {

    var onEditClick: ((Banner) -> Unit)? = null
    var onDeleteClick: ((Banner) -> Unit)? = null
    var onStatusChange: ((Banner, Boolean) -> Unit)? = null

    // İşlemde olan banner ID'leri
    private val pendingSwitches = HashSet<String>()
    
    // Debug amaçlı - eklenen ve silinen itemların takibi
    init {
        Log.d("AdminBannerAdapter", "Adapter oluşturuldu")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner_admin, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val banner = getItem(position)
        Log.d("AdminBannerAdapter", "Binding banner at position $position: id=${banner.id}, title=${banner.title}")
        holder.bind(banner)
    }
    
    override fun getItem(position: Int): Banner {
        return try {
            super.getItem(position)
        } catch (e: Exception) {
            Log.e("AdminBannerAdapter", "getItem hatası position=$position, mevcut liste boyutu=${currentList.size}")
            Banner(id = "", title = "Hatalı öğe", description = "", imageUrl = "", isActive = false, priority = 0)
        }
    }

    // UI'ı doğrudan güncelle - veritabanı değişikliği olmadan
    fun updateSwitchUi(bannerId: String, isActive: Boolean) {
        try {
            val currentList = currentList.toMutableList()
            val index = currentList.indexOfFirst { it.id == bannerId }
            
            if (index != -1) {
                val currentItem = currentList[index]
                // Banner'ı güncellerken tüm alanlarını koru
                val updatedBanner = currentItem.copy(isActive = isActive)
                
                Log.d("AdminBannerAdapter", "Switch UI güncelleniyor: ID=${bannerId}, title=${currentItem.title}, yeni durum=$isActive")
                
                // Eski ve yeni öğe arasındaki farkı logla
                if (currentItem != updatedBanner) {
                    Log.d("AdminBannerAdapter", "Güncelleme öncesi: $currentItem")
                    Log.d("AdminBannerAdapter", "Güncelleme sonrası: $updatedBanner")
                }
                
                currentList[index] = updatedBanner
                
                // Listeyi güncelle
                submitList(currentList) {
                    notifyItemChanged(index)
                }
            } else {
                Log.e("AdminBannerAdapter", "Banner bulunamadı: $bannerId")
            }
        } catch (e: Exception) {
            Log.e("AdminBannerAdapter", "Switch UI güncellenirken hata: ${e.message}", e)
        }
    }
    
    // Banner durumunu güncelle (veritabanı işlemi BannerManagementFragment'da yapılacak)
    fun notifySwitchClicked(banner: Banner, newState: Boolean) {
        // Eğer zaten işlemde olan bir banner varsa, işlemi atlayalım
        if (banner.id.isEmpty()) {
            Log.e("AdminBannerAdapter", "Banner ID eksik, işlem yapılamıyor")
            return
        }
        
        if (pendingSwitches.contains(banner.id)) {
            Log.d("AdminBannerAdapter", "İşlem zaten devam ediyor, atlıyoruz: ${banner.id}")
            return
        }
        
        // Switch işlemi başlıyor
        pendingSwitches.add(banner.id)
        Log.d("AdminBannerAdapter", "Switch başlatılıyor: ${banner.id} (${banner.title}) -> $newState")
        
        // Callback'i çağır - Fragment'ta işlem gerçekleştirilecek
        onStatusChange?.invoke(banner, newState)
    }
    
    // Switch işlemi tamamlandı, işlemi temizle
    fun switchProcessingCompleted(bannerId: String) {
        val removed = pendingSwitches.remove(bannerId)
        Log.d("AdminBannerAdapter", "Switch işlemi tamamlandı: $bannerId (kaldırıldı: $removed)")
    }

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.ivBannerAdmin)
        private val title: TextView = itemView.findViewById(R.id.tvBannerTitle)
        private val description: TextView = itemView.findViewById(R.id.tvBannerDesc)
        private val priority: TextView = itemView.findViewById(R.id.tvPriority)
        private val switchContainer: LinearLayout = itemView.findViewById(R.id.switchContainer)
        private val textAktif: TextView = itemView.findViewById(R.id.textAktif)
        private val switchActive: SwitchCompat = itemView.findViewById(R.id.switchActive)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(banner: Banner) {
            try {
                Log.d("AdminBannerAdapter", "Binding banner: ${banner.id}, ${banner.title}, active=${banner.isActive}")
                
                // Temel bilgileri doldur
            title.text = banner.title
            description.text = banner.description
            priority.text = "Öncelik: ${banner.priority}"

                // Görsel
            Glide.with(itemView.context)
                .load(banner.imageUrl)
                .placeholder(R.drawable.ic_pet_logo)
                .error(R.drawable.ic_pet_logo)
                .into(image)

                // Switch'in mevcut durumu - adapter değişmeden önce listener'ları temizleyelim
                switchActive.setOnCheckedChangeListener(null)
                switchContainer.setOnClickListener(null)
                
                // Switch durumunu ayarla
                switchActive.isChecked = banner.isActive
                
                // Label'ı da duruma göre ayarla
                updateSwitchLabel(banner.isActive)
                
                // İşlemdeyse container'ı devre dışı bırak
                val isProcessing = pendingSwitches.contains(banner.id)
                switchContainer.isEnabled = !isProcessing
                switchContainer.alpha = if (isProcessing) 0.5f else 1.0f
                
                // Tıklama olayı
                switchContainer.setOnClickListener {
                    if (!pendingSwitches.contains(banner.id) && !banner.id.isEmpty()) {
                        // Yeni durum - mevcut durumun tersi
                        val newState = !banner.isActive
                        
                        // Switch UI'ı hemen güncelle
                        switchActive.isChecked = newState
                        updateSwitchLabel(newState)
                        
                        // Switch işlemini başlat
                        notifySwitchClicked(banner, newState)
                        
                        // UI geri bildirimi - container'ı devre dışı bırakarak çift tıklamayı önle
                        switchContainer.isEnabled = false
                        switchContainer.alpha = 0.5f
                    } else {
                        Log.d("AdminBannerAdapter", "Switch'e tıklama yoksayıldı: ${banner.id}, işleniyor=${pendingSwitches.contains(banner.id)}, ID boş=${banner.id.isEmpty()}")
                    }
            }

                // Düzenleme ve silme butonları - Bundle ile taşınan ID'leri önle
            btnEdit.setOnClickListener {
                    if (banner.id.isNotEmpty()) {
                        Log.d("AdminBannerAdapter", "Düzenleme tıklaması: ${banner.id}, ${banner.title}")
                onEditClick?.invoke(banner)
                    }
            }

            btnDelete.setOnClickListener {
                    if (banner.id.isNotEmpty()) {
                        Log.d("AdminBannerAdapter", "Silme tıklaması: ${banner.id}, ${banner.title}")
                onDeleteClick?.invoke(banner)
            }
        }
                
            } catch (e: Exception) {
                Log.e("AdminBannerAdapter", "Banner bağlama hatası: ${e.message}", e)
            }
        }
        
        private fun updateSwitchLabel(isActive: Boolean) {
            textAktif.text = if (isActive) "Aktif" else "Pasif"
            textAktif.setTextColor(
                itemView.context.getColor(
                    if (isActive) R.color.green_700 else R.color.gray_700
                )
            )
        }
    }
    
    override fun submitList(list: List<Banner>?) {
        Log.d("AdminBannerAdapter", "Yeni liste gönderiyor, boyut: ${list?.size ?: 0}")
        if (list != null) {
            for (item in list) {
                Log.d("AdminBannerAdapter", "Liste öğesi: id=${item.id}, title=${item.title}, active=${item.isActive}")
            }
        }
        super.submitList(list)
    }

    companion object {
        private val BANNER_COMPARATOR = object : DiffUtil.ItemCallback<Banner>() {
            override fun areItemsTheSame(oldItem: Banner, newItem: Banner): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Banner, newItem: Banner): Boolean {
                return oldItem == newItem
            }
        }
    }
} 