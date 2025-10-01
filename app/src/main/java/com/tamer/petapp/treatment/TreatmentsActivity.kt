package com.tamer.petapp.treatment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.adapter.TreatmentAdapter
import com.tamer.petapp.databinding.ActivityTreatmentsBinding
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Treatment
import com.tamer.petapp.model.TreatmentStatus
import com.tamer.petapp.model.Medication
import com.tamer.petapp.model.VetAppointment
import com.tamer.petapp.model.AppointmentStatus
import java.util.Date

class TreatmentsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTreatmentsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private val treatmentsList = mutableListOf<Treatment>()
    private val petsMap = mutableMapOf<String, Pet>()
    private lateinit var adapter: TreatmentAdapter
    
    private val TAG = "TreatmentsActivity"
    
    // Yüklenen tedavi belgelerinin her birini daha sonra kontrol edeceğiz
    private var loadedTreatments = mutableListOf<Treatment>()
    private var checkedTreatments = 0
    
    companion object {
        const val ADD_TREATMENT_REQUEST_CODE = 100
        const val EDIT_TREATMENT_REQUEST_CODE = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTreatmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Toolbar'ı ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.treatments)
        
        // Firebase bileşenlerini başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // RecyclerView ayarları
        adapter = TreatmentAdapter(
            treatmentsList, 
            petsMap,
            onItemClick = { treatment ->
                // Tedavi detayına gitme işlemi
                val intent = Intent(this, TreatmentDetailsActivity::class.java)
                intent.putExtra("treatmentId", treatment.id)
                intent.putExtra("petId", treatment.petId)
                startActivity(intent)
            },
            onEditClick = { treatment ->
                // Tedavi düzenleme işlemi - çökmeyi önlemek için hata kontrolü eklenmiş
                try {
                    // Düzenlenecek tedavinin eksik bilgisi olup olmadığını kontrol et
                    if (treatment.id.isEmpty() || treatment.petId.isEmpty()) {
                        Toast.makeText(this, "Bu tedavi düzenlenemez: eksik ID bilgisi", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Düzenleme için eksik bilgi - Treatment ID: ${treatment.id}, Pet ID: ${treatment.petId}")
                        return@TreatmentAdapter
                    }
                    
                    // Pet'in hala mevcut olup olmadığını kontrol et
                    if (!petsMap.containsKey(treatment.petId)) {
                        Toast.makeText(this, "Bu tedaviye ait evcil hayvan bulunamadı", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Tedaviye ait evcil hayvan bulunamadı - Pet ID: ${treatment.petId}")
                        return@TreatmentAdapter
                    }
                    
                    // Önce kullanıcıya bir onay diyaloğu göster ve hata tespiti için bilgi ver
                    val confirmDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Tedavi Düzenle")
                        .setMessage("'${treatment.name}' tedavisini düzenlemek istediğinize emin misiniz?")
                        .setPositiveButton("Düzenle") { dialog, _ ->
                            dialog.dismiss()
                            proceedToEditTreatment(treatment)
                        }
                        .setNegativeButton("Vazgeç", null)
                        .create()
                    confirmDialog.show()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Tedavi düzenleme genel hatası: ${e.message}")
                    Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { treatment ->
                // Tedavi silme işlemi
                removeTreatment(treatment)
            }
        )
        
        binding.rvTreatments.layoutManager = LinearLayoutManager(this)
        binding.rvTreatments.adapter = adapter
        
        // Yeni tedavi ekleme butonu
        binding.fabAddTreatment.setOnClickListener {
            val intent = Intent(this, AddTreatmentActivity::class.java)
            startActivityForResult(intent, ADD_TREATMENT_REQUEST_CODE)
        }
        
        // Tedavi listesini yükle
        loadTreatments()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Eğer tedaviler zaten yüklenmişse, hızlı yenileme yap
        if (treatmentsList.isNotEmpty()) {
            Log.d(TAG, "onResume: Hızlı tedavi listesi yenilemesi yapılıyor")
            refreshTreatments()
        } else {
            Log.d(TAG, "onResume: Tedavi listesi tamamen yenileniyor")
            loadTreatments()
        }
    }
    
    private fun loadTreatments() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoTreatments.visibility = View.GONE
        
        try {
            Log.d(TAG, "Tedaviler yükleniyor, Kullanıcı ID: $userId")
            
            // Firestore bağlantısını kontrol et
            val firestore = FirebaseFirestore.getInstance()
            if (firestore == null) {
                Log.e(TAG, "Firestore bağlantısı kurulamadı")
                binding.progressBar.visibility = View.GONE
                binding.tvNoTreatments.visibility = View.VISIBLE
                binding.tvNoTreatments.text = "Firebase bağlantısı kurulamadı"
                return
            }
            
            // Evcil hayvanları yükle
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .get()
                .addOnSuccessListener { petDocuments ->
                    Log.d(TAG, "Evcil hayvan sayısı: ${petDocuments.size()}")
                    
                    if (petDocuments.isEmpty) {
                        Log.d(TAG, "Hiç evcil hayvan bulunamadı")
                        binding.progressBar.visibility = View.GONE
                        binding.tvNoTreatments.visibility = View.VISIBLE
                        binding.rvTreatments.visibility = View.GONE
                        return@addOnSuccessListener
                    }
                    
                    // Tüm tedavileri toplamak için listeleri hazırla
                    val allTreatments = mutableListOf<Treatment>()
                    var petCounter = 0
                    var totalPets = petDocuments.size()
                    
                    // Her evcil hayvanın tedavilerini yükle
                    for (petDoc in petDocuments) {
                        try {
                            val pet = petDoc.toObject(Pet::class.java)
                            pet.id = petDoc.id
                            petsMap[pet.id] = pet
                            
                            Log.d(TAG, "Evcil hayvan: ${pet.name}, ID: ${pet.id} için tedaviler yükleniyor")
                            
                            // Bu evcil hayvanın tedavilerini yükle
                            val treatmentsRef = firestore.collection("users")
                                .document(userId)
                                .collection("pets")
                                .document(pet.id)
                                .collection("treatments")
                            
                            treatmentsRef.get()
                                .addOnCompleteListener { task ->
                                    petCounter++
                                    
                                    if (task.isSuccessful) {
                                        val treatmentDocs = task.result
                                        if (treatmentDocs != null && !treatmentDocs.isEmpty) {
                                            Log.d(TAG, "${pet.name} için ${treatmentDocs.size()} tedavi bulundu")
                                            
                                            for (treatmentDoc in treatmentDocs) {
                                                try {
                                                    // Treatment nesnesini manuel oluştur
                                                    val id = treatmentDoc.id
                                                    val data = treatmentDoc.data
                                                    
                                                    if (data != null) {
                                                        val name = data["name"] as? String ?: ""
                                                        val petId = data["petId"] as? String ?: pet.id
                                                        val startDate = (data["startDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                                                        val endDate = (data["endDate"] as? com.google.firebase.Timestamp)?.toDate()
                                                        
                                                        // Status enum değerini analiz et
                                                        val statusStr = data["status"] as? String ?: "ACTIVE"
                                                        val status = try {
                                                            TreatmentStatus.valueOf(statusStr)
                                                        } catch (e: Exception) {
                                                            TreatmentStatus.ACTIVE
                                                        }
                                                        
                                                        val notes = data["notes"] as? String ?: ""
                                                        
                                                        // Dönüştürme işlemini manuel olarak yapıyoruz
                                                        val treatment = Treatment(
                                                            id = id,
                                                            petId = petId,
                                                            name = name,
                                                            startDate = startDate,
                                                            endDate = endDate,
                                                            status = status,
                                                            notes = notes
                                                        )
                                                        
                                                        // İlaçları işle
                                                        try {
                                                            val medicationsData = data["medications"] as? List<Map<String, Any>>
                                                            if (medicationsData != null) {
                                                                val medications = medicationsData.mapNotNull { medicationMap ->
                                                                    try {
                                                                        Medication(
                                                                            id = medicationMap["id"] as? String ?: "",
                                                                            name = medicationMap["name"] as? String ?: "",
                                                                            dosage = medicationMap["dosage"] as? String ?: "",
                                                                            frequency = medicationMap["frequency"] as? String ?: "",
                                                                            startDate = (medicationMap["startDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                                                            endDate = (medicationMap["endDate"] as? com.google.firebase.Timestamp)?.toDate(),
                                                                            reminderTime = medicationMap["reminderTime"] as? String ?: "",
                                                                            notes = medicationMap["notes"] as? String ?: ""
                                                                        )
                                                                    } catch (e: Exception) {
                                                                        Log.e(TAG, "İlaç dönüştürme hatası: ${e.message}")
                                                                        null
                                                                    }
                                                                }
                                                                if (medications.isNotEmpty()) {
                                                                    treatment.medications = medications
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "İlaçlar işlenirken hata: ${e.message}")
                                                        }
                                                        
                                                        // Randevuları işle
                                                        try {
                                                            val appointmentsData = data["vetAppointments"] as? List<Map<String, Any>>
                                                            if (appointmentsData != null) {
                                                                val appointments = appointmentsData.mapNotNull { appointmentMap ->
                                                                    try {
                                                                        val appointment = VetAppointment(
                                                                            id = appointmentMap["id"] as? String ?: "",
                                                                            clinicName = appointmentMap["clinicName"] as? String ?: "",
                                                                            date = (appointmentMap["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                                                            notes = appointmentMap["notes"] as? String ?: "",
                                                                            status = try {
                                                                                val statusStr = appointmentMap["status"] as? String ?: "SCHEDULED"
                                                                                AppointmentStatus.valueOf(statusStr)
                                                                            } catch (e: Exception) {
                                                                                AppointmentStatus.SCHEDULED
                                                                            }
                                                                        )
                                                                        appointment
                                                                    } catch (e: Exception) {
                                                                        Log.e(TAG, "Randevu dönüştürme hatası: ${e.message}")
                                                                        null
                                                                    }
                                                                }
                                                                if (appointments.isNotEmpty()) {
                                                                    treatment.vetAppointments = appointments
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "Randevular işlenirken hata: ${e.message}")
                                                        }
                                                        
                                                        allTreatments.add(treatment)
                                                        Log.d(TAG, "Tedavi yüklendi: ${treatment.name}, Pet: ${pet.name}")
                                                    } else {
                                                        Log.e(TAG, "Tedavi verileri boş, ID: ${treatmentDoc.id}")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Tedavi dönüştürme hatası: ${e.message}")
                                                }
                                            }
                                        } else {
                                            Log.d(TAG, "${pet.name} için tedavi bulunamadı")
                                        }
                                    } else {
                                        Log.e(TAG, "${pet.name} için tedavi yüklenirken hata: ${task.exception?.message}")
                                    }
                                    
                                    // Son evcil hayvan işlendiyse UI'ı güncelle
                                    if (petCounter >= totalPets) {
                                        Log.d(TAG, "Tüm tedaviler yüklendi, toplam: ${allTreatments.size}")
                                        updateUI(allTreatments)
                                    }
                                }
                        } catch (e: Exception) {
                            Log.e(TAG, "Evcil hayvan işlenirken hata: ${e.message}")
                            petCounter++
                            // Son evcil hayvan işlendiyse UI'ı güncelle
                            if (petCounter >= totalPets) {
                                updateUI(allTreatments)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Evcil hayvanlar yüklenemedi: ${e.message}")
                    Toast.makeText(this, "Evcil hayvanlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoTreatments.visibility = View.VISIBLE
                    binding.rvTreatments.visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e(TAG, "loadTreatments genel hata: ${e.message}")
            Toast.makeText(this, "Tedaviler yüklenirken beklenmeyen hata: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.tvNoTreatments.visibility = View.VISIBLE
            binding.rvTreatments.visibility = View.GONE
        }
    }
    
    private fun updateUI(treatments: List<Treatment>) {
        binding.progressBar.visibility = View.GONE
        
        if (treatments.isEmpty()) {
            binding.tvNoTreatments.visibility = View.VISIBLE
            binding.rvTreatments.visibility = View.GONE
        } else {
            binding.tvNoTreatments.visibility = View.GONE
            binding.rvTreatments.visibility = View.VISIBLE
            
            // Tedavileri listeye ekle
            loadedTreatments.clear()
            loadedTreatments.addAll(treatments)
            
            // Her tedavi için belge kontrolü yap
            checkedTreatments = 0
            if (treatments.isNotEmpty()) {
                checkTreatmentDocuments()
            } else {
                treatmentsList.clear()
                adapter.notifyDataSetChanged()
            }
        }
    }
    
    // Tedavi belgelerini kontrol eden yeni metod
    private fun checkTreatmentDocuments() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            displayTreatmentsList()
            return
        }
        
        for (treatment in loadedTreatments) {
            // Her tedavinin belge koleksiyonunu kontrol et
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(treatment.petId)
                .collection("treatments")
                .document(treatment.id)
                .collection("documents")
                .get()
                .addOnSuccessListener { documentsSnapshot ->
                    // Test belgesini hariç tut
                    val realDocuments = documentsSnapshot.documents.filter { 
                        !(it.id == "test_doc" || it.getBoolean("isTest") == true) 
                    }
                    
                    // Belge varsa hasDocuments'ı true yap
                    val hasDocuments = realDocuments.isNotEmpty()
                    
                    // Değer farklıysa tedavi belgesini güncelle
                    if (hasDocuments != treatment.hasDocuments) {
                        treatment.hasDocuments = hasDocuments
                        
                        // Firestore'daki değeri de güncelle
                        firestore.collection("users")
                            .document(userId)
                            .collection("pets")
                            .document(treatment.petId)
                            .collection("treatments")
                            .document(treatment.id)
                            .update("hasDocuments", hasDocuments)
                            .addOnSuccessListener {
                                Log.d(TAG, "Tedavi hasDocuments değeri güncellendi: ${treatment.id}, değer: $hasDocuments")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Tedavi hasDocuments değeri güncellenemedi: ${treatment.id}, hata: ${e.message}")
                            }
                    }
                    
                    // İşlenen tedavi sayısını artır
                    checkedTreatments++
                    
                    // Tüm tedaviler işlendiyse listeyi güncelle
                    if (checkedTreatments >= loadedTreatments.size) {
                        displayTreatmentsList()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Tedavi belgeleri kontrol edilemedi: ${treatment.id}, hata: ${e.message}")
                    
                    // İşlenen tedavi sayısını artır
                    checkedTreatments++
                    
                    // Tüm tedaviler işlendiyse listeyi güncelle
                    if (checkedTreatments >= loadedTreatments.size) {
                        displayTreatmentsList()
                    }
                }
        }
    }
    
    // Kontrol tamamlandıktan sonra listeyi göster
    private fun displayTreatmentsList() {
        treatmentsList.clear()
        treatmentsList.addAll(loadedTreatments)
        adapter.notifyDataSetChanged()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                ADD_TREATMENT_REQUEST_CODE, EDIT_TREATMENT_REQUEST_CODE -> {
                    // Tedavi ekleme veya düzenleme sonrası listeyi güncelle
                    loadTreatments()
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun removeTreatment(treatment: Treatment) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Firestore'dan tedaviyi sil
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .document(treatment.petId)
            .collection("treatments")
            .document(treatment.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Tedavi silindi: ${treatment.name}")
                Toast.makeText(this, getString(R.string.treatment_deleted), Toast.LENGTH_SHORT).show()
                
                // Listeyi güncelle
                treatmentsList.remove(treatment)
                adapter.notifyDataSetChanged()
                
                // Liste boşsa, boş görünümü göster
                if (treatmentsList.isEmpty()) {
                    binding.tvNoTreatments.visibility = View.VISIBLE
                    binding.rvTreatments.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Tedavi silinemedi: ${e.message}")
                Toast.makeText(this, "Tedavi silinemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Düzenleme işlemine devam et - yeni bir metod
    private fun proceedToEditTreatment(treatment: Treatment) {
        try {
            // Firestore'da tedavi belgesi kontrolü yap
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }
            
            binding.progressBar.visibility = View.VISIBLE
            
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(treatment.petId)
                .collection("treatments")
                .document(treatment.id)
                .get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (document.exists()) {
                        // Belge varsa düzenleme ekranına git
                        val intent = Intent(this, AddTreatmentActivity::class.java).apply {
                            // Flag ekleyerek yeni bir görev olarak başlat
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            
                            // Gerekli tüm bilgileri ekle
                            putExtra("isEditing", true)
                            putExtra("treatmentId", treatment.id)
                            putExtra("petId", treatment.petId)
                            putExtra("name", treatment.name)
                            
                            // Basitleştirilmiş veri alanları (tercihen)
                            putExtra("treatmentName", treatment.name)
                            putExtra("treatmentNotes", treatment.notes)
                            putExtra("treatmentStatus", treatment.status.toString())
                        }
                        
                        // Toast göster ve günlüğe kaydet
                        Toast.makeText(this, "${treatment.name} düzenleme ekranı açılıyor...", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Düzenleme ekranına geçiliyor: ${treatment.id}, ${treatment.name}")
                        
                        // Yeni Activity'yi başlat
                        try {
                            startActivityForResult(intent, EDIT_TREATMENT_REQUEST_CODE)
                        } catch (e: Exception) {
                            Log.e(TAG, "Activity başlatma hatası: ${e.message}")
                            Toast.makeText(this, "Düzenleme ekranı açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Tedavi belgesi bulunamadı: ${treatment.id}")
                        Toast.makeText(this, "Bu tedavi artık mevcut değil", Toast.LENGTH_SHORT).show()
                        // Tedavi listesini yeniden yükle
                        loadTreatments()
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Tedavi kontrol edilemedi: ${e.message}")
                    Toast.makeText(this, "Tedavi bilgileri kontrol edilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "Düzenleme ekranına geçiş hatası: ${e.message}")
            Toast.makeText(this, "Düzenleme ekranına geçilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Tedavilerin bilgilerini Firestore'dan yeniden yükleyen bir fonksiyon
    private fun refreshTreatments() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        // Mevcut tedavileri güncelle
        var updatedCount = 0
        for (treatment in treatmentsList.toList()) { // Thread-safe bir kopya üzerinde çalışalım
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(treatment.petId)
                .collection("treatments")
                .document(treatment.id)
                .get()
                .addOnSuccessListener { document ->
                    updatedCount++
                    
                    if (document.exists()) {
                        // Tedavi bilgilerini güncelle
                        document.getString("name")?.let { treatment.name = it }
                        document.getString("notes")?.let { treatment.notes = it }
                        document.getTimestamp("startDate")?.toDate()?.let { treatment.startDate = it }
                        document.getTimestamp("endDate")?.toDate()?.let { treatment.endDate = it }
                        
                        // Status enum'u güncelle
                        document.getString("status")?.let { statusStr ->
                            try {
                                treatment.status = TreatmentStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                Log.e(TAG, "Status dönüştürme hatası: $statusStr")
                            }
                        }
                        
                        document.getBoolean("hasDocuments")?.let { treatment.hasDocuments = it }
                        
                        // İlaçlar ve randevular çok değişebileceği için onları da güncelle
                        try {
                            // İlaçlar
                            val medicationsData = document.get("medications") as? List<Map<String, Any>>
                            if (medicationsData != null) {
                                val medications = medicationsData.mapNotNull { medicationMap ->
                                    try {
                                        Medication(
                                            id = medicationMap["id"] as? String ?: "",
                                            name = medicationMap["name"] as? String ?: "",
                                            dosage = medicationMap["dosage"] as? String ?: "",
                                            frequency = medicationMap["frequency"] as? String ?: "",
                                            startDate = (medicationMap["startDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                            endDate = (medicationMap["endDate"] as? com.google.firebase.Timestamp)?.toDate(),
                                            reminderTime = medicationMap["reminderTime"] as? String ?: "",
                                            notes = medicationMap["notes"] as? String ?: ""
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "İlaç dönüştürme hatası: ${e.message}")
                                        null
                                    }
                                }
                                treatment.medications = medications
                            }
                            
                            // Randevular
                            val appointmentsData = document.get("vetAppointments") as? List<Map<String, Any>>
                            if (appointmentsData != null) {
                                val appointments = appointmentsData.mapNotNull { appointmentMap ->
                                    try {
                                        VetAppointment(
                                            id = appointmentMap["id"] as? String ?: "",
                                            clinicName = appointmentMap["clinicName"] as? String ?: "",
                                            date = (appointmentMap["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                                            notes = appointmentMap["notes"] as? String ?: "",
                                            status = try {
                                                val statusStr = appointmentMap["status"] as? String ?: "SCHEDULED"
                                                AppointmentStatus.valueOf(statusStr)
                                            } catch (e: Exception) {
                                                AppointmentStatus.SCHEDULED
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Randevu dönüştürme hatası: ${e.message}")
                                        null
                                    }
                                }
                                treatment.vetAppointments = appointments
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "İlaç/randevu listesi işlenirken hata: ${e.message}")
                        }
                    } else {
                        // Tedavi artık yok, listeden kaldıralım
                        treatmentsList.remove(treatment)
                    }
                    
                    // Son tedavi güncellendiğinde adapter'ı yenile
                    if (updatedCount >= treatmentsList.size) {
                        binding.progressBar.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                        
                        // Liste boşsa, boş görünümü göster
                        if (treatmentsList.isEmpty()) {
                            binding.tvNoTreatments.visibility = View.VISIBLE
                            binding.rvTreatments.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    updatedCount++
                    Log.e(TAG, "Tedavi güncellenemedi (${treatment.id}): ${e.message}")
                    
                    // Son tedavi güncellendiğinde adapter'ı yenile
                    if (updatedCount >= treatmentsList.size) {
                        binding.progressBar.visibility = View.GONE
                        adapter.notifyDataSetChanged()
                    }
                }
        }
        
        // Hiç tedavi yoksa loading'i gizle
        if (treatmentsList.isEmpty()) {
            binding.progressBar.visibility = View.GONE
        }
    }
} 