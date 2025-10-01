package com.tamer.petapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tamer.petapp.model.Pet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PetViewModel : ViewModel() {
    
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val TAG = "PetViewModel"
    
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
                    val petsSnapshot = firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .orderBy("name", Query.Direction.ASCENDING)
                        .get()
                        .await()
                    
                    if (petsSnapshot.isEmpty) {
                        _pets.value = emptyList()
                        _isLoading.value = false
                        return@launch
                    }
                    
                    val petsList = petsSnapshot.documents.mapNotNull { document ->
                        try {
                            val pet = document.toObject(Pet::class.java)
                            pet?.apply {
                                id = document.id
                                // Eğer ownerId boşsa, mevcut kullanıcıya atama yap
                                if (ownerId.isEmpty()) {
                                    ownerId = userId
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Evcil hayvan dönüştürme hatası: ${e.message}")
                            null
                        }
                    }
                    
                    _pets.value = petsList
                } catch (e: Exception) {
                    _errorMessage.value = "Evcil hayvanlar yüklenemedi: ${e.message}"
                    Log.e(TAG, "Evcil hayvan yükleme hatası: ${e.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Beklenmeyen hata: ${e.message}"
                Log.e(TAG, "Veri yükleme hatası: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deletePet(pet: Pet, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                
                // Önce evcil hayvanın tüm aşı kayıtlarını sil
                val vaccinationsSnapshot = firestore.collection("users")
                    .document(pet.ownerId)
                    .collection("pets")
                    .document(pet.id)
                    .collection("vaccinations")
                    .get()
                    .await()
                
                // Batch işlemi başlat
                val batch = firestore.batch()
                
                // Tüm aşıları batch'e ekle
                for (vaccination in vaccinationsSnapshot.documents) {
                    batch.delete(vaccination.reference)
                }
                
                // Evcil hayvanı da batch'e ekle
                batch.delete(
                    firestore.collection("users")
                        .document(pet.ownerId)
                        .collection("pets")
                        .document(pet.id)
                )
                
                // Batch işlemini gerçekleştir
                batch.commit().await()
                
                // StateFlow güncellemesi - mevcut evcil hayvan listesinden silinen hayvanı çıkar
                val currentPets = _pets.value.toMutableList()
                currentPets.removeIf { it.id == pet.id }
                _pets.value = currentPets
                
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Evcil hayvan silme hatası: ${e.message}")
                _errorMessage.value = "Evcil hayvan silinirken hata oluştu: ${e.message}"
                onComplete(false)
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
} 