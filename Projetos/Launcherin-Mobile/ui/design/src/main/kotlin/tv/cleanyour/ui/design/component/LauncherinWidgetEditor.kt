package tv.cleanyour.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import tv.cleanyour.core.flows.*
import tv.cleanyour.ui.design.R
import tv.cleanyour.ui.design.icon.CytIcons
import tv.cleanyour.ui.design.theme.CytTheme

@Composable
fun widgetName(id: String): String = stringResource(when(id) {
    "clock"->R.string.widget_clock;"weather"->R.string.widget_weather;"music"->R.string.widget_music
    "sports"->R.string.widget_sports;"agenda"->R.string.widget_agenda;"photos"->R.string.widget_photos;else->R.string.widget_system
})

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.LauncherinWidgetEditor(slots: List<WidgetSlot>, onEdit: (List<WidgetSlot>) -> Unit) {
    var selected by remember { mutableStateOf("clock") }
    var problem by remember { mutableStateOf(false) }
    val current=slots.firstOrNull { it.id==selected } ?: slots.first()
    fun change(value: WidgetSlot, findSpace: Boolean=false) {
        val next=LauncherinWidgets.change(slots,value,findSpace)
        problem=next==null;if(next!=null)onEdit(next)
    }
    LookBody(stringResource(R.string.widgets_layout_hint),Modifier.padding(bottom=12.dp))
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(2.7f).clip(RoundedCornerShape(16.dp)).background(CytTheme.colors.surface1)
        .border(1.dp,CytTheme.colors.outline,RoundedCornerShape(16.dp))) {
        val cellW=maxWidth/12;val cellH=maxHeight/2;val pxW=with(LocalDensity.current){cellW.toPx()};val pxH=with(LocalDensity.current){cellH.toPx()}
        for(y in 0..1)for(x in 0..11)Box(Modifier.offset(cellW*x,cellH*y).size(cellW,cellH).border(.5.dp,CytTheme.colors.outline))
        if(slots.none { it.enabled })Text(stringResource(R.string.widgets_none),Modifier.align(Alignment.Center).padding(12.dp),color=CytTheme.colors.textSecondary,style=CytTheme.type.bodySmall)
        slots.filter { it.enabled }.forEach { slot ->
            var dx by remember(slot) { mutableFloatStateOf(0f) };var dy by remember(slot){mutableFloatStateOf(0f)}
            val shape=RoundedCornerShape(8.dp)
            Box(Modifier.offset { IntOffset((slot.x*pxW+dx).roundToInt(),(slot.y*pxH+dy).roundToInt()) }
                .size(cellW*slot.w-4.dp,cellH*slot.h-4.dp).padding(2.dp).clip(shape)
                .background(if(selected==slot.id)CytTheme.colors.surface3 else CytTheme.colors.surface2)
                .border(1.dp,if(selected==slot.id)CytTheme.colors.primary else CytTheme.colors.outline,shape)
                .clickable { selected=slot.id }
                .pointerInput(slot,pxW,pxH,slots) {
                    detectDragGestures(onDragStart={selected=slot.id},onDragCancel={dx=0f;dy=0f},onDragEnd={
                        val next=slot.copy(x=(slot.x+dx/pxW).roundToInt().coerceIn(0,12-slot.w),y=(slot.y+dy/pxH).roundToInt().coerceIn(0,2-slot.h))
                        change(next);dx=0f;dy=0f
                    }) { event, delta -> event.consume();dx+=delta.x;dy+=delta.y }
                },contentAlignment=Alignment.Center) {
                Text(widgetName(slot.id),Modifier.padding(4.dp),color=CytTheme.colors.textPrimary,style=CytTheme.type.bodySmall,maxLines=2)
            }
        }
    }
    FlowRow(Modifier.padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        LauncherinWidgets.ids.forEach { id -> TertiaryButton((if(id==selected)"✓ " else "")+widgetName(id),{selected=id},plain=true) }
    }
    ListGroup(Modifier.padding(top=8.dp)) { LookSwitchRow(widgetName(selected),current.enabled,{change(current.copy(enabled=it),true)},description=stringResource(R.string.widgets_show_hint)) }
    LookSublabel(stringResource(R.string.widgets_size))
    LookRadioList(listOf(
        RadioChoice("small",stringResource(R.string.widgets_small),stringResource(R.string.widgets_small_hint)),
        RadioChoice("medium",stringResource(R.string.widgets_medium),stringResource(R.string.widgets_medium_hint)),
        RadioChoice("large",stringResource(R.string.widgets_large),stringResource(R.string.widgets_large_hint)),
    ),if(current.w==6)"large" else if(current.w==4)"medium" else "small",{size->
        val w=if(size=="large")6 else if(size=="medium")4 else 3;val h=if(size=="large")2 else 1
        change(current.copy(w=w,h=h,x=current.x.coerceAtMost(12-w),y=current.y.coerceAtMost(2-h)),true)
    })
    LookSublabel(stringResource(R.string.widgets_position))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
        CytIconButton(CytIcons.Back,stringResource(R.string.widgets_left),{change(current.copy(x=current.x-1))})
        CytIconButton(CytIcons.ArrowUp,stringResource(R.string.widgets_up),{change(current.copy(y=current.y-1))})
        CytIconButton(CytIcons.ArrowDown,stringResource(R.string.widgets_down),{change(current.copy(y=current.y+1))})
        CytIconButton(CytIcons.ChevronForward,stringResource(R.string.widgets_right),{change(current.copy(x=current.x+1))})
    }
    if(problem)LookBody(stringResource(R.string.widgets_no_space),Modifier.padding(top=8.dp))
}
