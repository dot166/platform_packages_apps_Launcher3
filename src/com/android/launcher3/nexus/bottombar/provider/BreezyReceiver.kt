package com.android.launcher3.nexus.bottombar.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.launcher3.nexus.bottombar.preference.SmartspacePrefStore
import kotlinx.coroutines.runBlocking

class BreezyReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(EXTERNAL_BREEZY_BROADCAST_DATA_CHANGED)) {
            val prefs by SmartspacePrefStore
            val locationIds = intent.getStringArrayExtra("AllLocationIds")
            val idToQuery = (locationIds?.firstOrNull() ?: "CURRENT_POSITION")
                .filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ':' }
                .take(64)
                .ifEmpty { "CURRENT_POSITION" }
            runBlocking {
                prefs.smartspaceWeatherLocation.set(idToQuery)
            }
            context.sendBroadcast(Intent(INTERNAL_BROADCAST_CHANGED))
        }
    }

    companion object {
        const val INTERNAL_BROADCAST_CHANGED = "com.android.launcher3.nexus.bottombar.provider.BREEZY_WEATHER_CHANGED"
        const val EXTERNAL_BREEZY_BROADCAST_DATA_CHANGED = "org.breezyweather.ACTION_UPDATE_NOTIFIER"
    }
}