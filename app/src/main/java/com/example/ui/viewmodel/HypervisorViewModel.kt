package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.HostNodeInfo
import com.example.data.model.HypervisorLogEntity
import com.example.data.model.IsolationTestEntity
import com.example.data.model.IsoImage
import com.example.data.model.VmEntity
import com.example.data.repository.HypervisorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavTab {
    NODE, VMS, TESTS, CONSOLE, STORAGE
}

data class HypervisorUiState(
    val hostNode: HostNodeInfo,
    val vms: List<VmEntity> = emptyList(),
    val tests: List<IsolationTestEntity> = emptyList(),
    val logs: List<HypervisorLogEntity> = emptyList(),
    val isoImages: List<IsoImage> = emptyList(),
    val selectedTab: NavTab = NavTab.NODE,
    val selectedVmId: Int? = null,
    val isAuditing: Boolean = false,
    val isCreateVmDialogOpen: Boolean = false,
    val selectedTestCategory: String = "ALL",
    val consoleOutputs: Map<Int, List<String>> = emptyMap(),
    val stressedVmIds: Set<Int> = emptySet()
)

class HypervisorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = HypervisorRepository(
        vmDao = database.vmDao(),
        isolationTestDao = database.isolationTestDao(),
        hypervisorLogDao = database.hypervisorLogDao(),
        vmMetricDao = database.vmMetricDao(),
        scope = viewModelScope
    )

    private val _selectedTab = MutableStateFlow(NavTab.NODE)
    private val _selectedVmId = MutableStateFlow<Int?>(100)
    private val _isAuditing = MutableStateFlow(false)
    private val _isCreateVmDialogOpen = MutableStateFlow(false)
    private val _selectedTestCategory = MutableStateFlow("ALL")
    private val _stressedVmIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _consoleOutputs = MutableStateFlow<Map<Int, List<String>>>(
        mapOf(
            100 to listOf(
                "pkvm: Stage-2 translation initialized for VM 100",
                "seccomp: BPF filter active: STRICT_BPF",
                "Linux microdroid-vm 6.6.21-virt aarch64 ready.",
                "Type 'help' for hypervisor console commands."
            )
        )
    )

    val uiState: StateFlow<HypervisorUiState> = combine(
        repository.hostNodeInfo,
        repository.allVms,
        repository.allTests,
        repository.recentLogs,
        repository.isoImages,
        _selectedTab,
        _selectedVmId,
        _isAuditing,
        _isCreateVmDialogOpen,
        _selectedTestCategory,
        _consoleOutputs,
        _stressedVmIds
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        HypervisorUiState(
            hostNode = args[0] as HostNodeInfo,
            vms = args[1] as List<VmEntity>,
            tests = args[2] as List<IsolationTestEntity>,
            logs = args[3] as List<HypervisorLogEntity>,
            isoImages = args[4] as List<IsoImage>,
            selectedTab = args[5] as NavTab,
            selectedVmId = args[6] as Int?,
            isAuditing = args[7] as Boolean,
            isCreateVmDialogOpen = args[8] as Boolean,
            selectedTestCategory = args[9] as String,
            consoleOutputs = args[10] as Map<Int, List<String>>,
            stressedVmIds = args[11] as Set<Int>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HypervisorUiState(hostNode = repository.hostNodeInfo.value)
    )

    init {
        viewModelScope.launch {
            repository.engineManager.consoleOutputFlow.collect { (vmId, output) ->
                val current = _consoleOutputs.value.toMutableMap()
                val list = current[vmId]?.toMutableList() ?: mutableListOf()
                list.add(output)
                current[vmId] = list
                _consoleOutputs.value = current
            }
        }
    }

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun selectVm(id: Int?) {
        _selectedVmId.value = id
    }

    fun setCreateVmDialogOpen(open: Boolean) {
        _isCreateVmDialogOpen.value = open
    }

    fun selectTestCategory(category: String) {
        _selectedTestCategory.value = category
    }

    fun startVm(id: Int) {
        viewModelScope.launch {
            repository.startVm(id)
        }
    }

    fun stopVm(id: Int) {
        viewModelScope.launch {
            repository.stopVm(id)
        }
    }

    fun pauseVm(id: Int) {
        viewModelScope.launch {
            repository.pauseVm(id)
        }
    }

    fun forceResetVm(id: Int) {
        viewModelScope.launch {
            repository.forceResetVm(id)
        }
    }

    fun toggleStressTest(id: Int) {
        val current = _stressedVmIds.value.toMutableSet()
        val isNowStressed = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        _stressedVmIds.value = current
        repository.toggleLoadSpike(id, isNowStressed)
    }

    fun triggerFault(id: Int) {
        viewModelScope.launch {
            repository.triggerFault(id)
        }
    }

    fun deleteVm(id: Int) {
        viewModelScope.launch {
            repository.deleteVm(id)
            if (_selectedVmId.value == id) {
                _selectedVmId.value = null
            }
        }
    }

    fun createVm(
        name: String,
        osType: String,
        vCpu: Int,
        ramMb: Int,
        isProtectedPkvm: Boolean,
        seccomp: String,
        selinux: String,
        osFamily: String = "LINUX",
        architecture: String = "ARM64",
        firmware: String = "UEFI (EDK2 with Secure Boot)",
        hasTpm: Boolean = false,
        hasVirtIoDrivers: Boolean = true,
        displayMode: String = "VIRTIO_GPU",
        diskSizeGb: Int = 32
    ) {
        viewModelScope.launch {
            val newVm = repository.createVm(
                name = name,
                osType = osType,
                vCpu = vCpu,
                ramMb = ramMb,
                isProtectedPkvm = isProtectedPkvm,
                seccomp = seccomp,
                selinux = selinux,
                osFamily = osFamily,
                architecture = architecture,
                firmware = firmware,
                hasTpm = hasTpm,
                hasVirtIoDrivers = hasVirtIoDrivers,
                displayMode = displayMode,
                diskSizeGb = diskSizeGb
            )
            _selectedVmId.value = newVm.id
            _isCreateVmDialogOpen.value = false
        }
    }

    fun addCustomIsoImage(image: IsoImage) {
        viewModelScope.launch {
            repository.addCustomIsoImage(image)
        }
    }

    fun runSingleTest(testId: String) {
        viewModelScope.launch {
            repository.runSingleTest(testId)
        }
    }

    fun runAllTests() {
        viewModelScope.launch {
            _isAuditing.value = true
            try {
                repository.runAllTests()
            } finally {
                _isAuditing.value = false
            }
        }
    }

    fun sendConsoleCommand(vmId: Int, command: String) {
        viewModelScope.launch {
            val trimmed = command.trim()
            if (trimmed.equals("clear", ignoreCase = true) || trimmed.equals("cls", ignoreCase = true)) {
                val current = _consoleOutputs.value.toMutableMap()
                current[vmId] = emptyList()
                _consoleOutputs.value = current
            } else {
                val vm = repository.getVm(vmId)
                repository.engineManager.executeConsoleCommand(vmId, command, vm)
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun refreshHost() {
        viewModelScope.launch {
            repository.refreshHostNodeInfo()
        }
    }
}
