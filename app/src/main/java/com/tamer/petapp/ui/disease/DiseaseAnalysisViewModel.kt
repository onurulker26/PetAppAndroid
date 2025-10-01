package com.tamer.petapp.ui.disease

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tamer.petapp.ml.DogDiseaseClassifier
import com.tamer.petapp.ml.CatDiseaseClassifier
import com.tamer.petapp.ml.Classification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiseaseAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private var dogClassifier: DogDiseaseClassifier? = null
    private var catClassifier: CatDiseaseClassifier? = null
    private val appContext = application.applicationContext
    
    private val _analysisResult = MutableLiveData<AnalysisResult>()
    val analysisResult: LiveData<AnalysisResult> = _analysisResult

    private var currentAnimalType: AnimalType? = null

    fun setAnimalType(type: AnimalType) {
        currentAnimalType = type
        _analysisResult.value = AnalysisResult.Ready("${type.getDisplayName()} analizi için fotoğraf seçin")
    }

    suspend fun analyzeImage(bitmap: Bitmap) {
        try {
            // Hayvan türü seçilmiş mi kontrol et
            val animalType = currentAnimalType
            if (animalType == null) {
                _analysisResult.postValue(AnalysisResult.Error("Lütfen önce hayvan türünü seçin"))
                return
            }

            // Bitmap kontrolü
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                _analysisResult.postValue(AnalysisResult.Error("Geçersiz fotoğraf boyutu"))
                return
            }

            // Classifier'ı lazy olarak yükle
            val results = try {
                when (animalType) {
                    AnimalType.DOG -> {
                        if (dogClassifier == null) {
                            android.util.Log.d("DiseaseAnalysisViewModel", "Köpek classifier'ı yükleniyor...")
                            dogClassifier = DogDiseaseClassifier(appContext)
                        }
                        dogClassifier!!.analyze(bitmap)
                    }
                    AnimalType.CAT -> {
                        if (catClassifier == null) {
                            android.util.Log.d("DiseaseAnalysisViewModel", "Kedi classifier'ı yükleniyor...")
                            catClassifier = CatDiseaseClassifier(appContext)
                        }
                        catClassifier!!.analyze(bitmap)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DiseaseAnalysisViewModel", "Classifier yüklenirken hata: ${e.message}", e)
                _analysisResult.postValue(
                    AnalysisResult.Error("Model yüklenirken hata oluştu. ${animalType.getDisplayName().lowercase()} hastalık tespit modeli dosyası (${animalType.getModelFileName()}) eksik olabilir.\n\nDetay: ${e.message}")
                )
                return
            }
            
            if (results.isNotEmpty()) {
                _analysisResult.postValue(
                    AnalysisResult.Success(
                        results.map { 
                            "${it.label}: ${String.format("%.1f", it.confidence * 100)}%" 
                        }
                    )
                )
            } else {
                _analysisResult.postValue(
                    AnalysisResult.Error("${animalType.getDisplayName()} hastalığı tespit edilemedi")
                )
            }
        } catch (e: Exception) {
            _analysisResult.postValue(
                AnalysisResult.Error("Analiz sırasında bir hata oluştu: ${e.message}")
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        dogClassifier?.close()
        catClassifier?.close()
    }
}

enum class AnimalType {
    DOG, CAT;
    
    fun getDisplayName(): String = when (this) {
        DOG -> "Köpek"
        CAT -> "Kedi"
    }
    
    fun getModelFileName(): String = when (this) {
        DOG -> "dog_disease_model.tflite"
        CAT -> "cat_disease_model.tflite"
    }
}

sealed class AnalysisResult {
    data class Success(val predictions: List<String>) : AnalysisResult()
    data class Error(val message: String) : AnalysisResult()
    data class Ready(val message: String) : AnalysisResult()
} 