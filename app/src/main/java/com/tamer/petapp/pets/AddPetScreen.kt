package com.tamer.petapp.pets

import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto

import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tamer.petapp.R
import com.tamer.petapp.model.Pet
import com.tamer.petapp.ui.theme.PetAppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    petToEdit: Pet? = null,
    isLoading: Boolean = false,
    onSaveClick: (petName: String, petType: String, petBreed: String, 
                 birthDate: Long, petGender: String, petWeight: Double,
                 notes: String, imageUri: Uri?) -> Unit,
    onPhotoSelect: () -> Unit,
    selectedImageUri: Uri? = null,
    onBackClick: () -> Unit,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current
    
    // Pet bilgileri için state değişkenleri
    var petName by remember { mutableStateOf(petToEdit?.name ?: "") }
    var petType by remember { mutableStateOf(petToEdit?.type ?: "") }
    var petBreed by remember { mutableStateOf(petToEdit?.breed ?: "") }
    var petGender by remember { mutableStateOf(petToEdit?.gender ?: "") }
    var petWeightText by remember { mutableStateOf(if (petToEdit?.weight != null && petToEdit.weight > 0) petToEdit.weight.toString() else "") }
    var petNotes by remember { mutableStateOf(petToEdit?.notes ?: "") }
    
    // Tarih için state değişkenleri
    val defaultDate = if (petToEdit != null && petToEdit.birthDate > 0) {
        Date(petToEdit.birthDate)
    } else {
        Date()
    }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(defaultDate) }
    var dateText by remember { mutableStateOf(if (petToEdit != null && petToEdit.birthDate > 0) dateFormat.format(defaultDate) else "") }
    
    // Dropdown açılma durumları
    var petTypeExpanded by remember { mutableStateOf(false) }
    var petGenderExpanded by remember { mutableStateOf(false) }
    
    // Dropdown seçenekleri
    val petTypes = listOf("Kedi", "Köpek", "Kuş", "Balık", "Kemirgen", "Sürüngen", "Diğer")
    val genderOptions = listOf("Erkek", "Dişi")
    
    // Validasyon hataları
    var nameError by remember { mutableStateOf<String?>(null) }
    var typeError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    
    val primaryColor = Color(0xFF5B37B7)
    val errorColor = Color(0xFFB00020)
    
    PetAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditMode) "Evcil Hayvanı Düzenle" else "Evcil Hayvan Ekle") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Geri"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Ana içerik
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fotoğraf seçme alanı
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, primaryColor, CircleShape)
                            .clickable { onPhotoSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            // AsyncImage ile resmi göster
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedImageUri)
                                    .crossfade(true)
                                    .memoryCacheKey(selectedImageUri.toString())
                                    .build(),
                                contentDescription = "Evcil Hayvan Resmi",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_pet_placeholder),
                                fallback = painterResource(id = R.drawable.ic_pet_placeholder)
                            )
                        } else if (!petToEdit?.base64Image.isNullOrEmpty() || !petToEdit?.imageUrl.isNullOrEmpty()) {
                            // Düzenleme modunda mevcut resmi göster
                            val imageSource = if (!petToEdit?.base64Image.isNullOrEmpty()) {
                                petToEdit?.base64Image
                            } else {
                                petToEdit?.imageUrl
                            }
                            
                            if (imageSource != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageSource)
                                        .crossfade(true)
                                        .memoryCacheKey(imageSource)
                                        .build(),
                                    contentDescription = "Evcil Hayvan Resmi",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.ic_pet_placeholder),
                                    fallback = painterResource(id = R.drawable.ic_pet_placeholder)
                                )
                            }
                        } else {
                            // Foto seçme ikonu
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Fotoğraf Ekle",
                                modifier = Modifier.size(64.dp),
                                tint = primaryColor.copy(alpha = 0.5f)
                            )
                            
                            Text(
                                text = "Fotoğraf Ekle",
                                fontSize = 14.sp,
                                color = primaryColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form alanları
                    OutlinedTextField(
                        value = petName,
                        onValueChange = { 
                            petName = it
                            nameError = null
                        },
                        label = { Text("İsim") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tür dropdown
                    ExposedDropdownMenuBox(
                        expanded = petTypeExpanded,
                        onExpandedChange = { petTypeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = petType,
                            onValueChange = { 
                                petType = it
                                typeError = null
                            },
                            readOnly = true,
                            label = { Text("Tür") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "Tür Seç"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = typeError != null,
                            supportingText = { typeError?.let { Text(it) } }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = petTypeExpanded,
                            onDismissRequest = { petTypeExpanded = false }
                        ) {
                            petTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        petType = type
                                        petTypeExpanded = false
                                        typeError = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = petBreed,
                        onValueChange = { petBreed = it },
                        label = { Text("Irk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Doğum tarihi seçici
                    val dateDialogState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDate.time
                    )
                    
                    var showDateDialog by remember { mutableStateOf(false) }
                    
                    if (showDateDialog) {
                        DatePickerDialog(
                            onDismissRequest = { showDateDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    dateDialogState.selectedDateMillis?.let { millis ->
                                        selectedDate = Date(millis)
                                        dateText = dateFormat.format(selectedDate)
                                    }
                                    showDateDialog = false
                                }) {
                                    Text("Tamam")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDateDialog = false }) {
                                    Text("İptal")
                                }
                            }
                        ) {
                            DatePicker(state = dateDialogState)
                        }
                    }
                    
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { },
                        label = { Text("Doğum Tarihi") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Tarih Seç",
                                modifier = Modifier.clickable { showDateDialog = true }
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cinsiyet dropdown
                    ExposedDropdownMenuBox(
                        expanded = petGenderExpanded,
                        onExpandedChange = { petGenderExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = petGender,
                            onValueChange = { 
                                petGender = it
                                genderError = null
                            },
                            readOnly = true,
                            label = { Text("Cinsiyet") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "Cinsiyet Seç"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = genderError != null,
                            supportingText = { genderError?.let { Text(it) } }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = petGenderExpanded,
                            onDismissRequest = { petGenderExpanded = false }
                        ) {
                            genderOptions.forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender) },
                                    onClick = {
                                        petGender = gender
                                        petGenderExpanded = false
                                        genderError = null
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = petWeightText,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                petWeightText = it
                                weightError = null
                            }
                        },
                        label = { Text("Ağırlık (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        isError = weightError != null,
                        supportingText = { weightError?.let { Text(it) } }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = petNotes,
                        onValueChange = { petNotes = it },
                        label = { Text("Notlar") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Kaydet Butonu
                    Button(
                        onClick = {
                            // Alanları doğrula
                            var hasError = false
                            
                            if (petName.trim().isEmpty()) {
                                nameError = "İsim gerekli"
                                hasError = true
                            }
                            
                            if (petType.trim().isEmpty()) {
                                typeError = "Tür seçimi gerekli"
                                hasError = true
                            }
                            
                            if (petGender.trim().isEmpty()) {
                                genderError = "Cinsiyet seçimi gerekli"
                                hasError = true
                            }
                            
                            val weight = petWeightText.toDoubleOrNull() ?: 0.0
                            
                            if (!hasError) {
                                onSaveClick(
                                    petName.trim(),
                                    petType.trim(),
                                    petBreed.trim(),
                                    selectedDate.time,
                                    petGender.trim(),
                                    weight,
                                    petNotes.trim(),
                                    selectedImageUri
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text(
                            text = if (isEditMode) "Güncelle" else "Kaydet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Yükleniyor göstergesi
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
            }
        }
    }
} 