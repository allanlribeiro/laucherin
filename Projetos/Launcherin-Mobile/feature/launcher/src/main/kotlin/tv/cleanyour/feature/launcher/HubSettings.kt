package tv.cleanyour.feature.launcher

import java.text.Normalizer
import java.util.Locale

enum class HubTab { HOME, PERSONALIZE, WIDGETS, TV }

enum class HubSetting(val tab: HubTab, val title: Int, val description: Int, val keywords: String = "") {
    APPEARANCE(HubTab.PERSONALIZE,R.string.visual_title,R.string.hub_appearance_desc,"cores paleta premium glass minimal tema efeitos acabamento"),
    LAYOUT(HubTab.WIDGETS,R.string.hub_layout,R.string.hub_layout_desc,"ordem posição tamanho mostrar ocultar widgets grade"),
    AGENDA(HubTab.WIDGETS,R.string.hub_agenda,R.string.hub_agenda_desc,"calendário compromisso hoje evento"),
    PHOTOS(HubTab.WIDGETS,R.string.hub_photos,R.string.hub_photos_desc,"porta retratos galeria imagens fotos"),
    SYSTEM(HubTab.WIDGETS,R.string.hub_system,R.string.hub_system_desc,"ram cpu processador memória desempenho"),
    WALLPAPER(HubTab.PERSONALIZE, R.string.hub_wallpaper, R.string.hub_wallpaper_desc, "fundo foto imagem galeria aparência estilo"),
    THEME(HubTab.PERSONALIZE, R.string.hub_theme, R.string.hub_theme_desc, "natal decoração sazonal"),
    TRANSPARENCY(HubTab.PERSONALIZE, R.string.hub_transparency, R.string.hub_transparency_desc, "vidro widgets opacidade"),
    APPS(HubTab.PERSONALIZE, R.string.hub_apps, R.string.hub_apps_desc, "ícones ordem dock aplicativos tamanho"),
    WEATHER(HubTab.WIDGETS, R.string.hub_weather, R.string.hub_weather_desc, "cidade previsão clima localização"),
    SPORTS(HubTab.WIDGETS, R.string.hub_sports, R.string.hub_sports_desc, "times futebol gols notificações favoritos escudos"),
    WATCH(HubTab.WIDGETS, R.string.hub_watch, R.string.hub_watch_desc, "netflix prime disney max histórico assistir youtube tmdb logos imagem chave api"),
    LISTEN(HubTab.WIDGETS, R.string.hub_listen, R.string.hub_listen_desc, "spotify youtube music música álbum play reprodução"),
    SAVER(HubTab.TV, R.string.hub_saver, R.string.hub_saver_desc, "google fotos álbum ambiente inatividade descanso"),
    POWER(HubTab.TV, R.string.hub_power, R.string.hub_power_desc, "desligar dormir temporizador ociosidade standby"),
    REMOTE(HubTab.TV, R.string.hub_remote, R.string.hub_remote_desc, "home controle acessibilidade configurações tv aberta hdmi entradas"),
    TV_DETAILS(HubTab.TV, R.string.hub_tv_details, R.string.hub_tv_details_desc, "nome sala esquecer televisão"),
    UPDATE(HubTab.HOME, R.string.hub_update, R.string.hub_update_desc, "apk instalar versão launcher atualização"),
    TOOLS(HubTab.TV, R.string.hub_tools, R.string.hub_tools_desc, "adb conexão parear diagnóstico limpeza backup restaurar módulos avançado"),
}

/** Accent/case insensitive; every result opens the same destination as its category card. */
internal fun hubSearch(query: String, label: (Int) -> String): List<HubSetting> {
    val words = normalizeHubQuery(query).split(' ').filter(String::isNotBlank)
    if (words.isEmpty()) return emptyList()
    return HubSetting.entries.filter { item ->
        val text = normalizeHubQuery("${label(item.title)} ${label(item.description)} ${item.keywords}")
        words.all(text::contains)
    }
}

private fun normalizeHubQuery(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
