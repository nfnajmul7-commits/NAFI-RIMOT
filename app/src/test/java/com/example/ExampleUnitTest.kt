package com.example

import com.example.model.RemoteCommand
import com.example.model.TvDevice
import com.example.model.TvProtocol
import com.example.model.VirtualTvState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testKeycodeMappings() {
        assertEquals(24, RemoteCommand.VOLUME_UP.androidKeyCode)
        assertEquals(25, RemoteCommand.VOLUME_DOWN.androidKeyCode)
        assertEquals(164, RemoteCommand.MUTE.androidKeyCode)
        assertEquals(26, RemoteCommand.POWER.androidKeyCode)
        assertEquals(19, RemoteCommand.DPAD_UP.androidKeyCode)
        assertEquals(20, RemoteCommand.DPAD_DOWN.androidKeyCode)
        assertEquals(21, RemoteCommand.DPAD_LEFT.androidKeyCode)
        assertEquals(22, RemoteCommand.DPAD_RIGHT.androidKeyCode)
        assertEquals(23, RemoteCommand.OK.androidKeyCode)
        assertEquals(4, RemoteCommand.BACK.androidKeyCode)
        assertEquals(3, RemoteCommand.HOME.androidKeyCode)
    }

    @Test
    fun testRokuKeyMappings() {
        assertEquals("VolumeUp", RemoteCommand.VOLUME_UP.rokuKey)
        assertEquals("VolumeDown", RemoteCommand.VOLUME_DOWN.rokuKey)
        assertEquals("VolumeMute", RemoteCommand.MUTE.rokuKey)
        assertEquals("Power", RemoteCommand.POWER.rokuKey)
        assertEquals("Select", RemoteCommand.OK.rokuKey)
    }

    @Test
    fun testTvProtocolDefaults() {
        assertEquals(8060, TvProtocol.ROKU_ECP.defaultPort)
        assertEquals(5555, TvProtocol.ANDROID_TV_TCP.defaultPort)
        assertEquals(8080, TvProtocol.UNIVERSAL_REST.defaultPort)
    }

    @Test
    fun testTvDeviceCreation() {
        val device = TvDevice(
            id = "192.168.1.50:8060",
            name = "Bedroom Roku TV",
            ipAddress = "192.168.1.50",
            port = 8060,
            protocol = TvProtocol.ROKU_ECP
        )
        assertEquals("192.168.1.50", device.ipAddress)
        assertEquals(8060, device.port)
        assertFalse(device.isVirtual)
    }

    @Test
    fun testVirtualTvState() {
        val state = VirtualTvState()
        assertTrue(state.isPoweredOn)
        assertEquals(30, state.volume)
        assertFalse(state.isMuted)
    }
}
