package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import com.example.ui.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvProtocol
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ButtonBorder
import com.example.ui.theme.ButtonSurface
import com.example.ui.theme.ButtonSurfacePressed
import com.example.ui.theme.RemoteBody
import com.example.ui.theme.RemoteCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TvBoxGuideDialog(
    subnetPrefix: String,
    userWifiIp: String?,
    isTestingPing: Boolean,
    pingResult: String?,
    onTestPing: (ip: String, port: Int) -> Unit,
    onConnect: (name: String, ip: String, port: Int, protocol: TvProtocol) -> Unit,
    onDismiss: () -> Unit
) {
    var ipInput by remember { mutableStateOf("192.168.0.100") }
    var selectedBoxType by remember { mutableStateOf("Android TV Box") }
    var port by remember { mutableStateOf(5555) }
    var protocol by remember { mutableStateOf(TvProtocol.ANDROID_TV_TCP) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RemoteBody,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00384D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Tv,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "টিভি বক্স কানেক্ট করার সহজ গাইড",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Easy TV Box Setup Guide (IP: 192.168.0.100)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Step 1: Same Wi-Fi
                GuideStepCard(
                    stepNumber = "১",
                    title = "একই Wi-Fi তে কানেক্ট রাখুন",
                    description = "আপনার মোবাইল এবং টিভি বক্স (TX3, Tanix, X96, Walton, Mi Box ইত্যাদি) একই Wi-Fi নেটওয়ার্কে রাখুন।\nআপনার টিভির IP: 192.168.0.100",
                    icon = AppIcons.Wifi,
                    badge = if (userWifiIp != null) "ফোন: $userWifiIp" else null
                )

                // Step 2: Enable Network Debugging
                GuideStepCard(
                    stepNumber = "২",
                    title = "টিভি বক্সে Network Debugging চালু করুন",
                    description = "Wi-Fi রিমোট কাজ করতে টিভিতে ADB চালু থাকতে হবে:\n১. টিভির আসল রিমোট দিয়ে যান: Settings ➔ About (ডিভাইস তথ্য)।\n২. নিচে গিয়ে Build (বিল্ড নম্বর) এ ৭ বার OK চাপুন (ডেভেলপার মোড চালু হবে)।\n৩. Developer Options এ গিয়ে Network Debugging / USB Debugging অন করুন।",
                    icon = Icons.Default.Settings
                )

                // Step 3: Quick Connect Form
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RemoteCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ধাপ ৩: IP লিখে এক ক্লিকে কানেক্ট করুন",
                                color = AccentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // TV Box Type Selector Chips
                        Text(
                            text = "আপনার টিভি বক্সের ধরন বেছে নিন:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BoxTypeChip(
                                label = "Android Box",
                                isSelected = selectedBoxType == "Android TV Box",
                                onClick = {
                                    selectedBoxType = "Android TV Box"
                                    port = 5555
                                    protocol = TvProtocol.ANDROID_TV_TCP
                                }
                            )
                            BoxTypeChip(
                                label = "Universal/Smart",
                                isSelected = selectedBoxType == "Smart TV",
                                onClick = {
                                    selectedBoxType = "Smart TV"
                                    port = 8080
                                    protocol = TvProtocol.UNIVERSAL_REST
                                }
                            )
                            BoxTypeChip(
                                label = "Roku Box",
                                isSelected = selectedBoxType == "Roku Box",
                                onClick = {
                                    selectedBoxType = "Roku Box"
                                    port = 8060
                                    protocol = TvProtocol.ROKU_ECP
                                }
                            )
                        }

                        // IP Address Text Field
                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("টিভি বক্সের IP Address") },
                            placeholder = { Text("${subnetPrefix}xxx") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = ButtonBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tvbox_guide_ip_input")
                        )

                        // Ping Test Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { onTestPing(ipInput.trim(), port) },
                                enabled = !isTestingPing && ipInput.isNotBlank(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingPing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = AccentCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = AppIcons.NetworkCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("টেস্ট পিং (Test)", fontSize = 11.sp)
                            }

                            if (pingResult != null) {
                                Text(
                                    text = pingResult,
                                    color = if (pingResult.contains("Online")) StatusGreen else Color(0xFFFF9100),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConnect(selectedBoxType, ipInput.trim(), port, protocol)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("tvbox_guide_connect_button")
            ) {
                Text("কানেক্ট করুন (Connect)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ButtonSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E283D)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00384D))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AccentCyan else Color(0xFF182233))
            .border(
                1.dp,
                if (isSelected) AccentCyan else ButtonBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
