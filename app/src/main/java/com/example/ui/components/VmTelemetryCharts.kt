package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg
import com.example.ui.viewmodel.MetricViewMode
import com.example.ui.viewmodel.VmMetricsUiState
import com.example.ui.viewmodel.VmTelemetrySeries
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VmPerformanceChartCard(
    metricsState: VmMetricsUiState,
    onSelectMode: (MetricViewMode) -> Unit,
    onSelectVmFilter: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vm_telemetry_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Live Pulse indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StatusGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VM TELEMETRY & RESOURCE CHARTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProxmoxOrange,
                            letterSpacing = 1.1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Live Stage-2 MMU & vCPU Metrics",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                // Sample count pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800
                ) {
                    Text(
                        text = "${metricsState.recentMetrics.size} SAMPLES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Mode Selector Tabs (CPU, RAM, STATUS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate950)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricModeTabItem(
                    title = "CPU Usage",
                    icon = Icons.Default.Speed,
                    isSelected = metricsState.selectedMetricMode == MetricViewMode.CPU,
                    accentColor = ProxmoxOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(MetricViewMode.CPU) }
                )
                MetricModeTabItem(
                    title = "Memory RAM",
                    icon = Icons.Default.Memory,
                    isSelected = metricsState.selectedMetricMode == MetricViewMode.RAM,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(MetricViewMode.RAM) }
                )
                MetricModeTabItem(
                    title = "Status Map",
                    icon = Icons.Default.PieChart,
                    isSelected = metricsState.selectedMetricMode == MetricViewMode.STATUS,
                    accentColor = StatusGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(MetricViewMode.STATUS) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VM Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAllSelected = metricsState.selectedVmIdFilter == null
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAllSelected) ProxmoxOrange.copy(alpha = 0.2f) else Slate850,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAllSelected) ProxmoxOrange else Slate700
                    ),
                    modifier = Modifier.clickable { onSelectVmFilter(null) }
                ) {
                    Text(
                        text = "ALL GUESTS (${metricsState.vms.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllSelected) ProxmoxOrangeLight else Slate400,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                metricsState.vms.forEach { vm ->
                    val isSelected = metricsState.selectedVmIdFilter == vm.id
                    val vmColor = if (vm.osFamily.equals("WINDOWS", ignoreCase = true)) CyberCyan else ProxmoxOrange
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) vmColor.copy(alpha = 0.2f) else Slate850,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) vmColor else Slate700
                        ),
                        modifier = Modifier.clickable { onSelectVmFilter(vm.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (vm.status) {
                                            "RUNNING" -> StatusGreen
                                            "PAUSED" -> StatusYellow
                                            else -> Slate600
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "VM ${vm.id} (${vm.name})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PureWhite else Slate400,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Content based on Mode
            when (metricsState.selectedMetricMode) {
                MetricViewMode.CPU -> {
                    CpuMetricsView(metricsState = metricsState)
                }
                MetricViewMode.RAM -> {
                    RamMetricsView(metricsState = metricsState)
                }
                MetricViewMode.STATUS -> {
                    StatusDistributionView(metricsState = metricsState)
                }
            }
        }
    }
}

@Composable
private fun MetricModeTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Slate850 else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else Slate400,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PureWhite else Slate400
            )
        }
    }
}

@Composable
private fun CpuMetricsView(metricsState: VmMetricsUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary Stat Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartStatBox(
                label = "AVG ACTIVE LOAD",
                value = String.format("%.1f%%", metricsState.aggregateCpuPercent),
                accentColor = ProxmoxOrange,
                modifier = Modifier.weight(1f)
            )
            val peakLoad = metricsState.seriesList.flatMap { it.points }.maxOfOrNull { it.value } ?: 0f
            ChartStatBox(
                label = "PEAK SPIKE",
                value = String.format("%.1f%%", peakLoad),
                accentColor = StatusRed,
                modifier = Modifier.weight(1f)
            )
            ChartStatBox(
                label = "ONLINE VMS",
                value = "${metricsState.runningVmCount} / ${metricsState.vms.size}",
                accentColor = StatusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Series Canvas Area Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate950)
                .border(1.dp, Slate850, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            VmTimeSeriesCanvas(
                seriesList = metricsState.seriesList,
                maxValue = 100f,
                unit = "%",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend Row
        ChartLegendRow(seriesList = metricsState.seriesList, unit = "%")
    }
}

@Composable
private fun RamMetricsView(metricsState: VmMetricsUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary Stat Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartStatBox(
                label = "TOTAL CONSUMED",
                value = "${metricsState.aggregateRamUsedMb} MB",
                accentColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            ChartStatBox(
                label = "ALLOCATED POOL",
                value = "${metricsState.aggregateRamAllocatedMb} MB",
                accentColor = ProxmoxOrangeLight,
                modifier = Modifier.weight(1f)
            )
            val ramPercent = if (metricsState.aggregateRamAllocatedMb > 0) {
                (metricsState.aggregateRamUsedMb.toFloat() / metricsState.aggregateRamAllocatedMb * 100f)
            } else 0f
            ChartStatBox(
                label = "RAM UTILIZATION",
                value = String.format("%.1f%%", ramPercent),
                accentColor = if (ramPercent > 80f) StatusYellow else StatusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RAM Time-Series Chart
        val maxAllocated = (metricsState.seriesList.map { it.peakValue }.maxOrNull() ?: 1024f).coerceAtLeast(512f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate950)
                .border(1.dp, Slate850, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            VmTimeSeriesCanvas(
                seriesList = metricsState.seriesList,
                maxValue = (maxAllocated * 1.25f),
                unit = "MB",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Per-VM Memory Allocation Progress Bars
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PER-GUEST MEMORY CONSUMPTION (COMMITTED VS PROVISIONED)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                fontFamily = FontFamily.Monospace
            )

            metricsState.vms.forEach { vm ->
                val usedMb = vm.memoryUsageMb
                val totalMb = vm.ramMb.coerceAtLeast(1)
                val percent = (usedMb.toFloat() / totalMb).coerceIn(0f, 1f)
                val isRunning = vm.status == "RUNNING"

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate850,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "VM ${vm.id} [${vm.name}]",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                Text(
                                    text = "$usedMb / $totalMb MB (${(percent * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isRunning) CyberCyan else Slate400
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // Custom progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Slate800)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percent)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when {
                                                !isRunning -> Slate600
                                                percent > 0.85f -> StatusRed
                                                percent > 0.65f -> StatusYellow
                                                else -> CyberCyan
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDistributionView(metricsState: VmMetricsUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPillStat("RUNNING", metricsState.runningVmCount, StatusGreen, StatusGreenBg, Modifier.weight(1f))
            StatusPillStat("PAUSED", metricsState.pausedVmCount, StatusYellow, StatusYellowBg, Modifier.weight(1f))
            StatusPillStat("STOPPED", metricsState.stoppedVmCount, Slate400, Slate800, Modifier.weight(1f))
            StatusPillStat("FAULTED", metricsState.faultedVmCount, StatusRed, StatusRedBg, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Donut Chart & Breakdown Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate950)
                .border(1.dp, Slate850, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Donut Chart Canvas
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                VmStatusDonutCanvas(
                    running = metricsState.runningVmCount,
                    paused = metricsState.pausedVmCount,
                    stopped = metricsState.stoppedVmCount,
                    faulted = metricsState.faultedVmCount,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${metricsState.vms.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VIRTUAL MACHINES",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Distribution Legend Table
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val total = metricsState.vms.size.coerceAtLeast(1)
                DistributionRowItem(
                    label = "Active / Running",
                    count = metricsState.runningVmCount,
                    percent = (metricsState.runningVmCount * 100 / total),
                    color = StatusGreen
                )
                DistributionRowItem(
                    label = "Paused / Suspended",
                    count = metricsState.pausedVmCount,
                    percent = (metricsState.pausedVmCount * 100 / total),
                    color = StatusYellow
                )
                DistributionRowItem(
                    label = "Stopped / Offline",
                    count = metricsState.stoppedVmCount,
                    percent = (metricsState.stoppedVmCount * 100 / total),
                    color = Slate400
                )
                DistributionRowItem(
                    label = "Kernel Faulted",
                    count = metricsState.faultedVmCount,
                    percent = (metricsState.faultedVmCount * 100 / total),
                    color = StatusRed
                )
            }
        }
    }
}

@Composable
private fun StatusPillStat(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DistributionRowItem(
    label: String,
    count: Int,
    percent: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, color = Slate200)
        }
        Text(
            text = "$count ($percent%)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ChartStatBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate950,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate850),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun VmTimeSeriesCanvas(
    seriesList: List<VmTelemetrySeries>,
    maxValue: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val effectiveMax = if (maxValue > 0f) maxValue else 100f

        // Draw horizontal grid lines (0%, 25%, 50%, 75%, 100%)
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = h - (i.toFloat() / gridSteps * h)
            drawLine(
                color = Color(0xFF1E293B), // Slate800
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Draw each VM series
        seriesList.forEach { series ->
            val points = series.points
            if (points.size >= 2) {
                val stepX = w / (points.size - 1).coerceAtLeast(1)
                val strokeColor = Color(series.colorHex)

                val linePath = Path()
                val fillPath = Path()

                var startX = 0f
                var startY = h - ((points[0].value / effectiveMax).coerceIn(0f, 1f) * h)
                linePath.moveTo(startX, startY)
                fillPath.moveTo(startX, h)
                fillPath.lineTo(startX, startY)

                for (i in 1 until points.size) {
                    val currentX = i * stepX
                    val currentY = h - ((points[i].value / effectiveMax).coerceIn(0f, 1f) * h)

                    // Smooth bezier curve control points
                    val prevX = (i - 1) * stepX
                    val prevY = h - ((points[i - 1].value / effectiveMax).coerceIn(0f, 1f) * h)
                    val cX1 = (prevX + currentX) / 2f
                    val cY1 = prevY
                    val cX2 = (prevX + currentX) / 2f
                    val cY2 = currentY

                    linePath.cubicTo(cX1, cY1, cX2, cY2, currentX, currentY)
                    fillPath.cubicTo(cX1, cY1, cX2, cY2, currentX, currentY)
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                // Draw gradient under the curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            strokeColor.copy(alpha = 0.25f),
                            strokeColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // Draw curve stroke
                drawPath(
                    path = linePath,
                    color = strokeColor,
                    style = Stroke(
                        width = 2.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw live end point indicator with glow
                val lastIdx = points.size - 1
                val lastX = lastIdx * stepX
                val lastY = h - ((points[lastIdx].value / effectiveMax).coerceIn(0f, 1f) * h)

                drawCircle(
                    color = strokeColor.copy(alpha = 0.35f),
                    radius = 6f,
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = strokeColor,
                    radius = 3.5f,
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}

@Composable
private fun VmStatusDonutCanvas(
    running: Int,
    paused: Int,
    stopped: Int,
    faulted: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = (running + paused + stopped + faulted).coerceAtLeast(1).toFloat()
        val strokeWidth = 18.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        var startAngle = -90f

        val slices = listOf(
            running.toFloat() to StatusGreen,
            paused.toFloat() to StatusYellow,
            stopped.toFloat() to Color(0xFF475569), // Slate600
            faulted.toFloat() to StatusRed
        )

        slices.forEach { (count, color) ->
            if (count > 0f) {
                val sweepAngle = (count / total) * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f, // 2 degree gap for clean segmenting
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun ChartLegendRow(seriesList: List<VmTelemetrySeries>, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        seriesList.forEach { series ->
            val color = Color(series.colorHex)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate950)
                    .border(1.dp, Slate850, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${series.vmName}:",
                    fontSize = 10.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f%s", series.currentValue, unit),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
