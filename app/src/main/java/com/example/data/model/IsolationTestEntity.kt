package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "isolation_tests")
data class IsolationTestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // HYPERVISOR, SECCOMP, NAMESPACES, MEMORY, PRIVILEGES, LEAK_SHIELD
    val status: String = "PENDING", // PASSED, WARNING, FAILED, RUNNING, PENDING
    val score: Int = 0, // 0 - 100
    val summary: String,
    val technicalOutput: String,
    val kernelConfig: String,
    val securityImpact: String,
    val lastRunTimestamp: Long = 0L
)
