package com.futurecode.voicelockscreen

import com.futurecode.voicelockscreen.service.CameraVoiceService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraVoiceServiceTest {
    @Test
    fun listensOnSetupAndInBackgroundWithoutTakingOtherScreensMicrophone() {
        assertTrue(listens(appVisible = true, setupVisible = true))
        assertTrue(listens(appVisible = false))
        assertFalse(listens(appVisible = true))
    }

    @Test
    fun keepsListeningAfterCameraOpens() {
        assertTrue(CameraVoiceService.shouldListen(
            running = true, appVisible = true, setupVisible = false,
            screenOn = true, locked = false, cameraVisible = true
        ))
    }

    @Test
    fun skipsListeningOnLockedOrDarkScreenAndWhenServiceIsStopped() {
        assertFalse(listens(appVisible = false, locked = true))
        assertFalse(listens(appVisible = false, screenOn = false))
        assertFalse(listens(appVisible = false, running = false))
    }

    @Test
    fun pausesCameraVoiceCommandsWithoutInternet() {
        assertFalse(CameraVoiceService.shouldListen(
            running = true, appVisible = true, setupVisible = false,
            screenOn = true, locked = false, cameraVisible = true,
            internetAvailable = false
        ))
    }

    private fun listens(
        appVisible: Boolean,
        setupVisible: Boolean = false,
        running: Boolean = true,
        screenOn: Boolean = true,
        locked: Boolean = false
    ) = CameraVoiceService.shouldListen(running, appVisible, setupVisible, screenOn, locked)
}
