package com.tamer.petapp.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tamer.petapp.R
import com.tamer.petapp.adapter.AdminBannerAdapter
import com.tamer.petapp.model.Banner
import java.util.Date
import java.util.UUID

class BannerManagementFragment : Fragment() {

    private val TAG = "BannerManagement"
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminBannerAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyBanners: TextView
    
    private val db = FirebaseFirestore.getInstance()
    
    // Görsel seçildiğinde kullanmak için dialog referansı
    private var currentDialogView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_banner_management, container, false)
        
        // View'ları tanımla
        recyclerView = view.findViewById(R.id.recyclerBanners)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyBanners = view.findViewById(R.id.tvEmptyBanners)
        
        setupRecyclerView()
        loadBanners()
        
        return view
    }
    
    private fun setupRecyclerView() {
        adapter = AdminBannerAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Düzenleme olayı
        adapter.onEditClick = { banner ->
            showEditDialog(banner)
        }
        
        // Silme olayı
        adapter.onDeleteClick = { banner ->
            showDeleteConfirmationDialog(banner)
        }
        
        // Aktif/Pasif değiştirme olayı
        adapter.onStatusChange = { banner, isActive ->
            toggleBannerStatus(banner, isActive)
        }
    }
    
    private fun loadBanners() {
        progressBar.visibility = View.VISIBLE
        tvEmptyBanners.visibility = View.GONE
        
        try {
            Log.d(TAG, "loadBanners - Duyuruları yüklemeye başlıyorum")
            
            // Mevcut oturumu kontrol et
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            if (currentUser == null) {
                Log.e(TAG, "Oturum açık kullanıcı yok!")
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Oturum açık kullanıcı: ${currentUser.email} (${currentUser.uid})")
            
            // Önce token'ı yenileyelim 
            currentUser.getIdToken(true)
                .addOnSuccessListener { tokenResult ->
                    Log.d(TAG, "Firebase token yenilendi, koleksiyona erişmeye çalışıyorum")
                    
                    // Firestore koleksiyonunu doğrudan çağıralım (db değişkeni yerine)
                    val firestoreDb = FirebaseFirestore.getInstance()
                    
                    // Doğrudan sunucudan güncel verileri alalım - NO_CACHE kullanarak
                    firestoreDb.collection("announcements")
                        .orderBy("priority", Query.Direction.ASCENDING)
                        .get(com.google.firebase.firestore.Source.SERVER) // Sunucudan taze verileri al
                        .addOnSuccessListener { snapshot ->
                            progressBar.visibility = View.GONE
                            
                            if (!snapshot.isEmpty) {
                                Log.d(TAG, "Sunucudan ${snapshot.size()} adet duyuru yüklendi")
                                
                                val banners = snapshot.documents.mapNotNull { doc ->
                            try {
                                        // Her belge için map oluştur
                                        val banner = Banner(
                                            id = doc.id,
                                            title = doc.getString("title") ?: "",
                                            description = doc.getString("description") ?: "",
                                            imageUrl = doc.getString("imageUrl") ?: "",
                                            buttonText = doc.getString("buttonText"),
                                            buttonUrl = doc.getString("buttonUrl"),
                                            priority = doc.getLong("priority")?.toInt() ?: 0,
                                            isActive = doc.getBoolean("isActive") ?: false,
                                            createdAt = doc.getDate("createdAt") ?: Date()
                                        )
                                        
                                        // Debug log
                                        Log.d(TAG, "Duyuru yüklendi: id=${banner.id}, title=${banner.title}, active=${banner.isActive}")
                                    banner
                            } catch (e: Exception) {
                                        Log.e(TAG, "Banner oluşturma hatası (${doc.id}): ${e.message}")
                                null
                            }
                        }
                        
                                Log.d(TAG, "İşlenmiş duyuru sayısı: ${banners.size}")
                                
                                // Adapter'a listeyi gönder
                                adapter.submitList(null) // Önce listeyi temizle
                                adapter.submitList(banners) // Sonra yeni listeyi ekle
                        
                                tvEmptyBanners.visibility = if (banners.isEmpty()) View.VISIBLE else View.GONE
                                recyclerView.visibility = if (banners.isEmpty()) View.GONE else View.VISIBLE
                } else {
                                Log.d(TAG, "Duyuru bulunamadı")
                    tvEmptyBanners.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                                
                                // Adapter'ı temizle
                                adapter.submitList(null)
                            }
                        }
                        .addOnFailureListener { error ->
                            progressBar.visibility = View.GONE
                            Log.e(TAG, "Banner yüklenirken hata: ${error.message}")
                            Toast.makeText(context, "Duyurular yüklenirken hata oluştu: ${error.message}", Toast.LENGTH_SHORT).show()
                            
                            // Hata hakkında daha detaylı bilgi
                            if (error is com.google.firebase.firestore.FirebaseFirestoreException) {
                                Log.e(TAG, "Firestore hata kodu: ${error.code}")
                            }
                        }
                }
                .addOnFailureListener { error ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Token yenileme hatası: ${error.message}")
                    Toast.makeText(context, "Yetkilendirme hatası: Lütfen yeniden giriş yapın", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e(TAG, "Banner yükleme işleminde beklenmeyen hata: ${e.message}")
            Toast.makeText(context, "Beklenmeyen bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Aktif/Pasif durumunu değiştir
    private fun toggleBannerStatus(banner: Banner, isActive: Boolean) {
        Log.d(TAG, "Switch değişikliği tespit edildi - Banner ID: ${banner.id}, Yeni durum: $isActive")
        
        // ID kontrolü
        if (banner.id.isEmpty()) {
            Log.e(TAG, "Banner ID eksik, güncelleme yapılamıyor")
            Toast.makeText(context, "Hata: ID eksik", Toast.LENGTH_SHORT).show()
            
            // İşlemi temizle
            adapter.switchProcessingCompleted(banner.id)
            return
        }
        
        // Önce UI güncelle - kullanıcı deneyimi için
        adapter.updateSwitchUi(banner.id, isActive)
            
        // İlerleme çubuğunu göster (opsiyonel)
        progressBar.visibility = View.VISIBLE
        
        try {
            // Güncel oturum token'ını al
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { tokenResult ->
                    Log.d(TAG, "Token yenilendi, güncelleme başlıyor...")
                    
                    // Firebase üzerinde güncelleme yap - doğrudan collection ismine eriş
                    FirebaseFirestore.getInstance().collection("announcements")
            .document(banner.id)
            .update("isActive", isActive)
            .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                Log.d(TAG, "Banner durumu güncellendi: ${banner.id}, Aktif: $isActive")
                            
                            // İşlemi temizle
                            adapter.switchProcessingCompleted(banner.id)
                        }
                }
                ?.addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Banner durumu güncellenemedi: ${e.message}", e)
                }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e(TAG, "Beklenmeyen hata: ${e.message}", e)
            Toast.makeText(context, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Banner silme işlemi
    private fun deleteBanner(banner: Banner) {
        if (banner.id.isEmpty()) {
            Log.e(TAG, "Silme hatası: Banner ID boş")
            Toast.makeText(context, "Duyuru silinemedi: ID bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }

        try {
        progressBar.visibility = View.VISIBLE
            
            // Silme işlemini logla
            Log.d(TAG, "Duyuru silme işlemi başlatıldı - ID: ${banner.id}, Başlık: ${banner.title}")
        
        // Önce veritabanından sil
        db.collection("announcements")
            .document(banner.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Banner başarıyla silindi: ${banner.id}")
                progressBar.visibility = View.GONE
                    Toast.makeText(context, "Duyuru silindi: ${banner.title}", Toast.LENGTH_SHORT).show()
                    
                    // Silme işleminden sonra listeyi yenile
                    loadBanners()
            }
            .addOnFailureListener { e ->
                    Log.e(TAG, "Banner silinirken hata: ${e.message}")
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Silme işlemi başarısız oldu: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Silme işleminde beklenmeyen hata: ${e.message}")
                progressBar.visibility = View.GONE
            Toast.makeText(context, "Beklenmeyen bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Silme onay dialogu
    private fun showDeleteConfirmationDialog(banner: Banner) {
        if (context == null) {
            Log.e(TAG, "Context null: Dialog gösterilemiyor")
            return
        }

        try {
        AlertDialog.Builder(requireContext())
            .setTitle("Duyuru Sil")
                .setMessage("'${banner.title}' duyurusunu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
                .setPositiveButton("Sil") { dialog, _ ->
                    dialog.dismiss()
                deleteBanner(banner)
            }
                .setNegativeButton("İptal") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
            .show()
        } catch (e: Exception) {
            Log.e(TAG, "Silme dialogu gösterilirken hata: ${e.message}")
            Toast.makeText(context, "Dialog gösterilemiyor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Ekleme/Düzenleme dialogu
    private fun showEditDialog(banner: Banner? = null) {
        val isEditMode = banner != null
        val dialogView = layoutInflater.inflate(R.layout.dialog_banner_edit, null)
        
        // Dialog view elemanlarını tanımla
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val rgBannerType = dialogView.findViewById<RadioGroup>(R.id.rgBannerType)
        val rbImageOnly = dialogView.findViewById<RadioButton>(R.id.rbImageOnly)
        val rbImageWithText = dialogView.findViewById<RadioButton>(R.id.rbImageWithText)
        val rbFullBanner = dialogView.findViewById<RadioButton>(R.id.rbFullBanner)
        
        // Önizleme bileşenleri
        val ivBannerPreview = dialogView.findViewById<ImageView>(R.id.ivBannerPreview)
        val tvPreviewTitle = dialogView.findViewById<TextView>(R.id.tvPreviewTitle)
        val previewContent = dialogView.findViewById<LinearLayout>(R.id.previewContent)
        val tvPreviewDescription = dialogView.findViewById<TextView>(R.id.tvPreviewDescription)
        val btnPreviewAction = dialogView.findViewById<Button>(R.id.btnPreviewAction)
        
        // Form bileşenleri
        val etImageUrl = dialogView.findViewById<TextInputEditText>(R.id.etImageUrl)
        val btnPreviewImage = dialogView.findViewById<Button>(R.id.btnPreviewImage)
        val layoutTextContent = dialogView.findViewById<LinearLayout>(R.id.layoutTextContent)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val layoutButtonContent = dialogView.findViewById<LinearLayout>(R.id.layoutButtonContent)
        val etButtonText = dialogView.findViewById<TextInputEditText>(R.id.etButtonText)
        val etButtonUrl = dialogView.findViewById<TextInputEditText>(R.id.etButtonUrl)
        val etPriority = dialogView.findViewById<TextInputEditText>(R.id.etPriority)
        val switchActive = dialogView.findViewById<SwitchMaterial>(R.id.switchActive)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        
        // Dialog başlığını ayarla
        tvDialogTitle.text = if (isEditMode) "Duyuru Düzenle" else "Duyuru Ekle"
        
        // Düzenleme modunda verileri doldur
        if (isEditMode && banner != null) {
            etTitle.setText(banner.title)
            etDescription.setText(banner.description)
            etButtonText.setText(banner.buttonText)
            etButtonUrl.setText(banner.buttonUrl)
            etPriority.setText(banner.priority.toString())
            switchActive.isChecked = banner.isActive
            etImageUrl.setText(banner.imageUrl)
            
            // Duyuru tipini belirle ve RadioButton seçimini yap
            if (banner.buttonText.isNullOrEmpty() && banner.buttonUrl.isNullOrEmpty()) {
                if (banner.title.isEmpty() && banner.description.isEmpty()) {
                    rbImageOnly.isChecked = true
                } else {
                    rbImageWithText.isChecked = true
                }
            } else {
                rbFullBanner.isChecked = true
            }
            
            // UI'ı seçilen tipe göre güncelle
            updateUIBasedOnBannerType(
                if (rbImageOnly.isChecked) 0 else if (rbImageWithText.isChecked) 1 else 2,
                layoutTextContent, layoutButtonContent, tvPreviewTitle, previewContent, btnPreviewAction
            )
            
            // Görseli yükle
            if (banner.imageUrl.isNotEmpty()) {
                loadImagePreview(banner.imageUrl, ivBannerPreview)
                
                // Önizleme görünümlerini güncelle
                updatePreviewUI(
                    banner.title,
                    banner.description,
                    banner.buttonText,
                    tvPreviewTitle,
                    tvPreviewDescription,
                    btnPreviewAction
                )
            }
        } else {
            // Yeni ekleme modunda varsayılan değerler
            rbImageWithText.isChecked = true
            updateUIBasedOnBannerType(1, layoutTextContent, layoutButtonContent, tvPreviewTitle, previewContent, btnPreviewAction)
        }
        
        // Duyuru tipi değişikliğini dinle
        rgBannerType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.rbImageOnly -> 0
                R.id.rbImageWithText -> 1
                R.id.rbFullBanner -> 2
                else -> 1
            }
            updateUIBasedOnBannerType(type, layoutTextContent, layoutButtonContent, tvPreviewTitle, previewContent, btnPreviewAction)
            }
        
        // TextWatcher ekleyerek canlı önizleme sağla
        setupLivePreview(
            etTitle,
            etDescription,
            etButtonText,
            tvPreviewTitle,
            tvPreviewDescription,
            btnPreviewAction
        )
        
        // Görsel önizleme butonu
        btnPreviewImage.setOnClickListener {
            val imageUrl = etImageUrl.text.toString().trim()
            if (imageUrl.isNotEmpty()) {
                loadImagePreview(imageUrl, ivBannerPreview)
            } else {
                Toast.makeText(context, "Lütfen görsel URL'si girin", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Dialog oluştur
        val materialDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        // Referans olarak kaydet
        this.currentDialogView = dialogView
        
        // İptal butonu
        btnCancel.setOnClickListener {
            materialDialog.dismiss()
        }
        
        // Kaydet butonu
        btnSave.setOnClickListener {
            // Değerleri al
            val imageUrl = etImageUrl.text.toString().trim()
            
            // Temel alan kontrolü - Görsel URL'si her durumda gerekli
            if (imageUrl.isEmpty()) {
                etImageUrl.error = "Görsel URL'si gerekli"
                return@setOnClickListener
            }
            
            // Seçilen duyuru tipine göre değerleri kontrol et
            val bannerType = when {
                rbImageOnly.isChecked -> 0
                rbImageWithText.isChecked -> 1
                rbFullBanner.isChecked -> 2
                else -> 1
            }
            
            // Değerleri al
            val title = if (bannerType > 0) etTitle.text.toString().trim() else ""
            val description = if (bannerType > 0) etDescription.text.toString().trim() else ""
            val buttonText = if (bannerType == 2) etButtonText.text.toString().trim() else ""
            val buttonUrl = if (bannerType == 2) etButtonUrl.text.toString().trim() else ""
            val priorityText = etPriority.text.toString().trim()
            val isActive = switchActive.isChecked
            
            // Alan kontrolleri
            if (bannerType > 0) {
            if (title.isEmpty()) {
                etTitle.error = "Başlık gerekli"
                return@setOnClickListener
            }
            
            if (description.isEmpty()) {
                etDescription.error = "Açıklama gerekli"
                return@setOnClickListener
            }
            }
            
            if (bannerType == 2) {
                if (buttonText.isEmpty()) {
                    etButtonText.error = "Buton metni gerekli"
                return@setOnClickListener
            }
            
                if (buttonUrl.isEmpty()) {
                    etButtonUrl.error = "Buton URL'si gerekli"
                return@setOnClickListener
                }
            }
            
            // Öncelik değeri kontrolü
            val priority = try {
                priorityText.toInt().coerceIn(0, 99)
            } catch (e: Exception) {
                0
            }
            
            // Yeni Banner nesnesi oluştur
            val newBanner = Banner(
                id = if (isEditMode && banner != null) banner.id else "",
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    buttonText = if (buttonText.isEmpty()) null else buttonText,
                    buttonUrl = if (buttonUrl.isEmpty()) null else buttonUrl,
                    priority = priority,
                    isActive = isActive,
                createdAt = if (isEditMode && banner != null) banner.createdAt else Date()
            )
            
            // Kaydetme işlemi
            saveBanner(newBanner, materialDialog)
        }
        
        // Dialogu göster
        materialDialog.show()
    }
    
    private fun updateUIBasedOnBannerType(
        type: Int,
        layoutTextContent: LinearLayout,
        layoutButtonContent: LinearLayout,
        previewTitle: TextView,
        previewContent: LinearLayout,
        previewButton: Button
    ) {
        when (type) {
            0 -> { // Sadece görsel
                layoutTextContent.visibility = View.GONE
                layoutButtonContent.visibility = View.GONE
                previewTitle.visibility = View.GONE
                previewContent.visibility = View.GONE
                previewButton.visibility = View.GONE
            }
            1 -> { // Görsel ve metin
                layoutTextContent.visibility = View.VISIBLE
                layoutButtonContent.visibility = View.GONE
                previewTitle.visibility = View.VISIBLE
                previewContent.visibility = View.VISIBLE
                previewButton.visibility = View.GONE
            }
            2 -> { // Tam duyuru
                layoutTextContent.visibility = View.VISIBLE
                layoutButtonContent.visibility = View.VISIBLE
                previewTitle.visibility = View.VISIBLE
                previewContent.visibility = View.VISIBLE
                previewButton.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupLivePreview(
        titleInput: TextInputEditText,
        descriptionInput: TextInputEditText,
        buttonTextInput: TextInputEditText,
        previewTitle: TextView,
        previewDescription: TextView,
        previewButton: Button
    ) {
        // Başlık değişikliğini dinle
        titleInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                previewTitle.text = s?.toString() ?: "Duyuru Başlığı"
            }
        })
        
        // Açıklama değişikliğini dinle
        descriptionInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                previewDescription.text = s?.toString() ?: "Duyuru açıklaması burada görünecek."
            }
        })
        
        // Buton metni değişikliğini dinle
        buttonTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                previewButton.text = s?.toString() ?: "Detaylar"
            }
        })
    }
    
    private fun updatePreviewUI(
        title: String,
        description: String,
        buttonText: String?,
        tvPreviewTitle: TextView,
        tvPreviewDescription: TextView,
        btnPreviewAction: Button
    ) {
        tvPreviewTitle.text = if (title.isNotEmpty()) title else "Duyuru Başlığı"
        tvPreviewDescription.text = if (description.isNotEmpty()) description else "Duyuru açıklaması burada görünecek."
        btnPreviewAction.text = buttonText ?: "Detaylar"
    }
    
    private fun loadImagePreview(imageUrl: String, imageView: ImageView) {
        try {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_pet_logo)
                .error(R.drawable.ic_pet_logo)
                .into(imageView)
        } catch (e: Exception) {
            Toast.makeText(context, "Görsel URL'si geçersiz: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Banner'ı kaydet
    private fun saveBanner(banner: Banner, dialog: androidx.appcompat.app.AlertDialog) {
        progressBar.visibility = View.VISIBLE
        
        // Direk Firestore'a kaydet
        saveToFirestore(banner, dialog)
    }
    
    // Firestore'a kaydet
    private fun saveToFirestore(banner: Banner, dialog: androidx.appcompat.app.AlertDialog) {
        Log.d(TAG, "saveToFirestore - Banner ID: ${banner.id}")
        
        // Yeni mi yoksa güncelleme mi?
        if (banner.id.isEmpty()) {
            Log.d(TAG, "Yeni duyuru ekleniyor")
            // Yeni banner için map oluştur
            val newBannerMap = hashMapOf(
            "title" to banner.title,
            "description" to banner.description,
            "imageUrl" to banner.imageUrl,
            "buttonText" to banner.buttonText,
            "buttonUrl" to banner.buttonUrl,
            "priority" to banner.priority,
                "isActive" to banner.isActive,
                "createdAt" to Date()
        )
            
            db.collection("announcements")
                .add(newBannerMap)
                .addOnSuccessListener { docRef ->
                    val newId = docRef.id
                    Log.d(TAG, "Yeni duyuru eklendi - ID: $newId")
                    progressBar.visibility = View.GONE
                    dialog.dismiss()
                    Toast.makeText(context, "Duyuru eklendi", Toast.LENGTH_SHORT).show()
                    
                    // Ekleme işleminden sonra listeyi yenile
                    loadBanners()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Duyuru eklenirken hata: ${e.message}")
                    Toast.makeText(context, "Duyuru eklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d(TAG, "Mevcut duyuru güncelleniyor - ID: ${banner.id}")
            // Güncelleme için map oluştur
            val updateMap = mapOf(
                "title" to banner.title,
                "description" to banner.description,
                "imageUrl" to banner.imageUrl,
                "buttonText" to banner.buttonText,
                "buttonUrl" to banner.buttonUrl,
                "priority" to banner.priority,
                "isActive" to banner.isActive
            )
            
            // Doğrudan belge ID'si ile güncelle
            db.collection("announcements")
                .document(banner.id)
                .update(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Duyuru güncellendi - ID: ${banner.id}")
                    progressBar.visibility = View.GONE
                    dialog.dismiss()
                    Toast.makeText(context, "Duyuru güncellendi", Toast.LENGTH_SHORT).show()
                    
                    // Güncelleme işleminden sonra listeyi yenile
                    loadBanners()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Duyuru güncellenirken hata: ${e.message}")
                    Toast.makeText(context, "Duyuru güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Artık dosya seçimi yapmıyoruz, bu metot kullanılmıyor
    }
    
    // Ana Fragment'tan çağrılacak metod
    fun onFabClick() {
        showEditDialog()
    }

    @Override
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Admin Panel'e dönüldü, verileri yeniliyorum")
        loadBanners()
    }
} 