package com.android.launcher3.nexus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.android.launcher3.LauncherPrefs
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.google.android.gsa.overlay.controllers.OverlayController as GsaOverlayController

class DefaultMinusOneService: Service() {

    private lateinit var overlaysController: OverlaysController

    override fun onCreate() {
        super.onCreate()
        overlaysController = OverlayController()
    }

    override fun onDestroy() {
        overlaysController.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i("-1", intent.toString())
        return overlaysController.onBind(intent, null)
    }

    override fun onUnbind(intent: Intent): Boolean {
        overlaysController.onUnbind(intent)
        return false
    }

    private inner class OverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(uid: Int, context: Context): GsaOverlayController {
            return DefaultOverlay(uid, context)
        }
    }
}