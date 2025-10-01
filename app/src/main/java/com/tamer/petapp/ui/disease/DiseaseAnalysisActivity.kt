package com.tamer.petapp.ui.disease

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tamer.petapp.MainActivity
import com.tamer.petapp.ProfileActivity
import com.tamer.petapp.R
import com.tamer.petapp.VetClinicsActivity
import com.tamer.petapp.guide.PetCareGuideActivity
import com.tamer.petapp.ui.theme.PetAppTheme
import kotlinx.coroutines.launch
import kotlin.math.min
import androidx.compose.runtime.livedata.observeAsState

class DiseaseAnalysisActivity : ComponentActivity() {
    private val viewModel: DiseaseAnalysisViewModel by viewModels()
    private var selectedImageBitmap by mutableStateOf<Bitmap?>(null)

    // Galeri için sonuç alıcı
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val maxDimension = 224
                val scale = min(
                    maxDimension.toFloat() / options.outWidth,
                    maxDimension.toFloat() / options.outHeight
                )

                val finalOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                    inScaled = true
                    inDensity = 1000
                    inTargetDensity = (1000 * scale).toInt()
                }

                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream, null, finalOptions)
                    bitmap?.let {
                        selectedImageBitmap = it
                    } ?: run {
                        Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Fotoğraf yüklenirken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Kamera için sonuç alıcı
    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap == null) {
                    Toast.makeText(this, "Fotoğraf alınamadı", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                val convertedBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
                imageBitmap.recycle()
                selectedImageBitmap = convertedBitmap
            } catch (e: Exception) {
                Toast.makeText(this, "Kameradan fotoğraf alınırken bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // İzinler için sonuç alıcı
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showImagePickerDialog()
        } else {
            Toast.makeText(this, "Bu özelliği kullanmak için gerekli izinleri vermeniz gerekiyor", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetAppTheme {
                DiseaseAnalysisScreen(
                    viewModel = viewModel,
                    selectedImageBitmap = selectedImageBitmap,
                    onSelectImage = { checkAndRequestPermissions() },
                    onAnalyze = { bitmap, animalType ->
                        startAnalysis(bitmap, animalType)
                    },
                    onBackClick = { finish() },
                    onNavigationItemClick = { itemId ->
                        when (itemId) {
                            0 -> { // Ana Sayfa
                                startActivity(Intent(this@DiseaseAnalysisActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                finish()
                            }
                            1 -> { // Veterinerler
                                startActivity(Intent(this@DiseaseAnalysisActivity, VetClinicsActivity::class.java))
                                finish()
                            }
                            2 -> { // Bakım
                                startActivity(Intent(this@DiseaseAnalysisActivity, PetCareGuideActivity::class.java))
                                finish()
                            }
                            3 -> { // Profil
                                startActivity(Intent(this@DiseaseAnalysisActivity, ProfileActivity::class.java))
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                showImagePickerDialog()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Kamera", "Galeri")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Fotoğraf Seç")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    takePicture.launch(intent)
                }
                1 -> {
                    pickImage.launch("image/*")
                }
            }
        }
        builder.show()
    }

    private fun startAnalysis(bitmap: Bitmap, animalType: AnimalType) {
        // ViewModel'e hayvan türünü set et
        viewModel.setAnimalType(animalType)
        // Coroutine scope içinde suspend fonksiyonu çağır
        lifecycleScope.launch {
            viewModel.analyzeImage(bitmap)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseAnalysisScreen(
    viewModel: DiseaseAnalysisViewModel,
    selectedImageBitmap: Bitmap?,
    onSelectImage: () -> Unit,
    onAnalyze: (Bitmap, AnimalType) -> Unit,
    onBackClick: () -> Unit,
    onNavigationItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val primaryColor = Color(0xFF673AB7)
    
    var selectedAnimalType by remember { mutableStateOf<AnimalType?>(null) }
    
    // ViewModel state'lerini observe et
    val analysisResult by viewModel.analysisResult.observeAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hastalık Analizi", color = Color.White) },
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
                
                // Bakım (Pet Care Guide)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Bakım") },
                    label = { Text("Bakım") },
                    selected = false,
                    onClick = { onNavigationItemClick(2) }
                )
                
                // Profil
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = false,
                    onClick = { onNavigationItemClick(3) }
                )
                
                // Hastalık Analizi - Seçili
                NavigationBarItem(
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = "Hastalık Analizi") },
                    label = { Text("Hastalık Analizi") },
                    selected = true,
                    onClick = { /* Zaten buradayız */ }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
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
                        .height(120.dp)
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
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Hastalık Analizi",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                text = "Evcil hayvanınızın fotoğrafını yükleyip yapay zeka ile hastalık analizi yapın",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Hayvan türü seçimi
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Hayvan Türü Seçin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnimalTypeCard(
                            animalType = AnimalType.DOG,
                            title = "Köpek",
                            icon = Icons.Default.Pets,
                            isSelected = selectedAnimalType == AnimalType.DOG,
                            onClick = { selectedAnimalType = AnimalType.DOG },
                            primaryColor = primaryColor
                        )
                        
                        AnimalTypeCard(
                            animalType = AnimalType.CAT,
                            title = "Kedi",
                            icon = Icons.Default.Favorite,
                            isSelected = selectedAnimalType == AnimalType.CAT,
                            onClick = { selectedAnimalType = AnimalType.CAT },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
            
            // Fotoğraf seçimi
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Fotoğraf Seçin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (selectedImageBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                bitmap = selectedImageBitmap.asImageBitmap(),
                                contentDescription = "Seçilen Fotoğraf",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(bottom = 16.dp)
                                .clickable { onSelectImage() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            border = BorderStroke(2.dp, primaryColor.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = primaryColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Fotoğraf Seçmek İçin Tıklayın",
                                    color = primaryColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = onSelectImage,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Fotoğraf Seç")
                    }
                }
            }
            
            // Analiz butonu
            AnimatedVisibility(
                visible = selectedImageBitmap != null && selectedAnimalType != null
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedImageBitmap?.let { bitmap ->
                                    selectedAnimalType?.let { animalType ->
                                        onAnalyze(bitmap, animalType)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Analizi Başlat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Sonuçlar
            analysisResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Analiz Sonuçları",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        when (result) {
                            is AnalysisResult.Success -> {
                                for (prediction in result.predictions) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Text(
                                            text = prediction,
                                            modifier = Modifier.padding(12.dp),
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                            is AnalysisResult.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = result.message,
                                        modifier = Modifier.padding(12.dp),
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                            is AnalysisResult.Ready -> {
                                Text(
                                    text = result.message,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AnimalTypeCard(
    animalType: AnimalType,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .size(120.dp, 100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(3.dp, primaryColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) primaryColor else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) primaryColor else Color.Gray
            )
        }
    }
} 
