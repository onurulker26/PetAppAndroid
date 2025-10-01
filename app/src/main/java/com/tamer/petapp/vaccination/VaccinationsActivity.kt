package com.tamer.petapp.vaccination

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.model.Vaccination
import com.tamer.petapp.ui.theme.PetAppTheme
import com.tamer.petapp.util.NotificationHelper
import com.tamer.petapp.viewmodel.VaccinationViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class VaccinationsActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var notificationHelper: NotificationHelper
    private var currentViewModel: VaccinationViewModel? = null
    private val TAG = "VaccinationsActivity"

    companion object {
        const val ADD_VACCINATION_REQUEST_CODE = 100
        const val EDIT_VACCINATION_REQUEST_CODE = 200
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Bildirim izni verildi")
            setupNotifications()
        } else {
            // Toast.makeText(this, 
            //     "Bildirim izni olmadan hatırlatmaları alamayacaksınız", 
            //     Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // NotificationHelper başlatma
        notificationHelper = NotificationHelper(this)
        
        // Bildirim iznini kontrol et
        checkNotificationPermission()
        
        // Firebase başlatma
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setContent {
            PetAppTheme {
                val viewModel = viewModel<VaccinationViewModel>()
                
                // ViewModel referansını sakla
                DisposableEffect(viewModel) {
                    currentViewModel = viewModel
                    onDispose {
                        // Cleanup işlemi gerekirse burada yapılabilir
                    }
                }
                
                val vaccinations by viewModel.vaccinations.collectAsState()
                val pets by viewModel.pets.collectAsState() 
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val context = LocalContext.current
                
                // Hata mesajı varsa göster
                // if (errorMessage.isNotEmpty()) {
                //     Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                //     viewModel.clearErrorMessage()
                // }
                
                // İlk yükleme
                LaunchedEffect(key1 = Unit) {
                    viewModel.loadData(auth.currentUser?.uid)
                }
                
                VaccinationsComposeScreen(
                    vaccinations = vaccinations,
                    pets = pets,
                    isLoading = isLoading,
                    onAddVaccinationClick = {
                        startActivityForResult(
                            Intent(context, AddVaccinationActivity::class.java),
                            ADD_VACCINATION_REQUEST_CODE
                        )
                    },
                    onEditVaccinationClick = { vaccination ->
                        // Gerekli parametrelerin geçerli olup olmadığını kontrol et
                        // if (vaccination.id.isNullOrEmpty() || vaccination.petId.isNullOrEmpty()) {
                        //     Toast.makeText(context, "Aşı bilgileri eksik, düzenleme yapılamıyor", Toast.LENGTH_LONG).show()
                        //     return@VaccinationsComposeScreen
                        // }
                        
                        val intent = Intent(context, AddVaccinationActivity::class.java).apply {
                            putExtra("vaccination_id", vaccination.id)
                            putExtra("pet_id", vaccination.petId)
                            putExtra("is_edit_mode", true)
                        }
                        startActivityForResult(intent, EDIT_VACCINATION_REQUEST_CODE)
                    },
                    onDeleteVaccinationClick = { vaccination ->
                        viewModel.deleteVaccination(vaccination) { success ->
                            if (success) {
                                // Toast.makeText(context, "Aşı başarıyla silindi", Toast.LENGTH_SHORT).show()
                            } else {
                                // Toast.makeText(context, "Aşı silinirken hata oluştu", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBackClick = {
                        finish()
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        try {
            // Aşı ekranına geri döndüğünde verileri yenile
            Log.d(TAG, "onResume çağrıldı - aşı listesi yeniden yükleniyor")
            currentViewModel?.loadData(auth.currentUser?.uid)
        } catch (e: Exception) {
            Log.e(TAG, "onResume hatası: ${e.message}", e)
        }
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == 
                        PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten var, bildirimleri ayarla
                    setupNotifications()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Kullanıcıya neden izin istediğimizi açıklama
                    showNotificationPermissionRationale()
                }
                else -> {
                    // İzni iste
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13 (Tiramisu) öncesi sürümlerde bildirim izni otomatik olarak verilir
            setupNotifications()
        }
    }
    
    private fun setupNotifications() {
        // Bildirim kanalını oluştur
        notificationHelper.createNotificationChannel()
        
        // Yaklaşan aşıları kontrol et ve bildirim gönder
        checkUpcomingVaccinations()
    }
    
    private fun showNotificationPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bildirim İzni Gerekli")
            .setMessage("Aşı hatırlatmalarını alabilmeniz için bildirim iznine ihtiyacımız var.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Reddet") { dialog, _ ->
                dialog.dismiss()
                // Toast.makeText(this, 
                //     "Bildirim izni olmadan hatırlatmaları alamayacaksınız", 
                //     Toast.LENGTH_LONG).show()
            }
            .show()
    }
    
    private fun checkUpcomingVaccinations() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        
        // Aktivite bağlamında viewModel oluşturalım
        val vaccViewModel = VaccinationViewModel()
        val vaccinations = vaccViewModel.vaccinations.value
        
        // 1 hafta ve 1 gün kala olan aşıları filtrele
        val oneWeekVaccinations = vaccinations.filter { vaccination ->
            vaccination.nextDate?.time?.let { nextDate ->
                val daysUntil = TimeUnit.MILLISECONDS.toDays(nextDate - today)
                daysUntil == 7L // 1 hafta kala
            } ?: false
        }

        val oneDayVaccinations = vaccinations.filter { vaccination ->
            vaccination.nextDate?.time?.let { nextDate ->
                val daysUntil = TimeUnit.MILLISECONDS.toDays(nextDate - today)
                daysUntil == 1L // 1 gün kala
            } ?: false
        }
        
        if (oneWeekVaccinations.isNotEmpty()) {
            showUpcomingVaccinationsNotification(oneWeekVaccinations, true)
        }
        
        if (oneDayVaccinations.isNotEmpty()) {
            showUpcomingVaccinationsNotification(oneDayVaccinations, false)
        }
    }
    
    private fun showUpcomingVaccinationsNotification(upcomingVaccinations: List<Vaccination>, isWeekNotification: Boolean) {
        val days = if (isWeekNotification) "7 gün" else "1 gün"
                val title = "Yaklaşan Aşı Hatırlatması"
        val contentText = if (upcomingVaccinations.size == 1) {
            "${upcomingVaccinations[0].name} aşısına $days kaldı"
        } else {
            "${upcomingVaccinations.size} aşıya $days kaldı"
        }
        
        notificationHelper.showNotification(
            title,
            contentText,
            if (isWeekNotification) NotificationHelper.NOTIFICATION_ID_WEEK else NotificationHelper.NOTIFICATION_ID_DAY
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        try {
            when (requestCode) {
                ADD_VACCINATION_REQUEST_CODE -> {
                    if (resultCode == RESULT_OK) {
                        Log.d(TAG, "Aşı ekleme başarılı - veri güncelleniyor")
                        // Mevcut ViewModel'i kullanarak verileri yenile
                        currentViewModel?.loadData(auth.currentUser?.uid)
                        // Toast.makeText(this, "Aşı listesi güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
                EDIT_VACCINATION_REQUEST_CODE -> {
                    if (resultCode == RESULT_OK) {
                        Log.d(TAG, "Aşı düzenleme başarılı - veri güncelleniyor")
                        // Mevcut ViewModel'i kullanarak verileri yenile
                        currentViewModel?.loadData(auth.currentUser?.uid)
                        // Toast.makeText(this, "Aşı listesi güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onActivityResult hatası: ${e.message}", e)
        }
    }
} 