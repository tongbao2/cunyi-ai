package com.cunyi.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * 拍照识药页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineRecognitionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var selectedMedicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var interactions by remember { mutableStateOf<List<DrugInteraction>>(emptyList()) }
    var identifiedMedicine by remember { mutableStateOf<Medicine?>(null) }

    // 相机权限Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            Toast.makeText(context, "需要相机权限才能拍照识别药品", Toast.LENGTH_LONG).show()
        }
    }

    // 拍照Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            // 在实际应用中，这里应该调用OCR或AI模型识别药盒上的文字
            // 目前简化处理，提示用户手动搜索
            Toast.makeText(context, "请在下方搜索药品名称", Toast.LENGTH_LONG).show()
        }
    }

    // 选择图片Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            capturedImageUri = it
            Toast.makeText(context, "已选择图片，请在下方搜索药品名称", Toast.LENGTH_LONG).show()
        }
    }

    fun takePicture() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED -> {
                // 创建临时文件
                val photoFile = File.createTempFile(
                    "medicine_",
                    ".jpg",
                    context.cacheDir
                )
                capturedImageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                takePictureLauncher.launch(capturedImageUri)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
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
            // 拍照按钮
            item {
                LargeButton(
                    text = "📷 拍照药盒",
                    onClick = { takePicture() },
                    icon = Icons.Default.CameraAlt
                )
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
