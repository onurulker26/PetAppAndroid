package com.tamer.petapp.forum

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemCommentReplyBinding
import com.tamer.petapp.model.ForumComment
import java.text.SimpleDateFormat
import java.util.Locale

class CommentReplyAdapter(
    private val onLikeClick: (String, String) -> Unit,
    private val onReplyClick: (ForumComment) -> Unit,
    private val onReportClick: (ForumComment) -> Unit = {}
) : ListAdapter<ForumComment, CommentReplyAdapter.ReplyViewHolder>(ReplyDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private var highlightedCommentId: String? = null
    
    // Yanıtlanan yorumu vurgula
    fun highlightComment(commentId: String) {
        highlightedCommentId = commentId
        notifyDataSetChanged()
    }

    // Vurgulamayı temizle
    fun clearHighlight() {
        highlightedCommentId = null
        notifyDataSetChanged()
    }

    // Yorumu güncelle
    fun updateComment(updatedComment: ForumComment) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedComment.id }
        if (index != -1) {
            currentList[index] = updatedComment
            submitList(currentList) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val binding = ItemCommentReplyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReplyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReplyViewHolder(private val binding: ItemCommentReplyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: ForumComment) {
            binding.apply {
                // Vurgulanan yorumu belirgin hale getir
                if (comment.id == highlightedCommentId) {
                    root.setBackgroundResource(com.tamer.petapp.R.color.selected_item_background)
                } else {
                    root.background = null
                }
                
                tvUserName.text = comment.userName
                tvContent.text = comment.content
                tvDate.text = comment.createdAt?.let { dateFormat.format(it) } ?: ""
                
                // Kullanıcı profil fotoğrafını yükle
                if (!comment.userId.isNullOrEmpty()) {
                    com.tamer.petapp.utils.UserPhotoManager.loadUserPhoto(
                        comment.userId,
                        ivUserPhoto,
                        R.drawable.ic_profile
                    )
                } else {
                    ivUserPhoto.setImageResource(R.drawable.ic_profile)
                }
                
                // Beğeni sayısını göster
                tvLikeCount.text = comment.likes.size.toString()
                
                // Beğeni durumunu kontrol et ve ikonu güncelle
                val isLiked = comment.likes.contains(auth.currentUser?.uid)
                btnLike.setImageResource(
                    if (isLiked) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_outline
                )
                
                // Beğeni butonuna tıklama
                btnLike.setOnClickListener {
                    // Beğeni butonunu devre dışı bırak
                    btnLike.isEnabled = false
                    
                    // Beğeni işlemini gerçekleştir
                    onLikeClick(comment.postId, comment.id)
                    
                    // 1 saniye sonra butonu tekrar aktif et
                    btnLike.postDelayed({
                        btnLike.isEnabled = true
                    }, 1000)
                }

                // Yanıtla butonuna tıklama - burada yoruma yanıt yazma işlemi gerçekleşiyor
                btnReply.setOnClickListener {
                    Log.d("CommentReplyAdapter", "Yanıta yanıt yazılıyor. ID: ${comment.id}")
                    // Burada direkt olarak yorumun kendisini gönderiyoruz, böylece
                    // yorumun yanıtına yanıt olarak kaydedilecek
                    onReplyClick(comment)
                }
                
                // Rapor butonunu göster ve işlevsellik ekle
                btnReport.visibility = View.VISIBLE
                btnReport.setOnClickListener {
                    onReportClick(comment)
                }
            }
        }
    }

    class ReplyDiffCallback : DiffUtil.ItemCallback<ForumComment>() {
        override fun areItemsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem == newItem
        }
    }
} 