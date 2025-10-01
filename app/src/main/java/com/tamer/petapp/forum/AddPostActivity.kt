package com.tamer.petapp.forum

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.databinding.ActivityAddPostBinding
import com.tamer.petapp.model.ForumCategory
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.repository.ForumRepository
import com.tamer.petapp.viewmodel.ForumViewModel
import com.tamer.petapp.viewmodel.ForumViewModelFactory
import kotlinx.coroutines.launch
import java.util.Date

class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedCategoryId: String? = null
    private var selectedCategoryName: String? = null

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

    private val viewModel: ForumViewModel by viewModels {
        ForumViewModelFactory(ForumRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Toolbar ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupListeners()
        observeViewModel()
        
        // Kategorileri yükle
        viewModel.loadCategories()
    }

    private fun setupListeners() {
        // Gönderme butonu
        binding.btnSubmit.setOnClickListener {
            validateAndSubmitPost()
        }
    }

    private fun observeViewModel() {
        // Kategorileri gözlemle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoriesState.collect { state ->
                    when (state) {
                        is ForumViewModel.UiState.Loading -> {
                            binding.chipProgressBar.visibility = View.VISIBLE
                            binding.chipGroupCategories.visibility = View.GONE
                        }
                        is ForumViewModel.UiState.Success<*> -> {
                            binding.chipProgressBar.visibility = View.GONE
                            val categories = state.data as? List<ForumCategory>
                            Log.d("AddPostActivity", "Kategoriler başarıyla yüklendi: ${categories?.size ?: 0}")
                            categories?.let { updateCategoryChips(it) }
                        }
                        is ForumViewModel.UiState.Error -> {
                            binding.chipProgressBar.visibility = View.GONE
                            val errorMessage = "Hata: ${state.message}"
                            Log.e("AddPostActivity", errorMessage, state.throwable)
                            Toast.makeText(this@AddPostActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryChips(categories: List<ForumCategory>) {
        val TAG = "AddPostActivity"
        Log.d(TAG, "updateCategoryChips çağrıldı. Kategori sayısı: ${categories.size}")
        
        binding.chipGroupCategories.removeAllViews()
        binding.chipGroupCategories.visibility = View.VISIBLE
        
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
                
                Log.d(TAG, "Kategori chip oluşturuldu: ${category.name} (ID: ${category.id})")
            }
            binding.chipGroupCategories.addView(chip)
        }
        
        // Kategori seçildiğinde
        binding.chipGroupCategories.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                val selectedChip = group.findViewById<Chip>(checkedId)
                selectedCategoryId = selectedChip?.tag as? String
                selectedCategoryName = selectedChip?.text?.toString()
                Log.d(TAG, "Kategori seçildi: ${selectedChip?.text}, ID: $selectedCategoryId")
            } else {
                selectedCategoryId = null
                selectedCategoryName = null
                Log.d(TAG, "Kategori seçimi temizlendi")
            }
        }
    }

    private fun validateAndSubmitPost() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        
        // Validasyon
        when {
            title.isEmpty() -> {
                binding.tilTitle.error = "Başlık boş olamaz"
                return
            }
            title.length < 5 -> {
                binding.tilTitle.error = "Başlık en az 5 karakter olmalıdır"
                return
            }
            content.isEmpty() -> {
                binding.tilContent.error = "İçerik boş olamaz"
                return
            }
            content.length < 10 -> {
                binding.tilContent.error = "İçerik en az 10 karakter olmalıdır"
                return
            }
            selectedCategoryId == null -> {
                Toast.makeText(this, "Lütfen bir kategori seçiniz", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        binding.tilTitle.error = null
        binding.tilContent.error = null
        
        // Gönderiyi oluştur
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val categoryId = selectedCategoryId
            val categoryName = selectedCategoryName
            
            if (categoryId != null && categoryName != null) {
                val post = ForumPost(
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                // Gönderiyi ekle
                viewModel.addPost(post)
                
                // Kullanıcıya geri bildirim
                Toast.makeText(this, "Gönderiniz başarıyla eklendi", Toast.LENGTH_SHORT).show()
                
                // Aktiviteyi kapat
                finish()
            } else {
                Toast.makeText(this, "Kategori bilgisi eksik", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Deprecated("Replaced by onBackPressedDispatcher.onBackPressed()", level = DeprecationLevel.WARNING)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 