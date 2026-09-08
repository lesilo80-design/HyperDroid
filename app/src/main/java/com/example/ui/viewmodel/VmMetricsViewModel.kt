package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.VmEntity
import com.example.data.model.VmMetricSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MetricViewMode {
    CPU,    // CPU Usage Percentage (0 - 100%)
    RAM,    // Memory Used vs Allocated (MB)
    STATUS  // Distribution of guest states (Running / Paused / Stopped / Faulted)
}

data class VmSeriesPoint(
    val timestamp: Long,
    val value: Float,
    val label: String
)

data class VmTelemetrySeries(
    val vmId: Int,
    val vmName: String,
    val colorHex: Long,
    val points: List<VmSeriesPoint>,
    val currentValue: Float,
    val peakValue: Float,
    val avgValue: Float,
    val isRunning: Boolean
)

data class VmMetricsUiState(
    val recentMetrics: List<VmMetricSnapshotEntity> = emptyList(),
    val vms: List<VmEntity> = emptyList(),
    val selectedMetricMode: MetricViewMode = MetricViewMode.CPU,
    val selectedVmIdFilter: Int? = null, // null means "All VMs"
    val seriesList: List<VmTelemetrySeries> = emptyList(),
    val aggregateCpuPercent: Float = 0f,
    val aggregateRamUsedMb: Int = 0,
    val aggregateRamAllocatedMb: Int = 0,
    val runningVmCount: Int = 0,
    val stoppedVmCount: Int = 0,
    val pausedVmCount: Int = 0,
    val faultedVmCount: Int = 0,
    val statusDistribution: Map<String, Int> = emptyMap(),
    val latestPerVm: Map<Int, VmMetricSnapshotEntity> = emptyMap()
)

class VmMetricsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val vmMetricDao = database.vmMetricDao()
    private val vmDao = database.vmDao()

    private val _selectedMetricMode = MutableStateFlow(MetricViewMode.CPU)
    val selectedMetricMode: StateFlow<MetricViewMode> = _selectedMetricMode.asStateFlow()

    private val _selectedVmIdFilter = MutableStateFlow<Int?>(null)
    val selectedVmIdFilter: StateFlow<Int?> = _selectedVmIdFilter.asStateFlow()

    // Distinct color palette for VM metrics
    private val vmColorPalette = listOf(
        0xFFE57000, // Proxmox Orange
        0xFF00E5FF, // Cyber Cyan
        0xFF00E676, // Terminal Green
        0xFFFFD600, // Amber Yellow
        0xFFFF5252, // Crimson Red
        0xFFB388FF, // Electric Purple
        0xFF40C4FF  // Sky Blue
    )

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    val uiState: StateFlow<VmMetricsUiState> = combine(
        vmMetricDao.getAllRecentMetrics(180),
        vmDao.getAllVms(),
        _selectedMetricMode,
        _selectedVmIdFilter
    ) { metrics, vms, mode, selectedVmId ->
        processMetricsState(metrics, vms, mode, selectedVmId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VmMetricsUiState()
    )

    init {
        // Start background telemetry poller to persist time-series snapshots in Room
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3000)
                try {
                    val activeVms = vmDao.getAllVms().first()
                    val now = System.currentTimeMillis()
                    val newSnapshots = mutableListOf<VmMetricSnapshotEntity>()

                    for (vm in activeVms) {
                        // Record metric snapshot for each VM
                        newSnapshots.add(
                            VmMetricSnapshotEntity(
                                vmId = vm.id,
                                vmName = vm.name,
                                timestamp = now,
                                status = vm.status,
                                cpuUsagePercent = vm.cpuUsagePercent,
                                memoryUsageMb = vm.memoryUsageMb,
                                memoryAllocatedMb = vm.ramMb,
                                trappedHypercalls = vm.trappedHypercalls
                            )
                        )
                    }
                    if (newSnapshots.isNotEmpty()) {
                        vmMetricDao.insertMetrics(newSnapshots)
                        vmMetricDao.pruneOldMetrics(350)
                    }
                } catch (_: Exception) {
                    // Ignore transient background errors
                }
            }
        }
    }

    private fun processMetricsState(
        metrics: List<VmMetricSnapshotEntity>,
        vms: List<VmEntity>,
        mode: MetricViewMode,
        selectedVmId: Int?
    ): VmMetricsUiState {
        val runningCount = vms.count { it.status == "RUNNING" }
        val pausedCount = vms.count { it.status == "PAUSED" }
        val stoppedCount = vms.count { it.status == "STOPPED" }
        val faultedCount = vms.count { it.status == "FAULTED" }

        val statusMap = mapOf(
            "RUNNING" to runningCount,
            "PAUSED" to pausedCount,
            "STOPPED" to stoppedCount,
            "FAULTED" to faultedCount
        )

        val runningVms = vms.filter { it.status == "RUNNING" }
        val aggCpu = if (runningVms.isNotEmpty()) {
            runningVms.map { it.cpuUsagePercent }.average().toFloat()
        } else 0f
        val aggRamUsed = runningVms.sumOf { it.memoryUsageMb }
        val aggRamAlloc = vms.sumOf { it.ramMb }

        // Find latest metric for each VM
        val latestPerVm = mutableMapOf<Int, VmMetricSnapshotEntity>()
        for (m in metrics) {
            if (!latestPerVm.containsKey(m.vmId)) {
                latestPerVm[m.vmId] = m
            }
        }

        // Filter metrics by selected VM if specified
        val filteredMetrics = if (selectedVmId != null) {
            metrics.filter { it.vmId == selectedVmId }
        } else {
            metrics
        }

        // Group by VM and create series
        val groupedByVm = filteredMetrics.groupBy { it.vmId }
        val seriesList = mutableListOf<VmTelemetrySeries>()

        var colorIndex = 0
        vms.forEach { vm ->
            if (selectedVmId == null || vm.id == selectedVmId) {
                val vmMetrics = (groupedByVm[vm.id] ?: emptyList()).sortedBy { it.timestamp }
                val color = vmColorPalette[colorIndex % vmColorPalette.size]
                colorIndex++

                val points = vmMetrics.takeLast(25).map { snapshot ->
                    val yVal = when (mode) {
                        MetricViewMode.CPU -> snapshot.cpuUsagePercent
                        MetricViewMode.RAM -> snapshot.memoryUsageMb.toFloat()
                        MetricViewMode.STATUS -> if (snapshot.status == "RUNNING") 100f else if (snapshot.status == "PAUSED") 50f else 0f
                    }
                    VmSeriesPoint(
                        timestamp = snapshot.timestamp,
                        value = yVal,
                        label = timeFormatter.format(Date(snapshot.timestamp))
                    )
                }

                val currentVal = points.lastOrNull()?.value ?: 0f
                val peakVal = points.maxOfOrNull { it.value } ?: 0f
                val avgVal = if (points.isNotEmpty()) points.map { it.value }.average().toFloat() else 0f

                seriesList.add(
                    VmTelemetrySeries(
                        vmId = vm.id,
                        vmName = vm.name,
                        colorHex = color,
                        points = points,
                        currentValue = currentVal,
                        peakValue = peakVal,
                        avgValue = avgVal,
                        isRunning = vm.status == "RUNNING"
                    )
                )
            }
        }

        return VmMetricsUiState(
            recentMetrics = metrics,
            vms = vms,
            selectedMetricMode = mode,
            selectedVmIdFilter = selectedVmId,
            seriesList = seriesList,
            aggregateCpuPercent = aggCpu,
            aggregateRamUsedMb = aggRamUsed,
            aggregateRamAllocatedMb = aggRamAlloc,
            runningVmCount = runningCount,
            stoppedVmCount = stoppedCount,
            pausedVmCount = pausedCount,
            faultedVmCount = faultedCount,
            statusDistribution = statusMap,
            latestPerVm = latestPerVm
        )
    }

    fun setMetricMode(mode: MetricViewMode) {
        _selectedMetricMode.value = mode
    }

    fun selectVmFilter(vmId: Int?) {
        _selectedVmIdFilter.value = vmId
    }

    fun recordSnapshot(
        vmId: Int,
        name: String,
        status: String,
        cpu: Float,
        ramMb: Int,
        ramAllocated: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            vmMetricDao.insertMetric(
                VmMetricSnapshotEntity(
                    vmId = vmId,
                    vmName = name,
                    timestamp = System.currentTimeMillis(),
                    status = status,
                    cpuUsagePercent = cpu,
                    memoryUsageMb = ramMb,
                    memoryAllocatedMb = ramAllocated
                )
            )
        }
    }

    fun pruneMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            vmMetricDao.pruneOldMetrics(150)
        }
    }
}
