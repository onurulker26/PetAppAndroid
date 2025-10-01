package com.tamer.petapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.auth.LoginActivity
import com.tamer.petapp.guide.PetCareGuideActivity
import com.tamer.petapp.ui.disease.DiseaseAnalysisActivity
import com.tamer.petapp.ui.screens.ProfileScreen
import com.tamer.petapp.ui.theme.PetAppTheme
import com.tamer.petapp.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException

class ProfileActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentPhotoData: String? = null
    private val TAG = "ProfileActivity"
    
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2
    private val REQUEST_PERMISSION_CAMERA = 101
    private val REQUEST_PERMISSION_GALLERY = 102
    
    // Admin UID'lerini burada tanımla
    private val ADMIN_UIDS = listOf("ES9Vqa1m5eODBIaa0yYYNZsBh4q1", "ADMIN_UID2") // Gerçek admin UID'leri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Firebase başlatma
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // Yükleme durumlarında Firebase bağlantısını test et
            com.tamer.petapp.utils.FirebaseHelper.testConnection()
            
            // Kullanıcı giriş yapmamışsa giriş ekranına yönlendir
            if (auth.currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            
            setContent {
                PetAppTheme {
                    val user = auth.currentUser
                    var userName by remember { mutableStateOf(user?.displayName ?: "") }
                    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
                    var isAdmin by remember { mutableStateOf(false) }
                    var isLoading by remember { mutableStateOf(true) }
                    
                    // Firestore'dan kullanıcı bilgilerini yükle
                    LaunchedEffect(key1 = user?.uid) {
                        user?.uid?.let { uid ->
                            firestore.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document != null) {
                                        // Profil fotoğrafını yükle
                                        val photoData = document.getString("photoData")
                                        if (!photoData.isNullOrEmpty()) {
                                            currentPhotoData = photoData
                                            userPhotoUrl = photoData
                                        }
                                        
                                        // Admin kontrolü
                                        isAdmin = ADMIN_UIDS.contains(uid)
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Kullanıcı bilgileri yüklenirken hata: ${e.message}")
                                    isLoading = false
                                }
                        }
                    }
                    
                    if (!isLoading) {
                        ProfileScreen(
                            userEmail = user?.email ?: "",
                            userName = userName,
                            userPhotoUrl = userPhotoUrl,
                            isAdmin = isAdmin,
                            onBackClick = { finish() },
                            onNavigationItemClick = { itemId ->
                                when (itemId) {
                                    0 -> { // Ana Sayfa
                                        startActivity(Intent(this@ProfileActivity, MainActivity::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                        finish()
                                    }
                                    1 -> { // Veterinerler
                                        startActivity(Intent(this@ProfileActivity, VetClinicsActivity::class.java))
                                        finish()
                                    }
                                    2 -> { // Bakım
                                        startActivity(Intent(this@ProfileActivity, PetCareGuideActivity::class.java))
                                        finish()
                                    }
                                    4 -> { // Hastalık Analizi
                                        startActivity(Intent(this@ProfileActivity, DiseaseAnalysisActivity::class.java))
                                        finish()
                                    }
                                }
                            },
                            onUpdateProfile = { name ->
                                updateProfile(name)
                            },
                            onChangePassword = { currentPassword, newPassword ->
                                changePassword(currentPassword, newPassword)
                            },
                            onLogout = {
                                logout()
                            },
                            onTakePhoto = {
                                checkAndRequestCameraPermission()
                            },
                            onPickImage = {
                                checkAndRequestGalleryPermission()
                            },
                            onAdminPanelClick = {
                                startActivity(Intent(this@ProfileActivity, com.tamer.petapp.admin.AdminActivity::class.java))
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası: ${e.message}", e)
            Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateProfile(name: String) {
        val user = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                // Yükleniyor göstergesi gösterilebilir
                
                // Firestore'da kullanıcı bilgilerini güncelle
                val userUpdates = hashMapOf<String, Any>(
                    "displayName" to name
                )
                
                firestore.collection("users")
                    .document(user.uid)
                    .update(userUpdates)
                    .addOnSuccessListener {
                        // Firebase Auth'da da displayName'i güncelle
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        
                        user.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                Toast.makeText(this@ProfileActivity, "Profil başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Auth profil güncellenirken hata: ${e.message}")
                                Toast.makeText(this@ProfileActivity, "Profil güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore profil güncellenirken hata: ${e.message}")
                        Toast.makeText(this@ProfileActivity, "Profil güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Profil güncellenirken hata: ${e.message}")
                Toast.makeText(this@ProfileActivity, "Profil güncellenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        
        if (user.email.isNullOrEmpty()) {
            Toast.makeText(this, "Kullanıcı e-posta adresi bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Yeniden kimlik doğrulama
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Şifre değiştirme
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Şifreniz başarıyla değiştirildi", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Şifre değiştirirken hata: ${e.message}")
                        Toast.makeText(this, "Şifre değiştirirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Kimlik doğrulama hatası: ${e.message}")
                Toast.makeText(this, "Mevcut şifre yanlış", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun logout() {
        try {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Çıkış yapılırken hata: ${e.message}")
            Toast.makeText(this, "Çıkış yapılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION_CAMERA)
        } else {
            dispatchTakePictureIntent()
        }
    }
    
    private fun checkAndRequestGalleryPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION_GALLERY)
        } else {
            dispatchPickImageIntent()
        }
    }
    
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }
    
    private fun dispatchPickImageIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "Kamera izni verilmedi", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSION_GALLERY -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchPickImageIntent()
                } else {
                    Toast.makeText(this, "Galeri izni verilmedi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        uploadProfileImage(imageBitmap)
                    }
                }
                REQUEST_PICK_IMAGE -> {
                    val selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        try {
                            val inputStream = contentResolver.openInputStream(selectedImageUri)
                            val imageBitmap = BitmapFactory.decodeStream(inputStream)
                            uploadProfileImage(imageBitmap)
                        } catch (e: IOException) {
                            Log.e(TAG, "Görüntü seçiminde hata: ${e.message}")
                            Toast.makeText(this, "Görüntü yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    
    private fun uploadProfileImage(bitmap: Bitmap) {
        val user = auth.currentUser ?: return
        
        try {
            // Resmi boyutlandır ve Base64'e dönüştür
            val resizedBitmap = ImageUtils.resizeBitmap(bitmap, 300, 300)
            val base64Image = encodeToBase64(resizedBitmap)
            
            // Firestore'a kaydet
            val userRef = firestore.collection("users").document(user.uid)
            userRef.update("photoData", base64Image)
                .addOnSuccessListener {
                    currentPhotoData = base64Image
                    Toast.makeText(this, "Profil fotoğrafı güncellendi", Toast.LENGTH_SHORT).show()
                    
                    // Aktiviteyi yenile - Compose kullanırken gerekli olmayabilir
                    recreate()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Profil fotoğrafı güncellenirken hata: ${e.message}")
                    Toast.makeText(this, "Profil fotoğrafı güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Görüntü işlenirken hata: ${e.message}")
            Toast.makeText(this, "Görüntü işlenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun encodeToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
} 