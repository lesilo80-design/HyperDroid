package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IsolationTestEntity
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.ProxmoxOrangeDark
import com.example.ui.theme.ProxmoxOrangeLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
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
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IsolationTestSuiteScreen(
    tests: List<IsolationTestEntity>,
    selectedCategory: String,
    isAuditing: Boolean,
    onSelectCategory: (String) -> Unit,
    onRunSingleTest: (String) -> Unit,
    onRunAllTests: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("ALL", "HYPERVISOR", "SECCOMP", "NAMESPACES", "PRIVILEGES", "MEMORY", "LEAK_SHIELD")
    val filteredTests = if (selectedCategory == "ALL") {
        tests
    } else {
        tests.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val passedCount = tests.count { it.status == "PASSED" }
    val warningCount = tests.count { it.status == "WARNING" }
    val failedCount = tests.count { it.status == "FAILED" }
    val testedCount = tests.count { it.status != "PENDING" }
    val averageScore = if (testedCount > 0) {
        tests.filter { it.status != "PENDING" }.map { it.score }.average().toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .testTag("isolation_test_suite_screen")
    ) {
        // Top Summary Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KERNEL ISOLATION AUDIT SUITE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProxmoxOrange,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hardware & OS Confinement",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    // Score pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (averageScore >= 80) StatusGreenBg else Slate800,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (averageScore >= 80) StatusGreen else Slate700
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (testedCount > 0) "$averageScore%" else "NOT RUN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (averageScore >= 80) StatusGreen else Slate200,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestStatBadge("PASSED", passedCount, StatusGreen, StatusGreenBg, Modifier.weight(1f))
                    TestStatBadge("WARNING", warningCount, StatusYellow, StatusYellowBg, Modifier.weight(1f))
                    TestStatBadge("FAILED", failedCount, StatusRed, StatusRedBg, Modifier.weight(1f))
                    TestStatBadge("TOTAL", tests.size, CyberCyan, CyberCyanDim, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRunAllTests,
                    enabled = !isAuditing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProxmoxOrange,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_all_tests_button")
                ) {
                    if (isAuditing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Slate950
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUNNING KERNEL SUITE...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXECUTE FULL KERNEL AUDIT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ProxmoxOrange else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ProxmoxOrange else Slate700
                    ),
                    modifier = Modifier.clickable { onSelectCategory(category) }
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Slate950 else Slate400,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tests List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredTests, key = { it.id }) { test ->
                IsolationTestCard(
                    test = test,
                    onRun = { onRunSingleTest(test.id) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TestStatBadge(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun IsolationTestCard(
    test: IsolationTestEntity,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier
            .fillMaxWidth()
            .testTag("test_card_${test.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = test.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProxmoxOrangeLight,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = test.kernelConfig,
                            fontSize = 10.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = test.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = if (test.status == "RUNNING") "RUNNING_TEST" else test.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = test.summary,
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer / Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand Details toggle
                Row(
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Hide Technical Report" else "Inspect Probe Output",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberCyan
                    )
                }

                // Run Test Button
                OutlinedButton(
                    onClick = onRun,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProxmoxOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProxmoxOrangeDark),
                    modifier = Modifier.testTag("run_test_${test.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PROBE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expanded Technical Output
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TerminalBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "LOW-LEVEL KERNEL PROBE LOG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = test.technicalOutput,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalGreen,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate850,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Security Confinement Impact",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                Text(
                                    text = test.securityImpact,
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
