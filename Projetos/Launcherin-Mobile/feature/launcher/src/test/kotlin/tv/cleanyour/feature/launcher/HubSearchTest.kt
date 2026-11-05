package tv.cleanyour.feature.launcher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HubSearchTest {
    private fun search(query: String) = hubSearch(query) { "" }
    @Test fun aliasesHandleAccentsCaseAndMultipleWords() {
        assertEquals(listOf(HubSetting.SPORTS), search("FUTEBOL GOLS"))
        assertEquals(listOf(HubSetting.SAVER), search("album google"))
        assertEquals(listOf(HubSetting.WEATHER), search("localizacao"))
        assertEquals(listOf(HubSetting.TRANSPARENCY), search("opacidade"))
        assertEquals(listOf(HubSetting.TOOLS), search("restaurar"))
    }
    @Test fun emptyAndUnknownQueriesHaveNoFalseMatches() {
        assertTrue(search("   ").isEmpty())
        assertTrue(search("asdfgh").isEmpty())
    }
    @Test fun frequentlyUsedSettingsHaveDirectCategories() {
        assertEquals(HubTab.WIDGETS, HubSetting.SPORTS.tab)
        assertEquals(HubTab.WIDGETS, HubSetting.WEATHER.tab)
        assertEquals(HubTab.TV, HubSetting.SAVER.tab)
        assertEquals(HubTab.TV, HubSetting.POWER.tab)
        assertEquals(HubTab.PERSONALIZE, HubSetting.TRANSPARENCY.tab)
    }
}
