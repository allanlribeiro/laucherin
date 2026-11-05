package tv.cleanyour.core.ops

import tv.cleanyour.core.model.RawShell
import tv.cleanyour.core.model.ShellLine

/** Fixed allowlisted call; no titles, dates, URIs or other caller text enter a shell command. */
object WidgetDataLines {
    @OptIn(RawShell::class)
    fun commitAgenda(): ShellLine = ShellLine("content call --uri content://app.launcherin.tv.theme/agenda.pending.json --method commitAgenda")
}
