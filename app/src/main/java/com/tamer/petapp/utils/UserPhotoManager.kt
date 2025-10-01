package com.tamer.petapp.utils

import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Kullanıcı profil fotoğraflarını yönetmek için yardımcı sınıf.
 */
object UserPhotoManager {
    private const val TAG = "UserPhotoManager"
    private val cache = mutableMapOf<String, String>() // userId -> photoData

    /**
     * Belirtilen kullanıcının profil fotoğrafını yükler ve ImageView'e yerleştirir.
     *
     * @param userId Fotoğrafı yüklenecek kullanıcının ID'si
     * @param imageView Fotoğrafın yerleştirileceği ImageView
     * @param defaultResource Fotoğraf yoksa veya yüklenemezse gösterilecek varsayılan resim
     */
    fun loadUserPhoto(userId: String, imageView: ImageView, defaultResource: Int = R.drawable.ic_profile) {
        // UserID null, boş ya da geçersiz ise varsayılan resmi göster ve çık
        if (userId.isBlank()) {
            imageView.setImageResource(defaultResource)
            return
        }

        // Önce cache'den kontrol et
        if (cache.containsKey(userId)) {
            val cachedPhotoData = cache[userId]
            if (!cachedPhotoData.isNullOrEmpty()) {
                val bitmap = ImageUtils.base64ToBitmap(cachedPhotoData)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            } else {
                // Cache'de boş değer varsa varsayılan resmi göster
                imageView.setImageResource(defaultResource)
                return
            }
        }

        // Cache'de yoksa Firestore'dan yükle
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val photoData = document.getString("photoData")
                        
                        // Cache'e ekle (null bile olsa)
                        cache[userId] = photoData ?: ""
                        
                        if (!photoData.isNullOrEmpty()) {
                            val bitmap = ImageUtils.base64ToBitmap(photoData)
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                            } else {
                                imageView.setImageResource(defaultResource)
                            }
                        } else {
                            imageView.setImageResource(defaultResource)
                        }
                    } else {
                        imageView.setImageResource(defaultResource)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Kullanıcı fotoğrafı yüklenirken hata: ${e.message}")
                    imageView.setImageResource(defaultResource)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore işlemi sırasında hata: ${e.message}")
            imageView.setImageResource(defaultResource)
        }
    }

    /**
     * Mevcut kullanıcının profil fotoğrafını yükler ve ImageView'e yerleştirir.
     *
     * @param imageView Fotoğrafın yerleştirileceği ImageView
     * @param defaultResource Fotoğraf yoksa veya yüklenemezse gösterilecek varsayılan resim
     */
    fun loadCurrentUserPhoto(imageView: ImageView, defaultResource: Int = R.drawable.ic_profile) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            loadUserPhoto(currentUser.uid, imageView, defaultResource)
        } else {
            imageView.setImageResource(defaultResource)
        }
    }

    /**
     * Belirtilen kullanıcının profil fotoğrafını yükler.
     * Coroutine kullanımı için suspend fonksiyon.
     *
     * @param userId Fotoğrafı yüklenecek kullanıcının ID'si
     * @return Base64 formatında profil fotoğrafı veya null
     */
    suspend fun getUserPhotoData(userId: String): String? = suspendCoroutine { continuation ->
        // Önce cache'den kontrol et
        if (cache.containsKey(userId)) {
            val cachedPhotoData = cache[userId]
            continuation.resume(cachedPhotoData)
            return@suspendCoroutine
        }

        // Cache'de yoksa Firestore'dan yükle
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val photoData = document?.getString("photoData")
                cache[userId] = photoData ?: ""
                continuation.resume(photoData)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Kullanıcı fotoğrafı yüklenirken hata: ${e.message}")
                continuation.resumeWithException(e)
            }
    }

    /**
     * Cache'i temizler.
     */
    fun clearCache() {
        cache.clear()
    }
} 