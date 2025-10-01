package com.tamer.petapp

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.auth.LoginActivity
import com.tamer.petapp.model.Pet
import com.tamer.petapp.ui.theme.PetAppTheme
import com.tamer.petapp.viewmodel.PetViewModel

class PetsListActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "PetsListActivity"

    companion object {
        const val ADD_PET_REQUEST_CODE = 100
        const val EDIT_PET_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Firebase başlatma
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // Kullanıcı giriş yapmamışsa giriş ekranına yönlendir
            if (auth.currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            setContent {
                PetAppTheme {
                    val viewModel = viewModel<PetViewModel>()
                    val pets by viewModel.pets.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()
                    val errorMessage by viewModel.errorMessage.collectAsState()
                    val context = LocalContext.current
                    
                    // Hata mesajı varsa göster
                    if (errorMessage.isNotEmpty()) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
                    }
                    
                    // İlk yükleme
                    LaunchedEffect(key1 = Unit) {
                        viewModel.loadPets(auth.currentUser?.uid)
                    }
                    
                    PetsScreen(
                        pets = pets,
                        isLoading = isLoading,
                        onAddPetClick = {
                    startActivityForResult(
                                Intent(context, AddPetActivity::class.java),
                        ADD_PET_REQUEST_CODE
                    )
                        },
                        onEditPetClick = { pet ->
                            val intent = Intent(context, AddPetActivity::class.java).apply {
                                putExtra("pet_id", pet.id)
                                putExtra("is_edit_mode", true)
                            }
                            startActivityForResult(intent, EDIT_PET_REQUEST_CODE)
                        },
                        onDeletePetClick = { pet ->
                            showDeleteConfirmationDialog(pet, viewModel)
                        },
                        onBackClick = {
                            finish()
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate sırasında hata: ${e.message}", e)
            Toast.makeText(this, "Uygulama başlatılırken hata: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showDeleteConfirmationDialog(pet: Pet, viewModel: PetViewModel) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Evcil Hayvan Sil")
            .setMessage("${pet.name} isimli evcil hayvanınızı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz ve tüm aşı kayıtları da silinecektir.")
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deletePet(pet) { success ->
                    if (success) {
                        Toast.makeText(this, "${pet.name} başarıyla silindi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Silme işlemi başarısız", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ADD_PET_REQUEST_CODE || requestCode == EDIT_PET_REQUEST_CODE) && 
            resultCode == RESULT_OK) {
            // Aktivitede yeni bir viewModel oluşturup yükleyelim
            auth.currentUser?.uid?.let { uid ->
                val newViewModel = PetViewModel()
                newViewModel.loadPets(uid)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    pets: List<Pet>,
    isLoading: Boolean = false,
    onAddPetClick: () -> Unit,
    onEditPetClick: (Pet) -> Unit,
    onDeletePetClick: (Pet) -> Unit,
    onBackClick: () -> Unit
) {
    val primaryColor = Color(0xFF5B37B7)
    val accentColor = Color(0xFFFF9800)
    val backgroundColor = Color(0xFFF6F6F6)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evcil Hayvanlarınız") },
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
                onClick = onAddPetClick,
                containerColor = accentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Evcil Hayvan Ekle")
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
            } else if (pets.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Henüz evcil hayvan eklenmemiş",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Evcil hayvan eklemek için + butonuna dokunun",
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
                    items(pets.sortedBy { it.name }) { pet ->
                        PetItem(
                            pet = pet,
                            onEditClick = { onEditPetClick(pet) },
                            onDeleteClick = { onDeletePetClick(pet) }
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
fun PetItem(
    pet: Pet,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryColor = Color(0xFF5B37B7)
    val cardBackgroundColor = Color.White
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Renkli avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(getPetTypeColor(pet.type)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pet.name.firstOrNull()?.toString() ?: "P",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                    }
            
            // Evcil hayvan bilgileri
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = pet.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${pet.type}${if (pet.breed.isNotEmpty()) ", ${pet.breed}" else ""}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Düzenle ve Sil düğmeleri
            Row {
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
        }
    }
}

@Composable
fun getPetTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "kedi" -> Color(0xFFE91E63) // Pembe
        "köpek" -> Color(0xFF2196F3) // Mavi
        "kuş" -> Color(0xFF4CAF50)   // Yeşil
        "balık" -> Color(0xFF00BCD4) // Turkuaz
        else -> Color(0xFFFF5722)    // Turuncu - diğer
    }
} 