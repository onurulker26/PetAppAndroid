package com.tamer.petapp.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userEmail: String,
    userName: String,
    userPhotoUrl: String?,
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onNavigationItemClick: (Int) -> Unit,
    onUpdateProfile: (name: String) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    onLogout: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onAdminPanelClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // State değişkenleri
    var fullName by remember { mutableStateOf(userName) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf("") }
    
    // İzin durumları için state'ler
    var notificationPermissionEnabled by remember { mutableStateOf(true) }
    var locationPermissionEnabled by remember { mutableStateOf(true) }
    
    // İstatistik verileri için state
    var petCount by remember { mutableStateOf(0) }
    var vaccineCount by remember { mutableStateOf(0) }
    var treatmentCount by remember { mutableStateOf(0) }
    var isLoadingStats by remember { mutableStateOf(true) }
    
    // Renk tanımları
    val primaryColor = Color(0xFF673AB7)
    val secondaryColor = Color(0xFFFF9800)
    val lightPrimaryColor = Color(0xFFD1C4E9)
    val backgroundColor = Color(0xFFF5F5F5)
    
    // Gradient arka plan
    val gradientColors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
    
    // Firebase'den istatistik verilerini çek
    LaunchedEffect(key1 = userEmail) {
        loadUserStatistics(
            onPetCount = { petCount = it },
            onVaccineCount = { vaccineCount = it },
            onTreatmentCount = { treatmentCount = it },
            onComplete = { isLoadingStats = false }
        )
    }
    
    // İzin durumlarını kontrol et
    LaunchedEffect(key1 = Unit) {
        // Bildirim izni kontrolü (API 33+)
        notificationPermissionEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Eski sürümlerde varsayılan olarak açık kabul edilir
        }
        
        // Konum izni kontrolü
        locationPermissionEnabled = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                ),
                actions = {
                    // Çıkış butonu
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Çıkış Yap",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
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
                    selected = false,
                    onClick = { onNavigationItemClick(1) }
                )
                
                // Bakım (Pet Care Guide)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Bakım") },
                    label = { Text("Bakım") },
                    selected = false,
                    onClick = { onNavigationItemClick(2) }
                )
                
                // Profil - Seçili
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = true,
                    onClick = { /* Zaten buradayız */ }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 70.dp) // Bottom navigation için boşluk
            ) {
                // Profil Başlık Kartı
                ProfileHeaderCard(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    primaryColor = primaryColor,
                    onPhotoClick = { showPhotoPickerDialog = true }
                )
                
                // Ana İçerik
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // İstatistikler Kartı - Hızlı erişim kısmı kaldırıldı, bu kısım yukarıya taşındı
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Aktivite İstatistikleri",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            if (isLoadingStats) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = primaryColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatisticItem(
                                        icon = Icons.Default.Pets,
                                        value = petCount.toString(),
                                        label = "Evcil Hayvan",
                                        primaryColor = primaryColor
                                    )
                                    
                                    StatisticItem(
                                        icon = Icons.Default.Vaccines,
                                        value = vaccineCount.toString(),
                                        label = "Aşı",
                                        primaryColor = primaryColor
                                    )
                                    
                                    StatisticItem(
                                        icon = Icons.Default.Medication,
                                        value = treatmentCount.toString(),
                                        label = "Tedavi",
                                        primaryColor = primaryColor
                                    )
                                }
                            }
                        }
                    }

                    // Profil Bilgileri Kartı
                    ExpandableCard(
                        title = "Profil Bilgileri",
                        icon = Icons.Default.Person,
                        expanded = expandedSection == "profile",
                        onClick = { 
                            expandedSection = if (expandedSection == "profile") "" else "profile"
                        },
                        primaryColor = primaryColor
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Ad Soyad alanı
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Ad Soyad") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )
                            
                            // E-posta (sadece gösterim)
                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = { },
                                label = { Text("E-posta") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                readOnly = true,
                                enabled = false
                            )
                            
                            // Güncelleme butonu
                            Button(
                                onClick = { onUpdateProfile(fullName) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Kaydet",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Profili Güncelle",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Güvenlik Ayarları Kartı
                    ExpandableCard(
                        title = "Güvenlik Ayarları",
                        icon = Icons.Default.Security,
                        expanded = expandedSection == "security",
                        onClick = { 
                            expandedSection = if (expandedSection == "security") "" else "security"
                        },
                        primaryColor = primaryColor
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Şifre değiştirme butonu
                            Button(
                                onClick = { showPasswordDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Şifre Değiştir",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Şifre Değiştir",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Bildirim ayarları (örnek olarak)
                            ListItem(
                                headlineContent = { Text("Bildirimler") },
                                supportingContent = { Text("Bildirim tercihlerinizi yönetin") },
                                leadingContent = { 
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = notificationPermissionEnabled,
                                        onCheckedChange = { newValue ->
                                            if (newValue && !notificationPermissionEnabled) {
                                                // İzin iste
                                                onRequestNotificationPermission()
                                            }
                                        }
                                    )
                                }
                            )
                            
                            Divider()
                            
                            // Konum izinleri (örnek olarak)
                            ListItem(
                                headlineContent = { Text("Konum İzinleri") },
                                supportingContent = { Text("Konumunuzu kullanma izinleri") },
                                leadingContent = { 
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = locationPermissionEnabled,
                                        onCheckedChange = { newValue ->
                                            if (newValue && !locationPermissionEnabled) {
                                                // İzin iste
                                                onRequestLocationPermission()
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                    
                    // Admin panel kartı (sadece admin kullanıcılar için görünür)
                    if (isAdmin) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            ),
                            border = BorderStroke(1.dp, secondaryColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = secondaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Admin Paneli",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryColor
                                    )
                                }
                                
                                Button(
                                    onClick = onAdminPanelClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = secondaryColor
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Admin Paneli",
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Admin Paneline Git",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Dialog'lar
        // Şifre değiştirme dialog'u
        if (showPasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showPasswordDialog = false },
                onConfirm = { currentPassword, newPassword ->
                    onChangePassword(currentPassword, newPassword)
                    showPasswordDialog = false
                }
            )
        }
        
        // Çıkış yapma onay dialog'u
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Çıkış Yap") },
                text = { Text("Hesabınızdan çıkış yapmak istediğinizden emin misiniz?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onLogout()
                            showLogoutDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Evet, Çıkış Yap")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("İptal")
                    }
                }
            )
        }
        
        // Fotoğraf seçme dialog'u
        if (showPhotoPickerDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoPickerDialog = false },
                title = { Text("Profil Fotoğrafı Seç") },
                text = { Text("Profil fotoğrafınızı nasıl güncellemek istersiniz?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onTakePhoto()
                            showPhotoPickerDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Kamera ile Çek")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            onPickImage()
                            showPhotoPickerDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Galeriden Seç")
                    }
                }
            )
        }
    }
}

// Firebase'den kullanıcı istatistiklerini çek
suspend fun loadUserStatistics(
    onPetCount: (Int) -> Unit,
    onVaccineCount: (Int) -> Unit,
    onTreatmentCount: (Int) -> Unit,
    onComplete: () -> Unit
) {
    try {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            var pets = 0
            var vaccines = 0
            var treatments = 0
            
            // Evcil hayvan sayısını çek
            try {
                val petsSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("pets")
                    .get()
                    .await()
                pets = petsSnapshot.size()
                onPetCount(pets)
                
                // Her evcil hayvan için aşıları say
                for (petDoc in petsSnapshot.documents) {
                    try {
                        val petVaccinations = firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("pets")
                            .document(petDoc.id)
                            .collection("vaccinations")
                            .get()
                            .await()
                        vaccines += petVaccinations.size()
                    } catch (e: Exception) {
                        // Pet'in aşılarını çekerken hata olsa bile devam et
                    }
                }
                onVaccineCount(vaccines)
                
                // Tedavi sayısını çek - Hem ana treatments koleksiyonundan hem de pets altındaki treatments'ları say
                try {
                    var totalTreatments = 0
                    
                    // Ana treatments koleksiyonunu kontrol et
                    try {
                        val mainTreatmentsSnapshot = firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("treatments")
                            .get()
                            .await()
                        totalTreatments += mainTreatmentsSnapshot.size()
                        android.util.Log.d("ProfileScreen", "Ana treatments koleksiyonu: ${mainTreatmentsSnapshot.size()}")
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "Ana treatments koleksiyonu hatası: ${e.message}")
                    }
                    
                    // Her pet için treatments koleksiyonunu kontrol et
                    for (petDoc in petsSnapshot.documents) {
                        try {
                            val petTreatments = firestore.collection("users")
                                .document(currentUser.uid)
                                .collection("pets")
                                .document(petDoc.id)
                                .collection("treatments")
                                .get()
                                .await()
                            totalTreatments += petTreatments.size()
                            android.util.Log.d("ProfileScreen", "Pet ${petDoc.id} treatments: ${petTreatments.size()}")
                        } catch (e: Exception) {
                            android.util.Log.e("ProfileScreen", "Pet ${petDoc.id} treatments hatası: ${e.message}")
                        }
                    }
                    
                    // Ayrıca medical_records koleksiyonunu da kontrol et (eski veriler için)
                    try {
                        val medicalRecordsSnapshot = firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("medical_records")
                            .whereEqualTo("type", "treatment")
                            .get()
                            .await()
                        totalTreatments += medicalRecordsSnapshot.size()
                        android.util.Log.d("ProfileScreen", "Medical records treatments: ${medicalRecordsSnapshot.size()}")
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "Medical records hatası: ${e.message}")
                    }
                    
                    onTreatmentCount(totalTreatments)
                    android.util.Log.d("ProfileScreen", "Toplam tedavi sayısı: $totalTreatments")
                    
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "Tedavi sayısı çekerken genel hata: ${e.message}")
                    onTreatmentCount(0)
                }
                
            } catch (e: Exception) {
                // Hata durumunda 0 olarak kalacak
                onPetCount(0)
                onVaccineCount(0)
                onTreatmentCount(0)
            }
        }
    } catch (e: Exception) {
        // Genel hata durumu
        onPetCount(0)
        onVaccineCount(0)
        onTreatmentCount(0)
    } finally {
        onComplete()
    }
}

// Güncellenmiş ProfileHeaderCard - Base64 profil fotoğrafı desteği ile
@Composable
fun ProfileHeaderCard(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    primaryColor: Color,
    onPhotoClick: () -> Unit
) {
    val context = LocalContext.current
    var profileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Base64 string'i bitmap'e çevir
    LaunchedEffect(userPhotoUrl) {
        if (!userPhotoUrl.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(userPhotoUrl, Base64.DEFAULT)
                profileBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                profileBitmap = null
            }
        } else {
            profileBitmap = null
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Profil fotoğrafı
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onPhotoClick() }
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap!!.asImageBitmap(),
                            contentDescription = "Profil Fotoğrafı",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Varsayılan profil resmi
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                    }
                    
                    // Fotoğraf düzenleme simgesi
                    FloatingActionButton(
                        onClick = onPhotoClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        containerColor = Color.White,
                        contentColor = primaryColor
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Fotoğrafı Değiştir",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Kullanıcı adı
                Text(
                    text = userName.ifEmpty { "Kullanıcı Adı" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Kullanıcı emaili
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    primaryColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = primaryColor.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ExpandableCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Başlık
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    tint = primaryColor
                )
            }
            
            // İçerik
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Şifre Değiştir",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Mevcut Şifre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    visualTransformation = if (passwordVisible) PasswordVisualTransformation() else PasswordVisualTransformation(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"
                            )
                        }
                    }
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        passwordError = false
                    },
                    label = { Text("Yeni Şifre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = passwordError,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        passwordError = false
                    },
                    label = { Text("Yeni Şifreyi Onayla") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = passwordError,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                )
                
                if (passwordError) {
                    Text(
                        text = "Şifreler eşleşmiyor veya şifre çok kısa (min. 6 karakter)",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("İptal")
                    }
                    
                    Button(
                        onClick = {
                            if (newPassword == confirmPassword && newPassword.length >= 6) {
                                onConfirm(currentPassword, newPassword)
                            } else {
                                passwordError = true
                            }
                        }
                    ) {
                        Text("Şifreyi Değiştir")
                    }
                }
            }
        }
    }
} 