package com.blockschedule.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes every placed Block Schedule widget. Call after any data change. */
object WidgetUpdater {
    suspend fun update(context: Context) {
        ScheduleWidget().updateAll(context.applicationContext)
    }
}
