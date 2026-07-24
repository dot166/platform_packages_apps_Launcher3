package com.android.launcher3.nexus.bottombar.preference

import android.content.Context
import com.android.launcher3.ConstantItem
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.nexus.bottombar.model.SmartspaceCalendar
import com.android.launcher3.nexus.bottombar.model.SmartspaceTimeFormat
import com.android.launcher3.nexus.bottombar.provider.IBottomBarProvider
import com.android.launcher3.util.DaggerSingletonObject
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@LauncherAppSingleton
class BottomBarPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _showDate = MutableStateFlow(LauncherPrefs.smartspaceShowDate.get(context))
    val showDate: StateFlow<Boolean> = _showDate

    private val _showTime = MutableStateFlow(LauncherPrefs.smartspaceShowTime.get(context))
    val showTime: StateFlow<Boolean> = _showTime

    private val _timeFormat = MutableStateFlow(
        SmartspaceTimeFormat.fromString(
            LauncherPrefs.smartspaceTimeFormat.get(context)
        )
    )
    val timeFormat: StateFlow<SmartspaceTimeFormat> = _timeFormat

    private val _calendar = MutableStateFlow(
        SmartspaceCalendar.fromString(
            LauncherPrefs.smartspaceCalendar.get(context)
        )
    )
    val calendar: StateFlow<SmartspaceCalendar> = _calendar

    fun setShowDate(value: Boolean) {
        LauncherPrefs.get(context).put(LauncherPrefs.smartspaceShowDate, value)
        _showDate.value = value
    }

    fun setShowTime(value: Boolean) {
        LauncherPrefs.get(context).put(LauncherPrefs.smartspaceShowTime, value)
        _showTime.value = value
    }

    fun setTimeFormat(value: SmartspaceTimeFormat) {
        LauncherPrefs.get(context).put(LauncherPrefs.smartspaceTimeFormat, value.toString())
        _timeFormat.value = value
    }

    fun setCalendar(value: SmartspaceCalendar) {
        LauncherPrefs.get(context).put(LauncherPrefs.smartspaceCalendar, value.toString())
        _calendar.value = value
    }

    val smartspaceAagWidget =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.smartspaceAagWidget, context)
    val smartspaceBatteryStatus =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.smartspaceBatteryStatus, context)
    val smartspaceTorch =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.smartspaceTorch, context)
    val smartspaceNowPlaying =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.smartspaceNowPlaying, context)
    val smartspaceOnboarding =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.smartspaceOnboarding, context)
    val alwaysTrue =
        BottomBarProviderDataSourcePreferenceImpl(LauncherPrefs.bottomBarHiddenAlwaysTrue, context)

    class BottomBarProviderDataSourcePreferenceImpl(
        private val pref: ConstantItem<Boolean>,
        private val context: Context
    ) : BottomBarProviderDataSourcePreference {
        private val _flow = MutableStateFlow(pref.get(context))
        override val flow: StateFlow<Boolean> = _flow
        override fun set(bool: Boolean) {
            if (pref != LauncherPrefs.bottomBarHiddenAlwaysTrue) {
                LauncherPrefs.get(context).put(pref, bool)
                _flow.value = bool
            }
        }
    }

    class RemoteBottomBarProviderDataSourcePreference(private val provider: IBottomBarProvider) :
        BottomBarProviderDataSourcePreference {
        private val _flow = MutableStateFlow(provider.getEnabled())
        override val flow: StateFlow<Boolean> = _flow
        override fun set(bool: Boolean) {
            provider.setEnabled(bool)
            _flow.value = bool
        }
    }

    interface BottomBarProviderDataSourcePreference {
        fun set(bool: Boolean)
        val flow: StateFlow<Boolean>
    }

    companion object {
        @JvmField
        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getBottomBarPreferences)
    }
}
