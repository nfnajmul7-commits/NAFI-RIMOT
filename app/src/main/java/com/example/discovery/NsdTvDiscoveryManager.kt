package com.example.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.TvDevice
import com.example.model.TvProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NsdTvDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "NsdTvDiscovery"

        val SERVICE_TYPES = listOf(
            "_androidtvremote2._tcp.",
            "_googlecast._tcp.",
            "_roku._tcp.",
            "_http._tcp.",
            "_airplay._tcp."
        )
    }

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var subnetScanJob: Job? = null

    init {
        // Always include the Virtual Simulator TV in the device list so users can test immediately
        val virtualTv = TvDevice(
            id = "virtual_tv_demo",
            name = "Demo Smart TV (Virtual)",
            ipAddress = "127.0.0.1",
            port = 8080,
            protocol = TvProtocol.VIRTUAL_SIMULATOR,
            serviceType = "virtual.tv",
            isVirtual = true
        )
        _discoveredDevices.value = listOf(virtualTv)
    }

    /**
     * Start mDNS / NSD service discovery across Smart TV protocols
     */
    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        acquireMulticastLock()

        // Scan mDNS service types
        if (nsdManager != null) {
            SERVICE_TYPES.forEach { serviceType ->
                try {
                    val listener = createDiscoveryListener(serviceType)
                    activeListeners.add(listener)
                    nsdManager.discoverServices(
                        serviceType,
                        NsdManager.PROTOCOL_DNS_SD,
                        listener
                    )
                    Log.d(TAG, "Started discovery for: $serviceType")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start discovery for $serviceType: ${e.message}")
                }
            }
        }

        // Parallel local subnet scan for silent devices (Roku / Android TV / REST)
        startSubnetScan()
    }

    /**
     * Stop ongoing discovery listeners and release multicast lock
     */
    fun stopDiscovery() {
        if (!_isScanning.value) return
        _isScanning.value = false

        subnetScanJob?.cancel()
        subnetScanJob = null

        if (nsdManager != null) {
            activeListeners.forEach { listener ->
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping discovery listener: ${e.message}")
                }
            }
            activeListeners.clear()
        }

        releaseMulticastLock()
    }

    private fun createDiscoveryListener(serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName} (${serviceInfo.serviceType})")
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                val lostName = serviceInfo.serviceName
                _discoveredDevices.value = _discoveredDevices.value.filterNot { it.name == lostName && !it.isVirtual }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to start for $serviceType with code $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to stop for $serviceType with code $errorCode")
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val nsd = nsdManager ?: return
        try {
            nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}, code $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val name = serviceInfo.serviceName ?: "Smart TV"
                    val type = serviceInfo.serviceType ?: ""

                    val protocol = when {
                        type.contains("androidtvremote") || port == 6466 || port == 6467 -> TvProtocol.ANDROID_TV_REMOTE_V2
                        type.contains("roku") || port == 8060 -> TvProtocol.ROKU_ECP
                        type.contains("androidtv") || port == 5555 -> TvProtocol.ANDROID_TV_TCP
                        else -> TvProtocol.UNIVERSAL_REST
                    }

                    val device = TvDevice(
                        id = "$host:$port",
                        name = cleanDeviceName(name),
                        ipAddress = host,
                        port = port,
                        protocol = protocol,
                        serviceType = type,
                        isVirtual = false
                    )

                    addDiscoveredDevice(device)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Exception resolving service: ${e.message}")
        }
    }

    private fun cleanDeviceName(raw: String): String {
        return raw.replace("\\032", " ")
            .replace("-", " ")
            .trim()
    }

    private fun addDiscoveredDevice(device: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == device.ipAddress && it.port == device.port }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    /**
     * Fast parallel scan of the entire local Wi-Fi subnet (1..254) for any Android TV,
     * Google TV, China TV Box, Chromecast, or Roku device. Completes in ~2 seconds.
     */
    suspend fun scanSubnetNow(): List<TvDevice> = withContext(Dispatchers.IO) {
        val found = java.util.Collections.synchronizedList(mutableListOf<TvDevice>())
        val localIp = getLocalWifiIp()
        val prefix = if (localIp != null && localIp.contains(".")) {
            localIp.substringBeforeLast(".")
        } else {
            "192.168.0"
        }

        val semaphore = kotlinx.coroutines.sync.Semaphore(48)
        kotlinx.coroutines.coroutineScope {
            val jobs = (1..254).map { lastOctet ->
                val targetIp = "$prefix.$lastOctet"
                this.async {
                    if (targetIp == localIp) return@async
                    semaphore.acquire()
                    try {
                        val device = checkCommonTvPorts(targetIp, timeoutMs = 280)
                        if (device != null) {
                            found.add(device)
                            addDiscoveredDevice(device)
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.forEach { it.await() }
        }
        found.toList()
    }

    /**
     * Probes local network IP addresses in parallel for common TV & TV Box ports
     */
    private fun startSubnetScan() {
        subnetScanJob?.cancel()
        subnetScanJob = scope.launch(Dispatchers.IO) {
            try {
                scanSubnetNow()
            } catch (e: Exception) {
                Log.w(TAG, "Subnet scan error: ${e.message}")
            }
        }
    }

    private fun checkCommonTvPorts(ip: String, timeoutMs: Int = 280): TvDevice? {
        // 1. Android TV / China TV Box ADB Port 5555
        if (isPortOpen(ip, 5555, timeoutMs)) {
            return TvDevice(
                id = "$ip:5555",
                name = "Android / China TV Box ($ip)",
                ipAddress = ip,
                port = 5555,
                protocol = TvProtocol.ANDROID_TV_TCP,
                serviceType = "android:adb_tcp"
            )
        }

        // 2. Google TV / Android TV Remote v2 Port 6466 or 6467
        if (isPortOpen(ip, 6466, timeoutMs) || isPortOpen(ip, 6467, timeoutMs)) {
            return TvDevice(
                id = "$ip:6466",
                name = "Google / Android TV ($ip)",
                ipAddress = ip,
                port = 6466,
                protocol = TvProtocol.ANDROID_TV_REMOTE_V2,
                serviceType = "_androidtvremote2._tcp."
            )
        }

        // 3. Roku TV Port 8060
        if (isPortOpen(ip, 8060, timeoutMs)) {
            return TvDevice(
                id = "$ip:8060",
                name = "Roku TV ($ip)",
                ipAddress = ip,
                port = 8060,
                protocol = TvProtocol.ROKU_ECP,
                serviceType = "roku:ecp"
            )
        }

        // 4. China TV Box Custom Socket Port 55555
        if (isPortOpen(ip, 55555, timeoutMs)) {
            return TvDevice(
                id = "$ip:55555",
                name = "China TV Box ($ip)",
                ipAddress = ip,
                port = 55555,
                protocol = TvProtocol.ANDROID_TV_TCP,
                serviceType = "chinatv:socket"
            )
        }

        // 5. Chromecast / Universal REST Port 8008 or 8080
        if (isPortOpen(ip, 8008, timeoutMs)) {
            return TvDevice(
                id = "$ip:8008",
                name = "Chromecast / Android Box ($ip)",
                ipAddress = ip,
                port = 8008,
                protocol = TvProtocol.UNIVERSAL_REST,
                serviceType = "googlecast:dial"
            )
        }

        if (isPortOpen(ip, 8080, timeoutMs)) {
            return TvDevice(
                id = "$ip:8080",
                name = "Smart TV Bridge ($ip)",
                ipAddress = ip,
                port = 8080,
                protocol = TvProtocol.UNIVERSAL_REST,
                serviceType = "smarttv:rest"
            )
        }

        return null
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getLocalWifiIp(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return try {
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt == 0) null
            else {
                String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getSubnetPrefix(): String {
        val ip = getLocalWifiIp()
        return if (ip != null && ip.contains(".")) {
            ip.substringBeforeLast(".") + "."
        } else {
            "192.168.1."
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("SmartTvMulticastLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
            Log.d(TAG, "Multicast lock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "Multicast lock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release multicast lock: ${e.message}")
        }
    }
}
