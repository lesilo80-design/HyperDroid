package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HypervisorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HypervisorLogDao {
    @Query("SELECT * FROM hypervisor_logs ORDER BY timestamp DESC LIMIT 300")
    fun getRecentLogs(): Flow<List<HypervisorLogEntity>>

    @Query("SELECT * FROM hypervisor_logs WHERE vmId = :vmId ORDER BY timestamp DESC LIMIT 200")
    fun getLogsForVm(vmId: Int): Flow<List<HypervisorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HypervisorLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<HypervisorLogEntity>)

    @Query("DELETE FROM hypervisor_logs")
    suspend fun clearAllLogs()
}
