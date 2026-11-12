package tv.cleanyour.core.ops
import tv.cleanyour.core.model.*
/** Fixed routes; the user-selected profile id is only JSON, never interpolated into shell. */
object MusicVisualLines {
 @OptIn(RawShell::class) fun status()=ShellLine("content call --uri content://app.launcherin.tv.theme --method musicVisualStatus")
 @OptIn(RawShell::class) fun commit()=ShellLine("content call --uri content://app.launcherin.tv.theme --method commitMusicVisual")
}
