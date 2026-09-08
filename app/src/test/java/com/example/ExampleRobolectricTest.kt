package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.VmMetricSnapshotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HyperDroid", appName)
    }

    @Test
    fun `test VmMetricDao insert and query metrics`() = runBlocking {
        val dao = database.vmMetricDao()
        val now = System.currentTimeMillis()

        val sample1 = VmMetricSnapshotEntity(
            vmId = 100,
            vmName = "ubuntu-srv-2404",
            timestamp = now - 5000,
            status = "RUNNING",
            cpuUsagePercent = 14.5f,
            memoryUsageMb = 512,
            memoryAllocatedMb = 1024,
            trappedHypercalls = 120L
        )

        val sample2 = VmMetricSnapshotEntity(
            vmId = 101,
            vmName = "win11-arm64-pro",
            timestamp = now,
            status = "RUNNING",
            cpuUsagePercent = 28.2f,
            memoryUsageMb = 1200,
            memoryAllocatedMb = 2048,
            trappedHypercalls = 350L
        )

        dao.insertMetrics(listOf(sample1, sample2))

        val recent = dao.getAllRecentMetrics(10).first()
        assertEquals(2, recent.size)
        assertEquals("win11-arm64-pro", recent[0].vmName) // Ordered by timestamp DESC
        assertEquals(28.2f, recent[0].cpuUsagePercent, 0.01f)

        val vm100Metrics = dao.getRecentMetricsForVm(100, 10).first()
        assertEquals(1, vm100Metrics.size)
        assertEquals("ubuntu-srv-2404", vm100Metrics[0].vmName)
        assertEquals(512, vm100Metrics[0].memoryUsageMb)
    }
}
