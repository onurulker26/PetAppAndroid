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

data class Treatment(
    val id: String,
    val petId: String,
    val name: String,
    val condition: String,
    val medicationName: String,
    val dosage: String,
    val startDate: Long,
    val endDate: Long,
    val frequency: String,
    val veterinarian: String,
    val notes: String,
    val isCompleted: Boolean,
    val reminderEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentsScreen(
    petsList: List<Pet>,
    treatments: List<Treatment>,
    selectedPetId: String?,
    isLoading: Boolean,
    onAddTreatment: (petId: String) -> Unit,
    onEditTreatment: (Treatment) -> Unit,
    onDeleteTreatment: (Treatment) -> Unit,
    onCompleteTreatment: (Treatment, Boolean) -> Unit,
    onToggleReminder: (Treatment, Boolean) -> Unit,
    onPetSelected: (String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Renk tanımları
    val primaryColor = Color(0xFF673AB7)
    val secondaryColor = Color(0xFFFF9800)
    val backgroundColor = Color(0xFFF5F5F5)
    val successColor = Color(0xFF4CAF50)
    val inProgressColor = Color(0xFF2196F3)
    val errorColor = Color(0xFFF44336)
    
    // Durum değişkenleri
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Treatment?>(null) }
    var expandedTreatmentId by remember { mutableStateOf<String?>(null) }
    
    // Seçili hayvanın adını bul
    val selectedPetName = petsList.find { it.id == selectedPetId }?.name ?: "Tüm Hayvanlar"
    
    // Tedavileri aktif ve tamamlanmış olarak ayır
    val activeTreatments = treatments.filter { !it.isCompleted }
    val completedTreatments = treatments.filter { it.isCompleted }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tedavi Takibi", color = Color.White) },
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
                        onAddTreatment(selectedPetId)
                    } else if (petsList.isNotEmpty()) {
                        showFilterDialog = true // Önce pet seçimi yapılmalı
                    }
                },
                containerColor = secondaryColor,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tedavi Ekle"
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
                
                // Özet bilgiler
                if (treatments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TreatmentSummaryCard(
                            count = activeTreatments.size,
                            label = "Devam Eden",
                            icon = Icons.Default.Medication,
                            color = inProgressColor,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        TreatmentSummaryCard(
                            count = completedTreatments.size,
                            label = "Tamamlanan",
                            icon = Icons.Default.CheckCircle,
                            color = successColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tedavi listesi
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (treatments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedPetId == null) 
                                        "Henüz tedavi kaydı bulunmuyor" 
                                      else 
                                        "Bu evcil hayvan için henüz tedavi kaydı bulunmuyor",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    if (selectedPetId != null) {
                                        onAddTreatment(selectedPetId)
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
                                Text("Tedavi Ekle")
                            }
                        }
                    }
                } else {
                    // Tedavi listesini göster
                    var showCompletedTreatments by remember { mutableStateOf(false) }
                    
                    // Aktif/Tamamlanan seçici
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TabRow(
                            selectedTabIndex = if (showCompletedTreatments) 1 else 0,
                            containerColor = Color.Transparent,
                            contentColor = primaryColor,
                            divider = { }
                        ) {
                            Tab(
                                selected = !showCompletedTreatments,
                                onClick = { showCompletedTreatments = false },
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Devam Eden (${activeTreatments.size})",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Tab(
                                selected = showCompletedTreatments,
                                onClick = { showCompletedTreatments = true },
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tamamlanan (${completedTreatments.size})",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    // İlgili tedavi listesini göster
                    val displayedTreatments = if (showCompletedTreatments) completedTreatments else activeTreatments
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // FAB için alt boşluk
                    ) {
                        items(
                            items = displayedTreatments.sortedByDescending { it.startDate },
                            key = { it.id }
                        ) { treatment ->
                            TreatmentItem(
                                treatment = treatment,
                                pet = petsList.find { it.id == treatment.petId },
                                isExpanded = treatment.id == expandedTreatmentId,
                                onItemClick = { 
                                    expandedTreatmentId = if (expandedTreatmentId == treatment.id) null else treatment.id 
                                },
                                onEdit = { onEditTreatment(treatment) },
                                onDelete = { showDeleteConfirmDialog = treatment },
                                onComplete = { isCompleted -> onCompleteTreatment(treatment, isCompleted) },
                                onToggleReminder = { isEnabled -> onToggleReminder(treatment, isEnabled) },
                                primaryColor = primaryColor,
                                successColor = successColor,
                                errorColor = errorColor,
                                inProgressColor = inProgressColor
                            )
                        }
                    }
                }
            }
        }
        
        // Dialog: Evcil hayvan filtresi
        if (showFilterDialog) {
            TreatmentsPetFilterDialog(
                pets = petsList,
                selectedPetId = selectedPetId,
                onDismiss = { showFilterDialog = false },
                onPetSelected = { 
                    onPetSelected(it)
                    showFilterDialog = false
                }
            )
        }
        
        // Dialog: Tedavi silme onayı
        if (showDeleteConfirmDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Tedavi Kaydını Sil") },
                text = { 
                    Text("Bu tedavi kaydını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog?.let { onDeleteTreatment(it) }
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
fun TreatmentSummaryCard(
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = label,
                fontSize = 14.sp,
                color = color
            )
        }
    }
}

@Composable
fun TreatmentItem(
    treatment: Treatment,
    pet: Pet?,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: (Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    primaryColor: Color,
    successColor: Color,
    errorColor: Color,
    inProgressColor: Color
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (treatment.isCompleted) 
                successColor.copy(alpha = 0.05f) else Color.White
        )
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
                // Tedavi durumu
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (treatment.isCompleted) successColor.copy(alpha = 0.1f)
                            else inProgressColor.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (treatment.isCompleted) 
                            Icons.Default.CheckCircle else Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (treatment.isCompleted) successColor else inProgressColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = treatment.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Evcil hayvan adı
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
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Başlangıç tarihi
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormat.format(Date(treatment.startDate)),
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
                    
                    // İlaç bilgileri
                    if (treatment.medicationName.isNotEmpty()) {
                        TreatmentDetailItem(
                            icon = Icons.Default.Medication,
                            label = "İlaç Adı",
                            value = treatment.medicationName
                        )
                    }
                    
                    // Doz bilgisi
                    if (treatment.dosage.isNotEmpty()) {
                        TreatmentDetailItem(
                            icon = Icons.Default.LocalPharmacy,
                            label = "Doz",
                            value = treatment.dosage
                        )
                    }
                    
                    // Durum
                    TreatmentDetailItem(
                        icon = Icons.Default.HealthAndSafety,
                        label = "Durum",
                        value = treatment.condition
                    )
                    
                    // Sıklık
                    if (treatment.frequency.isNotEmpty()) {
                        TreatmentDetailItem(
                            icon = Icons.Default.Schedule,
                            label = "Sıklık",
                            value = treatment.frequency
                        )
                    }
                    
                    // Bitiş tarihi
                    if (treatment.endDate > 0) {
                        TreatmentDetailItem(
                            icon = Icons.Default.Event,
                            label = "Bitiş Tarihi",
                            value = dateFormat.format(Date(treatment.endDate))
                        )
                    }
                    
                    // Veteriner
                    if (treatment.veterinarian.isNotEmpty()) {
                        TreatmentDetailItem(
                            icon = Icons.Default.Person,
                            label = "Veteriner",
                            value = treatment.veterinarian
                        )
                    }
                    
                    // Notlar
                    if (treatment.notes.isNotEmpty()) {
                        TreatmentDetailItem(
                            icon = Icons.Default.Notes,
                            label = "Notlar",
                            value = treatment.notes
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Hatırlatma ayarları
                    if (!treatment.isCompleted) {
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
                                checked = treatment.reminderEnabled,
                                onCheckedChange = { onToggleReminder(it) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Tamamlama durumu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (treatment.isCompleted) successColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tedavi Durumu",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (treatment.isCompleted) {
                            Text(
                                text = "Tamamlandı",
                                color = successColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        } else {
                            Button(
                                onClick = { onComplete(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = successColor
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Tamamlandı Olarak İşaretle")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Aksiyonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (treatment.isCompleted) {
                            OutlinedButton(
                                onClick = { onComplete(false) },
                                modifier = Modifier.padding(end = 8.dp),
                                border = BorderStroke(1.dp, inProgressColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Devam Ediyor",
                                    tint = inProgressColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Devam Ediyor",
                                    color = inProgressColor
                                )
                            }
                        } else {
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
fun TreatmentDetailItem(
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
fun TreatmentsPetFilterDialog(
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