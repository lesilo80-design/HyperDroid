package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HostNodeInfo
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg

@Composable
fun ProxmoxTopBar(
    hostNode: HostNodeInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate900,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(listOf(ProxmoxOrange, ProxmoxOrangeLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Hypervisor Node",
                        tint = Slate950,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hostNode.nodeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PulsingStatusDot(isOnline = true)
                    }
                    Text(
                        text = "${hostNode.ipAddress} • ${hostNode.cpuArch}",
                        fontSize = 11.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // pKVM / EL2 status pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (hostNode.pkvmSupported) StatusGreenBg else CyberCyanDim,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hostNode.pkvmSupported) StatusGreen else CyberCyan
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "pKVM",
                        tint = if (hostNode.pkvmSupported) StatusGreen else CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hostNode.pkvmSupported) "pKVM EL2" else "AVF VIRT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingStatusDot(isOnline: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background((if (isOnline) StatusGreen else StatusRed).copy(alpha = alpha))
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status.uppercase()) {
        "RUNNING" -> Triple(StatusGreenBg, StatusGreen, Icons.Default.PlayCircle)
        "STOPPED" -> Triple(Slate800, Slate400, Icons.Default.StopCircle)
        "PAUSED" -> Triple(StatusYellowBg, StatusYellow, Icons.Default.PauseCircle)
        "FAULTED" -> Triple(StatusRedBg, StatusRed, Icons.Default.Error)
        "PASSED" -> Triple(StatusGreenBg, StatusGreen, Icons.Default.CheckCircle)
        "WARNING" -> Triple(StatusYellowBg, StatusYellow, Icons.Default.Warning)
        "FAILED" -> Triple(StatusRedBg, StatusRed, Icons.Default.Error)
        "RUNNING_TEST" -> Triple(CyberCyanDim, CyberCyan, null)
        else -> Triple(Slate800, Slate400, null)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = status,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else if (status.equals("RUNNING", ignoreCase = true) || status.equals("RUNNING_TEST", ignoreCase = true)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    badgeCount: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ProxmoxOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            if (badgeCount != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Text(
                        text = badgeCount,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProxmoxOrangeLight,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        action?.invoke()
    }
}

@Composable
fun MetricGauge(
    label: String,
    currentValue: String,
    maxLimit: String,
    percent: Float, // 0.0 to 1.0
    color: Color = ProxmoxOrange,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate850,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )
                Text(
                    text = "${(percent * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = Slate800
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentValue,
                    fontSize = 11.sp,
                    color = Slate200,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = maxLimit,
                    fontSize = 11.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
