package tv.cleanyour.ui.design.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import tv.cleanyour.core.flows.*
import tv.cleanyour.ui.design.R
import tv.cleanyour.ui.design.icon.CytIcons

@Composable
fun ColumnScope.LauncherinAgendaEditor(entries: List<AgendaEntry>,sync: AgendaSync,onSave:(AgendaEntry)->Unit,onRemove:(String)->Unit,onRetry:()->Unit) {
    var editing by remember { mutableStateOf<AgendaEntry?>(null) }
    val context=LocalContext.current
    LookBody(stringResource(R.string.agenda_local_hint),Modifier.padding(bottom=12.dp))
    val status=when(sync){
        AgendaSync.SENDING->R.string.agenda_sending;AgendaSync.SYNCED->R.string.agenda_synced;AgendaSync.PENDING->R.string.agenda_pending
        AgendaSync.FAILED->R.string.agenda_failed;AgendaSync.NEEDS_UPDATE->R.string.agenda_needs_update
        AgendaSync.NEEDS_UNLOCK->R.string.agenda_unlock;AgendaSync.READY->R.string.agenda_ready
    }
    LookBody(stringResource(status),Modifier.padding(bottom=12.dp))
    if(editing==null){
        PrimaryButton(stringResource(R.string.agenda_add),{editing=AgendaEntry(UUID.randomUUID().toString(),"",LocalDate.now().toString())},enabled=entries.size<LauncherinAgenda.MAX)
        if(entries.isEmpty())LookBody(stringResource(R.string.agenda_empty),Modifier.padding(top=12.dp))
        else ListGroup(Modifier.padding(top=12.dp)) {
            entries.forEach { event -> ListRow(event.title,subtitle="${LocalDate.parse(event.date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${event.time.ifEmpty { stringResource(R.string.agenda_all_day) }}",
                onClick={editing=event},trailing={CytIconButton(CytIcons.Delete,stringResource(R.string.agenda_remove,event.title),{onRemove(event.id)})}) }
        }
        if(sync in listOf(AgendaSync.PENDING,AgendaSync.FAILED,AgendaSync.NEEDS_UNLOCK))SecondaryButton(stringResource(R.string.agenda_retry),onRetry,Modifier.padding(top=12.dp))
    }else{
        val event=editing!!;val date=LocalDate.parse(event.date)
        CytTextField(event.title,{editing=event.copy(title=it.take(120))},stringResource(R.string.agenda_title))
        ListGroup(Modifier.padding(top=12.dp)) {
            ListRow(stringResource(R.string.agenda_date),value=date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),onClick={
                DatePickerDialog(context,{_,y,m,d->editing=event.copy(date=LocalDate.of(y,m+1,d).toString())},date.year,date.monthValue-1,date.dayOfMonth).show()
            })
            ListRow(stringResource(R.string.agenda_time),value=event.time.ifEmpty { stringResource(R.string.agenda_all_day) },onClick={
                val time=runCatching { LocalTime.parse(event.time) }.getOrDefault(LocalTime.of(9,0))
                TimePickerDialog(context,{_,h,m->editing=event.copy(time=LocalTime.of(h,m).format(DateTimeFormatter.ofPattern("HH:mm")))},time.hour,time.minute,true).show()
            })
        }
        TertiaryButton(stringResource(R.string.agenda_all_day),{editing=event.copy(time="")})
        PrimaryButton(stringResource(R.string.agenda_save),{onSave(event);editing=null},enabled=event.title.isNotBlank())
        TertiaryButton(stringResource(R.string.agenda_cancel),{editing=null})
    }
}

fun interface WidgetPhotoPicker { fun pick(result: (List<ByteArray>) -> Unit) }
