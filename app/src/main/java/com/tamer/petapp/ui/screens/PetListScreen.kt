package com.tamer.petapp.ui.screens

import android.util.Log
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamer.petapp.model.Pet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetListScreen(
    pets: List<Pet>,
    onPetClick: (Pet) -> Unit,
    onAddPetClick: () -> Unit,
    onEditPetClick: (Pet) -> Unit,
    onDeletePetClick: (Pet) -> Unit
) {
    val primaryColor = Color(0xFF5B37B7)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evcil Hayvanlarım", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPetClick,
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Evcil Hayvan Ekle")
            }
        }
    ) { innerPadding ->
        if (pets.isEmpty()) {
            // Evcil hayvan yoksa boş durum ekranı
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Henüz evcil hayvanınız yok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Evcil hayvan eklemek için artı butonuna tıklayın",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Evcil hayvan listesi
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pets) { pet ->
                    PetItemCard(
                        pet = pet,
                        onClick = { onPetClick(pet) },
                        onEditClick = { onEditPetClick(pet) },
                        onDeleteClick = { onDeletePetClick(pet) }
                    )
                }
            }
        }
    }
}

@Composable
fun PetItemCard(
    pet: Pet,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet resmi
            if (pet.imageUrl.isNotEmpty() || pet.base64Image.isNotEmpty()) {
                val imageData = when {
                    pet.base64Image.isNotEmpty() -> pet.base64Image
                    else -> pet.imageUrl
                }
                
                // Resim yükleme yerine sadece evcil hayvan baş harfini içeren bir görsel gösteriyoruz
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5B37B7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pet.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B37B7)
                    )
                }
            } else {
                // Resim yoksa avatar
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E1E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pet.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
            
            // Pet bilgileri
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = pet.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${pet.type} - ${pet.breed}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                // Doğum tarihi
                if (pet.birthDate > 0) {
                    val birthDateStr = dateFormat.format(Date(pet.birthDate))
                    val ageInYears = calculateAgeInYears(pet.birthDate)
                    Text(
                        text = "$birthDateStr (${ageInYears} yaş)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // İşlem butonları
            Column(
                horizontalAlignment = Alignment.End
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Color(0xFF5B37B7)
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

// Yaş hesaplama fonksiyonu
private fun calculateAgeInYears(birthdateMillis: Long): Int {
    val birthdate = Calendar.getInstance().apply { timeInMillis = birthdateMillis }
    val today = Calendar.getInstance()
    
    var age = today.get(Calendar.YEAR) - birthdate.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birthdate.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    
    return age
} 