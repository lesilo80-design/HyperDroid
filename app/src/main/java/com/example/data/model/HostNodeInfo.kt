package com.example.data.model

data class HostNodeInfo(
    val nodeName: String = "pve-mobile-01",
    val ipAddress: String = "127.0.0.1 (lo/bridge0)",
    val kernelVersion: String,
    val osRelease: String,
    val cpuModel: String,
    val cpuArch: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val selinuxStatus: String,
    val pkvmSupported: Boolean,
    val pkvmState: String,
    val stage2Protection: Boolean,
    val seccompBpfActive: Boolean,
    val mteHardwareSupported: Boolean,
    val kptiActive: Boolean,
    val namespacesActive: List<String>,
    val isolationScore: Int = 88
)
