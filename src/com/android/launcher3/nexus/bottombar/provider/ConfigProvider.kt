package com.android.launcher3.nexus.bottombar.provider

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.model.SmartspaceAction
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferenceActivity
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import kotlinx.coroutines.flow.flowOf

class ConfigProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.action_customize),
        BottomBarPreferences.INSTANCE.get(context).alwaysTrue,
    ) {

    override val internalTargets = flowOf(listOfNotNull(getSmartspaceTarget()))

    fun getSmartspaceTarget(): SmartspaceTarget {
        return SmartspaceTarget(
            id = "config",
            headerAction = SmartspaceAction(
                id = "config-action",
                icon = Icon.createWithResource(context, R.drawable.ic_setting),
                title = context.getString(R.string.action_customize),
                subtitle = null,//context.getString(R.string.onboarding_swipe_up),
                intent = Intent(context, BottomBarPreferenceActivity::class.java),
            ),
            score = -9999999f,
            featureType = SmartspaceTarget.FeatureType.FEATURE_UNDEFINED,
        )
    }
}
