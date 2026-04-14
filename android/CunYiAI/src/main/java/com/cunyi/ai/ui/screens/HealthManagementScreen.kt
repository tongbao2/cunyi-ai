package com.cunyi.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cunyi.ai.data.HealthRecord
import com.cunyi.ai.data.HealthAlert
import com.cunyi.ai.data.RecordType
import com.cunyi.ai.data.AlertLevel
import com.cunyi.ai.manager.HealthRecordManager
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.theme.*

/**
 * 慢病管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthManagementScreen(
    healthRecordManager: HealthRecordManager,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("记录", "历史", "趋势")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("慢病管理", style = MaterialTheme.typography.headlineMedium) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab 选择器
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundWhite
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.titleMedium) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RecordTab(healthRecordManager)
                1 -> HistoryTab(healthRecordManager)
                2 -> TrendTab(healthRecordManager)
            }
        }
    }
}

@Composable
private fun RecordTab(healthRecordManager: HealthRecordManager) {
    var showBloodPressureDialog by remember { mutableStateOf(false) }
    var showBloodSugarDialog by remember { mutableStateOf(false) }
    var showHeartRateDialog by remember { mutableStateOf(false) }
    var lastAlert by remember { mutableStateOf<HealthAlert?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SpacingL.dp),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingL.dp)
    ) {
        // 记录选项
        Text(
            text = "选择记录类型",
            style = MaterialTheme.typography.titleLarge
        )

        FunctionCard(
            title = "血压",
            description = "记录收缩压和舒张压",
            icon = "❤️",
            onClick = { showBloodPressureDialog = true }
        )

        FunctionCard(
            title = "血糖",
            description = "记录空腹或餐后血糖",
            icon = "🩸",
            onClick = { showBloodSugarDialog = true }
        )

        FunctionCard(
            title = "心率",
            description = "记录脉搏次数",
            icon = "💓",
            onClick = { showHeartRateDialog = true }
        )

        // 显示警报
        lastAlert?.let { alert ->
            AlertCard(
                title = alert.title,
                message = alert.message,
                level = alert.level
            )
        }
    }

    // 血压输入对话框
    if (showBloodPressureDialog) {
        BloodPressureDialog(
            onDismiss = { showBloodPressureDialog = false },
            onConfirm = { systolic, diastolic ->
                val (level, title, message) = checkBloodPressureAlert(systolic, diastolic)
                val record = HealthRecord(
                    type = RecordType.BLOOD_PRESSURE,
                    value = systolic,
                    secondaryValue = diastolic
                )
                healthRecordManager.saveRecord(record)
                lastAlert = HealthAlert(level, title, message, record = record)
                showBloodPressureDialog = false
            }
        )
    }

    // 血糖输入对话框
    if (showBloodSugarDialog) {
        BloodSugarDialog(
            onDismiss = { showBloodSugarDialog = false },
            onConfirm = { value, isFasting ->
                val (level, title, message) = checkBloodSugarAlert(value)
                val record = HealthRecord(
                    type = RecordType.BLOOD_SUGAR,
                    value = value
                )
                healthRecordManager.saveRecord(record)
                lastAlert = HealthAlert(level, title, message, record = record)
                showBloodSugarDialog = false
            }
        )
    }

    // 心率输入对话框
    if (showHeartRateDialog) {
        HeartRateDialog(
            onDismiss = { showHeartRateDialog = false },
            onConfirm = { value ->
                val (level, title, message) = checkHeartRateAlert(value)
                val record = HealthRecord(
                    type = RecordType.HEART_RATE,
                    value = value
                )
                healthRecordManager.saveRecord(record)
                lastAlert = HealthAlert(level, title, message, record = record)
                showHeartRateDialog = false
            }
        )
    }
}

// ========== 内联健康检查逻辑 ==========

private fun checkBloodPressureAlert(systolic: Float, diastolic: Float): Triple<AlertLevel, String, String> {
    return when {
        systolic > 180 || diastolic > 120 -> Triple(
            AlertLevel.RED,
            "血压危险！立即就医！",
            "收缩压 ${systolic.toInt()}mmHg，舒张压 ${diastolic.toInt()}mmHg，严重超标！请立即拨打120或前往最近医院！"
        )
        systolic < 90 || diastolic < 60 -> Triple(
            AlertLevel.RED,
            "血压过低！",
            "血压偏低，可能导致晕厥。请坐下休息并尽快就医。"
        )
        systolic > 160 || diastolic > 100 -> Triple(
            AlertLevel.ORANGE,
            "血压偏高！",
            "收缩压 ${systolic.toInt()}mmHg，舒张压 ${diastolic.toInt()}mmHg，建议尽快就医调整用药。"
        )
        systolic > 140 || diastolic > 90 -> Triple(
            AlertLevel.YELLOW,
            "血压轻度升高",
            "血压略高，建议清淡饮食、适当运动，密切关注。"
        )
        else -> Triple(
            AlertLevel.GREEN,
            "血压正常",
            "血压在正常范围内，请继续保持健康生活方式。"
        )
    }
}

private fun checkBloodSugarAlert(value: Float): Triple<AlertLevel, String, String> {
    return when {
        value > 16.7f -> Triple(
            AlertLevel.RED,
            "血糖危险！立即就医！",
            "血糖 ${value}mmol/L，严重超标！可能导致酮症酸中毒，请立即就医！"
        )
        value < 2.8f -> Triple(
            AlertLevel.RED,
            "血糖危险！",
            "血糖 ${value}mmol/L，严重偏低！请立即补充糖分并就医！"
        )
        value > 13.9f -> Triple(
            AlertLevel.ORANGE,
            "血糖偏高",
            "血糖 ${value}mmol/L，偏高。请注意控制饮食，增加运动，或咨询医生调整用药。"
        )
        value < 3.9f -> Triple(
            AlertLevel.ORANGE,
            "血糖偏低",
            "血糖 ${value}mmol/L，偏低。请适当补充糖分，避免低血糖反应。"
        )
        value > 10.0f -> Triple(
            AlertLevel.YELLOW,
            "血糖略高",
            "血糖 ${value}mmol/L，略高。请继续控制饮食，密切关注。"
        )
        value < 4.4f -> Triple(
            AlertLevel.YELLOW,
            "血糖略低",
            "血糖 ${value}mmol/L，略低。请注意营养均衡。"
        )
        else -> Triple(
            AlertLevel.GREEN,
            "血糖正常",
            "血糖 ${value}mmol/L，在正常范围内，请继续保持。"
        )
    }
}

private fun checkTrendAlert(records: List<HealthRecord>): HealthAlert? {
    if (records.size < 3) return null
    val bpRecords = records.filter { it.type == RecordType.BLOOD_PRESSURE }.take(5)
    if (bpRecords.size >= 3) {
        val systolicValues = bpRecords.map { it.value }
        var risingCount = 0
        for (i in 1 until systolicValues.size) {
            if (systolicValues[i] > systolicValues[i - 1] + 5) risingCount++
        }
        if (risingCount >= 2) {
            return HealthAlert(
                level = AlertLevel.YELLOW,
                title = "血压上升趋势",
                message = "检测到血压连续上升趋势，建议密切关注，必要时就医。"
            )
        }
    }
    return null
}

private fun checkHeartRateAlert(value: Float): Triple<AlertLevel, String, String> {
    return when {
        value > 150 || value < 40 -> Triple(
            AlertLevel.RED,
            "心率危险！立即就医！",
            "心率 ${value.toInt()}次/分，严重异常！请立即拨打120！"
        )
        value > 120 || value < 50 -> Triple(
            AlertLevel.ORANGE,
            "心率异常",
            "心率 ${value.toInt()}次/分，建议就医检查心脏功能。"
        )
        value > 100 || value < 60 -> Triple(
            AlertLevel.YELLOW,
            "心率略异常",
            "心率 ${value.toInt()}次/分，略偏离正常范围，请注意休息，密切关注。"
        )
        else -> Triple(
            AlertLevel.GREEN,
            "心率正常",
            "心率 ${value.toInt()}次/分，在正常范围内，请继续保持。"
        )
    }
}

@Composable
private fun HistoryTab(healthRecordManager: HealthRecordManager) {
    val records by remember { 
        mutableStateOf(healthRecordManager.getRecords()) 
    }

    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无记录\n请先添加记录",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.SpacingM.dp),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingM.dp)
        ) {
            items(records) { record ->
                HealthRecordCard(record)
            }
        }
    }
}

@Composable
private fun TrendTab(healthRecordManager: HealthRecordManager) {
    val weekRecords = remember { healthRecordManager.getWeekRecords() }
    val trendAlert = remember { checkTrendAlert(weekRecords) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SpacingL.dp)
    ) {
        Text(
            text = "本周趋势分析",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

        // 本周统计
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
        ) {
            Column(modifier = Modifier.padding(Dimensions.SpacingL.dp)) {
                val bpRecords = weekRecords.filter { it.type == RecordType.BLOOD_PRESSURE }
                val bsRecords = weekRecords.filter { it.type == RecordType.BLOOD_SUGAR }
                val hrRecords = weekRecords.filter { it.type == RecordType.HEART_RATE }

                Text("本周记录统计", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))
                Text("血压记录: ${bpRecords.size} 次", style = MaterialTheme.typography.bodyLarge)
                Text("血糖记录: ${bsRecords.size} 次", style = MaterialTheme.typography.bodyLarge)
                Text("心率记录: ${hrRecords.size} 次", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

        // 趋势警报
        trendAlert?.let { alert ->
            AlertCard(
                title = alert.title,
                message = alert.message,
                level = alert.level
            )
        } ?: run {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AlertGreen.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "✓ 本周健康趋势良好",
                    modifier = Modifier.padding(Dimensions.SpacingL.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = AlertGreen
                )
            }
        }
    }
}

@Composable
private fun HealthRecordCard(record: HealthRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingM.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (record.type) {
                        RecordType.BLOOD_PRESSURE -> "❤️ 血压"
                        RecordType.BLOOD_SUGAR -> "🩸 血糖"
                        RecordType.HEART_RATE -> "💓 心率"
                        RecordType.BODY_TEMPERATURE -> "🌡️ 体温"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = record.getFormattedTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Text(
                text = when (record.type) {
                    RecordType.BLOOD_PRESSURE -> "${record.value.toInt()}/${record.secondaryValue?.toInt() ?: 0} mmHg"
                    RecordType.BLOOD_SUGAR -> "${record.value} mmol/L"
                    RecordType.HEART_RATE -> "${record.value.toInt()} 次/分"
                    RecordType.BODY_TEMPERATURE -> "${record.value} °C"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun BloodPressureDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float, Float) -> Unit
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录血压", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text("收缩压（高压）", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = systolic,
                    onValueChange = { systolic = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("mmHg") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("舒张压（低压）", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = diastolic,
                    onValueChange = { diastolic = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("mmHg") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sys = systolic.toFloatOrNull()
                    val dia = diastolic.toFloatOrNull()
                    if (sys != null && dia != null) {
                        onConfirm(sys, dia)
                    }
                }
            ) {
                Text("确认", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
private fun BloodSugarDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float, Boolean) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var isFasting by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录血糖", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text("血糖值", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("mmol/L") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isFasting,
                        onCheckedChange = { isFasting = it }
                    )
                    Text("空腹测量", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toFloatOrNull()
                    if (v != null) {
                        onConfirm(v, isFasting)
                    }
                }
            ) {
                Text("确认", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}

@Composable
private fun HeartRateDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录心率", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text("心率", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("次/分") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toFloatOrNull()
                    if (v != null) {
                        onConfirm(v)
                    }
                }
            ) {
                Text("确认", style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", style = MaterialTheme.typography.titleMedium)
            }
        }
    )
}
