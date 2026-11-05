package tv.cleanyour.core.ops

import tv.cleanyour.core.model.RawShell
import tv.cleanyour.core.model.ShellLine

/** Public settings action. Android 14's Somnambulator is a no-op, not a preview API. */
object HomePreviewLines {
    @OptIn(RawShell::class)
    fun dreamSettings(): ShellLine = ShellLine(ShellTokens.line(listOf("am", "start", "-W", "-a", "android.settings.DREAM_SETTINGS")))
}
