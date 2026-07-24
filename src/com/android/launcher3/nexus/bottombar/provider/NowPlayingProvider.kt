package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.lawnchair.BlankActivity
import com.android.launcher3.nexus.bottombar.lawnchair.getAppName
import com.android.launcher3.nexus.bottombar.model.SmartspaceAction
import com.android.launcher3.nexus.bottombar.model.SmartspaceScores
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import com.android.launcher3.notification.NotificationListener
import com.android.launcher3.settings.SettingsActivity
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.launcher3.util.SettingsCache
import com.android.launcher3.util.SettingsCache.NOTIFICATION_BADGING_URI
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first


class NowPlayingProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.smartspace_now_playing),
        BottomBarPreferences.INSTANCE.get(context).smartspaceNowPlaying,
    ) {

    private val defaultIcon = Icon.createWithResource(context, R.drawable.ic_music_note)

    override val internalTargets = callbackFlow {
        val mediaListener = MediaListener(context) {
            trySend(listOfNotNull(getSmartspaceTarget(it)))
        }
        mediaListener.onResume()
        awaitClose { mediaListener.onPause() }
    }

    private fun getSmartspaceTarget(media: MediaListener): SmartspaceTarget? {
        val tracking = media.tracking ?: return null
        val title = tracking.info.title ?: return null

        val sbn = tracking.sbn
        val icon = sbn.notification.smallIcon ?: defaultIcon

        val mediaInfo = tracking.info
        val subtitle = mediaInfo.artist?.takeIf { it.isNotEmpty() }
            ?: sbn?.getAppName(context)
            ?: context.getAppName(tracking.packageName)
        val intent = sbn?.notification?.contentIntent
        return SmartspaceTarget(
            id = "nowPlaying-${mediaInfo.hashCode()}",
            headerAction = SmartspaceAction(
                id = "nowPlayingAction-${mediaInfo.hashCode()}",
                icon = icon,
                title = title,
                subtitle = subtitle,
                pendingIntent = intent,
                onClick = if (intent == null) Runnable { media.toggle(true) } else null,
            ),
            score = SmartspaceScores.SCORE_MEDIA,
            featureType = SmartspaceTarget.FeatureType.FEATURE_MEDIA,
        )
    }

    override suspend fun requiresSetup(): Boolean = isNotificationServiceEnabled(context = context).not() ||
        notificationDotsEnabled(context = context).first().not()

    override suspend fun startSetup(activity: Activity) {
        val intent = Intent(activity, SettingsActivity::class.java)
        val message = activity.getString(
            R.string.event_provider_missing_notification_dots,
            providerName,
        )
        BlankActivity.startBlankActivityDialog(
            activity,
            intent,
            activity.getString(R.string.title_missing_notification_access),
            message,
            context.getString(R.string.title_change_settings),
        )
    }

    fun notificationDotsEnabled(context: Context) = callbackFlow {
        val observer = SettingsCache.INSTANCE.get(context).getListenableRef(NOTIFICATION_BADGING_URI)
            .forEach(MAIN_EXECUTOR) { dotsEnabled ->
                trySend(dotsEnabled)
            }
        awaitClose { observer.close() }
    }

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )
        val myListener = ComponentName(context, NotificationListener::class.java)
        return enabledListeners != null &&
            (
                enabledListeners.contains(myListener.flattenToString()) ||
                    enabledListeners.contains(myListener.flattenToShortString())
                )
    }
}
