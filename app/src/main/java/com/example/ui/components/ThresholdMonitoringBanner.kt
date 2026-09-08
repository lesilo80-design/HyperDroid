package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VmEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellow

data class ThresholdBreach(
    val vm: VmEntity,
    val isCpuOver: Boolean,
    val isRamOver: Boolean,
    val cpuUsage: Float,
    val ramUsagePercent: Float
)

@Composable
fun ThresholdMonitoringBanner(
    vms: List<VmEntity>,
    onPauseVm: (Int) -> Unit,
    onForceResetVm: (Int) -> Unit,
    onOpenConsoleForVm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val breaches = vms.mapNotNull { vm ->
        if (vm.status != "RUNNING" && vm.status != "PAUSED") return@mapNotNull null
        val isCpuOver = vm.cpuUsagePercent >= 90.0f
        val ramUsagePercent = if (vm.ramMb > 0) (vm.memoryUsageMb.toFloat() / vm.ramMb.toFloat()) * 100f else 0f
        val isRamOver = ramUsagePercent >= 90.0f
        if (isCpuOver || isRamOver) {
            ThresholdBreach(
                vm = vm,
                isCpuOver = isCpuOver,
                isRamOver = isRamOver,
                cpuUsage = vm.cpuUsagePercent,
                ramUsagePercent = ramUsagePercent
            )
        } else {
            null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_warning")
    val animatedBorderColor by infiniteTransition.animateColor(
        initialValue = StatusRed,
        targetValue = StatusYellow,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_pulse"
    )

    AnimatedVisibility(
        visible = breaches.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Slate900,
            border = BorderStroke(2.dp, animatedBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("threshold_warning_banner")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Warning Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = "Critical Resource Threshold Exceeded",
                                tint = StatusRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "CRITICAL RESOURCE THRESHOLD EXCEEDED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusRed,
                                letterSpacing = 0.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Guest virtual machine(s) exceeding 90.0% safety threshold",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    // Threat count badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusRedBg,
                        border = BorderStroke(1.dp, StatusRed)
                    ) {
                        Text(
                            text = "${breaches.size} CRITICAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusRed,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of breaching VMs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    breaches.forEach { breach ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate950,
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Slate800
                                        ) {
                                            Text(
                                                text = "VM ${breach.vm.id}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PureWhite,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = breach.vm.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }

                                    // Specific metrics exceeded
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (breach.isCpuOver) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StatusRedBg,
                                                border = BorderStroke(1.dp, StatusRed)
                                            ) {
                                                Text(
                                                    text = "CPU: ${String.format("%.1f%%", breach.cpuUsage)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusRed,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (breach.isRamOver) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StatusRedBg,
                                                border = BorderStroke(1.dp, StatusRed)
                                            ) {
                                                Text(
                                                    text = "RAM: ${String.format("%.1f%%", breach.ramUsagePercent)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusRed,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Fast Remediation Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pause Action
                                    Button(
                                        onClick = { onPauseVm(breach.vm.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .testTag("threshold_pause_vm_${breach.vm.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Pause,
                                            contentDescription = null,
                                            tint = Slate950,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PAUSE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate950
                                        )
                                    }

                                    // Force Reset Action
                                    Button(
                                        onClick = { onForceResetVm(breach.vm.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .testTag("threshold_reset_vm_${breach.vm.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.RestartAlt,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "HARD RESET",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }

                                    // Inspect Console Action
                                    OutlinedButton(
                                        onClick = { onOpenConsoleForVm(breach.vm.id) },
                                        border = BorderStroke(1.dp, Slate700),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .testTag("threshold_console_vm_${breach.vm.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CONSOLE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
