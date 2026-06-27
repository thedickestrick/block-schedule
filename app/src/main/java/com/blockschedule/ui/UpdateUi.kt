package com.blockschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockschedule.game.GamePrefs
import com.blockschedule.reminder.ReminderPrefs
import com.blockschedule.reminder.ReminderScheduler
import com.blockschedule.update.UpdateUiState
import com.blockschedule.update.UpdateViewModel

/** Compact banner shown on the Today screen when an update is available or in progress. */
@Composable
fun UpdateBanner(updateVm: UpdateViewModel) {
    val state by updateVm.state.collectAsStateWithLifecycle()

    val show = state is UpdateUiState.Available ||
        state is UpdateUiState.Downloading ||
        state is UpdateUiState.Installing ||
        state is UpdateUiState.NeedsPermission
    if (!show) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            when (val s = state) {
                is UpdateUiState.Available -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Update available — v${s.info.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (s.info.notes.isNotBlank()) {
                                Text(
                                    s.info.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        IconButton(onClick = updateVm::dismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = updateVm::startUpdate, modifier = Modifier.fillMaxWidth()) {
                        Text("Update now")
                    }
                }

                is UpdateUiState.NeedsPermission -> {
                    Text(
                        "Allow “install unknown apps” for Block Schedule, then tap Update.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = updateVm::startUpdate, modifier = Modifier.fillMaxWidth()) {
                        Text("Update now")
                    }
                }

                is UpdateUiState.Downloading -> {
                    val pct = if (s.progress >= 0f) "${(s.progress * 100).toInt()}%" else ""
                    Text("Downloading update… $pct", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (s.progress >= 0f) {
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                is UpdateUiState.Installing ->
                    Text("Starting installer…", style = MaterialTheme.typography.titleMedium)

                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(updateVm: UpdateViewModel, onBack: () -> Unit) {
    val state by updateVm.state.collectAsStateWithLifecycle()
    val autoCheck by updateVm.autoCheck.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FunSection()

            HorizontalDivider()

            SpotifySection()

            HorizontalDivider()

            RemindersSection()

            HorizontalDivider()

            Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Check for updates automatically", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "When the app opens, it looks for a newer version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoCheck, onCheckedChange = updateVm::setAutoCheck)
            }

            Text(
                "Installed version: v${updateVm.currentVersionName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live status
            when (val s = state) {
                is UpdateUiState.Checking -> Text("Checking…")
                is UpdateUiState.UpToDate -> Text("You're on the latest version.")
                is UpdateUiState.Available ->
                    Text("Update available: v${s.info.versionName}")
                is UpdateUiState.NeedsPermission ->
                    Text("Allow installing updates, then tap Update.")
                is UpdateUiState.Downloading -> {
                    val pct = if (s.progress >= 0f) "${(s.progress * 100).toInt()}%" else ""
                    Text("Downloading… $pct")
                }
                is UpdateUiState.Installing -> Text("Starting installer…")
                is UpdateUiState.Error -> Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { updateVm.checkNow() }) { Text("Check now") }
                if (state is UpdateUiState.Available || state is UpdateUiState.NeedsPermission) {
                    Button(onClick = updateVm::startUpdate) { Text("Update now") }
                }
            }

            Text(
                "Updates are downloaded from the project's GitHub releases and installed with one " +
                    "confirmation. The first time, Android will ask you to allow installs from this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpotifySection() {
    val context = LocalContext.current
    val prefs = remember { com.blockschedule.game.SpotifyPrefs(context) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var clientId by remember { mutableStateOf(prefs.clientId) }
    var playlist by remember { mutableStateOf(prefs.playlistRaw) }

    Text("Dance party music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    ToggleRow(
        "Play my Spotify playlist",
        "Needs Spotify Premium + the Spotify app. Otherwise the built-in tune plays.",
        enabled
    ) { prefs.enabled = it; enabled = it }

    if (enabled) {
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it; prefs.clientId = it },
            label = { Text("Spotify client ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = playlist,
            onValueChange = {
                playlist = it
                prefs.playlistRaw = it
                prefs.playlistUri = com.blockschedule.game.SpotifyPrefs.normalizeToUri(it)
            },
            label = { Text("Playlist link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val uri = com.blockschedule.game.SpotifyPrefs.normalizeToUri(playlist)
        Text(
            if (playlist.isBlank()) "Paste a Spotify playlist link (open the playlist in Spotify → Share → Copy link)."
            else if (uri.isBlank()) "Hmm, that doesn't look like a Spotify playlist link."
            else "Ready: $uri",
            style = MaterialTheme.typography.bodyMedium,
            color = if (playlist.isNotBlank() && uri.isBlank()) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "One-time setup is needed in Spotify's developer dashboard — see SPOTIFY_SETUP in the project.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FunSection() {
    val context = LocalContext.current
    val prefs = remember { GamePrefs(context) }
    var sound by remember { mutableStateOf(prefs.soundEnabled) }
    var celebrations by remember { mutableStateOf(prefs.celebrationsEnabled) }
    var achievements by remember { mutableStateOf(prefs.achievementsEnabled) }

    Text("Fun", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    ToggleRow("Completion sounds", "A cute chime when you finish a task", sound) {
        prefs.soundEnabled = it; sound = it
    }
    ToggleRow("Celebrations", "Cute animals rain down when you complete tasks", celebrations) {
        prefs.celebrationsEnabled = it; celebrations = it
    }
    ToggleRow("Badge popups", "Show a popup when you earn a new badge", achievements) {
        prefs.achievementsEnabled = it; achievements = it
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RemindersSection() {
    val context = LocalContext.current
    val prefs = remember { ReminderPrefs(context) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var lead by remember { mutableIntStateOf(prefs.leadMinutes) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        prefs.enabled = granted
        enabled = granted
        ReminderScheduler.rescheduleAsync(context)
    }

    fun setEnabled(on: Boolean) {
        val needsPermission = on &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            prefs.enabled = on
            enabled = on
            ReminderScheduler.rescheduleAsync(context)
        }
    }

    fun setLead(value: Int) {
        val v = value.coerceIn(0, 60)
        lead = v
        prefs.leadMinutes = v
        ReminderScheduler.rescheduleAsync(context)
    }

    Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Remind me before each task", style = MaterialTheme.typography.bodyLarge)
            Text(
                "A notification shortly before each scheduled block.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = ::setEnabled)
    }

    if (enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Remind", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(72.dp))
            OutlinedButton(onClick = { setLead(lead - 5) }) { Text("–") }
            Text(
                if (lead == 0) "at start" else "$lead min before",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(140.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            OutlinedButton(onClick = { setLead(lead + 5) }) { Text("+") }
        }
    }
}
