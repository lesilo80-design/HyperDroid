package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HostNodeInfo
import com.example.data.model.VmEntity
import com.example.ui.components.MetricGauge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.VmPerformanceChartCard
import com.example.ui.viewmodel.MetricViewMode
import com.example.ui.viewmodel.VmMetricsUiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeDark
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeDashboardScreen(
    hostNode: HostNodeInfo,
    vms: List<VmEntity>,
    isAuditing: Boolean,
    onRunAllTests: () -> Unit,
    onRefreshHost: () -> Unit,
    onNavigateToTests: () -> Unit,
    onNavigateToVms: () -> Unit,
    onNavigateToConsole: () -> Unit,
    metricsState: VmMetricsUiState = VmMetricsUiState(),
    onSelectMetricMode: (MetricViewMode) -> Unit = {},
    onSelectVmFilter: (Int?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val runningVms = vms.filter { it.status == "RUNNING" }
    val totalAllocatedCpu = runningVms.sumOf { it.vCpuCount }
    val totalAllocatedRam = runningVms.sumOf { it.ramMb }
    val totalHypercalls = vms.sumOf { it.trappedHypercalls }

    val cpuPercent = (totalAllocatedCpu.toFloat() / (hostNode.cpuCores.coerceAtLeast(1) * 2)).coerceIn(0.05f, 0.95f)
    val ramPercent = (totalAllocatedRam.toFloat() / hostNode.totalRamMb.coerceAtLeast(512)).coerceIn(0.1f, 0.95f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("node_dashboard_screen")
    ) {
        // Isolation Score Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KERNEL ISOLATION STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProxmoxOrange,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hostNode.isolationScore >= 80) "Hardened & Confined" else "Standard Isolation",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite
                        )
                    }

                    // Score Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(StatusGreen, CyberCyan, ProxmoxOrange, StatusGreen)
                                )
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Slate900),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${hostNode.isolationScore}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "INDEX",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "System active with ${hostNode.pkvmState}. Stage-2 MMU translation and BPF syscall filters isolate guest memory spaces.",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRunAllTests,
                        enabled = !isAuditing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProxmoxOrange,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_audit_button")
                    ) {
                        if (isAuditing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Slate950
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUDITING KERNEL...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN ISOLATION AUDIT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToTests,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW PROBES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Resource Gauges
        SectionHeader(
            title = "Node Resource Allocation",
            icon = Icons.Default.Speed,
            badgeCount = "${runningVms.size}/${vms.size} ONLINE",
            action = {
                IconButton(onClick = onRefreshHost) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricGauge(
                label = "vCPU Subscription",
                currentValue = "$totalAllocatedCpu Cores",
                maxLimit = "${hostNode.cpuCores * 2} Cores (Max)",
                percent = cpuPercent,
                color = ProxmoxOrange,
                modifier = Modifier.weight(1f)
            )
            MetricGauge(
                label = "Memory Provisioned",
                currentValue = "$totalAllocatedRam MB",
                maxLimit = "${hostNode.totalRamMb} MB",
                percent = ramPercent,
                color = CyberCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // VM Telemetry & Status Performance Charts (Room DB Tracked)
        SectionHeader(
            title = "VM Telemetry & Performance Charts",
            icon = Icons.Default.ShowChart,
            badgeCount = "DATABASE TRACKED"
        )

        VmPerformanceChartCard(
            metricsState = metricsState,
            onSelectMode = onSelectMetricMode,
            onSelectVmFilter = onSelectVmFilter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Virtualization & Isolation Security Matrix
        SectionHeader(
            title = "Hardware & Isolation Matrix",
            icon = Icons.Default.Security
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                IsolationMatrixRow(
                    feature = "Protected KVM (pKVM EL2)",
                    config = "CONFIG_ARM64_PKVM",
                    enabled = hostNode.pkvmSupported,
                    detail = hostNode.pkvmState
                )
                Spacer(modifier = Modifier.height(8.dp))
                IsolationMatrixRow(
                    feature = "Seccomp-BPF Filter Mode",
                    config = "SECCOMP_MODE_FILTER",
                    enabled = hostNode.seccompBpfActive,
                    detail = "Active syscall whitelist interceptor"
                )
                Spacer(modifier = Modifier.height(8.dp))
                IsolationMatrixRow(
                    feature = "Mandatory Access Control (MAC)",
                    config = "SELinux Enforcing",
                    enabled = hostNode.selinuxStatus.startsWith("Enforcing"),
                    detail = hostNode.selinuxStatus
                )
                Spacer(modifier = Modifier.height(8.dp))
                IsolationMatrixRow(
                    feature = "Linux Namespaces Separation",
                    config = "CLONE_NEWPID/NET/MNT/IPC",
                    enabled = hostNode.namespacesActive.size >= 4,
                    detail = hostNode.namespacesActive.joinToString(", ")
                )
                Spacer(modifier = Modifier.height(8.dp))
                IsolationMatrixRow(
                    feature = "Hardware Memory Tagging (MTE)",
                    config = "ARM64 FEAT_MTE",
                    enabled = hostNode.mteHardwareSupported,
                    detail = if (hostNode.mteHardwareSupported) "4-bit hardware tag validation active" else "Hardware MTE simulated / software sanitized"
                )
                Spacer(modifier = Modifier.height(8.dp))
                IsolationMatrixRow(
                    feature = "Kernel Page Table Isolation (KPTI)",
                    config = "Meltdown/Spectre Shield",
                    enabled = hostNode.kptiActive,
                    detail = "Isolated user/kernel page tables"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Host Kernel Details
        SectionHeader(
            title = "Host Node Specifications",
            icon = Icons.Default.Dns
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SpecItem("Node Name", hostNode.nodeName)
                SpecItem("OS Platform", hostNode.osRelease)
                SpecItem("Linux Kernel", hostNode.kernelVersion)
                SpecItem("Hardware Model", hostNode.cpuModel)
                SpecItem("Architecture", hostNode.cpuArch)
                SpecItem("Physical Cores", "${hostNode.cpuCores} cores available")
                SpecItem("Trapped Hypercalls", "$totalHypercalls exits handled")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Navigation Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate850,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToVms() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "VMs",
                        tint = ProxmoxOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Manage VMs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("${vms.size} Guests", fontSize = 11.sp, color = Slate400)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate850,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToConsole() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Console",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Hypercall Log", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("EL2 Traps", fontSize = 11.sp, color = Slate400)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun IsolationMatrixRow(
    feature: String,
    config: String,
    enabled: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (enabled) StatusGreenBg else StatusYellowBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (enabled) StatusGreen else StatusYellow,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feature,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
                Text(
                    text = config,
                    fontSize = 10.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = detail,
                fontSize = 11.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Slate400
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Slate200,
            fontFamily = FontFamily.Monospace
        )
    }
}
