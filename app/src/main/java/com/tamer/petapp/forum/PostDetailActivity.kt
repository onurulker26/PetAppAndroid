package com.tamer.petapp.forum

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tamer.petapp.databinding.ActivityPostDetailBinding
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.model.ForumComment
import com.tamer.petapp.repository.ForumRepository
import com.tamer.petapp.viewmodel.ForumViewModel
import com.tamer.petapp.viewmodel.ForumViewModelFactory
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.Timestamp
import java.util.Date
import androidx.core.content.ContextCompat
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class PostDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var commentAdapter: CommentAdapter
    private val viewModel: ForumViewModel by viewModels {
        ForumViewModelFactory(
            ForumRepository()
        )
    }
    private var postId: String? = null
    private var currentPost: ForumPost? = null
    private val TAG = "PostDetailActivity"
    private val auth = FirebaseAuth.getInstance()
    private var currentReplyToComment: ForumComment? = null
    private var commentRepliesMap = mutableMapOf<String, List<ForumComment>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            // Post ID al
            postId = intent.getStringExtra("postId")
            if (postId.isNullOrEmpty()) {
                Log.e(TAG, "Post ID bulunamadı")
                Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            setupRecyclerView()
            setupCommentInput()
            observeViewModel()
            loadPostDetailsSafe()

        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası", e)
            Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        try {
            commentAdapter = CommentAdapter(
                onLikeClick = { postId, commentId ->
                    viewModel.toggleCommentLike(postId, commentId)
                },
                onReplyClick = { comment ->
                    // Yorum yanıtlama modunu aktifleştir
                    showReplyMode(comment)
                },
                onViewRepliesClick = { comment -> 
                    // Yanıtları yükle
                    loadCommentReplies(comment)
                },
                onReportClick = { comment ->
                    // Yorum raporlama işlevini çağır
                    showReportCommentDialog(comment)
                },
                replies = commentRepliesMap
            )
            binding.rvComments.apply {
                adapter = commentAdapter
                layoutManager = LinearLayoutManager(this@PostDetailActivity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupRecyclerView hatası", e)
            Toast.makeText(this, "Yorumlar yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCommentInput() {
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty() && postId != null) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    // Eğer yanıt modundaysak
                    if (currentReplyToComment != null) {
                        // Gönder butonunu devre dışı bırak (çift tıklamayı önle)
                        binding.btnSendComment.isEnabled = false
                        
                        val reply = ForumComment(
                            postId = postId!!,
                            content = commentText,
                            parentCommentId = currentReplyToComment?.id,
                            userId = currentUser.uid,
                            userName = currentUser.displayName ?: "Anonim Kullanıcı",
                            createdAt = Date()
                        )
                        
                        Log.d(TAG, "Yanıt gönderiliyor. Hedef ID: ${currentReplyToComment?.id}")
                        
                        // Yanıt ekle
                        viewModel.addCommentReply(postId!!, currentReplyToComment!!.id, reply)
                            .invokeOnCompletion { throwable ->
                                runOnUiThread {
                                    // Her durumda butonu tekrar aktif et
                                    binding.btnSendComment.isEnabled = true
                                    
                                    if (throwable != null) {
                                        // Hata durumu
                                        Log.e(TAG, "Yanıt eklenirken hata: ${throwable.message}", throwable)
                                        Snackbar.make(binding.root, "Yanıt eklenemedi: ${throwable.message}", Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(ContextCompat.getColor(this, com.tamer.petapp.R.color.colorError))
                                            .setTextColor(Color.WHITE)
                                            .show()
                                    } else {
                                        // Başarılı durum
                                        // Yanıtı ekranda hemen görmek için yanıtları yeniden yükle
                                        binding.etComment.setText("")
                                        loadCommentReplies(currentReplyToComment!!)
                                        
                                        // Yanıt modunu kapat
                                        hideReplyMode()
                                        
                                        // Kullanıcıya bilgi ver
                                        Snackbar.make(binding.root, "Yanıtınız eklendi", Snackbar.LENGTH_SHORT)
                                            .setBackgroundTint(ContextCompat.getColor(this, com.tamer.petapp.R.color.colorSuccess))
                                            .setTextColor(Color.WHITE)
                                            .show()
                                    }
                                }
                            }
                    } else {
                        // Ana yorum ekle
                        binding.btnSendComment.isEnabled = false
                        
                        val comment = ForumComment(
                            postId = postId!!,
                            content = commentText,
                            userId = currentUser.uid,
                            userName = currentUser.displayName ?: "Anonim Kullanıcı",
                            createdAt = Date()
                        )
                        
                        viewModel.addComment(postId!!, comment)
                            .invokeOnCompletion { throwable ->
                                runOnUiThread {
                                    binding.btnSendComment.isEnabled = true
                                    binding.etComment.setText("")
                                    
                                    if (throwable == null) {
                                        // Yorumları yeniden yükle
                                        viewModel.loadComments(postId!!)
                                    } else {
                                        // Hata mesajı göster
                                        Snackbar.make(binding.root, "Yorum eklenemedi: ${throwable.message}", Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(ContextCompat.getColor(this, com.tamer.petapp.R.color.colorError))
                                            .setTextColor(Color.WHITE)
                                            .show()
                                    }
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Yorum yapmak için giriş yapmalısınız", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Lütfen bir yorum yazın", Toast.LENGTH_SHORT).show()
            }
        }

        // Yanıt modu iptal butonu
        binding.btnCancelReply.setOnClickListener {
            hideReplyMode()
        }
    }

    private fun showReplyMode(comment: ForumComment) {
        currentReplyToComment = comment
        binding.replyModeLayout.visibility = View.VISIBLE
        
        // Kime yanıt yazıldığını daha net göster
        val replyInfo = if (comment.parentCommentId != null) {
            "Yanıta Yanıt Yazıyorsunuz: ${comment.userName}"
        } else {
            "Yoruma Yanıt Yazıyorsunuz: ${comment.userName}"
        }
        binding.tvReplyingTo.text = replyInfo
        
        // Yanıt yazılan yorumun içeriğini göster
        binding.tvOriginalComment.text = comment.content
        
        // Klavyeyi göster ve edit text'e odaklan
        binding.etComment.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
        
        // Yanıtlanan yorumu vurgula
        commentAdapter.highlightComment(comment.id)
        
        // Yoruma veya yanıta yanıt yazıldığını logla
        Log.d(TAG, "Yanıt yazılıyor. Yorum ID: ${comment.id}, Parent ID: ${comment.parentCommentId}")
    }

    private fun hideReplyMode() {
        currentReplyToComment = null
        binding.replyModeLayout.visibility = View.GONE
        binding.etComment.hint = "Yorumunuzu yazın..."
        
        // Vurgulamayı kaldır
        commentAdapter.clearHighlight()
    }

    private fun loadCommentReplies(comment: ForumComment) {
        if (postId != null) {
            val job = lifecycleScope.launch {
                try {
                    viewModel.getCommentReplies(postId!!, comment.id).collect { state ->
                        // CancellationException try-catch bloğu içinde yakalanacak
                        // İşleme devam et, herhangi bir isActive kontrolüne gerek yok
                        when (state) {
                            is ForumViewModel.UiState.Loading -> {
                                // Yükleniyor gösterebiliriz
                                Log.d(TAG, "Yanıtlar yükleniyor: ${comment.id}")
                            }
                            is ForumViewModel.UiState.Success<*> -> {
                                val replies = (state.data as? List<*>)?.filterIsInstance<ForumComment>() ?: emptyList()
                                Log.d(TAG, "Yanıtlar yüklendi: ${replies.size} adet yanıt")
                                
                                // İşlemi yapmadan önce Activity'nin hala aktif olduğundan emin olalım
                                if (!isFinishing && !isDestroyed) {
                                    commentRepliesMap[comment.id] = replies
                                    commentAdapter.updateRepliesForComment(comment.id, replies)
                                    
                                    // Yanıt yoksa kullanıcıyı bilgilendir
                                    if (replies.isEmpty()) {
                                        Toast.makeText(this@PostDetailActivity, "Bu yoruma henüz yanıt verilmemiş", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            is ForumViewModel.UiState.Error -> {
                                Log.e(TAG, "Yanıtlar yüklenemedi: ${state.message}", state.throwable)
                                
                                // Job iptal hatalarını filtreleyelim - bu normal bir durum, kullanıcıya hata göstermeye gerek yok
                                // if (state.throwable !is CancellationException && !isFinishing && !isDestroyed) {
                                //     Toast.makeText(this@PostDetailActivity, "Yanıtlar yüklenemedi: ${state.message}", Toast.LENGTH_SHORT).show()
                                // }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    // Coroutine iptal edildiğinde bu beklenen bir durum, hatayı yut ve loglama yap
                    Log.d(TAG, "Yanıt yükleme işlemi iptal edildi: ${comment.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Yanıt yükleme sırasında beklenmeyen hata: ${e.message}", e)
                    
                    // Aktivite hala aktifse kullanıcıya bilgi ver
                    // if (!isFinishing && !isDestroyed) {
                    //     Toast.makeText(this@PostDetailActivity, "Yanıtlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    // }
                }
            }
        }
    }

    private fun observeViewModel() {
        // Gönderiler için state flow
        lifecycleScope.launch {
            // Lifecycle'a bağlı olarak çalışması için repeatOnLifecycle kullan
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.postsState.collect { state ->
                        // CancellationException try-catch içinde yakalanacak
                        // isActive kontrolüne gerek yok
                        when (state) {
                            is ForumViewModel.UiState.Success<*> -> {
                                val posts = (state.data as? List<*>)?.filterIsInstance<ForumPost>()
                                posts?.find { it.id == postId }?.let { post ->
                                    // İşlemi yapmadan önce Activity'nin hala aktif olduğundan emin olalım
                                    if (!isFinishing && !isDestroyed) {
                                        currentPost = post
                                        updateLikeUI(post)
                                    }
                                }
                            }
                            is ForumViewModel.UiState.Error -> {
                                // Job iptal hatalarını filtreleyelim
                                // if (state.throwable !is CancellationException && !isFinishing && !isDestroyed) {
                                //     Toast.makeText(this@PostDetailActivity, "Beğeni işlemi sırasında bir hata oluştu: ${state.message}", Toast.LENGTH_SHORT).show()
                                // }
                            }
                            else -> {}
                        }
                    }
                } catch (e: CancellationException) {
                    // Beklenen bir durum, sadece loglama yap
                    Log.d(TAG, "Gönderi state izleme işlemi iptal edildi")
                } catch (e: Exception) {
                    Log.e(TAG, "Gönderi state izlenirken beklenmeyen hata: ${e.message}", e)
                }
            }
        }

        // Yorumlar için state flow
        lifecycleScope.launch {
            // Lifecycle'a bağlı olarak çalışması için repeatOnLifecycle kullan
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.commentsState.collect { state ->
                        // CancellationException try-catch içinde yakalanacak
                        // isActive kontrolüne gerek yok
                        when (state) {
                            is ForumViewModel.UiState.Loading -> {
                                binding.progressBarComments.visibility = View.VISIBLE
                                binding.rvComments.visibility = View.GONE
                            }
                            is ForumViewModel.UiState.Success<*> -> {
                                val comments = (state.data as? List<*>)?.filterIsInstance<ForumComment>() ?: emptyList()
                                Log.d(TAG, "Yorumlar yüklendi: ${comments.size} adet yorum")
                                
                                if (!isFinishing && !isDestroyed) {
                                    commentAdapter.submitList(comments) {
                                        // Liste güncellendikten sonra UI'ı güncelle
                                        commentAdapter.notifyDataSetChanged()
                                    }
                                    
                                    binding.progressBarComments.visibility = View.GONE
                                    binding.rvComments.visibility = View.VISIBLE
                                    
                                    // Yorum yoksa mesaj göster
                                    if (comments.isEmpty()) {
                                        binding.tvNoComments.visibility = View.VISIBLE
                                    } else {
                                        binding.tvNoComments.visibility = View.GONE
                                    }
                                }
                            }
                            is ForumViewModel.UiState.Error -> {
                                binding.progressBarComments.visibility = View.GONE
                                binding.rvComments.visibility = View.VISIBLE
                                
                                // Job iptal hatalarını filtreleyelim
                                // if (state.throwable !is CancellationException && !isFinishing && !isDestroyed) {
                                //     Toast.makeText(this@PostDetailActivity, "Yorumlar yüklenemedi: ${state.message}", Toast.LENGTH_SHORT).show()
                                // }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    // Beklenen bir durum, sadece loglama yap
                    Log.d(TAG, "Yorumlar yükleme işlemi iptal edildi")
                } catch (e: Exception) {
                    // Beklenmeyen hatalar için
                    Log.e(TAG, "Yorumlar yüklenirken beklenmeyen hata: ${e.message}", e)
                    
                    // Aktivite hala aktifse kullanıcıya bilgi ver
                    // if (!isFinishing && !isDestroyed) {
                    //     Toast.makeText(this@PostDetailActivity, "Yorumlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    // }
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun loadPostDetailsSafe() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            binding.progressBarPost.visibility = View.VISIBLE
            binding.postContentLayout.visibility = View.GONE

            firestore.collection("forumPosts").document(postId!!).get()
                .addOnSuccessListener { doc ->
                    try {
                        if (doc.exists()) {
                            val post = doc.toObject(ForumPost::class.java)
                            if (post != null) {
                                post.id = postId!! // ID'yi elle atayalım
                                currentPost = post
                                binding.postContentLayout.visibility = View.VISIBLE
                                binding.tvPostTitle.text = post.title
                                binding.tvPostContent.text = post.content
                                binding.tvPostAuthor.text = post.userName
                                binding.chipCategoryDetail.text = post.categoryName
                                binding.chipCategoryDetail.chipBackgroundColor = com.google.android.material.color.MaterialColors.getColorStateListOrNull(binding.chipCategoryDetail.context, com.tamer.petapp.R.color.purple_700)
                                val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                binding.tvPostDate.text = post.createdAt?.let { dateFormat.format(it) } ?: ""
                                
                                // Profil fotoğrafını yükle
                                if (!post.userId.isNullOrEmpty()) {
                                    com.tamer.petapp.utils.UserPhotoManager.loadUserPhoto(
                                        post.userId,
                                        binding.ivProfileDetail,
                                        com.tamer.petapp.R.drawable.ic_profile
                                    )
                                    
                                    Log.d(TAG, "Kullanıcı profil fotoğrafı yükleniyor: ${post.userId}")
                                } else {
                                    binding.ivProfileDetail.setImageResource(com.tamer.petapp.R.drawable.ic_profile)
                                    Log.d(TAG, "Kullanıcı ID'si boş, varsayılan profil fotoğrafı gösteriliyor")
                                }
                                
                                // Beğeni sayısını ve durumunu güncelle
                                updateLikeUI(post)
                                
                                // Beğeni butonuna tıklama
                                setupLikeButton(post)
                                
                                // Yorumları yükle
                                viewModel.loadComments(postId!!)
                            } else {
                                Log.e(TAG, "Post null olarak döndü")
                                Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } else {
                            Log.e(TAG, "Post dökümanı bulunamadı")
                            Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Post dönüştürme hatası", e)
                        Toast.makeText(this, "Gönderi yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                        finish()
                    } finally {
                        binding.progressBarPost.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore getirme hatası", e)
                    Toast.makeText(this, "Gönderi yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    binding.progressBarPost.visibility = View.GONE
                    finish()
                }
        } catch (e: Exception) {
            Log.e(TAG, "loadPostDetailsSafe hatası", e)
            Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBarPost.visibility = View.GONE
            finish()
        }
    }

    private fun updateLikeUI(post: ForumPost) {
        val isLiked = post.likes.contains(auth.currentUser?.uid)
        binding.tvPostLikeCount.text = post.likes.size.toString()
        binding.btnLikePost.setColorFilter(
            if (isLiked) ContextCompat.getColor(this, com.tamer.petapp.R.color.purple_700)
            else ContextCompat.getColor(this, android.R.color.darker_gray)
        )
    }

    private fun setupLikeButton(post: ForumPost) {
        binding.btnLikePost.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Butonu devre dışı bırak
                binding.btnLikePost.isEnabled = false
                
                // UI'ı güncelle (geçici olarak)
                val isLiked = post.likes.contains(currentUser.uid)
                val newLikes = if (isLiked) {
                    post.likes.filter { it != currentUser.uid }
                } else {
                    post.likes + currentUser.uid
                }
                
                // Beğeni durumunu güncelle
                viewModel.toggleLike(post.id)
                
                // 1 saniye sonra butonu tekrar aktif et
                binding.btnLikePost.postDelayed({
                    binding.btnLikePost.isEnabled = true
                }, 1000)
            } else {
                Toast.makeText(this@PostDetailActivity, "Beğenmek için giriş yapmalısınız", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReportCommentDialog(comment: ForumComment) {
        val reportReasons = arrayOf(
            "Uygunsuz içerik",
            "Rahatsız edici dil",
            "Spam veya reklam",
            "Yanlış bilgi",
            "Diğer"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Yorumu Rapor Et")
            .setItems(reportReasons) { _, which ->
                reportComment(comment.id, reportReasons[which])
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun reportComment(commentId: String, reason: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Rapor etmek için giriş yapmalısınız", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Yorum raporlama başlatıldı. Yorum ID: $commentId, Kullanıcı: ${currentUser.uid}")
        
        // Önce token yenilenmesi
        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                Log.d(TAG, "Token başarıyla yenilendi. Raporlama devam ediyor...")
                
                // forumComments koleksiyonunu kullan - önceki "comments" yerine
                val commentRef = FirebaseFirestore.getInstance().collection("forumComments").document(commentId)
                
                commentRef.get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            Toast.makeText(this, "Yorum bulunamadı", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        
                        // Rapor bilgilerini hazırla
                        // Güvenli casting işlemi yapalım
                        val reportedByField = document.get("reportedBy")
                        val reportedBy = when {
                            reportedByField is ArrayList<*> -> {
                                // ArrayList içerisindeki öğelerin String olduğunu kontrol et
                                val stringList = ArrayList<String>()
                                reportedByField.forEach { item ->
                                    if (item is String) {
                                        stringList.add(item)
                                    }
                                }
                                stringList
                            }
                            reportedByField is List<*> -> {
                                // Liste içerisindeki öğelerin String olduğunu kontrol et
                                val stringList = ArrayList<String>()
                                reportedByField.forEach { item ->
                                    if (item is String) {
                                        stringList.add(item)
                                    }
                                }
                                stringList
                            }
                            else -> ArrayList<String>()
                        }
                        
                        if (reportedBy.contains(currentUser.uid)) {
                            Toast.makeText(this, "Bu yorumu zaten rapor ettiniz", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        
                        // Kullanıcı ID'sini raporlayanlar listesine ekle
                        reportedBy.add(currentUser.uid)
                        
                        // Raporlanma sebebini kaydet
                        commentRef.update(
                            mapOf(
                                "reportedBy" to reportedBy,
                                "reportReason" to reason
                            )
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Yorum başarıyla raporlandı: $commentId")
                            Toast.makeText(this, "Yorum raporlandı, teşekkürler", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Yorum raporlanırken hata: ${e.message}", e)
                            
                            if (e.message?.contains("permission") == true) {
                                Toast.makeText(this, "Yetki hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                                
                                // Tokeni yenile ve yeniden dene
                                Handler(Looper.getMainLooper()).postDelayed({
                                    reportComment(commentId, reason)
                                }, 1000)
                            } else {
                                Toast.makeText(this, "Raporlama başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Yorum alınırken hata: ${e.message}", e)
                        Toast.makeText(this, "Yorum alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Token yenileme hatası: ${e.message}", e)
                Toast.makeText(this, "Yetkilendirme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @Deprecated("Replaced by onBackPressedDispatcher.onBackPressed()", level = DeprecationLevel.WARNING)
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 