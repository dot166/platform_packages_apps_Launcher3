package com.android.launcher3.nexus.bottombar.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import com.android.launcher3.LauncherPrefChangeListener
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherPrefs.Companion.getPrefs
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.model.SmartspaceAction
import com.android.launcher3.nexus.bottombar.model.SmartspaceScores
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import com.android.launcher3.util.OnboardingPrefs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class OnboardingProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.smartspace_onboarding),
        BottomBarPreferences.INSTANCE.get(context).smartspaceOnboarding,
    ) {

    companion object {
        private val PREF_KEYS = setOf(
            OnboardingPrefs.HAS_OPENED_SETTINGS.sharedPrefKey,
            OnboardingPrefs.HOME_BOUNCE_SEEN.sharedPrefKey,
        )

        private const val REQUEST_CODE_SETTINGS = 1
    }
    private val prefs = LauncherPrefs.get(context)

    private val lawnSettingsIntent: Intent = Intent(Intent.ACTION_APPLICATION_PREFERENCES)
        .setPackage(context.packageName)

    private val lawnSettingsPendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_SETTINGS,
        lawnSettingsIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override val internalTargets = callbackFlow {
        val prefsListener = LauncherPrefChangeListener {
                key,
            ->
            if (key == null) return@LauncherPrefChangeListener

            val isRelevant = key in PREF_KEYS
            if (!isRelevant) return@LauncherPrefChangeListener

            trySend(listOfNotNull(getSmartspaceTarget()))
        }

        prefs.addListener(prefsListener)

        trySend(listOfNotNull(getSmartspaceTarget()))

        awaitClose {
            prefs.removeListener(prefsListener)
        }
    }

    private fun hasSeenHomeBounce(): Boolean {
        return OnboardingPrefs.HOME_BOUNCE_SEEN.get(context)
    }

    private fun hasSeenSettings(): Boolean {
        return OnboardingPrefs.HAS_OPENED_SETTINGS.get(context)
    }

    private fun getSmartspaceTarget(): SmartspaceTarget? {
        return when {
            !hasSeenHomeBounce() -> {
                SmartspaceTarget(
                    id = "onboarding-swipe",
                    headerAction = SmartspaceAction(
                        id = "onboarding-swipe-action",
                        icon = null,
                        title = context.getString(R.string.onboarding_welcome),
                        subtitle = context.getString(R.string.onboarding_swipe_up),
                        pendingIntent = null,
                    ),
                    score = SmartspaceScores.SCORE_ONBOARDING,
                    featureType = SmartspaceTarget.FeatureType.FEATURE_ONBOARDING,
                )
            }

            !hasSeenSettings() -> {
                SmartspaceTarget(
                    id = "onboarding-settings",
                    headerAction = SmartspaceAction(
                        id = "onboarding-settings-action",
                        icon = Icon.createWithResource(context, R.drawable.ic_lightbulb),
                        title = context.getString(R.string.onboarding_open_settings_title),
                        subtitle = context.getString(R.string.onboarding_open_settings_subtitle),
                        pendingIntent = lawnSettingsPendingIntent,
                    ),
                    score = SmartspaceScores.SCORE_ONBOARDING,
                    featureType = SmartspaceTarget.FeatureType.FEATURE_ONBOARDING,
                )
            }

            else -> null
        }
    }
}
