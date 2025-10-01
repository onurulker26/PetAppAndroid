package com.tamer.petapp.vaccination

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityAddVaccinationBinding
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Vaccination
import com.tamer.petapp.model.VaccinationStatus
import com.tamer.petapp.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddVaccinationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddVaccinationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var notificationHelper: NotificationHelper
    private var selectedDate: Date = Date()
    private var selectedNextDate: Date? = null
    private val pets = mutableMapOf<String, Pet>()
    private val TAG = "AddVaccinationActivity"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    private var isEditMode = false
    private var vaccinationId = ""
    private var selectedPetId = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupNotifications()
        } else {
            Toast.makeText(this, "Bildirim izni olmadan hatırlatmaları alamayacaksınız", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVaccinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase başlatma - En başta yapılmalı
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // NotificationHelper başlatma
        notificationHelper = NotificationHelper(this)
        
        // Bildirim iznini kontrol et
        checkNotificationPermission()

        // Edit modu kontrolü
        isEditMode = intent.getBooleanExtra("is_edit_mode", false)
        
        if (isEditMode) {
            vaccinationId = intent.getStringExtra("vaccination_id") ?: ""
            selectedPetId = intent.getStringExtra("pet_id") ?: ""
            
            // Gerekli parametrelerin kontrolü
            if (vaccinationId.isEmpty() || selectedPetId.isEmpty()) {
                Toast.makeText(this, "Aşı bilgileri eksik, düzenleme yapılamıyor", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Edit mode: Eksik parametreler - vaccinationId: '$vaccinationId', selectedPetId: '$selectedPetId'")
                finish()
                return
            }
            
            // Başlık ve buton metnini değiştir
            binding.btnSave.text = "Güncelle"
            
            Log.d(TAG, "Edit mode başlatılıyor - vaccinationId: $vaccinationId, selectedPetId: $selectedPetId")
        } else {
            selectedPetId = intent.getStringExtra("pet_id") ?: ""
        }
        
        // Support Action Bar ayarla
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (isEditMode) "Aşı Düzenle" else "Aşı Ekle"
        }
        
        // Tarih seçicileri ayarla
        setupDatePickers()
        
        // Evcil hayvan spinner'ını ayarla
        loadPets()
        
        // Kaydet butonu
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                if (isEditMode) {
                    updateVaccination()
                } else {
                    saveVaccination()
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş, aşıyı kaydet ve bildirimleri ayarla
                    setupNotifications()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // İzin daha önce reddedilmiş, kullanıcıya açıklama göster
                    Toast.makeText(
                        this,
                        "Aşı hatırlatmaları için bildirim iznine ihtiyacımız var",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // İlk kez izin isteniyor
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13'ten düşük sürümler için direkt kaydet
            setupNotifications()
        }
    }

    private fun setupNotifications() {
        notificationHelper.createNotificationChannel()
        notificationHelper.scheduleDaily()
    }
    
    private fun setupDatePickers() {
        binding.etDate.setOnClickListener { showDatePicker(true) }
        binding.etNextDate.setOnClickListener { showDatePicker(false) }
        
        // Set current date as default
        binding.etDate.setText(dateFormat.format(selectedDate))
    }
    
    private fun showDatePicker(isVaccinationDate: Boolean) {
        val calendar = Calendar.getInstance()
        val currentDate = if (isVaccinationDate) selectedDate else selectedNextDate ?: Date()
        calendar.time = currentDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val date = calendar.time
                val formattedDate = dateFormat.format(date)
                
                if (isVaccinationDate) {
                    selectedDate = date
                    binding.etDate.setText(formattedDate)
                } else {
                    selectedNextDate = date
                    binding.etNextDate.setText(formattedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun loadPets() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .get()
            .addOnSuccessListener { documents ->
                pets.clear()
                for (document in documents) {
                    val pet = document.toObject(Pet::class.java)
                    pet.id = document.id
                    pets[document.id] = pet
                }
                setupPetSpinner()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Evcil hayvanlar yüklenemedi: ${e.message}")
                Toast.makeText(this, "Evcil hayvanlar yüklenemedi", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }
    
    private fun setupPetSpinner() {
        val petNames = pets.values.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPet.adapter = adapter
        
        // Edit modunda veya önceden seçilmiş pet varsa spinnerda seç
        if (selectedPetId.isNotEmpty()) {
            val petIndex = pets.values.indexOfFirst { it.id == selectedPetId }
            if (petIndex != -1) {
                binding.spinnerPet.setSelection(petIndex)
                Log.d(TAG, "Seçilen evcil hayvan pozisyonu: $petIndex, ID: $selectedPetId")
            } else {
                Log.e(TAG, "Seçilen evcil hayvan bulunamadı, ID: $selectedPetId")
            }
        }
        
        // Spinner değişikliğini dinle
        binding.spinnerPet.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Seçilen evcil hayvanın ID'sini al
                if (position >= 0 && position < pets.size) {
                    selectedPetId = pets.values.elementAt(position).id
                    Log.d(TAG, "Evcil hayvan seçildi: ${pets.values.elementAt(position).name}, ID: $selectedPetId")
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Bir şey seçilmediğinde yapılacak bir şey yok
            }
        }
        
        // Evcil hayvanlar yüklendikten sonra edit modundaysa vaccination verilerini yükle
        if (isEditMode && vaccinationId.isNotEmpty() && selectedPetId.isNotEmpty()) {
            Log.d(TAG, "Evcil hayvanlar yüklendi, şimdi vaccination verileri yükleniyor...")
            loadVaccinationData()
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Spinner değerini kontrol et
        val selectedPosition = binding.spinnerPet.selectedItemPosition
        if (selectedPosition == -1 || pets.isEmpty()) {
            Toast.makeText(this, "Lütfen bir evcil hayvan seçin", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            // Pozisyon geçerli, evcil hayvan ID'sini ayarla
            selectedPetId = pets.values.elementAt(selectedPosition).id
            Log.d(TAG, "Doğrulanan evcil hayvan ID: $selectedPetId")
        }
        
        // Aşı adı kontrolü
        if (TextUtils.isEmpty(binding.etName.text)) {
            binding.etName.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.etName.error = null
        }
        
        // Tarih kontrolü
        if (selectedDate == Date(0)) {
            binding.etDate.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.etDate.error = null
        }
        
        return isValid
    }
    
    private fun saveVaccination() {
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
            Log.d(TAG, "Aşı kaydı başlatılıyor...")
            Log.d(TAG, "Kullanıcı ID: $userId")
            Log.d(TAG, "Evcil hayvan ID: $selectedPetId")

            // Verileri hazırla
            val name = binding.etName.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()
            val nextDate = selectedNextDate

            // Debugger için veri kontrolü
            Log.d(TAG, "Aşı adı: $name")
            Log.d(TAG, "Aşı tarihi: $selectedDate")
            Log.d(TAG, "Sonraki aşı tarihi: $nextDate")

            val vaccination = Vaccination(
                petId = selectedPetId,
                ownerId = userId,
                name = name,
                date = selectedDate,
                nextDate = nextDate,
                notes = notes,
                status = VaccinationStatus.COMPLETED
            )

            // Hiyerarşiyi doğru şekilde oluştur
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(selectedPetId)
                .collection("vaccinations")
                .add(vaccination)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Aşı kaydı başarılı, ID: ${documentReference.id}")
                    
                    // Aşı ID'si güncellemesi
                    documentReference.update("id", documentReference.id)
                        .addOnSuccessListener {
                            Log.d(TAG, "Aşı ID güncellendi")
                            
                            // Bildirim ayarla (eğer sonraki tarih varsa)
                            if (nextDate != null) {
                                scheduleNotification(documentReference.id, selectedPetId, name, nextDate)
                            }
                            
                            Toast.makeText(this, "Aşı başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Aşı ID güncellemesi başarısız: ${e.message}")
                            // Yine de başarılı kabul et
                            Toast.makeText(this, "Aşı kaydedildi, ancak ID güncellenemedi", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Aşı kaydetme hatası: ${e.message}")
                    Toast.makeText(this, "Aşı kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }
        } catch (e: Exception) {
            Log.e(TAG, "saveVaccination genel hata: ${e.message}")
            Toast.makeText(this, "Aşı kaydetme sırasında bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
        }
    }

    /**
     * Belirli bir aşı için bildirim zamanlar
     */
    private fun scheduleNotification(vaccinationId: String, petId: String, vaccinationName: String, nextDate: Date) {
        try {
            Log.d(TAG, "Bildirim zamanlanıyor: $vaccinationName, Tarih: $nextDate")
            
            // Evcil hayvan adını al
            val pet = pets[petId]
            val petName = pet?.name ?: "Evcil hayvanınız"
            
            // Bildirim içeriği oluştur
            val title = "Aşı Hatırlatması"
            val content = "$petName için $vaccinationName aşı zamanı geldi"
            
            // Bildirim kanalı oluştur ve zamanla
            notificationHelper.scheduleNotification(
                notificationId = vaccinationId.hashCode(),
                title = title,
                content = content,
                triggerTime = nextDate.time
            )
            
            Log.d(TAG, "Bildirim başarıyla zamanlandı")
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim zamanlama hatası: ${e.message}")
            // Bildirimi zamanlamada bir hata olması, aşı kaydını etkilemeyecek
        }
    }

    private fun updateVaccination() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
        try {
            val name = binding.etName.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()
            
            val vaccination = Vaccination(
                id = vaccinationId,
                petId = selectedPetId,
                ownerId = userId,
                name = name,
                date = selectedDate,
                nextDate = selectedNextDate,
                notes = notes,
                status = if (selectedNextDate != null && selectedNextDate!!.after(Date())) 
                    VaccinationStatus.UPCOMING 
                else 
                    VaccinationStatus.COMPLETED
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .document(selectedPetId)
                .collection("vaccinations")
                .document(vaccinationId)
                .set(vaccination)
                .addOnSuccessListener {
                    Log.d(TAG, "Aşı başarıyla güncellendi")
                    Toast.makeText(this, "Aşı başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Aşı güncellenemedi: ${e.message}")
                    Toast.makeText(this, "Aşı güncellenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }
            
        } catch (e: Exception) {
            Log.e(TAG, "updateVaccination hatası: ${e.message}")
            Toast.makeText(this, "Güncelleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
        }
    }
    
    private fun loadVaccinationData() {
        val userId = auth.currentUser?.uid
        if (userId == null || vaccinationId.isEmpty() || selectedPetId.isEmpty()) {
            Toast.makeText(this, "Aşı bilgileri yüklenemedi - eksik parametreler", Toast.LENGTH_LONG).show()
            Log.e(TAG, "loadVaccinationData - Eksik parametreler: userId=$userId, vaccinationId='$vaccinationId', selectedPetId='$selectedPetId'")
            finish()
            return
        }
        
        Log.d(TAG, "loadVaccinationData başlatılıyor - userId: $userId, vaccinationId: $vaccinationId, selectedPetId: $selectedPetId")
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("users")
            .document(userId)
            .collection("pets")
            .document(selectedPetId)
            .collection("vaccinations")
            .document(vaccinationId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                
                if (document.exists()) {
                    val vaccination = document.toObject(Vaccination::class.java)
                    vaccination?.let { vacc ->
                        // Form alanlarını doldur
                        binding.etName.setText(vacc.name)
                        binding.etNotes.setText(vacc.notes)
                        
                        // Tarihleri ayarla
                        vacc.date?.let { date ->
                            selectedDate = date
                            binding.etDate.setText(dateFormat.format(date))
                        }
                        
                        vacc.nextDate?.let { nextDate ->
                            selectedNextDate = nextDate
                            binding.etNextDate.setText(dateFormat.format(nextDate))
                        }
                        
                        Log.d(TAG, "Aşı verileri başarıyla yüklendi: ${vacc.name}")
                    }
                } else {
                    Toast.makeText(this, "Aşı kaydı bulunamadı", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Aşı dokümanı bulunamadı - ID: $vaccinationId")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Aşı verileri yüklenemedi: ${e.message}")
                Toast.makeText(this, "Aşı verileri yüklenirken hata oluştu. Lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
                finish()
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
} 