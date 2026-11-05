package tv.cleanyour.core.flows

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.*

data class AgendaEntry(val id: String, val title: String, val date: String, val time: String = "")

object LauncherinAgenda {
    const val MAX = 40
    fun clean(entries: List<AgendaEntry>): List<AgendaEntry> = entries.distinctBy { it.id }.mapNotNull { e ->
        val title = e.title.replace(Regex("[\\p{Cntrl}\\u2028\\u2029]"), " ").trim().take(120)
        if (!Regex("[a-zA-Z0-9_-]{1,64}").matches(e.id) || title.isBlank() ||
            runCatching { LocalDate.parse(e.date) }.isFailure || (e.time.isNotEmpty() && runCatching { LocalTime.parse(e.time) }.isFailure)) null
        else e.copy(title = title, time = e.time.take(5))
    }.sortedWith(compareBy({ it.date }, { it.time }, { it.id })).take(MAX)
    fun encode(entries: List<AgendaEntry>): String = buildJsonObject {
        put("version", 1)
        put("events", JsonArray(clean(entries).map { e -> buildJsonObject {
            put("id", e.id); put("title", e.title); put("date", e.date); put("time", e.time)
        } }))
    }.toString()
    fun decode(text: String): List<AgendaEntry> = runCatching {
        val root = Json.parseToJsonElement(text).jsonObject
        clean(root["events"]!!.jsonArray.mapNotNull { node ->
            val e = node as? JsonObject ?: return@mapNotNull null
            AgendaEntry(e["id"]?.jsonPrimitive?.contentOrNull.orEmpty(), e["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                e["date"]?.jsonPrimitive?.contentOrNull.orEmpty(), e["time"]?.jsonPrimitive?.contentOrNull.orEmpty())
        })
    }.getOrDefault(emptyList())
}

enum class AgendaSync { READY, PENDING, SENDING, SYNCED, FAILED, NEEDS_UPDATE, NEEDS_UNLOCK }
