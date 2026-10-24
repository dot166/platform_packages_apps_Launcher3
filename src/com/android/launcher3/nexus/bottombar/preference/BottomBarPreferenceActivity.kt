package com.android.launcher3.nexus.bottombar.preference

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.SmartspaceViewContainer
import com.android.launcher3.nexus.bottombar.model.SmartspaceCalendar
import com.android.launcher3.nexus.bottombar.model.SmartspaceTimeFormat
import com.android.launcher3.nexus.bottombar.provider.SmartspaceProvider
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.ProvideDefaultDialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import io.github.dot166.jlib.compose.JLibAppTheme
import io.github.dot166.jlib.compose.JLibScreen
import io.github.dot166.jlib.compose.conditional
import io.github.dot166.jlib.compose.preference.ScrollableScreenColumn
import io.github.dot166.jlib.compose.preference.SegmentedListColumn

class BottomBarPreferenceActivity: ComponentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JLibAppTheme {
                ProvideDefaultDialogPrefStrings(
                    confirmLabel = stringResource(R.string.action__ok),
                    dismissLabel = stringResource(R.string.action__cancel),
                    neutralLabel = stringResource(R.string.action__default),
                ) {
                    Column(
                        modifier = Modifier
                            //.statusBarsPadding()
                            .navigationBarsPadding()
                            .conditional(LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                displayCutoutPadding()
                            }
                            .imePadding(),
                    ) {
                        JLibScreen(
                            title = stringResource(R.string.bottom_bar_title),
                        ) { (contentPadding, translucent) ->
                            ScrollableScreenColumn(contentPadding, translucent) {
                                val smartspaceProvider =
                                    SmartspaceProvider.INSTANCE.get(LocalContext.current)
                                val prefs by SmartspacePrefStore
                                SmartspacePreview()
                                SegmentedListColumn(
                                    title = stringResource(id = R.string.what_to_show),
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    val sources = smartspaceProvider.dataSources
                                        .collectAsState().value
                                    val count = sources.size
                                    sources.asSequence()
                                        .filter { it.isAvailable }
                                        .filter { it.enabledPref != prefs.alwaysTrue }
                                        .forEachIndexed { index, source ->
                                            key(source.providerName, index) {
                                                SwitchPreference(
                                                    pref = source.enabledPref,
                                                    shapes = ListItemDefaults.segmentedShapes(
                                                        index,
                                                        count
                                                    ),
                                                    title = source.providerName,
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
    }

    @Composable
    fun SmartspacePreview() {
        val themeRes =
            if (isSystemInDarkTheme()) R.style.AppTheme_Dark else R.style.AppTheme_DarkText
        val context = LocalContext.current
        val themedContext = remember(themeRes) { ContextThemeWrapper(context, themeRes) }

        SegmentedListColumn(
            title = stringResource(id = R.string.preview_label),
        ) {
            CompositionLocalProvider(LocalContext provides themedContext) {
                AndroidView(
                    factory = {
                        val view = SmartspaceViewContainer(it, previewMode = true)
                        val height = it.resources
                            .getDimensionPixelSize(R.dimen.enhanced_smartspace_height)
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

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun SmartspaceDateAndTimePreferences(prefs: SmartspacePreferenceModel) {
        val calendarHasMinimumContent =
            !prefs.showDate.collectAsState().value || !prefs.showTime.collectAsState().value
        val calendar = prefs.calendar.collectAsState().value

        SegmentedListColumn(
            title = stringResource(id = R.string.smartspace_date_and_time),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            val count = 4
            val supportCustomizationFormat = calendar.formatCustomizationSupport
            if (supportCustomizationFormat) {
                SwitchPreference(
                    pref = prefs.showDate,
                    shapes = ListItemDefaults.segmentedShapes(
                        0,
                        count
                    ),
                    title = stringResource(id = R.string.smartspace_date),
                    enabledIf = { if (prefs.showDate.collectAsState().value) !calendarHasMinimumContent else true }
                )
            }
            if (supportCustomizationFormat && prefs.showDate.collectAsState().value) {
                ListPreference(
                    prefs.calendar,
                    title = stringResource(id = R.string.smartspace_calendar),
                    entries = listPrefEntries {
                        for (value in SmartspaceCalendar.values()) {
                            entry(value, stringResource(value.nameResourceId))
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        1,
                        count
                    ),
                )
            }
            if (supportCustomizationFormat) {
                SwitchPreference(
                    pref = prefs.showTime,
                    shapes = ListItemDefaults.segmentedShapes(
                        2,
                        count
                    ),
                    title = stringResource(id = R.string.smartspace_time),
                    enabledIf = { if (prefs.showTime.collectAsState().value) !calendarHasMinimumContent else true }
                )
            }
            if (supportCustomizationFormat && prefs.showTime.collectAsState().value) {
                ListPreference(
                    prefs.timeFormat,
                    title = stringResource(id = R.string.smartspace_time_format),
                    entries = listPrefEntries {
                        for (value in SmartspaceTimeFormat.values()) {
                            entry(value, stringResource(value.nameResourceId))
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(
                        3,
                        count
                    ),
                )
            }
        }
    }

}
