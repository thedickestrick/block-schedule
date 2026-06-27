package com.blockschedule.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.blockschedule.reminder.ReminderPrefs
import com.blockschedule.reminder.ReminderScheduler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockschedule.ui.theme.BlockScheduleTheme
import com.blockschedule.widget.WidgetUpdater
import kotlinx.coroutines.launch

/** Navigation target. Kept deliberately simple — three screens, no nav library. */
sealed interface Screen {
    data object Today : Screen
    data object AllTasks : Screen
    data object Settings : Screen
    data object Achievements : Screen
    data class Edit(val taskId: Long?) : Screen
}

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Re-schedule regardless; reminders only post if granted.
            ReminderScheduler.rescheduleAsync(applicationContext)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.blockschedule.game.SoundPlayer.init(applicationContext)

        // If reminders are on, make sure we have notification permission (Android 13+).
        if (ReminderPrefs(this).enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            BlockScheduleTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Keep the widget's "now" highlight fresh and reminders scheduled whenever the app is used.
        lifecycleScope.launch { WidgetUpdater.update(applicationContext) }
        ReminderScheduler.rescheduleAsync(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        // Don't let the dance music keep playing if the app goes to the background.
        com.blockschedule.game.DancePlayer.stop()
    }
}

@Composable
private fun AppRoot() {
    val vm: TaskViewModel = viewModel()
    val updateVm: com.blockschedule.update.UpdateViewModel = viewModel()
    var screen by remember { mutableStateOf<Screen>(Screen.Today) }

    // Check for an update once when the app starts (if the user left auto-update on).
    androidx.compose.runtime.LaunchedEffect(Unit) { updateVm.checkOnLaunch() }

    when (val s = screen) {
        is Screen.Today -> TodayScreen(
            vm = vm,
            updateVm = updateVm,
            onAddTask = { screen = Screen.Edit(null) },
            onEditTask = { id -> screen = Screen.Edit(id) },
            onManageTasks = { screen = Screen.AllTasks },
            onOpenSettings = { screen = Screen.Settings },
            onOpenAchievements = { screen = Screen.Achievements }
        )

        is Screen.Achievements -> AchievementsScreen(
            vm = vm,
            onBack = { screen = Screen.Today }
        )

        is Screen.AllTasks -> AllTasksScreen(
            vm = vm,
            onBack = { screen = Screen.Today },
            onAddTask = { screen = Screen.Edit(null) },
            onEditTask = { id -> screen = Screen.Edit(id) }
        )

        is Screen.Settings -> SettingsScreen(
            updateVm = updateVm,
            onBack = { screen = Screen.Today }
        )

        is Screen.Edit -> EditTaskScreen(
            vm = vm,
            taskId = s.taskId,
            onDone = { screen = Screen.Today }
        )
    }
}
