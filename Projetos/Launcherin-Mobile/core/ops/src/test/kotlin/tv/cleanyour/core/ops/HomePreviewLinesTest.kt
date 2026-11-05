package tv.cleanyour.core.ops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HomePreviewLinesTest {
    @Test fun dreamSettingsUseThePublicActionWithoutMutatingPowerOrSendingKeys() {
        val text=HomePreviewLines.dreamSettings().text
        assertEquals(listOf("am","start","-W","-a","android.settings.DREAM_SETTINGS"),ShellTokens.tokenize(text))
        for(forbidden in listOf("Somnambulator","settings put","force-stop","keyevent","pm grant"))assertFalse(forbidden in text)
    }
}
