package tv.cleanyour.ui.design.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Subject icons use the same 24dp, 1.5px outline as the existing design system. */
object LauncherinIcons {
    val Weather by lazy { outline("Weather", "M4 16a4 4 0 0 1 0-8a5 5 0 0 1 9-2a4 4 0 1 1 4 10H4z", "M6 19l-1 2M12 19l-1 2M18 19l-1 2") }
    val Sports by lazy { outline("Sports", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20z", "M12 7l5 4-2 6H9l-2-6zM12 2v5M2.5 9L7 11M5 19l4-2M19 19l-4-2M21.5 9L17 11") }
    val Watch by lazy { outline("Watch", "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M10 9l6 3-6 3z") }
    val Music by lazy { outline("Music", "M9 17V5l11-2v12M9 8l11-2", "M9 17a3 2.5 0 1 1-6 0a3 2.5 0 1 1 6 0zM20 15a3 2.5 0 1 1-6 0a3 2.5 0 1 1 6 0z") }
    val Picture by lazy { outline("Picture", "M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M3 17l6-6 4 4 3-3 5 5M16 6a2 2 0 1 1 0 4a2 2 0 1 1 0-4z") }
    val Transparency by lazy { outline("Transparency", "M12 2l10 6-10 6L2 8zM2 12l10 6 10-6M2 16l10 6 10-6") }
    val Timer by lazy { outline("Timer", "M12 5a8 8 0 1 1 0 16a8 8 0 1 1 0-16zM9 2h6M12 5V2M18 6l2-2M12 9v5l3 2") }
    val Theme by lazy { outline("Theme", "M12 2l3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1z") }
    val Apps by lazy { outline("Apps", "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z") }

    private fun outline(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { path ->
            builder.addPath(PathParser().parsePathString(path).toNodes(), fill = null,
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
        return builder.build()
    }
}
