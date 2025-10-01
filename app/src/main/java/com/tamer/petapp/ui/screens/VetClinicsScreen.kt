package com.tamer.petapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.tamer.petapp.model.VetClinic
import com.tamer.petapp.R
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetClinicsScreen(
    vetClinics: List<VetClinic>,
    isLoading: Boolean,
    currentLocation: LatLng?,
    selectedClinicId: String?,
    cityName: String,
    searchRadius: Double,
    onClinicSelected: (VetClinic) -> Unit,
    onCallClinic: (VetClinic) -> Unit,
    onGetDirections: (VetClinic) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    onBackClick: () -> Unit,
    onNavigationItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = androidx.compose.ui.graphics.Color(0xFF673AB7)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veteriner Klinikleri") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = primaryColor
            ) {
                // Ana Sayfa
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") },
                    selected = false,
                    onClick = { onNavigationItemClick(0) }
                )
                
                // Veteriner Klinikleri
                NavigationBarItem(
                    icon = { Icon(painterResource(id = R.drawable.ic_vet), contentDescription = "Veterinerler") },
                    label = { Text("Veterinerler") },
                    selected = true,
                    onClick = { onNavigationItemClick(1) }
                )
                
                // Bakım (Pet Care Guide)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Bakım") },
                    label = { Text("Bakım") },
                    selected = false,
                    onClick = { onNavigationItemClick(2) }
                )
                
                // Profil
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = false,
                    onClick = { onNavigationItemClick(3) }
                )
                
                // Hastalık Analizi
                NavigationBarItem(
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = "Hastalık Analizi") },
                    label = { Text("Hastalık Analizi") },
                    selected = false,
                    onClick = { onNavigationItemClick(4) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Harita Bölümü (Yukarıda)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f) // Haritaya daha fazla alan ver
            ) {
                // GoogleMap'i AndroidView içinde kullanıyoruz
                MapViewContainer(
                    vetClinics = vetClinics,
                    currentLocation = currentLocation,
                    selectedClinicId = selectedClinicId,
                    cityName = cityName,
                    searchRadius = searchRadius,
                    onMapClick = onMapClick,
                    onMapLoaded = onMapLoaded,
                    onMarkerClick = { clickedClinicId -> 
                        vetClinics.find { it.id == clickedClinicId }?.let { onClinicSelected(it) }
                    },
                    context = context
                )
                
                // Yükleniyor göstergesi
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                // Konum bilgisi kartı - haritanın üstünde
                if (currentLocation != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Modern konum ikonu
                            Card(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Konum bilgileri
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cityName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                                )
                                
                                Text(
                                    text = "${searchRadius.toInt()} km yarıçapında arama",
                                    fontSize = 13.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF666666),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            // GPS yeniden konumlandırma butonu
                            Card(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { /* GPS'e odaklan */ },
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = primaryColor.copy(alpha = 0.1f)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Konumuma git",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Veteriner Listesi Bölümü (Aşağıda)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f), // Liste için daha az alan
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Modern başlık tasarımı
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_vet),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = "Veteriner Klinikleri",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = primaryColor
                                )
                                if (vetClinics.isNotEmpty()) {
                                    Text(
                                        text = "${vetClinics.size} klinik bulundu",
                                        fontSize = 12.sp,
                                        color = androidx.compose.ui.graphics.Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Yakındaki veterinerler aranıyor...",
                                    fontSize = 14.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            }
                        }
                    } else if (vetClinics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Bu bölgede veteriner kliniği bulunamadı",
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.Gray,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Farklı bir konum seçmeyi deneyin",
                                    textAlign = TextAlign.Center,
                                    color = androidx.compose.ui.graphics.Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        
                        // Seçili veterinere otomatik scroll
                        LaunchedEffect(selectedClinicId) {
                            selectedClinicId?.let { clinicId ->
                                val index = vetClinics.indexOfFirst { it.id == clinicId }
                                if (index != -1) {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(vetClinics) { clinic ->
                                VetClinicItem(
                                    clinic = clinic,
                                    isSelected = clinic.id == selectedClinicId,
                                    onClinicClicked = { onClinicSelected(clinic) },
                                    onDirectionsClicked = { onGetDirections(clinic) },
                                    onCallClicked = { onCallClinic(clinic) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapViewContainer(
    vetClinics: List<VetClinic>,
    currentLocation: LatLng?,
    selectedClinicId: String?,
    cityName: String,
    searchRadius: Double,
    onMapClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    onMarkerClick: (String) -> Unit,
    context: Context
) {
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // MapView'ı AndroidView içinde kullanıyoruz
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView.also {
            it.getMapAsync { googleMap ->
                // Harita ayarları
                googleMap.uiSettings.apply {
                    isZoomControlsEnabled = true
                    isMyLocationButtonEnabled = true
                    isMapToolbarEnabled = true
                }
                
                // Harita tıklama olayı
                googleMap.setOnMapClickListener { latLng ->
                    onMapClick(latLng)
                }
                
                // Marker tıklama olayı
                googleMap.setOnMarkerClickListener { marker ->
                    marker.tag?.toString()?.let { clinicId ->
                        onMarkerClick(clinicId)
                    }
                    true // Olayı işlediğimizi belirtiyoruz
                }
                
                // Mevcut konum varsa, haritayı o konuma odakla
                currentLocation?.let { location ->
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(location, 15f)
                    )
                }
                
                updateMapMarkers(googleMap, vetClinics, currentLocation, selectedClinicId, context)
                onMapLoaded()
            }
        } },
        update = { mapView ->
            mapView.getMapAsync { googleMap ->
                // Haritadaki tüm işaretçileri temizle
                googleMap.clear()
                
                // Mevcut konum varsa, haritayı o konuma odakla
                currentLocation?.let { location ->
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(location, 15f)
                    )
                }
                
                // Marker tıklama olayı
                googleMap.setOnMarkerClickListener { marker ->
                    marker.tag?.toString()?.let { clinicId ->
                        onMarkerClick(clinicId)
                    }
                    true // Olayı işlediğimizi belirtiyoruz
                }
                
                updateMapMarkers(googleMap, vetClinics, currentLocation, selectedClinicId, context)
            }
        }
    )
}

private fun updateMapMarkers(
    googleMap: GoogleMap,
    vetClinics: List<VetClinic>,
    currentLocation: LatLng?,
    selectedClinicId: String?,
    context: Context
) {
    // Veteriner kliniklerini haritaya ekle
    vetClinics.forEach { clinic ->
        val clinicLocation = LatLng(clinic.latitude, clinic.longitude)
        val isSelected = clinic.id == selectedClinicId
        
        // Özel vet marker oluştur
        val markerOptions = MarkerOptions()
            .position(clinicLocation)
            .title(clinic.name)
            .snippet("${clinic.address}\n${String.format("%.0f", clinic.distance * 1000)} m uzaklıkta")
        
        // ic_vet_marker kullan
        try {
            val vetMarkerBitmap = createCustomVetMarker(context, clinic.name, isSelected)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(vetMarkerBitmap))
        } catch (e: Exception) {
            // Fallback olarak varsayılan marker kullan
            if (isSelected) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
        }
        
        val marker = googleMap.addMarker(markerOptions)
        marker?.tag = clinic.id // Marker'a klinik ID'sini ekle
        
        // Seçili klinik varsa ona zoom yap
        if (isSelected) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clinicLocation, 16f))
        }
    }
    
    // Kullanıcı konumunu ekle
    currentLocation?.let {
        try {
            // Özel kullanıcı marker'ı oluştur
            val userMarkerBitmap = createCustomUserMarker(context)
            
            val userMarker = MarkerOptions()
                .position(it)
                .title("Konumunuz")
                .snippet("Mevcut konumunuz")
                .icon(BitmapDescriptorFactory.fromBitmap(userMarkerBitmap))
            
            googleMap.addMarker(userMarker)
        } catch (e: Exception) {
            // Fallback olarak varsayılan marker kullan
            val userMarker = MarkerOptions()
                .position(it)
                .title("Konumunuz")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            
            googleMap.addMarker(userMarker)
        }
        
        // Arama yarıçapını görsel olarak göster - şeffaf daire
        googleMap.addCircle(
            com.google.android.gms.maps.model.CircleOptions()
                .center(it)
                .radius(5000.0) // 5 km yarıçap
                .strokeWidth(2f)
                .strokeColor(0x30673AB7) // Mor, düşük alpha
                .fillColor(0x106750A8) // Mor doldurma, çok düşük alpha
        )
    }
}

// Veteriner klinikleri için özel marker oluşturma fonksiyonu - Modern Material Design
private fun createCustomVetMarker(context: Context, clinicName: String, isSelected: Boolean): Bitmap {
    try {
        // Modern tasarım boyutları
        val cardWidth = 280
        val cardHeight = 70
        val iconSize = 50
        val cornerRadius = 25f
        val shadowOffset = 8f
        val pointerSize = 12f
        
        // İsmi optimize et
        val displayName = when {
            clinicName.length > 22 -> clinicName.take(20) + ".."
            else -> clinicName
        }
        
        // Toplam boyutlar
        val totalHeight = cardHeight + pointerSize.toInt() + shadowOffset.toInt()
        val bitmap = Bitmap.createBitmap(cardWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Renk paleti - Modern gradientler
        val primaryColor = if (isSelected) Color.parseColor("#4CAF50") else Color.parseColor("#673AB7")
        val secondaryColor = if (isSelected) Color.parseColor("#66BB6A") else Color.parseColor("#9C27B0")
        val accentColor = if (isSelected) Color.parseColor("#A5D6A7") else Color.parseColor("#E1BEE7")
        
        // Gölge efekti - Daha yumuşak ve modern
        val shadowPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            // Çoklu gölge efekti için
            setShadowLayer(shadowOffset, 0f, shadowOffset/2, Color.parseColor("#40000000"))
        }
        
        // Ana kart gölgesi
        canvas.drawRoundRect(
            shadowOffset, shadowOffset/2, 
            (cardWidth - shadowOffset/2).toFloat(), 
            (cardHeight - shadowOffset/2).toFloat(),
            cornerRadius, cornerRadius, shadowPaint
        )
        
        // Ana kart - Gradient arka plan
        val gradientPaint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.LinearGradient(
                0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                intArrayOf(primaryColor, secondaryColor, accentColor),
                floatArrayOf(0f, 0.6f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        
        canvas.drawRoundRect(
            0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
            cornerRadius, cornerRadius, gradientPaint
        )
        
        // Modern glassmorphism efekti
        val glassPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawRoundRect(
            0f, 0f, cardWidth.toFloat(), cardHeight/2f,
            cornerRadius, cornerRadius, glassPaint
        )
        
        // Beyaz iç çerçeve - İnce ve zarif
        val borderPaint = Paint().apply {
            color = Color.parseColor("#60FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        canvas.drawRoundRect(
            1f, 1f, (cardWidth - 1).toFloat(), (cardHeight - 1).toFloat(),
            cornerRadius - 1f, cornerRadius - 1f, borderPaint
        )
        
        // İkon container - Yuvarlak modern tasarım
        val iconCenterX = iconSize/2f + 15f
        val iconCenterY = cardHeight/2f
        val iconRadius = iconSize/2f + 5f
        
        // İkon arka planı - Beyaz/şeffaf daire
        val iconBgPaint = Paint().apply {
            color = Color.parseColor("#25FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconBgPaint)
        
        // İkon border
        val iconBorderPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        
        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconBorderPaint)
        
        // Veteriner ikonunu çiz
        val vetDrawable = ContextCompat.getDrawable(context, R.drawable.ic_vet_marker)
        vetDrawable?.let { drawable ->
            val iconLeft = (iconCenterX - iconSize/2f).toInt()
            val iconTop = (iconCenterY - iconSize/2f).toInt()
            
            drawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            drawable.setTint(Color.WHITE)
            drawable.draw(canvas)
        }
        
        // Text alanı
        val textStartX = iconCenterX + iconRadius + 20f
        val textEndX = cardWidth - 20f
        val textCenterY = cardHeight/2f
        
        // Ana başlık - Veteriner adı
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(2f, 0f, 2f, Color.parseColor("#80000000"))
        }
        
        // Alt başlık - "Veteriner Kliniği"
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#E0FFFFFF")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        // Text'i çiz
        val titleBounds = Rect()
        titlePaint.getTextBounds(displayName, 0, displayName.length, titleBounds)
        
        // Başlık
        canvas.drawText(
            displayName,
            textStartX,
            textCenterY - 3f,
            titlePaint
        )
        
        // Alt başlık
        canvas.drawText(
            "Veteriner Kliniği",
            textStartX,
            textCenterY + 16f,
            subtitlePaint
        )
        
        // Modern pointer - Daha zarif
        val pointerPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val pointerPath = android.graphics.Path().apply {
            val centerX = cardWidth / 2f
            val startY = cardHeight.toFloat()
            
            moveTo(centerX - pointerSize, startY)
            lineTo(centerX + pointerSize, startY)
            lineTo(centerX, startY + pointerSize)
            close()
        }
        
        canvas.drawPath(pointerPath, pointerPaint)
        
        // Pointer'a da gölge ekle
        val pointerShadowPaint = Paint().apply {
            color = Color.parseColor("#40000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val pointerShadowPath = android.graphics.Path().apply {
            val centerX = cardWidth / 2f + 2f
            val startY = cardHeight.toFloat() + 2f
            
            moveTo(centerX - pointerSize, startY)
            lineTo(centerX + pointerSize, startY)
            lineTo(centerX, startY + pointerSize)
            close()
        }
        
        canvas.drawPath(pointerShadowPath, pointerShadowPaint)
        canvas.drawPath(pointerPath, pointerPaint)
        
        return bitmap
    } catch (e: Exception) {
        // Hata durumunda basit bir marker oluştur
        return createSimpleVetMarker(context, isSelected)
    }
}

// Basit veteriner marker'ı (fallback) - Modern Material Design
private fun createSimpleVetMarker(context: Context, isSelected: Boolean): Bitmap {
    val cardWidth = 220
    val cardHeight = 65
    val cornerRadius = 22f
    val shadowOffset = 6f
    val pointerSize = 10f
    
    val totalHeight = cardHeight + pointerSize.toInt() + shadowOffset.toInt()
    val bitmap = Bitmap.createBitmap(cardWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Modern renk paleti
    val primaryColor = if (isSelected) Color.parseColor("#4CAF50") else Color.parseColor("#673AB7")
    val secondaryColor = if (isSelected) Color.parseColor("#66BB6A") else Color.parseColor("#9C27B0")
    val accentColor = if (isSelected) Color.parseColor("#A5D6A7") else Color.parseColor("#E1BEE7")
    
    // Yumuşak gölge
    val shadowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(shadowOffset, 0f, shadowOffset/2, Color.parseColor("#40000000"))
    }
    
    canvas.drawRoundRect(
        shadowOffset, shadowOffset/2, 
        (cardWidth - shadowOffset/2).toFloat(), 
        (cardHeight - shadowOffset/2).toFloat(),
        cornerRadius, cornerRadius, shadowPaint
    )
    
    // Gradient arka plan
    val gradientPaint = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
            intArrayOf(primaryColor, secondaryColor, accentColor),
            floatArrayOf(0f, 0.6f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    
    canvas.drawRoundRect(
        0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
        cornerRadius, cornerRadius, gradientPaint
    )
    
    // Glassmorphism efekti
    val glassPaint = Paint().apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    canvas.drawRoundRect(
        0f, 0f, cardWidth.toFloat(), cardHeight/2f,
        cornerRadius, cornerRadius, glassPaint
    )
    
    // Zarif border
    val borderPaint = Paint().apply {
        color = Color.parseColor("#60FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    canvas.drawRoundRect(
        1f, 1f, (cardWidth - 1).toFloat(), (cardHeight - 1).toFloat(),
        cornerRadius - 1f, cornerRadius - 1f, borderPaint
    )
    
    // İkon alanı
    val iconCenterX = 45f
    val iconCenterY = cardHeight/2f
    val iconRadius = 25f
    
    // İkon arka planı
    val iconBgPaint = Paint().apply {
        color = Color.parseColor("#25FFFFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconBgPaint)
    
    // İkon border
    val iconBorderPaint = Paint().apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    
    canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconBorderPaint)
    
    // Veteriner sembolü (+ işareti) - Modern tasarım
    val symbolPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        setShadowLayer(1f, 0f, 1f, Color.parseColor("#80000000"))
    }
    
    val symbolSize = 12f
    
    // Dikey çizgi
    canvas.drawLine(iconCenterX, iconCenterY - symbolSize, iconCenterX, iconCenterY + symbolSize, symbolPaint)
    // Yatay çizgi
    canvas.drawLine(iconCenterX - symbolSize, iconCenterY, iconCenterX + symbolSize, iconCenterY, symbolPaint)
    
    // Text alanı - Modern tipografi
    val textStartX = iconCenterX + iconRadius + 15f
    
    // Ana başlık
    val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(2f, 0f, 2f, Color.parseColor("#80000000"))
    }
    
    // Alt başlık
    val subtitlePaint = Paint().apply {
        color = Color.parseColor("#E0FFFFFF")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    // Text'i çiz
    canvas.drawText(
        "Veteriner",
        textStartX,
        cardHeight/2f - 2f,
        titlePaint
    )
    
    canvas.drawText(
        "Kliniği",
        textStartX,
        cardHeight/2f + 15f,
        subtitlePaint
    )
    
    // Modern pointer
    val pointerPaint = Paint().apply {
        color = primaryColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val pointerPath = android.graphics.Path().apply {
        val centerX = cardWidth / 2f
        val startY = cardHeight.toFloat()
        
        moveTo(centerX - pointerSize, startY)
        lineTo(centerX + pointerSize, startY)
        lineTo(centerX, startY + pointerSize)
        close()
    }
    
    // Pointer gölgesi
    val pointerShadowPath = android.graphics.Path().apply {
        val centerX = cardWidth / 2f + 2f
        val startY = cardHeight.toFloat() + 2f
        
        moveTo(centerX - pointerSize, startY)
        lineTo(centerX + pointerSize, startY)
        lineTo(centerX, startY + pointerSize)
        close()
    }
    
    val pointerShadowPaint = Paint().apply {
        color = Color.parseColor("#40000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    canvas.drawPath(pointerShadowPath, pointerShadowPaint)
    canvas.drawPath(pointerPath, pointerPaint)
    
    return bitmap
}

// Kullanıcı konumu için özel marker oluşturma fonksiyonu - daha güzel tasarım
private fun createCustomUserMarker(context: Context): Bitmap {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Dış halka (pulse efekti için)
    val pulsePaint = Paint().apply {
        color = Color.parseColor("#332196F3") // Açık mavi, şeffaf
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5f, pulsePaint)
    
    // Orta halka (beyaz)
    val outerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 15f, outerPaint)
    
    // İç çember (mavi)
    val innerPaint = Paint().apply {
        color = Color.parseColor("#2196F3") // Parlak mavi
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 25f, innerPaint)
    
    // Beyaz nokta (konum hassasiyeti için)
    val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, 8f, dotPaint)
    
    // Çerçeve
    val strokePaint = Paint().apply {
        color = Color.parseColor("#1976D2") // Koyu mavi çerçeve
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 15f, strokePaint)
    
    return bitmap
}

@Composable
fun VetClinicItem(
    clinic: VetClinic,
    isSelected: Boolean,
    onClinicClicked: () -> Unit,
    onDirectionsClicked: () -> Unit,
    onCallClicked: () -> Unit
) {
    val primaryColor = androidx.compose.ui.graphics.Color(0xFF673AB7)
    val backgroundColor = if (isSelected) {
        androidx.compose.ui.graphics.Color(0xFFE8F5E8) // Daha belirgin yeşilimsi arka plan
    } else {
        androidx.compose.ui.graphics.Color.White
    }
    
    // Animasyonlu elevation
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) 16.dp else 4.dp, // Daha belirgin elevation farkı
        animationSpec = androidx.compose.animation.core.tween(300)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) BorderStroke(3.dp, androidx.compose.ui.graphics.Color(0xFF4CAF50)) else null,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClinicClicked
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Üst kısım - Klinik adı ve durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern veteriner ikonu
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) primaryColor else androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_vet),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Klinik bilgileri
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clinic.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isSelected) primaryColor else androidx.compose.ui.graphics.Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Durum badge'i
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (clinic.isOpen) {
                                androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f)
                            } else {
                                androidx.compose.ui.graphics.Color(0xFFFF5722).copy(alpha = 0.15f)
                            }
                        )
                    ) {
                        Text(
                            text = if (clinic.isOpen) "Açık" else "Kapalı",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (clinic.isOpen) {
                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            } else {
                                androidx.compose.ui.graphics.Color(0xFFD84315)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Mesafe ve rating
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Mesafe
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "${String.format("%.0f", clinic.distance * 1000)} m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Rating
                    if (clinic.rating > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", clinic.rating),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = androidx.compose.ui.graphics.Color(0xFF424242)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Adres
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF757575),
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = clinic.address,
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF616161),
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ara butonu
                ElevatedButton(
                    onClick = onCallClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF5722),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ARA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Rota butonu
                ElevatedButton(
                    onClick = onDirectionsClicked,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROTA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingBar(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val fullStars = rating.toInt()
        val partialStar = rating - fullStars
        
        repeat(5) { index ->
            when {
                index < fullStars -> Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
                index == fullStars && partialStar > 0 -> {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                            modifier = Modifier
                                .size(20.dp * partialStar.toFloat())
                                .clip(
                                    ClipShape(ratio = partialStar.toFloat())
                                )
                        )
                    }
                }
                else -> Icon(
                    imageVector = Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Özel kırpma şekli
private class ClipShape(private val ratio: Float) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        return androidx.compose.ui.graphics.Outline.Rectangle(
            androidx.compose.ui.geometry.Rect(
                0f, 0f, size.width * ratio, size.height
            )
        )
    }
}