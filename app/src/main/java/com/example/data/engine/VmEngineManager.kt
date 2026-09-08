package com.example.data.engine

import com.example.data.model.HypervisorLogEntity
import com.example.data.model.VmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class VmEngineManager(
    private val scope: CoroutineScope,
    private val onVmUpdated: suspend (VmEntity) -> Unit,
    private val onLogEmitted: suspend (HypervisorLogEntity) -> Unit
) {
    private val activeVmJobs = mutableMapOf<Int, Job>()
    private val _consoleOutputFlow = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 64)
    val consoleOutputFlow: SharedFlow<Pair<Int, String>> = _consoleOutputFlow.asSharedFlow()

    fun startVm(vm: VmEntity) {
        if (activeVmJobs.containsKey(vm.id)) return

        val job = scope.launch(Dispatchers.Default) {
            var currentUptime = vm.uptimeSeconds
            var hypercalls = vm.trappedHypercalls
            var faults = vm.pageFaults

            // Initial boot sequence log based on OS family
            val bootLogs = when (vm.osFamily.uppercase()) {
                "WINDOWS" -> listOf(
                    "pkvm: [VM ${vm.id}] Initializing vCPU 0..${vm.vCpuCount - 1} with TianoCore EDK2 UEFI (ARM64)",
                    "uefi: [VM ${vm.id}] Secure Boot active (Microsoft Production PCA 2011 enrolled)",
                    "tpm: [VM ${vm.id}] Virtual TPM 2.0 (swtpm) initialized: SHA256 PCR banks armed",
                    "acpi: [VM ${vm.id}] Emitted ACPI 6.4 DSDT/MADT/FADT tables for ARM64 Windows HAL",
                    "bootmgr: [VM ${vm.id}] EFI\\Microsoft\\Boot\\bootmgr.efi loaded from virtual NVMe (disk 0)",
                    "ntoskrnl: [VM ${vm.id}] Windows NT 10.0 Kernel (Build 26100) boot phase complete",
                    "virtio-win: [VM ${vm.id}] Viostor.sys (storage) and Netkvm.sys (network) attached",
                    "winlogon: [VM ${vm.id}] Windows Logon initialized. VirtIO-GPU desktop ready (1920x1080)"
                )
                "LINUX" -> listOf(
                    "pkvm: [VM ${vm.id}] Initializing vCPU 0..${vm.vCpuCount - 1} with Stage-2 memory mapping (${vm.ramMb} MB)",
                    "uefi: [VM ${vm.id}] Loading Linux 6.8 ARM64 kernel with initramfs",
                    "seccomp: [VM ${vm.id}] BPF filter profile enforced: ${vm.seccompLevel}",
                    "kernel: [VM ${vm.id}] Linux version 6.8.0-31-generic (build@hyperdroid) aarch64 ready",
                    "systemd[1]: [VM ${vm.id}] Detected virtualization 'kvm' under pKVM EL2 stage-2 realm",
                    "virtio_net: [VM ${vm.id}] Link up eth0 10.0.2.15/24 (virtio-pci interface)",
                    "cloud-init: [VM ${vm.id}] Reached target multi-user.target (Guest OS ready)"
                )
                else -> listOf(
                    "pkvm: [VM ${vm.id}] Initializing vCPU 0..${vm.vCpuCount - 1} with Stage-2 memory mapping (${vm.ramMb} MB)",
                    "pkvm: [VM ${vm.id}] IPA range [0x80000000 - 0x${Integer.toHexString(0x80000000.toInt() + (vm.ramMb * 1024 * 1024))}] locked",
                    "seccomp: [VM ${vm.id}] BPF filter profile loaded: ${vm.seccompLevel}",
                    "selinux: [VM ${vm.id}] Context established: u:r:microdroid_app:s0",
                    "kernel: [VM ${vm.id}] Microdroid Android minimal kernel 6.6-arm64 booted successfully"
                )
            }

            for (log in bootLogs) {
                emitLog(vm.id, "INFO", if (vm.osFamily.uppercase() == "WINDOWS") "WIN-BOOT" else "BOOT", log)
                delay(250)
            }

            val defaultLastLog = when (vm.osFamily.uppercase()) {
                "WINDOWS" -> "Windows 11 desktop operational (VirtIO-GPU 1920x1080 active)"
                "LINUX" -> "Linux guest kernel operational (multi-user.target reached)"
                else -> "Guest kernel operational (Stage-2 protected)"
            }

            var updatedVm = vm.copy(
                status = "RUNNING",
                lastLogLine = defaultLastLog,
                uptimeSeconds = currentUptime
            )
            onVmUpdated(updatedVm)

            // Runtime loop
            while (isActive) {
                delay(2000)
                currentUptime += 2
                val deltaHypercalls = Random.nextLong(20, 75)
                hypercalls += deltaHypercalls
                val deltaFaults = Random.nextLong(0, 4)
                faults += deltaFaults

                val cpu = (Random.nextFloat() * 15f + (vm.vCpuCount * 4f)).coerceIn(3f, 85f)
                val ramMultiplier = if (vm.osFamily.uppercase() == "WINDOWS") 0.55f else 0.38f
                val ramUsed = (vm.ramMb * (ramMultiplier + Random.nextFloat() * 0.08f)).toInt()

                val trapType = if (vm.osFamily.uppercase() == "WINDOWS") "ACPI/MMIO" else "HVC"
                updatedVm = updatedVm.copy(
                    uptimeSeconds = currentUptime,
                    trappedHypercalls = hypercalls,
                    pageFaults = faults,
                    cpuUsagePercent = String.format("%.1f", cpu).toFloat(),
                    memoryUsageMb = ramUsed,
                    lastLogLine = "vCPU trap: $trapType 0x${Integer.toHexString(Random.nextInt(0x10, 0xFF))} handled (${deltaHypercalls} calls/s)"
                )
                onVmUpdated(updatedVm)

                // Periodic telemetry log
                if (Random.nextInt(5) == 0) {
                    val tag = if (vm.osFamily.uppercase() == "WINDOWS") "WIN-HYPER-V" else "pKVM-EL2"
                    emitLog(
                        vm.id,
                        "HYPERCALL",
                        tag,
                        "VM ${vm.id} [${vm.name}]: $trapType exit 0x${Integer.toHexString(Random.nextInt(0x8000, 0x9000))} trapped & emulated in 24ns"
                    )
                }
            }
        }
        activeVmJobs[vm.id] = job
    }

    fun stopVm(vm: VmEntity) {
        activeVmJobs[vm.id]?.cancel()
        activeVmJobs.remove(vm.id)
        scope.launch {
            emitLog(vm.id, "INFO", "HALT", "VM ${vm.id} [${vm.name}] sent ACPI shutdown signal and unmapped Stage-2 frames")
            onVmUpdated(
                vm.copy(
                    status = "STOPPED",
                    cpuUsagePercent = 0f,
                    memoryUsageMb = 0,
                    lastLogLine = "Virtual machine halted cleanly"
                )
            )
        }
    }

    fun pauseVm(vm: VmEntity) {
        val currentJob = activeVmJobs.remove(vm.id)
        currentJob?.cancel()
        scope.launch {
            emitLog(vm.id, "WARN", "VCPU", "VM ${vm.id} vCPU clocks paused; MMU state frozen")
            onVmUpdated(
                vm.copy(
                    status = "PAUSED",
                    cpuUsagePercent = 0f,
                    lastLogLine = "Execution paused by operator"
                )
            )
        }
    }

    fun triggerFaultInjection(vm: VmEntity) {
        scope.launch {
            emitLog(
                vm.id,
                "ISOLATION_VIOLATION",
                "SECURITY-TRAP",
                "CRITICAL: VM ${vm.id} attempted unauthorized host memory write @ 0xFFFFFFC008000000 -> INTERCEPTED BY pKVM STAGE-2 MMU!"
            )
            emitLog(
                vm.id,
                "WARN",
                "SECCOMP",
                "VM ${vm.id} issued restricted syscall sys_bpf(BPF_PROG_LOAD) -> Denied by Seccomp (EPERM)"
            )
            onVmUpdated(
                vm.copy(
                    pageFaults = vm.pageFaults + 1,
                    lastLogLine = "Isolation violation intercepted and contained"
                )
            )
            _consoleOutputFlow.emit(
                vm.id to "[ALERT] Kernel isolation boundary trap caught unauthorized MMIO escape attempt!\n[SECCOMP] Syscall bpf blocked with SECCOMP_RET_ERRNO(1)\n"
            )
        }
    }

    suspend fun executeConsoleCommand(vmId: Int, command: String, vm: VmEntity? = null): String {
        val trimmed = command.trim()
        val isWindows = vm?.osFamily?.uppercase() == "WINDOWS" || vm?.osType?.contains("Windows", ignoreCase = true) == true
        val promptPrefix = if (isWindows) "PS C:\\Windows\\system32> " else "root@${vm?.name ?: "guest"}:~# "

        val response = if (isWindows) {
            when (trimmed.lowercase()) {
                "help", "get-help" -> """
                    Windows PowerShell / Hypervisor Command Subsystem:
                      systeminfo       - Query Windows OS build, HAL, RAM, and BIOS info
                      winver           - Display Windows version and license build
                      get-process      - List active NT kernel processes (services, explorer, virtio)
                      tasklist         - Standard Windows task list
                      get-service      - List Windows services (VirtIO, BitLocker, Hyper-V)
                      ipconfig         - Display VirtIO-Net ARM64 network adapter IP configuration
                      ipconfig /all    - Full Ethernet adapter details & DNS servers
                      tpm.msc, get-tpm - Verify hardware vTPM 2.0 security root-of-trust
                      wmic cpu         - Query virtualized ARM64 processor cores
                      dir, ls          - Directory listing of C:\
                      status           - Report hypervisor Stage-2 pKVM confinement
                      trigger-fault    - Test EL2 hypervisor trap against unauthorized MMIO access
                      cls, clear       - Clear terminal window
                """.trimIndent()

                "systeminfo" -> """
                    Host Name:                 ${(vm?.name ?: "WIN11-ARM64").uppercase()}
                    OS Name:                   Microsoft Windows 11 Pro on ARM
                    OS Version:                10.0.26100 N/A Build 26100.1742
                    OS Manufacturer:           Microsoft Corporation
                    OS Configuration:          Stand-Alone Workstation
                    OS Build Type:             Multiprocessor Free (${vm?.vCpuCount ?: 4} Cores)
                    Registered Owner:          HyperDriod Hypervisor
                    System Manufacturer:       Proxmox / Android Virtualization Framework (pKVM)
                    System Model:              pKVM ARM64 Virtual Machine
                    System Type:               ARM64-based PC
                    Processor(s):              1 Processor(s) Installed.
                                               [01]: ARMv8 Family 8 Model 0 Revision 0 ~2840 Mhz
                    BIOS Version:              TianoCore EDK II UEFI 2.7.0 (UEFI 2.80, 06/24/2024)
                    Secure Boot State:         Enabled (Hardware Root of Trust)
                    Windows Directory:         C:\Windows
                    Total Physical Memory:     ${vm?.ramMb ?: 2048} MB
                    Available Physical Memory: ${((vm?.ramMb ?: 2048) * 0.65).toInt()} MB
                    Virtual Memory: Max Size:  ${(vm?.ramMb ?: 2048) * 2} MB
                    Hyper-V Requirements:      VM Monitor Mode Extensions: Yes
                                               Virtualization Enabled In Firmware: Yes
                                               Second Level Address Translation (SLAT): Yes (pKVM Stage-2)
                                               Data Execution Prevention (DEP): Yes
                """.trimIndent()

                "winver" -> """
                    Microsoft Windows [Version 10.0.26100.1742]
                    (c) Microsoft Corporation. All rights reserved.
                    Windows 11 Professional ARM64 Edition
                    HyperDriod pKVM Mobile Hypervisor Guest Environment
                """.trimIndent()

                "get-process", "tasklist", "ps" -> """
                    Handles  NPM(K)    PM(K)      WS(K)     CPU(s)     Id ProcessName
                    -------  ------    -----      -----     ------     -- -----------
                        412      18     3210       8450       0.42      4 System (ntoskrnl.exe)
                        128       6     1040       2100       0.05    180 smss.exe
                        210      11     2150       4890       0.12    320 csrss.exe
                        160       8     1820       3910       0.08    412 wininit.exe
                        290      14     4210       9200       0.34    520 services.exe
                        310      15     5120      11400       0.45    580 lsass.exe
                        480      22     8410      18200       0.82    720 svchost.exe (RPCSS)
                        380      18     6200      14100       0.61    840 svchost.exe (DcomLaunch)
                        512      24    12400      24500       1.20   1120 explorer.exe
                        190      10     3100       7800       0.15   1440 virtio-win-service.exe
                        280      16     7800      16900       0.74   1680 powershell.exe
                """.trimIndent()

                "get-service" -> """
                    Status   Name               DisplayName
                    ------   ----               -----------
                    Running  CryptSvc           Cryptographic Services
                    Running  Dhcp               DHCP Client
                    Running  Dnscache           DNS Client
                    Running  EventLog           Windows Event Log
                    Running  LanmanWorkstation  Workstation
                    Running  PlugPlay           Plug and Play
                    Running  RpcSs              Remote Procedure Call (RPC)
                    Running  VioStor            Red Hat VirtIO SCSI/Block Driver
                    Running  VioNet             Red Hat VirtIO Network Adapter
                    Running  WinDefend          Microsoft Defender Antivirus Service
                """.trimIndent()

                "ipconfig", "ipconfig /all" -> """
                    Windows IP Configuration

                    Ethernet adapter VirtIO-Net Ethernet:
                       Connection-specific DNS Suffix  . : guest.hyperdroid.internal
                       Description . . . . . . . . . . . : Red Hat VirtIO Ethernet Adapter (ARM64)
                       Physical Address. . . . . . . . . : 52-54-00-12-34-AD
                       DHCP Enabled. . . . . . . . . . . : Yes
                       IPv4 Address. . . . . . . . . . . : 10.0.2.15(Preferred)
                       Subnet Mask . . . . . . . . . . . : 255.255.255.0
                       Default Gateway . . . . . . . . . : 10.0.2.2
                       DNS Servers . . . . . . . . . . . : 10.0.2.3, 1.1.1.1
                       NetBIOS over Tcpip. . . . . . . . : Enabled
                """.trimIndent()

                "tpm.msc", "get-tpm" -> """
                    [TPM Management on Local Computer]
                    Status: The TPM is ready for use.
                    TPM Specification Version: 2.0 (Family: 2.0, Level: 0, Revision: 1.59)
                    Manufacturer Name: SWTPM-pKVM (SWTP)
                    Manufacturer Version: 0.9.1.0
                    Attestation: Ready
                    Storage: Ready
                    PCR Banks Active: SHA1, SHA256 (BitLocker Pre-Boot Measurement Valid)
                """.trimIndent()

                "wmic cpu", "wmic cpu get name,numberofcores" -> """
                    Name                                             NumberOfCores
                    ARMv8 Processor Rev 0 (HyperDriod pKVM vCPU)     ${vm?.vCpuCount ?: 4}
                """.trimIndent()

                "dir", "dir c:\\", "ls" -> """
                     Volume in drive C is WINDOWS_OS
                     Volume Serial Number is 4C29-87FA

                     Directory of C:\

                    06/24/2024  08:14 AM    <DIR>          PerfLogs
                    06/24/2024  09:30 AM    <DIR>          Program Files
                    06/24/2024  09:30 AM    <DIR>          Program Files (x86)
                    06/24/2024  09:35 AM    <DIR>          Users
                    06/24/2024  10:02 AM    <DIR>          Windows
                    06/24/2024  10:15 AM    <DIR>          VirtIO-Win-0.1.240
                                   0 File(s)              0 bytes
                                   6 Dir(s)  42,854,121,472 bytes free
                """.trimIndent()

                "status" -> """
                    VM ID: $vmId (${vm?.name ?: "Windows Guest"})
                    OS Family: WINDOWS (Windows 11 Pro ARM64)
                    Firmware: TianoCore EDK2 UEFI (Secure Boot: Active)
                    Virtual TPM: 2.0 (Hardware swtpm emulation)
                    Stage-2 Translation: Hardware Enforced (pKVM EL2 IPA Locked)
                    VirtIO Bus: Storage (viostor), Net (netkvm), GPU (viogpu)
                """.trimIndent()

                "trigger-fault" -> {
                    emitLog(
                        vmId,
                        "ISOLATION_VIOLATION",
                        "STAGE2-TRAP",
                        "VM $vmId [Windows 11] attempted unauthorized MMIO write -> Trapped by EL2 Hypervisor"
                    )
                    """
                        [FAULT INJECTION TEST - WINDOWS GUEST]
                        Attempting out-of-bounds guest memory probe at 0xFFFFFFFFFF000000...
                        Hypervisor Response: Stage-2 Data Abort trapped at EL2.
                        Memory Isolation: SECURE. No host leakage detected.
                    """.trimIndent()
                }

                "cls", "clear" -> "__CLEAR__"

                else -> "The term '$command' is not recognized as the name of a cmdlet, function, script file, or operable program. Type 'help' for available commands."
            }
        } else {
            when (trimmed.lowercase()) {
                "help" -> """
                    Available Hypervisor Guest Console Commands:
                      uname -a         - Print guest virtual kernel information
                      top, htop        - Monitor guest processes and CPU load
                      free -h          - Display total, used, and free RAM
                      ip a             - Show network interfaces and IP addresses
                      systemctl status - Query systemd init state and services
                      dmesg            - Dump recent guest kernel dmesg ring buffer
                      cat /proc/isolation - Query active kernel isolation attributes
                      status           - Print current vCPU, memory, and Stage-2 MMU state
                      test-seccomp     - Trigger test syscall to verify BPF confinement
                      trigger-fault    - Test Stage-2 memory escape boundary containment
                      probe-mem        - Audit guest physical address range mapping
                      clear            - Clear terminal buffer
                """.trimIndent()

                "uname -a" -> "Linux ${vm?.name ?: "ubuntu-guest"} 6.8.0-31-generic #31-Ubuntu SMP PREEMPT_DYNAMIC aarch64 GNU/Linux"

                "free -h", "free -m" -> """
                                   total        used        free      shared  buff/cache   available
                    Mem:           ${vm?.ramMb ?: 1024}Mi       ${((vm?.ramMb ?: 1024) * 0.38).toInt()}Mi       ${((vm?.ramMb ?: 1024) * 0.42).toInt()}Mi       8.0Mi       ${((vm?.ramMb ?: 1024) * 0.20).toInt()}Mi       ${((vm?.ramMb ?: 1024) * 0.60).toInt()}Mi
                    Swap:          2048Mi          0Mi      2048Mi
                """.trimIndent()

                "ip a", "ip addr", "ifconfig" -> """
                    1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
                        inet 127.0.0.1/8 scope host lo
                           valid_lft forever preferred_lft forever
                    2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP group default qlen 1000
                        link/ether 52:54:00:ab:cd:ef brd ff:ff:ff:ff:ff:ff
                        inet 10.0.2.15/24 metric 100 brd 10.0.2.255 scope global dynamic eth0
                           valid_lft 86240sec preferred_lft 86240sec
                """.trimIndent()

                "systemctl status", "systemctl" -> """
                    * ${vm?.name ?: "ubuntu-guest"}
                        State: running
                         Jobs: 0 queued
                       Failed: 0 units
                        Since: Wed 2024-06-24 10:00:00 UTC; 24min ago
                       CGroup: /
                               |-init.scope
                               | `-1 /sbin/init
                               `-system.slice
                                 |-systemd-journald.service
                                 |-systemd-networkd.service
                                 |-systemd-resolved.service
                                 |-qemu-guest-agent.service
                                 `-ssh.service
                """.trimIndent()

                "top", "htop", "ps aux", "ps" -> """
                    USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
                    root           1  0.0  0.8 168420 12800 ?        Ss   10:00   0:01 /sbin/init
                    root         240  0.0  0.6  89200  9400 ?        Ss   10:00   0:00 /lib/systemd/systemd-journald
                    root         312  0.0  0.4  24100  6200 ?        Ss   10:00   0:00 /lib/systemd/systemd-networkd
                    root         410  0.0  0.3  14200  4800 ?        Ss   10:00   0:00 /usr/sbin/sshd -D
                    qemu         480  0.1  0.5  32400  7800 ?        S    10:00   0:00 /usr/bin/qemu-ga
                    root         620  0.2  0.8  48200 12400 pts/0    Ss+  10:05   0:00 -bash
                """.trimIndent()

                "status" -> """
                    VM ID: $vmId (${vm?.name ?: "Linux Guest"})
                    Hypervisor Architecture: ARM64-v8a / Protected KVM (EL2)
                    Stage-2 Translation: Hardware Enforced (IPA Space Locked)
                    Seccomp Filter: ACTIVE (${vm?.seccompLevel ?: "STRICT_BPF"})
                    SELinux Mode: ${vm?.selinuxMode ?: "ENFORCING"}
                    vCPU State: Active (${vm?.vCpuCount ?: 2} cores), Trap Handling Ready
                """.trimIndent()

                "dmesg" -> """
                    [    0.000000] Booting Linux on physical CPU 0x0000000000 [0x410fd034]
                    [    0.000000] Linux version 6.8.0-31-generic (build@pkvm)
                    [    0.000000] pkvm: Stage-2 translation initialized, 48-bit IPA space
                    [    0.012431] seccomp: Enforcing syscall BPF profile (whitelist mode)
                    [    0.048112] mm: Memory Tagging Extension (MTE) verified active
                    [    0.104239] virtio_pci: virtio-net, virtio-blk initialized
                    [    0.158402] systemd[1]: Reached target Isolated VM Environment
                """.trimIndent()

                "cat /proc/isolation" -> """
                    [KERNEL-LEVEL ISOLATION REPORT]
                    STAGE2_PROTECTED_PAGES: 131072
                    HOST_SNOOP_BLOCKED: YES
                    SECCOMP_BPF_MODE: 2 (FILTER)
                    DROPPED_CAPABILITIES: 0x0000000000000000 (ZERO-ROOT)
                    PID_NAMESPACE_ISOLATED: YES (Inode 4026531836)
                    MOUNT_NAMESPACE_PRIVATE: YES
                    MTE_TAG_CONFINEMENT: HARDWARE_CHECKED
                """.trimIndent()

                "test-seccomp" -> {
                    emitLog(vmId, "WARN", "SECCOMP", "VM $vmId test: syscall sys_ptrace -> intercepted by BPF filter")
                    """
                        [SECCOMP TEST RESULT]
                        Attempting syscall: sys_ptrace(PTRACE_ATTACH, 1)...
                        Return Code: -1 (Operation not permitted / EPERM)
                        Seccomp Filter Action: SECCOMP_RET_ERRNO
                        Confinement: PASSED (Syscall properly blocked)
                    """.trimIndent()
                }

                "trigger-fault" -> {
                    emitLog(
                        vmId,
                        "ISOLATION_VIOLATION",
                        "STAGE2-TRAP",
                        "VM $vmId simulated invalid memory probe -> Trapped by EL2 Hypervisor"
                    )
                    """
                        [FAULT INJECTION TEST]
                        Attempting out-of-bounds guest memory probe at 0xFFFFFFFFFF000000...
                        Hypervisor Response: Stage-2 Data Abort trapped at EL2.
                        Memory Isolation: SECURE. No host leakage detected.
                    """.trimIndent()
                }

                "probe-mem" -> """
                    [GUEST PHYSICAL MEMORY PROBE]
                    IPA Base: 0x0000000080000000
                    IPA Limit: 0x00000000A0000000
                    Host Physical Translation: Encrypted / Stage-2 Unmapped from Host
                    DMA Remapping (IOMMU): Active
                """.trimIndent()

                "clear" -> "__CLEAR__"

                else -> "bash: ${command.take(20)}: command not found (type 'help' for commands)"
            }
        }

        _consoleOutputFlow.emit(vmId to "$promptPrefix$command\n$response\n")
        return response
    }

    private suspend fun emitLog(vmId: Int?, level: String, tag: String, msg: String) {
        onLogEmitted(
            HypervisorLogEntity(
                timestamp = System.currentTimeMillis(),
                vmId = vmId,
                level = level,
                tag = tag,
                message = msg
            )
        )
    }
}
