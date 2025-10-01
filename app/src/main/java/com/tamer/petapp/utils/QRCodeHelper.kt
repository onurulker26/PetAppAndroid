package com.tamer.petapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.tamer.petapp.model.Pet
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object QRCodeHelper {
    private const val TAG = "QRCodeHelper"
    
    /**
     * Pet bilgileri için QR kod oluşturur
     * @param pet Evcil hayvan bilgileri
     * @param includeVaccinations Aşı bilgilerini dahil et
     * @param includeTreatments Tedavi bilgilerini dahil et
     * @return QR kod bitmap'i
     */
    suspend fun generatePetQRCode(
        pet: Pet,
        includeVaccinations: Boolean = true,
        includeTreatments: Boolean = true
    ): Bitmap? {
        return try {
            val petData = createPetDataJson(pet, includeVaccinations, includeTreatments)
            generateQRCodeBitmap(petData, 512, 512)
        } catch (e: Exception) {
            Log.e(TAG, "QR kod oluşturma hatası: ${e.message}")
            null
        }
    }
    
    /**
     * Veteriner için acil durum QR kodu oluşturur
     * @param pet Evcil hayvan
     * @param ownerPhone Sahip telefonu
     * @param emergencyContact Acil durum kişisi
     * @return Acil durum QR kodu
     */
    suspend fun generateEmergencyQRCode(
        pet: Pet,
        ownerPhone: String,
        emergencyContact: String = ""
    ): Bitmap? {
        return try {
            val emergencyData = JSONObject().apply {
                put("type", "emergency")
                put("pet_name", pet.name)
                put("pet_type", pet.type)
                put("pet_breed", pet.breed)
                put("owner_phone", ownerPhone)
                put("emergency_contact", emergencyContact)
                put("medical_notes", "Acil durumda veterinere başvurun")
                put("timestamp", System.currentTimeMillis())
            }
            
            generateQRCodeBitmap(emergencyData.toString(), 256, 256)
        } catch (e: Exception) {
            Log.e(TAG, "Acil durum QR kod hatası: ${e.message}")
            null
        }
    }
    
    /**
     * Veteriner kliniği için check-in QR kodu
     * @param clinicId Klinik ID
     * @param clinicName Klinik adı
     * @return Check-in QR kodu
     */
    fun generateClinicCheckInQR(clinicId: String, clinicName: String): Bitmap? {
        return try {
            val checkInData = JSONObject().apply {
                put("type", "clinic_checkin")
                put("clinic_id", clinicId)
                put("clinic_name", clinicName)
                put("timestamp", System.currentTimeMillis())
            }
            
            generateQRCodeBitmap(checkInData.toString(), 300, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Check-in QR kod hatası: ${e.message}")
            null
        }
    }
    
    /**
     * Pet data'sını JSON formatında oluşturur
     */
    private suspend fun createPetDataJson(
        pet: Pet,
        includeVaccinations: Boolean,
        includeTreatments: Boolean
    ): String {
        val firestore = FirebaseFirestore.getInstance()
        val petData = JSONObject()
        
        // Temel pet bilgileri
        petData.put("type", "pet_info")
        petData.put("id", pet.id)
        petData.put("name", pet.name)
        petData.put("animal_type", pet.type)
        petData.put("breed", pet.breed)
        petData.put("birth_date", pet.birthDate)
        petData.put("weight", pet.weight)
        
        // Aşı bilgileri
        if (includeVaccinations && pet.id != null) {
            try {
                val vaccinations = firestore.collection("vaccinations")
                    .whereEqualTo("petId", pet.id)
                    .get()
                    .await()
                
                val vaccinationArray = vaccinations.documents.map { doc ->
                    JSONObject().apply {
                        put("name", doc.getString("name"))
                        put("date", doc.getLong("date"))
                        put("next_date", doc.getLong("nextDate"))
                    }
                }
                
                petData.put("vaccinations", vaccinationArray)
            } catch (e: Exception) {
                Log.e(TAG, "Aşı bilgileri yüklenemedi: ${e.message}")
            }
        }
        
        // Tedavi bilgileri
        if (includeTreatments && pet.id != null) {
            try {
                val treatments = firestore.collection("treatments")
                    .whereEqualTo("petId", pet.id)
                    .get()
                    .await()
                
                val treatmentArray = treatments.documents.map { doc ->
                    JSONObject().apply {
                        put("name", doc.getString("name"))
                        put("start_date", doc.getLong("startDate"))
                        put("end_date", doc.getLong("endDate"))
                        put("status", doc.getString("status"))
                    }
                }
                
                petData.put("treatments", treatmentArray)
            } catch (e: Exception) {
                Log.e(TAG, "Tedavi bilgileri yüklenemedi: ${e.message}")
            }
        }
        
        petData.put("generated_at", System.currentTimeMillis())
        return petData.toString()
    }
    
    /**
     * String veriyi QR kod bitmap'ine çevirir
     */
    private fun generateQRCodeBitmap(data: String, width: Int, height: Int): Bitmap? {
        return try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "QR kod bitmap oluşturma hatası: ${e.message}")
            null
        }
    }
    
    /**
     * QR kod datasını parse eder
     */
    fun parseQRData(qrData: String): QRDataResult {
        return try {
            val jsonData = JSONObject(qrData)
            val type = jsonData.getString("type")
            
            when (type) {
                "pet_info" -> QRDataResult.PetInfo(jsonData)
                "emergency" -> QRDataResult.Emergency(jsonData)
                "clinic_checkin" -> QRDataResult.ClinicCheckIn(jsonData)
                else -> QRDataResult.Unknown(qrData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "QR data parse hatası: ${e.message}")
            QRDataResult.Error(e.message ?: "Parse error")
        }
    }
}

// QR Data Results
sealed class QRDataResult {
    data class PetInfo(val data: JSONObject) : QRDataResult()
    data class Emergency(val data: JSONObject) : QRDataResult()
    data class ClinicCheckIn(val data: JSONObject) : QRDataResult()
    data class Unknown(val rawData: String) : QRDataResult()
    data class Error(val message: String) : QRDataResult()
} 