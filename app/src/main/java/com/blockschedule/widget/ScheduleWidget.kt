package com.blockschedule.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.blockschedule.data.TaskRepository
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import com.blockschedule.ui.MainActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Home-screen widget showing today's schedule with the current block highlighted. */
class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = TaskRepository.from(context).enabledTasks()
        val today = LocalDate.now()
        val blocks = Scheduler.blocksFor(today, tasks)
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val flat = Scheduler.flatten(blocks)
        val currentKey = Scheduler.activeLeafKey(blocks, now)
        val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

        provideContent {
            WidgetContent(dateLabel, flat, currentKey)
        }
    }
}

private val Card = Color(0xFFFFFFFF)
private val HeaderBg = Color(0xFF3B5BDB)
private val TextDark = Color(0xFF1A1C1E)
private val TextMuted = Color(0xFF5A5F66)
private val NowTint = Color(0xFFEAF0FF)
private val DividerCol = Color(0xFFE6E8EC)

private fun openApp() = actionStartActivity<MainActivity>()

@androidx.compose.runtime.Composable
private fun WidgetContent(
    dateLabel: String,
    blocks: List<ScheduledBlock>,
    currentKey: String?
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Card))
            .cornerRadius(16.dp)
            .clickable(openApp())
    ) {
        // Header
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(HeaderBg))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = dateLabel,
                style = TextStyle(color = ColorProvider(Color(0xFFD7E0FF)), fontSize = 13.sp)
            )
        }

        if (blocks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks today.\nTap to add one.",
                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 15.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(vertical = 4.dp)) {
                items(blocks.size) { i ->
                    BlockRow(blocks[i], isCurrent = blocks[i].key == currentKey)
                }
            }
        }
    }
}

private val OnColor = Color.White
private val OnColorMuted = Color(0xCCFFFFFF)

@androidx.compose.runtime.Composable
private fun BlockRow(block: ScheduledBlock, isCurrent: Boolean) {
    val catColor = Color(block.category.colorArgb)
    val timeText = if (block.unscheduled) "— —" else ScheduledBlock.formatTime(block.startMinute)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(
                start = if (block.isSubBlock) 22.dp else 8.dp,
                end = 8.dp, top = 3.dp, bottom = 3.dp
            )                                               // indent sub-blocks
            .background(ColorProvider(catColor))
            .cornerRadius(10.dp)
            .clickable(openApp())
            .padding(horizontal = 12.dp, vertical = if (block.isSubBlock) 7.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (block.isSubBlock) {
            Text(
                text = "↳ ",
                style = TextStyle(color = ColorProvider(OnColorMuted), fontSize = 13.sp)
            )
        }
        // Time
        Text(
            text = timeText,
            style = TextStyle(
                color = ColorProvider(OnColorMuted),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.width(64.dp)
        )

        // Title
        Text(
            text = block.title + if (block.hasConflict) "  ⚠" else "",
            style = TextStyle(
                color = ColorProvider(OnColor),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        // Current-block badge: white pill with the category color as its text.
        if (isCurrent) {
            Text(
                text = "NOW",
                style = TextStyle(
                    color = ColorProvider(catColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .background(ColorProvider(OnColor))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
