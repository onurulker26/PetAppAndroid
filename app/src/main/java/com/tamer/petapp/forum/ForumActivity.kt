package com.tamer.petapp.forum

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityForumBinding
import com.tamer.petapp.forum.ForumAdapter
import com.tamer.petapp.model.ForumCategory
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.repository.ForumRepository
import com.tamer.petapp.viewmodel.ForumViewModel
import com.tamer.petapp.viewmodel.ForumViewModelFactory
import kotlinx.coroutines.launch

class ForumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForumBinding
    private lateinit var forumAdapter: ForumAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentPostId: String? = null
    private val TAG = "ForumActivity"

    // Kategorilere göre renk ataması
    private val categoryColors = mapOf(
        "Tümü" to "#607D8B",              // Grimsi mavi
        "Genel Sohbet" to "#4CAF50",      // Yeşil
        "Beslenme" to "#FF9800",          // Turuncu
        "Sağlık" to "#2196F3",            // Mavi
        "Eğitim" to "#F44336",            // Kırmızı
        "Etkinlikler" to "#9C27B0"        // Mor
    )
    
    // Varsayılan kategori rengi
    private val defaultCategoryColor = "#757575"  // Gri

    private val viewModel: ForumViewModel by viewModels {
        ForumViewModelFactory(ForumRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Toolbar ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // ChipGroup görünürlüğünü ayarla
        binding.chipGroupCategories.visibility = View.VISIBLE

        // Debug: Test kategorilerini oluştur (geliştirme aşamasında)
        createTestCategoriesIfNeeded()

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Verileri yükle
        viewModel.loadCategories()
        viewModel.loadPosts()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_forum, menu)
        // Rapor etme butonu sadece post seçildiğinde aktif olmalı
        menu.findItem(R.id.action_report)?.isVisible = currentPostId != null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_my_profile -> {
                // Profil sayfasına yönlendir
                val intent = Intent(this, UserProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_report -> {
                showReportDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        forumAdapter = ForumAdapter(
            onItemClick = { post ->
                val intent = Intent(this, PostDetailActivity::class.java)
                intent.putExtra("postId", post.id)
                startActivity(intent)
            },
            onLikeClick = { post ->
                viewModel.toggleLike(post.id)
            },
            onLongClick = { post ->
                // Uzun basma işlemi: post seçildiğinde raporlama seçeneği göster
                currentPostId = post.id
                invalidateOptionsMenu() // Menüyü güncelle
                showPostActionDialog(post)
                true
            }
        )

        binding.rvPosts.apply {
            adapter = forumAdapter
            layoutManager = LinearLayoutManager(this@ForumActivity)
        }
    }

    private fun setupListeners() {
        // TAG değişkenini kaldırıyorum çünkü artık sınıf seviyesinde tanımlandı
        
        // Arama işlevselliği
        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            val searchQuery = v.text.toString().trim()
            if (searchQuery.isNotEmpty()) {
                viewModel.searchPosts(searchQuery)
            }
            true
        }

        // Arama çubuğu temizleme
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    viewModel.clearSearch()
                }
            }
        })

        // Yeni gönderi ekleme butonu
        binding.fabAddPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
        }

        // Kategori seçimi
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds.first()
                val selectedChip = group.findViewById<Chip>(checkedId)
                val categoryId = selectedChip?.tag as? String
                
                Log.d(TAG, "Kategori seçildi: ${selectedChip?.text}, ID: $categoryId")
                
                // Seçilen kategoriye göre gönderileri filtrele
                viewModel.loadPosts(categoryId)
                
                // Seçilen chip'in rengini vurgula
                for (i in 0 until group.childCount) {
                    val chip = group.getChildAt(i) as? Chip
                    chip?.let {
                        val isSelected = it.id == checkedId
                        val categoryName = it.text.toString()
                        val categoryColor = categoryColors[categoryName] ?: defaultCategoryColor
                        
                        // Seçili chip için daha koyu renk kullan
                        val color = if (isSelected) {
                            // Seçili renk için daha koyu ton
                            val colorInt = Color.parseColor(categoryColor)
                            val factor = 0.8f
                            Color.rgb(
                                (Color.red(colorInt) * factor).toInt(),
                                (Color.green(colorInt) * factor).toInt(),
                                (Color.blue(colorInt) * factor).toInt()
                            )
                        } else {
                            Color.parseColor(categoryColor)
                        }
                        
                        it.chipBackgroundColor = ColorStateList.valueOf(color)
                        it.setTextColor(Color.WHITE)
                        
                        Log.d(TAG, "Chip güncellendi: ${it.text}, Seçili: $isSelected")
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postsState.collect { state ->
                    when (state) {
                        is ForumViewModel.UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvPosts.visibility = View.GONE
                        }
                        is ForumViewModel.UiState.Success<*> -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvPosts.visibility = View.VISIBLE
                            val posts = (state.data as? List<*>)?.filterIsInstance<ForumPost>() ?: emptyList()
                            forumAdapter.submitList(posts)
                        }
                        is ForumViewModel.UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvPosts.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoriesState.collect { state ->
                    when (state) {
                        is ForumViewModel.UiState.Success<*> -> {
                            val categories = (state.data as? List<*>)?.filterIsInstance<ForumCategory>() ?: emptyList()
                            updateCategoryChips(categories)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateCategoryChips(categories: List<ForumCategory>) {
        val TAG = "ForumActivity"
        Log.d(TAG, "updateCategoryChips çağrıldı. Kategori sayısı: ${categories.size}")
        
        binding.chipGroupCategories.removeAllViews()
        binding.chipGroupCategories.visibility = View.VISIBLE
        
        // "Tümü" kategorisi
        val allChip = Chip(this).apply {
            text = "Tümü"
            isCheckable = true
            isChecked = true
            id = View.generateViewId()
            tag = null
            
            // Chip tasarımını özelleştir
            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(categoryColors["Tümü"] ?: defaultCategoryColor))
            setTextColor(Color.WHITE)
        }
        binding.chipGroupCategories.addView(allChip)
        Log.d(TAG, "Tümü chip eklendi")
        
        // Diğer kategoriler
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category.name
                isCheckable = true
                id = View.generateViewId()
                tag = category.id
                
                // Chip tasarımını özelleştir
                val categoryColor = categoryColors[category.name] ?: defaultCategoryColor
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(categoryColor))
                setTextColor(Color.WHITE)
            }
            binding.chipGroupCategories.addView(chip)
            Log.d(TAG, "Kategori chip eklendi: ${category.name} (ID: ${category.id})")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun createTestCategoriesIfNeeded() {
        val TAG = "ForumActivity"
        Log.d(TAG, "Test kategorileri kontrol ediliyor")
        
        // Kategori koleksiyonunu tamamen temizle ve yeniden oluştur
        firestore.collection("forumCategories").get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Kategori koleksiyonu kontrol ediliyor - ${snapshot.size()} adet kategori var")
                
                // Önceki kategorileri temizle
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                    Log.d(TAG, "Kategori silinecek: ${doc.id}")
                }
                
                batch.commit().addOnSuccessListener {
                    Log.d(TAG, "Kategoriler temizlendi, yeni kategoriler oluşturuluyor")
                    createTestCategories()
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Kategoriler temizlenirken hata: ${e.message}", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Kategori kontrolü başarısız: ${e.message}", e)
            }
    }
    
    private fun createTestCategories() {
        val TAG = "ForumActivity"
        val categories = listOf(
            hashMapOf(
                "name" to "Genel Sohbet",
                "description" to "Hayvanlarımız hakkında genel konular ve sohbetler",
                "order" to 0
            ),
            hashMapOf(
                "name" to "Beslenme",
                "description" to "Evcil hayvanların beslenmesi hakkında bilgiler ve sorular",
                "order" to 1
            ),
            hashMapOf(
                "name" to "Sağlık",
                "description" to "Evcil hayvan sağlığı, hastalıklar ve tedavi yöntemleri",
                "order" to 2
            ),
            hashMapOf(
                "name" to "Eğitim",
                "description" to "Evcil hayvan eğitimi, davranış problemleri ve çözümler",
                "order" to 3
            ),
            hashMapOf(
                "name" to "Etkinlikler",
                "description" to "Hayvan severler için buluşmalar ve etkinlikler",
                "order" to 4
            )
        )
        
        val batch = firestore.batch()
        
        categories.forEach { category ->
            val docRef = firestore.collection("forumCategories").document()
            batch.set(docRef, category)
            Log.d(TAG, "Kategori hazırlandı: ${category["name"]} (ID: ${docRef.id})")
        }
        
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Test kategorileri başarıyla oluşturuldu")
                // Kategorileri yeniden yükle
                viewModel.loadCategories()
                
                // Birkaç test postu ekle
                addSamplePosts()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Test kategorileri oluşturulamadı: ${e.message}", e)
            }
    }
    
    private fun addSamplePosts() {
        Log.d(TAG, "Örnek gönderiler oluşturuluyor")
        
        // Kategorileri al
        firestore.collection("forumCategories").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.d(TAG, "Örnek gönderiler eklenemedi: Kategori yok")
                    return@addOnSuccessListener
                }
                
                val categories = snapshot.documents.mapNotNull { doc ->
                    try {
                        val category = doc.toObject(ForumCategory::class.java)
                        category?.apply { id = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (categories.isEmpty()) {
                    Log.d(TAG, "Kategoriler dönüştürülemedi")
                    return@addOnSuccessListener
                }
                
                val currentUser = auth.currentUser ?: return@addOnSuccessListener
                
                Log.d(TAG, "Şu kategorilere örnek gönderiler eklenecek: ${categories.map { it.name }}")
                
                // Her kategori için örnek post ekle
                val batch = firestore.batch()
                
                categories.forEach { category ->
                    val postRef = firestore.collection("forumPosts").document()
                    val post = hashMapOf(
                        "id" to postRef.id,
                        "title" to "${category.name} kategorisinde örnek gönderi",
                        "content" to "Bu bir test gönderisidir. ${category.name} kategorisinde paylaşılan içerikler burada görüntülenecektir.",
                        "authorId" to currentUser.uid,
                        "authorName" to (currentUser.displayName ?: "Test Kullanıcısı"),
                        "categoryId" to category.id,
                        "categoryName" to category.name,
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "updatedAt" to com.google.firebase.Timestamp.now(),
                        "likedBy" to listOf<String>(),
                        "commentCount" to 0
                    )
                    
                    batch.set(postRef, post)
                    Log.d(TAG, "Örnek gönderi hazırlandı: ${post["title"]} (ID: ${postRef.id})")
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Örnek gönderiler başarıyla oluşturuldu")
                        // Gönderileri yeniden yükle
                        viewModel.loadPosts()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Örnek gönderiler oluşturulamadı: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Kategoriler yüklenirken hata: ${e.message}", e)
            }
    }

    private fun showPostActionDialog(post: ForumPost) {
        val options = arrayOf("Gönderiyi Görüntüle", "Gönderiyi Rapor Et", "İptal")
        
        AlertDialog.Builder(this)
            .setTitle("Gönderi İşlemleri")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Gönderiyi Görüntüle
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra("postId", post.id)
                        startActivity(intent)
                    }
                    1 -> { // Gönderiyi Rapor Et
                        showReportDialog(post.id)
                    }
                }
            }
            .show()
    }
    
    private fun showReportDialog(postId: String? = currentPostId) {
        if (postId == null) {
            Log.e(TAG, "Rapor edilecek gönderi bulunamadı")
            return
        }
        
        val reportReasons = arrayOf(
            "Uygunsuz içerik",
            "Rahatsız edici dil",
            "Spam veya reklam",
            "Yanlış bilgi",
            "Diğer"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Gönderiyi Rapor Et")
            .setItems(reportReasons) { _, which ->
                reportPost(postId, reportReasons[which])
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun reportPost(postId: String, reason: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Rapor etmek için giriş yapmalısınız")
            return
        }
        
        Log.d(TAG, "Gönderi raporlama başlatıldı. Post ID: $postId, Kullanıcı: ${currentUser.uid}")
        
        // Token yenileme işlemi
        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                Log.d(TAG, "Token başarıyla yenilendi, raporlama devam ediyor...")
                
                // Basitleştirilmiş raporlama işlemi - doğrudan array union kullan
                val updates = hashMapOf<String, Any>(
                    "reportedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.uid),
                    "reportReason" to reason
                )
                
                Log.d(TAG, "Gönderi güncelleniyor, FieldValue.arrayUnion kullanılıyor")
                
                firestore.collection("forumPosts").document(postId)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Gönderi başarıyla raporlandı: $postId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Gönderi raporlanırken hata: ${e.message}", e)
                        
                        // Hata içeriğini daha ayrıntılı göster
                        val errorDetails = "Hata Mesajı: ${e.message}\n" +
                                           "Sebep: ${if (e.cause != null) e.cause!!.message else "Bilinmiyor"}"
                        Log.e(TAG, "Detaylı hata: $errorDetails")
                        
                        // Yetki hatası için yeniden deneme
                        if (e.message?.contains("permission") == true || e.message?.contains("PERMISSION_DENIED") == true) {
                            Log.d(TAG, "Yetki hatası tespit edildi, 2 saniye sonra yeniden deneniyor")
                            Handler(Looper.getMainLooper()).postDelayed({
                                reportPost(postId, reason)
                            }, 2000)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Token yenileme hatası: ${e.message}", e)
            }
    }

    @Deprecated("Replaced by onBackPressedDispatcher.onBackPressed()", level = DeprecationLevel.WARNING)
    override fun onBackPressed() {
        if (binding.etSearch.isFocused) {
            binding.etSearch.clearFocus()
        } else {
            super.onBackPressed()
        }
    }
} 