package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.HypervisorLogEntity
import com.example.data.model.IsolationTestEntity
import com.example.data.model.VmEntity
import com.example.data.model.VmMetricSnapshotEntity

@Database(
    entities = [
        VmEntity::class,
        IsolationTestEntity::class,
        HypervisorLogEntity::class,
        VmMetricSnapshotEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vmDao(): VmDao
    abstract fun isolationTestDao(): IsolationTestDao
    abstract fun hypervisorLogDao(): HypervisorLogDao
    abstract fun vmMetricDao(): VmMetricDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hypervisor_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
