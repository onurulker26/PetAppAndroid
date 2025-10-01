package com.tamer.petapp.admin

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tamer.petapp.R
import com.tamer.petapp.forum.ForumActivity
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.model.ForumComment
import java.util.Date

class ForumManagementFragment : Fragment() {

    private val TAG = "ForumManagementFragment"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ForumManagementAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private var currentFilter = "posts" // Varsayılan filtre (gönderiler)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forum_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewForumPosts)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        tabLayout = view.findViewById(R.id.tabLayoutForumFilter)

        setupTabLayout()
        setupRecyclerView()
        setupSwipeRefresh()
        
        // İlk yükleme
        refreshUserTokenAndLoadData()
    }

    private fun setupTabLayout() {
        // Seçenekler: Raporlanan Gönderiler, Raporlanan Yorumlar
        tabLayout.addTab(tabLayout.newTab().setText("Raporlanan Gönderiler"))
        tabLayout.addTab(tabLayout.newTab().setText("Raporlanan Yorumlar"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> currentFilter = "posts"
                    1 -> currentFilter = "comments"
                }
                refreshUserTokenAndLoadData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = ForumManagementAdapter(
            onEditClick = { post ->
                // Düzenleme işlemi için iletişim kutusu göster
                showEditDialog(post)
            },
            onDeleteClick = { post ->
                showDeleteConfirmation(post)
            },
            onApproveClick = { post ->
                approvePost(post)
            },
            onRejectClick = { post ->
                rejectPost(post)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshUserTokenAndLoadData()
        }
    }
    
    private fun refreshUserTokenAndLoadData() {
        swipeRefreshLayout.isRefreshing = true
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Tokeni yenile ve verileri yükle - daha fazla log ekle
            Log.d(TAG, "Token yenileme işlemi başlatılıyor - Kullanıcı: ${currentUser.uid}, UID: ${currentUser.uid}")
            Log.d(TAG, "Kullanıcı admin mi: ${isUserAdmin(currentUser.uid)}")
            
            currentUser.getIdToken(true)
                .addOnSuccessListener { tokenResult ->
                    Log.d(TAG, "Firebase token başarıyla yenilendi, token: ${tokenResult.token?.take(15)}...")
                    Log.d(TAG, "Token geçerlilik süresi: ${tokenResult.expirationTimestamp}")
                    Log.d(TAG, "Token claims: ${tokenResult.claims}")
                    Log.d(TAG, "Verileri yüklemeye başlıyorum")
                    
                    // Kısa bir bekleme ekle
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadForumPosts(currentFilter)
                    }, 1000)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Token yenileme hatası: ${e.message}", e)
                    swipeRefreshLayout.isRefreshing = false
                    
                    // Daha detaylı hata bilgisi
                    if (e.message?.contains("PERMISSION_DENIED") == true || 
                        e.message?.contains("Missing or insufficient permissions") == true) {
                        Toast.makeText(requireContext(), "Yetkilendirme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                        
                        // Yeniden deneme seçeneği sunan diyalog göster
                        AlertDialog.Builder(requireContext())
                            .setTitle("Yetkilendirme Hatası")
                            .setMessage("Oturum bilgileriniz yenilenirken bir sorun oluştu. Yeniden denemek ister misiniz?")
                            .setPositiveButton("Yeniden Dene") { _, _ ->
                                refreshUserTokenAndLoadData()
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                }
        } else {
            Log.e(TAG, "Oturum açmış kullanıcı bulunamadı")
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show()
            
            // Giriş sayfasına yönlendir
            requireActivity().finish()
        }
    }

    private fun loadForumPosts(filter: String) {
        val db = FirebaseFirestore.getInstance()
        
        when (filter) {
            "posts" -> {
                // Raporlanan gönderileri yüklerken log kayıtları ekle
                Log.d(TAG, "Raporlanan gönderiler yükleniyor - forumPosts koleksiyonundan")
                
                // Raporlanan gönderiler - doğrudan sorgu koleksiyondaki tüm gönderileri getirir
                db.collection("forumPosts")
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d(TAG, "forumPosts koleksiyonundan toplam ${documents.size()} gönderi alındı")
                        
                        // Her bir belgeyi debug amacıyla incele
                        documents.forEach { doc ->
                            val data = doc.data
                            val reportedByField = data["reportedBy"]
                            Log.d(TAG, "Gönderi ${doc.id}: reportedBy=${reportedByField?.javaClass?.simpleName} (${reportedByField})")
                        }
                        
                        val posts = mutableListOf<ForumPost>()
                        for (document in documents) {
                            try {
                                // Gönderi verisini manuel olarak oluşturalım
                                val data = document.data
                                
                                // reportedBy alanını kontrol edelim
                                val reportedByRaw = data["reportedBy"]
                                var reportedByList: List<String> = emptyList()
                                
                                if (reportedByRaw is List<*>) {
                                    reportedByList = reportedByRaw.filterIsInstance<String>()
                                    Log.d(TAG, "Gönderi ${document.id} için reportedBy: $reportedByList")
                                }
                                
                                // Sadece raporlanmış gönderileri ekle
                                if (reportedByList.isNotEmpty()) {
                                    val post = document.toObject(ForumPost::class.java).apply {
                                        id = document.id
                                    }
                                    
                                    Log.d(TAG, "Raporlanan gönderi bulundu: ${post.id}, rapor sayısı: ${reportedByList.size}")
                                    posts.add(post)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Forum post dönüştürme hatası: ${e.message}, ${e.stackTraceToString()}")
                            }
                        }
                        
                        // Tarihe göre manuel sıralama
                        val sortedPosts = posts.sortedByDescending { it.createdAt }
                        
                        Log.d(TAG, "Raporlanan toplam gönderi sayısı: ${posts.size}")
                        adapter.submitList(sortedPosts)
                        swipeRefreshLayout.isRefreshing = false
                        
                        // Sonuç mesajı
                        if (posts.isEmpty()) {
                            Log.d(TAG, "Raporlanmış gönderi bulunamadı")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Forum postları yüklenirken hata: ${e.message}", e)
                        swipeRefreshLayout.isRefreshing = false
                        
                        // Yetki hatası durumunda tokeni yenile
                        if (e.message?.contains("PERMISSION_DENIED") == true || 
                            e.message?.contains("Missing or insufficient permissions") == true) {
                            refreshUserTokenAndLoadData()
                        }
                    }
            }
            "comments" -> {
                // Raporlanan yorumları yüklerken log kayıtları ekle
                Log.d(TAG, "Raporlanan yorumlar yükleniyor - forumComments koleksiyonundan")
                
                // Raporlanan yorumlar - Firestore kuralına uygun sorgu
                db.collection("forumComments")
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d(TAG, "forumComments koleksiyonundan toplam ${documents.size()} yorum alındı")
                        
                        val comments = mutableListOf<ForumComment>()
                        for (document in documents) {
                            try {
                                val comment = document.toObject(ForumComment::class.java)
                                comment.id = document.id
                                
                                // Belgeyi manual olarak filtrele ve log ekle
                                val commentReportedBy = comment.reportedBy
                                val commentHasReports = commentReportedBy != null && commentReportedBy.isNotEmpty()
                                if (commentHasReports) {
                                    Log.d(TAG, "Raporlanan yorum bulundu: ${comment.id}, rapor sayısı: ${commentReportedBy?.size ?: 0}")
                                    comments.add(comment)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Yorum dönüştürme hatası: ${e.message}")
                            }
                        }
                        
                        // Tarihe göre manuel sıralama
                        val sortedComments = comments.sortedByDescending { it.createdAt }
                        
                        // Yorumları gönderi formatına dönüştür
                        val commentPosts = sortedComments.map { comment ->
                            ForumPost(
                                id = comment.id,
                                title = "Yorum: ${comment.content.take(30)}${if (comment.content.length > 30) "..." else ""}",
                                content = comment.content,
                                userId = comment.userId,
                                userName = comment.userName,
                                createdAt = comment.createdAt,
                                reportedBy = comment.reportedBy
                            )
                        }
                        
                        adapter.submitList(commentPosts)
                        swipeRefreshLayout.isRefreshing = false
                        
                        // Sonuç mesajı
                        if (comments.isEmpty()) {
                            Log.d(TAG, "Raporlanmış yorum bulunamadı")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Yorumlar yüklenirken hata: ${e.message}")
                        swipeRefreshLayout.isRefreshing = false
                        
                        // Yetki hatası durumunda tokeni yenile
                        if (e.message?.contains("PERMISSION_DENIED") == true || 
                            e.message?.contains("Missing or insufficient permissions") == true) {
                            refreshUserTokenAndLoadData()
                        }
                    }
            }
        }
    }

    private fun showDeleteConfirmation(post: ForumPost) {
        AlertDialog.Builder(requireContext())
            .setTitle("İçeriği Sil")
            .setMessage("Bu içeriği silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Sil") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deletePost(post: ForumPost) {
        if (currentFilter == "posts") {
            // Gönderi silme
            FirebaseFirestore.getInstance().collection("forumPosts")
                .document(post.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Gönderi silindi", Toast.LENGTH_SHORT).show()
                    refreshUserTokenAndLoadData() // Listeyi yenile
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gönderi silinirken hata: ${e.message}")
                    
                    // Yetki hatası durumunda tokeni yenile ve tekrar dene
                    if (e.message?.contains("PERMISSION_DENIED") == true || 
                        e.message?.contains("Missing or insufficient permissions") == true) {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                            ?.addOnSuccessListener {
                                // Kısa bir bekleme ekle
                                Handler(Looper.getMainLooper()).postDelayed({
                                    deletePost(post) // İşlemi tekrar dene
                                }, 1000)
                            }
                    }
                }
        } else {
            // Yorum silme - koleksiyon adını düzelt
            FirebaseFirestore.getInstance().collection("forumComments")
                .document(post.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Yorum silindi", Toast.LENGTH_SHORT).show()
                    refreshUserTokenAndLoadData() // Listeyi yenile
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Yorum silinirken hata: ${e.message}")
                    
                    // Yetki hatası durumunda tokeni yenile ve tekrar dene
                    if (e.message?.contains("PERMISSION_DENIED") == true || 
                        e.message?.contains("Missing or insufficient permissions") == true) {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                            ?.addOnSuccessListener {
                                deletePost(post) // İşlemi tekrar dene
                            }
                    }
                }
        }
    }

    private fun approvePost(post: ForumPost) {
        if (currentFilter == "posts") {
            // Gönderi onaylama
            FirebaseFirestore.getInstance().collection("forumPosts")
                .document(post.id)
                .update(
                    mapOf(
                        "reportedBy" to listOf<String>(),
                        "reportReason" to null
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Gönderi onaylandı, rapor kaldırıldı", Toast.LENGTH_SHORT).show()
                    refreshUserTokenAndLoadData() // Listeyi yenile
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gönderi onaylanırken hata: ${e.message}")
                    
                    // Yetki hatası durumunda tokeni yenile ve tekrar dene
                    if (e.message?.contains("PERMISSION_DENIED") == true || 
                        e.message?.contains("Missing or insufficient permissions") == true) {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                            ?.addOnSuccessListener {
                                // Kısa bir bekleme ekle
                                Handler(Looper.getMainLooper()).postDelayed({
                                    approvePost(post) // İşlemi tekrar dene
                                }, 1000)
                            }
                    }
                }
        } else {
            // Yorum onaylama - koleksiyon adını düzelt
            FirebaseFirestore.getInstance().collection("forumComments")
                .document(post.id)
                .update(
                    mapOf(
                        "reportedBy" to emptyList<String>(), // Raporları temizle
                        "reportReason" to null // Rapor sebebini temizle
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Yorum onaylandı, raporlar temizlendi", Toast.LENGTH_SHORT).show()
                    refreshUserTokenAndLoadData() // Listeyi yenile
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Yorum onaylanırken hata: ${e.message}")
                    
                    // Yetki hatası durumunda tokeni yenile ve tekrar dene
                    if (e.message?.contains("PERMISSION_DENIED") == true || 
                        e.message?.contains("Missing or insufficient permissions") == true) {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                            ?.addOnSuccessListener {
                                approvePost(post) // İşlemi tekrar dene
                            }
                    }
                }
        }
    }

    private fun rejectPost(post: ForumPost) {
        // Reddetme öncesinde onay iste
        AlertDialog.Builder(requireContext())
            .setTitle("İçeriği Reddet")
            .setMessage("Bu içeriği reddetmek istediğinizden emin misiniz? İçerik tamamen silinecektir.")
            .setPositiveButton("Reddet ve Sil") { _, _ ->
                deletePost(post) // Silme işlemini çağır
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Fragment dışından FAB tıklaması için
    fun onFabClick() {
        // Forum Management için FAB yok, bu işlevi boş bırakıyoruz
    }

    // Admin kontrolü için yardımcı fonksiyon
    private fun isUserAdmin(uid: String): Boolean {
        // Admin UID'lerini kontrol et
        return uid == "ES9Vqa1m5eODBIaa0yYYNZsBh4q1" || uid == "ADMIN_UID2"
    }

    // Düzenleme iletişim kutusunu göster
    private fun showEditDialog(post: ForumPost) {
        val editView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_forum_post, null)
        val etTitle = editView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPostTitle)
        val etContent = editView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPostContent)
        
        // Mevcut verileri göster
        etTitle.setText(post.title)
        etContent.setText(post.content)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Gönderiyi Düzenle")
            .setView(editView)
            .setPositiveButton("Kaydet") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newContent = etContent.text.toString().trim()
                
                if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                    updatePost(post, newTitle, newContent)
                } else {
                    Toast.makeText(requireContext(), "Başlık ve içerik boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    // Gönderiyi güncelle
    private fun updatePost(post: ForumPost, newTitle: String, newContent: String) {
        val db = FirebaseFirestore.getInstance()
        val collection = when (currentFilter) {
            "posts" -> "forumPosts"
            else -> "forumComments"
        }
        
        val updates = if (currentFilter == "posts") {
            mapOf(
                "title" to newTitle,
                "content" to newContent,
                "updatedAt" to Date()
            )
        } else {
            // Yorumlarda title alanı olmayabilir
            mapOf(
                "content" to newContent,
                "updatedAt" to Date()
            )
        }
        
        db.collection(collection)
            .document(post.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "İçerik güncellendi", Toast.LENGTH_SHORT).show()
                refreshUserTokenAndLoadData() // Listeyi yenile
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "İçerik güncellenirken hata: ${e.message}")
                
                // Yetki hatası durumunda tokeni yenile ve tekrar dene
                if (e.message?.contains("PERMISSION_DENIED") == true || 
                    e.message?.contains("Missing or insufficient permissions") == true) {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                        ?.addOnSuccessListener {
                            // Kısa bir bekleme ekle
                            Handler(Looper.getMainLooper()).postDelayed({
                                updatePost(post, newTitle, newContent) // İşlemi tekrar dene
                            }, 1000)
                        }
                }
            }
    }
} 