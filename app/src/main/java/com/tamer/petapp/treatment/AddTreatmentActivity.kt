package com.tamer.petapp.treatment

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.tamer.petapp.R
import com.tamer.petapp.adapter.MedicationAdapter
import com.tamer.petapp.adapter.AppointmentAdapter
import com.tamer.petapp.adapter.DocumentAdapter
import com.tamer.petapp.databinding.ActivityAddTreatmentBinding
import com.tamer.petapp.model.Medication
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Treatment
import com.tamer.petapp.model.TreatmentDocument
import com.tamer.petapp.model.TreatmentStatus
import com.tamer.petapp.model.VetAppointment
import com.tamer.petapp.model.AppointmentStatus
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTreatmentActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddTreatmentBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private val pets = mutableMapOf<String, Pet>()
    private var selectedPetId = ""
    private var selectedStartDate = Date()
    private var selectedEndDate: Date? = null
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    
    // İlaç, randevu ve belgeleri saklamak için listeler
    private val medications = mutableListOf<Medication>()
    private val vetAppointments = mutableListOf<VetAppointment>()
    private val documents = mutableListOf<TreatmentDocument>()
    
    private lateinit var medicationsAdapter: MedicationAdapter
    private lateinit var appointmentsAdapter: AppointmentAdapter
    private lateinit var documentsAdapter: DocumentAdapter
    
    private val TAG = "AddTreatmentActivity"
    
    // Tedavi düzenleme parametreleri
    private var isEditing = false
    private var editTreatmentId = ""
    
    // Tedavi düzenleme için intent ekstra anahtarları
    companion object {
        const val EXTRA_TREATMENT_ID = "treatmentId"
        const val EXTRA_PET_ID = "petId"
        const val EXTRA_IS_EDITING = "isEditing"
        const val EXTRA_TREATMENT_NAME = "name"
        const val REQUEST_CODE_PICK_DOCUMENT = 1001 // Belge seçimi için request code
    }
    
    private fun loadDocumentsForEdit(userId: String, petId: String, treatmentId: String) {
        Log.d(TAG, "Düzenleme için belgeler yükleniyor - userId: $userId, petId: $petId, treatmentId: $treatmentId")
        binding.progressBar.visibility = View.VISIBLE
        
        // Önce documents listesini temizle
        documents.clear()
        documentsAdapter.notifyDataSetChanged()
        
        try {
            // Belgeleri güvenli bir şekilde yüklemeye çalış
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId)
                .collection("treatments")
                .document(treatmentId)
                .collection("documents")
            
            Log.d(TAG, "Belge koleksiyonu sorgulanıyor: ${docRef.path}")
            
            docRef.get()
                .addOnSuccessListener { documentsSnapshot ->
                    binding.progressBar.visibility = View.GONE
                    
                    try {
                        if (documentsSnapshot.isEmpty) {
                            Log.d(TAG, "Hiç belge bulunamadı")
                            updateDocumentsView() // Belge yoksa UI'ı güncelle
                            return@addOnSuccessListener
                        }
                        
                        Log.d(TAG, "Belge snapshot alındı, ${documentsSnapshot.size()} belge bulundu")
                        
                        // Yüklenen belgeleri geçici bir listede topla
                        val tempDocuments = mutableListOf<TreatmentDocument>()
                        
                        // Her bir belgeyi tek tek işle
                        for (docSnapshot in documentsSnapshot.documents) {
                            try {
                                // test_doc belge kontrolü
                                if (docSnapshot.id == "test_doc" || docSnapshot.getBoolean("isTest") == true) {
                                    Log.d(TAG, "Test belgesi atlandı: ${docSnapshot.id}")
                                    continue // Test belgelerini atla
                                }
                                
                                // Belge verilerini manuel olarak çıkar
                                val docId = docSnapshot.id
                                val docName = docSnapshot.getString("name") ?: "Adsız Belge"
                                val docType = docSnapshot.getString("type") ?: "Bilinmeyen Tür"
                                val docUploadDate = docSnapshot.getTimestamp("uploadDate")?.toDate() ?: Date()
                                val docFileUrl = docSnapshot.getString("fileUrl") ?: ""
                                
                                // fileContent manuel olarak yükle
                                var fileContent = ""
                                try {
                                    fileContent = docSnapshot.getString("fileContent") ?: ""
                                } catch (e: Exception) {
                                    Log.e(TAG, "fileContent alanı yüklenemedi: ${e.message}")
                                }
                                
                                // Belge nesnesi oluştur
                                val document = TreatmentDocument(
                                    id = docId,
                                    name = docName,
                                    type = docType,
                                    uploadDate = docUploadDate,
                                    fileUrl = docFileUrl,
                                    fileContent = fileContent
                                )
                                
                                // Önceden varolanları işaretle
                                document.extraProperties["isExistingDocument"] = true
                                
                                // Belgeyi geçici listeye ekle
                                tempDocuments.add(document)
                                Log.d(TAG, "Belge eklendi (varolan): ${docId} - ${docName}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Belge işlenirken hata: ${docSnapshot.id}, ${e.message}", e)
                            }
                        }
                        
                        // UI thread'inde ana listeyi güncelle
                        runOnUiThread {
                            try {
                                // Ana listeyi temizle ve yeni belgeleri ekle
                                documents.clear()
                                documents.addAll(tempDocuments)
                                
                                Log.d(TAG, "${documents.size} belge listeye eklendi, adapter güncelleniyor")
                                documentsAdapter.notifyDataSetChanged()
                                updateDocumentsView()
                                
                                // Boş belge uyarısını güncelle
                                binding.tvNoDocuments.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
                                binding.rvDocuments.visibility = if (documents.isEmpty()) View.GONE else View.VISIBLE
                                
                                // hasDocuments alanını güncelle
                                if (documents.isNotEmpty()) {
                                    firestore.collection("users")
                                        .document(userId)
                                        .collection("pets")
                                        .document(petId)
                                        .collection("treatments")
                                        .document(treatmentId)
                                        .update("hasDocuments", true)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "hasDocuments alanı true olarak güncellendi")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "hasDocuments alanı güncellenemedi: ${e.message}")
                                        }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Adapter güncellenirken hata: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Belge snapshot işlenirken genel hata: ${e.message}", e)
                        Toast.makeText(this@AddTreatmentActivity, "Belgeler işlenirken hata oluştu", Toast.LENGTH_SHORT).show()
                        updateDocumentsView() // Hata durumunda UI'ı güncelle
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Belge yükleme hatası: ${e.message}", e)
                    Toast.makeText(this, "Belgeler yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateDocumentsView() // Hata durumunda UI'ı güncelle
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "loadDocumentsForEdit genel hatası: ${e.message}", e)
            Toast.makeText(this, "Belge yükleme sırasında beklenmeyen hata", Toast.LENGTH_SHORT).show()
            updateDocumentsView() // Hata durumunda UI'ı güncelle
        }
    }
    
    // Tedavi verilerini yüklemek için yardımcı fonksiyon
    private fun loadTreatmentData() {
        try {
            Log.d(TAG, "Tedavi verileri yükleniyor...")
            // Tedavi verilerini yükle
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Log.e(TAG, "loadTreatment: Kullanıcı kimliği bulunamadı")
                finish()
                return
            }
            
            if (selectedPetId.isEmpty() || editTreatmentId.isEmpty()) {
                Toast.makeText(this, "Düzenleme için gerekli bilgiler eksik", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Log.e(TAG, "loadTreatment: ID'ler boş. Pet ID: $selectedPetId, Treatment ID: $editTreatmentId")
                finish()
                return
            }
            
            // Pet kontrolü
            if (!pets.containsKey(selectedPetId)) {
                // Pets içinde bu ID yoksa, tekrar evcil hayvanları yüklemeyi dene
                Log.w(TAG, "Seçilen pet ID bulunamadı ($selectedPetId), evcil hayvanlar yeniden yükleniyor")
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Evcil hayvan bilgisi bulunamadı, tekrar deneniyor", Toast.LENGTH_SHORT).show()
                loadPets()
                return
            }
            
            // Firestore sorgusu yap
            Log.d(TAG, "Tedavi belgesi Firestore'dan yükleniyor: users/$userId/pets/$selectedPetId/treatments/$editTreatmentId")
            
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(selectedPetId)
                .collection("treatments")
                .document(editTreatmentId)
            
            docRef.get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (document != null && document.exists()) {
                        Log.d(TAG, "Tedavi belgesi bulundu: ${document.id}")
                        Log.d(TAG, "Belge verileri: ${document.data}")
                        
                        try {
                            // Tedavi nesnesini manuel oluşturalım, null kontrolleri ile
                            val name = document.getString("name") ?: ""
                            val notes = document.getString("notes") ?: ""
                            val statusStr = document.getString("status") ?: TreatmentStatus.ACTIVE.toString()
                            val hasDocuments = document.getBoolean("hasDocuments") ?: false
                            val petId = document.getString("petId") ?: selectedPetId
                            
                            // Tarihleri güvenli bir şekilde al
                            var startDate: Date? = null
                            document.getTimestamp("startDate")?.let { startDate = it.toDate() }
                            
                            var endDate: Date? = null
                            document.getTimestamp("endDate")?.let { endDate = it.toDate() }
                            
                            // Tarihler null ise
                            if (startDate == null) {
                                startDate = Date()
                                Log.w(TAG, "Tedavinin başlangıç tarihi null, bugün olarak ayarlandı")
                            }
                            
                            // Status enum'a çevir
                            val status = try {
                                TreatmentStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                Log.e(TAG, "Status dönüştürme hatası: $statusStr")
                                TreatmentStatus.ACTIVE
                            }
                            
                            // Medications listesini al
                            val medicationsList = mutableListOf<Medication>()
                            try {
                                val medicationsArray = document.get("medications") as? ArrayList<*>
                                medicationsArray?.forEach { medMap ->
                                    try {
                                        if (medMap is Map<*, *>) {
                                            val medId = (medMap["id"] as? String) ?: java.util.UUID.randomUUID().toString()
                                            val medName = (medMap["name"] as? String) ?: ""
                                            val medDosage = (medMap["dosage"] as? String) ?: ""
                                            val medFrequency = (medMap["frequency"] as? String) ?: ""
                                            val medNotes = (medMap["notes"] as? String) ?: ""
                                            
                                            // Tarihler
                                            var medStartDate: Date? = null
                                            (medMap["startDate"] as? Timestamp)?.let { 
                                                medStartDate = it.toDate() 
                                            }
                                            
                                            var medEndDate: Date? = null
                                            (medMap["endDate"] as? Timestamp)?.let {
                                                medEndDate = it.toDate()
                                            }
                                            
                                            if (medStartDate == null) medStartDate = Date()
                                            
                                            // Reminder time
                                            val reminderTime = (medMap["reminderTime"] as? String) ?: ""
                                            
                                            val medication = Medication(
                                                id = medId,
                                                name = medName,
                                                dosage = medDosage,
                                                frequency = medFrequency,
                                                startDate = medStartDate ?: Date(),
                                                endDate = medEndDate,
                                                reminderTime = reminderTime,
                                                notes = medNotes
                                            )
                                            
                                            medicationsList.add(medication)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "İlaç işlenirken hata: ${e.message}")
                                    }
                                }
                                
                                Log.d(TAG, "${medicationsList.size} ilaç yüklendi")
                            } catch (e: Exception) {
                                Log.e(TAG, "İlaç listesi işlenirken hata: ${e.message}")
                            }
                            
                            // Appointments listesini al
                            val appointmentsList = mutableListOf<VetAppointment>()
                            try {
                                val appointmentsArray = document.get("vetAppointments") as? ArrayList<*>
                                appointmentsArray?.forEach { appMap ->
                                    try {
                                        if (appMap is Map<*, *>) {
                                            val appId = (appMap["id"] as? String) ?: java.util.UUID.randomUUID().toString()
                                            val appClinicName = (appMap["clinicName"] as? String) ?: ""
                                            val appNotes = (appMap["notes"] as? String) ?: ""
                                            
                                            // Tarih
                                            var appDate: Date? = null
                                            (appMap["date"] as? Timestamp)?.let {
                                                appDate = it.toDate()
                                            }
                                            
                                            if (appDate == null) appDate = Date()
                                            
                                            // Status
                                            val appStatusStr = (appMap["status"] as? String) ?: AppointmentStatus.SCHEDULED.toString()
                                            val appStatus = try {
                                                AppointmentStatus.valueOf(appStatusStr)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Randevu status dönüştürme hatası: $appStatusStr")
                                                AppointmentStatus.SCHEDULED
                                            }
                                            
                                            val appointment = VetAppointment(
                                                id = appId,
                                                clinicName = appClinicName,
                                                date = appDate ?: Date(),
                                                notes = appNotes,
                                                status = appStatus
                                            )
                                            
                                            appointmentsList.add(appointment)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Randevu işlenirken hata: ${e.message}")
                                    }
                                }
                                
                                Log.d(TAG, "${appointmentsList.size} randevu yüklendi")
                            } catch (e: Exception) {
                                Log.e(TAG, "Randevu listesi işlenirken hata: ${e.message}")
                            }
                            
                            // Ana UI thread'inde işlemleri gerçekleştir
                            runOnUiThread {
                                try {
                                    // Tedavi bilgilerini form alanlarına doldur
                                    binding.etTreatmentName.setText(name)
                                    
                                    // Tarih bilgilerini ayarla
                                    if (startDate != null) {
                                        selectedStartDate = startDate!!
                                        binding.etStartDate.setText(dateFormat.format(startDate!!))
                                    }
                                    
                                    if (endDate != null) {
                                        selectedEndDate = endDate
                                        binding.etEndDate.setText(dateFormat.format(endDate!!))
                                    } else {
                                        binding.etEndDate.setText("")
                                    }
                                    
                                    // Notları ayarla
                                    binding.etNotes.setText(notes)
                                    
                                    // Durum spinner'ını ayarla - daha güvenli bir şekilde
                                    val statusText = when (status) {
                                        TreatmentStatus.ACTIVE -> getString(R.string.active)
                                        TreatmentStatus.COMPLETED -> getString(R.string.completed)
                                        TreatmentStatus.CANCELLED -> getString(R.string.cancelled)
                                        else -> getString(R.string.active)
                                    }
                                    
                                    val statusSpinner = binding.spinnerStatus as? AutoCompleteTextView
                                    statusSpinner?.setText(statusText, false)
                                    
                                    // İlaçları yükle
                                    medications.clear()
                                    medications.addAll(medicationsList)
                                    medicationsAdapter.notifyDataSetChanged()
                                    updateMedicationsView()
                                    
                                    // Randevuları yükle
                                    vetAppointments.clear()
                                    vetAppointments.addAll(appointmentsList)
                                    appointmentsAdapter.notifyDataSetChanged()
                                    updateAppointmentsView()
                                    
                                    // Belgeleri yükle
                                    if (hasDocuments) {
                                        loadDocumentsForEdit(userId, petId, editTreatmentId)
                                    }
                                    
                                    // Tedavi başarıyla yüklendi, butonu etkinleştir
                                    binding.btnSave.isEnabled = true
                                } catch (e: Exception) {
                                    binding.btnSave.isEnabled = true
                                    Log.e(TAG, "UI güncelleme hatası: ${e.message}", e)
                                    Toast.makeText(this@AddTreatmentActivity, "Arayüz güncellenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                        } catch (e: Exception) {
                            binding.btnSave.isEnabled = true
                            Log.e(TAG, "Tedavi verisi işlenirken hata: ${e.message}", e)
                            Toast.makeText(this, "Tedavi bilgileri işlenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        binding.btnSave.isEnabled = true
                        Log.e(TAG, "Tedavi belgesi bulunamadı: $editTreatmentId")
                        Toast.makeText(this, "Tedavi bulunamadı", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Log.e(TAG, "Tedavi yükleme hatası: ${e.message}", e)
                    
                    // Firebase hatasının ayrıntılarını kaydet
                    if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                        Log.e(TAG, "Firestore Hatası: Kod: ${e.code}, Mesaj: ${e.message}")
                        Toast.makeText(this, "Firebase hatası: ${e.code} - ${e.message}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Tedavi yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    
                    finish()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Tedavi verileri yüklenirken hata: ${e.message}", e)
            Toast.makeText(this, "Tedavi yüklenirken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTreatmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Firebase bileşenlerini başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        
        // Hata ayıklama için activity'nin başlangıç bilgilerini kaydet
        Log.d(TAG, "AddTreatmentActivity başlatıldı")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString(", ")}")
        
        // Toolbar'ı ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Başlangıç tarihi bugün olarak ayarla
        selectedStartDate = Date()
        binding.etStartDate.setText(dateFormat.format(selectedStartDate))
        
        // Tarih seçicileri ayarla
        setupDatePickers()
        
        // Durum seçimini ayarla - önce status spinner'ı hazırla
        setupStatusSpinner()
        
        // Adapter'ları ayarla
        setupMedicationsAdapter()
        setupAppointmentsAdapter()
        setupDocumentsAdapter()
        
        // İlaç, randevu ve belge ekleme butonlarını ayarla
        setupAddButtons()
        
        // Kaydetme butonu
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTreatment()
            }
        }
        
        // Düzenleme modunu kontrol et - UI hazırlandıktan sonra yap
        try {
            isEditing = intent.getBooleanExtra("isEditing", false)
            
            if (isEditing) {
                editTreatmentId = intent.getStringExtra("treatmentId") ?: ""
                selectedPetId = intent.getStringExtra("petId") ?: ""
                val treatmentName = intent.getStringExtra("name") ?: ""
                
                supportActionBar?.title = getString(R.string.edit_treatment)
                Log.d(TAG, "Düzenleme modu aktif. Tedavi ID: $editTreatmentId, Pet ID: $selectedPetId, Name: $treatmentName")
                
                // Veri kontrolü
                if (editTreatmentId.isEmpty() || selectedPetId.isEmpty()) {
                    Toast.makeText(this, "Düzenleme için gerekli bilgiler eksik", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Düzenleme ID veya Pet ID boş! Treatment ID: $editTreatmentId, Pet ID: $selectedPetId")
                    finish()
                    return
                }
            } else {
                supportActionBar?.title = getString(R.string.add_treatment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Düzenleme modu kontrolünde hata: ${e.message}", e)
            Toast.makeText(this, "Başlatma hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            supportActionBar?.title = getString(R.string.add_treatment)
            isEditing = false
        }
        
        // Evcil hayvanları yükle - en son yap
        loadPets()
    }
    
    private fun setupDatePickers() {
        // Başlangıç tarihi seçici
        binding.etStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        // Bitiş tarihi seçici
        binding.etEndDate.setOnClickListener {
            showDatePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val c = Calendar.getInstance()
        
        // Eğer varsa mevcut seçili tarihi kullan
        if (isStartDate && selectedStartDate != Date(0)) {
            c.time = selectedStartDate
        } else if (!isStartDate && selectedEndDate != null) {
            c.time = selectedEndDate!!
        }
        
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth, selectedDay)
                
                if (isStartDate) {
                    selectedStartDate = cal.time
                    binding.etStartDate.setText(dateFormat.format(cal.time))
                    
                    // Eğer bitiş tarihi başlangıçtan önceyse, bitiş tarihini temizle
                    if (selectedEndDate != null && selectedEndDate!!.before(selectedStartDate)) {
                        selectedEndDate = null
                        binding.etEndDate.setText("")
                    }
                } else {
                    selectedEndDate = cal.time
                    binding.etEndDate.setText(dateFormat.format(cal.time))
                }
            },
            year,
            month,
            day
        )
        
        // Başlangıç tarihinden önceki tarihleri bitiş tarihi olarak seçmeyi engelle
        if (!isStartDate) {
            datePickerDialog.datePicker.minDate = selectedStartDate.time
        }
        
        datePickerDialog.show()
    }
    
    private fun loadPets() {
        try {
            Log.d(TAG, "loadPets başlatıldı")
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "loadPets: Kullanıcı kimliği bulunamadı")
                finish()
                return
            }
            
            binding.progressBar.visibility = View.VISIBLE
            
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .get()
                .addOnSuccessListener { documents ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Önce bir evcil hayvan eklemelisiniz", Toast.LENGTH_LONG).show()
                        Log.w(TAG, "Evcil hayvan listesi boş")
                        finish()
                        return@addOnSuccessListener
                    }
                    
                    pets.clear()
                    
                    // Evcil hayvanları yükle
                    for (document in documents) {
                        val pet = document.toObject(Pet::class.java)
                        pet.id = document.id
                        pets[pet.id] = pet
                    }
                    
                    Log.d(TAG, "${pets.size} evcil hayvan yüklendi: ${pets.keys.joinToString(", ")}")
                    
                    // Spinner'ı doldur - UI Thread'de güvenli bir şekilde
                    runOnUiThread {
                        setupPetSpinner()
                        
                        // Spinnerlar hazır olduktan sonra düzenleme modundaysa tedavi bilgilerini getir
                        if (isEditing && selectedPetId.isNotEmpty() && editTreatmentId.isNotEmpty()) {
                            Log.d(TAG, "Evcil hayvanlar yüklendi, şimdi tedavi yüklenecek")
                            
                            // Seçilen evcil hayvanın var olduğunu kontrol et
                            if (pets.containsKey(selectedPetId)) {
                                binding.progressBar.visibility = View.VISIBLE
                                loadTreatmentData() // Direkt olarak yükle
                            } else {
                                Log.e(TAG, "Seçilen evcil hayvan bulunamadı: $selectedPetId")
                                Toast.makeText(this, "Seçilen evcil hayvan bulunamadı", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Evcil hayvanlar yüklenemedi: ${e.message}")
                    Toast.makeText(this, "Evcil hayvanlar yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "loadPets genel hata: ${e.message}")
            Toast.makeText(this, "Evcil hayvanlar yüklenirken beklenmeyen hata: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupPetSpinner() {
        try {
            val petNames = pets.values.map { it.name }.toList()
            
            // Evcil hayvan listesi boşsa uyarı göster
            if (petNames.isEmpty()) {
                Toast.makeText(this, "Evcil hayvan bulunamadı", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Pet listesi boş - spinner kurulumu yapılamadı")
                return
            }
            
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                petNames
            )
            
            // Null güvenliği
            val petSpinner = binding.spinnerPet as? AutoCompleteTextView
            if (petSpinner == null) {
                Log.e(TAG, "spinnerPet AutoCompleteTextView olarak cast edilemedi")
                return
            }
            
            try {
                petSpinner.setAdapter(adapter)
                
                // Düzenleme modundaysa doğru evcil hayvanı seçelim
                if (isEditing && selectedPetId.isNotEmpty()) {
                    // Seçilen evcil hayvanın adını bul
                    val selectedPet = pets[selectedPetId]
                    if (selectedPet != null) {
                        Log.d(TAG, "Düzenleme modu - evcil hayvan seçildi: ${selectedPet.name}")
                        petSpinner.setText(selectedPet.name, false)
                    } else {
                        Log.e(TAG, "Seçilen evcil hayvan bulunamadı: $selectedPetId")
                        val firstPet = petNames.firstOrNull()
                        if (firstPet != null) {
                            petSpinner.setText(firstPet, false)
                            selectedPetId = pets.values.first().id
                        }
                    }
                } else {
                    // Yeni kayıt - ilk evcil hayvanı seç
                    val firstPet = petNames.firstOrNull()
                    if (firstPet != null) {
                        petSpinner.setText(firstPet, false)
                        selectedPetId = pets.values.first().id
                    }
                }
                
                // Dropdown öğesi seçildiğinde
                petSpinner.setOnItemClickListener { _, _, position, _ ->
                    try {
                        if (position >= 0 && position < pets.values.size) {
                            selectedPetId = pets.values.elementAt(position).id
                            Log.d(TAG, "Evcil hayvan seçildi: $selectedPetId")
                        } else {
                            Log.e(TAG, "Geçersiz pozisyon seçildi: $position, liste boyutu: ${pets.values.size}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Evcil hayvan seçilirken hata: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pet spinner ayarlanırken detaylı hata: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupPetSpinner hata: ${e.message}")
            Toast.makeText(this, "Evcil hayvan listesi ayarlanamadı", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupStatusSpinner() {
        try {
            val statusOptions = listOf(
                getString(R.string.active),
                getString(R.string.completed),
                getString(R.string.cancelled)
            )
            
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
            )
            
            val statusSpinner = binding.spinnerStatus as? AutoCompleteTextView
            if (statusSpinner == null) {
                Log.e(TAG, "spinnerStatus AutoCompleteTextView olarak cast edilemedi")
                return
            }
            
            statusSpinner.apply {
                setAdapter(adapter)
                setText(statusOptions.first(), false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupStatusSpinner hata: ${e.message}")
        }
    }
    
    private fun setupAddButtons() {
        // İlaç ekleme butonu
        binding.btnAddMedication.setOnClickListener {
            addMedication()
        }
        
        // Randevu ekleme butonu
        binding.btnAddAppointment.setOnClickListener {
            addAppointment()
        }
        
        // Belge ekleme butonu
        binding.btnAddDocument.setOnClickListener {
            addDocument()
        }
    }
    
    private fun setupMedicationsAdapter() {
        medicationsAdapter = MedicationAdapter(medications) { position ->
            // İlaç silme işlemi
            medications.removeAt(position)
            medicationsAdapter.notifyItemRemoved(position)
            updateMedicationsView()
        }
        
        binding.rvMedications.apply {
            adapter = medicationsAdapter
            layoutManager = LinearLayoutManager(this@AddTreatmentActivity)
        }
    }
    
    private fun setupAppointmentsAdapter() {
        appointmentsAdapter = AppointmentAdapter(vetAppointments) { position ->
            // Randevu silme işlemi
            vetAppointments.removeAt(position)
            appointmentsAdapter.notifyItemRemoved(position)
            updateAppointmentsView()
        }
        
        binding.rvAppointments.apply {
            adapter = appointmentsAdapter
            layoutManager = LinearLayoutManager(this@AddTreatmentActivity)
        }
    }
    
    private fun setupDocumentsAdapter() {
        documentsAdapter = DocumentAdapter(
            documents,
            onDeleteClick = { position ->
                // Belge silme işlemi
                documents.removeAt(position)
                documentsAdapter.notifyItemRemoved(position)
                updateDocumentsView()
            },
            onItemClick = { document ->
                // Belgeyi görüntüleme işlemi (gelecekte eklenecek)
                Toast.makeText(this, "Belge görüntüleme özelliği geliştirilecek", Toast.LENGTH_SHORT).show()
            }
        )
        
        binding.rvDocuments.apply {
            adapter = documentsAdapter
            layoutManager = LinearLayoutManager(this@AddTreatmentActivity)
        }
    }
    
    private fun addMedication() {
        val dialog = AddMedicationDialog.newInstance()
        dialog.onMedicationAdded = { medication ->
            medications.add(medication)
            medicationsAdapter.notifyItemInserted(medications.size - 1)
            updateMedicationsView()
        }
        dialog.show(supportFragmentManager, "AddMedicationDialog")
    }
    
    private fun updateMedicationsView() {
        if (medications.isEmpty()) {
            binding.tvNoMedications.visibility = View.VISIBLE
            binding.rvMedications.visibility = View.GONE
        } else {
            binding.tvNoMedications.visibility = View.GONE
            binding.rvMedications.visibility = View.VISIBLE
        }
    }
    
    private fun addAppointment() {
        val dialog = AddAppointmentDialog.newInstance()
        dialog.onAppointmentAdded = { appointment ->
            vetAppointments.add(appointment)
            appointmentsAdapter.notifyItemInserted(vetAppointments.size - 1)
            updateAppointmentsView()
        }
        dialog.show(supportFragmentManager, "AddAppointmentDialog")
    }
    
    private fun updateAppointmentsView() {
        if (vetAppointments.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.rvAppointments.visibility = View.GONE
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.rvAppointments.visibility = View.VISIBLE
        }
    }
    
    private fun addDocument() {
        val dialog = AddDocumentDialog.newInstance()
        dialog.onDocumentAdded = { document, uri ->
            // Belgeyi Firebase Storage'a yükle
            uploadDocument(document, uri)
        }
        dialog.show(supportFragmentManager, "AddDocumentDialog")
    }
    
    private fun uploadDocument(document: TreatmentDocument, uri: Uri) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Güvenli bir ID oluştur - UUID kullan
            document.id = java.util.UUID.randomUUID().toString()
            
            // Progress bar göster
            binding.progressBar.visibility = View.VISIBLE
            
            try {
                // İçerik çözümleyiciden stream al
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    try {
                        // Dosyayı bayt dizisine dönüştür
                        val bytes = inputStream.readBytes()
                        
                        // Dosya boyutu kontrolü - daha yüksek limit
                        if (bytes.size > 950000) { // Firestore 1MB limit altında
                            binding.progressBar.visibility = View.GONE
                            Log.e(TAG, "Dosya çok büyük: ${bytes.size} bytes")
                            Toast.makeText(this, "PDF dosyası çok büyük (maksimum 950KB). Lütfen daha küçük bir dosya seçin.", Toast.LENGTH_LONG).show()
                            return
                        } else if (bytes.size > 800000) {
                            // Dosya boyutu 800KB ile 950KB arasında ise uyarı göster
                            Toast.makeText(this, "Dosya boyutu ${bytes.size/1000}KB. Maksimum 1MB olabilir.", Toast.LENGTH_LONG).show()
                        }
                        
                        Log.d(TAG, "Dosya okundu, boyut: ${bytes.size} bytes")
                        
                        // Base64'e dönüştür
                        val base64Content = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        Log.d(TAG, "Base64 dönüşümü tamamlandı, uzunluk: ${base64Content.length} karakter")
                        
                        // Karakter kontrolü (Firestore limiti 1MB)
                        if (base64Content.length > 980000) {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Dosya çok büyük (base64 karakter sayısı: ${base64Content.length})", Toast.LENGTH_LONG).show()
                            return
                        }
                        
                        // Belge nesnesini güncelle
                        document.fileContent = base64Content
                        document.fileUrl = "base64://${document.id}" // Bu URL formatı belgenin base64 olduğunu gösterir
                        
                        // Aynı belgenin daha önce eklenip eklenmediğini kontrol et
                        val existingDoc = documents.find { it.name == document.name }
                        if (existingDoc != null) {
                            // Eğer aynı isimde belge varsa, güncelle
                            val index = documents.indexOf(existingDoc)
                            documents[index] = document
                            documentsAdapter.notifyItemChanged(index)
                        } else {
                            // Yeni belge ekle
                            documents.add(document)
                            documentsAdapter.notifyItemInserted(documents.size - 1)
                        }
                        
                        updateDocumentsView()
                        
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Belge başarıyla yüklendi", Toast.LENGTH_SHORT).show()
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "Bellek yetersiz hatası: ${e.message}", e)
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Dosya işlenemeyecek kadar büyük", Toast.LENGTH_LONG).show()
                    }
                } ?: run {
                    // InputStream null ise
                    Log.e(TAG, "Dosya açılamadı: URI=$uri")
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dosya işleme hatası: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Dosya işleme hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Belge yükleme hatası: ${e.message}", e)
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Beklenmeyen bir hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateDocumentsView() {
        if (documents.isEmpty()) {
            binding.tvNoDocuments.visibility = View.VISIBLE
            binding.rvDocuments.visibility = View.GONE
        } else {
            binding.tvNoDocuments.visibility = View.GONE
            binding.rvDocuments.visibility = View.VISIBLE
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Evcil hayvan kontrolü
        if (selectedPetId.isEmpty() || pets.isEmpty()) {
            Toast.makeText(this, "Lütfen bir evcil hayvan seçin", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        // Tedavi adı kontrolü
        if (TextUtils.isEmpty(binding.etTreatmentName.text)) {
            binding.etTreatmentName.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.etTreatmentName.error = null
        }
        
        // Başlangıç tarihi kontrolü
        if (selectedStartDate == Date(0)) {
            binding.etStartDate.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.etStartDate.error = null
        }
        
        return isValid
    }
    
    private fun saveTreatment() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (selectedPetId.isEmpty()) {
                Toast.makeText(this, "Lütfen bir evcil hayvan seçin", Toast.LENGTH_SHORT).show()
                return
            }
            
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false
            
            // Log bilgileri
            Log.d(TAG, "Tedavi kaydı başlatılıyor...")
            Log.d(TAG, "Kullanıcı ID: $userId")
            Log.d(TAG, "Evcil hayvan ID: $selectedPetId")
            Log.d(TAG, "İlaç sayısı: ${medications.size}")
            Log.d(TAG, "Randevu sayısı: ${vetAppointments.size}")
            Log.d(TAG, "Belge sayısı: ${documents.size}")
            
            // Durum değerini al
            val statusText = (binding.spinnerStatus as? AutoCompleteTextView)?.text?.toString() ?: getString(R.string.active)
            val status = when (statusText) {
                getString(R.string.active) -> TreatmentStatus.ACTIVE
                getString(R.string.completed) -> TreatmentStatus.COMPLETED
                getString(R.string.cancelled) -> TreatmentStatus.CANCELLED
                else -> TreatmentStatus.ACTIVE
            }
            
            Log.d(TAG, "Tedavi durumu: $status")
            
            // Ana tedavi verileri
            val treatmentData = HashMap<String, Any?>()
            treatmentData["name"] = binding.etTreatmentName.text?.toString()?.trim() ?: ""
            treatmentData["petId"] = selectedPetId
            treatmentData["startDate"] = selectedStartDate
            treatmentData["endDate"] = selectedEndDate
            treatmentData["status"] = status.toString()
            treatmentData["notes"] = binding.etNotes.text?.toString()?.trim() ?: ""
            treatmentData["hasDocuments"] = (documents.isNotEmpty())
            
            // Düzenleme modunda güncel tarihi koruyan fakat yeni kayıtta createdAt ekleyen kontrol
            if (!isEditing) {
                treatmentData["createdAt"] = Timestamp(Date().time / 1000, ((Date().time % 1000) * 1000000).toInt())
            }
            
            // İlaçlar listesi
            val medicationsList = ArrayList<HashMap<String, Any?>>()
            for (medication in medications) {
                val medMap = HashMap<String, Any?>()
                medMap["id"] = medication.id
                medMap["name"] = medication.name
                medMap["dosage"] = medication.dosage
                medMap["frequency"] = medication.frequency
                medMap["startDate"] = medication.startDate
                medMap["endDate"] = medication.endDate
                medMap["reminderTime"] = medication.reminderTime
                medMap["notes"] = medication.notes
                medicationsList.add(medMap)
            }
            
            // Randevular listesi
            val appointmentsList = ArrayList<HashMap<String, Any?>>()
            for (appointment in vetAppointments) {
                val appMap = HashMap<String, Any?>()
                appMap["id"] = appointment.id
                appMap["clinicName"] = appointment.clinicName
                appMap["date"] = appointment.date
                appMap["notes"] = appointment.notes
                appMap["status"] = appointment.status.toString()
                appointmentsList.add(appMap)
            }
            
            // Ana veri modeline listeleri ekle
            treatmentData["medications"] = medicationsList
            treatmentData["vetAppointments"] = appointmentsList
            
            Log.d(TAG, "Tedavi verisi hazırlandı")
            Log.d(TAG, "Kayıt yolu: users/$userId/pets/$selectedPetId/treatments")
            
            // Path doğruluğunu kontrol et
            val petDocRef = firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(selectedPetId)
            
            // Önce pet dokümanının var olduğunu kontrol et
            petDocRef.get()
                .addOnSuccessListener { petDocument ->
                    if (petDocument.exists()) {
                        Log.d(TAG, "Evcil hayvan dokümanı bulundu, tedavi kaydı devam ediyor")
                        
                        if (isEditing) {
                            // Mevcut tedaviyi güncelle
                            petDocRef.collection("treatments")
                                .document(editTreatmentId)
                                .update(treatmentData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Tedavi güncellendi: $editTreatmentId")
                                    
                                    // Belgeler varsa belge kayıt işlemi
                                    if (documents.isNotEmpty()) {
                                        saveDocuments(userId, selectedPetId, editTreatmentId)
                                    } else {
                                        // Belge yoksa hasDocuments = false olarak güncelle
                                        petDocRef.collection("treatments")
                                            .document(editTreatmentId)
                                            .update("hasDocuments", false)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "hasDocuments alanı false olarak güncellendi")
                                                binding.progressBar.visibility = View.GONE
                                                binding.btnSave.isEnabled = true
                                                Toast.makeText(this, "Tedavi güncellendi!", Toast.LENGTH_SHORT).show()
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "hasDocuments alanı güncellenemedi: ${e.message}")
                                                binding.progressBar.visibility = View.GONE
                                                binding.btnSave.isEnabled = true
                                                Toast.makeText(this, "Tedavi güncellendi!", Toast.LENGTH_SHORT).show()
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    binding.progressBar.visibility = View.GONE
                                    binding.btnSave.isEnabled = true
                                    Log.e(TAG, "Tedavi güncelleme hatası: ${e.message}", e)
                                    Toast.makeText(this, "Tedavi güncellenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            // Yeni tedavi ekle
                            petDocRef.collection("treatments")
                                .add(treatmentData)
                                .addOnSuccessListener { documentReference ->
                                    val treatmentId = documentReference.id
                                    Log.d(TAG, "Tedavi kaydı başarılı, ID: $treatmentId")
                                    
                                    // Tedavi ID'si güncellemesi
                                    documentReference.update("id", treatmentId)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Tedavi ID güncellendi: $treatmentId")
                                            
                                            // Belgeler varsa belge kayıt işlemi
                                            if (documents.isNotEmpty()) {
                                                saveDocuments(userId, selectedPetId, treatmentId)
                                            } else {
                                                binding.progressBar.visibility = View.GONE
                                                binding.btnSave.isEnabled = true
                                                Toast.makeText(this, "Tedavi başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Tedavi ID güncellemesi başarısız: ${e.message}", e)
                                            // Yine de başarılı kabul et
                                            binding.progressBar.visibility = View.GONE
                                            binding.btnSave.isEnabled = true
                                            Toast.makeText(this, "Tedavi kaydedildi, ancak ID güncellenemedi", Toast.LENGTH_SHORT).show()
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    binding.progressBar.visibility = View.GONE
                                    binding.btnSave.isEnabled = true
                                    Log.e(TAG, "Tedavi kaydetme hatası: ${e.message}", e)
                                    
                                    // Daha fazla tanılama bilgisi
                                    when (e) {
                                        is com.google.firebase.firestore.FirebaseFirestoreException -> {
                                            Log.e(TAG, "Firebase Exception: ${e.code} - ${e.message}")
                                        }
                                        else -> {
                                            Log.e(TAG, "Genel hata: ${e.javaClass.simpleName} - ${e.message}")
                                        }
                                    }
                                    
                                    Toast.makeText(this, "Tedavi kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Log.e(TAG, "Evcil hayvan bulunamadı: $selectedPetId")
                        Toast.makeText(this, "Evcil hayvan bulunamadı: ID=$selectedPetId", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Log.e(TAG, "Evcil hayvan kontrolü başarısız: ${e.message}", e)
                    Toast.makeText(this, "Evcil hayvan kontrolü başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
            Log.e(TAG, "saveTreatment genel hata: ${e.message}", e)
            Log.e(TAG, "Hata detayı:", e)
            Toast.makeText(this, "Tedavi kaydetme sırasında bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun saveDocuments(userId: String, petId: String, treatmentId: String) {
        Log.d(TAG, "Tedavi belgeleri kaydediliyor... Belge sayısı: ${documents.size}")
        
        if (documents.isEmpty()) {
            // Belge yoksa ana aktiviteye dön
            Log.d(TAG, "Belgeler boş, aktivite sonlandırılıyor")
            Toast.makeText(this, getString(R.string.treatment_saved), Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        try {
            Log.d(TAG, "Firestore yolu: users/$userId/pets/$petId/treatments/$treatmentId")
            
            // Ana tedavi belgesine hasDocuments alanı zaten eklendi, şimdi belgeleri ekleyelim
            val treatmentRef = firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId)
                .collection("treatments")
                .document(treatmentId)
            
            // Önce tedavi belgesinin var olduğunu doğrula
            treatmentRef.get().addOnSuccessListener { treatmentDoc ->
                if (!treatmentDoc.exists()) {
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Tedavi belgesi bulunamadı: users/$userId/pets/$petId/treatments/$treatmentId")
                    Toast.makeText(this, "Tedavi belgesi bulunamadığı için belge kaydedilemedi", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                    return@addOnSuccessListener
                }
                
                // hasDocuments değerini true olarak ayarla
                treatmentRef.update("hasDocuments", true)
                    .addOnSuccessListener {
                        Log.d(TAG, "hasDocuments alanı başarıyla güncellendi")
                        
                        // Düzenleme modunda önce mevcut belgeler koleksiyonunu temizle ve sonra yeniden ekle
                        if (isEditing) {
                            // Mevcut belge koleksiyonunu kontrol et
                            val documentsRef = treatmentRef.collection("documents")
                            
                            documentsRef.get()
                                .addOnSuccessListener { existingDocuments ->
                                    Log.d(TAG, "Mevcut ${existingDocuments.size()} belge bulundu")
                                    
                                    // Eğer herhangi bir belge varsa önce onları temizleyelim
                                    if (existingDocuments.isEmpty) {
                                        // Silecek belge yoksa direkt kaydet
                                        saveRealDocuments(userId, petId, treatmentId)
                                        return@addOnSuccessListener
                                    }
                                    
                                    // Mevcut belgeleri işaretle
                                    val existingDocIds = existingDocuments.documents
                                        .filter { !it.getBoolean("isTest")!! ?: false }
                                        .map { it.getString("id") ?: "" }
                                        .toSet()
                                    
                                    // documents listesindeki belgeleri güncelle
                                    documents.forEach { doc ->
                                        if (existingDocIds.contains(doc.id)) {
                                            doc.extraProperties["isExistingDocument"] = true
                                        }
                                    }
                                    
                                    // Artık saveRealDocuments'ı çağırabiliriz
                                    saveRealDocuments(userId, petId, treatmentId)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Mevcut belgeler kontrol edilemedi: ${e.message}")
                                    // Hata olsa da yeni belgeleri kaydetmeye çalış
                                    saveRealDocuments(userId, petId, treatmentId)
                                }
                        } else {
                            // Düzenleme modu değilse, doğrudan saveRealDocuments'ı çağır
                            saveRealDocuments(userId, petId, treatmentId)
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Log.e(TAG, "hasDocuments alanı güncellenemedi: ${e.message}", e)
                        
                        when (e) {
                            is com.google.firebase.firestore.FirebaseFirestoreException -> {
                                Log.e(TAG, "Firestore Exception: ${e.code} - ${e.message}")
                                Toast.makeText(this, "Firestore hatası: ${e.code}", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this, "Tedavi kaydedildi fakat hasDocuments alanı güncellenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        
                        setResult(RESULT_OK)
                        finish()
                    }
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Tedavi belgesi kontrol edilemedi: ${e.message}", e)
                Toast.makeText(this, "Tedavi belgesi kontrol edilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "Belge kaydetme genel hatası: ${e.message}", e)
            Toast.makeText(this, "Tedavi kaydedildi fakat belgeler kaydedilemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
    
    private fun saveRealDocuments(userId: String, petId: String, treatmentId: String) {
        // Yerel takip için başarı ve hata sayılarını sıfırla
        var successCount = 0
        var errorCount = 0
        val totalDocuments = documents.size
        var processedCount = 0

        // Sadece yeni eklenen belgeleri ve güncellenen belgeleri işle
        val documentsToProcess = documents.filter { doc -> 
            doc.extraProperties["isExistingDocument"] != true || 
            doc.extraProperties["isUpdated"] == true 
        }
        
        Log.d(TAG, "İşlenecek belge sayısı: ${documentsToProcess.size} (toplam belge: $totalDocuments)")
        
        if (documentsToProcess.isEmpty()) {
            Log.d(TAG, "İşlenecek belge yok, işlem tamamlandı")
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Tedavi başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            return
        }

        // Belge koleksiyonu referansını al
        val documentsRef = firestore.collection("users")
            .document(userId)
            .collection("pets")
            .document(petId)
            .collection("treatments")
            .document(treatmentId)
            .collection("documents")

        // Her bir belgeyi Firestore'a kaydet
        for (document in documentsToProcess) {
            // Belge verilerini oluştur
            val docData = HashMap<String, Any>()
            
            // Ana belge alanları
            docData["id"] = document.id
            docData["name"] = document.name
            docData["type"] = document.type
            docData["uploadDate"] = Timestamp(document.uploadDate.time / 1000, ((document.uploadDate.time % 1000) * 1000000).toInt())
            docData["fileUrl"] = document.fileUrl
            docData["isTest"] = false

            // Eğer belgenin içeriği varsa ekle
            if (document.fileContent.isNotEmpty()) {
                docData["fileContent"] = document.fileContent
            }

            // extraProperties alanlarını ekle (isExistingDocument ve isUpdated hariç)
            if (document.extraProperties.isNotEmpty()) {
                try {
                    for ((key, value) in document.extraProperties) {
                        if (key != "isExistingDocument" && key != "isUpdated") {
                            when (value) {
                                is String, is Boolean, is Number, is Map<*, *>, is List<*>, is Timestamp, is Date -> {
                                    if (value is Date) {
                                        docData[key] = Timestamp(value.time / 1000, ((value.time % 1000) * 1000000).toInt())
                                    } else {
                                        docData[key] = value
                                    }
                                }
                                else -> {
                                    Log.d(TAG, "Desteklenmeyen tip: $key - ${value?.javaClass?.name}")
                                    docData[key] = value.toString()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "extraProperties işlenirken hata: ${e.message}")
                }
            }

            // Belgeyi Firestore'a kaydet
            documentsRef.document(document.id).set(docData)
                .addOnSuccessListener {
                    successCount++
                    processedCount++
                    Log.d(TAG, "Belge başarıyla kaydedildi: ${document.id} - ${document.name}")

                    // Tüm belgeler işlendiyse UI'ı güncelle
                    if (processedCount == documentsToProcess.size) {
                        runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            val message = if (errorCount > 0) {
                                "$successCount belge kaydedildi, $errorCount hata oluştu"
                            } else {
                                "Tüm belgeler başarıyla kaydedildi ($successCount)"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Belge kayıt işlemi tamamlandı: $message")
                            
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    errorCount++
                    processedCount++
                    Log.e(TAG, "Belge kaydedilirken hata: ${document.id} - ${e.message}")

                    // Tüm belgeler işlendiyse UI'ı güncelle
                    if (processedCount == documentsToProcess.size) {
                        runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            val message = if (successCount > 0) {
                                "$successCount belge kaydedildi, $errorCount hata oluştu"
                            } else {
                                "Belge kayıt hatası: ${e.message}"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Belge kayıt işlemi tamamlandı: $message")
                            
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }
        }
    }
    
    // onResume metodu - activity yeniden ön plana geldiğinde çalışır
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume çağrıldı - Activity ön planda")
        
        // Eğer düzenleme modundaysa ve tedavi adı boşsa, verileri yükle
        if (isEditing && binding.etTreatmentName.text.toString().isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false
            
            // Tedavi verilerini yükle
            loadTreatmentData()
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
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Belge seçimi sonuçlarını işle
        if (requestCode == REQUEST_CODE_PICK_DOCUMENT && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Seçilen belgeyi işle
                val document = TreatmentDocument(
                    id = java.util.UUID.randomUUID().toString(),
                    name = getFileNameFromUri(uri) ?: "Belge",
                    type = getMimeType(uri) ?: "application/octet-stream",
                    uploadDate = Date(),
                    fileUrl = "",
                    fileContent = ""
                )
                uploadDocument(document, uri)
            }
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex("_display_name")
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
    
    private fun getMimeType(uri: Uri): String? {
        return contentResolver.getType(uri)
    }
} 