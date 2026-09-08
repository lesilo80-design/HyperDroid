package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IsolationTestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IsolationTestDao {
    @Query("SELECT * FROM isolation_tests ORDER BY id ASC")
    fun getAllTests(): Flow<List<IsolationTestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<IsolationTestEntity>)

    @Update
    suspend fun updateTest(test: IsolationTestEntity)

    @Query("SELECT * FROM isolation_tests WHERE id = :id")
    suspend fun getTestById(id: String): IsolationTestEntity?
}
