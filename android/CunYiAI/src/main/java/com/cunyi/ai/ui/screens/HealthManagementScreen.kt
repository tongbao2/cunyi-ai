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
import com.cunyi.ai.manager.RulesEngine
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
                val alert = RulesEngine.checkBloodPressure(systolic, diastolic)
                val record = HealthRecord(
                    type = RecordType.BLOOD_PRESSURE,
                    value = systolic,
                    secondaryValue = diastolic
                )
                healthRecordManager.saveRecord(record)
                lastAlert = alert
                showBloodPressureDialog = false
            }
        )
    }

    // 血糖输入对话框
    if (showBloodSugarDialog) {
        BloodSugarDialog(
            onDismiss = { showBloodSugarDialog = false },
            onConfirm = { value, isFasting ->
                val alert = RulesEngine.checkBloodSugar(value, isFasting)
                val record = HealthRecord(
                    type = RecordType.BLOOD_SUGAR,
                    value = value
                )
                healthRecordManager.saveRecord(record)
                lastAlert = alert
                showBloodSugarDialog = false
            }
        )
    }

    // 心率输入对话框
    if (showHeartRateDialog) {
        HeartRateDialog(
            onDismiss = { showHeartRateDialog = false },
            onConfirm = { value ->
                val alert = RulesEngine.checkHeartRate(value)
                val record = HealthRecord(
                    type = RecordType.HEART_RATE,
                    value = value
                )
                healthRecordManager.saveRecord(record)
                lastAlert = alert
                showHeartRateDialog = false
            }
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
    val trendAlert = remember { RulesEngine.checkTrend(weekRecords) }

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
