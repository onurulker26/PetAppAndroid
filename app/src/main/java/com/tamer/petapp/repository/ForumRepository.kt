package com.tamer.petapp.repository

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.model.ForumComment
import com.tamer.petapp.model.ForumCategory
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Date

class ForumRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ForumRepository"
    
    // Koleksiyon referansları - tutarlı isimlendirme kullan
    private val postsCollection = firestore.collection("forumPosts")
    private val commentsCollection = firestore.collection("forumComments")
    private val usersCollection = firestore.collection("users")
    
    // Post fonksiyonları
    fun getForumPosts(callback: (List<ForumPost>, Exception?) -> Unit) {
        // Önce tokeni yenile, sonra veriyi getir
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(emptyList(), null)
            return
        }
        
        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                Log.d(TAG, "Token yenilendi, forumPosts verilerini alıyorum")
                
                postsCollection
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        val postList = documents.mapNotNull { doc ->
                            try {
                                val post = doc.toObject(ForumPost::class.java)
                                post.id = doc.id
                                post
                            } catch (e: Exception) {
                                Log.e(TAG, "Post dönüştürme hatası: ${e.message}")
                                null
                            }
                        }
                        callback(postList, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Postları getirme hatası: ${e.message}")
                        callback(emptyList(), e)
                        
                        // Yetki hatası ise tokeni yenile ve tekrar dene
                        if (e.message?.contains("permission") == true || e.message?.contains("PERMISSION_DENIED") == true) {
                            Log.d(TAG, "Yetki hatası tespit edildi, yeniden deneniyor")
                            Handler(Looper.getMainLooper()).postDelayed({
                                getForumPosts(callback)
                            }, 1000)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Token yenileme hatası: ${e.message}")
                callback(emptyList(), e)
            }
    }
    
    fun getPostComments(postId: String, callback: (List<ForumComment>, Exception?) -> Unit) {
        // Önce tokeni yenile, sonra veriyi getir
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(emptyList(), null)
            return
        }
        
        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                Log.d(TAG, "Token yenilendi, forumComments verilerini alıyorum - Post ID: $postId")
                
                commentsCollection
                    .whereEqualTo("postId", postId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        val commentList = documents.mapNotNull { doc ->
                            try {
                                val comment = doc.toObject(ForumComment::class.java)
                                comment.id = doc.id
                                comment
                            } catch (e: Exception) {
                                Log.e(TAG, "Yorum dönüştürme hatası: ${e.message}")
                                null
                            }
                        }
                        callback(commentList, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Yorumları getirme hatası: ${e.message}")
                        callback(emptyList(), e)
                        
                        // Yetki hatası ise tokeni yenile ve tekrar dene
                        if (e.message?.contains("permission") == true || e.message?.contains("PERMISSION_DENIED") == true) {
                            Log.d(TAG, "Yetki hatası tespit edildi, yeniden deneniyor")
                            Handler(Looper.getMainLooper()).postDelayed({
                                getPostComments(postId, callback)
                            }, 1000)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Token yenileme hatası: ${e.message}")
                callback(emptyList(), e)
            }
    }

    // Posts koleksiyonu referansı
    private val categoriesCollection = firestore.collection("forumCategories")
    
    // Gönderileri getir
    fun getPosts(categoryId: String? = null): Flow<List<ForumPost>> = callbackFlow {
        Log.d(TAG, "getPosts çağrıldı. Kategori ID: $categoryId")
        
        try {
            // Kullanıcı girişi kontrolü
            if (auth.currentUser == null) {
                Log.w(TAG, "Kullanıcı girişi yapılmamış")
                close(Exception("Lütfen giriş yapın"))
                return@callbackFlow
            }

            var query = postsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            
            // Kategori filtresi varsa uygula
            if (categoryId != null) {
                query = query.whereEqualTo("categoryId", categoryId)
                Log.d(TAG, "Kategori filtresi uygulandı: $categoryId")
            }
            
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Gönderiler dinlenirken hata oluştu: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w(TAG, "Gönderi snapshot'ı null")
                    close()
                    return@addSnapshotListener
                }
                
                try {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            ForumPost(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                content = data["content"] as? String ?: "",
                                categoryId = data["categoryId"] as? String,
                                categoryName = data["categoryName"] as? String,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                likes = (data["likes"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                                commentCount = (data["commentCount"] as? Number)?.toInt() ?: 0,
                                imageUrl = data["imageUrl"] as? String,
                                reportedBy = (data["reportedBy"] as? List<*>)?.filterIsInstance<String>() ?: listOf()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Gönderi dönüştürme hatası. Doc ID: ${doc.id}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Gönderiler başarıyla alındı. Toplam: ${posts.size}")
                    trySend(posts)
                } catch (e: Exception) {
                    Log.e(TAG, "Gönderi listesi oluşturulurken hata", e)
                    close(e)
                }
            }
            
            // Flow kapandığında listener'ı kaldır
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getPosts başlatılırken hata", e)
            close(e)
        }
    }

    // Post ekle
    suspend fun addPost(post: ForumPost): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))
            }

            val postData = hashMapOf(
                "title" to post.title,
                "content" to post.content,
                "categoryId" to post.categoryId,
                "categoryName" to post.categoryName,
                "userId" to currentUser.uid,
                "userName" to (currentUser.displayName ?: "Anonim Kullanıcı"),
                "createdAt" to Date(),
                "updatedAt" to Date(),
                "likes" to listOf<String>(),
                "commentCount" to 0,
                "reportedBy" to listOf<String>()
            )

            val docRef = firestore.collection("forumPosts").document()
            docRef.set(postData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ForumRepository", "Gönderi eklenirken hata oluştu", e)
            Result.failure(e)
        }
    }

    // Beğeni toggle (beğen/beğenme)
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))
            }

            val postRef = postsCollection.document(postId)
            val docSnapshot = postRef.get().await()
            
            if (!docSnapshot.exists()) {
                return Result.failure(Exception("Gönderi bulunamadı"))
            }
            
            val post = docSnapshot.toObject(ForumPost::class.java)
                ?: return Result.failure(Exception("Gönderi verisi okunamadı"))
            
            // ID'yi manuel olarak atayalım
            post.id = postId

            val isLiked = post.likes.contains(currentUser.uid)
            val newLikes = if (isLiked) {
                post.likes.filter { it != currentUser.uid }
            } else {
                post.likes + currentUser.uid
            }

            // Transaction kullanarak atomik işlem yapalım
            firestore.runTransaction { transaction ->
                val latestDoc = transaction.get(postRef)
                if (latestDoc.exists()) {
                    transaction.update(postRef, "likes", newLikes)
                } else {
                    throw Exception("Gönderi silindi veya bulunamadı")
                }
            }.await()
            
            // İşlem başarılı ise yeni durumu döndür
            Result.success(!isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Beğeni durumu değiştirilirken hata oluştu", e)
            Result.failure(e)
        }
    }

    // Yorum ekle
    suspend fun addComment(postId: String, comment: ForumComment): Result<String> {
        Log.d(TAG, "addComment çağrıldı. Post ID: $postId")
        
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(IllegalStateException("Kullanıcı oturum açmamış"))
            }

            // Kullanıcı bilgilerini ayarla
            comment.userId = currentUser.uid
            comment.userName = currentUser.displayName ?: "İsimsiz Kullanıcı"
            comment.postId = postId
            comment.createdAt = Date()

            // forumComments koleksiyonuna yorum ekle
            val docRef = commentsCollection.document()
            val commentId = docRef.id
            comment.id = commentId

            docRef.set(comment).await()

            // Post için yorum sayısını güncelle
            postsCollection.document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(commentId)
        } catch (e: Exception) {
            Log.e(TAG, "Yorum eklenirken hata oluştu: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Yorumları getir
    fun getComments(postId: String): Flow<List<ForumComment>> = callbackFlow {
        val TAG = "ForumRepository"
        Log.d(TAG, "Yorumlar yükleniyor. Post ID: $postId")
        
        try {
            // forumComments koleksiyonundan verilen post ID'ye ait yorumları al
            val commentsRef = commentsCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("parentCommentId", null) // Sadece ana yorumları al
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            val listener = commentsRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Yorum dinleyicisi hatası: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            val comment = doc.toObject(ForumComment::class.java)
                            comment?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Yorum dönüştürme hatası: ${e.message}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Yorumlar başarıyla yüklendi. Toplam: ${comments.size}")
                    trySend(comments)
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getComments başlatılırken hata: ${e.message}", e)
            close(e)
        }
    }

    // Kategorileri getir
    fun getCategories(): Flow<List<ForumCategory>> = callbackFlow {
        Log.d(TAG, "getCategories çağrıldı")
        
        try {
            val query = categoriesCollection.orderBy("order")

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Kategoriler dinlenirken hata oluştu", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w(TAG, "Kategori snapshot'ı null")
                    close()
                    return@addSnapshotListener
                }
                
                try {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ForumCategory::class.java)?.apply {
                                id = doc.id
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Kategori dönüştürme hatası. Doc ID: ${doc.id}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Kategoriler başarıyla alındı. Toplam: ${categories.size}")
                    trySend(categories)
                } catch (e: Exception) {
                    Log.e(TAG, "Kategori listesi oluşturulurken hata", e)
                    close(e)
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getCategories başlatılırken hata", e)
            close(e)
        }
    }

    // Yorum beğenme/beğenmekten vazgeçme
    suspend fun toggleCommentLike(postId: String, commentId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))
            }

            // forumComments koleksiyonunu kullan
            val commentRef = commentsCollection.document(commentId)
            
            val comment = commentRef.get().await().toObject(ForumComment::class.java)
                ?: return Result.failure(Exception("Yorum bulunamadı"))
            
            val isLiked = comment.likes.contains(currentUser.uid)
            val newLikes = if (isLiked) {
                comment.likes.filter { it != currentUser.uid }
            } else {
                comment.likes + currentUser.uid
            }
            
            commentRef.update("likes", newLikes).await()
            Result.success(!isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Yorum beğenme işlemi başarısız", e)
            Result.failure(e)
        }
    }

    // Gönderilerde arama yap
    fun searchPosts(query: String): Flow<List<ForumPost>> = callbackFlow {
        val TAG = "ForumRepository"
        Log.d(TAG, "Arama yapılıyor: $query")
        
        val searchQuery = query.lowercase()
        val postsRef = firestore.collection("forumPosts")
        
        val listener = postsRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Arama dinleyicisi hatası", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val post = doc.toObject(ForumPost::class.java)
                            post?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Post dönüştürme hatası", e)
                            null
                        }
                    }.filter { post ->
                        post.title.lowercase().contains(searchQuery) ||
                        post.content.lowercase().contains(searchQuery) ||
                        post.userName.lowercase().contains(searchQuery)
                    }
                    
                    Log.d(TAG, "Arama sonuçları alındı. Toplam: ${posts.size}")
                    trySend(posts)
                }
            }
            
        awaitClose { listener.remove() }
    }

    // Kullanıcının gönderilerini getir
    fun getUserPosts(userId: String): Flow<List<ForumPost>> = callbackFlow {
        Log.d(TAG, "getUserPosts çağrıldı. Kullanıcı ID: $userId")
        
        try {
            if (auth.currentUser == null) {
                Log.w(TAG, "Kullanıcı girişi yapılmamış")
                close(Exception("Lütfen giriş yapın"))
                return@callbackFlow
            }

            val query = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Kullanıcı gönderileri dinlenirken hata oluştu: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w(TAG, "Kullanıcı gönderi snapshot'ı null")
                    close()
                    return@addSnapshotListener
                }
                
                try {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            ForumPost(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                content = data["content"] as? String ?: "",
                                categoryId = data["categoryId"] as? String,
                                categoryName = data["categoryName"] as? String,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                likes = (data["likes"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                                commentCount = (data["commentCount"] as? Number)?.toInt() ?: 0,
                                imageUrl = data["imageUrl"] as? String,
                                reportedBy = (data["reportedBy"] as? List<*>)?.filterIsInstance<String>() ?: listOf()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Gönderi dönüştürme hatası. Doc ID: ${doc.id}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Kullanıcı gönderileri başarıyla alındı. Toplam: ${posts.size}")
                    trySend(posts)
                } catch (e: Exception) {
                    Log.e(TAG, "Kullanıcı gönderi listesi oluşturulurken hata", e)
                    close(e)
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getUserPosts başlatılırken hata", e)
            close(e)
        }
    }

    // Kullanıcının beğendiği gönderileri getir
    fun getLikedPosts(userId: String): Flow<List<ForumPost>> = callbackFlow {
        Log.d(TAG, "getLikedPosts çağrıldı. Kullanıcı ID: $userId")
        
        try {
            if (auth.currentUser == null) {
                Log.w(TAG, "Kullanıcı girişi yapılmamış")
                trySend(emptyList()) // Boş liste gönderiyoruz hata mesajı yerine
                close()
                return@callbackFlow
            }

            val query = postsCollection
                .whereArrayContains("likes", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50) // Performans için limitleme ekledik
            
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Beğenilen gönderiler dinlenirken hata oluştu: ${error.message}", error)
                    trySend(emptyList()) // Hata durumunda da boş liste gönderiyoruz
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w(TAG, "Beğenilen gönderi snapshot'ı null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot.isEmpty) {
                    Log.d(TAG, "Beğenilen gönderi bulunamadı")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                try {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            ForumPost(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                content = data["content"] as? String ?: "",
                                categoryId = data["categoryId"] as? String,
                                categoryName = data["categoryName"] as? String,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate(),
                                likes = (data["likes"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                                commentCount = (data["commentCount"] as? Number)?.toInt() ?: 0,
                                imageUrl = data["imageUrl"] as? String,
                                reportedBy = (data["reportedBy"] as? List<*>)?.filterIsInstance<String>() ?: listOf()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Gönderi dönüştürme hatası. Doc ID: ${doc.id}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Beğenilen gönderiler başarıyla alındı. Toplam: ${posts.size}")
                    trySend(posts)
                } catch (e: Exception) {
                    Log.e(TAG, "Beğenilen gönderi listesi oluşturulurken hata", e)
                    trySend(emptyList()) // Hata durumunda boş liste gönder
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getLikedPosts başlatılırken hata", e)
            trySend(emptyList()) // Hata durumunda boş liste gönder
            close(e)
        }
    }

    // Yoruma cevap ekle
    suspend fun addCommentReply(postId: String, parentCommentId: String, reply: ForumComment): Result<String> {
        Log.d(TAG, "addCommentReply çağrıldı. Post ID: $postId, Parent Comment ID: $parentCommentId")
        
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(IllegalStateException("Kullanıcı oturum açmamış"))
            }

            // Kullanıcı bilgilerini ayarla
            reply.userId = currentUser.uid
            reply.userName = currentUser.displayName ?: "İsimsiz Kullanıcı"
            reply.postId = postId
            reply.parentCommentId = parentCommentId
            reply.createdAt = Date()

            // Yoruma cevap ekle (forumComments koleksiyonuna)
            val docRef = commentsCollection.document()
            val replyId = docRef.id
            reply.id = replyId
            
            // Önce cevabı kaydet
            docRef.set(reply).await()
            
            // Eğer ana yorum ID'si varsa, ana yorumun replies dizisini güncellemeye çalış
            try {
                val parentRef = commentsCollection.document(parentCommentId)
                val parentDoc = parentRef.get().await()
                
                if (parentDoc.exists()) {
                    val parentComment = parentDoc.toObject(ForumComment::class.java)
                        ?: throw Exception("Ana yorum verisi okunamadı")
                    
                    // Replies listesini güncelle
                    val newReplies = parentComment.replies + replyId
                    
                    // Ana yorumu güncelle
                    parentRef.update("replies", newReplies).await()
                    Log.d(TAG, "Ana yorumun replies listesi güncellendi: $parentCommentId")
                } else {
                    Log.w(TAG, "Ana yorum bulunamadı: $parentCommentId")
                }
            } catch (e: Exception) {
                // Ana yorumu güncelleme işlemi başarısız olsa bile cevap eklendiği için
                // işlemi başarılı sayıyoruz, sadece log tutuyoruz
                Log.w(TAG, "Ana yorumun replies listesi güncellenirken hata: ${e.message}")
            }
            
            Result.success(replyId)
        } catch (e: Exception) {
            Log.e(TAG, "Yoruma cevap eklenirken hata oluştu: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Bir yoruma ait cevapları getir
    fun getCommentReplies(postId: String, commentId: String): Flow<List<ForumComment>> = callbackFlow {
        Log.d(TAG, "getCommentReplies çağrıldı. Post ID: $postId, Comment ID: $commentId")
        
        try {
            // forumComments koleksiyonundan cevapları al
            val commentsRef = commentsCollection
                .whereEqualTo("parentCommentId", commentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
            
            val listener = commentsRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Cevap dinleyicisi hatası: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val replies = snapshot.documents.mapNotNull { doc ->
                        try {
                            val comment = doc.toObject(ForumComment::class.java)
                            comment?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Cevap dönüştürme hatası: ${e.message}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "Cevaplar başarıyla yüklendi. Toplam: ${replies.size}")
                    trySend(replies)
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "getCommentReplies başlatılırken hata: ${e.message}", e)
            close(e)
        }
    }
} 