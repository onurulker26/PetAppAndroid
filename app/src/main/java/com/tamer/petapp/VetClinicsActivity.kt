package com.tamer.petapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.tamer.petapp.model.VetClinic
import com.tamer.petapp.ui.screens.VetClinicsScreen
import com.tamer.petapp.ui.theme.PetAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class VetClinicsActivity : ComponentActivity() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var locationManager: LocationManager
    
    private val TAG = "VetClinicsActivity"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val GPS_REQUEST_CODE = 1001
    
    // State flows
    private val _vetClinics = MutableStateFlow<List<VetClinic>>(emptyList())
    val vetClinics: StateFlow<List<VetClinic>> = _vetClinics.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()
    
    private val _selectedClinicId = MutableStateFlow<String?>(null)
    val selectedClinicId: StateFlow<String?> = _selectedClinicId.asStateFlow()
    
    private val _cityName = MutableStateFlow("Konum Seçilmedi")
    val cityName: StateFlow<String> = _cityName.asStateFlow()
    
    private val _searchRadius = MutableStateFlow(5.0)
    val searchRadius: StateFlow<Double> = _searchRadius.asStateFlow()

    // İzin isteği sonucu
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                
        if (locationGranted) {
            checkGpsEnabled()
        } else {
            // Toast.makeText(this, "Konum izni verilmedi. Haritada bir konuma tıklayarak devam edebilirsiniz.", Toast.LENGTH_LONG).show()
            _isLoading.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // LocationManager başlat
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        
        // Places API'yi başlat
        try {
            val apiKey = getString(R.string.google_maps_api_key)
            Places.initialize(applicationContext, apiKey)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            okHttpClient = OkHttpClient()
            
            // Konum callback'i oluştur
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        // Konum alındığında işle
                        _currentLocation.value = LatLng(location.latitude, location.longitude)
                        getCityName(location)
                        findNearbyVetClinics(location)
                        
                        // Konum alındıktan sonra güncellemeyi durdur
                        stopLocationUpdates()
                        break
                    }
                }
            }
            
            // Konum isteği ayarları
            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Places API başlatılamadı: ${e.message}", e)
        }
        
        // Konum izni kontrolü
        checkLocationPermission()
        
        setContent {
            PetAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val clinics by vetClinics.collectAsState()
                    val loading by isLoading.collectAsState()
                    val location by currentLocation.collectAsState()
                    val clinicId by selectedClinicId.collectAsState()
                    val city by cityName.collectAsState()
                    val radius by searchRadius.collectAsState()
                    
                    VetClinicsScreen(
                        vetClinics = clinics,
                        isLoading = loading,
                        currentLocation = location,
                        selectedClinicId = clinicId,
                        cityName = city,
                        searchRadius = radius,
                        onClinicSelected = { clinic -> selectClinic(clinic) },
                        onCallClinic = { clinic -> callClinic(clinic) },
                        onGetDirections = { clinic -> getDirections(clinic) },
                        onMapClick = { latLng -> handleMapClick(latLng) },
                        onMapLoaded = { _isLoading.value = false },
                        onBackClick = { finish() },
                        onNavigationItemClick = { index -> handleNavigationClick(index) }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Uygulama öne çıktığında ve hala konum alınmamışsa tekrar deneyin
        if (_currentLocation.value == null && hasLocationPermission()) {
            checkGpsEnabled()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Uygulama arka plana geçtiğinde konum güncellemelerini durdur
        stopLocationUpdates()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GPS_REQUEST_CODE) {
            if (isGpsEnabled()) {
                // GPS açıldı, konum güncellemelerini başlat
                startLocationUpdates()
            } else {
                // GPS hala kapalı
                // Toast.makeText(this, "GPS kapalı olduğu için konum alınamıyor. Haritada bir konuma tıklayarak devam edebilirsiniz.", Toast.LENGTH_LONG).show()
                _isLoading.value = false
            }
        }
    }
    
    private fun checkLocationPermission() {
        when {
            hasLocationPermission() -> {
                checkGpsEnabled()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Toast.makeText(this, "Yakındaki veteriner kliniklerini görebilmek için konum izni gereklidir.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    private fun checkGpsEnabled() {
        if (isGpsEnabled()) {
            startLocationUpdates()
        } else {
            // GPS kapalı, kullanıcıyı ayarlara yönlendir
            showGpsDisabledAlert()
        }
    }
    
    private fun showGpsDisabledAlert() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("GPS Kapalı")
            .setMessage("Konumunuzu alabilmek için GPS'i açmanız gerekiyor. GPS ayarlarını açmak ister misiniz?")
            .setPositiveButton("Ayarlar") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, GPS_REQUEST_CODE)
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
                // Toast.makeText(this, "GPS kapalı olduğu için konum alınamıyor. Haritada bir konuma tıklayarak devam edebilirsiniz.", Toast.LENGTH_LONG).show()
                _isLoading.value = false
            }
            .setCancelable(false)
            .create()
        
        alertDialog.show()
    }
    
    private fun startLocationUpdates() {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                
                // Aynı zamanda son konumu da deneyelim
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        _currentLocation.value = LatLng(location.latitude, location.longitude)
                        getCityName(location)
                        findNearbyVetClinics(location)
                        stopLocationUpdates() // Konum alındığında güncellemeyi durdur
                    }
                }
                
                // 30 saniye içinde konum alınamazsa yükleme durumunu iptal et
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (_currentLocation.value == null) {
                        _isLoading.value = false
                        // Toast.makeText(this, "Konum alınamadı. Lütfen haritada bir yere tıklayın.", Toast.LENGTH_SHORT).show()
                        stopLocationUpdates()
                    }
                }, 30000)
            } else {
                _isLoading.value = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Konum güncellemeleri başlatılamadı: ${e.message}", e)
            _isLoading.value = false
        }
    }
    
    private fun getCityName(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            geocoder.getFromLocation(location.latitude, location.longitude, 1)?.let { addresses ->
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    _cityName.value = address.locality ?: address.adminArea ?: "Bilinmeyen Konum"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Şehir adı alınamadı: ${e.message}", e)
            _cityName.value = "Bilinmeyen Konum"
        }
    }
    
    private fun findNearbyVetClinics(location: Location) {
        try {
            _isLoading.value = true
            
            val url = buildNearbySearchUrl(location, (_searchRadius.value * 1000).toInt())
            val request = Request.Builder().url(url).build()
            
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Yakındaki veterinerler alınamadı: ${e.message}", e)
                    runOnUiThread {
                        _isLoading.value = false
                        // Toast.makeText(applicationContext, "Veteriner klinikleri yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            processVetClinicsResponse(responseBody, location)
                        } else {
                            runOnUiThread { _isLoading.value = false }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Yanıt işlenirken hata: ${e.message}", e)
                        runOnUiThread { _isLoading.value = false }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Veteriner klinikleri araması sırasında hata: ${e.message}", e)
            _isLoading.value = false
        }
    }
    
    private fun buildNearbySearchUrl(location: Location, radius: Int): String {
        val baseUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        val locationParam = "${location.latitude},${location.longitude}"
        val apiKey = getString(R.string.google_maps_api_key)
        
        return Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("location", locationParam)
            .appendQueryParameter("radius", radius.toString())
            .appendQueryParameter("type", "veterinary_care")
            .appendQueryParameter("key", apiKey)
            .build()
            .toString()
    }
    
    private fun processVetClinicsResponse(responseBody: String, location: Location) {
        try {
            val jsonResponse = JSONObject(responseBody)
            val status = jsonResponse.optString("status", "UNKNOWN_ERROR")
            
            Log.d(TAG, "API yanıtı durumu: $status")
            
            if (status != "OK" && status != "ZERO_RESULTS") {
                runOnUiThread { 
                    _isLoading.value = false
                    // Toast.makeText(applicationContext, "API Hata: $status", Toast.LENGTH_SHORT).show()
                }
                return
            }
            
            val results = jsonResponse.optJSONArray("results")
            val clinics = mutableListOf<VetClinic>()
            
            Log.d(TAG, "Bulunan veteriner klinik sayısı: ${results?.length() ?: 0}")
            
            if (results != null && results.length() > 0) {
                for (i in 0 until results.length()) {
                    try {
                        val place = results.getJSONObject(i)
                        
                        val name = place.optString("name", "")
                        val placeId = place.optString("place_id", "")
                        val vicinity = place.optString("vicinity", "")
                        val rating = place.optDouble("rating", 0.0)
                        
                        // Konum bilgilerini al
                        val geometry = place.optJSONObject("geometry")
                        val locationObj = geometry?.optJSONObject("location")
                        
                        if (locationObj != null && placeId.isNotEmpty() && name.isNotEmpty()) {
                            val lat = locationObj.optDouble("lat", 0.0)
                            val lng = locationObj.optDouble("lng", 0.0)
                            
                            // Mesafeyi hesapla
                            val clinicLocation = Location("").apply {
                                latitude = lat
                                longitude = lng
                            }
                            val distanceInMeters = location.distanceTo(clinicLocation)
                            val distanceInKm = distanceInMeters / 1000.0
                            
                            // Açık/kapalı durumunu kontrol et
                            var isOpen = false
                            if (place.has("opening_hours")) {
                                val openingHours = place.getJSONObject("opening_hours")
                                isOpen = openingHours.optBoolean("open_now", false)
                            }
                            
                            // Veteriner kliniği nesnesini oluştur
                            val clinic = VetClinic(
                                id = placeId,
                                placeId = placeId,
                                name = name,
                                address = vicinity,
                                phoneNumber = "", // Detaylarla doldurulacak
                                rating = rating,
                                latitude = lat,
                                longitude = lng,
                                distance = distanceInKm,
                                openingHours = if (isOpen) "Şu anda açık" else "Şu anda kapalı",
                                isOpen = isOpen
                            )
                            
                            Log.d(TAG, "Veteriner kliniği eklendi: $name, ID: $placeId")
                            clinics.add(clinic)
                        } else {
                            Log.e(TAG, "Eksik klinik bilgisi: placeId=$placeId, name=$name, locationObj=$locationObj")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Klinik işlenirken hata: ${e.message}", e)
                    }
                }
            } else {
                Log.d(TAG, "Sonuç bulunamadı veya sonuç dizisi boş")
            }
            
            // Klinikleri mesafeye göre sırala
            val sortedClinics = clinics.sortedBy { it.distance }
            
            Log.d(TAG, "İşlenen toplam klinik sayısı: ${clinics.size}")
            
            runOnUiThread {
                _vetClinics.value = sortedClinics
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON yanıtı işlenirken hata: ${e.message}", e)
            runOnUiThread { 
                _isLoading.value = false
                // Toast.makeText(applicationContext, "Veteriner klinikleri yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getClinicDetails(clinicId: String, onDetailsReceived: (String, String, Boolean) -> Unit) {
        try {
            val url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=$clinicId&fields=formatted_phone_number,international_phone_number,opening_hours&key=${getString(R.string.google_maps_api_key)}"
            
            val request = Request.Builder().url(url).build()
            
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Klinik detayları alınamadı: ${e.message}", e)
                    runOnUiThread {
                        onDetailsReceived("Telefon numarası bulunamadı", "Çalışma saatleri bilinmiyor", false)
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (responseBody == null) {
                            runOnUiThread {
                                onDetailsReceived("Telefon numarası bulunamadı", "Çalışma saatleri bilinmiyor", false)
                            }
                            return
                        }
                        
                        val jsonObject = JSONObject(responseBody)
                        val result = jsonObject.optJSONObject("result")
                        
                        if (result == null) {
                            runOnUiThread {
                                onDetailsReceived("Telefon numarası bulunamadı", "Çalışma saatleri bilinmiyor", false)
                            }
                            return
                        }
                        
                        // Telefon numarası
                        var phoneNumber = "Telefon numarası bulunamadı"
                        if (result.has("formatted_phone_number")) {
                            phoneNumber = result.getString("formatted_phone_number")
                        } else if (result.has("international_phone_number")) {
                            phoneNumber = result.getString("international_phone_number")
                        }
                        
                        // Çalışma saatleri
                        var openingHoursText = "Çalışma saatleri bilinmiyor"
                        var isOpenNow = false
                        
                        if (result.has("opening_hours")) {
                            val openingHours = result.getJSONObject("opening_hours")
                            isOpenNow = openingHours.optBoolean("open_now", false)
                            
                            if (openingHours.has("weekday_text")) {
                                val weekdayText = openingHours.getJSONArray("weekday_text")
                                if (weekdayText.length() > 0) {
                                    openingHoursText = weekdayText.getString(0)
                                        .replace("Monday: ", "Pazartesi: ")
                                        .replace("Tuesday: ", "Salı: ")
                                        .replace("Wednesday: ", "Çarşamba: ")
                                        .replace("Thursday: ", "Perşembe: ")
                                        .replace("Friday: ", "Cuma: ")
                                        .replace("Saturday: ", "Cumartesi: ")
                                        .replace("Sunday: ", "Pazar: ")
                                }
                            }
                        }
                        
                        runOnUiThread {
                            onDetailsReceived(phoneNumber, openingHoursText, isOpenNow)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Klinik detayları işlenirken hata: ${e.message}", e)
                        runOnUiThread {
                            onDetailsReceived("Telefon numarası bulunamadı", "Çalışma saatleri bilinmiyor", false)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Klinik detayları istenirken hata: ${e.message}", e)
            onDetailsReceived("Telefon numarası bulunamadı", "Çalışma saatleri bilinmiyor", false)
        }
    }
    
    private fun selectClinic(clinic: VetClinic) {
        _selectedClinicId.value = clinic.id
        
        // Detayları al
        getClinicDetails(clinic.id) { phoneNumber, openingHoursText, isOpenNow ->
            val updatedClinics = _vetClinics.value.map { 
                if (it.id == clinic.id) {
                    it.copy(
                        phoneNumber = phoneNumber,
                        openingHours = openingHoursText,
                        isOpen = isOpenNow
                    )
                } else {
                    it
                }
            }
            _vetClinics.value = updatedClinics
        }
    }
    
    private fun callClinic(clinic: VetClinic) {
        if (clinic.phoneNumber.isNotEmpty() && clinic.phoneNumber != "Telefon numarası bulunamadı") {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${clinic.phoneNumber}")
            startActivity(intent)
        } else {
            // Toast.makeText(this, "Telefon numarası alınıyor...", Toast.LENGTH_SHORT).show()
            
            getClinicDetails(clinic.id) { phoneNumber, openingHoursText, isOpenNow ->
                if (phoneNumber.isNotEmpty() && phoneNumber != "Telefon numarası bulunamadı") {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:$phoneNumber")
                    startActivity(intent)
                } else {
                    // Toast.makeText(this, "Telefon numarası bulunamadı", Toast.LENGTH_SHORT).show()
                }
                
                // Klinik bilgilerini güncelle
                val updatedClinics = _vetClinics.value.map { 
                    if (it.id == clinic.id) {
                        it.copy(
                            phoneNumber = phoneNumber,
                            openingHours = openingHoursText,
                            isOpen = isOpenNow
                        )
                    } else {
                        it
                    }
                }
                _vetClinics.value = updatedClinics
            }
        }
    }
    
    private fun getDirections(clinic: VetClinic) {
        val gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${clinic.latitude},${clinic.longitude}&destination_name=${Uri.encode(clinic.name)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Toast.makeText(this, "Google Maps uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleMapClick(latLng: LatLng) {
        _currentLocation.value = latLng
        _selectedClinicId.value = null
        
        val location = Location("").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        }
        
        getCityName(location)
        findNearbyVetClinics(location)
    }
    
    private fun handleNavigationClick(index: Int) {
        when (index) {
            0 -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            1 -> { /* Zaten veteriner klinikleri sayfasındayız */ }
            2 -> startActivity(Intent(this, com.tamer.petapp.guide.PetCareGuideActivity::class.java))
            3 -> startActivity(Intent(this, ProfileActivity::class.java))
            4 -> startActivity(Intent(this, com.tamer.petapp.ui.disease.DiseaseAnalysisActivity::class.java))
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
} 