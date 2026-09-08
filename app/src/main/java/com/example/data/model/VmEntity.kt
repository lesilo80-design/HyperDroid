package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_machines")
data class VmEntity(
    @PrimaryKey val id: Int, // e.g. 100, 101, 102
    val name: String,
    val osType: String, // Windows 11 Pro ARM64, Ubuntu Server 24.04 LTS, Microdroid Minimal, etc.
    val osFamily: String = "LINUX", // WINDOWS, LINUX, ANDROID
    val architecture: String = "ARM64", // ARM64 (pKVM Native), x86_64 (TCG Emulated)
    val firmware: String = "UEFI (EDK2 with Secure Boot)", // UEFI (EDK2 with Secure Boot), Direct Kernel Boot (vmlinux), SeaBIOS
    val hasTpm: Boolean = false, // Virtual TPM 2.0 (swtpm emulation)
    val hasVirtIoDrivers: Boolean = true, // VirtIO guest bus (viostor, netkvm, viogpu)
    val displayMode: String = "VIRTIO_GPU", // VIRTIO_GPU, SERIAL_CONSOLE
    val diskSizeGb: Int = 32,
    val vCpuCount: Int = 2,
    val ramMb: Int = 512,
    val status: String = "STOPPED", // RUNNING, STOPPED, PAUSED, FAULTED
    val isProtectedPkvm: Boolean = true,
    val selinuxMode: String = "ENFORCING", // ENFORCING, PERMISSIVE, DISABLED
    val seccompLevel: String = "STRICT_BPF", // STRICT_BPF, AUDIT_ONLY, DISABLED
    val namespaceIsolation: String = "FULL_CLONE", // FULL_CLONE, USER_NET_ONLY, HOST_SHARED
    val capabilitiesStripped: Boolean = true,
    val uptimeSeconds: Long = 0,
    val cpuUsagePercent: Float = 0f,
    val memoryUsageMb: Int = 0,
    val trappedHypercalls: Long = 0,
    val pageFaults: Long = 0,
    val lastLogLine: String = "Guest initialized and halted"
)
