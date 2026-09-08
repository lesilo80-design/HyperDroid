package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VmMetricSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VmMetricDao {

    @Query("SELECT * FROM vm_metrics ORDER BY timestamp DESC LIMIT :limit")
    fun getAllRecentMetrics(limit: Int = 150): Flow<List<VmMetricSnapshotEntity>>

    @Query("SELECT * FROM vm_metrics WHERE vmId = :vmId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMetricsForVm(vmId: Int, limit: Int = 50): Flow<List<VmMetricSnapshotEntity>>

    @Query("SELECT * FROM vm_metrics ORDER BY timestamp ASC")
    fun getAllMetricsAsc(): Flow<List<VmMetricSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: VmMetricSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<VmMetricSnapshotEntity>)

    @Query("DELETE FROM vm_metrics WHERE id NOT IN (SELECT id FROM vm_metrics ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldMetrics(keepCount: Int = 400)

    @Query("DELETE FROM vm_metrics WHERE vmId = :vmId")
    suspend fun deleteMetricsForVm(vmId: Int)

    @Query("DELETE FROM vm_metrics")
    suspend fun clearAllMetrics()
}
