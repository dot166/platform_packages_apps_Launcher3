package com.android.launcher3.nexus.bottombar.provider

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.core.content.getSystemService
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.BcSmartSpaceUtil
import com.android.launcher3.nexus.bottombar.lawnchair.util.broadcastReceiverFlow
import com.android.launcher3.nexus.bottombar.model.SmartspaceIconView
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.model.SmartspaceTextView
import com.android.launcher3.nexus.bottombar.model.SmartspaceView
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import java.util.Calendar
import java.util.Locale
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
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val info = am.nextAlarmClock
        if (info != null) {
            val alarmTime = Calendar.getInstance()
            alarmTime.setTimeInMillis(info.triggerTime)
            val skeleton = if (DateFormat.is24HourFormat(context)) "EHm" else "Ehma"
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
            val alarm = DateFormat.format(pattern, alarmTime) as String
            if (!TextUtils.isEmpty(alarm)) {
                val description =
                    context.getString(R.string.next_alarm_description, alarm)
                tiles.add(
                    SmartspaceIconView(
                        icon = Icon.createWithResource(context, R.drawable.alarm_24px),
                        contentDescription = description,
                    )
                )
                tiles.add(
                    SmartspaceTextView(
                        text = alarm,
                        contentDescription = description,
                    )
                )
            }
        }
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
