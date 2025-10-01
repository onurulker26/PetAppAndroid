package com.tamer.petapp.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Firebase bağlantısını ve yetkilendirme sorunlarını yöneten yardımcı sınıf.
 */
object FirebaseHelper {
    private const val TAG = "FirebaseHelper"
    
    /**
     * Firebase bağlantısını başlatır ve yapılandırır.
     * - Önbellek ayarları
     * - Yetkilendirme kontrolü
     * - Bağlantı testi
     */
    fun initializeFirebase() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Firestore ayarlarını optimize et
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            
            // Bağlantı testi yap
            testConnection()
            
            Log.d(TAG, "Firebase başarıyla yapılandırıldı")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase başlatma hatası: ${e.message}", e)
        }
    }
    
    /**
     * Firebase bağlantısını test eder.
     * Hata durumunda uygun işlemler yapılır.
     */
    fun testConnection() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        
        if (auth.currentUser == null) {
            Log.d(TAG, "Anonim kullanıcı sorgulama")
            return
        }
        
        // Test sorgusu - sadece kullanıcının kendi dokümanını oku
        firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { 
                Log.d(TAG, "Firebase bağlantı testi başarılı")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase bağlantı testi başarısız: ${e.message}", e)
                
                // Yetkilendirme hatası durumunda yeniden oturum açmayı dene
                if (e.message?.contains("PERMISSION_DENIED") == true || 
                    e.message?.contains("Missing or insufficient permissions") == true) {
                    refreshUserCredentials()
                }
            }
    }
    
    /**
     * Kullanıcı kimlik bilgilerini yenilemeye çalışır.
     * Bu, token süresi dolduğunda veya izin hataları olduğunda kullanışlıdır.
     */
    private fun refreshUserCredentials() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        
        if (user != null) {
            user.getIdToken(true)
                .addOnSuccessListener { result ->
                    val token = result.token
                    Log.d(TAG, "Kullanıcı token'ı başarıyla yenilendi")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Token yenileme hatası: ${e.message}", e)
                }
        } else {
            Log.w(TAG, "Oturum açmış kullanıcı olmadığından token yenilenemiyor")
        }
    }
} 