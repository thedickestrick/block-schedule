package com.blockschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
