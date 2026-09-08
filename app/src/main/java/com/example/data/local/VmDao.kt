package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VmDao {
    @Query("SELECT * FROM virtual_machines ORDER BY id ASC")
    fun getAllVms(): Flow<List<VmEntity>>

    @Query("SELECT * FROM virtual_machines WHERE id = :id")
    suspend fun getVmById(id: Int): VmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVm(vm: VmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVms(vms: List<VmEntity>)

    @Update
    suspend fun updateVm(vm: VmEntity)

    @Query("UPDATE virtual_machines SET status = :status WHERE id = :id")
    suspend fun updateVmStatus(id: Int, status: String)

    @Query("DELETE FROM virtual_machines WHERE id = :id")
    suspend fun deleteVmById(id: Int)
}
