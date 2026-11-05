package tv.cleanyour.core.flows

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tv.cleanyour.core.spec.BundledThemeSource
import tv.cleanyour.core.spec.StyleId
import tv.cleanyour.testing.fixtures.FakeClock
import tv.cleanyour.testing.fixtures.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherinWidgetTransparencyTest {
    private val themes = BundledThemeSource()
    private val drafts = LookDrafts(themes, themes, ConfigEncoder.Codec, FakeClock(Samples.NOW_MS))
    private val context = LookContext(tv = LookTv(9L, 2048))
    private fun saved() = HomeChoice(emptyList(), null, style = StyleId.GLASS.id, picture = LookPicture.NONE_ID)
    private fun amount(draft: LookDraft): Int? = Json.parseToJsonElement(draft.fullFile)
        .jsonObject["widget_transparency"]?.jsonPrimitive?.int
    private fun season(draft: LookDraft) = Json.parseToJsonElement(draft.fullFile).jsonObject["seasonal_theme"]?.jsonPrimitive?.content

    @Test fun goalAndIdleOptionsSurviveReopeningAndTravelToTv() {
        var chosen = drafts.open(saved(), context)
        chosen = drafts.edit(chosen, LookEdit.SetGoalAlerts(true), context)
        chosen = drafts.edit(chosen, LookEdit.SetIdleScreensaver(5), context)
        chosen = drafts.edit(chosen, LookEdit.SetIdleStandby(30), context)
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        val reopened = drafts.open(HomePrefs.decode(HomePrefs.encode(choice)), context)
        val json = Json.parseToJsonElement(reopened.fullFile).jsonObject
        assertEquals(1, json.getValue("sports_goal_alerts").jsonPrimitive.int)
        assertEquals(5, json.getValue("idle_screensaver_minutes").jsonPrimitive.int)
        assertEquals(30, json.getValue("idle_standby_minutes").jsonPrimitive.int)
        val styled = drafts.edit(reopened, LookEdit.SelectStyle(StyleId.CLASSIC), context)
        assertEquals(reopened.details.goalAlerts, styled.details.goalAlerts)
        assertEquals(reopened.details.idleStandbyMinutes, styled.details.idleStandbyMinutes)
    }
    @Test fun idleEditsCannotScheduleStandbyBeforeTheSaver() {
        var chosen = drafts.open(saved(), context)
        chosen = drafts.edit(chosen, LookEdit.SetIdleStandby(10), context)
        chosen = drafts.edit(chosen, LookEdit.SetIdleScreensaver(15), context)
        assertEquals(20, chosen.details.idleStandbyMinutes)
        chosen = drafts.edit(chosen, LookEdit.SetIdleStandby(5), context)
        assertEquals(0, chosen.details.idleScreensaverMinutes)
    }
    @Test fun resettingAppearanceDoesNotDisableGoalAlertsOrIdleTimers() {
        val initial = drafts.open(saved().copy(details = LookDetails(widgetTransparency = 60,
            goalAlerts = true, idleScreensaverMinutes = 5, idleStandbyMinutes = 30)), context)
        val reset = drafts.edit(initial, LookEdit.ResetDetails, context)
        assertEquals(true, reset.details.goalAlerts)
        assertEquals(5, reset.details.idleScreensaverMinutes)
        assertEquals(30, reset.details.idleStandbyMinutes)
    }
    @Test fun disablingGoalAlertsIsExplicitAndUndoRestoresIt() {
        val initial = drafts.open(saved().copy(details = LookDetails(goalAlerts = true)), context)
        val edited = drafts.edit(initial, LookEdit.SetGoalAlerts(false), context)
        assertEquals(0, Json.parseToJsonElement(edited.fullFile).jsonObject.getValue("sports_goal_alerts").jsonPrimitive.int)
        assertEquals(initial.fullFile, drafts.edit(edited, LookEdit.UndoChanges, context).fullFile)
    }
    @Test fun christmasChoiceIsSavedReopenedAndSentToTheTv() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetSeasonalTheme("christmas"), context)
        assertEquals("christmas", season(chosen))
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        assertEquals("christmas", season(drafts.open(HomePrefs.decode(HomePrefs.encode(choice)), context)))
    }
    @Test fun decorativeThemeKeepsTheWallpaperAndOtherWidgetChoicesAcrossStyleChanges() {
        var chosen = drafts.open(saved().copy(details = LookDetails(widgetTransparency = 67, weatherCityQuery = "Serra, ES", favoriteTeams = listOf("819", "2029"))), context)
        chosen = drafts.edit(chosen, LookEdit.SetSeasonalTheme("christmas"), context)
        chosen = drafts.edit(chosen, LookEdit.SelectStyle(StyleId.CLASSIC), context)
        assertEquals("christmas", season(chosen));assertEquals(67, amount(chosen))
        assertEquals("Serra, ES", chosen.details.weatherCityQuery);assertEquals(listOf("819", "2029"), chosen.details.favoriteTeams)
        // Changing the season itself does not choose another photo.
        val after = drafts.edit(chosen, LookEdit.SetSeasonalTheme("none"), context)
        assertEquals(chosen.picture, after.picture)
    }
    @Test fun standardExplicitlyRemovesDecorationsAndUndoRestoresChristmas() {
        val initial = drafts.open(saved().copy(details = LookDetails(seasonalTheme = "christmas")), context)
        val standard = drafts.edit(initial, LookEdit.SetSeasonalTheme("none"), context)
        assertEquals("none", season(standard))
        assertEquals(initial.fullFile, drafts.edit(standard, LookEdit.UndoChanges, context).fullFile)
    }
    @Test fun unknownSeasonCannotInjectSavedSettings() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetSeasonalTheme("christmas\ncolumns=4"), context)
        assertEquals("none", season(chosen))
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        assertEquals("none", HomePrefs.decode(HomePrefs.encode(choice)).details.seasonalTheme)
    }

    @Test fun phoneChoiceSurvivesStorageAndIsSentAsWidgetOnlyTransparency() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetWidgetTransparency(65), context)
        assertEquals(65, amount(chosen))
        assertTrue(chosen.changed)
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        val restored = HomePrefs.decode(HomePrefs.encode(choice))
        assertEquals(65, amount(drafts.open(restored, context)))
        val json = Json.parseToJsonElement(chosen.fullFile).jsonObject
        assertEquals(Json.parseToJsonElement(chosen.fullFile).jsonObject["dock_opacity"],
            Json.parseToJsonElement(drafts.open(saved(), context).fullFile).jsonObject["dock_opacity"])
        assertEquals(65, json.getValue("widget_transparency").jsonPrimitive.int)
    }
    @Test fun undoRestoresPreviousTransparencyWithoutSendingAnything() {
        val initial = drafts.open(saved().copy(details = LookDetails(widgetTransparency = 40)), context)
        val edited = drafts.edit(initial, LookEdit.SetWidgetTransparency(100), context)
        val undone = drafts.edit(edited, LookEdit.UndoChanges, context)
        assertEquals(initial.fullFile, undone.fullFile)
        assertEquals(40, amount(undone))
    }
    @Test fun extremesStayInRangeAndStyleChangesKeepTheChoice() {
        var draft = drafts.edit(drafts.open(saved(), context), LookEdit.SetWidgetTransparency(-20), context)
        assertEquals(0, amount(draft))
        draft = drafts.edit(draft, LookEdit.SetWidgetTransparency(120), context)
        assertEquals(100, amount(draft))
        draft = drafts.edit(draft, LookEdit.SelectStyle(StyleId.CLASSIC), context)
        assertEquals(100, amount(draft))
    }
    @Test fun weatherCityTravelsThroughTheSameLookAndSurvivesReopening() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetWeatherCityQuery("Serra, ES"), context)
        val json = Json.parseToJsonElement(chosen.fullFile).jsonObject
        assertEquals("Serra, ES", json.getValue("weather_city_query").jsonPrimitive.content)
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        val reopened = drafts.open(HomePrefs.decode(HomePrefs.encode(choice)), context)
        assertEquals("Serra, ES", reopened.details.weatherCityQuery)
    }
    @Test fun cityTextCannotInjectAnotherSavedSetting() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetWeatherCityQuery("Serra\nstyle=glass\r\u2028"), context)
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        val encoded = HomePrefs.encode(choice)
        assertEquals(1, encoded.lines().count { it.startsWith("style=") })
        assertEquals(choice.details.weatherCityQuery, HomePrefs.decode(encoded).details.weatherCityQuery)
    }
    @Test fun twoFavoriteTeamsTravelAsAJsonArrayAndSurviveReopening() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetFavoriteTeams(listOf("819", "2029")), context)
        val json = Json.parseToJsonElement(chosen.fullFile).jsonObject
        assertEquals("[\"819\",\"2029\"]", json.getValue("sports_favorite_teams").toString())
        val choice = drafts.choiceOf(chosen, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        val reopened = drafts.open(HomePrefs.decode(HomePrefs.encode(choice)), context)
        assertEquals(listOf("819", "2029"), reopened.details.favoriteTeams)
    }
    @Test fun invalidDuplicateAndThirdTeamsCannotEnterTheSavedConfig() {
        val chosen = drafts.edit(drafts.open(saved(), context), LookEdit.SetFavoriteTeams(listOf("invalid\nstyle=glass", "819", "819", "2029", "874")), context)
        assertEquals(listOf("819", "2029"), chosen.details.favoriteTeams)
        assertEquals(LauncherinTeams.all.size, LauncherinTeams.all.map { it.id }.toSet().size)
    }
    @Test fun clearingTeamsIsExplicitAndUndoRestoresTheEarlierChoice() {
        val initial = drafts.open(saved().copy(details = LookDetails(favoriteTeams = listOf("819"))), context)
        val cleared = drafts.edit(initial, LookEdit.SetFavoriteTeams(emptyList()), context)
        assertEquals("[]", Json.parseToJsonElement(cleared.fullFile).jsonObject.getValue("sports_favorite_teams").toString())
        val choice = drafts.choiceOf(cleared, saved(), emptyList(), true, LookPicture.NONE_ID, null)
        assertEquals(emptyList(), HomePrefs.decode(HomePrefs.encode(choice)).details.favoriteTeams)
        assertEquals(initial.fullFile, drafts.edit(cleared, LookEdit.UndoChanges, context).fullFile)
    }

}
