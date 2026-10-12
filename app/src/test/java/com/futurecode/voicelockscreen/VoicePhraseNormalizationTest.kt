package com.futurecode.voicelockscreen

import com.futurecode.voicelockscreen.ui.afterlogin.VoiceSetupFragment
import org.junit.Assert.assertEquals
import org.junit.Test

class VoicePhraseNormalizationTest {
    @Test fun punctuationAndWhitespaceDoNotBreakConfirmation() {
        assertEquals("hey unlock my screen",VoiceSetupFragment.normalizePhrase("  Hey,  unlock my screen!  "))
    }
    @Test fun nonEnglishPasswordsRemainUsable() {
        assertEquals("میرا فون کھولو",VoiceSetupFragment.normalizePhrase("میرا فون کھولو!"))
        assertEquals("मेरा फोन खोलो",VoiceSetupFragment.normalizePhrase("मेरा फोन खोलो"))
    }
    @Test fun punctuationAloneCannotBecomeAPassword() {
        assertEquals("",VoiceSetupFragment.normalizePhrase(" !!! "))
    }
}
