package com.tamer.petapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.model.ForumComment
import com.tamer.petapp.model.ForumCategory
import com.tamer.petapp.repository.ForumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import java.util.Date
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class ForumViewModel(private val repository: ForumRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Sealed class for UI state
    sealed class UiState {
        object Loading : UiState()
        data class Success<T>(val data: T) : UiState()
        data class Error(val message: String, val throwable: Throwable? = null) : UiState()
    }

    // State flows for different data
    private val _postsState = MutableStateFlow<UiState>(UiState.Loading)
    val postsState: StateFlow<UiState> = _postsState

    private val _categoriesState = MutableStateFlow<UiState>(UiState.Loading)
    val categoriesState: StateFlow<UiState> = _categoriesState

    private val _commentsState = MutableStateFlow<UiState>(UiState.Loading)
    val commentsState: StateFlow<UiState> = _commentsState
    
    // Tüm gönderiler ve aktif kategori için state'ler
    private var allPosts: List<ForumPost> = listOf()
    private var currentCategoryId: String? = null
    private var currentSearchQuery: String? = null

    // Yanıtları saklamak için state
    private val _commentRepliesState = mutableMapOf<String, MutableStateFlow<UiState>>()
    
    // Load posts, optionally filtered by category
    fun loadPosts(categoryId: String? = null) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "loadPosts çağrıldı. Kategori ID: $categoryId")
        
        _postsState.value = UiState.Loading
        this.currentCategoryId = categoryId
        
        viewModelScope.launch {
            try {
                // Eğer aktif bir arama varsa, arama sonuçlarını kategoriye göre filtrele
                if (currentSearchQuery != null) {
                    searchPosts(currentSearchQuery!!)
                } else {
                    repository.getPosts(categoryId)
                        .catch { e ->
                            Log.e(TAG, "Gönderiler yüklenirken hata oluştu", e)
                            _postsState.value = UiState.Error(e.message ?: "Gönderi yüklenirken hata oluştu", e)
                        }
                        .collect { posts ->
                            Log.d(TAG, "Gönderiler alındı. Toplam: ${posts.size}")
                            _postsState.value = UiState.Success(posts)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gönderiler yüklenirken beklenmeyen hata", e)
                _postsState.value = UiState.Error(e.message ?: "Gönderi yüklenirken hata oluştu", e)
            }
        }
    }

    // Load categories
    fun loadCategories() {
        _categoriesState.value = UiState.Loading
        Log.d("ForumViewModel", "Kategoriler yükleniyor...")
        
        viewModelScope.launch {
            try {
                repository.getCategories()
                    .catch { e ->
                        Log.e("ForumViewModel", "Kategoriler yüklenirken hata oluştu: ${e.message}", e)
                        _categoriesState.value = UiState.Error(e.message ?: "Kategoriler yüklenirken hata oluştu", e)
                    }
                    .collect { categories ->
                        // Kategorileri sırayla sunalım
                        val sortedCategories = categories.sortedBy { it.order }
                        Log.d("ForumViewModel", "Kategoriler başarıyla yüklendi. Toplam: ${sortedCategories.size}")
                        
                        sortedCategories.forEach { category ->
                            Log.d("ForumViewModel", "Kategori: ${category.name} (ID: ${category.id}, Sıra: ${category.order})")
                        }
                        
                        _categoriesState.value = UiState.Success(sortedCategories)
                    }
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Kategoriler yüklenirken beklenmeyen hata: ${e.message}", e)
                _categoriesState.value = UiState.Error(e.message ?: "Kategoriler yüklenirken hata oluştu", e)
            }
        }
    }

    // Load comments for a post
    fun loadComments(postId: String) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Yorumlar yükleniyor. Post ID: $postId")
        
        _commentsState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repository.getComments(postId)
                    .catch { e ->
                        Log.e(TAG, "Yorumlar yüklenirken hata oluştu: ${e.message}", e)
                        
                        // CancellationException durumunu kontrol et
                        if (e is CancellationException) {
                            Log.d(TAG, "Yorum yükleme işlemi iptal edildi")
                        } else {
                            _commentsState.value = UiState.Error(e.message ?: "Yorumlar yüklenirken hata oluştu", e)
                        }
                    }
                    .collect { comments ->
                        Log.d(TAG, "Yorumlar başarıyla yüklendi. Toplam: ${comments.size}")
                        _commentsState.value = UiState.Success(comments)
                    }
            } catch (e: CancellationException) {
                // Coroutine iptal edildiğinde, bunu normal durum olarak değerlendir
                Log.d(TAG, "Yorumlar yükleme coroutine işlemi iptal edildi")
            } catch (e: Exception) {
                Log.e(TAG, "Yorumlar yüklenirken beklenmeyen hata: ${e.message}", e)
                _commentsState.value = UiState.Error(e.message ?: "Yorumlar yüklenirken hata oluştu", e)
            }
        }
    }

    // Toggle like for a post
    fun toggleLike(postId: String) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Beğeni durumu değiştiriliyor. Post ID: $postId")
        
        viewModelScope.launch {
            try {
                // Önce mevcut gönderiyi bul
                val currentPosts = (_postsState.value as? UiState.Success<*>)?.data as? List<ForumPost>
                val currentPost = currentPosts?.find { it.id == postId }
                
                if (currentPost == null) {
                    Log.e(TAG, "Gönderi bulunamadı, doğrudan repository'den çağırıyorum")
                    
                    // Gönderiyi doğrudan repository'den çağır
                    val firestore = FirebaseFirestore.getInstance()
                    val docRef = firestore.collection("forumPosts").document(postId)
                    
                    try {
                        val doc = docRef.get().await()
                        if (doc.exists()) {
                            val post = doc.toObject(ForumPost::class.java)
                            if (post != null) {
                                post.id = postId
                                
                                // Beğeni durumunu değiştir
                                repository.toggleLike(postId)
                                    .onSuccess { isLiked ->
                                        Log.d(TAG, "Beğeni durumu güncellendi. Yeni durum: $isLiked")
                                        
                                        // Gönderiyi güncelle
                                        val updatedPost = post.copy(
                                            likes = if (isLiked) {
                                                post.likes + (auth.currentUser?.uid ?: "")
                                            } else {
                                                post.likes.filter { it != auth.currentUser?.uid }
                                            }
                                        )
                                        
                                        // Tüm gönderileri yükle
                                        loadPosts(currentCategoryId)
                                    }
                                    .onFailure { e ->
                                        Log.e(TAG, "Beğeni işlemi başarısız: ${e.message}", e)
                                        _postsState.value = UiState.Error(e.message ?: "Beğeni işlemi başarısız", e)
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gönderi bulunamadı", e)
                    }
                    
                    return@launch
                }

                // Beğeni durumunu değiştir
                repository.toggleLike(postId)
                    .onSuccess { isLiked ->
                        Log.d(TAG, "Beğeni durumu güncellendi. Yeni durum: $isLiked")
                        
                        // Gönderiyi güncelle
                        val updatedPost = currentPost.copy(
                            likes = if (isLiked) {
                                currentPost.likes + (auth.currentUser?.uid ?: "")
                            } else {
                                currentPost.likes.filter { it != auth.currentUser?.uid }
                            }
                        )
                        
                        // State'i güncelle
                        _postsState.value = UiState.Success(currentPosts.map { 
                            if (it.id == postId) updatedPost else it 
                        })
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Beğeni işlemi başarısız: ${e.message}", e)
                        _postsState.value = UiState.Error(e.message ?: "Beğeni işlemi başarısız", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Beğeni işlemi sırasında beklenmeyen hata: ${e.message}", e)
                _postsState.value = UiState.Error(e.message ?: "Beğeni işlemi başarısız", e)
            }
        }
    }
    
    // Add a post
    fun addPost(post: ForumPost) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Yeni gönderi ekleniyor: ${post.title}")
        
        viewModelScope.launch {
            try {
                repository.addPost(post)
                    .onFailure { e ->
                        Log.e(TAG, "Gönderi eklenemedi: ${e.message}", e)
                    }
                    .onSuccess { postId ->
                        Log.d(TAG, "Gönderi başarıyla eklendi. ID: $postId")
                        loadPosts(currentCategoryId)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Gönderi ekleme sırasında beklenmeyen hata: ${e.message}", e)
            }
        }
    }
    
    // Add a comment
    fun addComment(postId: String, comment: ForumComment): kotlinx.coroutines.Job {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Yeni yorum ekleniyor. Post ID: $postId")
        
        return viewModelScope.launch {
            try {
                repository.addComment(postId, comment)
                    .onFailure { e ->
                        Log.e(TAG, "Yorum eklenemedi: ${e.message}", e)
                    }
                    .onSuccess { commentId ->
                        Log.d(TAG, "Yorum başarıyla eklendi. ID: $commentId")
                        loadComments(postId)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Yorum ekleme sırasında beklenmeyen hata: ${e.message}", e)
            }
        }
    }

    // Yanıtları saklamak için güncellenmiş toggleCommentLike fonksiyonu
    fun toggleCommentLike(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                // Önce mevcut yorumu bul
                val currentComments = (_commentsState.value as? UiState.Success<*>)?.data as? List<ForumComment>
                val currentComment = currentComments?.find { it.id == commentId }
                
                // Eğer ana yorumlar içinde değilse, yanıtlar içinde ara
                if (currentComment == null) {
                    // Tüm yanıt koleksiyonlarını kontrol et
                    _commentRepliesState.forEach { (parentId, stateFlow) ->
                        val state = stateFlow.value
                        if (state is UiState.Success<*>) {
                            val replies = state.data as? List<ForumComment>
                            val reply = replies?.find { it.id == commentId }
                            
                            if (reply != null) {
                                // Yanıt beğeni durumunu değiştir
                                repository.toggleCommentLike(postId, commentId)
                                    .onSuccess { isLiked ->
                                        // Reply'ı güncelle
                                        val updatedReplies = replies.map { 
                                            if (it.id == commentId) {
                                                val newLikes = if (isLiked) {
                                                    it.likes + (auth.currentUser?.uid ?: "")
                                                } else {
                                                    it.likes.filter { likeId -> likeId != auth.currentUser?.uid }
                                                }
                                                it.copy(likes = newLikes)
                                            } else {
                                                it
                                            }
                                        }
                                        _commentRepliesState[parentId]?.value = UiState.Success(updatedReplies)
                                    }
                                    .onFailure { e ->
                                        Log.e("ForumViewModel", "Yorum beğenme işlemi başarısız", e)
                                    }
                                return@launch
                            }
                        }
                    }
                    return@launch
                }

                // Beğeni durumunu değiştir
                repository.toggleCommentLike(postId, commentId)
                    .onSuccess { isLiked ->
                        // Yorumu güncelle
                        val updatedComment = currentComment.copy(
                            likes = if (isLiked) {
                                currentComment.likes + (auth.currentUser?.uid ?: "")
                            } else {
                                currentComment.likes.filter { it != auth.currentUser?.uid }
                            }
                        )
                        
                        // State'i güncelle
                        _commentsState.value = UiState.Success(currentComments.map { 
                            if (it.id == commentId) updatedComment else it 
                        })
                    }
                    .onFailure { e ->
                        Log.e("ForumViewModel", "Yorum beğenme işlemi başarısız", e)
                        _commentsState.value = UiState.Error(e.message ?: "Yorum beğenme işlemi başarısız", e)
                    }
            } catch (e: Exception) {
                Log.e("ForumViewModel", "Yorum beğenme işlemi sırasında beklenmeyen hata", e)
                _commentsState.value = UiState.Error(e.message ?: "Yorum beğenme işlemi başarısız", e)
            }
        }
    }

    // Arama fonksiyonu
    fun searchPosts(query: String) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Arama yapılıyor: $query")
        
        _postsState.value = UiState.Loading
        currentSearchQuery = query
        
        viewModelScope.launch {
            try {
                repository.searchPosts(query)
                    .catch { e ->
                        Log.e(TAG, "Arama yapılırken hata oluştu", e)
                        _postsState.value = UiState.Error(e.message ?: "Arama yapılırken hata oluştu", e)
                    }
                    .collect { posts ->
                        Log.d(TAG, "Arama sonuçları alındı. Toplam: ${posts.size}")
                        // Eğer aktif bir kategori varsa, sonuçları filtrele
                        val filteredPosts = if (currentCategoryId != null) {
                            posts.filter { it.categoryId == currentCategoryId }
                        } else {
                            posts
                        }
                        _postsState.value = UiState.Success(filteredPosts)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Arama yapılırken beklenmeyen hata", e)
                _postsState.value = UiState.Error(e.message ?: "Arama yapılırken hata oluştu", e)
            }
        }
    }

    // Arama sorgusunu temizle
    fun clearSearch() {
        currentSearchQuery = null
        loadPosts(currentCategoryId)
    }

    // Bir yoruma ait yanıtları getir
    fun getCommentReplies(postId: String, commentId: String): StateFlow<UiState> {
        if (!_commentRepliesState.containsKey(commentId)) {
            _commentRepliesState[commentId] = MutableStateFlow(UiState.Loading)
        }
        
        loadCommentReplies(postId, commentId)
        return _commentRepliesState[commentId]!!
    }
    
    // Bir yoruma ait yanıtları yükle
    private fun loadCommentReplies(postId: String, commentId: String) {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Yanıtlar yükleniyor. Post ID: $postId, Comment ID: $commentId")
        
        if (!_commentRepliesState.containsKey(commentId)) {
            _commentRepliesState[commentId] = MutableStateFlow(UiState.Loading)
        }
        
        _commentRepliesState[commentId]?.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                repository.getCommentReplies(postId, commentId)
                    .catch { e ->
                        Log.e(TAG, "Yanıtlar yüklenirken hata oluştu: ${e.message}", e)
                        
                        // CancellationException durumunu kontrol et
                        if (e is CancellationException) {
                            Log.d(TAG, "Yanıt yükleme işlemi iptal edildi")
                        } else {
                            _commentRepliesState[commentId]?.value = UiState.Error(e.message ?: "Yanıtlar yüklenirken hata oluştu", e)
                        }
                    }
                    .collect { replies ->
                        Log.d(TAG, "Yanıtlar başarıyla yüklendi. Toplam: ${replies.size}")
                        _commentRepliesState[commentId]?.value = UiState.Success(replies)
                    }
            } catch (e: CancellationException) {
                // Coroutine iptal edildiğinde, bunu normal durum olarak değerlendir
                Log.d(TAG, "Yanıt yükleme coroutine işlemi iptal edildi")
            } catch (e: Exception) {
                Log.e(TAG, "Yanıtlar yüklenirken beklenmeyen hata: ${e.message}", e)
                _commentRepliesState[commentId]?.value = UiState.Error(e.message ?: "Yanıtlar yüklenirken hata oluştu", e)
            }
        }
    }
    
    // Yoruma cevap ekle
    fun addCommentReply(postId: String, parentCommentId: String, reply: ForumComment): kotlinx.coroutines.Job {
        val TAG = "ForumViewModel"
        Log.d(TAG, "Yoruma cevap ekleniyor. Post ID: $postId, Parent Comment ID: $parentCommentId")
        
        return viewModelScope.launch {
            try {
                repository.addCommentReply(postId, parentCommentId, reply)
                    .onFailure { e ->
                        Log.e(TAG, "Yoruma cevap eklenemedi: ${e.message}", e)
                        _commentRepliesState[parentCommentId]?.value?.let { state ->
                            if (state is UiState.Success<*>) {
                                _commentRepliesState[parentCommentId]?.value = UiState.Error("Yorum eklenemedi: ${e.message}", e) 
                            }
                        }
                    }
                    .onSuccess { replyId ->
                        Log.d(TAG, "Yoruma cevap başarıyla eklendi. ID: $replyId")
                        // Yanıtları yeniden yükle
                        loadCommentReplies(postId, parentCommentId)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Yoruma cevap ekleme sırasında beklenmeyen hata: ${e.message}", e)
                _commentRepliesState[parentCommentId]?.value = UiState.Error(e.message ?: "Yoruma cevap eklenemedi", e)
            }
        }
    }
} 