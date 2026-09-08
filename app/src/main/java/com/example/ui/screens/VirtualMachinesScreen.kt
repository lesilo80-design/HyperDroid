package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VmEntity
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeDark
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
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalYellow

@Composable
fun VirtualMachinesScreen(
    vms: List<VmEntity>,
    selectedVmId: Int?,
    consoleOutputs: Map<Int, List<String>>,
    onSelectVm: (Int?) -> Unit,
    onStartVm: (Int) -> Unit,
    onStopVm: (Int) -> Unit,
    onPauseVm: (Int) -> Unit,
    onForceResetVm: (Int) -> Unit = {},
    onTriggerFault: (Int) -> Unit,
    onDeleteVm: (Int) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onSendCommand: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeConsoleVm by remember { mutableStateOf<VmEntity?>(null) }
    var vmToDelete by remember { mutableStateOf<VmEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Slate950).testTag("virtual_machines_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Virtual Machines & Sandboxes",
                    icon = Icons.Default.Memory,
                    badgeCount = "${vms.count { it.status == "RUNNING" }}/${vms.size} RUNNING"
                )
                Text(
                    text = "Proxmox QEMU/crosvm equivalents. Isolated guest environments executing under pKVM Stage-2 memory translation and Seccomp filters.",
                    fontSize = 12.sp,
                    color = Slate400,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(vms, key = { it.id }) { vm ->
                VmCard(
                    vm = vm,
                    isSelected = vm.id == selectedVmId,
                    onSelect = { onSelectVm(vm.id) },
                    onStart = { onStartVm(vm.id) },
                    onStop = { onStopVm(vm.id) },
                    onPause = { onPauseVm(vm.id) },
                    onForceReset = { onForceResetVm(vm.id) },
                    onTriggerFault = { onTriggerFault(vm.id) },
                    onOpenConsole = { activeConsoleVm = vm },
                    onDelete = { vmToDelete = vm }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to Add VM
        FloatingActionButton(
            onClick = onOpenCreateDialog,
            containerColor = ProxmoxOrange,
            contentColor = Slate950,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_vm_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create VM")
                Spacer(modifier = Modifier.width(6.dp))
                Text("NEW VM", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Terminal Console Dialog
        if (activeConsoleVm != null) {
            val vm = activeConsoleVm!!
            val logs = consoleOutputs[vm.id] ?: listOf("Initializing console for VM ${vm.id}...")
            VmConsoleDialog(
                vm = vm,
                consoleLogs = logs,
                onDismiss = { activeConsoleVm = null },
                onSendCommand = { cmd -> onSendCommand(vm.id, cmd) },
                onTriggerFault = { onTriggerFault(vm.id) }
            )
        }

        // Delete Confirmation Dialog
        if (vmToDelete != null) {
            val target = vmToDelete!!
            AlertDialog(
                onDismissRequest = { vmToDelete = null },
                title = { Text("Delete VM ${target.id}?", color = PureWhite) },
                text = {
                    Text(
                        "Are you sure you want to delete ${target.name}? All stage-2 memory mappings and ephemeral disk states will be purged.",
                        color = Slate400
                    )
                },
                containerColor = Slate900,
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteVm(target.id)
                            vmToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Delete", color = PureWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vmToDelete = null }) {
                        Text("Cancel", color = Slate400)
                    }
                }
            )
        }
    }
}

@Composable
fun VmCard(
    vm: VmEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onForceReset: () -> Unit = {},
    onTriggerFault: () -> Unit,
    onOpenConsole: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = vm.status == "RUNNING"
    val isPaused = vm.status == "PAUSED"
    val isWindows = vm.osFamily.equals("WINDOWS", ignoreCase = true)
    val isLinux = vm.osFamily.equals("LINUX", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Slate850 else Slate900,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) (if (isWindows) CyberCyan else ProxmoxOrange) else Slate800
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("vm_card_${vm.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: VM ID + Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isWindows -> CyberCyan.copy(alpha = 0.2f)
                                    isLinux -> ProxmoxOrange.copy(alpha = 0.2f)
                                    else -> StatusGreen.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = vm.osFamily.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isWindows -> CyberCyan
                                        isLinux -> ProxmoxOrange
                                        else -> StatusGreen
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${vm.osType} • ${vm.architecture}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
                StatusBadge(status = vm.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs and Isolation Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SpecBadge("${vm.vCpuCount} vCPU")
                SpecBadge("${vm.ramMb}MB")
                SpecBadge("${vm.diskSizeGb}GB NVMe")
                if (vm.hasTpm) {
                    IsolationPill("vTPM 2.0", CyberCyan, CyberCyanDim)
                }
                if (vm.firmware.contains("UEFI")) {
                    IsolationPill("UEFI SECURE", ProxmoxOrangeLight, Slate800)
                }
                if (vm.isProtectedPkvm) {
                    IsolationPill("pKVM STAGE-2", StatusGreen, StatusGreenBg)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Metrics (when running)
            if (isRunning || isPaused) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CPU LOAD", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text("${vm.cpuUsagePercent}%", fontSize = 12.sp, color = PureWhite, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("RAM USAGE", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text("${vm.memoryUsageMb} MB", fontSize = 12.sp, color = PureWhite, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("HYPERCALLS", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text("${vm.trappedHypercalls}", fontSize = 12.sp, color = CyberCyan, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("TRAPPED MMIO", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            Text("${vm.pageFaults}", fontSize = 12.sp, color = if (vm.pageFaults > 0) StatusYellow else Slate400, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Last Log Snippet
            Text(
                text = "Log: ${vm.lastLogLine}",
                fontSize = 10.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning) {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_vm_${vm.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(16.dp), tint = Slate950)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate950)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_vm_${vm.id}")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp), tint = PureWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }

                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp), tint = Slate950)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "RESUME" else "PAUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate950)
                    }
                }

                // Force Reset button
                IconButton(
                    onClick = onForceReset,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                        .border(1.dp, StatusRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("reset_vm_${vm.id}")
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Force Reset VM",
                        tint = StatusRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Console button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    modifier = Modifier.clickable { onOpenConsole() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Console", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                }

                // Fault injection probe test
                IconButton(
                    onClick = onTriggerFault,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusRedBg)
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "Test Fault Containment",
                        tint = StatusRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete VM",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpecBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Slate800
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Slate200,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun IsolationPill(text: String, color: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun VmConsoleDialog(
    vm: VmEntity,
    consoleLogs: List<String>,
    onDismiss: () -> Unit,
    onSendCommand: (String) -> Unit,
    onTriggerFault: () -> Unit
) {
    var commandInput by remember { mutableStateOf("") }
    val isWindows = vm.osFamily.equals("WINDOWS", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Slate900,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (isWindows) CyberCyan else ProxmoxOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isWindows) "VM ${vm.id} PowerShell [Win11]" else "VM ${vm.id} Bash Console",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isWindows)
                        "Windows kernel serial COM1 console (PS C:\\Users\\Administrator>)"
                    else
                        "Interactive serial terminal bound to guest ttyS0 / virtio-console.",
                    fontSize = 11.sp,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Terminal window
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        items(consoleLogs) { line ->
                            val color = when {
                                line.contains("pkvm", ignoreCase = true) || line.contains("Hyper-V", ignoreCase = true) -> TerminalCyan
                                line.contains("ALERT", ignoreCase = true) || line.contains("CRITICAL", ignoreCase = true) || line.contains("FAULT", ignoreCase = true) -> StatusRed
                                line.contains("PASSED", ignoreCase = true) || line.contains("OK", ignoreCase = true) -> TerminalGreen
                                line.startsWith("$") || line.startsWith("PS") -> TerminalYellow
                                else -> Slate200
                            }
                            Text(
                                text = line,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Command input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = {
                            Text(
                                if (isWindows) "Get-ComputerInfo, systeminfo, dir..." else "uname -a, status, test-seccomp...",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isWindows) CyberCyan else ProxmoxOrange,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_input_field")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                onSendCommand(commandInput)
                                commandInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isWindows) CyberCyan else ProxmoxOrange)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Slate950)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick test command chips (OS adaptive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isWindows) {
                        QuickCommandChip("Get-ComputerInfo") { onSendCommand("Get-ComputerInfo") }
                        QuickCommandChip("systeminfo") { onSendCommand("systeminfo") }
                        QuickCommandChip("Get-Service") { onSendCommand("Get-Service") }
                        QuickCommandChip("cls") { onSendCommand("cls") }
                    } else {
                        QuickCommandChip("status") { onSendCommand("status") }
                        QuickCommandChip("uname -a") { onSendCommand("uname -a") }
                        QuickCommandChip("test-seccomp") { onSendCommand("test-seccomp") }
                        QuickCommandChip("clear") { onSendCommand("clear") }
                    }
                    QuickCommandChip("fault") { onTriggerFault() }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate400)
            }
        }
    )
}

@Composable
fun QuickCommandChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Slate800,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = CyberCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
