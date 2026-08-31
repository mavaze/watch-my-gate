package com.mavaze.mygate

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.view.WindowInsets
import android.view.WindowInsetsController

class KioskManager(
    private val context: Context
) {
    private val devicePolicyManager =
        context.getSystemService(
            DevicePolicyManager::class.java
        )

    private val adminComponent =
        ComponentName(
            context,
            MyGateDeviceAdminReceiver::class.java
        )

    fun isDeviceOwner(): Boolean =
        devicePolicyManager.isDeviceOwnerApp(
            context.packageName
        )

    fun enter(activity: Activity): Boolean {
        if (
            !devicePolicyManager.isDeviceOwnerApp(
                context.packageName
            )
        ) {
            return false
        }

        devicePolicyManager.setLockTaskPackages(
            adminComponent,
            arrayOf(context.packageName)
        )

        activity.window.insetsController?.let {
            it.hide(
                WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars()
            )
            it.systemBarsBehavior =
                WindowInsetsController
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        activity.startLockTask()
        return true
    }
}
