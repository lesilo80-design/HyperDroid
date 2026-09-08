package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HypervisorLogEntity
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ProxmoxOrange
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
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HypercallConsoleScreen(
    logs: List<HypervisorLogEntity>,
    onClearLogs: () -> Unit,
    onSendCommand: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var commandInput by remember { mutableStateOf("") }
    val filters = listOf("ALL", "HYPERCALL", "ISOLATION_VIOLATION", "WARN", "INFO")

    val filteredLogs = if (selectedFilter == "ALL") {
        logs
    } else {
        logs.filter { it.level.equals(selectedFilter, ignoreCase = true) }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .testTag("hypercall_console_screen")
    ) {
        SectionHeader(
            title = "Hypervisor & Trapped Isolation Syslog",
            icon = Icons.Default.Terminal,
            badgeCount = "${logs.size} EVENTS",
            action = {
                IconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Text(
            text = "Real-time streaming log of hardware EL2 hypercalls, stage-2 page traps, Seccomp policy violations, and container sandbox lifecycles.",
            fontSize = 12.sp,
            color = Slate400,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Filter Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ProxmoxOrange else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ProxmoxOrange else Slate800
                    ),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Slate950 else Slate400,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Log Stream Terminal View
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TerminalBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events matching filter '$selectedFilter'",
                        fontSize = 12.sp,
                        color = Slate600,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val (levelColor, tagColor) = when (log.level.uppercase()) {
                            "ISOLATION_VIOLATION" -> StatusRed to StatusRed
                            "WARN" -> StatusYellow to StatusYellow
                            "HYPERCALL" -> CyberCyan to CyberCyan
                            else -> TerminalGreen to Slate400
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = timeFormat.format(Date(log.timestamp)),
                                fontSize = 10.sp,
                                color = Slate600,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[${log.tag}]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log.message,
                                fontSize = 11.sp,
                                color = levelColor,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hypervisor shell command row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Hypervisor CLI: 'status', 'test-seccomp', 'dmesg'...", fontSize = 11.sp, color = Slate600) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProxmoxOrange,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("hypervisor_cli_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onSendCommand(100, commandInput)
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProxmoxOrange)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Slate950)
            }
        }
    }
}
