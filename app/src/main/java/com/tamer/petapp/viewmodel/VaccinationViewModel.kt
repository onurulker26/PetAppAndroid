package com.tamer.petapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Vaccination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VaccinationViewModel : ViewModel() {
    
    private val _vaccinations = MutableStateFlow<List<Vaccination>>(emptyList())
    val vaccinations: StateFlow<List<Vaccination>> = _vaccinations.asStateFlow()
    
    private val _pets = MutableStateFlow<Map<String, Pet>>(emptyMap())
    val pets: StateFlow<Map<String, Pet>> = _pets.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val TAG = "VaccinationViewModel"
    
    fun loadData(userId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                if (userId == null) {
                    _errorMessage.value = "Oturum açmanız gerekiyor"
                    _isLoading.value = false
                    return@launch
                }
                
                val firestore = FirebaseFirestore.getInstance()
                
                // Önce evcil hayvanları yükle
                try {
                    val petDocuments = firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .get()
                        .await()
                    
                    if (petDocuments.isEmpty) {
                        _isLoading.value = false
                        return@launch
                    }
                    
                    val petsMap = mutableMapOf<String, Pet>()
                    val allVaccinations = mutableListOf<Vaccination>()
                    
                    for (petDoc in petDocuments) {
                        try {
                            val pet = petDoc.toObject(Pet::class.java)
                            pet.id = petDoc.id
                            petsMap[pet.id] = pet
                            
                            // Her evcil hayvanın aşılarını yükle
                            try {
                                val vaccinations = firestore.collection("users")
                                    .document(userId)
                                    .collection("pets")
                                    .document(pet.id)
                                    .collection("vaccinations")
                                    .get()
                                    .await()
                                
                                for (vacDoc in vaccinations) {
                                    try {
                                        val vaccination = vacDoc.toObject(Vaccination::class.java)
                                        vaccination?.let { vacc ->
                                            vacc.id = vacDoc.id
                                            vacc.petId = pet.id
                                            vacc.ownerId = userId
                                            allVaccinations.add(vacc)
                                            Log.d(TAG, "Aşı yüklendi: ${vacc.name}, ID: ${vacc.id}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Aşı dönüştürme hatası: ${e.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Aşı yükleme hatası: ${e.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Evcil hayvan dönüştürme hatası: ${e.message}")
                        }
                    }
                    
                    _pets.value = petsMap
                    _vaccinations.value = allVaccinations.sortedBy { it.date }
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
    
    fun deleteVaccination(vaccination: Vaccination, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                
                // ownerId varsa onu kullan, yoksa mevcut userId'yi al
                val userIdToUse = if (vaccination.ownerId.isNotEmpty()) {
                    vaccination.ownerId
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }
                
                if (userIdToUse.isEmpty()) {
                    onComplete(false)
                    _errorMessage.value = "Kullanıcı kimliği bulunamadı"
                    return@launch
                }
                
                // Firestore'dan silme işlemi
                firestore.collection("users")
                    .document(userIdToUse)
                    .collection("pets")
                    .document(vaccination.petId)
                    .collection("vaccinations")
                    .document(vaccination.id)
                    .delete()
                    .await()
                
                // StateFlow güncellemesi - mevcut aşılardan bu aşıyı çıkar
                val currentVaccinations = _vaccinations.value.toMutableList()
                currentVaccinations.removeIf { it.id == vaccination.id }
                _vaccinations.value = currentVaccinations
                
                Log.d(TAG, "Aşı başarıyla silindi: ${vaccination.name}")
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Aşı silme hatası: ${e.message}")
                _errorMessage.value = "Aşı silinirken hata oluştu: ${e.message}"
                onComplete(false)
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
    
    fun refreshData(userId: String?) {
        // Verileri yeniden yükle
        loadData(userId)
    }
} 