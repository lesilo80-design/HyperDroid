package com.example.data.repository

import com.example.data.engine.KernelIsolationProber
import com.example.data.engine.VmEngineManager
import com.example.data.local.HypervisorLogDao
import com.example.data.local.IsolationTestDao
import com.example.data.local.VmDao
import com.example.data.local.VmMetricDao
import com.example.data.model.HostNodeInfo
import com.example.data.model.HypervisorLogEntity
import com.example.data.model.IsolationTestEntity
import com.example.data.model.IsoImage
import com.example.data.model.VmEntity
import com.example.data.model.VmMetricSnapshotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HypervisorRepository(
    private val vmDao: VmDao,
    private val isolationTestDao: IsolationTestDao,
    private val hypervisorLogDao: HypervisorLogDao,
    private val vmMetricDao: VmMetricDao,
    private val scope: CoroutineScope
) {
    val allVms: Flow<List<VmEntity>> = vmDao.getAllVms()
    val allTests: Flow<List<IsolationTestEntity>> = isolationTestDao.getAllTests()
    val recentLogs: Flow<List<HypervisorLogEntity>> = hypervisorLogDao.getRecentLogs()
    val recentMetrics: Flow<List<VmMetricSnapshotEntity>> = vmMetricDao.getAllRecentMetrics(150)

    fun getMetricsForVm(vmId: Int): Flow<List<VmMetricSnapshotEntity>> = vmMetricDao.getRecentMetricsForVm(vmId, 50)

    private val _hostNodeInfo = MutableStateFlow(KernelIsolationProber.probeHostNode())
    val hostNodeInfo: StateFlow<HostNodeInfo> = _hostNodeInfo.asStateFlow()

    val engineManager: VmEngineManager = VmEngineManager(
        scope = scope,
        onVmUpdated = { updatedVm ->
            vmDao.updateVm(updatedVm)
            scope.launch(Dispatchers.IO) {
                vmMetricDao.insertMetric(
                    VmMetricSnapshotEntity(
                        vmId = updatedVm.id,
                        vmName = updatedVm.name,
                        timestamp = System.currentTimeMillis(),
                        status = updatedVm.status,
                        cpuUsagePercent = updatedVm.cpuUsagePercent,
                        memoryUsageMb = updatedVm.memoryUsageMb,
                        memoryAllocatedMb = updatedVm.ramMb,
                        trappedHypercalls = updatedVm.trappedHypercalls
                    )
                )
                vmMetricDao.pruneOldMetrics(400)
            }
        },
        onLogEmitted = { log -> hypervisorLogDao.insertLog(log) }
    )

    private val defaultIsoImages = listOf(
        IsoImage(
            id = "iso_win11_arm64",
            name = "Windows 11 Pro ARM64 (Build 26100)",
            osFamily = "Windows",
            architecture = "ARM64-v8a",
            sizeMb = 4200,
            format = "vhdx",
            isolationFeatures = listOf("vTPM 2.0 (swtpm)", "Secure Boot EDK2", "VirtIO-Win HAL", "HVCI Core Isolation"),
            sha256 = "9b64ea3e2f50334812cd9c5123910ef92318903c70621374a5bcfe8184d08b34",
            status = "READY",
            recommendedRamMb = 2048,
            recommendedCpu = 4,
            requiresUefi = true,
            requiresTpm = true
        ),
        IsoImage(
            id = "iso_tiny11_arm64",
            name = "Tiny11 ARM64 Minimal Edition",
            osFamily = "Windows",
            architecture = "ARM64-v8a",
            sizeMb = 2100,
            format = "vhdx",
            isolationFeatures = listOf("Stripped Bloatware", "vTPM 2.0 Compatible", "Low-RAM Optimization", "VirtIO Drivers"),
            sha256 = "4acb6e08811802bb632128710313f898a1290bb341e3ac818816c4e09f584e20",
            status = "READY",
            recommendedRamMb = 1024,
            recommendedCpu = 2,
            requiresUefi = true,
            requiresTpm = true
        ),
        IsoImage(
            id = "iso_win_server_2025",
            name = "Windows Server 2025 Datacenter ARM64",
            osFamily = "Windows",
            architecture = "ARM64-v8a",
            sizeMb = 3800,
            format = "vhdx",
            isolationFeatures = listOf("Hyper-V Nested Virt", "vTPM 2.0", "Windows Defender Guard", "Active Directory"),
            sha256 = "72ba43cf4a1801cdfe4b5c77271981aef130283c74823190ab778401ef912ca5",
            status = "READY",
            recommendedRamMb = 2048,
            recommendedCpu = 4,
            requiresUefi = true,
            requiresTpm = true
        ),
        IsoImage(
            id = "iso_ubuntu_2404",
            name = "Ubuntu Server 24.04 LTS (Noble)",
            osFamily = "Linux",
            architecture = "ARM64",
            sizeMb = 980,
            format = "qcow2",
            isolationFeatures = listOf("pKVM Stage-2", "AppArmor 3.0", "systemd cgroups v2", "Cloud-Init VirtIO"),
            sha256 = "b7a635fc909a34bc4a0429f9393710eb1f1092e4ab8a9319e34c22997e5961e0",
            status = "VERIFIED",
            recommendedRamMb = 1024,
            recommendedCpu = 2,
            requiresUefi = true,
            requiresTpm = false
        ),
        IsoImage(
            id = "iso_debian_12",
            name = "Debian 12 Bookworm Minimal Cloud",
            osFamily = "Linux",
            architecture = "ARM64",
            sizeMb = 240,
            format = "qcow2",
            isolationFeatures = listOf("Namespaces Clone", "Seccomp Strict", "Private RootFS", "CGroups v2"),
            sha256 = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
            status = "READY",
            recommendedRamMb = 512,
            recommendedCpu = 1,
            requiresUefi = false,
            requiresTpm = false
        ),
        IsoImage(
            id = "iso_arch_arm",
            name = "Arch Linux ARM Hypervisor Edition",
            osFamily = "Linux",
            architecture = "ARM64",
            sizeMb = 410,
            format = "raw",
            isolationFeatures = listOf("Rolling 6.8 Kernel", "Systemd-boot", "Btrfs Subvolumes", "Zram Swapping"),
            sha256 = "e1136b87aa78990145229ef58b293309a45612c8b8123849aa0182837bcde201",
            status = "READY",
            recommendedRamMb = 1024,
            recommendedCpu = 2,
            requiresUefi = true,
            requiresTpm = false
        ),
        IsoImage(
            id = "iso_alpine_hardened",
            name = "Alpine Linux 3.20 Hardened Kernel",
            osFamily = "Linux",
            architecture = "ARM64 / aarch64",
            sizeMb = 96,
            format = "raw",
            isolationFeatures = listOf("PaX/Grsecurity", "Stack-Clash Protect", "KASLR Active", "No Root SSH"),
            sha256 = "7d1a54127b222502f5b79b5fb0803061152a44f92b37e23c65dd004148324160",
            status = "READY",
            recommendedRamMb = 256,
            recommendedCpu = 1,
            requiresUefi = false,
            requiresTpm = false
        ),
        IsoImage(
            id = "iso_microdroid_arm64",
            name = "Microdroid Android Minimal (AVF)",
            osFamily = "Android",
            architecture = "ARM64-v8a",
            sizeMb = 184,
            format = "microdroid-sparse",
            isolationFeatures = listOf("pKVM Stage-2", "Seccomp Strict", "Encrypted Payloads", "Read-Only RootFS"),
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            status = "VERIFIED",
            recommendedRamMb = 512,
            recommendedCpu = 2,
            requiresUefi = false,
            requiresTpm = false
        )
    )

    private val _isoImages = MutableStateFlow(defaultIsoImages)
    val isoImages: StateFlow<List<IsoImage>> = _isoImages.asStateFlow()

    init {
        scope.launch {
            seedDatabaseIfEmpty()
        }
    }

    suspend fun addCustomIsoImage(image: IsoImage) = withContext(Dispatchers.IO) {
        val current = _isoImages.value.toMutableList()
        current.add(0, image)
        _isoImages.value = current
        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = null,
                level = "INFO",
                tag = "STORAGE",
                message = "Registered custom OS image [${image.name}] (${image.osFamily} ${image.architecture})"
            )
        )
    }

    suspend fun getVm(id: Int): VmEntity? = withContext(Dispatchers.IO) {
        vmDao.getVmById(id)
    }

    private suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val currentVms = vmDao.getAllVms().first()
        if (currentVms.isEmpty()) {
            val initialVms = listOf(
                VmEntity(
                    id = 100,
                    name = "ubuntu-srv-2404",
                    osType = "Ubuntu Server 24.04 LTS",
                    osFamily = "LINUX",
                    architecture = "ARM64",
                    firmware = "UEFI (EDK2 with Secure Boot)",
                    hasTpm = false,
                    hasVirtIoDrivers = true,
                    displayMode = "VIRTIO_GPU",
                    diskSizeGb = 32,
                    vCpuCount = 2,
                    ramMb = 1024,
                    status = "RUNNING",
                    isProtectedPkvm = true,
                    selinuxMode = "ENFORCING",
                    seccompLevel = "STRICT_BPF",
                    namespaceIsolation = "FULL_CLONE",
                    capabilitiesStripped = true,
                    uptimeSeconds = 1420,
                    cpuUsagePercent = 8.4f,
                    memoryUsageMb = 412,
                    trappedHypercalls = 3200,
                    pageFaults = 12,
                    lastLogLine = "Ubuntu cloud-init complete (ttyS0 / virtio-gpu active)"
                ),
                VmEntity(
                    id = 101,
                    name = "win11-arm64-pro",
                    osType = "Windows 11 Pro ARM64",
                    osFamily = "WINDOWS",
                    architecture = "ARM64",
                    firmware = "UEFI (EDK2 with Secure Boot)",
                    hasTpm = true,
                    hasVirtIoDrivers = true,
                    displayMode = "VIRTIO_GPU",
                    diskSizeGb = 64,
                    vCpuCount = 4,
                    ramMb = 2048,
                    status = "RUNNING",
                    isProtectedPkvm = true,
                    selinuxMode = "ENFORCING",
                    seccompLevel = "STRICT_BPF",
                    namespaceIsolation = "FULL_CLONE",
                    capabilitiesStripped = true,
                    uptimeSeconds = 890,
                    cpuUsagePercent = 16.5f,
                    memoryUsageMb = 1180,
                    trappedHypercalls = 4150,
                    pageFaults = 24,
                    lastLogLine = "Windows Logon active; VirtIO-GPU desktop ready (1920x1080)"
                ),
                VmEntity(
                    id = 102,
                    name = "tiny11-sandbox",
                    osType = "Tiny11 ARM64 Minimal",
                    osFamily = "WINDOWS",
                    architecture = "ARM64",
                    firmware = "UEFI (EDK2 with Secure Boot)",
                    hasTpm = true,
                    hasVirtIoDrivers = true,
                    displayMode = "VIRTIO_GPU",
                    diskSizeGb = 32,
                    vCpuCount = 2,
                    ramMb = 1024,
                    status = "STOPPED",
                    isProtectedPkvm = true,
                    selinuxMode = "ENFORCING",
                    seccompLevel = "STRICT_BPF",
                    namespaceIsolation = "FULL_CLONE",
                    capabilitiesStripped = true,
                    uptimeSeconds = 0,
                    cpuUsagePercent = 0f,
                    memoryUsageMb = 0,
                    trappedHypercalls = 0,
                    pageFaults = 0,
                    lastLogLine = "Windows VM stopped"
                ),
                VmEntity(
                    id = 103,
                    name = "microdroid-avf-01",
                    osType = "Microdroid Android Minimal",
                    osFamily = "ANDROID",
                    architecture = "ARM64",
                    firmware = "Direct Kernel Boot (vmlinux)",
                    hasTpm = false,
                    hasVirtIoDrivers = true,
                    displayMode = "SERIAL_CONSOLE",
                    diskSizeGb = 8,
                    vCpuCount = 2,
                    ramMb = 512,
                    status = "RUNNING",
                    isProtectedPkvm = true,
                    selinuxMode = "ENFORCING",
                    seccompLevel = "STRICT_BPF",
                    namespaceIsolation = "FULL_CLONE",
                    capabilitiesStripped = true,
                    uptimeSeconds = 340,
                    cpuUsagePercent = 5.2f,
                    memoryUsageMb = 190,
                    trappedHypercalls = 1200,
                    pageFaults = 6,
                    lastLogLine = "Stage-2 translation active (EL2 protected)"
                ),
                VmEntity(
                    id = 104,
                    name = "alpine-hardened-test",
                    osType = "Alpine Linux 3.20 Hardened",
                    osFamily = "LINUX",
                    architecture = "ARM64",
                    firmware = "Direct Kernel Boot (vmlinux)",
                    hasTpm = false,
                    hasVirtIoDrivers = true,
                    displayMode = "SERIAL_CONSOLE",
                    diskSizeGb = 4,
                    vCpuCount = 1,
                    ramMb = 256,
                    status = "STOPPED",
                    isProtectedPkvm = true,
                    selinuxMode = "ENFORCING",
                    seccompLevel = "STRICT_BPF",
                    namespaceIsolation = "FULL_CLONE",
                    capabilitiesStripped = true,
                    uptimeSeconds = 0,
                    cpuUsagePercent = 0f,
                    memoryUsageMb = 0,
                    trappedHypercalls = 0,
                    pageFaults = 0,
                    lastLogLine = "Guest halted cleanly"
                )
            )
            vmDao.insertVms(initialVms)

            // Auto start active VMs for immediate live telemetry
            engineManager.startVm(initialVms[0]) // Ubuntu Linux
            engineManager.startVm(initialVms[1]) // Windows 11 Pro
            engineManager.startVm(initialVms[3]) // Microdroid
        }

        val currentTests = isolationTestDao.getAllTests().first()
        if (currentTests.isEmpty()) {
            val tests = KernelIsolationProber.getInitialTests()
            isolationTestDao.insertTests(tests)
        }

        // Seed initial historical VM telemetry snapshots if empty
        val currentMetrics = vmMetricDao.getAllRecentMetrics(1).first()
        if (currentMetrics.isEmpty()) {
            val now = System.currentTimeMillis()
            val initialMetrics = mutableListOf<VmMetricSnapshotEntity>()
            for (i in 14 downTo 0) {
                val t = now - (i * 10_000L)
                // VM 100: Ubuntu Server
                initialMetrics.add(
                    VmMetricSnapshotEntity(
                        vmId = 100,
                        vmName = "ubuntu-srv-2404",
                        timestamp = t,
                        status = "RUNNING",
                        cpuUsagePercent = (9.5f + kotlin.math.sin(i * 0.45f).toFloat() * 5.2f + (i % 3)).coerceIn(3f, 45f),
                        memoryUsageMb = 412 + (i * 3) + (if (i % 2 == 0) 14 else -8),
                        memoryAllocatedMb = 1024,
                        trappedHypercalls = 3200L - (i * 40)
                    )
                )
                // VM 101: Windows 11 Pro ARM64
                initialMetrics.add(
                    VmMetricSnapshotEntity(
                        vmId = 101,
                        vmName = "win11-arm64-pro",
                        timestamp = t,
                        status = "RUNNING",
                        cpuUsagePercent = (17.5f + kotlin.math.cos(i * 0.65f).toFloat() * 7.0f + (i % 4)).coerceIn(5f, 75f),
                        memoryUsageMb = 1180 + (i * 5) + (if (i % 3 == 0) 22 else -12),
                        memoryAllocatedMb = 2048,
                        trappedHypercalls = 4150L - (i * 55)
                    )
                )
                // VM 102: Tiny11 Sandbox (STOPPED)
                initialMetrics.add(
                    VmMetricSnapshotEntity(
                        vmId = 102,
                        vmName = "tiny11-sandbox",
                        timestamp = t,
                        status = "STOPPED",
                        cpuUsagePercent = 0f,
                        memoryUsageMb = 0,
                        memoryAllocatedMb = 1024,
                        trappedHypercalls = 0L
                    )
                )
                // VM 103: Microdroid Sandbox (RUNNING)
                initialMetrics.add(
                    VmMetricSnapshotEntity(
                        vmId = 103,
                        vmName = "microdroid-sec-sandbox",
                        timestamp = t,
                        status = "RUNNING",
                        cpuUsagePercent = (5.2f + kotlin.math.sin(i * 0.8f).toFloat() * 2.8f).coerceIn(1.5f, 25f),
                        memoryUsageMb = 190 + (i * 2),
                        memoryAllocatedMb = 512,
                        trappedHypercalls = 1200L - (i * 25)
                    )
                )
                // VM 104: Alpine Hardened (STOPPED)
                initialMetrics.add(
                    VmMetricSnapshotEntity(
                        vmId = 104,
                        vmName = "alpine-hardened-test",
                        timestamp = t,
                        status = "STOPPED",
                        cpuUsagePercent = 0f,
                        memoryUsageMb = 0,
                        memoryAllocatedMb = 256,
                        trappedHypercalls = 0L
                    )
                )
            }
            vmMetricDao.insertMetrics(initialMetrics)
        }

        // Initial system hypervisor log
        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = null,
                level = "INFO",
                tag = "HYPERVISOR",
                message = "Proxmox Mobile Hypervisor daemon initialized on ${KernelIsolationProber.probeHostNode().nodeName}"
            )
        )
    }

    suspend fun refreshHostNodeInfo() = withContext(Dispatchers.IO) {
        val probed = KernelIsolationProber.probeHostNode()
        _hostNodeInfo.value = probed
    }

    suspend fun runSingleTest(testId: String): IsolationTestEntity = withContext(Dispatchers.IO) {
        val existing = isolationTestDao.getTestById(testId)
        if (existing != null) {
            isolationTestDao.updateTest(existing.copy(status = "RUNNING"))
        }
        val result = KernelIsolationProber.executeTest(testId)
        isolationTestDao.updateTest(result)
        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = null,
                level = if (result.status == "PASSED") "INFO" else "WARN",
                tag = "AUDIT",
                message = "Isolation test [${result.title}]: ${result.status} (Score: ${result.score}%)"
            )
        )
        result
    }

    suspend fun runAllTests(): List<IsolationTestEntity> = withContext(Dispatchers.IO) {
        val tests = isolationTestDao.getAllTests().first()
        val results = mutableListOf<IsolationTestEntity>()
        for (test in tests) {
            isolationTestDao.updateTest(test.copy(status = "RUNNING"))
            val res = KernelIsolationProber.executeTest(test.id)
            isolationTestDao.updateTest(res)
            results.add(res)
        }
        val avgScore = if (results.isNotEmpty()) results.map { it.score }.average().toInt() else 85
        val updatedHost = _hostNodeInfo.value.copy(isolationScore = avgScore)
        _hostNodeInfo.value = updatedHost

        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = null,
                level = "INFO",
                tag = "AUDIT-SUITE",
                message = "Full kernel isolation test suite completed. Composite Score: $avgScore/100"
            )
        )
        results
    }

    suspend fun createVm(
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
    ): VmEntity = withContext(Dispatchers.IO) {
        val vms = vmDao.getAllVms().first()
        val maxId = vms.maxOfOrNull { it.id } ?: 99
        val newId = maxId + 1

        val newVm = VmEntity(
            id = newId,
            name = name,
            osType = osType,
            osFamily = osFamily,
            architecture = architecture,
            firmware = firmware,
            hasTpm = hasTpm,
            hasVirtIoDrivers = hasVirtIoDrivers,
            displayMode = displayMode,
            diskSizeGb = diskSizeGb,
            vCpuCount = vCpu,
            ramMb = ramMb,
            status = "STOPPED",
            isProtectedPkvm = isProtectedPkvm,
            selinuxMode = selinux,
            seccompLevel = seccomp,
            namespaceIsolation = "FULL_CLONE",
            capabilitiesStripped = true,
            uptimeSeconds = 0,
            cpuUsagePercent = 0f,
            memoryUsageMb = 0,
            trappedHypercalls = 0,
            pageFaults = 0,
            lastLogLine = "VM created and allocated ($osFamily $architecture)"
        )
        vmDao.insertVm(newVm)

        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = newId,
                level = "INFO",
                tag = "CONFIG",
                message = "Created $osFamily VM $newId [$name] ($vCpu vCPU, $ramMb MB RAM, TPM=$hasTpm, UEFI=${firmware.contains("UEFI")})"
            )
        )
        newVm
    }

    suspend fun startVm(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.startVm(vm)
    }

    suspend fun stopVm(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.stopVm(vm)
    }

    suspend fun pauseVm(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.pauseVm(vm)
    }

    suspend fun forceResetVm(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.forceResetVm(vm)
        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = id,
                level = "WARN",
                tag = "RESET",
                message = "Operator triggered hard force-reset for VM $id [${vm.name}]"
            )
        )
    }

    suspend fun triggerFault(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.triggerFaultInjection(vm)
    }

    fun toggleLoadSpike(id: Int, enabled: Boolean) {
        engineManager.toggleLoadSpike(id, enabled)
    }

    fun isLoadSpikeActive(id: Int): Boolean = engineManager.isLoadSpikeActive(id)

    suspend fun deleteVm(id: Int) = withContext(Dispatchers.IO) {
        val vm = vmDao.getVmById(id) ?: return@withContext
        engineManager.stopVm(vm)
        vmDao.deleteVmById(id)
        vmMetricDao.deleteMetricsForVm(id)
        hypervisorLogDao.insertLog(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = id,
                level = "WARN",
                tag = "CONFIG",
                message = "Deleted VM $id [${vm.name}]"
            )
        )
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        hypervisorLogDao.clearAllLogs()
    }
}
