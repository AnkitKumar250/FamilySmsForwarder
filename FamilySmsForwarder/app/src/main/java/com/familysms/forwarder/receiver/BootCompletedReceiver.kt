package com.familysms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.familysms.forwarder.FamilySmsApplication
import com.familysms.forwarder.worker.ForwardSmsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // Guard against Direct Boot before Credential-Encrypted Room/DataStore storage is unlocked
        val userManager = context.getSystemService(UserManager::class.java)
        if (userManager != null && !userManager.isUserUnlocked) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as FamilySmsApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val queuedLogs = app.repository.getQueuedLogs()
                for (log in queuedLogs) {
                    ForwardSmsWorker.enqueue(context = app, logId = log.id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
