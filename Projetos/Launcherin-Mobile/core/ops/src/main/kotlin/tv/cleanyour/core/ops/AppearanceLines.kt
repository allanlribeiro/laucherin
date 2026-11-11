package tv.cleanyour.core.ops
import tv.cleanyour.core.model.*
/** Fixed, provider-guarded calls; no free text enters the shell and no Home restart is involved. */
object AppearanceLines {
 @OptIn(RawShell::class) fun status()=ShellLine("content call --uri content://app.launcherin.tv.theme --method appearanceStatus")
 @OptIn(RawShell::class) fun commit()=ShellLine("content call --uri content://app.launcherin.tv.theme --method commitAppearance")
}
