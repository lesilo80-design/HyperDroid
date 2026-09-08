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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IsoImage
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ProxmoxOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusGreen

@Composable
fun CreateVmDialog(
    isoImages: List<IsoImage>,
    onDismiss: () -> Unit,
    onCreateVm: (
        name: String,
        osType: String,
        vCpu: Int,
        ramMb: Int,
        isProtectedPkvm: Boolean,
        seccomp: String,
        selinux: String,
        osFamily: String,
        architecture: String,
        firmware: String,
        hasTpm: Boolean,
        hasVirtIoDrivers: Boolean,
        displayMode: String,
        diskSizeGb: Int
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, WINDOWS, LINUX, ANDROID

    val filteredImages = remember(selectedCategory, isoImages) {
        if (selectedCategory == "ALL") isoImages
        else isoImages.filter { it.osFamily.equals(selectedCategory, ignoreCase = true) }
    }

    val defaultSelected = filteredImages.firstOrNull() ?: isoImages.firstOrNull()

    var selectedImage by remember { mutableStateOf(defaultSelected?.name ?: "Ubuntu Server 24.04 LTS (Noble)") }
    val currentImageObj = isoImages.find { it.name == selectedImage } ?: defaultSelected

    val derivedFamily = currentImageObj?.osFamily?.uppercase() ?: "LINUX"
    val isWindowsFamily = derivedFamily.contains("WINDOWS")

    var vmName by remember(selectedImage) {
        mutableStateOf(
            when {
                derivedFamily.contains("WINDOWS") -> "win11-guest-${(100..999).random()}"
                derivedFamily.contains("LINUX") -> "ubuntu-srv-${(100..999).random()}"
                else -> "microdroid-${(100..999).random()}"
            }
        )
    }

    var vCpuCount by remember(selectedImage) {
        mutableIntStateOf(currentImageObj?.recommendedCpu ?: if (isWindowsFamily) 4 else 2)
    }
    var ramMb by remember(selectedImage) {
        mutableIntStateOf(currentImageObj?.recommendedRamMb ?: if (isWindowsFamily) 2048 else 1024)
    }
    var diskSizeGb by remember(selectedImage) {
        mutableIntStateOf(if (isWindowsFamily) 64 else 32)
    }

    var firmware by remember(selectedImage) {
        mutableStateOf(if (isWindowsFamily || currentImageObj?.requiresUefi == true) "UEFI (EDK2 with Secure Boot)" else "Direct Kernel Boot (vmlinux)")
    }
    var hasTpm by remember(selectedImage) {
        mutableStateOf(isWindowsFamily || currentImageObj?.requiresTpm == true)
    }
    var hasVirtIoDrivers by remember { mutableStateOf(true) }
    var displayMode by remember(selectedImage) {
        mutableStateOf(if (isWindowsFamily) "VIRTIO_GPU" else "VIRTIO_GPU")
    }

    var isProtectedPkvm by remember { mutableStateOf(true) }
    var seccompLevel by remember { mutableStateOf("STRICT_BPF") }
    var selinuxMode by remember { mutableStateOf("ENFORCING") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("create_vm_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = ProxmoxOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deploy Virtual Machine", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Launch Windows or Linux guest OS with hardware-assisted pKVM Stage-2 isolation, virtual TPM 2.0, and VirtIO devices.",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OS Family Selector Chips
                Text("Operating System Family", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "WINDOWS", "LINUX", "ANDROID").forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCatSelected) ProxmoxOrange else Slate850,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategory = cat
                                    val firstOfCat = if (cat == "ALL") isoImages.firstOrNull() else isoImages.firstOrNull { it.osFamily.equals(cat, ignoreCase = true) }
                                    if (firstOfCat != null) {
                                        selectedImage = firstOfCat.name
                                    }
                                }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCatSelected) Slate950 else Slate200,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // VM Hostname Input
                Text("VM Hostname / Tag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = vmName,
                    onValueChange = { vmName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProxmoxOrange,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vm_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OS Templates list
                Text("Select OS Image Template", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(6.dp))
                filteredImages.forEach { img ->
                    val isSelected = selectedImage == img.name
                    val isWin = img.osFamily.equals("Windows", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Slate800 else Slate950,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) (if (isWin) CyberCyan else ProxmoxOrange) else Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { selectedImage = img.name }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = img.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isWin) CyberCyan.copy(alpha = 0.2f) else ProxmoxOrange.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = img.osFamily.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWin) CyberCyan else ProxmoxOrange,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${img.architecture} • ${img.format.uppercase()} • ${img.sizeMb} MB",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isWin) CyberCyan else ProxmoxOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // vCPU Cores Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("vCPU Allocation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)
                    Text("$vCpuCount Cores", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProxmoxOrange, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = vCpuCount.toFloat(),
                    onValueChange = { vCpuCount = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = ProxmoxOrange,
                        activeTrackColor = ProxmoxOrange,
                        inactiveTrackColor = Slate800
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // RAM Allocation Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Memory (RAM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)
                    Text("$ramMb MB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = ramMb.toFloat(),
                    onValueChange = { ramMb = (it / 128).toInt() * 128 },
                    valueRange = 256f..4096f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = Slate800
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Virtual Disk Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Virtual Disk (NVMe)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(16, 32, 64, 128).forEach { size ->
                            val isSizeSelected = diskSizeGb == size
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSizeSelected) ProxmoxOrange else Slate800,
                                modifier = Modifier.clickable { diskSizeGb = size }
                            ) {
                                Text(
                                    text = "${size}GB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSizeSelected) Slate950 else Slate400,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Virtualization Options
                Text("Hardware Virtualization & Firmware", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(6.dp))

                // Firmware Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UEFI (EDK2 with Secure Boot)", "Direct Kernel Boot (vmlinux)").forEach { fw ->
                        val isFw = firmware == fw
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isFw) Slate800 else Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isFw) ProxmoxOrange else Slate800),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { firmware = fw }
                        ) {
                            Text(
                                text = if (fw.contains("UEFI")) "UEFI + Secure Boot" else "Direct vmlinux",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFw) PureWhite else Slate400,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Virtual TPM 2.0 (swtpm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Virtual TPM 2.0 (swtpm)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("Cryptographic hardware root-of-trust (required for Windows 11)", fontSize = 10.sp, color = Slate400)
                    }
                    Switch(
                        checked = hasTpm,
                        onCheckedChange = { hasTpm = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusGreen,
                            checkedTrackColor = Slate800
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // VirtIO Guest Drivers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VirtIO Guest Bus (viostor/netkvm)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("High performance para-virtualized block and network drivers", fontSize = 10.sp, color = Slate400)
                    }
                    Switch(
                        checked = hasVirtIoDrivers,
                        onCheckedChange = { hasVirtIoDrivers = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusGreen,
                            checkedTrackColor = Slate800
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Display Output", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("VirtIO-GPU Desktop vs Headless Serial Console", fontSize = 10.sp, color = Slate400)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("VIRTIO_GPU" to "GPU", "SERIAL_CONSOLE" to "TTY").forEach { (mode, label) ->
                            val isModeSelected = displayMode == mode
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isModeSelected) CyberCyan else Slate800,
                                modifier = Modifier.clickable { displayMode = mode }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isModeSelected) Slate950 else Slate400,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // pKVM Stage-2 Isolation Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Protected KVM (Stage-2 MMU)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("EL2 hardware memory encryption & host hypervisor isolation", fontSize = 10.sp, color = Slate400)
                    }
                    Switch(
                        checked = isProtectedPkvm,
                        onCheckedChange = { isProtectedPkvm = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusGreen,
                            checkedTrackColor = Slate800
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateVm(
                        vmName,
                        selectedImage,
                        vCpuCount,
                        ramMb,
                        isProtectedPkvm,
                        seccompLevel,
                        selinuxMode,
                        derivedFamily,
                        currentImageObj?.architecture ?: "ARM64",
                        firmware,
                        hasTpm,
                        hasVirtIoDrivers,
                        displayMode,
                        diskSizeGb
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProxmoxOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_create_vm_button")
            ) {
                Text("Deploy $derivedFamily VM", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
