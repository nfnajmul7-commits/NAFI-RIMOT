package com.example.network

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

enum class AdbAuthStatus {
    CONNECTED,
    WAITING_FOR_USER_AUTH,
    PORT_UNREACHABLE,
    ERROR
}

/**
 * Production-grade native ADB (Android Debug Bridge) protocol client over Wi-Fi TCP (Port 5555).
 * Enables direct control of Android TVs and TV Boxes (TX3, TX9, X96, Mi Box, Tanix, Walton,
 * TCL, Sony, etc.) with RSA key negotiation, AUTH token handling, and shell command execution.
 */
object AdbTvClient {

    private const val TAG = "AdbTvClient"
    private const val CONNECT_TIMEOUT_MS = 2500
    private const val SO_TIMEOUT_MS = 3000

    // ADB Protocol Constants
    private const val A_CNXN = 0x4e584e43 // "CNXN"
    private const val A_AUTH = 0x48545541 // "AUTH"
    private const val A_OPEN = 0x4e45504f // "OPEN"
    private const val A_OKAY = 0x59414b4f // "OKAY"
    private const val A_CLSE = 0x45534c43 // "CLSE"
    private const val A_WRTE = 0x45545257 // "WRTE"

    private const val ADB_VERSION = 0x01000000
    private const val MAX_DATA = 4096

    private const val ADB_AUTH_TOKEN = 1
    private const val ADB_AUTH_SIGNATURE = 2
    private const val ADB_AUTH_RSAPUBLICKEY = 3

    private const val PREF_NAME = "adb_rsa_keystore_prefs"
    private const val KEY_PRIV = "adb_rsa_priv_pkcs8"
    private const val KEY_PUB = "adb_rsa_pub_x509"

    // Cached RSA keypair for ADB authorization across app sessions
    private var cachedKeyPair: KeyPair? = null
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    private fun getOrCreateRsaKeyPair(): KeyPair {
        cachedKeyPair?.let { return it }

        // Try loading persisted keypair from SharedPreferences
        appContext?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
                val privB64 = prefs.getString(KEY_PRIV, null)
                val pubB64 = prefs.getString(KEY_PUB, null)
                if (!privB64.isNullOrBlank() && !pubB64.isNullOrBlank()) {
                    val kf = java.security.KeyFactory.getInstance("RSA")
                    val privKey = kf.generatePrivate(
                        java.security.spec.PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.DEFAULT))
                    )
                    val pubKey = kf.generatePublic(
                        java.security.spec.X509EncodedKeySpec(Base64.decode(pubB64, Base64.DEFAULT))
                    )
                    val loaded = KeyPair(pubKey, privKey)
                    cachedKeyPair = loaded
                    return loaded
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load persisted RSA key: ${e.message}")
            }
        }

        return try {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()
            cachedKeyPair = kp

            // Save generated keypair for future app launches
            appContext?.let { ctx ->
                try {
                    val prefs = ctx.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(KEY_PRIV, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
                        .putString(KEY_PUB, Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
                        .apply()
                } catch (_: Exception) {}
            }
            kp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate RSA key: ${e.message}")
            throw e
        }
    }

    /**
     * Converts an RSAPublicKey into the binary format (struct RSAPublicKey) expected by Android adbd
     */
    private fun buildAdbPublicKeyPayload(pubKey: RSAPublicKey): ByteArray {
        val n = pubKey.modulus
        val e = pubKey.publicExponent

        val buffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(64) // Modulus length in 32-bit words (2048 / 32 = 64)

        // n0inv = -1 / N[0] mod 2^32
        val r32 = BigInteger.ONE.shiftLeft(32)
        val n0 = n.mod(r32)
        val n0inv = n0.modInverse(r32).negate().and(BigInteger.valueOf(0xFFFFFFFFL)).toLong().toInt()
        buffer.putInt(n0inv)

        // Modulus N in little-endian format (256 bytes)
        val nRaw = n.toByteArray()
        val nPadded = ByteArray(256)
        val nSrcStart = if (nRaw.size > 256) nRaw.size - 256 else 0
        val nSrcLen = minOf(nRaw.size, 256)
        val nDestStart = 256 - nSrcLen
        System.arraycopy(nRaw, nSrcStart, nPadded, nDestStart, nSrcLen)
        nPadded.reverse()
        buffer.put(nPadded)

        // RR = (2^4096) mod N in little-endian format (256 bytes)
        val rrBig = BigInteger.ONE.shiftLeft(4096).mod(n)
        val rrRaw = rrBig.toByteArray()
        val rrPadded = ByteArray(256)
        val rrSrcStart = if (rrRaw.size > 256) rrRaw.size - 256 else 0
        val rrSrcLen = minOf(rrRaw.size, 256)
        val rrDestStart = 256 - rrSrcLen
        System.arraycopy(rrRaw, rrSrcStart, rrPadded, rrDestStart, rrSrcLen)
        rrPadded.reverse()
        buffer.put(rrPadded)

        // Exponent (usually 65537)
        buffer.putInt(e.toInt())

        val b64 = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
        return "$b64 SmartTvRemote\u0000".toByteArray(Charsets.UTF_8)
    }

    /**
     * Signs the ADB auth token with the client's RSA private key using PKCS#1 v1.5 padding
     */
    private fun signAuthToken(privKey: RSAPrivateKey, token: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, privKey)
        return cipher.doFinal(token)
    }

    /**
     * Sends an Android keycode (e.g. 24 for VOL+, 26 for POWER, 66 for OK) via ADB shell
     */
    suspend fun sendKeyevent(ip: String, port: Int = 5555, keyCode: Int): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val command = "shell:input keyevent $keyCode\u0000"
            executeAdbShell(ip, port, command)
        }

    /**
     * Sends text input to the active input field on the Android TV Box
     */
    suspend fun sendText(ip: String, port: Int = 5555, text: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val sanitized = text.replace(" ", "%s").replace("'", "\\'")
            val command = "shell:input text $sanitized\u0000"
            executeAdbShell(ip, port, command)
        }

    /**
     * Launch application package on Android TV Box
     */
    suspend fun launchAppPackage(ip: String, port: Int = 5555, packageName: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val command = "shell:monkey -p $packageName -c android.intent.category.LAUNCHER 1\u0000"
            executeAdbShell(ip, port, command)
        }

    /**
     * Tests and performs ADB handshake with detailed diagnostic status
     */
    suspend fun testAdbConnection(ip: String, port: Int = 5555): Triple<Boolean, AdbAuthStatus, String> =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = SO_TIMEOUT_MS
                    socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)

                    val output = socket.getOutputStream()
                    val input = socket.getInputStream()

                    // 1. Send CNXN handshake
                    val connectPayload = "host::SmartTvRemote\u0000".toByteArray(Charsets.UTF_8)
                    sendPacket(output, A_CNXN, ADB_VERSION, MAX_DATA, connectPayload)

                    // 2. Read first response
                    val resp1 = readHeader(input) ?: return@withContext Triple(false, AdbAuthStatus.ERROR, "No response header from TV")

                    if (resp1.cmd == A_CNXN) {
                        return@withContext Triple(true, AdbAuthStatus.CONNECTED, "Connected via ADB (Direct)")
                    }

                    if (resp1.cmd == A_AUTH && resp1.arg0 == ADB_AUTH_TOKEN) {
                        val token = ByteArray(resp1.dataLength)
                        readFully(input, token)

                        val keyPair = getOrCreateRsaKeyPair()
                        // Attempt signature
                        try {
                            val signature = signAuthToken(keyPair.private as RSAPrivateKey, token)
                            sendPacket(output, A_AUTH, ADB_AUTH_SIGNATURE, 0, signature)
                            val sigResp = readHeader(input)
                            if (sigResp != null && sigResp.cmd == A_CNXN) {
                                return@withContext Triple(true, AdbAuthStatus.CONNECTED, "Connected via ADB (Authenticated)")
                            }
                        } catch (_: Exception) {}

                        // Send public key to trigger TV screen dialog
                        val pubKeyPayload = buildAdbPublicKeyPayload(keyPair.public as RSAPublicKey)
                        sendPacket(output, A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, pubKeyPayload)

                        val pubResp = readHeader(input)
                        if (pubResp != null && pubResp.cmd == A_CNXN) {
                            return@withContext Triple(true, AdbAuthStatus.CONNECTED, "Connected via ADB (Key Accepted)")
                        }

                        return@withContext Triple(
                            true,
                            AdbAuthStatus.WAITING_FOR_USER_AUTH,
                            "টিভি স্ক্রিনে 'Allow USB Debugging' ডায়ালগে OK/Allow চাপুন।"
                        )
                    }

                    Triple(true, AdbAuthStatus.CONNECTED, "Port $port active")
                }
            } catch (e: Exception) {
                Log.w(TAG, "ADB test failed for $ip:$port: ${e.message}")
                Triple(
                    false,
                    AdbAuthStatus.PORT_UNREACHABLE,
                    "Port $port unreachable on $ip (${e.localizedMessage ?: "Connection refused"})"
                )
            }
        }

    /**
     * Executes an ADB shell command with full handshake and authentication support
     */
    private fun executeAdbShell(ip: String, port: Int, shellCommand: String): Pair<Boolean, String> {
        return try {
            Socket().use { socket ->
                socket.soTimeout = SO_TIMEOUT_MS
                socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)

                val output = socket.getOutputStream()
                val input = socket.getInputStream()

                // 1. Send CNXN handshake
                val cnxnPayload = "host::SmartTvRemote\u0000".toByteArray(Charsets.UTF_8)
                sendPacket(output, A_CNXN, ADB_VERSION, MAX_DATA, cnxnPayload)

                // 2. Read CNXN or AUTH response
                val cnxnResp = readHeader(input) ?: return Pair(false, "No handshake response from TV")

                var isConnected = (cnxnResp.cmd == A_CNXN)
                if (cnxnResp.cmd == A_AUTH && cnxnResp.arg0 == ADB_AUTH_TOKEN) {
                    val token = ByteArray(cnxnResp.dataLength)
                    readFully(input, token)

                    val keyPair = getOrCreateRsaKeyPair()
                    // Try signature
                    try {
                        val signature = signAuthToken(keyPair.private as RSAPrivateKey, token)
                        sendPacket(output, A_AUTH, ADB_AUTH_SIGNATURE, 0, signature)
                        val sigResp = readHeader(input)
                        if (sigResp != null && sigResp.cmd == A_CNXN) {
                            isConnected = true
                            if (sigResp.dataLength > 0) skipBytes(input, sigResp.dataLength)
                        }
                    } catch (_: Exception) {}

                    // If not connected yet, send public key
                    if (!isConnected) {
                        val pubKeyPayload = buildAdbPublicKeyPayload(keyPair.public as RSAPublicKey)
                        sendPacket(output, A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, pubKeyPayload)
                        val pubResp = readHeader(input)
                        if (pubResp != null && pubResp.cmd == A_CNXN) {
                            isConnected = true
                            if (pubResp.dataLength > 0) skipBytes(input, pubResp.dataLength)
                        } else {
                            return Pair(false, "টিভি স্ক্রিনে 'Allow USB Debugging' অনুমতি দিন।")
                        }
                    }
                } else if (cnxnResp.cmd == A_CNXN && cnxnResp.dataLength > 0) {
                    skipBytes(input, cnxnResp.dataLength)
                }

                // 3. Send OPEN command
                val localId = 1
                val cmdBytes = if (shellCommand.endsWith("\u0000")) {
                    shellCommand.toByteArray(Charsets.UTF_8)
                } else {
                    (shellCommand + "\u0000").toByteArray(Charsets.UTF_8)
                }
                sendPacket(output, A_OPEN, localId, 0, cmdBytes)

                // 4. Wait for OKAY response
                try {
                    val openResp = readHeader(input)
                    if (openResp != null) {
                        if (openResp.dataLength > 0) {
                            skipBytes(input, openResp.dataLength)
                        }
                        if (openResp.cmd == A_OKAY || openResp.cmd == A_WRTE || openResp.cmd == A_CLSE) {
                            return Pair(true, "Command executed: ${shellCommand.replace("\u0000", "").trim()}")
                        }
                    }
                } catch (_: Exception) {}

                Pair(true, "Sent to TV")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ADB Shell command failed for $ip:$port ($shellCommand): ${e.message}")
            Pair(false, e.localizedMessage ?: "Failed to connect to TV at $ip:$port")
        }
    }

    private data class AdbHeader(
        val cmd: Int,
        val arg0: Int,
        val arg1: Int,
        val dataLength: Int,
        val dataCheck: Int,
        val magic: Int
    )

    private fun sendPacket(out: OutputStream, cmd: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(cmd)
        header.putInt(arg0)
        header.putInt(arg1)
        header.putInt(data.size)
        header.putInt(calculateChecksum(data))
        header.putInt(cmd xor -0x1)

        out.write(header.array())
        if (data.isNotEmpty()) {
            out.write(data)
        }
        out.flush()
    }

    private fun readHeader(input: InputStream): AdbHeader? {
        val buffer = ByteArray(24)
        var totalRead = 0
        while (totalRead < 24) {
            val count = input.read(buffer, totalRead, 24 - totalRead)
            if (count == -1) break
            totalRead += count
        }
        if (totalRead < 24) return null

        val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        return AdbHeader(
            cmd = bb.getInt(),
            arg0 = bb.getInt(),
            arg1 = bb.getInt(),
            dataLength = bb.getInt(),
            dataCheck = bb.getInt(),
            magic = bb.getInt()
        )
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val count = input.read(buffer, totalRead, buffer.size - totalRead)
            if (count == -1) break
            totalRead += count
        }
    }

    private fun calculateChecksum(data: ByteArray): Int {
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    private fun skipBytes(input: InputStream, length: Int) {
        var remaining = length
        val buf = ByteArray(1024)
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size)
            val read = input.read(buf, 0, toRead)
            if (read == -1) break
            remaining -= read
        }
    }
}
