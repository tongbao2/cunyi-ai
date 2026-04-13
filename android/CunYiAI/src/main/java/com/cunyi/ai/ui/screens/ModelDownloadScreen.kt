package com.cunyi.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cunyi.ai.manager.ModelManager
import com.cunyi.ai.ui.components.LargeButton
import com.cunyi.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    modelManager: ModelManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val downloadState by modelManager.downloadState.collectAsState()

    // 初始化时检查模型状态
    LaunchedEffect(Unit) {
        modelManager.checkModelState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.SpacingL.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingM.dp)
    ) {
        // 标题
        Text(
            text = "🤖 AI模型管理",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryGreen
        )

        Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))

        // 模型信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.SpacingL.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gemma 4 E2B",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                Text(
                    text = "量化版本: Q4_K_M",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "模型大小: 约 2.4 GB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))
                Text(
                    text = "下载后可离线使用AI问诊功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))

        // 下载状态显示
        when (val state = downloadState) {
            is ModelManager.DownloadState.Idle -> {
                // 未下载状态
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertYellow.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.SpacingM.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AlertYellow)
                        Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                        Text("模型未下载，请点击下方按钮下载", color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

                LargeButton(
                    text = "⬇️ 下载AI模型",
                    onClick = { scope.launch { modelManager.downloadModel() } },
                    containerColor = PrimaryGreen
                )
            }

            is ModelManager.DownloadState.Checking -> {
                CircularProgressIndicator(color = PrimaryGreen)
                Text("检查中...", color = TextSecondary)
            }

            is ModelManager.DownloadState.Downloading -> {
                // 下载进度
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.SpacingL.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PrimaryGreen,
                            trackColor = PrimaryGreen.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))
                        Text(
                            text = "下载中... ${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryGreen
                        )
                        Text(
                            text = "${modelManager.formatSize(state.downloadedBytes)} / ${modelManager.formatSize(state.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            is ModelManager.DownloadState.Error -> {
                // 错误状态
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.SpacingM.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                        Text(state.message, color = AlertRed)
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

                LargeButton(
                    text = "🔄 重试下载",
                    onClick = { scope.launch { modelManager.downloadModel() } },
                    containerColor = PrimaryGreen
                )
            }

            is ModelManager.DownloadState.Success -> {
                // 下载成功
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertGreen.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.SpacingM.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertGreen)
                        Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                        Text("模型下载完成！", color = AlertGreen)
                    }
                }
            }

            is ModelManager.DownloadState.ModelExists -> {
                // 模型已存在
                val modelSize = modelManager.getDownloadedModelSize()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.SpacingM.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AlertGreen)
                            Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                            Text("模型已下载", color = AlertGreen)
                        }
                        Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                        Text(
                            text = "大小: ${modelManager.formatSize(modelSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

                // 删除按钮
                OutlinedButton(
                    onClick = {
                        modelManager.deleteModel()
                        modelManager.checkModelState()
                    },
                    modifier = Modifier.fillMaxWidth().height(Dimensions.ButtonHeight.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                    Text("删除模型", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.SpacingXL.dp))

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.SpacingL.dp)
            ) {
                Text(
                    text = "📋 使用说明",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                val instructions = listOf(
                    "1. 点击下载按钮开始下载AI模型",
                    "2. 模型约2.4GB，建议在WiFi环境下下载",
                    "3. 下载完成后可离线使用AI问诊功能",
                    "4. 模型存储在应用目录中，卸载应用会自动删除"
                )
                instructions.forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
