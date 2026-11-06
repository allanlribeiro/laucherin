package tv.cleanyour.ui.design.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.LookDetails
import tv.cleanyour.core.flows.LookEdit
import tv.cleanyour.ui.design.R

/** Focused editors share the same reducers and config transport as the complete Look editor. */
enum class LauncherinPanel { WEATHER, SPORTS, TRANSPARENCY, THEME, SAVER, POWER, WATCH, LAYOUT }

@Composable
fun ColumnScope.LauncherinSettingsPanel(panel: LauncherinPanel, details: LookDetails, onEdit: (LookEdit) -> Unit) {
    when (panel) {
        LauncherinPanel.LAYOUT -> LauncherinWidgetEditor(details.widgetLayout ?: tv.cleanyour.core.flows.LauncherinWidgets.defaults()) { onEdit(LookEdit.SetWidgetLayout(it)) }
        LauncherinPanel.WATCH -> { LookRadioList(listOf(
            RadioChoice("above",stringResource(R.string.watch_above),stringResource(R.string.watch_above_hint)),
            RadioChoice("below",stringResource(R.string.watch_below),stringResource(R.string.watch_below_hint)),
            RadioChoice("hidden",stringResource(R.string.watch_hidden),stringResource(R.string.watch_hidden_hint)),
        ),details.watchNextMode ?: "above",{onEdit(LookEdit.SetWatchNextMode(it))})
            ListGroup(Modifier.padding(top = 14.dp)) {
                LookSwitchRow(stringResource(R.string.tmdb_artwork), details.tmdbArtwork != false,
                    { onEdit(LookEdit.SetTmdbArtwork(it)) }, description = stringResource(R.string.tmdb_artwork_hint))
            }
            val key = details.tmdbApiKey.orEmpty()
            CytTextField(key, { onEdit(LookEdit.SetTmdbApiKey(it)) }, stringResource(R.string.tmdb_key),
                modifier = Modifier.padding(top = 14.dp), password = true,
                error = if (key.isNotEmpty() && !Regex("[a-fA-F0-9]{32}").matches(key)) stringResource(R.string.tmdb_key_error) else null,
                supporting = stringResource(R.string.tmdb_key_hint))
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.tmdb_logo), contentDescription = "TMDB", modifier = Modifier.padding(top = 12.dp))
            LookBody(stringResource(R.string.tmdb_credit), Modifier.padding(top = 12.dp))
        }
        LauncherinPanel.WEATHER -> {
            CytTextField(details.weatherCityQuery.orEmpty(), { onEdit(LookEdit.SetWeatherCityQuery(it)) },
                stringResource(R.string.launcherin_weather_city), supporting = stringResource(R.string.launcherin_weather_city_hint))
        }
        LauncherinPanel.SPORTS -> {
            LauncherinTeamsField(details.favoriteTeams.orEmpty()) { onEdit(LookEdit.SetFavoriteTeams(it)) }
            ListGroup(Modifier.padding(top = 14.dp)) {
                LookSwitchRow(stringResource(R.string.launcherin_goal_alerts), details.goalAlerts == true,
                    { onEdit(LookEdit.SetGoalAlerts(it)) }, description = stringResource(R.string.launcherin_goal_alerts_hint))
            }
        }
        LauncherinPanel.TRANSPARENCY -> {
            ListGroup {
                LookSliderRow(stringResource(R.string.launcherin_widget_transparency), details.widgetTransparency ?: 82,
                    0..100, 5, { onEdit(LookEdit.SetWidgetTransparency(it)) }, valueText = "${details.widgetTransparency ?: 82}%")
            }
            LookBody(stringResource(R.string.launcherin_widget_transparency_desc), Modifier.padding(top = 12.dp))
        }
        LauncherinPanel.THEME -> LookRadioList(listOf(
            RadioChoice("none", stringResource(R.string.launcherin_season_none), stringResource(R.string.launcherin_season_none_desc)),
            RadioChoice("christmas", stringResource(R.string.launcherin_season_christmas), stringResource(R.string.launcherin_season_christmas_desc)),
        ), details.seasonalTheme ?: "none", { onEdit(LookEdit.SetSeasonalTheme(it)) })
        LauncherinPanel.SAVER -> ListGroup {
            LookSliderRow(stringResource(R.string.launcherin_idle_saver), details.idleScreensaverMinutes ?: 0,
                0..120, 5, { onEdit(LookEdit.SetIdleScreensaver(it)) }, valueText = minutes(details.idleScreensaverMinutes ?: 0))
        }
        LauncherinPanel.POWER -> ListGroup {
            LookSliderRow(stringResource(R.string.launcherin_idle_standby), details.idleStandbyMinutes ?: 0,
                0..240, 5, { onEdit(LookEdit.SetIdleStandby(it)) }, valueText = minutes(details.idleStandbyMinutes ?: 0))
        }
    }
}

@Composable
private fun minutes(value: Int) = if (value == 0) stringResource(R.string.launcherin_timer_off) else "$value min"
