package com.futurecode.voicelockscreen

import com.futurecode.voicelockscreen.utils.CameraCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraCommandTest {
    @Test
    fun recognizesAdvertisedCameraPhrases() {
        val commands = mapOf(
            " Open CAMERA! " to CameraCommand.OPEN,
            "Open Photo" to CameraCommand.PHOTO,
            "Open Video" to CameraCommand.VIDEO,
            "Switch photo" to CameraCommand.PHOTO,
            "Switch to video" to CameraCommand.VIDEO,
            "Capture photo" to CameraCommand.CAPTURE,
            "Start video" to CameraCommand.RECORD,
            "Stop video" to CameraCommand.STOP,
            "Capture" to CameraCommand.CAPTURE,
            "Record" to CameraCommand.RECORD,
            "Record Video" to CameraCommand.RECORD,
            "End Record" to CameraCommand.STOP,
            "Stop recording" to CameraCommand.STOP
        )
        commands.forEach { (phrase, command) ->
            assertEquals(command, CameraCommand.fromPhrase(phrase, "open camera"))
        }
    }

    @Test
    fun ignoresLongerPhrasesContainingACommand() {
        assertNull(CameraCommand.fromPhrase("do not open camera", "open camera"))
        assertNull(CameraCommand.fromPhrase("capture the screen", "open camera"))
        assertNull(CameraCommand.fromPhrase("", ""))
    }
}
