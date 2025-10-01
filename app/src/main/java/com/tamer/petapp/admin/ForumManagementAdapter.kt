package com.tamer.petapp.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.tamer.petapp.R
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.utils.UserPhotoManager
import java.text.SimpleDateFormat
import java.util.Locale

class ForumManagementAdapter(
    private val onEditClick: (ForumPost) -> Unit,
    private val onDeleteClick: (ForumPost) -> Unit,
    private val onApproveClick: (ForumPost) -> Unit,
    private val onRejectClick: (ForumPost) -> Unit
) : ListAdapter<ForumPost, ForumManagementAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forum_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val chipCategory: Chip = itemView.findViewById(R.id.chipCategory)
        private val ivUserPhoto: android.widget.ImageView = itemView.findViewById(R.id.ivUserPhoto)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val chipReported: Chip = itemView.findViewById(R.id.chipReported)
        private val chipPending: Chip = itemView.findViewById(R.id.chipPending)
        private val tvReportReason: TextView = itemView.findViewById(R.id.tvReportReason)

        fun bind(post: ForumPost) {
            tvTitle.text = post.title
            tvContent.text = post.content
            tvAuthor.text = post.userName
            tvDate.text = post.createdAt?.let { dateFormat.format(it) } ?: "Tarih Yok"
            chipCategory.text = post.categoryName ?: "Genel"

            // Kullanıcı profil fotoğrafı
            if (!post.userId.isNullOrBlank()) {
                UserPhotoManager.loadUserPhoto(post.userId, ivUserPhoto)
            }

            // Gönderi durumu etiketleri
            val isReported = post.reportedBy.isNotEmpty()
            
            chipReported.visibility = if (isReported) View.VISIBLE else View.GONE
            chipPending.visibility = View.GONE // Artık kullanılmıyor
            
            // Raporlanma sayısı ve sebebi
            if (isReported) {
                chipReported.text = "Raporlandı (${post.reportedBy.size})"
                
                // Rapor sebebini göster
                if (post.reportReason != null) {
                    tvReportReason.visibility = View.VISIBLE
                    tvReportReason.text = "Rapor Sebebi: ${post.reportReason}"
                } else {
                    tvReportReason.visibility = View.GONE
                }
            } else {
                tvReportReason.visibility = View.GONE
            }

            // İşlem butonları
            btnEdit.setOnClickListener { onEditClick(post) }
            btnDelete.setOnClickListener { onDeleteClick(post) }
            btnApprove.setOnClickListener { onApproveClick(post) }
            btnReject.setOnClickListener { onRejectClick(post) }
            
            // Onay butonları sadece raporlanan gönderilerde görünsün
            val showApproveButtons = isReported
            btnApprove.visibility = if (showApproveButtons) View.VISIBLE else View.GONE
            btnReject.visibility = if (showApproveButtons) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ForumPost>() {
            override fun areItemsTheSame(oldItem: ForumPost, newItem: ForumPost): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ForumPost, newItem: ForumPost): Boolean {
                return oldItem == newItem
            }
        }
    }
} 