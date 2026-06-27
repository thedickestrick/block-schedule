package com.blockschedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** Asks the launcher to pin the schedule widget to the home screen (API 26+). */
object WidgetPinner {
    fun requestPin(context: Context): Boolean {
        val mgr = context.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, ScheduleWidgetReceiver::class.java)
        return mgr.requestPinAppWidget(provider, null, null)
    }
}
