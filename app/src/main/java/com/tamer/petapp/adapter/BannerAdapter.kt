package com.tamer.petapp.adapter

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.tamer.petapp.R
import com.tamer.petapp.model.Banner
import com.google.android.material.button.MaterialButton

class BannerAdapter : ListAdapter<Banner, BannerAdapter.BannerViewHolder>(DIFF_CALLBACK) {

    var onButtonClick: ((String) -> Unit)? = null
    var onBannerClick: ((Banner) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        try {
            val banner = getItem(position)
            holder.bind(banner)
        } catch (e: Exception) {
            android.util.Log.e("BannerAdapter", "Banner bind işlemi sırasında hata: ${e.message}", e)
        }
    }

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.ivBanner)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val description: TextView = itemView.findViewById(R.id.tvDescription)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.btnAction)

        fun bind(banner: Banner) {
            try {
                // Görsel yükleme
                if (!banner.imageUrl.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(banner.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(image)
                }
    
                // Başlık
                title.text = banner.title.ifEmpty { "Duyuru" }
                
                // Açıklama metnini düzenle
                val descText = if (banner.description.isNullOrEmpty()) {
                    "Detaylar için tıklayın"
                } else {
                    banner.description
                }
                
                // Açıklama metni için 3 satır ve elips ayarları
                description.maxLines = 3
                description.ellipsize = android.text.TextUtils.TruncateAt.END
                description.text = descText
                
                // Buton kontrolü
                if (!banner.buttonText.isNullOrEmpty() && !banner.buttonUrl.isNullOrEmpty()) {
                    actionButton.visibility = View.VISIBLE
                    // Buton metnini düzenle
                    actionButton.text = formatButtonText(banner.buttonText)
                    actionButton.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.buttonUrl))
                            itemView.context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("BannerAdapter", "URL açma hatası: ${e.message}")
                            Toast.makeText(itemView.context, "URL açılamıyor: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    actionButton.visibility = View.GONE
                }
    
                // Tüm banner kartına tıklama olayı
                itemView.setOnClickListener {
                    onBannerClick?.invoke(banner)
                }
            } catch (e: Exception) {
                android.util.Log.e("BannerAdapter", "Banner bind işleminde hata: ${e.message}", e)
                // Hatayı göster ama uygulamanın çökmesini engelle
                title.text = "Duyuru Yüklenemedi"
                description.text = "Duyuru içeriği gösterilemiyor"
                actionButton.visibility = View.GONE
            }
        }
        
        /**
         * Buton metnini biçimlendirir.
         * Çok uzun metinleri kısaltır.
         */
        private fun formatButtonText(text: String?): String {
            if (text.isNullOrEmpty()) return "Detaylar"
            
            return when {
                text.length > 20 -> text.substring(0, 18) + "..."
                else -> text
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Banner>() {
            override fun areItemsTheSame(oldItem: Banner, newItem: Banner): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Banner, newItem: Banner): Boolean {
                return oldItem == newItem
            }
        }
    }
} 