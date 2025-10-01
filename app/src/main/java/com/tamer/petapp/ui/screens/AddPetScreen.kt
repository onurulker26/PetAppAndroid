package com.tamer.petapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tamer.petapp.model.Pet
import com.tamer.petapp.R
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    isEditMode: Boolean = false,
    petName: String = "",
    petType: String = "",
    petBreed: String = "",
    petGender: String = "",
    petBirthdate: Long = 0L,
    petWeight: Double = 0.0,
    petImageUrl: String = "",
    petNotes: String = "",
    onSave: (
        name: String,
        type: String,
        breed: String,
        gender: String,
        birthdate: Long,
        weight: Double,
        imageUri: Uri?,
        notes: String
    ) -> Unit,
    onCancel: () -> Unit,
    onTakePhoto: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State değişkenleri
    var name by remember { mutableStateOf(petName) }
    var type by remember { mutableStateOf(petType) }
    var breed by remember { mutableStateOf(petBreed) }
    var gender by remember { mutableStateOf(petGender) }
    var birthdate by remember { mutableStateOf(petBirthdate) }
    var weight by remember { mutableStateOf(if (petWeight > 0) petWeight.toString() else "") }
    var notes by remember { mutableStateOf(petNotes) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    
    // Galeri için launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            onImageSelected(it)
        }
    }
    
    // Renk tanımları
    val primaryColor = Color(0xFF673AB7)
    val secondaryColor = Color(0xFFFF9800)
    val backgroundColor = Color(0xFFF5F5F5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditMode) "Evcil Hayvan Düzenle" else "Evcil Hayvan Ekle",
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
                    IconButton(
                        onClick = {
                            if (validateInput(context, name, type)) {
                                onSave(
                                    name,
                                    type,
                                    breed,
                                    gender,
                                    birthdate,
                                    weight.toDoubleOrNull() ?: 0.0,
                                    imageUri,
                                    notes
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Kaydet",
                            tint = Color.White
                        )
                    }
                }
            )
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Evcil hayvan fotoğrafı seçimi
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { showPhotoOptionsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Evcil Hayvan Fotoğrafı",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (petImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(petImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Evcil Hayvan Fotoğrafı",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Evcil Hayvan Fotoğrafı Ekle",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(16.dp),
                            tint = Color.White
                        )
                    }
                    
                    // Fotoğraf ekleme butonu
                    FloatingActionButton(
                        onClick = { showPhotoOptionsDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp)
                            .offset(x = (-8).dp, y = (-8).dp),
                        containerColor = secondaryColor
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Fotoğraf Ekle",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Bilgi girişi formları
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Temel Bilgiler",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // İsim
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("İsim") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Pets, contentDescription = null)
                            },
                            isError = name.isEmpty(),
                            supportingText = {
                                if (name.isEmpty()) {
                                    Text("İsim zorunludur")
                                }
                            },
                            singleLine = true
                        )
                        
                        // Tür seçimi
                        OutlinedTextField(
                            value = type,
                            onValueChange = { },
                            label = { Text("Tür") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { showTypeDialog = true },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null)
                            },
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                IconButton(onClick = { showTypeDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Tür Seç")
                                }
                            },
                            isError = type.isEmpty(),
                            supportingText = {
                                if (type.isEmpty()) {
                                    Text("Tür seçimi zorunludur")
                                }
                            },
                            singleLine = true
                        )
                        
                        // Cins
                        OutlinedTextField(
                            value = breed,
                            onValueChange = { breed = it },
                            label = { Text("Cins") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Pets, contentDescription = null)
                            },
                            singleLine = true
                        )
                        
                        // Cinsiyet seçimi
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { },
                            label = { Text("Cinsiyet") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { showGenderDialog = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (gender) {
                                        "Erkek" -> Icons.Default.Male
                                        "Dişi" -> Icons.Default.Female
                                        else -> Icons.Default.QuestionMark
                                    },
                                    contentDescription = null
                                )
                            },
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                IconButton(onClick = { showGenderDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Cinsiyet Seç")
                                }
                            },
                            singleLine = true
                        )
                    }
                }
                
                // Ek Bilgiler
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Ek Bilgiler",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Doğum Tarihi
                        OutlinedTextField(
                            value = if (birthdate > 0) {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthdate))
                            } else {
                                ""
                            },
                            onValueChange = { },
                            label = { Text("Doğum Tarihi") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { showDatePicker = true },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            },
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Tarih Seç")
                                }
                            },
                            singleLine = true
                        )
                        
                        // Ağırlık
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { 
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    weight = it
                                }
                            },
                            label = { Text("Ağırlık (kg)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Scale, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true
                        )
                        
                        // Notlar
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notlar") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Note, contentDescription = null)
                            }
                        )
                    }
                }
                
                // Kaydet butonu
                Button(
                    onClick = {
                        if (validateInput(context, name, type)) {
                            onSave(
                                name,
                                type,
                                breed,
                                gender,
                                birthdate,
                                weight.toDoubleOrNull() ?: 0.0,
                                imageUri,
                                notes
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
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
                        text = if (isEditMode) "Güncelle" else "Kaydet",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // İptal butonu
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "İptal",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "İptal",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Dialoglar
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                onDateSelected = { 
                    birthdate = it
                    showDatePicker = false
                },
                initialDate = if (birthdate > 0) Date(birthdate) else Date()
            )
        }
        
        if (showTypeDialog) {
            PetTypeDialog(
                onDismissRequest = { showTypeDialog = false },
                onTypeSelected = { 
                    type = it
                    showTypeDialog = false
                }
            )
        }
        
        if (showGenderDialog) {
            PetGenderDialog(
                onDismissRequest = { showGenderDialog = false },
                onGenderSelected = { 
                    gender = it
                    showGenderDialog = false
                }
            )
        }
        
        if (showPhotoOptionsDialog) {
            PhotoOptionsDialog(
                onDismissRequest = { showPhotoOptionsDialog = false },
                onTakePhoto = {
                    onTakePhoto()
                    showPhotoOptionsDialog = false
                },
                onPickFromGallery = {
                    galleryLauncher.launch("image/*")
                    showPhotoOptionsDialog = false
                }
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit,
    initialDate: Date
) {
    val calendar = Calendar.getInstance()
    calendar.time = initialDate
    
    // Basit tarih seçimi
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Tarih Seçin") },
        text = {
            Column {
                // Yıl seçimi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yıl:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Yıl")
                        }
                        Text(selectedYear.toString())
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Yıl")
                        }
                    }
                }
                
                // Ay seçimi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ay:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            if (selectedMonth > 0) selectedMonth-- else selectedMonth = 11
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Ay")
                        }
                        Text(
                            when (selectedMonth) {
                                0 -> "Ocak"
                                1 -> "Şubat"
                                2 -> "Mart"
                                3 -> "Nisan"
                                4 -> "Mayıs"
                                5 -> "Haziran"
                                6 -> "Temmuz"
                                7 -> "Ağustos"
                                8 -> "Eylül"
                                9 -> "Ekim"
                                10 -> "Kasım"
                                11 -> "Aralık"
                                else -> ""
                            }
                        )
                        IconButton(onClick = { 
                            if (selectedMonth < 11) selectedMonth++ else selectedMonth = 0
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Ay")
                        }
                    }
                }
                
                // Gün seçimi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gün:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val maxDay = when (selectedMonth) {
                            1 -> if (selectedYear % 4 == 0 && (selectedYear % 100 != 0 || selectedYear % 400 == 0)) 29 else 28
                            3, 5, 8, 10 -> 30
                            else -> 31
                        }
                        IconButton(onClick = { 
                            if (selectedDay > 1) selectedDay-- else selectedDay = maxDay
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Gün")
                        }
                        Text(selectedDay.toString())
                        IconButton(onClick = { 
                            if (selectedDay < maxDay) selectedDay++ else selectedDay = 1
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Gün")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(selectedYear, selectedMonth, selectedDay)
                    onDateSelected(newCalendar.timeInMillis)
                    onDismissRequest()
                }
            ) {
                Text("Tamam")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun PetTypeDialog(
    onDismissRequest: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    val petTypes = listOf("Kedi", "Köpek", "Kuş", "Tavşan", "Hamster", "Balık", "Diğer")
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Evcil Hayvan Türü") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                petTypes.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeSelected(type) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = Color(0xFF673AB7)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = type,
                            fontSize = 16.sp
                        )
                    }
                    if (type != petTypes.last()) {
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun PetGenderDialog(
    onDismissRequest: () -> Unit,
    onGenderSelected: (String) -> Unit
) {
    val genders = listOf("Erkek", "Dişi", "Bilinmiyor")
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Cinsiyet") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                genders.forEach { gender ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGenderSelected(gender) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (gender) {
                                "Erkek" -> Icons.Default.Male
                                "Dişi" -> Icons.Default.Female
                                else -> Icons.Default.QuestionMark
                            },
                            contentDescription = null,
                            tint = Color(0xFF673AB7)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = gender,
                            fontSize = 16.sp
                        )
                    }
                    if (gender != genders.last()) {
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun PhotoOptionsDialog(
    onDismissRequest: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Fotoğraf Seç") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTakePhoto() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF673AB7)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Kamera ile Çek",
                        fontSize = 16.sp
                    )
                }
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFromGallery() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color(0xFF673AB7)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Galeriden Seç",
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("İptal")
            }
        }
    )
}

// Validate input
private fun validateInput(context: Context, name: String, type: String): Boolean {
    var isValid = true
    
    if (name.isEmpty()) {
        isValid = false
    }
    
    if (type.isEmpty()) {
        isValid = false
    }
    
    return isValid
}

// Tarih seçici dialog
private fun showDatePickerDialog(context: Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    if (initialDate > 0) {
        calendar.timeInMillis = initialDate
    }
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(selectedCalendar.timeInMillis)
        },
        year, month, day
    )
    
    datePickerDialog.show()
}

// Uri'yi Base64'e çevirme
private fun convertUriToBase64(context: Context, uri: Uri, onComplete: (String) -> Unit) {
    val bitmap = try {
        context.contentResolver.openInputStream(uri).use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    
    bitmap?.let {
        val resizedBitmap = resizeBitmap(it, 800)
        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        onComplete(base64String)
    }
}

// Bitmap yeniden boyutlandırma
private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    var width = bitmap.width
    var height = bitmap.height
    
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 1) {
        // Yatay resim
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        // Dikey resim
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }
    
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

// Evcil hayvanı kaydet/güncelle
private fun savePet(context: Context, pet: Pet, isEditMode: Boolean, onComplete: (Boolean) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val petsCollection = db.collection("pets")
    
    try {
        if (isEditMode && pet.id.isNotEmpty()) {
            // Güncelleme
            petsCollection.document(pet.id)
                .set(pet)
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    onComplete(false)
                }
        } else {
            // Yeni kayıt
            petsCollection.add(pet)
                .addOnSuccessListener { documentReference ->
                    // Yeni ID ile güncelle
                    val updatedPet = pet.copy(id = documentReference.id)
                    petsCollection.document(documentReference.id)
                        .set(updatedPet)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        }
    } catch (e: Exception) {
        onComplete(false)
    }
}

// DefaultImagePlaceholder composable
@Composable
private fun DefaultImagePlaceholder(primaryColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .border(2.dp, primaryColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Resim ekle",
            modifier = Modifier.size(40.dp),
            tint = primaryColor
        )
    }
}

// PetAvatar composable
@Composable
private fun PetAvatar(
    letter: String,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(primaryColor.copy(alpha = 0.2f))
            .border(2.dp, primaryColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
    }
} 