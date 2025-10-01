package com.tamer.petapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.tamer.petapp.model.Banner
import com.tamer.petapp.model.Pet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()
    
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _userPhotoUrl = MutableStateFlow("")
    val userPhotoUrl: StateFlow<String> = _userPhotoUrl.asStateFlow()
    
    private val TAG = "MainViewModel"
    
    fun loadPets(userId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                if (userId == null) {
                    _errorMessage.value = "Oturum açmanız gerekiyor"
                    _isLoading.value = false
                    return@launch
                }
                
                val firestore = FirebaseFirestore.getInstance()
                
                try {
                    // Önce önbellekten veri yükle
                    val cacheSnapshot = firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .get(Source.CACHE)
                        .await()
                    
                    if (!cacheSnapshot.isEmpty) {
                        val petsList = cacheSnapshot.documents.mapNotNull { doc ->
                            try {
                                val pet = doc.toObject(Pet::class.java)
                                pet?.id = doc.id
                                pet
                            } catch (e: Exception) {
                                Log.e(TAG, "Evcil hayvan dönüştürme hatası: ${e.message}")
                                null
                            }
                        }
                        _pets.value = petsList
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Önbellekten veri yükleme hatası: ${e.message}")
                }
                
                // Sunucudan güncel veriyi al
                try {
                    val serverSnapshot = firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .get(Source.SERVER)
                        .await()
                    
                    val petsList = serverSnapshot.documents.mapNotNull { doc ->
                        try {
                            val pet = doc.toObject(Pet::class.java)
                            pet?.id = doc.id
                            pet
                        } catch (e: Exception) {
                            Log.e(TAG, "Evcil hayvan dönüştürme hatası: ${e.message}")
                            null
                        }
                    }
                    _pets.value = petsList
                } catch (e: Exception) {
                    if (_pets.value.isEmpty()) {
                        _errorMessage.value = "Evcil hayvanlar yüklenemedi: ${e.message}"
                    }
                    Log.e(TAG, "Sunucudan veri yükleme hatası: ${e.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Beklenmeyen hata: ${e.message}"
                Log.e(TAG, "Veri yükleme hatası: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadBanners() {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                
                val snapshot = firestore.collection("announcements")
                    .whereEqualTo("isActive", true)
                    .orderBy("priority")
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val bannersList = snapshot.documents.mapNotNull { doc ->
                        try {
                            Banner(
                                id = doc.id,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                buttonText = doc.getString("buttonText"),
                                buttonUrl = doc.getString("buttonUrl"),
                                priority = doc.getLong("priority")?.toInt() ?: 0,
                                isActive = doc.getBoolean("isActive") ?: true
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Banner dönüştürme hatası: ${e.message}")
                            null
                        }
                    }.filter { it.isActive }
                    
                    _banners.value = bannersList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Banner yükleme hatası: ${e.message}")
            }
        }
    }
    
    fun loadUserProfilePhoto(userId: String?) {
        viewModelScope.launch {
            try {
                if (userId == null) {
                    return@launch
                }
                
                val firestore = FirebaseFirestore.getInstance()
                
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    val photoUrl = userDoc.getString("photoUrl") ?: ""
                    _userPhotoUrl.value = photoUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Profil fotoğrafı yükleme hatası: ${e.message}")
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
} 