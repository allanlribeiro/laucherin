package tv.cleanyour.core.ops
import tv.cleanyour.core.model.*
/** Only fixed paired provider calls. Names and IDs travel as JSON, never shell. */
object MusicProfileLines {
 @OptIn(RawShell::class) fun status()=ShellLine("content call --uri content://app.launcherin.tv.theme --method musicProfilesStatus")
 @OptIn(RawShell::class) fun commit(hash:Sha256)=ShellLine("content call --uri content://app.launcherin.tv.theme --method commitMusicProfile --arg ${hash.hex}")
}
