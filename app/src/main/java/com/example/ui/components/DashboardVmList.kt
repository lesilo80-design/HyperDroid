package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VmEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
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
fun DashboardVmList(
    vms: List<VmEntity>,
    onStartVm: (Int) -> Unit,
    onStopVm: (Int) -> Unit,
    onPauseVm: (Int) -> Unit,
    onForceResetVm: (Int) -> Unit,
    onNavigateToConsole: (Int) -> Unit,
    stressedVmIds: Set<Int> = emptySet(),
    onToggleStressVm: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var vmToForceReset by remember { mutableStateOf<VmEntity?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        vms.forEach { vm ->
            DashboardVmItemCard(
                vm = vm,
                isStressed = stressedVmIds.contains(vm.id),
                onToggleStress = { onToggleStressVm(vm.id) },
                onStart = { onStartVm(vm.id) },
                onStop = { onStopVm(vm.id) },
                onPause = { onPauseVm(vm.id) },
                onForceReset = { vmToForceReset = vm },
                onOpenConsole = { onNavigateToConsole(vm.id) }
            )
        }
    }

    // Force-Reset Confirmation Modal
    if (vmToForceReset != null) {
        val target = vmToForceReset!!
        AlertDialog(
            onDismissRequest = { vmToForceReset = null },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Force Reset VM ${target.id}?", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to trigger a HARDWARE FORCE RESET on guest [${target.name}]?",
                        color = Slate200,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "• Asserts ACPI hardware reset pin signal",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "• Flushes Stage-2 translation & MMIO cache lines",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "• Cold boots guest UEFI/EDK2 firmware immediately",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        vmToForceReset = null
                        onForceResetVm(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_force_reset_button")
                ) {
                    Text("ASSERT RESET", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vmToForceReset = null }) {
                    Text("CANCEL", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun DashboardVmItemCard(
    vm: VmEntity,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onForceReset: () -> Unit,
    onOpenConsole: () -> Unit,
    isStressed: Boolean = false,
    onToggleStress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRunning = vm.status == "RUNNING"
    val isPaused = vm.status == "PAUSED"
    val isStopped = vm.status == "STOPPED"
    val isFaulted = vm.status == "FAULTED"
    val isStarting = vm.status == "STARTING"
    val isWindows = vm.osFamily.equals("WINDOWS", ignoreCase = true)
    val isLinux = vm.osFamily.equals("LINUX", ignoreCase = true)

    val isCpuOver = vm.cpuUsagePercent >= 90.0f
    val ramUsagePercent = if (vm.ramMb > 0) (vm.memoryUsageMb.toFloat() / vm.ramMb.toFloat()) * 100f else 0f
    val isRamOver = ramUsagePercent >= 90.0f
    val isThresholdBreached = (isCpuOver || isRamOver) && (isRunning || isPaused)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(
            if (isThresholdBreached) 2.dp else 1.dp,
            when {
                isThresholdBreached -> StatusRed
                isRunning -> if (isWindows) CyberCyan.copy(alpha = 0.6f) else ProxmoxOrange.copy(alpha = 0.6f)
                isPaused -> StatusYellow.copy(alpha = 0.5f)
                isFaulted -> StatusRed.copy(alpha = 0.5f)
                else -> Slate800
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_vm_card_${vm.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Header: ID + Name + OS Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ID Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        Text(
                            text = "VM ${vm.id}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWindows) CyberCyan else ProxmoxOrangeLight,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = vm.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // OS Family badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isWindows -> CyberCyan.copy(alpha = 0.2f)
                                    isLinux -> ProxmoxOrange.copy(alpha = 0.2f)
                                    else -> StatusGreen.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = vm.osType.take(12),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isWindows -> CyberCyan
                                        isLinux -> ProxmoxOrangeLight
                                        else -> StatusGreen
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${vm.vCpuCount} vCPU • ${vm.ramMb} MB RAM • ${vm.architecture}",
                            fontSize = 11.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Status Badge
                StatusBadge(status = vm.status)
            }

            // Threshold Warning Alert Strip inside the VM card
            if (isThresholdBreached) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusRedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("dashboard_vm_threshold_alert_${vm.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Resource Threshold Exceeded",
                            tint = StatusRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                append("CRITICAL (>90% THRESHOLD): ")
                                if (isCpuOver) append("vCPU ${String.format("%.1f%%", vm.cpuUsagePercent)} ")
                                if (isCpuOver && isRamOver) append("• ")
                                if (isRamOver) append("RAM ${String.format("%.1f%%", ramUsagePercent)}")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resource Metrics Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate950,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CPU Load
                    Column {
                        Text(
                            text = "CPU LOAD",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCpuOver && isRunning) StatusRed else Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isRunning) {
                                if (isCpuOver) "⚠ ${String.format("%.1f%%", vm.cpuUsagePercent)}" else String.format("%.1f%%", vm.cpuUsagePercent)
                            } else "0.0%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCpuOver && isRunning) StatusRed else if (isRunning) ProxmoxOrangeLight else Slate600,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // RAM Usage
                    Column {
                        Text(
                            text = "MEMORY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRamOver && isRunning) StatusRed else Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isRamOver && isRunning) {
                                "⚠ ${vm.memoryUsageMb}/${vm.ramMb} MB (${String.format("%.0f%%", ramUsagePercent)})"
                            } else {
                                "${vm.memoryUsageMb} / ${vm.ramMb} MB"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRamOver && isRunning) StatusRed else if (isRunning) CyberCyan else Slate600,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Hypercalls
                    Column {
                        Text(
                            text = "HYPERCALLS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${vm.trappedHypercalls}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vm.trappedHypercalls > 0) StatusGreen else Slate600,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Uptime
                    Column {
                        Text(
                            text = "UPTIME",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        val mins = vm.uptimeSeconds / 60
                        val secs = vm.uptimeSeconds % 60
                        Text(
                            text = if (isRunning) String.format("%02d:%02d", mins, secs) else "--:--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) PureWhite else Slate600,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last Log Snippet
            Text(
                text = "> ${vm.lastLogLine}",
                fontSize = 10.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // LIFECYCLE CONTROLS: START, STOP, PAUSE, RESET
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. START CONTROL
                Button(
                    onClick = onStart,
                    enabled = !isRunning || isPaused,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusGreen,
                        disabledContainerColor = Slate800.copy(alpha = 0.5f),
                        contentColor = Slate950,
                        disabledContentColor = Slate600
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("dashboard_start_vm_${vm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start VM",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isPaused) "RESUME" else "START",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. STOP CONTROL
                Button(
                    onClick = onStop,
                    enabled = isRunning || isPaused,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRed,
                        disabledContainerColor = Slate800.copy(alpha = 0.5f),
                        contentColor = PureWhite,
                        disabledContentColor = Slate600
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("dashboard_stop_vm_${vm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop VM",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "STOP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. PAUSE CONTROL
                Button(
                    onClick = onPause,
                    enabled = isRunning || isPaused,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusYellow,
                        disabledContainerColor = Slate800.copy(alpha = 0.5f),
                        contentColor = Slate950,
                        disabledContentColor = Slate600
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("dashboard_pause_vm_${vm.id}")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause VM",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isPaused) "RESUME" else "PAUSE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 4. FORCE-RESET CONTROL
                Button(
                    onClick = onForceReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = StatusRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("dashboard_reset_vm_${vm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Force Reset VM",
                        modifier = Modifier.size(15.dp),
                        tint = StatusRed
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "RESET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusRed
                    )
                }

                // Quick Console Shortcut
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onOpenConsole() }
                        .testTag("dashboard_console_vm_${vm.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Open Console",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Stress / 95% Spike Simulator Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isStressed) StatusRedBg else Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isStressed) StatusRed else Slate700),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onToggleStress() }
                        .testTag("dashboard_stress_vm_${vm.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = if (isStressed) "Stop Stress (>90%)" else "Simulate >90% Load Spike",
                            tint = if (isStressed) StatusRed else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
