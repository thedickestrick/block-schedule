package com.blockschedule.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/**
 * Handles PackageInstaller status callbacks. The key case is PENDING_USER_ACTION: the system
 * hands back an Intent that, when started, shows the "update this app?" confirmation dialog.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirm != null) context.startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // The app will be restarted by the system as the new version.
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // User cancelled — stay silent.
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Toast.makeText(context, "Update failed: ${msg ?: "unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
