package com.example.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

enum class AtvV2Status {
    CONNECTED,
    NEEDS_PAIRING,
    UNREACHABLE,
    ERROR
}

/**
 * Native implementation of the Google TV / Android TV Remote Protocol v2
 * (TLS on Port 6467 for Pairing and Port 6466 for Remote Control).
 * Zero third-party protobuf/crypto dependencies — keeps APK size under 5MB.
 */
object AndroidTvRemoteV2Client {

    private const val TAG = "AndroidTvRemoteV2"
    private const val KEY_ALIAS = "SmartTvRemoteAtvV2Key"
    private const val PREF_NAME = "atv_v2_paired_prefs"
    private const val PAIRING_PORT = 6467
    private const val CONTROL_PORT = 6466

    private var appContext: Context? = null

    // Active pairing session state
    private var activePairingSocket: SSLSocket? = null
    private var activeServerCert: X509Certificate? = null
    private var activePairingIp: String? = null

    // Active control session cache
    private var activeControlSocket: SSLSocket? = null
    private var activeControlIp: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isPaired(ip: String): Boolean {
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("paired_$ip", false)
    }

    private fun markPaired(ip: String, paired: Boolean) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("paired_$ip", paired)
            .apply()
    }

    @Synchronized
    private fun createSslContext(): Pair<SSLContext, X509Certificate?> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_NONE)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSubject(X500Principal("CN=SmartTvRemote"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, null)

        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, trustAll, SecureRandom())
        val clientCert = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
        return sslContext to clientCert
    }

    /**
     * Checks if Port 6466 / 6467 is open and whether the TV is already paired or needs a PIN.
     */
    suspend fun testConnection(ip: String): Pair<AtvV2Status, String> = withContext(Dispatchers.IO) {
        try {
            val connected = ensureControlSession(ip)
            if (connected) {
                markPaired(ip, true)
                return@withContext AtvV2Status.CONNECTED to "সফল! Google / Android TV কানেক্টেড (Port 6466)"
            }
        } catch (e: Exception) {
            Log.d(TAG, "Control port 6466 test error on $ip: ${e.message}")
        }

        // Check if pairing port 6467 is open
        return@withContext try {
            val socket = java.net.Socket()
            socket.connect(InetSocketAddress(ip, PAIRING_PORT), 1200)
            socket.close()
            AtvV2Status.NEEDS_PAIRING to "টিভি পাওয়া গেছে! স্ক্রিনে আসা ৬ অক্ষরের পিন কোড দিয়ে পেয়ার করুন।"
        } catch (e: Exception) {
            AtvV2Status.UNREACHABLE to "Port 6466/6467 unreachable on $ip"
        }
    }

    /**
     * Initiates Google TV / Android TV Pairing on Port 6467 so the TV displays the 6-hex-char code.
     */
    suspend fun startPairing(ip: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        closeActivePairing()
        try {
            val (sslContext, _) = createSslContext()
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 10000
            socket.connect(InetSocketAddress(ip, PAIRING_PORT), 2500)
            socket.startHandshake()

            val serverCert = socket.session.peerCertificates.firstOrNull() as? X509Certificate
                ?: return@withContext false to "টিভি সার্টিফিকেট পাওয়া যায়নি।"

            val out = socket.outputStream
            val input = socket.inputStream

            // 1. Send PairingRequest (field 10)
            val pairingReq = buildProtoMessage {
                writeString(1, "androidtvremote2")
                writeString(2, "Smart TV Remote")
            }
            sendFramedMessage(out, buildOuterPairing(10, pairingReq))

            // Read PairingRequestAck (field 11)
            val ack1 = readFramedMessage(input) ?: return@withContext false to "TV pairing request rejected"
            if (!hasStatusOk(ack1)) {
                return@withContext false to "TV declined pairing request"
            }

            // 2. Send PairingOption (field 20): HEXADECIMAL (type=3), length=6, role=INPUT (1)
            val encodingMsg = buildProtoMessage {
                writeVarint(1, 3) // ENCODING_TYPE_HEXADECIMAL
                writeVarint(2, 6) // 6 hex characters
            }
            val optionMsg = buildProtoMessage {
                writeBytes(1, encodingMsg) // input_encodings
                writeVarint(3, 1) // ROLE_TYPE_INPUT
            }
            sendFramedMessage(out, buildOuterPairing(20, optionMsg))

            val ack2 = readFramedMessage(input) ?: return@withContext false to "No option ack from TV"
            if (!hasStatusOk(ack2)) {
                return@withContext false to "TV rejected pairing options"
            }

            // 3. Send PairingConfiguration (field 30)
            val configMsg = buildProtoMessage {
                writeBytes(1, encodingMsg) // encoding
                writeVarint(2, 1) // ROLE_TYPE_INPUT
            }
            sendFramedMessage(out, buildOuterPairing(30, configMsg))

            val ack3 = readFramedMessage(input) ?: return@withContext false to "No configuration ack from TV"
            if (!hasStatusOk(ack3)) {
                return@withContext false to "TV did not display pairing PIN"
            }

            activePairingSocket = socket
            activeServerCert = serverCert
            activePairingIp = ip
            true to "টিভির স্ক্রিনে ৬ অক্ষরের একটি কোড দেখা যাচ্ছে—সেটি নিচে লিখুন।"
        } catch (e: Exception) {
            Log.w(TAG, "Pairing start error on $ip: ${e.message}")
            closeActivePairing()
            false to "পেয়ারিং শুরু করা যায়নি: ${e.localizedMessage ?: "Connection failed"}"
        }
    }

    /**
     * Completes pairing using the 6-character hex code displayed on the TV screen.
     */
    suspend fun completePairing(ip: String, code: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length != 6 || !cleanCode.all { it in '0'..'9' || it in 'A'..'F' }) {
            return@withContext false to "টিভির স্ক্রিনে দেখানো ৬ অক্ষরের কোড সঠিকভাবে লিখুন (যেমন: A1B2C3)।"
        }

        val socket = activePairingSocket
        val serverCert = activeServerCert
        if (socket == null || socket.isClosed || serverCert == null || activePairingIp != ip) {
            return@withContext false to "পেয়ারিং সেশন শেষ হয়ে গেছে। আবার চেষ্টা করুন।"
        }

        return@withContext try {
            val (_, clientCert) = createSslContext()
            val clientPub = clientCert?.publicKey as? RSAPublicKey
                ?: return@withContext false to "Client RSA key unavailable"
            val serverPub = serverCert.publicKey as? RSAPublicKey
                ?: return@withContext false to "TV RSA key unavailable"

            val codeBytes = hexToBytes(cleanCode) // 3 bytes: [checkByte, nonce0, nonce1]
            val nonce = codeBytes.copyOfRange(1, 3)

            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(stripLeadingZero(clientPub.modulus.toByteArray()))
            digest.update(stripLeadingZero(clientPub.publicExponent.toByteArray()))
            digest.update(stripLeadingZero(serverPub.modulus.toByteArray()))
            digest.update(stripLeadingZero(serverPub.publicExponent.toByteArray()))
            digest.update(nonce)
            val secretHash = digest.digest()

            if (secretHash[0] != codeBytes[0]) {
                return@withContext false to "কোডটি মেলেনি! টিভির স্ক্রিনের কোড দেখে আবার লিখুন।"
            }

            // Send PairingSecret (field 40)
            val secretMsg = buildProtoMessage {
                writeBytes(1, secretHash)
            }
            sendFramedMessage(socket.outputStream, buildOuterPairing(40, secretMsg))

            val ack = readFramedMessage(socket.inputStream)
            closeActivePairing()

            if (ack != null && hasStatusOk(ack)) {
                markPaired(ip, true)
                ensureControlSession(ip)
                true to "সফল! টিভির সাথে পেয়ারিং সম্পন্ন হয়েছে।"
            } else {
                false to "টিভি কোডটি গ্রহণ করেনি। আবার চেষ্টা করুন।"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pairing finish error: ${e.message}")
            closeActivePairing()
            false to "পেয়ারিং ব্যর্থ হয়েছে: ${e.localizedMessage ?: "Error"}"
        }
    }

    fun cancelPairing() {
        closeActivePairing()
    }

    private fun closeActivePairing() {
        try {
            activePairingSocket?.close()
        } catch (_: Exception) {}
        activePairingSocket = null
        activeServerCert = null
        activePairingIp = null
    }

    @Synchronized
    private fun closeControlSession() {
        try {
            activeControlSocket?.close()
        } catch (_: Exception) {}
        activeControlSocket = null
        activeControlIp = null
    }

    @Synchronized
    private fun ensureControlSession(ip: String): Boolean {
        val existing = activeControlSocket
        if (existing != null && !existing.isClosed && existing.isConnected && activeControlIp == ip) {
            return true
        }
        closeControlSession()

        return try {
            val (sslContext, _) = createSslContext()
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 2500
            socket.connect(InetSocketAddress(ip, CONTROL_PORT), 2000)
            socket.startHandshake()

            val input = socket.inputStream
            val out = socket.outputStream

            // Read TV's RemoteConfigure and reply with client's RemoteConfigure
            val tvMsg1 = readFramedMessage(input) ?: run {
                socket.close()
                return false
            }
            handleIncomingControlPackets(out, tvMsg1)

            val devInfo = buildProtoMessage {
                writeString(1, "Android")
                writeString(2, "SmartTvRemote")
                writeVarint(3, 1)
                writeString(4, "1")
                writeString(5, "com.aistudio.tvremote")
                writeString(6, "1.0")
            }
            val configureMsg = buildProtoMessage {
                writeVarint(1, 622)
                writeBytes(2, devInfo)
            }
            sendFramedMessage(out, buildProtoMessage { writeBytes(1, configureMsg) })

            // Read RemoteSetActive or ping and reply with RemoteSetActive
            try {
                val tvMsg2 = readFramedMessage(input)
                if (tvMsg2 != null) {
                    handleIncomingControlPackets(out, tvMsg2)
                }
            } catch (_: Exception) {}

            val setActive = buildProtoMessage { writeVarint(1, 622) }
            sendFramedMessage(out, buildProtoMessage { writeBytes(2, setActive) })

            activeControlSocket = socket
            activeControlIp = ip
            markPaired(ip, true)
            true
        } catch (e: Exception) {
            Log.d(TAG, "ensureControlSession($ip) failed: ${e.message}")
            closeControlSession()
            false
        }
    }

    private fun handleIncomingControlPackets(out: OutputStream, packet: ByteArray) {
        // Check if field 8 (remote_ping_request) is present, and echo field 9 (remote_ping_response)
        val pingVal = extractVarintFieldInSubmessage(packet, outerField = 8, innerField = 1)
        if (pingVal != null) {
            val pong = buildProtoMessage { writeVarint(1, pingVal) }
            sendFramedMessage(out, buildProtoMessage { writeBytes(9, pong) })
        }
    }

    suspend fun sendKeycode(ip: String, keyCode: Int): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        for (attempt in 1..2) {
            try {
                if (!ensureControlSession(ip)) {
                    return@withContext false to "টিভি পেয়ার করা নেই (PIN কোড দিয়ে পেয়ার করুন)"
                }
                val socket = activeControlSocket ?: continue
                val out = socket.outputStream
                val input = socket.inputStream

                // Drain any pending pings before sending key
                try {
                    while (input.available() > 0) {
                        val incoming = readFramedMessage(input) ?: break
                        handleIncomingControlPackets(out, incoming)
                    }
                } catch (_: Exception) {}

                val keyInject = buildProtoMessage {
                    writeVarint(1, keyCode.toLong())
                    writeVarint(2, 3) // SHORT click
                }
                sendFramedMessage(out, buildProtoMessage { writeBytes(10, keyInject) })
                return@withContext true to "Google TV Remote: OK"
            } catch (e: Exception) {
                Log.w(TAG, "sendKeycode attempt $attempt failed on $ip: ${e.message}")
                closeControlSession()
            }
        }
        false to "Google TV সংযোগ বিচ্ছিন্ন হয়েছে। আবার কানেক্ট করুন।"
    }

    suspend fun sendText(ip: String, text: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var anySent = false
        for (ch in text) {
            val code = charToAndroidKeyCode(ch)
            if (code != 0) {
                val res = sendKeycode(ip, code)
                if (!res.first) return@withContext res
                anySent = true
            }
        }
        if (anySent) true to "Typed on TV: \"$text\"" else false to "Unsupported characters"
    }

    private fun charToAndroidKeyCode(c: Char): Int = when (c) {
        in 'a'..'z' -> 29 + (c - 'a')
        in 'A'..'Z' -> 29 + (c - 'A')
        in '0'..'9' -> 7 + (c - '0')
        ' ' -> 62
        '\n' -> 66
        '.' -> 56
        ',' -> 55
        '-' -> 69
        '/' -> 76
        else -> 0
    }

    // --- Minimal Protobuf & Framing Helpers ---

    private fun buildOuterPairing(innerFieldNumber: Int, innerPayload: ByteArray): ByteArray {
        return buildProtoMessage {
            writeVarint(1, 2) // protocol_version = 2
            writeVarint(2, 200) // status = STATUS_OK
            writeBytes(innerFieldNumber, innerPayload)
        }
    }

    private fun buildProtoMessage(block: ProtoWriter.() -> Unit): ByteArray {
        val writer = ProtoWriter()
        writer.block()
        return writer.toByteArray()
    }

    private class ProtoWriter {
        private val baos = ByteArrayOutputStream()

        fun writeVarint(fieldNumber: Int, value: Long) {
            writeRawVarint(((fieldNumber shl 3) or 0).toLong())
            writeRawVarint(value)
        }

        fun writeBytes(fieldNumber: Int, data: ByteArray) {
            writeRawVarint(((fieldNumber shl 3) or 2).toLong())
            writeRawVarint(data.size.toLong())
            baos.write(data)
        }

        fun writeString(fieldNumber: Int, text: String) {
            writeBytes(fieldNumber, text.toByteArray(Charsets.UTF_8))
        }

        private fun writeRawVarint(v: Long) {
            var value = v
            while (true) {
                if ((value and 0x7FL.inv()) == 0L) {
                    baos.write(value.toInt())
                    return
                } else {
                    baos.write(((value.toInt() and 0x7F) or 0x80))
                    value = value ushr 7
                }
            }
        }

        fun toByteArray(): ByteArray = baos.toByteArray()
    }

    private fun sendFramedMessage(out: OutputStream, payload: ByteArray) {
        val frame = ByteArrayOutputStream()
        var len = payload.size
        while (true) {
            if ((len and 0x7F.inv()) == 0) {
                frame.write(len)
                break
            } else {
                frame.write((len and 0x7F) or 0x80)
                len = len ushr 7
            }
        }
        frame.write(payload)
        out.write(frame.toByteArray())
        out.flush()
    }

    private fun readFramedMessage(input: InputStream): ByteArray? {
        var length = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            length = length or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift > 28) return null
        }
        if (length <= 0 || length > 65536) return ByteArray(0)
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val c = input.read(buf, read, length - read)
            if (c == -1) return null
            read += c
        }
        return buf
    }

    private fun hasStatusOk(packet: ByteArray): Boolean {
        // Field 2 is status; if absent it defaults to 0 or 200
        val status = extractVarintField(packet, 2) ?: return true
        return status == 200L || status == 0L
    }

    private fun extractVarintField(data: ByteArray, targetField: Int): Long? {
        var idx = 0
        while (idx < data.size) {
            val (tag, nextIdx) = readVarintFromArray(data, idx) ?: return null
            idx = nextIdx
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()
            when (wireType) {
                0 -> {
                    val (v, afterVal) = readVarintFromArray(data, idx) ?: return null
                    if (fieldNum == targetField) return v
                    idx = afterVal
                }
                2 -> {
                    val (len, afterLen) = readVarintFromArray(data, idx) ?: return null
                    idx = afterLen + len.toInt()
                }
                else -> return null
            }
        }
        return null
    }

    private fun extractVarintFieldInSubmessage(data: ByteArray, outerField: Int, innerField: Int): Long? {
        var idx = 0
        while (idx < data.size) {
            val (tag, nextIdx) = readVarintFromArray(data, idx) ?: return null
            idx = nextIdx
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()
            when (wireType) {
                0 -> {
                    val (_, afterVal) = readVarintFromArray(data, idx) ?: return null
                    idx = afterVal
                }
                2 -> {
                    val (len, afterLen) = readVarintFromArray(data, idx) ?: return null
                    val end = (afterLen + len.toInt()).coerceAtMost(data.size)
                    if (fieldNum == outerField) {
                        val sub = data.copyOfRange(afterLen, end)
                        return extractVarintField(sub, innerField)
                    }
                    idx = end
                }
                else -> return null
            }
        }
        return null
    }

    private fun readVarintFromArray(data: ByteArray, startIdx: Int): Pair<Long, Int>? {
        var idx = startIdx
        var result = 0L
        var shift = 0
        while (idx < data.size) {
            val b = data[idx].toInt() and 0xFF
            idx++
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result to idx
            shift += 7
            if (shift > 63) return null
        }
        return null
    }

    private fun stripLeadingZero(bytes: ByteArray): ByteArray {
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val high = Character.digit(hex[i * 2], 16)
            val low = Character.digit(hex[i * 2 + 1], 16)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
