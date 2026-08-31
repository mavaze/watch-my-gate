package com.mavaze.mygate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyGateBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        val app =
            context.applicationContext as MyGateApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config =
                    app.database.appConfigDao().get()

                if (config?.deviceMode == "KIOSK") {
                    val launchIntent =
                        Intent(
                            context,
                            MainActivity::class.java
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                        }

                    context.startActivity(launchIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
