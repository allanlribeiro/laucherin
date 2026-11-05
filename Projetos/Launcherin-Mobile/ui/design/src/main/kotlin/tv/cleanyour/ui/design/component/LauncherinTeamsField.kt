package tv.cleanyour.ui.design.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tv.cleanyour.core.flows.LauncherinTeams
import tv.cleanyour.ui.design.R
import tv.cleanyour.ui.design.theme.CytTheme
import java.text.Normalizer

@Composable
internal fun LauncherinTeamsField(selected: List<String>, onChange: (List<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = LauncherinTeams.all.filter { it.id in selected }.joinToString(" · ") { it.name }
    SecondaryButton(stringResource(R.string.launcherin_teams_title), { open = true }, Modifier.fillMaxWidth())
    LookBody(names.ifEmpty { stringResource(R.string.launcherin_teams_empty) }, Modifier.padding(top = 8.dp, bottom = 16.dp))
    if (!open) return
    var draft by remember { mutableStateOf(LauncherinTeams.clean(selected)) }
    var query by remember { mutableStateOf("") }
    fun fold(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(java.util.Locale.ROOT)
    val shown = remember(query) { LauncherinTeams.all.filter { fold(it.name + " " + it.league).contains(fold(query)) } }
    Dialog(onDismissRequest = { open = false }) {
        Surface(shape = CytTheme.shapes.small, color = CytTheme.colors.surface1) {
            Column(Modifier.padding(20.dp).heightIn(max = 560.dp)) {
                Text(stringResource(R.string.launcherin_teams_title), color = CytTheme.colors.textPrimary, style = CytTheme.type.bodyLarge)
                LookBody(stringResource(R.string.launcherin_teams_hint), Modifier.padding(vertical = 8.dp))
                CytTextField(query, { query = it.take(80) }, stringResource(R.string.launcherin_teams_search))
                Text("${draft.size}/2", color = CytTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(min = 100.dp, max = 280.dp)) {
                    items(shown, key = { it.id }) { team ->
                        val checked = team.id in draft
                        val enabled = checked || draft.size < 2
                        fun toggle() { if (enabled) draft = if (checked) draft - team.id else draft + team.id }
                        Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { toggle() }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = { toggle() }, enabled = enabled)
                            Column(Modifier.weight(1f)) {
                                Text(team.name, color = if (enabled) CytTheme.colors.textPrimary else CytTheme.colors.textTertiary)
                                Text(team.league, color = CytTheme.colors.textTertiary, style = CytTheme.type.bodySmall)
                            }
                        }
                    }
                    if (shown.isEmpty()) item { LookBody(stringResource(R.string.launcherin_teams_no_results)) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                    TertiaryButton(stringResource(R.string.launcherin_teams_cancel), { open = false })
                    TertiaryButton(stringResource(R.string.launcherin_teams_save), { onChange(draft); open = false })
                }
            }
        }
    }
}
