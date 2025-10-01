package com.tamer.petapp.forum

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.tamer.petapp.R
import com.tamer.petapp.model.ForumPost
import java.text.SimpleDateFormat
import java.util.Locale

class ForumAdapter(
    private val onItemClick: (ForumPost) -> Unit,
    private val onLikeClick: (ForumPost) -> Unit,
    private val onLongClick: ((ForumPost) -> Boolean)? = null
) : ListAdapter<ForumPost, ForumAdapter.PostViewHolder>(PostDiffCallback()) {

    // Kategorilere göre renk ataması
    private val categoryColors = mapOf(
        "Genel Sohbet" to "#4CAF50",      // Yeşil
        "Beslenme" to "#FF9800",          // Turuncu
        "Sağlık" to "#2196F3",            // Mavi
        "Eğitim" to "#F44336",            // Kırmızı
        "Etkinlikler" to "#9C27B0"        // Mor
    )

    // Varsayılan kategori rengi
    private val defaultCategoryColor = "#757575"  // Gri

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forum_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }
        
        // Uzun tıklama dinleyicisi
        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(post)
            }
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun bind(post: ForumPost) {
            tvTitle.text = post.title
            tvContent.text = post.content
            
            // Kategori göstergesini ayarla
            chipCategory.text = post.categoryName
            val categoryColor = categoryColors[post.categoryName] ?: defaultCategoryColor
            chipCategory.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(categoryColor))
            chipCategory.setTextColor(Color.WHITE)

            // Kullanıcı adı doğrudan gösterilecek
            tvAuthor.text = post.userName

            // Profil fotoğrafını UserPhotoManager ile yükle
            val ivProfile = itemView.findViewById<android.widget.ImageView>(R.id.ivProfile)
            if (!post.userId.isNullOrBlank()) {
                com.tamer.petapp.utils.UserPhotoManager.loadUserPhoto(post.userId, ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile)
            }

            tvDate.text = post.createdAt?.let { dateFormat.format(it) }
            tvLikeCount.text = post.likes.size.toString()
            tvCommentCount.text = post.commentCount.toString()

            // Kullanıcı postu beğenmiş mi?
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val isLiked = currentUserId != null && post.likes.contains(currentUserId)
            btnLike.isSelected = isLiked
            btnLike.setColorFilter(
                if (isLiked) itemView.context.getColor(R.color.purple_700)
                else Color.parseColor("#757575")
            )

            // Tıklama dinleyicileri
            btnLike.setOnClickListener { onLikeClick(post) }
            // Not: itemView onClick ve onLongClick özelliği onBindViewHolder'da ayarlanıyor
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<ForumPost>() {
        override fun areItemsTheSame(oldItem: ForumPost, newItem: ForumPost): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumPost, newItem: ForumPost): Boolean {
            return oldItem == newItem
        }
    }
} 