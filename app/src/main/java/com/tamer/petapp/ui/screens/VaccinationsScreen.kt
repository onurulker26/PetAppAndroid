package com.tamer.petapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.*

data class Vaccination(
    val id: String,
    val petId: String,
    val name: String,
    val date: Long,
    val nextDate: Long,
    val veterinarian: String,
    val notes: String,
    val reminderSet: Boolean
)

data class Pet(
    val id: String,
    val name: String,
    val imageUrl: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(
    petsList: List<Pet>,
    vaccinations: List<Vaccination>,
    selectedPetId: String?,
    isLoading: Boolean,
    onAddVaccination: (petId: String) -> Unit,
    onEditVaccination: (Vaccination) -> Unit,
    onDeleteVaccination: (Vaccination) -> Unit,
    onToggleReminder: (Vaccination, Boolean) -> Unit,
    onPetSelected: (String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Renk tanımları
    val primaryColor = Color(0xFF673AB7)
    val secondaryColor = Color(0xFFFF9800)
    val backgroundColor = Color(0xFFF5F5F5)
    val errorColor = Color(0xFFF44336)
    
    // Durum değişkenleri
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Vaccination?>(null) }
    var expandedVaccinationId by remember { mutableStateOf<String?>(null) }
    
    // Seçili hayvanın adını bul
    val selectedPetName = petsList.find { it.id == selectedPetId }?.name ?: "Tüm Hayvanlar"
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aşı Takibi", color = Color.White) },
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
                    // Filtreleme butonu
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrele",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (selectedPetId != null) {
                        onAddVaccination(selectedPetId)
                    } else if (petsList.isNotEmpty()) {
                        showFilterDialog = true // Önce pet seçimi yapılmalı
                    }
                },
                containerColor = secondaryColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aşı Ekle"
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Filtre bilgisi
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFilterDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Evcil Hayvan",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = selectedPetName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Seç",
                            tint = primaryColor
                        )
                    }
                }
                
                // Yaklaşan Aşılar bölümü
                val upcomingVaccinations = vaccinations
                    .filter { it.nextDate > System.currentTimeMillis() }
                    .sortedBy { it.nextDate }
                    .take(3)
                
                if (upcomingVaccinations.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Yaklaşan Aşılar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            upcomingVaccinations.forEach { vaccination ->
                                val pet = petsList.find { it.id == vaccination.petId }
                                val daysLeft = ((vaccination.nextDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Vaccines,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = vaccination.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pets,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = pet?.name ?: "",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    Surface(
                                        modifier = Modifier.padding(start = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (daysLeft <= 7) errorColor.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "$daysLeft gün kaldı",
                                            fontSize = 12.sp,
                                            color = if (daysLeft <= 7) errorColor else primaryColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                if (vaccination != upcomingVaccinations.last()) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color.Gray.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Aşı Listesi
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (vaccinations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vaccines,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedPetId == null) 
                                        "Henüz aşı kaydı bulunmuyor" 
                                    else 
                                        "Bu evcil hayvan için henüz aşı kaydı bulunmuyor",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    if (selectedPetId != null) {
                                        onAddVaccination(selectedPetId)
                                    } else if (petsList.isNotEmpty()) {
                                        showFilterDialog = true
                                    }
                                },
                                enabled = selectedPetId != null || petsList.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Aşı Ekle")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // FAB için alt boşluk
                    ) {
                        items(
                            items = vaccinations.sortedByDescending { it.date },
                            key = { it.id }
                        ) { vaccination ->
                            VaccinationItem(
                                vaccination = vaccination,
                                pet = petsList.find { it.id == vaccination.petId },
                                isExpanded = vaccination.id == expandedVaccinationId,
                                onItemClick = { 
                                    expandedVaccinationId = if (expandedVaccinationId == vaccination.id) null else vaccination.id 
                                },
                                onEdit = { onEditVaccination(vaccination) },
                                onDelete = { showDeleteConfirmDialog = vaccination },
                                onToggleReminder = { isEnabled -> onToggleReminder(vaccination, isEnabled) },
                                primaryColor = primaryColor,
                                errorColor = errorColor
                            )
                        }
                    }
                }
            }
        }
        
        // Dialog: Evcil hayvan filtresi
        if (showFilterDialog) {
            VaccinationsPetFilterDialog(
                pets = petsList,
                selectedPetId = selectedPetId,
                onDismiss = { showFilterDialog = false },
                onPetSelected = { 
                    onPetSelected(it)
                    showFilterDialog = false
                }
            )
        }
        
        // Dialog: Aşı silme onayı
        if (showDeleteConfirmDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Aşı Kaydını Sil") },
                text = { 
                    Text("Bu aşı kaydını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog?.let { onDeleteVaccination(it) }
                            showDeleteConfirmDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = errorColor
                        )
                    ) {
                        Text("Sil")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = null }
                    ) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

@Composable
fun VaccinationItem(
    vaccination: Vaccination,
    pet: Pet?,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    primaryColor: Color,
    errorColor: Color
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Başlık ve özet bilgiler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hayvanın resmi veya ikon
                if (pet != null && pet.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pet.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Evcil Hayvan",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vaccination.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pet?.name ?: "",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormat.format(Date(vaccination.date)),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                    modifier = Modifier.rotate(rotationState)
                )
            }
            
            // Detaylı bilgiler (genişletildiğinde)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Divider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bir sonraki aşı tarihi
                    if (vaccination.nextDate > 0) {
                        VaccinationDetailItem(
                            icon = Icons.Default.Event,
                            label = "Bir Sonraki Aşı Tarihi",
                            value = dateFormat.format(Date(vaccination.nextDate))
                        )
                    }
                    
                    // Veteriner
                    if (vaccination.veterinarian.isNotEmpty()) {
                        VaccinationDetailItem(
                            icon = Icons.Default.Person,
                            label = "Veteriner",
                            value = vaccination.veterinarian
                        )
                    }
                    
                    // Notlar
                    if (vaccination.notes.isNotEmpty()) {
                        VaccinationDetailItem(
                            icon = Icons.Default.Notes,
                            label = "Notlar",
                            value = vaccination.notes
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Hatırlatma ayarları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hatırlatma",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = vaccination.reminderSet,
                            onCheckedChange = { onToggleReminder(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Aksiyonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.padding(end = 8.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Düzenle",
                                color = primaryColor
                            )
                        }
                        
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = errorColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sil")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaccinationDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun VaccinationsPetFilterDialog(
    pets: List<Pet>,
    selectedPetId: String?,
    onDismiss: () -> Unit,
    onPetSelected: (String?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Evcil Hayvan Seçin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Tüm hayvanlar seçeneği
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPetSelected(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPetId == null,
                        onClick = { onPetSelected(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tüm Hayvanlar",
                        fontSize = 16.sp
                    )
                }
                
                Divider()
                
                if (pets.isEmpty()) {
                    Text(
                        text = "Henüz evcil hayvan kaydı bulunmuyor",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    pets.forEach { pet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPetSelected(pet.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPetId == pet.id,
                                onClick = { onPetSelected(pet.id) }
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Hayvanın resmi
                            if (pet.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(pet.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = pet.name,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = pet.name,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = pet.type,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tamam")
                }
            }
        }
    }
} 