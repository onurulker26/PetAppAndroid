package com.tamer.petapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.tamer.petapp.adapter.BannerAdapter
import com.tamer.petapp.auth.LoginActivity
import com.tamer.petapp.forum.ForumActivity
import com.tamer.petapp.guide.PetCareGuideActivity
import com.tamer.petapp.model.Banner
import com.tamer.petapp.model.Pet
import com.tamer.petapp.treatment.TreatmentsActivity
import com.tamer.petapp.ui.theme.PetAppTheme
import com.tamer.petapp.utils.ImageUtils
import com.tamer.petapp.vaccination.VaccinationsActivity
import com.tamer.petapp.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.*
import com.tamer.petapp.model.NavItem
import com.tamer.petapp.ui.disease.DiseaseAnalysisActivity

class MainActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "MainActivity"
    
    companion object {
        const val ADD_PET_REQUEST_CODE = 100
        const val EDIT_PET_REQUEST_CODE = 200
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Firebase başlatma
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            
            // Kullanıcı giriş yapmamışsa giriş ekranına yönlendir
            if (auth.currentUser == null) {
                Log.d(TAG, "Kullanıcı giriş yapmamış, LoginActivity'ye yönlendiriliyor")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            // Firestore önbellek ayarları
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings

            setContent {
                PetAppTheme {
                    PetAppMainScreen()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası: ${e.message}", e)
            Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private var mainViewModel: MainViewModel? = null
    
    override fun onResume() {
        super.onResume()
        
        try {
            // Ana sayfaya geri döndüğünde evcil hayvan listesini yeniden yükle
            Log.d(TAG, "onResume çağrıldı - evcil hayvan listesi yeniden yükleniyor")
            mainViewModel?.loadPets(auth.currentUser?.uid)
        } catch (e: Exception) {
            Log.e(TAG, "onResume hatası: ${e.message}", e)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        try {
            when (requestCode) {
                ADD_PET_REQUEST_CODE -> {
                    if (resultCode == RESULT_OK) {
                        Log.d(TAG, "Evcil hayvan ekleme başarılı - veri güncelleniyor")
                        // Evcil hayvan başarıyla eklendiyse listeyi yenile
                        mainViewModel?.loadPets(auth.currentUser?.uid)
                        Toast.makeText(this, "Evcil hayvan listesi güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
                EDIT_PET_REQUEST_CODE -> {
                    if (resultCode == RESULT_OK) {
                        Log.d(TAG, "Evcil hayvan düzenleme başarılı - veri güncelleniyor")
                        // Evcil hayvan başarıyla güncellendiyse listeyi yenile
                        mainViewModel?.loadPets(auth.currentUser?.uid)
                        Toast.makeText(this, "Evcil hayvan listesi güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onActivityResult hatası: ${e.message}", e)
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PetAppMainScreen() {
        val viewModel: MainViewModel = viewModel()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        
        // ViewModel referansını sakla
        DisposableEffect(viewModel) {
            mainViewModel = viewModel
            onDispose {
                // Cleanup işlemi gerekirse burada yapılabilir
            }
        }
        
        val pets by viewModel.pets.collectAsState(initial = emptyList())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)
        val banners by viewModel.banners.collectAsState(initial = emptyList())
        val errorMessage by viewModel.errorMessage.collectAsState(initial = "")
        val userPhotoUrl by viewModel.userPhotoUrl.collectAsState(initial = "")
        
        val primaryColor = Color(0xFF673AB7) // Görseldeki mor renk
        val surfaceColor = Color(0xFFFAFAFA) // Arka plan rengi
        
        val navItems = listOf(
            NavItem("Ana Sayfa", Icons.Filled.Home),
            NavItem("Veterinerler", Icons.Filled.LocationOn),
            NavItem("Bakım", Icons.Filled.Settings),
            NavItem("Profil", Icons.Filled.Person),
            NavItem("Hastalık Analizi", Icons.Filled.HealthAndSafety)
        )
        
        var selectedTab by remember { mutableStateOf(0) }
        
        LaunchedEffect(key1 = Unit) {
            viewModel.loadUserProfilePhoto(auth.currentUser?.uid)
            viewModel.loadPets(auth.currentUser?.uid)
            viewModel.loadBanners()
        }
        
        // Hata mesajı varsa göster
        if (errorMessage.isNotEmpty()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Pet App", 
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { 
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEachIndexed { i, item ->
                        NavigationBarItem(
                            selected = selectedTab == i,
                            onClick = {
                                selectedTab = i
                                when (i) {
                                    0 -> {} // Ana sayfa
                                    1 -> context.startActivity(Intent(context, VetClinicsActivity::class.java))
                                    2 -> context.startActivity(Intent(context, PetCareGuideActivity::class.java))
                                    3 -> context.startActivity(Intent(context, ProfileActivity::class.java))
                                    4 -> context.startActivity(Intent(context, DiseaseAnalysisActivity::class.java))
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                indicatorColor = primaryColor.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            containerColor = surfaceColor
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                HomeContent(
                    modifier = Modifier.padding(paddingValues),
                    pets = pets,
                    banners = banners,
                    onPetsClick = {
                        context.startActivity(Intent(context, PetsListActivity::class.java))
                    },
                    onAddPetClick = {
                        val intent = Intent(context, AddPetActivity::class.java)
                        startActivityForResult(intent, ADD_PET_REQUEST_CODE)
                    },
                    onVaccinationClick = {
                        context.startActivity(Intent(context, VaccinationsActivity::class.java))
                    },
                    onTreatmentClick = {
                        context.startActivity(Intent(context, TreatmentsActivity::class.java))
                    },
                    onForumClick = {
                        context.startActivity(Intent(context, ForumActivity::class.java))
                    }
                )
            }
        }
    }
    
    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun HomeContent(
        modifier: Modifier = Modifier,
        pets: List<Pet>,
        banners: List<Banner>,
        onPetsClick: () -> Unit,
        onAddPetClick: () -> Unit,
        onVaccinationClick: () -> Unit,
        onTreatmentClick: () -> Unit,
        onForumClick: () -> Unit
    ) {
        val primaryColor = Color(0xFF673AB7) // Görseldeki mor renk
        
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hoş geldiniz mesajı
            item {
                WelcomeHeader()
            }
            
            // Duyurular başlığı
            item {
                Text(
                    text = "Duyurular",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            
            // Duyurular - Otomatik kaydırmalı
            item {
                BannerSlider(
                    banners = if (banners.isNotEmpty()) banners else listOf(
                        Banner(
                            id = "default1",
                            title = "Aşı Hatırlatma",
                            description = "Dostunuzun aşı takvimine göz atın",
                            buttonText = "Aşılara Git",
                            isActive = true
                        ),
                        Banner(
                            id = "default2",
                            title = "Veteriner Bul",
                            description = "Size yakın veteriner kliniklerini keşfedin",
                            buttonText = "Klinikleri Göster",
                            isActive = true
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            
            // Başlık - Evcil Dostlarınız İçin
            item {
                Text(
                    text = "Evcil Dostlarınız İçin",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            
            // Evcil hayvanlar ve ekle kartları (ilk sıra)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Evcil Hayvanlar kartı
                    PetCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        title = "Evcil Hayvanlarınız",
                        subtitle = "${pets.size} evcil hayvan",
                        iconBackground = Color(0xFFE8DEF8),
                        iconContent = {
                            Icon(
                                imageVector = Icons.Outlined.Pets,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = onPetsClick
                    )
                    
                    // Evcil Hayvan Ekle kartı
                    PetCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        title = "Evcil Hayvan Ekle",
                        subtitle = "Yeni kayıt",
                        iconBackground = Color(0xFFE3F2FD),
                        iconContent = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = onAddPetClick
                    )
                }
            }
            
            // Aşı ve tedavi kartları (ikinci sıra)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Aşı Takibi kartı
                    PetCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        title = "Aşı Takibi",
                        subtitle = "Yaklaşan aşılar",
                        iconBackground = Color(0xFFE8F5E9),
                        iconContent = {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = onVaccinationClick
                    )
                    
                    // Tedavi Takibi kartı
                    PetCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        title = "Tedavi Takibi",
                        subtitle = "Aktif tedaviler",
                        iconBackground = Color(0xFFFEE8E7),
                        iconContent = {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Healing,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                // Bildirim sayısı göstergesi
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFFFF5722), CircleShape)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "3",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        onClick = onTreatmentClick
                    )
                }
            }
            
            // Forum kartı
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clickable { onForumClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Forum ikonu
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Evcil Hayvan Forumu",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Sorular sorun, deneyim paylaşın",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Forumlara Katıl >",
                                color = Color.Yellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun BannerSlider(
        banners: List<Banner>,
        modifier: Modifier = Modifier
    ) {
        if (banners.isEmpty()) return
        
        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        
        // Otomatik kaydırma efekti - 15 saniyede bir
        LaunchedEffect(pagerState) {
            while (true) {
                delay(15000) // 15 saniye bekle
                if (banners.size > 1) {
                    val nextPage = (pagerState.currentPage + 1) % banners.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
        
        Box(
            modifier = modifier
        ) {
            // Kaydırmalı banner
            HorizontalPager(
                count = banners.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val banner = banners[page]
                
                // Banner kartı
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (!banner.buttonUrl.isNullOrEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.buttonUrl))
                                context.startActivity(intent)
                            }
                        }
                ) {
                    // Arkaplan görsel
                    if (!banner.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(banner.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Varsayılan gradient arkaplan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF673AB7),
                                            Color(0xFF311B92)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // İçerik overlay - yarı saydam arkaplan ve içerik
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    // İçerik
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = banner.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = banner.description,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Buton
                        if (!banner.buttonText.isNullOrEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    if (!banner.buttonUrl.isNullOrEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.buttonUrl))
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = banner.buttonText ?: "Detaylar",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Sayfa göstergesi (Dots)
            if (banners.size > 1) {
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
    
    @Composable
    fun WelcomeHeader() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Merhaba, Petci!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Evcil dostlarınız için tüm ihtiyaçlar burada",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
    
    @Composable
    fun BannerCard(
        banner: Banner,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        
        Card(
            modifier = modifier.clickable { 
                // Banner URL'si varsa aç
                if (!banner.buttonUrl.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.buttonUrl))
                    context.startActivity(intent)
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Sol taraf: İçerik
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = banner.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = banner.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Buton göster
                    if (!banner.buttonText.isNullOrEmpty()) {
                        OutlinedButton(
                            onClick = { 
                                if (!banner.buttonUrl.isNullOrEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.buttonUrl))
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White),
                            modifier = Modifier
                                .width(120.dp)
                                .height(32.dp)
                        ) {
                            Text(
                                text = banner.buttonText ?: "Detaylar",
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Sağ taraf: Resim (varsa)
                if (!banner.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(banner.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterVertically),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
    
    @Composable
    fun PetCard(
        modifier: Modifier = Modifier,
        title: String,
        subtitle: String,
        iconBackground: Color,
        iconContent: @Composable () -> Unit,
        onClick: () -> Unit
    ) {
        Card(
            modifier = modifier.clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // İkon alanı
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Başlık
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Alt yazı
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}