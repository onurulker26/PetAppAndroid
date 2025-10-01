package com.tamer.petapp.treatment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.model.Pet
import com.tamer.petapp.model.Treatment
import com.tamer.petapp.model.TreatmentStatus
import com.tamer.petapp.ui.theme.PetAppTheme
import java.text.SimpleDateFormat
import java.util.*

class TreatmentActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "TreatmentActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Firebase başlatma
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            
            setContent {
                PetAppTheme {
                    // State değişkenleri
                    var treatments by remember { mutableStateOf<List<Treatment>>(emptyList()) }
                    var pets by remember { mutableStateOf<Map<String, Pet>>(emptyMap()) }
                    var isLoading by remember { mutableStateOf(true) }
                    
                    // Verileri yükle
                    LaunchedEffect(key1 = Unit) {
                        loadTreatments { treatmentsList, petsMap ->
                            treatments = treatmentsList
                            pets = petsMap
                            isLoading = false
                        }
                    }
                    
                    // Tedavi ekranını göster
                    TreatmentScreen(
                        treatments = treatments,
                        pets = pets,
                        isLoading = isLoading,
                        onAddTreatmentClick = {
                            startActivity(Intent(this, AddTreatmentActivity::class.java))
                        },
                        onTreatmentClick = { treatment ->
                            val intent = Intent(this, TreatmentDetailsActivity::class.java)
                            intent.putExtra("treatmentId", treatment.id)
                            intent.putExtra("petId", treatment.petId)
                            startActivity(intent)
                        },
                        onEditClick = { treatment ->
                            val intent = Intent(this, AddTreatmentActivity::class.java)
                            intent.putExtra("treatmentId", treatment.id)
                            intent.putExtra("petId", treatment.petId)
                            intent.putExtra("isEditing", true)
                            startActivity(intent)
                        },
                        onDeleteClick = { treatment ->
                            showDeleteConfirmationDialog(treatment)
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hata: ${e.message}", e)
            Toast.makeText(this, "Uygulama başlatılırken hata: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun loadTreatments(onComplete: (List<Treatment>, Map<String, Pet>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Kullanıcı oturum açmamış")
            onComplete(emptyList(), emptyMap())
            return
        }
        
        try {
            val loadedTreatments = mutableListOf<Treatment>()
            val petsMap = mutableMapOf<String, Pet>()
            
            // SADECE kullanıcıya ait evcil hayvanları users/{userId}/pets koleksiyonundan yükle
            firestore.collection("users")
                .document(userId)
                .collection("pets")
                .get()
                .addOnSuccessListener { userPetsResult ->
                    Log.d(TAG, "Kullanıcı koleksiyonundan yüklenen evcil hayvan sayısı: ${userPetsResult.size()}")
                    
                    // Evcil hayvanları map'e ekle
                    for (petDoc in userPetsResult.documents) {
                        try {
                            val pet = petDoc.toObject(Pet::class.java)
                            if (pet != null) {
                                pet.id = petDoc.id // ID'yi manuel olarak ayarlayalım
                                petsMap[pet.id] = pet
                                Log.d(TAG, "Evcil hayvan yüklendi: ${pet.name}, ID: ${pet.id}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Pet verisi dönüştürme hatası: ${e.message}", e)
                        }
                    }
                    
                    // Evcil hayvanlar yoksa boş liste döndür
                    if (petsMap.isEmpty()) {
                        Log.d(TAG, "Hiç evcil hayvan bulunamadı, boş liste döndürülüyor")
                        onComplete(emptyList(), emptyMap())
                        return@addOnSuccessListener
                    }
                    
                    // Tedavileri yükle - sadece users/{userId}/treatments koleksiyonundan
                    loadUserTreatments(userId, petsMap, loadedTreatments, onComplete)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Evcil hayvan yükleme hatası: ${e.message}", e)
                    onComplete(emptyList(), emptyMap())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Veri yükleme hatası: ${e.message}", e)
            onComplete(emptyList(), emptyMap())
        }
    }
    
    private fun loadUserTreatments(
        userId: String,
        petsMap: Map<String, Pet>,
        loadedTreatments: MutableList<Treatment>,
        onComplete: (List<Treatment>, Map<String, Pet>) -> Unit
    ) {
        try {
            // SADECE users/{userId}/treatments koleksiyonundan tedavileri yükle
            firestore.collection("users")
                .document(userId)
                .collection("treatments")
                .get()
                .addOnSuccessListener { userTreatmentsResult ->
                    Log.d(TAG, "Kullanıcı koleksiyonundan yüklenen tedavi sayısı: ${userTreatmentsResult.size()}")
                    
                    // Kullanıcı koleksiyonundan tedavileri ekle
                    for (doc in userTreatmentsResult.documents) {
                        try {
                            val treatment = doc.toObject(Treatment::class.java)
                            if (treatment != null) {
                                treatment.id = doc.id
                                
                                // ownerId yoksa ekle
                                if (treatment.ownerId.isEmpty()) {
                                    treatment.ownerId = userId
                                }
                                
                                // Eğer geçerli bir petId varsa listeye ekle
                                if (treatment.petId.isNotEmpty() && petsMap.containsKey(treatment.petId)) {
                                    loadedTreatments.add(treatment)
                                    Log.d(TAG, "Tedavi eklendi: ${treatment.name} (${treatment.id})")
                                } else {
                                    Log.w(TAG, "Tedavi için geçerli evcil hayvan bulunamadı, PetID: ${treatment.petId}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Tedavi verisi dönüştürme hatası: ${e.message}", e)
                        }
                    }
                    
                    // Sonuçları döndür
                    Log.d(TAG, "Toplam yüklenen tedavi sayısı: ${loadedTreatments.size}")
                    Log.d(TAG, "Toplam yüklenen evcil hayvan sayısı: ${petsMap.size}")
                    onComplete(loadedTreatments, petsMap)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Tedavi yükleme hatası: ${e.message}", e)
                    // Hata olsa da mevcut yüklenen verileri döndür
                    onComplete(loadedTreatments, petsMap)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Tedavi yükleme kritik hatası: ${e.message}", e)
            onComplete(loadedTreatments, petsMap)
        }
    }
    
    private fun showDeleteConfirmationDialog(treatment: Treatment) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tedavi Sil")
                .setMessage("${treatment.name} isimli tedaviyi silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    deleteTreatment(treatment)
                }
                .setNegativeButton("Hayır", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Dialog gösterme hatası: ${e.message}", e)
            Toast.makeText(this, "İşlem gerçekleştirilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteTreatment(treatment: Treatment) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Oturum açmanız gerekiyor", Toast.LENGTH_SHORT).show()
                return
            }
            
            // SADECE users/{userId}/treatments koleksiyonundan sil
            firestore.collection("users")
                .document(userId)
                .collection("treatments")
                .document(treatment.id)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Tedavi başarıyla silindi: ${treatment.id}")
                    Toast.makeText(this, "${treatment.name} başarıyla silindi", Toast.LENGTH_SHORT).show()
                    recreate() // Ekranı yenile
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Tedavi silme hatası: ${e.message}", e)
                    Toast.makeText(this, "Silme işlemi başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Silme işlemi kritik hatası: ${e.message}", e)
            Toast.makeText(this, "Silme işlemi gerçekleştirilemedi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentScreen(
    treatments: List<Treatment>,
    pets: Map<String, Pet>,
    isLoading: Boolean = false,
    onAddTreatmentClick: () -> Unit,
    onTreatmentClick: (Treatment) -> Unit,
    onEditClick: (Treatment) -> Unit,
    onDeleteClick: (Treatment) -> Unit,
    onBackClick: () -> Unit
) {
    val primaryColor = Color(0xFF5B37B7)
    val accentColor = Color(0xFFE45C3A)
    val backgroundColor = Color(0xFFF6F6F6)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tedavi Takibi") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTreatmentClick,
                containerColor = accentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tedavi Ekle")
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = primaryColor
                )
            } else if (treatments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Henüz tedavi kaydı bulunmuyor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Evcil hayvanınızın tedavi bilgilerini eklemek için + butonuna dokunun",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(treatments.sortedByDescending { it.startDate }) { treatment ->
                        TreatmentItem(
                            treatment = treatment,
                            petName = pets[treatment.petId]?.name ?: "Bilinmeyen Hayvan",
                            onItemClick = { onTreatmentClick(treatment) },
                            onEditClick = { onEditClick(treatment) },
                            onDeleteClick = { onDeleteClick(treatment) }
                        )
                    }
                    // Alt boşluk
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }
}

@Composable
fun TreatmentItem(
    treatment: Treatment,
    petName: String,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val startDateFormatted = dateFormat.format(treatment.startDate)
    val endDateFormatted = treatment.endDate?.let { dateFormat.format(it) } ?: "Devam ediyor"
    
    // Renk paleti
    val primaryColor = Color(0xFF5B37B7)
    val statusColor = when (treatment.status) {
        TreatmentStatus.ACTIVE -> Color(0xFF4CAF50)  // Yeşil
        TreatmentStatus.COMPLETED -> Color(0xFF2196F3)  // Mavi
        TreatmentStatus.CANCELLED -> Color(0xFFE91E63)  // Kırmızı
    }
    val cardBackgroundColor = Color.White
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tedavi durumu göstergesi
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Tedavi adı
                Text(
                    text = treatment.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Düzenle ve Sil düğmeleri
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = primaryColor
                    )
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color.Red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Evcil hayvan adı
            Text(
                text = "Evcil Hayvan: $petName",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Tarih bilgileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Başlangıç: $startDateFormatted",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "Bitiş: $endDateFormatted",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Durum bilgisi
            val statusText = when (treatment.status) {
                TreatmentStatus.ACTIVE -> "Aktif"
                TreatmentStatus.COMPLETED -> "Tamamlandı"
                TreatmentStatus.CANCELLED -> "İptal Edildi"
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Durum: $statusText",
                fontSize = 14.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 