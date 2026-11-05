package tv.cleanyour.ui.design.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.LookEdit
import tv.cleanyour.core.spec.HighlightChoice
import tv.cleanyour.core.spec.LookKeys
import tv.cleanyour.ui.design.R

// Advanced (prototype pageAdv): Lift and zoom (shown per LookGates.liftShown), the Highlight
// (home screen 3.0, glass_style), the See-through
// amount, Sky strength, Show launcher names, the settings file with Copy (the full file, dock apps
// too) and the "Not in this app" line. Everything here goes to the TV with the look; a tap only
// chooses.
@Composable
internal fun ColumnScope.LookAdvancedPage(state: LookEditorState, onEdit: (LookEdit) -> Unit, onCopied: (String) -> Unit) {
    val keys = state.keyNames
    LookBody(stringResource(R.string.cyt_look_adv_lead), Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp))
    LookSublabel(stringResource(R.string.launcherin_season_title))
    LookRadioList(
        listOf(
            RadioChoice("none", stringResource(R.string.launcherin_season_none), stringResource(R.string.launcherin_season_none_desc)),
            RadioChoice("christmas", stringResource(R.string.launcherin_season_christmas), stringResource(R.string.launcherin_season_christmas_desc)),
        ), state.details.seasonalTheme ?: "none", { onEdit(LookEdit.SetSeasonalTheme(it)) },
        keyName = "seasonal_theme".takeIf { keys },
    )
    LookBody(stringResource(R.string.launcherin_season_hint), Modifier.padding(bottom = 14.dp))
    ListGroup(Modifier.padding(bottom = 14.dp)) {
        LookSliderRow(
            stringResource(R.string.launcherin_widget_transparency), state.details.widgetTransparency ?: 82,
            0..100, 5, { onEdit(LookEdit.SetWidgetTransparency(it)) },
            keyName = "widget_transparency".takeIf { keys },
        )
    }
    LookBody(stringResource(R.string.launcherin_widget_transparency_desc), Modifier.padding(bottom = 14.dp))
    CytTextField(
        value = state.details.weatherCityQuery.orEmpty(),
        onValueChange = { onEdit(LookEdit.SetWeatherCityQuery(it)) },
        label = stringResource(R.string.launcherin_weather_city),
        supporting = stringResource(R.string.launcherin_weather_city_hint),
        modifier = Modifier.padding(bottom = 14.dp),
    )
    LauncherinTeamsField(state.details.favoriteTeams.orEmpty()) { onEdit(LookEdit.SetFavoriteTeams(it)) }
    ListGroup(Modifier.padding(bottom = 14.dp)) {
        LookSwitchRow(stringResource(R.string.launcherin_goal_alerts), state.details.goalAlerts == true,
            { onEdit(LookEdit.SetGoalAlerts(it)) }, description = stringResource(R.string.launcherin_goal_alerts_hint))
    }
    LookSublabel(stringResource(R.string.launcherin_idle_title))
    ListGroup(Modifier.padding(bottom = 14.dp)) {
        LookSliderRow(stringResource(R.string.launcherin_idle_saver), state.details.idleScreensaverMinutes ?: 0,
            0..120, 5, { onEdit(LookEdit.SetIdleScreensaver(it)) },
            valueText = "${state.details.idleScreensaverMinutes ?: 0} min")
        LookSliderRow(stringResource(R.string.launcherin_idle_standby), state.details.idleStandbyMinutes ?: 0,
            0..240, 5, { onEdit(LookEdit.SetIdleStandby(it)) },
            valueText = "${state.details.idleStandbyMinutes ?: 0} min")
    }
    LookBody(stringResource(R.string.launcherin_idle_hint), Modifier.padding(bottom = 14.dp))
    val r = state.rows
    if (r != null) {
        if (r.lift.visible) {
            ListGroup {
                LookSwitchRow(
                    stringResource(R.string.cyt_look_lift), r.lift.enabled && r.lift.value == true, { onEdit(LookEdit.SetLift(it)) },
                    description = stringResource(R.string.cyt_look_lift_desc), enabled = r.lift.enabled,
                    lines = listOfNotNull(r.lift.reason?.takeUnless { r.lift.enabled }?.let { reasonText(it) }),
                    warning = r.liftSlowRamMb?.let { lookMsg(R.string.cyt_look_lift_low_memory, "size" to memoryWords(it)) },
                    keyName = KEY_NAMES["lift"].takeIf { keys },
                )
            }
        }
        val x = ExtraRows(state)
        if (x.highlight) {
            LookSublabel(stringResource(R.string.cyt_look_highlight))
            LookRadioList(
                listOf(
                    RadioChoice(HighlightChoice.GLASS.id, stringResource(R.string.cyt_look_highlight_glass), stringResource(R.string.cyt_look_highlight_glass_desc)),
                    RadioChoice(HighlightChoice.RIM.id, stringResource(R.string.cyt_look_highlight_rim), stringResource(R.string.cyt_look_highlight_rim_desc)),
                    RadioChoice(HighlightChoice.HALO.id, stringResource(R.string.cyt_look_highlight_halo), stringResource(R.string.cyt_look_highlight_halo_desc)),
                ),
                x.acting.highlight.id, { id -> HighlightChoice.entries.firstOrNull { it.id == id }?.let { h -> x.edit(onEdit) { copy(highlight = h) } } },
                keyName = KEY_NAMES["highlight"].takeIf { keys },
            )
        }
        val seeTitle = stringResource(R.string.cyt_look_see_amount)
        LookSublabel(seeTitle)
        val options = LookKeys.SEE_THROUGH.map { v ->
            val label = lookMsg(R.string.cyt_look_percent, "value" to v)
            SegmentOption(v.toString(), label, label)
        }
        val seeReason = r.seeThrough.reason?.let { reasonText(it) }
        SegmentedChoice(
            options, (r.seeThrough.value ?: LookKeys.SEE_THROUGH_DEFAULT).toString(), { onEdit(LookEdit.SetSeeThrough(it.key.toInt())) },
            enabled = r.seeThrough.enabled, disabledReason = seeReason, groupLabel = seeTitle,
        )
        LookBody(
            if (r.seeThrough.enabled) stringResource(R.string.cyt_look_see_amount_desc) else seeReason.orEmpty(),
            Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
        )
        if (keys) KeyNameLine(KEY_NAMES.getValue("see"), Modifier.padding(start = 4.dp))
        ListGroup(Modifier.padding(top = 14.dp)) {
            LookSliderRow(
                stringResource(R.string.cyt_look_sky_strength), r.skyStrength.value ?: LookKeys.SKY_STRENGTH_DEFAULT, 20..100, 5,
                { onEdit(LookEdit.SetSkyStrength(it)) }, enabled = r.skyStrength.enabled,
                reason = r.skyStrength.reason?.let { reasonText(it) }, keyName = KEY_NAMES["skyStr"].takeIf { keys },
            )
        }
    }
    ListGroup(Modifier.padding(top = 14.dp)) {
        LookSwitchRow(
            stringResource(R.string.cyt_look_key_names), keys, { onEdit(LookEdit.SetKeyNames(it)) },
            description = stringResource(R.string.cyt_look_key_names_desc), keyName = stringResource(R.string.cyt_look_key_names_phone_only).takeIf { keys },
        )
    }
    LookSection(stringResource(R.string.cyt_look_file_title))
    LookBody(stringResource(R.string.cyt_look_file_desc), Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
    val copy = rememberCopy()
    val title = stringResource(R.string.cyt_look_file_title)
    val copied = stringResource(R.string.cyt_look_file_copied)
    MonospaceBlock(
        state.settingsFile.lines(), Modifier.fillMaxWidth(), copyLabel = stringResource(R.string.cyt_look_file_copy),
        onCopy = { copy(title, state.fullFile); onCopied(copied) },
    )
    LookBody(stringResource(R.string.cyt_look_not_here), Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp).align(Alignment.Start))
}
