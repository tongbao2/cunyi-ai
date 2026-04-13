package com.cunyi.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cunyi.ai.manager.ModelManager
import com.cunyi.ai.ui.components.LargeButton
import com.cunyi.ai.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    modelManager: ModelManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val downloadState by modelManager.downloadState.collectAsState()
    var downloadJob by remember { mutableStateOf<Job?>(null) }

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
                    text = "模型大小: 约 3.1 GB",
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
                    onClick = { downloadJob = scope.launch { modelManager.downloadModel() } },
                    containerColor = PrimaryGreen
                )

                Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
                Text(
                    text = "⚠️ 建议在WiFi环境下下载（约3.1GB）",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            is ModelManager.DownloadState.Connecting -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.SpacingL.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(Dimensions.SpacingM.dp))
                        Text(
                            text = "正在连接服务器...",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            is ModelManager.DownloadState.Downloading -> {
                // 下载进度卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.SpacingL.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 圆形进度条
                        CircularProgressWithLabel(
                            progress = state.progress,
                            downloadedBytes = state.downloadedBytes,
                            totalBytes = state.totalBytes,
                            speed = state.speed,
                            formatSize = { modelManager.formatSize(it) }
                        )

                        Spacer(modifier = Modifier.height(Dimensions.SpacingL.dp))

                        // 取消按钮
                        OutlinedButton(
                            onClick = {
                                downloadJob?.cancel()
                                downloadJob = null
                                modelManager.checkModelState()
                            },
                            modifier = Modifier.fillMaxWidth().height(Dimensions.ButtonHeight.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(Dimensions.SpacingS.dp))
                            Text("取消下载", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            is ModelManager.DownloadState.Error -> {
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
                    onClick = { downloadJob = scope.launch { modelManager.downloadModel() } },
                    containerColor = PrimaryGreen
                )
            }

            is ModelManager.DownloadState.Success -> {
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
                    "2. 模型约3.1GB，建议在WiFi环境下下载",
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

/**
 * 圆形进度条 + 文字标签
 */
@Composable
private fun CircularProgressWithLabel(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speed: String,
    formatSize: (Long) -> String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 背景圆弧
            drawArc(
                color = Color(0xFFE8E8E8),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 进度圆弧
            drawArc(
                color = Color(0xFF2E7D32), // PrimaryGreen
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryGreen
            )
            Text(
                text = speed,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }

    Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))

    // 下载详情
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "已下载",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = formatSize(downloadedBytes),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总大小",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = formatSize(totalBytes),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "速度",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = speed,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGreen
            )
        }
    }
}
