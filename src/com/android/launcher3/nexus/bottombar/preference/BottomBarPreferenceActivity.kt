package com.android.launcher3.nexus.bottombar.preference

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.rememberNavController
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.SmartspaceViewContainer
import com.android.launcher3.nexus.bottombar.model.SmartspaceCalendar
import com.android.launcher3.nexus.bottombar.model.SmartspaceTimeFormat
import com.android.launcher3.nexus.bottombar.provider.SmartspaceProvider
import com.android.settingslib.spa.framework.compose.localNavController
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category

class BottomBarPreferenceActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsTheme {
                val navController = rememberNavController()
                CompositionLocalProvider(navController.localNavController()) {
                    RegularScaffold(stringResource(R.string.bottom_bar_title)) {
                        val smartspaceProvider =
                            SmartspaceProvider.INSTANCE.get(LocalContext.current)
                        val prefs = BottomBarPreferences.INSTANCE.get(LocalContext.current)
                        SmartspacePreview()
                        Column {
                            Category(
                                title = stringResource(id = R.string.what_to_show),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                smartspaceProvider.dataSources
                                    .collectAsState()
                                    .value
                                    .asSequence()
                                    .filter { it.isAvailable }
                                    .filter { it.enabledPref != prefs.alwaysTrue }
                                    .forEach {
                                        key(it.providerName) {
                                            val en = it.enabledPref.flow.collectAsState().value
                                            SwitchPreference(
                                                object : SwitchPreferenceModel {
                                                    override val title: String = it.providerName
                                                    override val checked: () -> Boolean =
                                                        { en }
                                                    override val onCheckedChange: (newChecked: Boolean) -> Unit =
                                                        { checked -> it.enabledPref.set(checked) }
                                                }
                                            )
                                        }
                                    }
                            }
                            SmartspaceDateAndTimePreferences(prefs)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SmartspacePreview() {
        val themeRes =
            if (isSystemInDarkTheme()) R.style.AppTheme_Dark else R.style.AppTheme_DarkText
        val context = LocalContext.current
        val themedContext = remember(themeRes) { ContextThemeWrapper(context, themeRes) }

        Category(
            title = stringResource(id = R.string.preview_label),
        ) {
            CompositionLocalProvider(LocalContext provides themedContext) {
                AndroidView(
                    factory = {
                        val view = SmartspaceViewContainer(it, previewMode = true)
                        val height = it.resources
                            .getDimensionPixelSize(R.dimen.qsb_widget_height)
                        view.layoutParams =
                            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                        view
                    },
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                )
            }
            LaunchedEffect(key1 = null) {
                SmartspaceProvider.INSTANCE.get(context).startSetup(context as Activity)
            }
        }
    }

    @Composable
    fun SmartspaceDateAndTimePreferences(prefs: BottomBarPreferences) {
        val calendarHasMinimumContent =
            !prefs.showDate.collectAsState().value || !prefs.showTime.collectAsState().value
        val calendar = prefs.calendar.collectAsState().value

        Category(
            title = stringResource(id = R.string.smartspace_date_and_time),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            val supportCustomizationFormat = calendar.formatCustomizationSupport
            if (supportCustomizationFormat) {
                val en = prefs.showDate.collectAsState().value
                SwitchPreference(
                    object : SwitchPreferenceModel {
                        override val title: String = stringResource(id = R.string.smartspace_date)
                        override val checked: () -> Boolean?
                            get() = { en }
                        override val onCheckedChange: ((newChecked: Boolean) -> Unit)
                            get() = { prefs.setShowDate(it) }
                        override val changeable: () -> Boolean
                            get() = { if (en) !calendarHasMinimumContent else true }
                    }
                )
            }
            if (supportCustomizationFormat && prefs.showDate.collectAsState().value) {
                SmartspaceCalendarPreference(LocalContext.current, prefs)
            }
            if (supportCustomizationFormat) {
                val en = prefs.showTime.collectAsState().value
                SwitchPreference(
                    object : SwitchPreferenceModel {
                        override val title: String = stringResource(id = R.string.smartspace_time)
                        override val checked: () -> Boolean?
                            get() = { en }
                        override val onCheckedChange: ((newChecked: Boolean) -> Unit)
                            get() = { prefs.setShowTime(it) }
                        override val changeable: () -> Boolean
                            get() = { if (en) !calendarHasMinimumContent else true }
                    }
                )
            }
            if (supportCustomizationFormat && prefs.showTime.collectAsState().value) {
                SmartspaceTimeFormatPreference(LocalContext.current, prefs)
            }
        }
    }

    @Composable
    fun SmartspaceTimeFormatPreference(
        ctx: Context,
        prefs: BottomBarPreferences,
    ) {
        val entries = remember {
            SmartspaceTimeFormat.values().mapIndexed { index, format ->
                ListPreferenceOption(index, ctx.getString(format.nameResourceId))
            }
        }

        ListPreference(
            object : ListPreferenceModel {
                override val title: String = stringResource(id = R.string.smartspace_time_format)
                override val options: List<ListPreferenceOption>
                    get() = entries
                override val selectedId: IntState
                    get() = mutableIntStateOf(
                        SmartspaceTimeFormat.values().indexOf(
                            prefs.timeFormat.value
                        )
                    )
                override val onIdSelected: (id: Int) -> Unit
                    get() = {
                        prefs.setTimeFormat(SmartspaceTimeFormat.values()[it])
                    }
            }
        )
    }

    @Composable
    fun SmartspaceCalendarPreference(
        ctx: Context,
        prefs: BottomBarPreferences,
    ) {
        val entries = remember {
            SmartspaceCalendar.values().mapIndexed { index, calendar ->
                ListPreferenceOption(index, ctx.getString(calendar.nameResourceId))
            }
        }

        ListPreference(
            object : ListPreferenceModel {
                override val title: String = stringResource(id = R.string.smartspace_calendar)
                override val options: List<ListPreferenceOption>
                    get() = entries
                override val selectedId: IntState
                    get() = mutableIntStateOf(
                        SmartspaceCalendar.values().indexOf(
                            prefs.calendar.value
                        )
                    )
                override val onIdSelected: (id: Int) -> Unit
                    get() = {
                        prefs.setCalendar(SmartspaceCalendar.values()[it])
                    }
            }
        )
    }

}
