package com.blockschedule.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.blockschedule.game.CompletionManager

/** Toggles a block's completion straight from the home-screen widget. */
class ToggleDoneAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        val index = parameters[indexKey] ?: 0
        val day = parameters[dayKey] ?: return
        // Updates points/streak and refreshes the widget.
        CompletionManager.toggle(context, taskId, index, day)
    }

    companion object {
        val taskIdKey = ActionParameters.Key<Long>("taskId")
        val indexKey = ActionParameters.Key<Int>("index")
        val dayKey = ActionParameters.Key<Long>("day")
    }
}
