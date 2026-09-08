package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hypervisor_logs")
data class HypervisorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val vmId: Int? = null,
    val level: String = "INFO", // INFO, WARN, CRIT, HYPERCALL, ISOLATION_VIOLATION
    val tag: String = "pKVM",
    val message: String
)
