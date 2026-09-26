package com.android.launcher3.nexus.bottombar.provider

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Icon
import androidx.core.content.getSystemService
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.BcSmartSpaceUtil
import com.android.launcher3.nexus.bottombar.lawnchair.util.broadcastReceiverFlow
import com.android.launcher3.nexus.bottombar.model.SmartspaceIconView
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.model.SmartspaceView
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import kotlinx.coroutines.flow.map

class DateProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.smartspace_date_and_time),
        BottomBarPreferences.INSTANCE.get(context).alwaysTrue,
    ) {

    val intentFilter = IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED).apply {
        addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
    }

    override val internalTargets = broadcastReceiverFlow(context, intentFilter, true).map { listOfNotNull(getSmartspaceTarget()) }

    fun getSmartspaceTarget(): SmartspaceTarget {
        return SmartspaceTarget(
            id = "date",
            title = "unusedTitle",
            intent = BcSmartSpaceUtil.getOpenCalendarIntent(),
            tiles = getTiles(),
            featureType = SmartspaceTarget.FeatureType.INTERNAL_FEATURE_DATE_TIME,
        )
    }

    fun getTiles(): List<SmartspaceView> {
        val tiles = mutableListOf<SmartspaceView>()
        val notificationManager = context.getSystemService<NotificationManager>()!!
        val filter = notificationManager.currentInterruptionFilter
        val isDndOn = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (isDndOn) {
            tiles.add(
                SmartspaceIconView(
                    icon = Icon.createWithResource(context, R.drawable.do_not_disturb_on_24px),
                    contentDescription = "Do not disturb enabled",
                )
            )
        }
        return tiles
    }
}
