package tv.cleanyour.feature.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.HomeScreenIntent
import tv.cleanyour.core.flows.HomeScreenState
import tv.cleanyour.core.flows.HomeTest
import tv.cleanyour.core.flows.HomeTestResult
import tv.cleanyour.ui.design.component.*
import tv.cleanyour.ui.design.icon.CytIcons
import tv.cleanyour.ui.design.icon.LauncherinIcons
import tv.cleanyour.ui.design.theme.CytTheme
import tv.cleanyour.ui.design.theme.Glow

/** Category screens only edit the existing Look draft. UseLook remains the sole settings write. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun LauncherinHubScreen(
    state: HomeScreenState,
    tvName: String,
    connected: Boolean,
    loading: Boolean,
    tvs: List<Pair<String, String>>,
    selectedTv: String?,
    onSelectTv: (String) -> Unit,
    onReconnect: () -> Unit,
    onAddTv: () -> Unit,
    onTools: () -> Unit,
    onTvDetails: () -> Unit,
    onAdvancedHome: () -> Unit,
    onAppSettings: () -> Unit,
    onExit: () -> Unit,
    onIntent: (HomeScreenIntent) -> Unit,
    photos: PhotoPicker? = null,
    initialTab: HubTab = HubTab.HOME,
    thumbnail: ByteArray? = null,
    framePicker: WidgetPhotoPicker? = null,
    musicProfilePicker:MusicProfilePhotoPicker?=null,
) {
    var tabName by rememberSaveable { mutableStateOf(initialTab.name) }
    val tab = HubTab.valueOf(tabName)
    var query by rememberSaveable { mutableStateOf("") }
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingTv by rememberSaveable { mutableStateOf(false) }
    var dockOpen by rememberSaveable { mutableStateOf(false) }
    var fullLook by rememberSaveable { mutableStateOf(false) }
    var musicProfilesPage by rememberSaveable {mutableStateOf(false)}
    var musicVisualPage by rememberSaveable {mutableStateOf(false)}
    var appearancePage by rememberSaveable { mutableStateOf<String?>(null) }
    var sendFailed by rememberSaveable { mutableStateOf(false) }
    var leaving by remember { mutableStateOf<(() -> Unit)?>(null) }
    val draft = state.look
    val pending = draft?.canSend == true || state.lookFailed
    val busy = state.busy != null || state.testing != null || state.importingPhotos || state.appearance.busy || state.musicVisual.busy
    val ready = !loading && state.tvName.isNotBlank() && state.notOnTv == null && !busy
    LaunchedEffect(connected,state.launcherVersionCode) { if(connected)onIntent(HomeScreenIntent.SyncAgenda(false)) }

    fun navigate(action: () -> Unit) {
        if (busy) return
        if (pending) leaving = action else action()
    }
    fun open(item: HubSetting) {
        if (busy) return
        when (item) {
            HubSetting.TOOLS -> navigate(onTools)
            HubSetting.TV_DETAILS -> navigate(onTvDetails)
            HubSetting.REMOTE -> navigate { sectionName = item.name }
            HubSetting.UPDATE -> if (state.updateTo == null) { sectionName = item.name } else navigate { onIntent(HomeScreenIntent.PreviewUpdate) }
            HubSetting.APPS -> navigate { dockOpen = true }
            HubSetting.MUSIC_PROFILES -> musicProfilesPage=true
            HubSetting.MUSIC_VISUAL -> musicVisualPage=true
            HubSetting.APPEARANCE -> appearancePage="appearance"
            HubSetting.WALLPAPER -> appearancePage="wallpaper"
            else -> {
                if ((item.panel() != null || item==HubSetting.PHOTOS) && draft == null) onIntent(HomeScreenIntent.OpenLook)
                sectionName = item.name
            }
        }
    }
    // A failed send retains the original machine's draft, including a staged photo, for retry.
    LaunchedEffect(state.lookFailed, state.lookSending, draft == null) {
        if (state.lookFailed) {
            sendFailed = true
            onIntent(HomeScreenIntent.OpenLook)
        } else if (state.lookSending || draft == null) sendFailed = false
    }
    BackHandler {
        when {
            busy -> Unit
            query.isNotEmpty() -> query = ""
            tab != HubTab.HOME -> tabName = HubTab.HOME.name
            else -> navigate(onExit)
        }
    }
    val context = LocalContext.current
    val results = remember(query, context) { hubSearch(query, context::getString) }
    ScreenScaffold(Glow.Tv,
        topBar = {
            CytTopBar(center = { Text(stringResource(R.string.hub_brand), style = CytTheme.type.titleMedium, color = CytTheme.colors.textPrimary) },
                trailing = { CytIconButton(CytIcons.Settings, stringResource(R.string.hub_app_settings), { navigate(onAppSettings) }) })
        },
        bottomBar = {
            if (pending || state.lookSending) {
                Body(stringResource(if (state.lookSending) R.string.hub_sending else R.string.hub_pending), Modifier.padding(bottom = 8.dp))
                if (pending && state.tvSideSettings) Body(stringResource(R.string.home_tv_changes_note), Modifier.padding(bottom = 8.dp))
                PrimaryButton(stringResource(R.string.hub_apply), { onIntent(HomeScreenIntent.UseLook); sectionName = null },
                    enabled = ready && draft?.canSend == true, loading = state.lookSending)
            }
            HubNavigation(tab, onTab = { tabName = it.name; query = "" }, enabled = !busy)
        },
    ) {
        ScreenTitle(stringResource(tab.title()), Modifier.padding(top = 16.dp))
        Body(stringResource(tab.description()), Modifier.padding(top = 8.dp, bottom = 18.dp))
        CytTextField(query, { query = it }, stringResource(R.string.hub_search),
            supporting = if (query.isEmpty()) stringResource(R.string.hub_search_hint) else null)
        if (!connected || loading) {
            Banner(stringResource(if (loading) R.string.hub_connecting else R.string.hub_offline), BannerTone.NEUTRAL,
                Modifier.padding(top = 14.dp), actionLabel = if (loading) null else stringResource(R.string.hub_reconnect), onAction = onReconnect)
        }
        if (sendFailed || state.lookFailed) Banner(stringResource(R.string.hub_failed), BannerTone.WARNING, Modifier.padding(top = 12.dp))
        if (state.busy != null) Body(stringResource(R.string.hub_working), Modifier.padding(top = 12.dp))
        state.testResult?.let { result ->
            Body(stringResource(when (result) {
                HomeTestResult.REQUESTED -> R.string.hub_test_requested
                HomeTestResult.FAILED -> R.string.hub_test_failed
                HomeTestResult.OFFLINE -> R.string.hub_offline
            }), Modifier.padding(top = 12.dp))
        }
        if (query.isNotBlank()) {
            Body(stringResource(R.string.hub_search_count, results.size), Modifier.padding(top = 20.dp, bottom = 8.dp))
            if (results.isEmpty()) Body(stringResource(R.string.hub_search_empty))
            HubRows(results, enabled = ready, onOpen = ::open)
        } else if (tab == HubTab.HOME) {
            ListGroup(Modifier.padding(top = 20.dp)) {
                ListRow(tvName.ifBlank { stringResource(R.string.hub_no_tv) },
                    subtitle = stringResource(if (connected) R.string.hub_connected else R.string.hub_offline),
                    leading = { CytIcon(CytIcons.Tv, null) }, onClick = if (busy) null else ({ choosingTv = true }))
                (state.lookShot ?: thumbnail)?.let { TvThumbnail(rememberPng(it), stringResource(R.string.hub_preview), Modifier.padding(12.dp), label = null) }
            }
            Body(stringResource(R.string.hub_shortcuts), Modifier.padding(top = 24.dp, bottom = 8.dp))
            HubRows(listOf(HubSetting.WALLPAPER, HubSetting.APPS, HubSetting.SPORTS, HubSetting.SAVER), enabled = ready, onOpen = ::open)
            if (state.updateTo != null) {
                ListGroup(Modifier.padding(top = 16.dp)) {
                    ListRow(stringResource(R.string.hub_update), subtitle = stringResource(R.string.hub_update_available, state.updateTo.orEmpty()),
                        leading = { CytIcon(CytIcons.Download, null) }, onClick = if (ready) ({ open(HubSetting.UPDATE) }) else null)
                }
            } else Body(stringResource(R.string.hub_no_update), Modifier.padding(top = 16.dp))
        } else {
            Body(stringResource(R.string.hub_category_hint), Modifier.padding(top = 20.dp, bottom = 8.dp))
            HubRows(HubSetting.entries.filter { it.tab == tab }, enabled = ready, onOpen = ::open)
        }
        if (state.notOnTv != null) {
            ListGroup(Modifier.padding(top = 16.dp)) {
                ListRow(stringResource(R.string.hub_setup), subtitle = stringResource(R.string.hub_setup_desc), onClick = onAdvancedHome)
            }
        }
        if (!busy && tab != HubTab.TV && query.isBlank()) TertiaryButton(stringResource(R.string.hub_tools), { navigate(onTools) }, Modifier.padding(top = 12.dp))
        Gap(16.dp)
    }

    if(musicProfilesPage)MusicProfilesPanel(state,connected,musicProfilePicker,onIntent,{musicProfilesPage=false})
    if(musicVisualPage)MusicVisualPanel(state,connected,onIntent,{musicVisualPage=false},{onIntent(HomeScreenIntent.PreviewUpdate)})
    appearancePage?.let { page -> AppearancePanel(state,connected,page=="wallpaper",photos,onIntent,{appearancePage=null},{onIntent(HomeScreenIntent.PreviewUpdate)}) }

    if (choosingTv) CytBottomSheet({ choosingTv = false }) {
        SheetHeader(stringResource(R.string.hub_choose_tv))
        ListGroup {
            tvs.forEach { (id, name) ->
                ListRow(name, value = if (id == selectedTv) stringResource(R.string.hub_current) else null,
                    onClick = { if (id == selectedTv) choosingTv = false else navigate { choosingTv = false; onSelectTv(id) } })
            }
        }
        TertiaryButton(stringResource(R.string.hub_add_tv), { navigate { choosingTv = false; onAddTv() } })
    }
    if (dockOpen) DockSheet(state.dock, state.apps, state.columns,
        onDone = { onIntent(HomeScreenIntent.SetDock(it)); dockOpen = false }, onDismiss = { dockOpen = false }, waiting = state.waiting)
    if (fullLook && draft != null && state.busy == null && !state.lookFailed) LookSheet(state, onIntent, photos)
    LaunchedEffect(draft, state.busy) { if (draft == null) fullLook = false }
    val section = sectionName?.let(HubSetting::valueOf)
    if (section != null && state.busy == null) {
        CytBottomSheet({ sectionName = null }) {
            val limit = LocalConfiguration.current.screenHeightDp.dp * 0.78f
            Column(Modifier.fillMaxWidth().heightIn(max = limit)) {
                SheetHeader(stringResource(section.title), subtitle = stringResource(section.description))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    val panel = section.panel()
                    if (panel != null && draft != null) LauncherinSettingsPanel(panel, draft.details) { onIntent(HomeScreenIntent.Look(it)) }
                    if(section in listOf(HubSetting.LAYOUT,HubSetting.PHOTOS,HubSetting.AGENDA,HubSetting.SYSTEM,HubSetting.WATCH) && (state.launcherVersionCode ?: 0)<18) {
                        Body(stringResource(R.string.hub_widgets_update),Modifier.padding(top=12.dp))
                        if(state.updateTo!=null)TertiaryButton(stringResource(R.string.hub_update),{sectionName=null;onIntent(HomeScreenIntent.PreviewUpdate)})
                    }
                    when (section) {
                        HubSetting.AGENDA -> LauncherinAgendaEditor(state.agendaEntries,state.agendaSync,
                            {onIntent(HomeScreenIntent.UpsertAgenda(it))},{onIntent(HomeScreenIntent.RemoveAgenda(it))},{onIntent(HomeScreenIntent.SyncAgenda())})
                        HubSetting.PHOTOS -> {
                            Body(stringResource(R.string.hub_photos_hint),Modifier.padding(top=12.dp))
                            Body(stringResource(R.string.hub_photos_count,draft?.details?.framePhotos.orEmpty().size),Modifier.padding(top=12.dp))
                            if(state.importingPhotos)Body(stringResource(R.string.hub_photos_loading),Modifier.padding(top=12.dp))
                            PrimaryButton(stringResource(R.string.hub_photos_choose),{framePicker?.pick { onIntent(HomeScreenIntent.ImportFramePhotos(it)) }},
                                Modifier.padding(top=12.dp),enabled=framePicker!=null&&!state.importingPhotos)
                            if(!draft?.details?.framePhotos.isNullOrEmpty())TertiaryButton(stringResource(R.string.hub_photos_clear),{onIntent(HomeScreenIntent.Look(tv.cleanyour.core.flows.LookEdit.SetFramePhotos(emptyList())))})
                            TertiaryButton(stringResource(R.string.hub_layout),{open(HubSetting.LAYOUT)})
                        }
                        HubSetting.SYSTEM -> {
                            Body(stringResource(R.string.hub_system_hint),Modifier.padding(top=12.dp))
                            TertiaryButton(stringResource(R.string.hub_layout),{open(HubSetting.LAYOUT)})
                        }
                        HubSetting.SAVER -> {
                            ListGroup(Modifier.padding(top = 16.dp)) {
                                ListRow(stringResource(R.string.hub_saver_source), subtitle = stringResource(R.string.hub_saver_source_value), chevron = false)
                            }
                            Body(stringResource(R.string.hub_google_photos_guide), Modifier.padding(top = 12.dp))
                        }
                        HubSetting.POWER -> Body(stringResource(R.string.hub_power_hint), Modifier.padding(top = 12.dp))
                        HubSetting.WEATHER -> Body(stringResource(R.string.hub_weather_hint), Modifier.padding(top = 12.dp))
                        HubSetting.SPORTS -> Body(stringResource(R.string.hub_sports_hint), Modifier.padding(top = 12.dp))
                        HubSetting.WATCH -> Body(stringResource(R.string.hub_watch_hint), Modifier.padding(top = 12.dp))
                        HubSetting.LISTEN -> Body(stringResource(R.string.hub_listen_hint), Modifier.padding(top = 12.dp))
                        HubSetting.UPDATE -> Body(stringResource(R.string.hub_no_update), Modifier.padding(top = 12.dp))
                        HubSetting.REMOTE -> {
                            Body(stringResource(R.string.hub_remote_hint), Modifier.padding(top = 12.dp))
                            state.helper?.let { HomeHelperCard(it, enabled = !busy, onIntent) }
                            TertiaryButton(stringResource(R.string.hub_remote_more), { navigate(onAdvancedHome) }, Modifier.padding(top = 12.dp))
                        }
                        else -> Unit
                    }
                    if (section in listOf(HubSetting.SAVER, HubSetting.WEATHER, HubSetting.SPORTS, HubSetting.LISTEN, HubSetting.WATCH, HubSetting.REMOTE)) {
                        if (pending) Body(stringResource(R.string.hub_test_pending), Modifier.padding(top = 12.dp))
                        SecondaryButton(stringResource(if (section == HubSetting.SAVER) R.string.hub_test_saver else R.string.hub_open_tv),
                            { onIntent(HomeScreenIntent.TestOnTv(if (section == HubSetting.SAVER) HomeTest.SCREENSAVER else HomeTest.LAUNCHER)) },
                            Modifier.padding(top = 12.dp), enabled = connected && !pending && !busy)
                    }
                    Gap(12.dp)
                }
                if (draft?.canSend == true && section!=HubSetting.AGENDA) {
                    if (state.tvSideSettings) Body(stringResource(R.string.home_tv_changes_note), Modifier.padding(bottom = 8.dp))
                    PrimaryButton(stringResource(R.string.hub_apply), { onIntent(HomeScreenIntent.UseLook); sectionName = null }, enabled = ready)
                }
                TertiaryButton(stringResource(R.string.hub_back_settings), { sectionName = null })
            }
        }
    }
    state.updatePreview?.let { plan -> CytBottomSheet({ onIntent(HomeScreenIntent.CancelPreview) }) { UpdatePreviewContent(plan, state.updateTo.orEmpty(), onIntent) } }
    if (leaving != null) CytBottomSheet({ leaving = null }) {
        SheetHeader(stringResource(R.string.hub_unsent_title), subtitle = stringResource(R.string.hub_unsent_body))
        PrimaryButton(stringResource(R.string.hub_keep_editing), { leaving = null }, guardObscured = false)
        TertiaryButton(stringResource(R.string.hub_discard), {
            onIntent(HomeScreenIntent.CloseLook)
            val action = leaving; leaving = null; action?.invoke()
        })
    }
}

@Composable
private fun HubRows(items: List<HubSetting>, enabled: Boolean, onOpen: (HubSetting) -> Unit) {
    ListGroup {
        items.forEachIndexed { index, item ->
            if (index > 0) ListDivider()
            ListRow(stringResource(item.title), subtitle = stringResource(item.description),
                leading = { CytIcon(item.icon(), null, tint = CytTheme.colors.primary) },
                onClick = if (enabled || item == HubSetting.TOOLS) ({ onOpen(item) }) else null)
        }
    }
}

@Composable
private fun HubNavigation(selected: HubTab, onTab: (HubTab) -> Unit, enabled: Boolean) {
    val largeText = LocalDensity.current.fontScale > 1.2f
    val scroll = rememberScrollState()
    // Keep full labels at the user's font size. Large text can scroll horizontally instead of
    // splitting a word in half; the selected destination stays in view.
    LaunchedEffect(largeText, selected, scroll.maxValue) {
        if (largeText && scroll.maxValue in 1 until Int.MAX_VALUE) scroll.scrollTo(if (selected.ordinal >= 2) scroll.maxValue else 0)
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).then(if (largeText) Modifier.horizontalScroll(scroll) else Modifier), horizontalArrangement = Arrangement.SpaceEvenly) {
        HubTab.entries.forEach { tab ->
            val color = if (tab == selected) CytTheme.colors.primary else CytTheme.colors.textTertiary
            val cell = if (largeText) Modifier.widthIn(min = 80.dp) else Modifier.weight(1f)
            Column(cell.selectable(tab == selected, enabled = enabled, role = Role.Tab, onClick = { onTab(tab) })
                .padding(vertical = 10.dp, horizontal = if (largeText) 12.dp else 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CytIcon(when (tab) { HubTab.HOME -> CytIcons.Home; HubTab.PERSONALIZE -> CytIcons.Edit; HubTab.WIDGETS -> CytIcons.Cards; HubTab.TV -> CytIcons.Tv }, null, tint = color)
                Text(stringResource(tab.title()), style = CytTheme.type.bodySmall, color = color, softWrap = false, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun HubTab.title() = when (this) { HubTab.HOME -> R.string.hub_home; HubTab.PERSONALIZE -> R.string.hub_personalize; HubTab.WIDGETS -> R.string.hub_widgets; HubTab.TV -> R.string.hub_tv }
private fun HubTab.description() = when (this) { HubTab.HOME -> R.string.hub_home_desc; HubTab.PERSONALIZE -> R.string.hub_personalize_desc; HubTab.WIDGETS -> R.string.hub_widgets_desc; HubTab.TV -> R.string.hub_tv_desc }
private fun HubSetting.icon() = when (this) {
    HubSetting.APPEARANCE -> LauncherinIcons.Theme
    HubSetting.LAYOUT -> CytIcons.Reorder
    HubSetting.AGENDA -> CytIcons.Cards
    HubSetting.PHOTOS -> LauncherinIcons.Picture
    HubSetting.SYSTEM -> LauncherinIcons.Timer
    HubSetting.WEATHER -> LauncherinIcons.Weather
    HubSetting.SPORTS -> LauncherinIcons.Sports
    HubSetting.WATCH -> LauncherinIcons.Watch
    HubSetting.MUSIC_PROFILES, HubSetting.LISTEN, HubSetting.MUSIC_VISUAL -> LauncherinIcons.Music
    HubSetting.WALLPAPER, HubSetting.SAVER -> LauncherinIcons.Picture
    HubSetting.POWER -> LauncherinIcons.Timer
    HubSetting.THEME -> LauncherinIcons.Theme
    HubSetting.TRANSPARENCY -> LauncherinIcons.Transparency
    HubSetting.APPS -> LauncherinIcons.Apps
    HubSetting.TOOLS, HubSetting.REMOTE -> CytIcons.Settings
    HubSetting.UPDATE -> CytIcons.Download
    HubSetting.TV_DETAILS -> CytIcons.Tv
}
private fun HubSetting.panel() = when (this) {
    HubSetting.LAYOUT -> LauncherinPanel.LAYOUT
    HubSetting.WATCH -> LauncherinPanel.WATCH
    HubSetting.WEATHER -> LauncherinPanel.WEATHER
    HubSetting.SPORTS -> LauncherinPanel.SPORTS
    HubSetting.TRANSPARENCY -> LauncherinPanel.TRANSPARENCY
    HubSetting.THEME -> LauncherinPanel.THEME
    HubSetting.SAVER -> LauncherinPanel.SAVER
    HubSetting.POWER -> LauncherinPanel.POWER
    else -> null
}
