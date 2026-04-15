package com.cunyi.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cunyi.ai.data.Medicine
import com.cunyi.ai.data.MedicineDatabase
import com.cunyi.ai.data.DrugInteraction
import com.cunyi.ai.data.RiskLevel
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.theme.*
import java.io.File
import android.graphics.Bitmap

private const val TAG = "MedicineRecognition"

// 从 URI 加载图片 Bitmap（非 Composable）
private fun loadBitmapFromUri(context: android.content.Context, uri: Uri?): Bitmap? {
    if (uri == null) return null
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bitmap from uri: $uri", e)
        null
    }
}

/**
 * 拍照识药页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineRecognitionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) } // 用于保存最近拍照的Uri
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var selectedMedicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var interactions by remember { mutableStateOf<List<DrugInteraction>>(emptyList()) }
    var identifiedMedicine by remember { mutableStateOf<Medicine?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }

    // 拍照Launcher - 使用局部Uri避免状态问题
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d(TAG, "TakePicture result: success=$success")
        if (success && capturedImageUri != null) {
            showImagePreview = true
            Toast.makeText(context, "拍照成功！请搜索药品名称", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "拍照失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    // 相机权限Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Camera permission result: $isGranted")
        if (isGranted) {
            // 权限授予后直接拍照
            try {
                val photoFile = File.createTempFile(
                    "medicine_${System.currentTimeMillis()}",
                    ".jpg",
                    context.cacheDir
                )
                Log.d(TAG, "Created temp file: ${photoFile.absolutePath}")
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                Log.d(TAG, "FileProvider Uri: $uri")
                
                capturedImageUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file", e)
                Toast.makeText(context, "创建临时文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "需要相机权限才能拍照识别药品", Toast.LENGTH_LONG).show()
        }
    }

    // 选择图片Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        Log.d(TAG, "PickImage result: uri=$uri")
        uri?.let {
            capturedImageUri = it
            showImagePreview = true
            Toast.makeText(context, "已选择图片，请搜索药品名称", Toast.LENGTH_SHORT).show()
        }
    }

    // 拍照函数
    fun takePicture() {
        Log.d(TAG, "takePicture called")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val photoFile = File.createTempFile(
                    "medicine_${System.currentTimeMillis()}",
                    ".jpg",
                    context.cacheDir
                )
                Log.d(TAG, "Created temp file: ${photoFile.absolutePath}")
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                Log.d(TAG, "FileProvider Uri: $uri")
                
                capturedImageUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file", e)
                Toast.makeText(context, "创建临时文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "Requesting camera permission")
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 从相册选择
    fun pickImage() {
        Log.d(TAG, "pickImage called")
        pickImageLauncher.launch("image/*")
    }

    // 搜索药品
    fun searchMedicine(query: String) {
        if (query.length >= 2) {
            searchResults = MedicineDatabase.searchMedicine(query)
        } else {
            searchResults = emptyList()
        }
    }

    // 添加药品到已识别列表
    fun addMedicine(medicine: Medicine) {
        if (!selectedMedicines.any { it.id == medicine.id }) {
            selectedMedicines = selectedMedicines + medicine
            identifiedMedicine = medicine
            // 检查相互作用
            interactions = MedicineDatabase.checkInteractions(
                selectedMedicines.map { it.name }
            )
        }
    }

    // 移除药品
    fun removeMedicine(medicine: Medicine) {
        selectedMedicines = selectedMedicines - medicine
        interactions = MedicineDatabase.checkInteractions(
            selectedMedicines.map { it.name }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍照识药", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen,
                    titleContentColor = TextOnPrimary,
                    navigationIconContentColor = TextOnPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimensions.SpacingL.dp),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingL.dp)
        ) {
            // 拍照/选图按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingM.dp)
                ) {
                    // 拍照按钮
                    LargeButton(
                        text = "📷 拍照药盒",
                        onClick = { takePicture() },
                        icon = Icons.Default.CameraAlt,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 从相册选择
                    LargeButton(
                        text = "🖼️ 从相册",
                        onClick = { pickImage() },
                        icon = Icons.Default.PhotoLibrary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 图片预览
            if (capturedImageUri != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.SpacingM.dp)
                        ) {
                            Text(
                                text = "📷 已拍摄/选择的图片",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                            
                            // 加载并显示图片
                            val bitmap = loadBitmapFromUri(context, capturedImageUri)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "药盒图片",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // 图片加载失败时显示占位符
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "无法加载图片",
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                            
                            Text(
                                text = "请在下方搜索药品名称进行识别",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            // 清除图片按钮
                            IconButton(
                                onClick = { 
                                    capturedImageUri = null
                                    showImagePreview = false
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Close, "清除图片", tint = AlertRed)
                            }
                        }
                    }
                }
            }

            // 搜索框
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        searchMedicine(it)
                    },
                    label = { Text("搜索药品名称", style = MaterialTheme.typography.bodyLarge) },
                    placeholder = { Text("输入药品名称，如：硝苯地平") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, "搜索")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, "清除")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // 搜索结果
            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "搜索结果",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                items(searchResults) { medicine ->
                    FunctionCard(
                        title = medicine.name,
                        description = "${medicine.genericName}\n${medicine.usage}",
                        icon = when (medicine.category) {
                            com.cunyi.ai.data.MedicineCategory.CARDIOVASCULAR -> "❤️"
                            com.cunyi.ai.data.MedicineCategory.DIABETES -> "🩸"
                            com.cunyi.ai.data.MedicineCategory.ANTIBIOTIC -> "💊"
                            com.cunyi.ai.data.MedicineCategory.PAIN_RELIEF -> "🧪"
                            com.cunyi.ai.data.MedicineCategory.ANTIHYPERTENSION -> "💉"
                            com.cunyi.ai.data.MedicineCategory.DIGESTIVE -> "🍽️"
                            com.cunyi.ai.data.MedicineCategory.RESPIRATORY -> "🌬️"
                            com.cunyi.ai.data.MedicineCategory.ANTIPLATELET -> "🫀"
                        },
                        onClick = { addMedicine(medicine) }
                    )
                }
            }

            // 已添加的药品
            if (selectedMedicines.isNotEmpty()) {
                item {
                    Text(
                        text = "已添加的药品",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                items(selectedMedicines) { medicine ->
                    MedicineDetailCard(
                        medicine = medicine,
                        onRemove = { removeMedicine(medicine) }
                    )
                }
            }

            // 药物相互作用警告
            if (interactions.isNotEmpty()) {
                item {
                    Text(
                        text = "⚠️ 药物相互作用警告",
                        style = MaterialTheme.typography.titleLarge,
                        color = AlertRed
                    )
                }
                
                items(interactions) { interaction ->
                    InteractionCard(interaction = interaction)
                }
            }

            // 当前服用药品列表
            item {
                Text(
                    text = "💡 农村常用药品参考",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            items(MedicineDatabase.getAllMedicines().take(10)) { medicine ->
                FunctionCard(
                    title = medicine.name,
                    description = medicine.timing,
                    icon = "💊",
                    onClick = { addMedicine(medicine) },
                    containerColor = if (selectedMedicines.any { it.id == medicine.id }) 
                        PrimaryGreen.copy(alpha = 0.1f) else BackgroundWhite
                )
            }
        }
    }
}

@Composable
private fun MedicineDetailCard(
    medicine: Medicine,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingL.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💊 ${medicine.name}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        "移除",
                        tint = AlertRed
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
            
            Text(
                text = "通用名: ${medicine.genericName}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "用法: ${medicine.usage}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "服用时间: ${medicine.timing}",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryGreen
            )
            
            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
            
            Text(
                text = "⚠️ ${medicine.precautions}",
                style = MaterialTheme.typography.bodyMedium,
                color = AlertOrange
            )
            
            if (medicine.contraindications.isNotEmpty()) {
                Text(
                    text = "🚫 禁忌: ${medicine.contraindications}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlertRed
                )
            }
        }
    }
}

@Composable
private fun InteractionCard(interaction: DrugInteraction) {
    val backgroundColor = when (interaction.riskLevel) {
        RiskLevel.HIGH -> AlertRed.copy(alpha = 0.15f)
        RiskLevel.MEDIUM -> AlertOrange.copy(alpha = 0.15f)
        RiskLevel.LOW -> AlertYellow.copy(alpha = 0.15f)
    }
    
    val borderColor = when (interaction.riskLevel) {
        RiskLevel.HIGH -> AlertRed
        RiskLevel.MEDIUM -> AlertOrange
        RiskLevel.LOW -> AlertYellow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingL.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (interaction.riskLevel) {
                        RiskLevel.HIGH -> "🔴 高风险"
                        RiskLevel.MEDIUM -> "🟠 中风险"
                        RiskLevel.LOW -> "🟡 低风险"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = borderColor
                )
            }
            
            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
            
            Text(
                text = "${interaction.drug1} + ${interaction.drug2}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
            
            Text(
                text = interaction.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
