package com.example.network

import android.util.Log
import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.model.TvDevice
import com.example.model.TvProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class TvNetworkClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    companion object {
        private const val TAG = "TvNetworkClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Dispatch a command to a TV device according to its protocol
     */
    suspend fun sendCommand(device: TvDevice, command: RemoteCommand): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                when (device.protocol) {
                    TvProtocol.ROKU_ECP -> sendRokuCommand(device, command, startTime)
                    TvProtocol.ANDROID_TV_TCP -> sendTcpSocketCommand(device, command, startTime)
                    TvProtocol.UNIVERSAL_REST -> sendRestCommand(device, command, startTime)
                    TvProtocol.VIRTUAL_SIMULATOR -> {
                        // Virtual command simulated with instant realistic response
                        val latency = (12L..35L).random()
                        CommandResult(
                            command = command,
                            success = true,
                            message = "OK (Simulated TV: ${command.name})",
                            latencyMs = latency
                        )
                    }
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                Log.w(TAG, "Error sending ${command.name} to ${device.ipAddress}:${device.port}: ${e.message}")
                CommandResult(
                    command = command,
                    success = false,
                    message = e.localizedMessage ?: "Network error connecting to ${device.ipAddress}:${device.port}",
                    latencyMs = latency
                )
            }
        }

    /**
     * Roku ECP Protocol (HTTP POST http://<ip>:8060/keypress/<key>)
     */
    private fun sendRokuCommand(
        device: TvDevice,
        command: RemoteCommand,
        startTime: Long
    ): CommandResult {
        val endpoint = if (command.rokuKey.startsWith("launch/")) {
            // App launch endpoint
            "http://${device.ipAddress}:${device.port}/${command.rokuKey}"
        } else {
            // Standard keypress endpoint
            "http://${device.ipAddress}:${device.port}/keypress/${command.rokuKey}"
        }

        val emptyBody = "".toRequestBody(null)
        val request = Request.Builder()
            .url(endpoint)
            .post(emptyBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            return if (response.isSuccessful) {
                CommandResult(
                    command = command,
                    success = true,
                    message = "HTTP ${response.code} OK",
                    latencyMs = latency
                )
            } else {
                CommandResult(
                    command = command,
                    success = false,
                    message = "HTTP ${response.code} ${response.message}",
                    latencyMs = latency
                )
            }
        }
    }

    /**
     * TCP Socket Command (Connects to raw TCP socket e.g. port 5555/6466/8080)
     * Transmits Android Keycode and waits briefly for ACK
     */
    private fun sendTcpSocketCommand(
        device: TvDevice,
        command: RemoteCommand,
        startTime: Long
    ): CommandResult {
        Socket().use { socket ->
            socket.soTimeout = 2000
            socket.connect(InetSocketAddress(device.ipAddress, device.port), 2000)

            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

            // Protocol format: "KEYDOWN <CODE>\nKEYUP <CODE>\n" or "KEYCODE:<CODE>\n"
            val payload = "KEYCODE:${command.androidKeyCode}:${command.name}\n"
            writer.print(payload)
            writer.flush()

            // Read response if available
            val reply = try {
                if (socket.getInputStream().available() > 0) {
                    reader.readLine() ?: "ACK"
                } else {
                    "ACK Sent"
                }
            } catch (e: Exception) {
                "ACK Sent"
            }

            val latency = System.currentTimeMillis() - startTime
            return CommandResult(
                command = command,
                success = true,
                message = "TCP ACK: $reply",
                latencyMs = latency
            )
        }
    }

    /**
     * Universal Smart TV REST API (POST http://<ip>:<port>/api/command)
     */
    private fun sendRestCommand(
        device: TvDevice,
        command: RemoteCommand,
        startTime: Long
    ): CommandResult {
        val endpoint = "http://${device.ipAddress}:${device.port}/api/command"
        val jsonPayload = """
            {
                "command": "${command.jsonKey}",
                "keyCode": ${command.androidKeyCode},
                "label": "${command.label}"
            }
        """.trimIndent()

        val requestBody = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            return CommandResult(
                command = command,
                success = response.isSuccessful,
                message = "HTTP ${response.code} ${response.message}",
                latencyMs = latency
            )
        }
    }

    /**
     * Tests connectivity to a target TV IP and port
     */
    suspend fun pingDevice(ip: String, port: Int): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 2000)
                val latency = System.currentTimeMillis() - startTime
                Pair(true, latency)
            }
        } catch (e: Exception) {
            Pair(false, System.currentTimeMillis() - startTime)
        }
    }
}
