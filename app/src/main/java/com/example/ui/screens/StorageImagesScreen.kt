package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.IsoImage
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.ProxmoxOrange
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

@Composable
fun StorageImagesScreen(
    images: List<IsoImage>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredImages = remember(selectedCategory, images) {
        if (selectedCategory == "ALL") images
        else images.filter { it.osFamily.equals(selectedCategory, ignoreCase = true) }
    }

    val totalSizeMb = images.sumOf { it.sizeMb }
    val poolTotalMb = 16384 // 16 GB virtual storage pool
    val poolPercent = (totalSizeMb.toFloat() / poolTotalMb).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(16.dp)
            .testTag("storage_images_screen")
    ) {
        SectionHeader(
            title = "Virtual Storage & OS Templates",
            icon = Icons.Default.Storage,
            badgeCount = "${images.size} IMAGES"
        )

        // Storage Pool Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ProxmoxOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("local-zfs (/var/lib/vz/template/iso)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Text(
                        text = "${totalSizeMb}MB / ${poolTotalMb}MB",
                        fontSize = 12.sp,
                        color = ProxmoxOrangeLight,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { poolPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ProxmoxOrange,
                    trackColor = Slate800
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Type: ZFS Sparse Volumes (qcow2/vhdx). Supports Windows ARM64, Linux KVM kernels, and Android AVF Microdroid payloads.",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // OS Category filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "WINDOWS", "LINUX", "ANDROID").forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ProxmoxOrange else Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ProxmoxOrange else Slate800),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Slate950 else Slate400,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Image Templates List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredImages, key = { it.id }) { image ->
                IsoImageCard(image = image)
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun IsoImageCard(
    image: IsoImage,
    modifier: Modifier = Modifier
) {
    val isWin = image.osFamily.equals("Windows", ignoreCase = true)
    val isLinux = image.osFamily.equals("Linux", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWin) CyberCyan.copy(alpha = 0.4f) else Slate800
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = image.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                isWin -> CyberCyan.copy(alpha = 0.2f)
                                isLinux -> ProxmoxOrange.copy(alpha = 0.2f)
                                else -> StatusGreen.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = image.osFamily.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isWin -> CyberCyan
                                    isLinux -> ProxmoxOrange
                                    else -> StatusGreen
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${image.architecture} • ${image.format.uppercase()} • ${image.sizeMb} MB",
                        fontSize = 11.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StatusGreenBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(image.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware Specs requirement bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Slate800
                ) {
                    Text(
                        text = "Rec: ${image.recommendedCpu} vCPU / ${image.recommendedRamMb}MB",
                        fontSize = 9.sp,
                        color = Slate200,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (image.requiresTpm) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "vTPM 2.0 REQUIRED",
                            fontSize = 9.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (image.requiresUefi) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ProxmoxOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "UEFI EDK2",
                            fontSize = 9.sp,
                            color = ProxmoxOrangeLight,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Isolation features tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                image.isolationFeatures.take(3).forEach { feat ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = feat,
                            fontSize = 9.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SHA256 checksum
            Text(
                text = "SHA256: ${image.sha256.take(24)}...",
                fontSize = 10.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
