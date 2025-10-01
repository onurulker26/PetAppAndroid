package com.tamer.petapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object BackupHelper {
    private const val TAG = "BackupHelper"
    
    /**
     * Kullanıcının tüm verilerini yedekler
     */
    suspend fun createBackup(context: Context): Uri? {
        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = auth.currentUser ?: return null
            
            val backupData = JSONObject()
            
            // User bilgileri
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val userData = JSONObject()
                userDoc.data?.forEach { (key, value) ->
                    userData.put(key, value)
                }
                backupData.put("user", userData)
            }
            
            // Pet bilgileri
            val pets = firestore.collection("users")
                .document(currentUser.uid)
                .collection("pets")
                .get()
                .await()
            
            val petsArray = JSONArray()
            for (pet in pets.documents) {
                val petData = JSONObject()
                pet.data?.forEach { (key, value) ->
                    petData.put(key, value)
                }
                petData.put("id", pet.id)
                
                // Pet'in aşılarını ekle
                val vaccinations = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("pets")
                    .document(pet.id)
                    .collection("vaccinations")
                    .get()
                    .await()
                
                val vaccinationsArray = JSONArray()
                vaccinations.documents.forEach { vaccination ->
                    val vaccinationData = JSONObject()
                    vaccination.data?.forEach { (key, value) ->
                        vaccinationData.put(key, value)
                    }
                    vaccinationData.put("id", vaccination.id)
                    vaccinationsArray.put(vaccinationData)
                }
                petData.put("vaccinations", vaccinationsArray)
                
                // Pet'in tedavilerini ekle
                val treatments = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("pets")
                    .document(pet.id)
                    .collection("treatments")
                    .get()
                    .await()
                
                val treatmentsArray = JSONArray()
                treatments.documents.forEach { treatment ->
                    val treatmentData = JSONObject()
                    treatment.data?.forEach { (key, value) ->
                        treatmentData.put(key, value)
                    }
                    treatmentData.put("id", treatment.id)
                    treatmentsArray.put(treatmentData)
                }
                petData.put("treatments", treatmentsArray)
                
                petsArray.put(petData)
            }
            backupData.put("pets", petsArray)
            
            // Backup metadata
            backupData.put("backup_info", JSONObject().apply {
                put("created_at", System.currentTimeMillis())
                put("version", "1.0")
                put("user_id", currentUser.uid)
                put("user_email", currentUser.email)
            })
            
            // Dosyaya kaydet
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "petapp_backup_${dateFormat.format(Date())}.json"
            
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { fos ->
                fos.write(backupData.toString(2).toByteArray())
            }
            
            // Cloud Storage'a da yükle
            uploadBackupToCloud(file, currentUser.uid)
            
            Uri.fromFile(file)
            
        } catch (e: Exception) {
            Log.e(TAG, "Backup oluşturma hatası: ${e.message}")
            null
        }
    }
    
    /**
     * Yedek dosyasını Firebase Storage'a yükler
     */
    private suspend fun uploadBackupToCloud(file: File, userId: String) {
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("backups/$userId/${file.name}")
            
            storageRef.putFile(Uri.fromFile(file)).await()
            Log.d(TAG, "Backup cloud'a yüklendi: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud backup hatası: ${e.message}")
        }
    }
    
    /**
     * Yedek dosyasından verileri geri yükler
     */
    suspend fun restoreFromBackup(context: Context, backupUri: Uri): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = auth.currentUser ?: return false
            
            // Backup dosyasını oku
            val inputStream = context.contentResolver.openInputStream(backupUri)
            val backupContent = inputStream?.bufferedReader()?.use { it.readText() }
            val backupData = JSONObject(backupContent ?: "")
            
            // User bilgilerini geri yükle
            if (backupData.has("user")) {
                val userData = backupData.getJSONObject("user")
                val userMap = jsonToMap(userData)
                
                firestore.collection("users")
                    .document(currentUser.uid)
                    .set(userMap)
                    .await()
            }
            
            // Pet bilgilerini geri yükle
            if (backupData.has("pets")) {
                val petsArray = backupData.getJSONArray("pets")
                
                for (i in 0 until petsArray.length()) {
                    val petData = petsArray.getJSONObject(i)
                    val petId = petData.optString("id")
                    
                    // Pet ana bilgilerini ekle
                    val petMap = jsonToMap(petData, excludeKeys = setOf("vaccinations", "treatments", "id"))
                    
                    val petRef = if (petId.isNotEmpty()) {
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("pets")
                            .document(petId)
                    } else {
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("pets")
                            .document()
                    }
                    
                    petRef.set(petMap).await()
                    
                    // Aşıları geri yükle
                    if (petData.has("vaccinations")) {
                        val vaccinationsArray = petData.getJSONArray("vaccinations")
                        for (j in 0 until vaccinationsArray.length()) {
                            val vaccinationData = vaccinationsArray.getJSONObject(j)
                            val vaccinationMap = jsonToMap(vaccinationData, excludeKeys = setOf("id"))
                            
                            petRef.collection("vaccinations")
                                .add(vaccinationMap)
                                .await()
                        }
                    }
                    
                    // Tedavileri geri yükle
                    if (petData.has("treatments")) {
                        val treatmentsArray = petData.getJSONArray("treatments")
                        for (j in 0 until treatmentsArray.length()) {
                            val treatmentData = treatmentsArray.getJSONObject(j)
                            val treatmentMap = jsonToMap(treatmentData, excludeKeys = setOf("id"))
                            
                            petRef.collection("treatments")
                                .add(treatmentMap)
                                .await()
                        }
                    }
                }
            }
            
            Log.d(TAG, "Backup başarıyla geri yüklendi")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Backup geri yükleme hatası: ${e.message}")
            false
        }
    }
    
    /**
     * Cloud'daki backup dosyalarını listeler
     */
    suspend fun listCloudBackups(userId: String): List<String> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val backupsRef = storage.reference.child("backups/$userId")
            
            val result = backupsRef.listAll().await()
            result.items.map { it.name }.sorted().reversed() // En yeni önce
            
        } catch (e: Exception) {
            Log.e(TAG, "Cloud backup listesi alma hatası: ${e.message}")
            emptyList()
        }
    }
    
    private fun jsonToMap(json: JSONObject, excludeKeys: Set<String> = emptySet()): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in excludeKeys) continue
            
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonArrayToList(value)
                else -> value
            }
        }
        
        return map
    }
    
    private fun jsonArrayToList(jsonArray: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            list.add(when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonArrayToList(value)
                else -> value
            })
        }
        return list
    }
} 