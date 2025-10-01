package com.tamer.petapp.pets

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.AddPetActivity
import com.tamer.petapp.model.Pet
import com.tamer.petapp.ui.theme.PetAppTheme
import android.content.Intent
import android.app.AlertDialog

class PetDetailActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var petId: String = ""
    private val TAG = "PetDetailActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Firebase başlatma
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // petId'yi intent'ten al
        petId = intent.getStringExtra("petId") ?: ""
        
        if (petId.isEmpty()) {
            Toast.makeText(this, "Evcil hayvan bilgisi bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setContent {
            PetAppTheme {
                var pet by remember { mutableStateOf<Pet?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                
                // Evcil hayvan verilerini yükle
                LaunchedEffect(key1 = petId) {
                    loadPetData(petId) { loadedPet ->
                        pet = loadedPet
                        isLoading = false
                    }
                }
                
                PetDetailScreen(
                    pet = pet,
                    isLoading = isLoading,
                    onBackClick = { finish() },
                    onEditClick = { selectedPet ->
                        val intent = Intent(this, AddPetActivity::class.java).apply {
                            putExtra("pet_id", selectedPet.id)
                            putExtra("is_edit_mode", true)
                        }
                        startActivity(intent)
                    },
                    onDeleteClick = { selectedPet ->
                        showDeleteConfirmationDialog(selectedPet)
                    }
                )
            }
        }
    }
    
    private fun loadPetData(petId: String, onComplete: (Pet?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Kullanıcı oturum açmamış")
            onComplete(null)
            return
        }
        
        firestore.collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val pet = Pet(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            type = document.getString("type") ?: "",
                            breed = document.getString("breed") ?: "",
                            birthDate = document.getLong("birthDate") ?: 0,
                            gender = document.getString("gender") ?: "",
                            weight = document.getDouble("weight") ?: 0.0,
                            imageUrl = document.getString("imageUrl") ?: "",
                            base64Image = document.getString("base64Image") ?: "",
                            ownerId = document.getString("ownerId") ?: "",
                            notes = document.getString("notes") ?: "",
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        
                        onComplete(pet)
                    } catch (e: Exception) {
                        Log.e(TAG, "Pet verisi dönüştürme hatası: ${e.message}")
                        onComplete(null)
                    }
                } else {
                    Log.d(TAG, "Evcil hayvan bulunamadı: $petId")
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Evcil hayvan yükleme hatası: ${e.message}")
                onComplete(null)
            }
    }
    
    private fun showDeleteConfirmationDialog(pet: Pet) {
        AlertDialog.Builder(this)
            .setTitle("Evcil Hayvan Sil")
            .setMessage("${pet.name} isimli evcil hayvanınızı silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deletePet(pet)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }
    
    private fun deletePet(pet: Pet) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
            return
        }
        
        firestore.collection("pets")
            .document(pet.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${pet.name} başarıyla silindi", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Evcil hayvan silme hatası: ${e.message}")
                Toast.makeText(this, "Silme işlemi başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    pet: Pet?,
    isLoading: Boolean = false,
    onBackClick: () -> Unit,
    onEditClick: (Pet) -> Unit,
    onDeleteClick: (Pet) -> Unit
) {
    val primaryColor = Color(0xFF5B37B7)
    val accentColor = Color(0xFFE45C3A)
    val backgroundColor = Color(0xFFF6F6F6)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Evcil Hayvan Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    pet?.let {
                        IconButton(onClick = { onEditClick(it) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { onDeleteClick(it) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = primaryColor
                )
            } else if (pet == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Evcil hayvan bulunamadı",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Evcil hayvan detaylarını göster
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Evcil hayvan fotoğrafı
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!pet.imageUrl.isNullOrEmpty() || !pet.base64Image.isNullOrEmpty()) {
                            val imageSource = if (!pet.base64Image.isNullOrEmpty()) {
                                pet.base64Image
                            } else {
                                pet.imageUrl
                            }
                            
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageSource)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Evcil Hayvan Resmi",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Avatar göster (ilk harfi)
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pet.name.firstOrNull()?.toString() ?: "?",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 48.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // İsim
                    Text(
                        text = pet.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tür, Irk, Cinsiyet bilgileri
                    DetailRow(title = "Tür", value = pet.type)
                    DetailRow(title = "Irk", value = pet.breed.ifEmpty { "Belirtilmemiş" })
                    DetailRow(title = "Cinsiyet", value = pet.gender)
                    
                    // Doğum Tarihi
                    if (pet.birthDate > 0) {
                        val birthDate = java.util.Date(pet.birthDate)
                        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        DetailRow(title = "Doğum Tarihi", value = dateFormat.format(birthDate))
                    }
                    
                    // Ağırlık
                    if (pet.weight > 0) {
                        DetailRow(title = "Ağırlık", value = "${pet.weight} kg")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notlar
                    if (!pet.notes.isNullOrEmpty()) {
                        Text(
                            text = "Notlar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = pet.notes,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp
        )
    }
    Divider(modifier = Modifier.padding(vertical = 8.dp))
} 