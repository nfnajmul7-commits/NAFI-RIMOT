package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TvRemoteRepository
import com.example.model.RemoteCommand
import com.example.model.TvDevice
import com.example.model.TvProtocol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart TV Remote", appName)
    }

    @Test
    fun `launch MainActivity without crash`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        assertNotNull(activity)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test repository sends command and updates virtual TV state`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)

        val repository = TvRemoteRepository(context, scope)

        // Ensure initial device is connected
        val connectedDevice = repository.connectedDevice.value
        assertNotNull(connectedDevice)
        assertTrue(connectedDevice?.isVirtual == true)

        // Initial volume is 30
        assertEquals(30, repository.virtualTvState.value.volume)

        // Send Volume Up
        val volUpResult = repository.executeCommand(RemoteCommand.VOLUME_UP)
        assertTrue(volUpResult.success)
        assertEquals(35, repository.virtualTvState.value.volume)

        // Send Mute
        val muteResult = repository.executeCommand(RemoteCommand.MUTE)
        assertTrue(muteResult.success)
        assertTrue(repository.virtualTvState.value.isMuted)

        // Send Power Toggle
        assertTrue(repository.virtualTvState.value.isPoweredOn)
        repository.executeCommand(RemoteCommand.POWER)
        assertFalse(repository.virtualTvState.value.isPoweredOn)

        // Add manual TV
        val manualDevice = repository.addManualDevice(
            name = "Living Room Roku",
            ip = "192.168.1.100",
            port = 8060,
            protocol = TvProtocol.ROKU_ECP
        )
        assertEquals("Living Room Roku", manualDevice.name)
        assertEquals("192.168.1.100", manualDevice.ipAddress)
        assertEquals(8060, manualDevice.port)
        assertEquals(manualDevice.id, repository.connectedDevice.value?.id)
    }
}
