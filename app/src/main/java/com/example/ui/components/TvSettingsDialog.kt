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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ui.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.RemoteBody
import com.example.ui.theme.RemoteCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TvSettingsDialog(
    initialIp: String = "192.168.0.100",
    initialPort: Int = 5555,
    initialName: String = "Android TV Box",
    initialProtocol: TvProtocol = TvProtocol.ANDROID_TV_TCP,
    isTestingPing: Boolean = false,
    pingResult: String? = null,
    onTestPing: (ip: String, port: Int) -> Unit,
    onSaveAndConnect: (name: String, ip: String, port: Int, protocol: TvProtocol) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName.ifBlank { "Android TV Box" }) }
    var ip by remember { mutableStateOf(initialIp.ifBlank { "192.168.0.100" }) }
    var portText by remember { mutableStateOf(initialPort.toString()) }
    var selectedProtocol by remember { mutableStateOf(initialProtocol) }
    var protocolDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RemoteBody,
        title = {
            Text(
                text = "Manual Smart TV Setup",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure IP address and protocol for direct local Wi-Fi control:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                // TV Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("TV Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = ButtonBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tv_name")
                )

                // TV IP address input
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address (e.g. 192.168.1.105)") },
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
                        .testTag("input_tv_ip")
                )

                // Port input
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port (Roku: 8060, Android: 5555, REST: 8080)") },
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
                        .testTag("input_tv_port")
                )

                // Protocol Dropdown
                Column {
                    Text(
                        text = "Protocol Type",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ButtonSurface)
                            .border(1.dp, ButtonBorder, RoundedCornerShape(8.dp))
                            .clickable { protocolDropdownExpanded = true }
                            .padding(12.dp)
                            .testTag("protocol_dropdown_trigger")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedProtocol.displayName,
                                color = AccentCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = AppIcons.ArrowDropDown,
                                contentDescription = null,
                                tint = AccentCyan
                            )
                        }

                        DropdownMenu(
                            expanded = protocolDropdownExpanded,
                            onDismissRequest = { protocolDropdownExpanded = false }
                        ) {
                            TvProtocol.values().forEach { proto ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(proto.displayName, fontWeight = FontWeight.SemiBold)
                                            Text(proto.description, fontSize = 11.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedProtocol = proto
                                        if (proto != TvProtocol.VIRTUAL_SIMULATOR) {
                                            portText = proto.defaultPort.toString()
                                        }
                                        protocolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Test Connection Button & Ping Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val parsedPort = portText.toIntOrNull() ?: selectedProtocol.defaultPort
                            onTestPing(ip.trim(), parsedPort)
                        },
                        enabled = !isTestingPing && ip.isNotBlank(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_ping_button")
                    ) {
                        if (isTestingPing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
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
                        Text("Test Ping", fontSize = 12.sp)
                    }

                    if (pingResult != null) {
                        Text(
                            text = pingResult,
                            color = if (pingResult.contains("Online")) StatusGreen else Color(0xFFFF9100),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPort = portText.toIntOrNull() ?: selectedProtocol.defaultPort
                    onSaveAndConnect(name.trim(), ip.trim(), parsedPort, selectedProtocol)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_and_connect_button")
            ) {
                Text("Connect TV", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
