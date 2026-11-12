package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.CommandResult
import com.example.model.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class BluetoothStatus {
    UNSUPPORTED,
    DISABLED,
    REGISTERING,
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Native Bluetooth HID (Human Interface Device) Remote Manager.
 * Emulates a real wireless Bluetooth TV Remote Control accessory.
 * Connects directly through Android TV's "Remotes & Accessories" menu.
 */
class BluetoothRemoteManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BluetoothRemote"
        private const val REPORT_ID_KEYBOARD: Byte = 1
        private const val REPORT_ID_CONSUMER: Byte = 2

        // Standard USB/Bluetooth HID Descriptor for TV Remote (Keyboard + Consumer Controls)
        private val HID_REPORT_DESC = byteArrayOf(
            // Keyboard Collection (Report ID 1) - 8 bytes
            0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
            0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0xe0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
            0x29.toByte(), 0xe7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs) - Modifier bits
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x81.toByte(), 0x01.toByte(), //   INPUT (Cnst,Ary,Abs) - Reserved
            0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x65.toByte(), //   LOGICAL_MAXIMUM (101)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
            0x29.toByte(), 0x65.toByte(), //   USAGE_MAXIMUM (Keyboard Application)
            0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs) - 6 Keycodes
            0xc0.toByte(),               // END_COLLECTION

            // Consumer Control Collection (Report ID 2) - 2 bytes
            0x05.toByte(), 0x0c.toByte(), // USAGE_PAGE (Consumer Devices)
            0x09.toByte(), 0x01.toByte(), // USAGE (Consumer Control)
            0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xff.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023)
            0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Unassigned)
            0x2a.toByte(), 0xff.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
            0x75.toByte(), 0x10.toByte(), //   REPORT_SIZE (16)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
            0xc0.toByte()                // END_COLLECTION
        )
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedTvDevice: BluetoothDevice? = null

    private val _status = MutableStateFlow(BluetoothStatus.DISCONNECTED)
    val status: StateFlow<BluetoothStatus> = _status.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()

    init {
        initHidProfile()
    }

    @SuppressLint("MissingPermission")
    fun initHidProfile() {
        if (bluetoothAdapter == null) {
            _status.value = BluetoothStatus.UNSUPPORTED
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _status.value = BluetoothStatus.DISABLED
            return
        }

        try {
            bluetoothAdapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                            hidDevice = proxy
                            registerHidApp()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            hidDevice = null
                            _status.value = BluetoothStatus.DISCONNECTED
                        }
                    }
                },
                BluetoothProfile.HID_DEVICE
            )
        } catch (e: Exception) {
            Log.w(TAG, "HID Device Profile not supported on this phone: ${e.message}")
            _status.value = BluetoothStatus.UNSUPPORTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val hid = hidDevice ?: return
        _status.value = BluetoothStatus.REGISTERING

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Smart TV Remote",
            "Wireless Bluetooth Remote Control",
            "Android",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HID_REPORT_DESC
        )

        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                Log.d(TAG, "HID App registered=$registered, device=$pluggedDevice")
                if (registered) {
                    if (pluggedDevice != null) {
                        connectedTvDevice = pluggedDevice
                        _connectedDeviceName.value = pluggedDevice.name ?: pluggedDevice.address
                        _status.value = BluetoothStatus.CONNECTED
                    } else {
                        _status.value = BluetoothStatus.DISCONNECTED
                    }
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                Log.d(TAG, "HID connection state changed: $state for ${device?.address}")
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedTvDevice = device
                        _connectedDeviceName.value = device?.name ?: device?.address
                        _status.value = BluetoothStatus.CONNECTED
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _status.value = BluetoothStatus.CONNECTING
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (connectedTvDevice?.address == device?.address) {
                            connectedTvDevice = null
                            _connectedDeviceName.value = null
                            _status.value = BluetoothStatus.DISCONNECTED
                        }
                    }
                }
            }
        }

        try {
            hid.registerApp(sdpSettings, null, qos, executor, callback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register HID app: ${e.message}")
        }
    }

    /**
     * Makes the phone discoverable to the TV box in "Remotes & Accessories"
     */
    fun makePhoneDiscoverable() {
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request discoverable: ${e.message}")
        }
    }

    /**
     * Send Remote Command via Bluetooth HID
     */
    @SuppressLint("MissingPermission")
    suspend fun sendCommand(command: RemoteCommand): CommandResult {
        val hid = hidDevice
        val tv = connectedTvDevice

        if (hid == null) {
            return CommandResult(
                command = command,
                success = false,
                message = "ব্লুটুথ HID সাপোর্ট নেই বা সক্রিয় নয়।",
                latencyMs = 0
            )
        }

        if (tv == null || _status.value != BluetoothStatus.CONNECTED) {
            return CommandResult(
                command = command,
                success = false,
                message = "টিভির সাথে ব্লুটুথ সংযুক্ত নেই। 'Remotes & Accessories' এ পেয়ার করুন।",
                latencyMs = 0
            )
        }

        val startTime = System.currentTimeMillis()

        return try {
            when (command) {
                // Consumer Controls (Report ID 2)
                RemoteCommand.VOLUME_UP -> sendConsumerKey(tv, 0x00E9)
                RemoteCommand.VOLUME_DOWN -> sendConsumerKey(tv, 0x00EA)
                RemoteCommand.MUTE -> sendConsumerKey(tv, 0x00E2)
                RemoteCommand.POWER -> sendConsumerKey(tv, 0x0030)
                RemoteCommand.PLAY_PAUSE -> sendConsumerKey(tv, 0x00CD)
                RemoteCommand.FAST_FORWARD -> sendConsumerKey(tv, 0x00B5)
                RemoteCommand.REWIND -> sendConsumerKey(tv, 0x00B6)
                RemoteCommand.STOP -> sendConsumerKey(tv, 0x00B7)
                RemoteCommand.HOME -> sendConsumerKey(tv, 0x0223) // AC Home
                RemoteCommand.BACK -> sendConsumerKey(tv, 0x0224) // AC Back
                RemoteCommand.MENU -> sendConsumerKey(tv, 0x0040) // Menu

                // Keyboard Controls (Report ID 1)
                RemoteCommand.DPAD_UP -> sendKeyboardKey(tv, 0x52.toByte())
                RemoteCommand.DPAD_DOWN -> sendKeyboardKey(tv, 0x51.toByte())
                RemoteCommand.DPAD_LEFT -> sendKeyboardKey(tv, 0x50.toByte())
                RemoteCommand.DPAD_RIGHT -> sendKeyboardKey(tv, 0x4F.toByte())
                RemoteCommand.OK -> sendKeyboardKey(tv, 0x28.toByte()) // Return/Enter
                RemoteCommand.ENTER -> sendKeyboardKey(tv, 0x28.toByte())
                RemoteCommand.BACKSPACE -> sendKeyboardKey(tv, 0x2A.toByte())
                RemoteCommand.SPACE -> sendKeyboardKey(tv, 0x2C.toByte())

                // Numbers 0-9
                RemoteCommand.NUM_1 -> sendKeyboardKey(tv, 0x1E.toByte())
                RemoteCommand.NUM_2 -> sendKeyboardKey(tv, 0x1F.toByte())
                RemoteCommand.NUM_3 -> sendKeyboardKey(tv, 0x20.toByte())
                RemoteCommand.NUM_4 -> sendKeyboardKey(tv, 0x21.toByte())
                RemoteCommand.NUM_5 -> sendKeyboardKey(tv, 0x22.toByte())
                RemoteCommand.NUM_6 -> sendKeyboardKey(tv, 0x23.toByte())
                RemoteCommand.NUM_7 -> sendKeyboardKey(tv, 0x24.toByte())
                RemoteCommand.NUM_8 -> sendKeyboardKey(tv, 0x25.toByte())
                RemoteCommand.NUM_9 -> sendKeyboardKey(tv, 0x26.toByte())
                RemoteCommand.NUM_0 -> sendKeyboardKey(tv, 0x27.toByte())

                else -> sendKeyboardKey(tv, 0x28.toByte())
            }

            CommandResult(
                command = command,
                success = true,
                message = "Bluetooth: ${command.label} OK",
                latencyMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            CommandResult(
                command = command,
                success = false,
                message = e.localizedMessage ?: "Bluetooth send failed",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Send Consumer Key (Report ID 2, 2 bytes little-endian)
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendConsumerKey(target: BluetoothDevice, usageCode: Int) {
        val hid = hidDevice ?: return
        val report = byteArrayOf(
            (usageCode and 0xFF).toByte(),
            ((usageCode shr 8) and 0xFF).toByte()
        )
        val release = byteArrayOf(0x00, 0x00)

        hid.sendReport(target, REPORT_ID_CONSUMER.toInt(), report)
        delay(25)
        hid.sendReport(target, REPORT_ID_CONSUMER.toInt(), release)
    }

    /**
     * Send Keyboard Key (Report ID 1, 8 bytes: modifier, reserved, 6 keycodes)
     */
    @SuppressLint("MissingPermission")
    private suspend fun sendKeyboardKey(target: BluetoothDevice, keyCode: Byte, modifier: Byte = 0) {
        val hid = hidDevice ?: return
        val report = byteArrayOf(modifier, 0x00, keyCode, 0x00, 0x00, 0x00, 0x00, 0x00)
        val release = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        hid.sendReport(target, REPORT_ID_KEYBOARD.toInt(), report)
        delay(25)
        hid.sendReport(target, REPORT_ID_KEYBOARD.toInt(), release)
    }

    /**
     * Type text over Bluetooth HID Keyboard
     */
    @SuppressLint("MissingPermission")
    suspend fun sendTypedText(text: String): CommandResult {
        val tv = connectedTvDevice ?: return CommandResult(
            command = RemoteCommand.OK,
            success = false,
            message = "TV Not connected via Bluetooth",
            latencyMs = 0
        )

        val startTime = System.currentTimeMillis()
        for (char in text) {
            val (keyCode, modifier) = charToHidKeycode(char)
            if (keyCode != 0.toByte()) {
                sendKeyboardKey(tv, keyCode, modifier)
                delay(15)
            }
        }

        return CommandResult(
            command = RemoteCommand.OK,
            success = true,
            message = "Typed over Bluetooth: \"$text\"",
            latencyMs = System.currentTimeMillis() - startTime
        )
    }

    private fun charToHidKeycode(c: Char): Pair<Byte, Byte> {
        return when (c) {
            in 'a'..'z' -> (0x04 + (c - 'a')).toByte() to 0.toByte()
            in 'A'..'Z' -> (0x04 + (c - 'A')).toByte() to 0x02.toByte() // Left Shift
            in '1'..'9' -> (0x1E + (c - '1')).toByte() to 0.toByte()
            '0' -> 0x27.toByte() to 0.toByte()
            ' ' -> 0x2C.toByte() to 0.toByte()
            '\n' -> 0x28.toByte() to 0.toByte()
            '.' -> 0x37.toByte() to 0.toByte()
            else -> 0.toByte() to 0.toByte()
        }
    }

    /**
     * Get bonded TV devices
     */
    @SuppressLint("MissingPermission")
    fun getBondedTvDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.filter {
                val name = it.name?.lowercase() ?: ""
                name.contains("tv") || name.contains("box") || name.contains("remote") || it.bluetoothClass?.deviceClass == 1060
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Check if necessary Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Connect to an already paired TV device
     */
    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice): Boolean {
        return try {
            hidDevice?.connect(device) ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect to ${device.address}: ${e.message}")
            false
        }
    }

    /**
     * Connect to a TV device by its Bluetooth MAC address (e.g. b4:04:18:97:7b:c3)
     */
    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String): Boolean {
        val hid = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        return try {
            val device = adapter.getRemoteDevice(address)
            hid.connect(device)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect to address $address: ${e.message}")
            false
        }
    }
}
