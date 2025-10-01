package com.tamer.petapp.treatment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.databinding.ActivityTreatmentDetailsBinding
import com.tamer.petapp.model.Treatment
import com.tamer.petapp.model.TreatmentStatus
import com.tamer.petapp.model.Medication
import com.tamer.petapp.model.VetAppointment
import com.tamer.petapp.model.TreatmentDocument
import com.tamer.petapp.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.File
import java.util.Date
import com.tamer.petapp.R
import com.tamer.petapp.adapter.MedicationAdapter
import com.tamer.petapp.adapter.AppointmentAdapter
import com.tamer.petapp.adapter.DocumentAdapter

class TreatmentDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTreatmentDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var treatmentId: String? = null
    private var petId: String? = null
    
    private val TAG = "TreatmentDetailsActivity"
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private lateinit var adapter: MedicationAdapter
    private lateinit var appointmentsAdapter: AppointmentAdapter
    private lateinit var documentsAdapter: DocumentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTreatmentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase bileşenlerini başlat
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Intent'ten tedavi ve evcil hayvan ID'lerini al
        treatmentId = intent.getStringExtra("treatmentId")
        petId = intent.getStringExtra("petId")
        
        Log.d(TAG, "Tedavi ID: $treatmentId, Pet ID: $petId")

        // Toolbar ayarları
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tedavi Detayları"
        
        // Progress bar'ı göster
        binding.progressBar.visibility = View.VISIBLE

        // Düzenleme butonu
        binding.fabEditTreatment.setOnClickListener {
            if (treatmentId != null && petId != null) {
                // Düzenleme ekranına git
                val intent = Intent(this, AddTreatmentActivity::class.java)
                intent.putExtra("isEditing", true)
                intent.putExtra("treatmentId", treatmentId)
                intent.putExtra("petId", petId)
                startActivityForResult(intent, EDIT_TREATMENT_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Tedavi bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }

        // Tedavi detaylarını yükle
        loadTreatmentDetails()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && requestCode == EDIT_TREATMENT_REQUEST_CODE) {
            // Tedavi düzenlendikten sonra detayları yeniden yükle
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollContent.visibility = View.GONE
            loadTreatmentDetails()
        }
    }

    private fun loadTreatmentDetails() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Kullanıcı kimliği bulunamadı", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.tvNoTreatment.visibility = View.VISIBLE
            return
        }
        
        treatmentId?.let { id ->
            if (petId != null) {
                // PetId biliniyorsa direkt o evcil hayvanın tedavisini yükle
                loadTreatmentFromPet(userId, petId!!, id)
            } else {
                // PetId bilinmiyorsa tüm evcil hayvanları tara
                loadTreatmentFromAllPets(userId, id)
            }
        } ?: run {
            // Tedavi ID'si yoksa hata mesajı göster
            binding.progressBar.visibility = View.GONE
            binding.tvNoTreatment.visibility = View.VISIBLE
            Log.e(TAG, "Tedavi ID'si null")
        }
    }
    
    private fun loadTreatmentFromPet(userId: String, petId: String, treatmentId: String) {
        Log.d(TAG, "Tedavi yükleniyor - Path: users/$userId/pets/$petId/treatments/$treatmentId")
        
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .document(petId)
            .collection("treatments")
            .document(treatmentId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                
                if (document.exists()) {
                    Log.d(TAG, "Tedavi belge verileri: ${document.data}")
                    
                    val treatment = document.toObject(Treatment::class.java)
                    if (treatment != null) {
                        treatment.id = document.id
                        treatment.petId = petId
                        displayTreatmentDetails(treatment)
                    } else {
                        Log.e(TAG, "Tedavi nesnesi dönüştürülemedi")
                        binding.tvNoTreatment.visibility = View.VISIBLE
                    }
                } else {
                    Log.e(TAG, "Tedavi belgesi bulunamadı")
                    binding.tvNoTreatment.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoTreatment.visibility = View.VISIBLE
                Log.e(TAG, "Tedavi yükleme hatası: ${e.message}", e)
                Toast.makeText(this, "Tedavi yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadTreatmentFromAllPets(userId: String, treatmentId: String) {
        Log.d(TAG, "Tüm evcil hayvanlardan tedavi aranıyor - UserId: $userId, TreatmentId: $treatmentId")
        
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .get()
            .addOnSuccessListener { petsSnapshot ->
                if (petsSnapshot.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoTreatment.visibility = View.VISIBLE
                    Log.d(TAG, "Hiç evcil hayvan bulunamadı")
                    return@addOnSuccessListener
                }
                
                var treatmentFound = false
                var petCount = 0
                
                for (petDoc in petsSnapshot.documents) {
                    val petId = petDoc.id
                    Log.d(TAG, "Evcil hayvan kontrol ediliyor - PetId: $petId")
                    
                    firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .document(petId)
                        .collection("treatments")
                        .document(treatmentId)
                        .get()
                        .addOnSuccessListener { treatmentDoc ->
                            petCount++
                            
                            if (treatmentDoc.exists()) {
                                if (!treatmentFound) {
                                    treatmentFound = true
                                    Log.d(TAG, "Tedavi bulundu - PetId: $petId")
                                    Log.d(TAG, "Tedavi verileri: ${treatmentDoc.data}")
                                    
                                    val treatment = treatmentDoc.toObject(Treatment::class.java)
                                    treatment?.id = treatmentDoc.id
                                    treatment?.petId = petId
                                    
                                    treatment?.let { 
                                        displayTreatmentDetails(it)
                                    }
                                }
                            }
                            
                            if (petCount == petsSnapshot.size() && !treatmentFound) {
                                binding.progressBar.visibility = View.GONE
                                binding.tvNoTreatment.visibility = View.VISIBLE
                                Log.d(TAG, "Hiçbir evcil hayvanda tedavi bulunamadı")
                            }
                        }
                        .addOnFailureListener { e ->
                            petCount++
                            Log.e(TAG, "Tedavi arama hatası - PetId: $petId, Hata: ${e.message}")
                            
                            if (petCount == petsSnapshot.size() && !treatmentFound) {
                                binding.progressBar.visibility = View.GONE
                                binding.tvNoTreatment.visibility = View.VISIBLE
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoTreatment.visibility = View.VISIBLE
                Log.e(TAG, "Evcil hayvanları yükleme hatası: ${e.message}", e)
            }
    }

    private fun displayTreatmentDetails(treatment: Treatment) {
        Log.d(TAG, "Tedavi detayları gösteriliyor: $treatment")
        
        binding.progressBar.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE
        
        binding.apply {
            tvTreatmentName.text = treatment.name
            tvTreatmentDate.text = dateFormat.format(treatment.startDate)
            tvTreatmentNotes.text = treatment.notes.ifEmpty { "Belirtilmedi" }
            
            // Status değerine göre metin atama
            val statusText = when (treatment.status) {
                TreatmentStatus.ACTIVE -> "Aktif"
                TreatmentStatus.COMPLETED -> "Tamamlandı"
                TreatmentStatus.CANCELLED -> "İptal Edildi"
            }
            tvTreatmentStatus.text = statusText
            
            // Vet klinik adını VetAppointment'tan alalım, eğer varsa
            val vetName = if (treatment.vetAppointments.isNotEmpty()) {
                treatment.vetAppointments[0].clinicName
            } else {
                "Belirtilmedi"
            }
            tvTreatmentVet.text = vetName
            
            // Maliyet bilgisi Treatment sınıfında bulunmuyor, onu kaldıralım
            tvTreatmentCost.visibility = View.GONE
            
            // İlaçları göster
            displayMedications(treatment.medications)
            
            // Randevuları göster
            displayAppointments(treatment.vetAppointments)
            
            // Belgeleri yükle
            loadDocuments(treatment.id)
        }
    }
    
    private fun displayMedications(medications: List<Medication>) {
        if (medications.isEmpty()) {
            binding.tvNoMedications.visibility = View.VISIBLE
            binding.rvMedications.visibility = View.GONE
        } else {
            binding.tvNoMedications.visibility = View.GONE
            binding.rvMedications.visibility = View.VISIBLE
            
            // TODO: RecyclerView adapter'ı ile ilaçları listele
            // Şimdilik sadece text olarak gösterelim
            val medicationText = medications.joinToString(separator = "\n\n") { med ->
                """
                |${med.name}
                |Doz: ${med.dosage}
                |Sıklık: ${med.frequency}
                |Başlangıç: ${dateFormat.format(med.startDate)}
                """.trimMargin()
            }
            
            binding.tvNoMedications.text = medicationText
            binding.tvNoMedications.visibility = View.VISIBLE
        }
    }
    
    private fun displayAppointments(appointments: List<VetAppointment>) {
        if (appointments.isEmpty()) {
            binding.tvNoAppointments.visibility = View.VISIBLE
            binding.rvAppointments.visibility = View.GONE
        } else {
            binding.tvNoAppointments.visibility = View.GONE
            binding.rvAppointments.visibility = View.VISIBLE
            
            // TODO: RecyclerView adapter'ı ile randevuları listele
            // Şimdilik sadece text olarak gösterelim
            val appointmentText = appointments.joinToString(separator = "\n\n") { app ->
                """
                |${app.clinicName}
                |Tarih: ${dateFormat.format(app.date)}
                |Durum: ${app.status}
                |${if (app.notes.isNotEmpty()) "Not: ${app.notes}" else ""}
                """.trimMargin()
            }
            
            binding.tvNoAppointments.text = appointmentText
            binding.tvNoAppointments.visibility = View.VISIBLE
        }
    }
    
    private fun loadDocuments(treatmentId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (petId == null) return
        
        try {
            Log.d(TAG, "Tedavi belgeleri yükleniyor - Yol: users/$userId/pets/$petId/treatments/$treatmentId/documents")
            
            // Önce tedavi belgesini kontrol et
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId!!)
                .collection("treatments")
                .document(treatmentId)
                .get()
                .addOnSuccessListener { treatmentDoc ->
                    if (!treatmentDoc.exists()) {
                        Log.e(TAG, "Tedavi belgesi bulunamadı")
                        binding.tvNoDocuments.visibility = View.VISIBLE
                        binding.rvDocuments.visibility = View.GONE
                        return@addOnSuccessListener
                    }
                    
                    // hasDocuments alanını kontrol et
                    val hasDocuments = treatmentDoc.getBoolean("hasDocuments") ?: false
                    Log.d(TAG, "Tedavi hasDocuments değeri: $hasDocuments")
                    
                    if (!hasDocuments) {
                        Log.d(TAG, "Belge işareti (hasDocuments) yok, belge olmadığı varsayılıyor")
                        binding.tvNoDocuments.visibility = View.VISIBLE
                        binding.rvDocuments.visibility = View.GONE
                        return@addOnSuccessListener
                    }
                    
                    // Belgeleri yükle
                    firestore.collection("users")
                        .document(userId)
                        .collection("pets")
                        .document(petId!!)
                        .collection("treatments")
                        .document(treatmentId)
                        .collection("documents")
                        .get()
                        .addOnSuccessListener { documentsSnapshot ->
                            try {
                                if (documentsSnapshot.isEmpty) {
                                    Log.d(TAG, "Belge koleksiyonu boş")
                                    binding.tvNoDocuments.visibility = View.VISIBLE
                                    binding.rvDocuments.visibility = View.GONE
                                    return@addOnSuccessListener
                                }
                                
                                // Önce test belgesini hariç tut
                                val realDocuments = documentsSnapshot.documents.filter { 
                                    !(it.id == "test_doc" || it.getBoolean("isTest") == true) 
                                }
                                
                                Log.d(TAG, "Test belgesi filtrelendikten sonra belge sayısı: ${realDocuments.size}")
                                
                                if (realDocuments.isEmpty()) {
                                    Log.d(TAG, "Gerçek belge bulunamadı, sadece test belgesi var")
                                    binding.tvNoDocuments.visibility = View.VISIBLE
                                    binding.rvDocuments.visibility = View.GONE
                                    return@addOnSuccessListener
                                }
                                
                                val documents = mutableListOf<TreatmentDocument>()
                                
                                for (doc in realDocuments) {
                                    try {
                                        Log.d(TAG, "Belge işleniyor: ${doc.id}")
                                        
                                        // Manuel nesne oluştur
                                        val documentId = doc.id
                                        val name = doc.getString("name") ?: "Adsız Belge"
                                        val type = doc.getString("type") ?: "Bilinmeyen Tür"
                                        val uploadDate = doc.getTimestamp("uploadDate")?.toDate() ?: Date()
                                        val fileUrl = doc.getString("fileUrl") ?: ""
                                        
                                        // Belge içeriğini güvenli şekilde al
                                        var fileContent = ""
                                        try {
                                            fileContent = doc.getString("fileContent") ?: ""
                                        } catch (e: Exception) {
                                            Log.e(TAG, "fileContent alanı alınamadı: ${e.message}")
                                        }
                                        
                                        val document = TreatmentDocument(
                                            id = documentId,
                                            name = name,
                                            type = type,
                                            uploadDate = uploadDate,
                                            fileUrl = fileUrl,
                                            fileContent = fileContent
                                        )
                                        
                                        documents.add(document)
                                        Log.d(TAG, "Belge eklendi: $documentId, $name")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Belge işlenirken hata: ${e.message}")
                                    }
                                }
                                
                                if (documents.isEmpty()) {
                                    binding.tvNoDocuments.visibility = View.VISIBLE
                                    binding.rvDocuments.visibility = View.GONE
                                    Log.d(TAG, "İşlenebilen belge bulunamadı")
                                } else {
                                    Log.d(TAG, "Toplam ${documents.size} belge görüntülenecek")
                                    
                                    // Tedavi belgesinde hasDocuments alanını güncelle
                                    updateHasDocuments(userId, petId!!, treatmentId, true)
                                    
                                    displayDocuments(documents)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Belge yükleme işlemesinde genel hata: ${e.message}", e)
                                binding.tvNoDocuments.visibility = View.VISIBLE
                                binding.rvDocuments.visibility = View.GONE
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Belgeler yüklenirken hata: ${e.message}")
                            binding.tvNoDocuments.visibility = View.VISIBLE
                            binding.rvDocuments.visibility = View.GONE
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Tedavi belgesi kontrol edilirken hata: ${e.message}")
                    binding.tvNoDocuments.visibility = View.VISIBLE
                    binding.rvDocuments.visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e(TAG, "loadDocuments genel hatası: ${e.message}", e)
            binding.tvNoDocuments.visibility = View.VISIBLE
            binding.rvDocuments.visibility = View.GONE
        }
    }
    
    private fun displayDocuments(documents: List<TreatmentDocument>) {
        try {
            if (documents.isEmpty()) {
                binding.tvNoDocuments.visibility = View.VISIBLE
                binding.rvDocuments.visibility = View.GONE
                return
            }
            
            binding.tvNoDocuments.visibility = View.GONE
            binding.rvDocuments.visibility = View.VISIBLE
            
            // Belge Adapter'ını ayarla
            documentsAdapter = DocumentAdapter(
                documents,
                onDeleteClick = { _ -> /* Silme işlemi yok */ },
                onItemClick = { document ->
                    try {
                        Log.d(TAG, "Belgeye tıklandı: ${document.id}, URL: ${document.fileUrl}")
                        
                        // Belge tıklandığında görüntüleme işlemi
                        if (document.fileUrl.startsWith("base64://")) {
                            // Base64 formatında ise
                            if (document.fileContent.isNotEmpty()) {
                                try {
                                    // Geçici dosya oluştur
                                    val tempFile = File(cacheDir, "${document.id}.pdf")
                                    
                                    // Base64'den byte array'e dönüştür
                                    val bytes = android.util.Base64.decode(document.fileContent, android.util.Base64.DEFAULT)
                                    
                                    // Byte array'i dosyaya yaz
                                    tempFile.writeBytes(bytes)
                                    Log.d(TAG, "PDF dosyası oluşturuldu: ${tempFile.absolutePath}, Boyut: ${tempFile.length()} bytes")
                                    
                                    // Intent ile dosyayı aç
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this,
                                        "${applicationContext.packageName}.fileprovider",
                                        tempFile
                                    )
                                    intent.setDataAndType(uri, "application/pdf")
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    
                                    try {
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "PDF açılamadı: ${e.message}", e)
                                        Toast.makeText(this, "PDF görüntüleyici bulunamadı", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Base64 dönüştürme hatası: ${e.message}", e)
                                    Toast.makeText(this, "Belge açılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.w(TAG, "Belgenin base64 içeriği boş")
                                Toast.makeText(this, "Belge içeriği bulunamadı", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Diğer URL tipleri için (normal url veya yerel dosya)
                            Log.d(TAG, "Belge URL ile açılıyor: ${document.fileUrl}")
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setData(Uri.parse(document.fileUrl))
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "URL açılamadı: ${e.message}", e)
                                Toast.makeText(this, "Belge açılamadı", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Belge açılırken genel hata: ${e.message}", e)
                        Toast.makeText(this, "Belge açılırken bir hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            
            binding.rvDocuments.adapter = documentsAdapter
            binding.rvDocuments.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "displayDocuments genel hatası: ${e.message}", e)
            binding.tvNoDocuments.visibility = View.VISIBLE
            binding.rvDocuments.visibility = View.GONE
        }
    }

    // Tedavi belgesinin hasDocuments alanını güncelleyen yeni fonksiyon
    private fun updateHasDocuments(userId: String, petId: String, treatmentId: String, hasDocuments: Boolean) {
        try {
            Log.d(TAG, "hasDocuments güncellemesi: users/$userId/pets/$petId/treatments/$treatmentId, değer: $hasDocuments")
            
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId)
                .collection("treatments")
                .document(treatmentId)
                .update("hasDocuments", hasDocuments)
                .addOnSuccessListener {
                    Log.d(TAG, "hasDocuments alanı başarıyla güncellendi: $hasDocuments")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "hasDocuments güncellenemedi: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "updateHasDocuments hatası: ${e.message}", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EDIT_TREATMENT_REQUEST_CODE = 100
    }
} 