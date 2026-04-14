package com.cunyi.ai

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.flow.StateFlow
import com.cunyi.ai.manager.AiEngine
import com.cunyi.ai.manager.HealthRecordManager
import com.cunyi.ai.manager.ModelManager
import com.cunyi.ai.manager.TtsManager
import com.cunyi.ai.manager.SOSManager
import com.cunyi.ai.manager.VoiceInputManager
import com.cunyi.ai.ui.components.*
import com.cunyi.ai.ui.screens.*
import com.cunyi.ai.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var healthRecordManager: HealthRecordManager
    private lateinit var sosManager: SOSManager
    private lateinit var modelManager: ModelManager
    private lateinit var ttsManager: TtsManager
    private lateinit var voiceInputManager: VoiceInputManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthRecordManager = HealthRecordManager(this)
        sosManager = SOSManager(this)
        modelManager = ModelManager(this)
        ttsManager = TtsManager(this)
        voiceInputManager = VoiceInputManager(this)
        voiceInputManager.initialize()
        
        setContent {
            CunYiAITheme {
                CunYiAIMainScreen(
                    healthRecordManager = healthRecordManager,
                    sosManager = sosManager,
                    modelManager = modelManager,
                    ttsManager = ttsManager,
                    voiceInputManager = voiceInputManager
                )
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        voiceInputManager.destroy()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CunYiAIMainScreen(
    healthRecordManager: HealthRecordManager,
    sosManager: SOSManager,
    modelManager: ModelManager,
    ttsManager: TtsManager,
    voiceInputManager: VoiceInputManager
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
                    voiceState = voiceInputManager.voiceState,
                    ttsManager = ttsManager,
                    onSendMessage = { message ->
                        // 添加用户消息
                        chatMessages = chatMessages + ChatMessage(message, isUser = true)
                        // 调用 AI 引擎生成回复
                        val aiResponse = AiEngine.generateResponse(message)
                        chatMessages = chatMessages + ChatMessage(aiResponse, isUser = false)
                        // 语音播报 AI 回复（受开关控制）
                        if (ttsEnabled) {
                            ttsManager.speak(aiResponse)
                        }
                    },
                    onVoiceStart = { cb -> voiceInputManager.startListening(cb) },
                    onVoiceStop = { voiceInputManager.stopListening() }
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
                    onBack = { currentScreen = Screen.Home },
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
                description = "一键拨打120急救或紧急联系人",
                icon = "🆘",
                onClick = onNavigateToSOS
            )
        }

        item {
            FunctionCard(
                title = "下载AI模型",
                description = "下载离线AI模型（约3.1GB）",
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
    voiceState: StateFlow<VoiceInputManager.VoiceState>,
    ttsManager: TtsManager,
    onSendMessage: (String) -> Unit,
    onVoiceStart: ((String) -> Unit) -> Unit,
    onVoiceStop: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val currentVoiceState by voiceState.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    var ttsEnabled by remember { mutableStateOf(true) }

    // 监听语音识别结果
    LaunchedEffect(currentVoiceState) {
        when (currentVoiceState) {
            is VoiceInputManager.VoiceState.Result -> {
                val text = (currentVoiceState as VoiceInputManager.VoiceState.Result).text
                if (text.isNotBlank()) {
                    inputText = text
                }
                isRecording = false
            }
            is VoiceInputManager.VoiceState.Listening -> {
                isRecording = true
            }
            is VoiceInputManager.VoiceState.Error -> {
                isRecording = false
            }
            else -> { /* Idle, Ready */ }
        }
    }

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
                    // TTS 语音播报开关
                    IconButton(onClick = { ttsEnabled = !ttsEnabled }) {
                        Icon(
                            if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            if (ttsEnabled) "关闭语音播报" else "开启语音播报",
                            tint = if (ttsEnabled) TextOnPrimary else TextOnPrimary.copy(alpha = 0.5f)
                        )
                    }
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
                    // 语音输入按钮
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            onVoiceStart { voiceText ->
                                if (voiceText.isNotBlank()) {
                                    inputText = voiceText
                                }
                                isRecording = false
                            }
                            isRecording = true
                        } else {
                            isRecording = false
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isRecording) {
                                onVoiceStop()
                                isRecording = false
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isRecording) AlertRed.copy(alpha = 0.15f) else PrimaryGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "停止录音" else "语音输入",
                            tint = if (isRecording) AlertRed else PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))

                    // 文本输入框
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = if (isRecording) "正在聆听..." else "输入您的问题，或按住🎤说话",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            cursorColor = PrimaryGreen
                        )
                    )

                    Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))

                    // 发送按钮
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        enabled = inputText.isNotBlank() && !isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            disabledContainerColor = PrimaryGreen.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("发送", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // 录音状态提示
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.SpacingM.dp)
                            .padding(bottom = Dimensions.SpacingS.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "正在聆听... 松开发送",
                            style = MaterialTheme.typography.bodySmall,
                            color = AlertRed
                        )
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
                            text = "大小：约 3.1 GB",
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
