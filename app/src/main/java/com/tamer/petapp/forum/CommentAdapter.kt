package com.tamer.petapp.forum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.R
import com.tamer.petapp.model.ForumComment
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.tamer.petapp.databinding.ItemCommentBinding
import android.util.Log
import android.widget.LinearLayout

class CommentAdapter(
    private val onLikeClick: (String, String) -> Unit,
    private val onReplyClick: (ForumComment) -> Unit,
    private val onViewRepliesClick: (ForumComment) -> Unit,
    private val onReportClick: (ForumComment) -> Unit = {},
    private val replies: Map<String, List<ForumComment>> = mapOf()
) : ListAdapter<ForumComment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    companion object {
        private const val PROGRESS_REPLIES_ID = 0x1001 // Benzersiz bir id
    }

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

    // Tüm yorumları güncelle
    fun updateComments(updatedComments: List<ForumComment>) {
        submitList(updatedComments) {
            notifyDataSetChanged()
        }
    }

    // Yanıtları güncelle
    fun updateRepliesForComment(commentId: String, replyList: List<ForumComment>) {
        val currentReplies = HashMap(replies)
        currentReplies[commentId] = replyList
        notifyDataSetChanged() // Normalde daha spesifik bir notifyItemChanged kullanılabilir
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        private var replyAdapter: CommentReplyAdapter? = null
        
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

                // Yanıtla butonuna tıklama
                btnReply.setOnClickListener {
                    onReplyClick(comment)
                }

                // Rapor butonuna tıklama
                btnReport.setOnClickListener {
                    onReportClick(comment)
                }
                
                // Rapor butonunu görünür yap
                btnReport.visibility = View.VISIBLE
                
                // Yanıtları gösterme/gizleme
                setupReplies(comment)
            }
        }
        
        private fun setupReplies(comment: ForumComment) {
            val currentReplies = replies[comment.id] ?: emptyList()
            val replyCount = comment.replies.size

            // Yanıt sayısı varsa göster, yoksa tamamen gizle
            if (replyCount > 0) {
                binding.layoutReplies.visibility = View.VISIBLE
                binding.tvViewReplies.visibility = View.VISIBLE
                binding.tvViewReplies.text = "Yanıtları Göster ($replyCount)"
                binding.rvReplies.visibility = View.GONE

                // Yükleme animasyonu için bir ProgressBar ekleyelim (daha önce eklenmediyse)
                if (binding.layoutReplies.findViewById<android.widget.ProgressBar?>(PROGRESS_REPLIES_ID) == null) {
                    val progressBar = android.widget.ProgressBar(binding.root.context)
                    progressBar.id = PROGRESS_REPLIES_ID
                    progressBar.visibility = View.GONE
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(16, 8, 16, 8)
                    progressBar.layoutParams = params
                    binding.layoutReplies.addView(progressBar)
                }
                val progressBar = binding.layoutReplies.findViewById<android.widget.ProgressBar>(PROGRESS_REPLIES_ID)
                progressBar.visibility = View.GONE

                // Yanıtları göster/gizle butonu
                binding.tvViewReplies.setOnClickListener {
                    if (binding.rvReplies.visibility == View.VISIBLE) {
                        // Yanıtlar açıkken kapat
                        binding.rvReplies.visibility = View.GONE
                        binding.tvViewReplies.text = "Yanıtları Göster ($replyCount)"
                    } else {
                        // Yanıtlar kapalıyken aç
                        // Eğer yanıtlar daha önce yüklenmemişse yükleme animasyonu göster
                        if (currentReplies.isEmpty()) {
                            progressBar.visibility = View.VISIBLE
                            onViewRepliesClick(comment) // Yanıtları yükle
                        }
                        binding.rvReplies.visibility = View.VISIBLE
                        binding.tvViewReplies.text = "Yanıtları Gizle"
                    }
                }

                // Yanıt adapter'ını oluştur
                if (replyAdapter == null) {
                    replyAdapter = CommentReplyAdapter(
                        onLikeClick = onLikeClick,
                        onReplyClick = onReplyClick,
                        onReportClick = onReportClick
                    )
                    binding.rvReplies.layoutManager = LinearLayoutManager(binding.root.context)
                    binding.rvReplies.adapter = replyAdapter
                }

                // Yanıtlar yüklendiyse göster
                if (currentReplies.isNotEmpty()) {
                    replyAdapter?.submitList(currentReplies) {
                        // Yanıtlar güncellendikten sonra görünürlüğü ayarla
                        progressBar.visibility = View.GONE
                    }
                } else {
                    progressBar.visibility = View.GONE
                }
            } else {
                binding.layoutReplies.visibility = View.GONE
                binding.tvViewReplies.visibility = View.GONE
                binding.rvReplies.visibility = View.GONE
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<ForumComment>() {
        override fun areItemsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ForumComment, newItem: ForumComment): Boolean {
            return oldItem == newItem
        }
    }
} 