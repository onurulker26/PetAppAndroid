package com.tamer.petapp.guide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamer.petapp.MainActivity
import com.tamer.petapp.ProfileActivity
import com.tamer.petapp.VetClinicsActivity
import com.tamer.petapp.R
import com.tamer.petapp.ui.disease.DiseaseAnalysisActivity
import com.tamer.petapp.ui.theme.PetAppTheme

class PetCareGuideActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetAppTheme {
                PetCareGuideScreen(
                    onBackClick = { finish() },
                    onNavigationItemClick = { itemId ->
                        when (itemId) {
                            0 -> { // Ana Sayfa
                                startActivity(Intent(this@PetCareGuideActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                finish()
                            }
                            1 -> { // Veterinerler
                                startActivity(Intent(this@PetCareGuideActivity, VetClinicsActivity::class.java))
                                finish()
                            }
                            3 -> { // Profil
                                startActivity(Intent(this@PetCareGuideActivity, ProfileActivity::class.java))
                                finish()
                            }
                            4 -> { // Hastalık Analizi
                                startActivity(Intent(this@PetCareGuideActivity, DiseaseAnalysisActivity::class.java))
                                finish()
                            }
                        }
                    },
                    onPetTypeClick = { type ->
                        startActivity(
                            PetCareGuideDetailActivity.newIntent(
                                this@PetCareGuideActivity,
                                type
                            )
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCareGuideScreen(
    onBackClick: () -> Unit,
    onNavigationItemClick: (Int) -> Unit,
    onPetTypeClick: (String) -> Unit
) {
    val primaryColor = Color(0xFF673AB7)
    
    // Evcil hayvan türleri
    val petTypes = listOf(
        PetTypeInfo("Köpek", Icons.Default.Pets, R.drawable.ic_dog, "Köpek bakım rehberi"),
        PetTypeInfo("Kedi", Icons.Default.Favorite, R.drawable.ic_cat, "Kedi bakım rehberi"),
        PetTypeInfo("Kuş", Icons.Default.Air, R.drawable.ic_bird, "Kuş bakım rehberi"),
        PetTypeInfo("Balık", Icons.Default.Water, R.drawable.ic_fish, "Balık bakım rehberi"),
        PetTypeInfo("Tavşan", Icons.Default.Grass, R.drawable.ic_rabbit, "Tavşan bakım rehberi"),
        PetTypeInfo("Hamster", Icons.Default.Circle, R.drawable.ic_hamster, "Hamster bakım rehberi")
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bakım Rehberi", color = Color.White) },
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
                )
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
                
                // Bakım (Pet Care Guide) - Seçili
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Bakım") },
                    label = { Text("Bakım") },
                    selected = true,
                    onClick = { /* Zaten buradayız */ }
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
                .background(Color(0xFFF5F5F5))
        ) {
            // Başlık kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor,
                                    primaryColor.copy(alpha = 0.8f),
                                    Color(0xFF9C27B0)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Evcil Hayvan Bakım Rehberi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Evcil hayvanınız için en iyi bakım önerilerini keşfedin",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Evcil hayvan türleri listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(petTypes) { petType ->
                    PetTypeCard(
                        petType = petType,
                        onClick = { onPetTypeClick(petType.name) },
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun PetTypeCard(
    petType: PetTypeInfo,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol taraf - İkon ve arka plan
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor.copy(alpha = 0.1f)
                ),
                border = BorderStroke(2.dp, primaryColor.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = petType.icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Orta kısım - Bilgiler
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = petType.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = petType.description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Özellik etiketleri
                Row {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "Bakım",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "Beslenme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Sağ taraf - Ok ikonu
            Card(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor.copy(alpha = 0.05f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Detaya Git",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Veri sınıfı
data class PetTypeInfo(
    val name: String,
    val icon: ImageVector,
    val imageRes: Int,
    val description: String
) 