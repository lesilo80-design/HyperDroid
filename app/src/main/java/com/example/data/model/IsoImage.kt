package com.example.data.model

data class IsoImage(
    val id: String,
    val name: String,
    val osFamily: String, // Windows, Linux, Android
    val architecture: String,
    val sizeMb: Int,
    val format: String, // raw, qcow2, vhdx, microdroid-sparse
    val isolationFeatures: List<String>,
    val sha256: String,
    val status: String, // INSTALLED, VERIFIED, READY
    val recommendedRamMb: Int = 1024,
    val recommendedCpu: Int = 2,
    val requiresUefi: Boolean = false,
    val requiresTpm: Boolean = false
)
