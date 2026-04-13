package com.cunyi.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cunyi.ai.manager.AiEngine
import com.cunyi.ai.manager.HealthRecordManager
import com.cunyi.ai.manager.ModelManager
import com.cunyi.ai.manager.SOSManager
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.screens.*
import com.cunyi.ai.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthRecordManager: HealthRecordManager
    private lateinit var sosManager: SOSManager
    private lateinit var modelManager: ModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthRecordManager = HealthRecordManager(this)
        sosManager = SOSManager(this)
        modelManager = ModelManager(this)

        setContent {
            CunYiAITheme {
                CunYiAIMainScreen(
                    healthRecordManager = healthRecordManager,
                    sosManager = sosManager,
                    modelManager = modelManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CunYiAIMainScreen(
    healthRecordManager: HealthRecordManager,
    sosManager: SOSManager,
    modelManager: ModelManager
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var modelStatus by remember { mutableStateOf(AiEngine.getModelStatus()) }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            if (currentScreen != Screen.Home) {
                // 子页面使用各自的 TopBar
            } else {
                // 首页状态栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.SpacingM.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏥 村医AI",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextOnPrimary
                        )
                        StatusIndicator(
                            text = modelStatus,
                            color = if (AiEngine.isModelReady()) AlertGreen else AccentOrange
                        )
                    }
                }
            }
        },
        bottomBar = {
            // 底部导航（仅首页显示）
            if (currentScreen == Screen.Home) {
                NavigationBar(
                    containerColor = BackgroundWhite
                ) {
                    NavigationBarItem(
                        icon = { Text("🏠", style = MaterialTheme.typography.titleLarge) },
                        label = { Text("首页", style = MaterialTheme.typography.labelLarge) },
                        selected = true,
                        onClick = { currentScreen = Screen.Home }
                    )
                    NavigationBarItem(
                        icon = { Text("💬", style = MaterialTheme.typography.titleLarge) },
                        label = { Text("问诊", style = MaterialTheme.typography.labelLarge) },
                        selected = false,
                        onClick = { currentScreen = Screen.Chat }
                    )
                    NavigationBarItem(
                        icon = { Text("📊", style = MaterialTheme.typography.titleLarge) },
                        label = { Text("健康", style = MaterialTheme.typography.labelLarge) },
                        selected = false,
                        onClick = { currentScreen = Screen.Health }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️", style = MaterialTheme.typography.titleLarge) },
                        label = { Text("设置", style = MaterialTheme.typography.labelLarge) },
                        selected = false,
                        onClick = { currentScreen = Screen.Settings }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onNavigateToSOS = { currentScreen = Screen.SOS },
                    onNavigateToMedicine = { currentScreen = Screen.Medicine },
                    onNavigateToHealth = { currentScreen = Screen.Health },
                    onNavigateToChat = { currentScreen = Screen.Chat },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToModelDownload = { currentScreen = Screen.ModelDownload }
                )
                Screen.Chat -> ChatScreen(
                    onBack = { currentScreen = Screen.Home },
                    onSOS = { currentScreen = Screen.SOS },
                    messages = chatMessages,
                    onSendMessage = { message ->
                        // 添加用户消息
                        chatMessages = chatMessages + ChatMessage(message, isUser = true)
                        // 调用 AI 引擎生成回复
                        val aiResponse = AiEngine.generateResponse(message)
                        chatMessages = chatMessages + ChatMessage(aiResponse, isUser = false)
                    }
                )
                Screen.Health -> HealthManagementScreen(
                    healthRecordManager = healthRecordManager,
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.SOS -> SOSScreen(
                    sosManager = sosManager,
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Medicine -> MedicineRecognitionScreen(
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Settings -> SettingsScreen(
                    sosManager = sosManager,
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.ModelDownload -> ModelDownloadScreen(
                    modelManager = modelManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

enum class Screen {
    Home, Chat, Health, SOS, Medicine, Settings, ModelDownload
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onNavigateToSOS: () -> Unit,
    onNavigateToMedicine: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModelDownload: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.SpacingL.dp),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingL.dp)
    ) {
        // SOS 大按钮
        item {
            SOSButton(onClick = onNavigateToSOS)
        }

        // 功能列表
        item {
            Text(
                text = "选择功能",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Dimensions.SpacingM.dp)
            )
        }

        item {
            FunctionCard(
                title = "语音问诊",
                description = "AI分析症状，给出健康建议",
                icon = "💬",
                onClick = onNavigateToChat
            )
        }

        item {
            FunctionCard(
                title = "拍照识药",
                description = "识别药盒，查看用药说明",
                icon = "💊",
                onClick = onNavigateToMedicine
            )
        }

        item {
            FunctionCard(
                title = "慢病管理",
                description = "记录血压、血糖、心率",
                icon = "📊",
                onClick = onNavigateToHealth
            )
        }

        item {
            FunctionCard(
                title = "紧急求救",
                description = "一键发送求救短信给家人",
                icon = "🆘",
                onClick = onNavigateToSOS
            )
        }

        item {
            FunctionCard(
                title = "下载AI模型",
                description = "下载离线AI模型（约2.4GB）",
                icon = "🤖",
                onClick = onNavigateToModelDownload
            )
        }

        item {
            FunctionCard(
                title = "设置",
                description = "设置紧急联系人、用户信息",
                icon = "⚙️",
                onClick = onNavigateToSettings
            )
        }

        // 免责声明
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AlertYellow.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(Dimensions.SpacingM.dp)) {
                    Text(
                        text = "⚠️ 免责声明",
                        style = MaterialTheme.typography.titleMedium,
                        color = AlertOrange
                    )
                    Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                    Text(
                        text = "AI 建议仅供参考，不能替代医生诊断。如有不适，请及时就医。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    onBack: () -> Unit,
    onSOS: () -> Unit,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音问诊", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // SOS 按钮
                    IconButton(onClick = onSOS) {
                        Icon(
                            Icons.Default.Warning,
                            "紧急求救",
                            tint = AlertRed
                        )
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
            // 聊天消息列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SpacingM.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingM.dp),
                state = rememberLazyListState()
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingXL.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🏥",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))
                                Text(
                                    text = "村医AI",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "请描述您的症状，我会尽力帮助您",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(messages) { message ->
                    ChatBubble(
                        text = message.content,
                        isUser = message.isUser
                    )
                }
            }

            // 输入区域
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = BackgroundWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.SpacingM.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入您的问题...", style = MaterialTheme.typography.bodyMedium) },
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(Dimensions.SpacingM.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("发送", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    sosManager: SOSManager,
    onBack: () -> Unit
) {
    var userName by remember { mutableStateOf(sosManager.getUserName()) }
    var userLocation by remember { mutableStateOf(sosManager.getUserLocation()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.headlineMedium) },
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
            item {
                Text(
                    text = "基本信息",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        sosManager.setUserName(it)
                    },
                    label = { Text("姓名（用于紧急求救）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = userLocation,
                    onValueChange = {
                        userLocation = it
                        sosManager.setUserLocation(it)
                    },
                    label = { Text("地址（用于紧急求救）") },
                    placeholder = { Text("如：河北省保定市某县某村") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "模型设置",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Dimensions.SpacingL.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SpacingL.dp)) {
                        Text(
                            text = "当前模型：Gemma 4 E2B Q4_K_M",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "大小：约 2.4 GB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))
                        Text(
                            text = "首次使用需要下载模型文件，下载完成后即可离线使用。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Dimensions.SpacingL.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.SpacingL.dp)) {
                        Text(
                            text = "村医AI v1.0.0",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                        Text(
                            text = "基于 Google Gemma 4 E2B 模型开发的离线医学助手",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                        Text(
                            text = "专为农村老年人设计，无需网络即可使用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
