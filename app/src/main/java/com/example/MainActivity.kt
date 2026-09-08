package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ProxmoxTopBar
import com.example.ui.screens.CreateVmDialog
import com.example.ui.screens.HypercallConsoleScreen
import com.example.ui.screens.IsolationTestSuiteScreen
import com.example.ui.screens.NodeDashboardScreen
import com.example.ui.screens.StorageImagesScreen
import com.example.ui.screens.VirtualMachinesScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeDark
import com.example.ui.theme.ProxmoxOrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.viewmodel.HypervisorUiState
import com.example.ui.viewmodel.HypervisorViewModel
import com.example.ui.viewmodel.MetricViewMode
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.VmMetricsUiState
import com.example.ui.viewmodel.VmMetricsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HypervisorViewModel by viewModels()
    private val metricsViewModel: VmMetricsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val metricsState by metricsViewModel.uiState.collectAsStateWithLifecycle()
                HypervisorApp(
                    uiState = uiState,
                    metricsState = metricsState,
                    onSelectMetricMode = { metricsViewModel.setMetricMode(it) },
                    onSelectVmFilter = { metricsViewModel.selectVmFilter(it) },
                    onSelectTab = { viewModel.selectTab(it) },
                    onSelectVm = { viewModel.selectVm(it) },
                    onStartVm = { viewModel.startVm(it) },
                    onStopVm = { viewModel.stopVm(it) },
                    onPauseVm = { viewModel.pauseVm(it) },
                    onForceResetVm = { viewModel.forceResetVm(it) },
                    onTriggerFault = { viewModel.triggerFault(it) },
                    onDeleteVm = { viewModel.deleteVm(it) },
                    onOpenCreateDialog = { viewModel.setCreateVmDialogOpen(true) },
                    onCloseCreateDialog = { viewModel.setCreateVmDialogOpen(false) },
                    onCreateVm = { name, os, vcpu, ram, pkvm, seccomp, selinux, osFamily, arch, fw, tpm, virtio, disp, disk ->
                        viewModel.createVm(
                            name = name,
                            osType = os,
                            vCpu = vcpu,
                            ramMb = ram,
                            isProtectedPkvm = pkvm,
                            seccomp = seccomp,
                            selinux = selinux,
                            osFamily = osFamily,
                            architecture = arch,
                            firmware = fw,
                            hasTpm = tpm,
                            hasVirtIoDrivers = virtio,
                            displayMode = disp,
                            diskSizeGb = disk
                        )
                    },
                    onRunSingleTest = { viewModel.runSingleTest(it) },
                    onRunAllTests = { viewModel.runAllTests() },
                    onSelectTestCategory = { viewModel.selectTestCategory(it) },
                    onSendCommand = { vmId, cmd -> viewModel.sendConsoleCommand(vmId, cmd) },
                    onClearLogs = { viewModel.clearLogs() },
                    onRefreshHost = { viewModel.refreshHost() }
                )
            }
        }
    }
}

data class NavItem(
    val tab: NavTab,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun HypervisorApp(
    uiState: HypervisorUiState,
    metricsState: VmMetricsUiState = VmMetricsUiState(),
    onSelectMetricMode: (MetricViewMode) -> Unit = {},
    onSelectVmFilter: (Int?) -> Unit = {},
    onSelectTab: (NavTab) -> Unit,
    onSelectVm: (Int?) -> Unit,
    onStartVm: (Int) -> Unit,
    onStopVm: (Int) -> Unit,
    onPauseVm: (Int) -> Unit,
    onForceResetVm: (Int) -> Unit = {},
    onTriggerFault: (Int) -> Unit,
    onDeleteVm: (Int) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onCloseCreateDialog: () -> Unit,
    onCreateVm: (
        name: String,
        osType: String,
        vCpu: Int,
        ramMb: Int,
        isProtectedPkvm: Boolean,
        seccomp: String,
        selinux: String,
        osFamily: String,
        architecture: String,
        firmware: String,
        hasTpm: Boolean,
        hasVirtIoDrivers: Boolean,
        displayMode: String,
        diskSizeGb: Int
    ) -> Unit,
    onRunSingleTest: (String) -> Unit,
    onRunAllTests: () -> Unit,
    onSelectTestCategory: (String) -> Unit,
    onSendCommand: (Int, String) -> Unit,
    onClearLogs: () -> Unit,
    onRefreshHost: () -> Unit
) {
    val navItems = listOf(
        NavItem(NavTab.NODE, "Node", Icons.Default.Dns, "nav_node"),
        NavItem(NavTab.VMS, "VMs", Icons.Default.Memory, "nav_vms"),
        NavItem(NavTab.TESTS, "Isolation", Icons.Default.Security, "nav_tests"),
        NavItem(NavTab.CONSOLE, "Console", Icons.Default.Terminal, "nav_console"),
        NavItem(NavTab.STORAGE, "Storage", Icons.Default.Storage, "nav_storage")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 720.dp

        Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        topBar = {
            ProxmoxTopBar(
                hostNode = uiState.hostNode,
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            if (!isExpanded) {
                NavigationBar(
                    containerColor = Slate900,
                    contentColor = PureWhite,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_nav_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = uiState.selectedTab == item.tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { onSelectTab(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Slate950,
                                selectedTextColor = ProxmoxOrange,
                                indicatorColor = ProxmoxOrange,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpanded) {
                NavigationRail(
                    containerColor = Slate900,
                    contentColor = PureWhite,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navItems.forEach { item ->
                        val isSelected = uiState.selectedTab == item.tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelectTab(item.tab) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Slate950,
                                selectedTextColor = ProxmoxOrange,
                                indicatorColor = ProxmoxOrange,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    NavTab.NODE -> {
                        NodeDashboardScreen(
                            hostNode = uiState.hostNode,
                            vms = uiState.vms,
                            isAuditing = uiState.isAuditing,
                            metricsState = metricsState,
                            onSelectMetricMode = onSelectMetricMode,
                            onSelectVmFilter = onSelectVmFilter,
                            onRunAllTests = onRunAllTests,
                            onRefreshHost = onRefreshHost,
                            onNavigateToTests = { onSelectTab(NavTab.TESTS) },
                            onNavigateToVms = { onSelectTab(NavTab.VMS) },
                            onNavigateToConsole = { onSelectTab(NavTab.CONSOLE) },
                            onStartVm = onStartVm,
                            onStopVm = onStopVm,
                            onPauseVm = onPauseVm,
                            onForceResetVm = onForceResetVm,
                            onOpenConsoleForVm = { vmId ->
                                onSelectVm(vmId)
                                onSelectTab(NavTab.VMS)
                            }
                        )
                    }

                    NavTab.VMS -> {
                        VirtualMachinesScreen(
                            vms = uiState.vms,
                            selectedVmId = uiState.selectedVmId,
                            consoleOutputs = uiState.consoleOutputs,
                            onSelectVm = onSelectVm,
                            onStartVm = onStartVm,
                            onStopVm = onStopVm,
                            onPauseVm = onPauseVm,
                            onForceResetVm = onForceResetVm,
                            onTriggerFault = onTriggerFault,
                            onDeleteVm = onDeleteVm,
                            onOpenCreateDialog = onOpenCreateDialog,
                            onSendCommand = onSendCommand
                        )
                    }

                    NavTab.TESTS -> {
                        IsolationTestSuiteScreen(
                            tests = uiState.tests,
                            selectedCategory = uiState.selectedTestCategory,
                            isAuditing = uiState.isAuditing,
                            onSelectCategory = onSelectTestCategory,
                            onRunSingleTest = onRunSingleTest,
                            onRunAllTests = onRunAllTests
                        )
                    }

                    NavTab.CONSOLE -> {
                        HypercallConsoleScreen(
                            logs = uiState.logs,
                            onClearLogs = onClearLogs,
                            onSendCommand = onSendCommand
                        )
                    }

                    NavTab.STORAGE -> {
                        StorageImagesScreen(
                            images = uiState.isoImages
                        )
                    }
                }
            }
        }

        // Create VM Dialog
        if (uiState.isCreateVmDialogOpen) {
            CreateVmDialog(
                isoImages = uiState.isoImages,
                onDismiss = onCloseCreateDialog,
                onCreateVm = onCreateVm
            )
        }
    }
}
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Mobile Hypervisor: $name", modifier = modifier, color = PureWhite)
}
