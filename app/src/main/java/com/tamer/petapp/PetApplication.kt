package com.tamer.petapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.tamer.petapp.utils.FirebaseHelper

class PetApplication : Application() {
    
    companion object {
        private const val TAG = "PetApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Firebase'i başlat
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase App başlatıldı")
            
            // Firebase Helper'ı kullanarak bağlantıları yapılandır
            FirebaseHelper.initializeFirebase()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase başlatma hatası: ${e.message}", e)
        }
    }
} 