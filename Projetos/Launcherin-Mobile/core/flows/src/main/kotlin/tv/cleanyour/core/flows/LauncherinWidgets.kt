package tv.cleanyour.core.flows

import kotlinx.serialization.json.*

data class WidgetSlot(val id: String, val x: Int, val y: Int, val w: Int = 3, val h: Int = 1, val enabled: Boolean = true)

object LauncherinWidgets {
    const val KEY = "dashboard_widgets"
    const val COLUMNS = 12
    const val ROWS = 2
    val ids = listOf("clock", "weather", "music", "sports", "agenda", "photos", "system")
    fun defaults(): List<WidgetSlot> = ids.mapIndexed { i, id -> WidgetSlot(id, i % 4 * 3, if (i < 4) 0 else 1, enabled = i < 4) }
    fun encode(slots: List<WidgetSlot>): JsonArray = JsonArray(slots.map { s -> buildJsonObject {
        put("id", s.id); put("x", s.x); put("y", s.y); put("w", s.w); put("h", s.h); put("enabled", s.enabled)
    } })
    fun decode(value: String): List<WidgetSlot> = runCatching { clean(Json.parseToJsonElement(value) as? JsonArray) }.getOrElse { defaults() }
    fun clean(array: JsonArray?): List<WidgetSlot> {
        if (array == null) return defaults()
        val out = mutableListOf<WidgetSlot>(); val seen = mutableSetOf<String>()
        for (item in array.take(32)) {
            val o = item as? JsonObject ?: continue
            val id = o["id"]?.jsonPrimitive?.contentOrNull ?: continue
            if (id !in ids || !seen.add(id)) continue
            val width = o["w"]?.jsonPrimitive?.intOrNull ?: 3
            val height = o["h"]?.jsonPrimitive?.intOrNull ?: 1
            val size = (width to height).takeIf { it in listOf(3 to 1, 4 to 1, 6 to 2) } ?: (3 to 1)
            var s = WidgetSlot(id, (o["x"]?.jsonPrimitive?.intOrNull ?: 0).coerceIn(0, COLUMNS-size.first),
                (o["y"]?.jsonPrimitive?.intOrNull ?: 0).coerceIn(0, ROWS-size.second), size.first, size.second,
                o["enabled"]?.jsonPrimitive?.booleanOrNull ?: true)
            if (out.any { overlap(it, s) }) s = firstFree(out, s) ?: s.copy(enabled = false)
            out += s
        }
        out += ids.filterNot(seen::contains).map { WidgetSlot(it, 0, 0, enabled = false) }
        return out
    }
    fun overlap(a: WidgetSlot, b: WidgetSlot) = a.enabled && b.enabled && a.x < b.x+b.w && a.x+a.w > b.x && a.y < b.y+b.h && a.y+a.h > b.y
    private fun firstFree(slots: List<WidgetSlot>, value: WidgetSlot): WidgetSlot? {
        for (y in 0..ROWS-value.h) for (x in 0..COLUMNS-value.w) {
            val next = value.copy(x=x, y=y)
            if (slots.none { overlap(it, next) }) return next
        }
        return null
    }
    /** Failed edits are rejected rather than silently hiding some other widget. */
    fun change(slots: List<WidgetSlot>, value: WidgetSlot, findSpace: Boolean = false): List<WidgetSlot>? {
        if (value.id !in ids || (value.w to value.h) !in listOf(3 to 1, 4 to 1, 6 to 2)) return null
        val others = slots.filterNot { it.id == value.id }
        val valid = value.x in 0..COLUMNS-value.w && value.y in 0..ROWS-value.h && others.none { overlap(it,value) }
        val next = if (valid || !value.enabled) value else if (findSpace) firstFree(others, value) ?: return null else return null
        return slots.map { if (it.id == value.id) next else it }
    }
}
