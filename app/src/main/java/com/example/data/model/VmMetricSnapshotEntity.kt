package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vm_metrics",
    indices = [
        Index(value = ["vmId"]),
        Index(value = ["timestamp"]),
        Index(value = ["vmId", "timestamp"])
    ]
)
data class VmMetricSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vmId: Int,
    val vmName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // RUNNING, STOPPED, PAUSED, FAULTED
    val cpuUsagePercent: Float = 0f,
    val memoryUsageMb: Int = 0,
    val memoryAllocatedMb: Int = 512,
    val trappedHypercalls: Long = 0
)
