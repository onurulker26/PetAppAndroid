package com.tamer.petapp.forum

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.databinding.ItemUserPostBinding
import com.tamer.petapp.model.ForumPost
import java.text.SimpleDateFormat
import java.util.Locale

class UserPostsAdapter(
    private val onItemClick: (ForumPost) -> Unit
) : ListAdapter<ForumPost, UserPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // Kategorilere göre renk ataması (ForumActivity ile aynı)
    private val categoryColors = mapOf(
        "Tümü" to "#607D8B",              // Grimsi mavi
        "Genel Sohbet" to "#4CAF50",      // Yeşil
        "Beslenme" to "#FF9800",          // Turuncu
        "Sağlık" to "#2196F3",            // Mavi
        "Eğitim" to "#F44336",            // Kırmızı
        "Etkinlikler" to "#9C27B0"        // Mor
    )
    private val defaultCategoryColor = "#757575"  // Gri

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemUserPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemUserPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: ForumPost) {
            binding.apply {
                tvTitle.text = post.title
                tvContent.text = post.content
                tvCategory.text = post.categoryName ?: "Kategori Yok"
                tvDate.text = post.createdAt?.let { dateFormat.format(it) } ?: ""
                tvLikeCount.text = post.likes.size.toString()
                tvCommentCount.text = post.commentCount.toString()
                
                // Kategori rengine göre arka planı ayarla
                val categoryColor = categoryColors[post.categoryName] ?: defaultCategoryColor
                tvCategory.setBackgroundColor(android.graphics.Color.parseColor(categoryColor))
                tvCategory.setTextColor(android.graphics.Color.WHITE)
                
                // Tıklama işlemi
                cardPost.setOnClickListener {
                    onItemClick(post)
                }
            }
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