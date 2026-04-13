package com.cunyi.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cunyi.ai.data.AlertLevel
import com.cunyi.ai.ui.theme.*

/**
 * 鍋ュ悍璀︽姤鍗＄墖
 */
@Composable
fun AlertCard(
    title: String,
    message: String,
    level: AlertLevel,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (level) {
        AlertLevel.RED -> AlertRed
        AlertLevel.ORANGE -> AlertOrange
        AlertLevel.YELLOW -> AlertYellow
        AlertLevel.GREEN -> AlertGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.CardPadding.dp)
        ) {
            // 鏍囬琛?            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (level) {
                            AlertLevel.RED -> "馃敶 鍗遍櫓"
                            AlertLevel.ORANGE -> "馃煚 璀﹀憡"
                            AlertLevel.YELLOW -> "馃煛 娉ㄦ剰"
                            AlertLevel.GREEN -> "馃煝 姝ｅ父"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = backgroundColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingM.dp))

            // 娑堟伅
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }
    }
}

/**
 * 鏁板€兼樉绀哄崱鐗囷紙濡傝鍘嬨€佽绯栧€硷級
 */
@Composable
fun ValueCard(
    label: String,
    value: String,
    unit: String,
    level: AlertLevel = AlertLevel.GREEN,
    modifier: Modifier = Modifier
) {
    val valueColor = when (level) {
        AlertLevel.RED -> AlertRed
        AlertLevel.ORANGE -> AlertOrange
        AlertLevel.YELLOW -> AlertYellow
        AlertLevel.GREEN -> PrimaryGreen
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimensions.CornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingL.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingS.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 瀵硅瘽姘旀场
 */
@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) PrimaryGreen else BackgroundWhite
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUser) TextOnPrimary else TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * 搴曢儴瀵艰埅椤? */
@Composable
fun BottomNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                Modifier.clickable(onClick = onClick)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) PrimaryGreen else TextSecondary
        )
    }
}
