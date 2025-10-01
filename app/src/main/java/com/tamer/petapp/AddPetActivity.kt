package com.tamer.petapp

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tamer.petapp.databinding.ActivityAddPetBinding
import com.tamer.petapp.model.Pet
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddPetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPetBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedDate: Date = Date()
    private var selectedImageUri: Uri? = null
    private val TAG = "AddPetActivity"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    private var isEditMode = false
    private var petId = ""
    
    // Galeri izni için launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // İzin verildi, galeriyi aç
            pickImageLauncher.launch("image/*")
        } else {
            // İzin reddedildi
            Toast.makeText(this, "Fotoğraf seçmek için depolama iznine ihtiyaç var", Toast.LENGTH_LONG).show()
        }
    }
    
    // Resim seçme için launcher
    private val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Log.d(TAG, "Seçilen fotoğraf URI: $selectedImageUri")
            
            // Seçilen fotoğrafı ImageView'da göster
            try {
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .centerCrop()
                    .into(binding.ivPetPhoto)
            } catch (e: Exception) {
                Log.e(TAG, "Fotoğraf gösterilirken hata: ${e.message}")
                binding.ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Firebase başlatma
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Edit modu kontrolü
        isEditMode = intent.getBooleanExtra("is_edit_mode", false)

        if (isEditMode) {
            petId = intent.getStringExtra("pet_id") ?: ""
            loadPetData(petId)
        }
        
        // Dropdown menüleri ayarla
        setupDropdownMenus()
        
        // Tarih seçici ayarla
        setupDatePicker()
        
        // Resim seçme butonu
        binding.btnSelectPhoto.setOnClickListener {
            checkStoragePermission()
        }
        
        // Kaydet butonu
        binding.btnSavePet.setOnClickListener {
            if (validateInputs()) {
                if (isEditMode) {
                    showUpdateConfirmationDialog()
                } else {
                    savePet()
                }
            }
        }
    }
    
    private fun loadPetData(petId: String) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty() || petId.isEmpty()) {
            Toast.makeText(this, "Evcil hayvan bilgileri yüklenemedi", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pet = document.toObject(Pet::class.java)
                    pet?.let {
                        binding.etPetName.setText(it.name)
                        binding.actvPetType.setText(it.type)
                        binding.etPetBreed.setText(it.breed)
                        binding.actvPetGender.setText(it.gender)
                        binding.etPetWeight.setText(if (it.weight > 0) it.weight.toString() else "")
                        binding.etPetNotes.setText(it.notes)
                        
                        // Doğum tarihini ayarla
                        if (it.birthDate > 0) {
                            selectedDate = Date(it.birthDate)
                            binding.etPetBirthDate.setText(dateFormat.format(selectedDate))
                        }
                        
                        // Resmi yükle
                        if (!it.base64Image.isNullOrEmpty()) {
                            // Base64 formatındaki resmi göster
                            try {
                                val imageBytes = Base64.decode(it.base64Image, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                binding.ivPetPhoto.setImageBitmap(bitmap)
                                Log.d(TAG, "Base64 resmi başarıyla yüklendi")
                            } catch (e: Exception) {
                                Log.e(TAG, "Base64 resim yükleme hatası: ${e.message}")
                                binding.ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
                            }
                        }
                        // Base64 yoksa imageUrl'e bak
                        else if (!it.imageUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(it.imageUrl)
                                .placeholder(R.drawable.ic_pet_placeholder)
                                .error(R.drawable.ic_pet_placeholder)
                                .into(binding.ivPetPhoto)
                            Log.d(TAG, "URL resmi yüklendi: ${it.imageUrl}")
                        }
                        else {
                            // Resim yoksa varsayılan göster
                            binding.ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
                            Log.d(TAG, "Evcil hayvan resmi bulunamadı, varsayılan resim gösteriliyor")
                        }
                    }
                    
                    // Edit modunda başlık ve buton metnini değiştir
                    binding.btnSavePet.text = "Güncelle"
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Evcil hayvan bilgileri yüklenemedi: ${e.message}")
                Toast.makeText(this, "Evcil hayvan bilgileri yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }
    
    private fun showUpdateConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Değişiklikleri Kaydet")
            .setMessage("Evcil hayvan bilgilerindeki değişiklikleri kaydetmek istediğinizden emin misiniz?")
            .setPositiveButton("Kaydet") { _, _ ->
                updatePet()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun savePet() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSavePet.isEnabled = false

        try {
            if (selectedImageUri != null) {
                // Resmi işle ve Base64'e dönüştür
                val base64Image = processAndEncodeImage(selectedImageUri!!)
                if (base64Image != null) {
                    // Base64 formatındaki resimle verileri kaydet
                    savePetData(userId, "", base64Image)
                } else {
                    // Resim işlenemezse fotoğrafsız kaydet
                    Toast.makeText(this, "Fotoğraf işlenemedi, fotoğrafsız kaydedilecek", Toast.LENGTH_SHORT).show()
                    savePetData(userId, "", "")
                }
            } else {
                // Fotoğraf seçilmemişse direkt verileri kaydet
                savePetData(userId, "", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "savePet hatası: ${e.message}")
            Toast.makeText(this, "Fotoğraf işleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSavePet.isEnabled = true
        }
    }
    
    private fun updatePet() {
        val userId = auth.currentUser?.uid
        if (userId == null || petId.isEmpty()) {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSavePet.isEnabled = false

        try {
            if (selectedImageUri != null && !selectedImageUri.toString().startsWith("http")) {
                // Yeni fotoğraf seçilmişse
                val base64Image = processAndEncodeImage(selectedImageUri!!)
                if (base64Image != null) {
                    // Base64 formatındaki resimle verileri güncelle
                    updatePetData(userId, "", base64Image)
                } else {
                    // Fotoğraf işlenemezse eski fotoğrafla güncelle
                    firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .document(petId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val existingPet = document.toObject(Pet::class.java)
                                existingPet?.let {
                                    // Eski Base64 kodunu kullan
                                    updatePetData(userId, "", it.base64Image ?: "")
                                }
                            } else {
                                updatePetData(userId, "", "")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Mevcut evcil hayvan bilgileri alınamadı: ${e.message}")
                            updatePetData(userId, "", "")
                        }
                }
            } else {
                // Fotoğraf değiştirilmemişse mevcut verileri güncelle
                firestore.collection("users")
                    .document(userId)
                    .collection("pets")
                    .document(petId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val existingPet = document.toObject(Pet::class.java)
                            existingPet?.let {
                                // Eski Base64 kodunu kullan
                                updatePetData(userId, "", it.base64Image ?: "")
                            }
                        } else {
                            updatePetData(userId, "", "")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Mevcut evcil hayvan bilgileri alınamadı: ${e.message}")
                        updatePetData(userId, "", "")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updatePet hatası: ${e.message}")
            Toast.makeText(this, "Beklenmeyen hata: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSavePet.isEnabled = true
        }
    }
    
    private fun processAndEncodeImage(imageUri: Uri): String? {
        return try {
            // Resmi küçültme
            val bitmap = decodeAndCompressImage(imageUri) ?: return null
            
            // Bitmap'i byte dizisine dönüştür (düşük kaliteli JPEG)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val imageBytes = baos.toByteArray()
            
            // Dosya boyutunu logla
            val fileSizeKB = imageBytes.size / 1024
            Log.d(TAG, "Kodlanacak fotoğraf boyutu: $fileSizeKB KB")
            
            // Dosya boyutu çok büyükse daha fazla sıkıştır
            if (fileSizeKB > 500) {
                baos.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos)
                val smallerBytes = baos.toByteArray()
                Log.d(TAG, "Daha fazla sıkıştırılmış fotoğraf boyutu: ${smallerBytes.size / 1024} KB")
                return Base64.encodeToString(smallerBytes, Base64.DEFAULT)
            }
            
            // Base64'e dönüştür
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            Log.d(TAG, "Base64 string uzunluğu: ${base64Image.length} karakter")
            
            base64Image
        } catch (e: Exception) {
            Log.e(TAG, "Resim işleme hatası: ${e.message}")
            null
        }
    }
    
    private fun decodeAndCompressImage(imageUri: Uri): Bitmap? {
        return try {
            // Input stream oluştur
            val inputStream = contentResolver.openInputStream(imageUri)
            
            // BitmapFactory options
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            // Önce resmin boyutunu öğren
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            // Yeniden boyutlandırma oranını hesapla
            val maxDimension = 500 // 500px maksimum boyut (Firestore için daha küçük)
            val width = options.outWidth
            val height = options.outHeight
            var scale = 1
            
            while (width / scale / 2 >= maxDimension || height / scale / 2 >= maxDimension) {
                scale *= 2
            }
            
            // Boyutlandırılmış resmi yükle
            val resizeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            
            val resizedInputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(resizedInputStream, null, resizeOptions)
            resizedInputStream?.close()
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Resim işleme hatası: ${e.message}")
            null
        }
    }
    
    private fun savePetData(userId: String, imageUrl: String, base64Image: String) {
        try {
            // Evcil hayvan bilgilerini al
            val name = binding.etPetName.text.toString().trim()
            val type = binding.actvPetType.text.toString().trim()
            val breed = binding.etPetBreed.text.toString().trim()
            val birthDate = selectedDate.time
            val gender = binding.actvPetGender.text.toString().trim()
            val weightText = binding.etPetWeight.text.toString().trim()
            val weight = if (weightText.isNotEmpty()) weightText.toDouble() else 0.0
            val notes = binding.etPetNotes.text.toString().trim()
            
            // Pet modelini güncelle - base64Image ekle
            val pet = Pet(
                id = "",
                name = name,
                type = type,
                breed = breed,
                birthDate = birthDate,
                gender = gender,
                weight = weight,
                imageUrl = imageUrl,
                base64Image = base64Image, // Base64 formatında resim ekledik
                ownerId = userId,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )
            
            // Firestore'a kaydet
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .add(pet)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Evcil hayvan başarıyla kaydedildi: ${documentReference.id}")
                    Toast.makeText(this, "${name} başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Evcil hayvan kaydedilemedi: ${e.message}")
                    Toast.makeText(this, "Evcil hayvan kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSavePet.isEnabled = true
                }
        } catch (e: Exception) {
            Log.e(TAG, "savePetData hatası: ${e.message}")
            Toast.makeText(this, "Veri kaydedilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSavePet.isEnabled = true
        }
    }
    
    private fun updatePetData(userId: String, imageUrl: String, base64Image: String) {
        try {
            // Evcil hayvan bilgilerini al
            val name = binding.etPetName.text.toString().trim()
            val type = binding.actvPetType.text.toString().trim()
            val breed = binding.etPetBreed.text.toString().trim()
            val birthDate = selectedDate.time
            val gender = binding.actvPetGender.text.toString().trim()
            val weightText = binding.etPetWeight.text.toString().trim()
            val weight = if (weightText.isNotEmpty()) weightText.toDouble() else 0.0
            val notes = binding.etPetNotes.text.toString().trim()
            
            // Güncellenecek alanları belirle
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "type" to type,
                "breed" to breed,
                "birthDate" to birthDate,
                "gender" to gender,
                "weight" to weight,
                "notes" to notes
            )
            
            // Base64 resim varsa ekle
            if (base64Image.isNotEmpty()) {
                updates["base64Image"] = base64Image
                Log.d(TAG, "Base64 image güncelleniyor, uzunluk: ${base64Image.length}")
            }
            
            // Firestore'da güncelle
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Evcil hayvan başarıyla güncellendi: $petId")
                    Toast.makeText(this, "${name} başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Evcil hayvan güncellenemedi: ${e.message}")
                    Toast.makeText(this, "Evcil hayvan güncellenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSavePet.isEnabled = true
                }
        } catch (e: Exception) {
            Log.e(TAG, "updatePetData hatası: ${e.message}")
            Toast.makeText(this, "Veri güncellenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSavePet.isEnabled = true
        }
    }
    
    private fun setupDropdownMenus() {
        // Evcil hayvan türleri
        val petTypes = arrayOf(
            getString(R.string.dog),
            getString(R.string.cat),
            getString(R.string.bird),
            getString(R.string.fish),
            getString(R.string.rabbit),
            getString(R.string.hamster),
            getString(R.string.other)
        )
        
        val petTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petTypes)
        binding.actvPetType.setAdapter(petTypeAdapter)
        
        // Cinsiyet
        val genders = arrayOf(
            getString(R.string.male),
            getString(R.string.female)
        )
        
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvPetGender.setAdapter(genderAdapter)
    }
    
    private fun setupDatePicker() {
        binding.etPetBirthDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    
                    binding.etPetBirthDate.setText(dateFormat.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            // Gelecek tarihleri seçmeyi engelle
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Evcil hayvan adı
        val name = binding.etPetName.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            binding.tilPetName.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilPetName.error = null
        }
        
        // Tür
        val type = binding.actvPetType.text.toString().trim()
        if (TextUtils.isEmpty(type)) {
            binding.tilPetType.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilPetType.error = null
        }
        
        // Doğum tarihi
        if (selectedDate.time == 0L) {
            binding.tilPetBirthDate.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.tilPetBirthDate.error = null
        }
        
        return isValid
    }
    
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 ve üzeri için READ_MEDIA_IMAGES iznini kontrol et
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş, galeriyi aç
                    pickImageLauncher.launch("image/*")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                    // İzin daha önce reddedilmiş, kullanıcıya açıklama göster
                    Toast.makeText(
                        this,
                        "Evcil hayvanınızın fotoğrafını seçmek için medya iznine ihtiyacımız var",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
                else -> {
                    // İlk kez izin isteniyor
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            // Android 12 ve altı için READ_EXTERNAL_STORAGE iznini kontrol et
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş, galeriyi aç
                    pickImageLauncher.launch("image/*")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    // İzin daha önce reddedilmiş, kullanıcıya açıklama göster
                    Toast.makeText(
                        this,
                        "Evcil hayvanınızın fotoğrafını seçmek için depolama iznine ihtiyacımız var",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    // İlk kez izin isteniyor
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 