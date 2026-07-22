package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_ALLOW_ACTIVITY_STARTS
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.nexus.bottombar.lawnchair.util.dropWhileBusy
import com.android.launcher3.nexus.bottombar.model.SmartspaceAction
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferenceActivity
import com.android.launcher3.util.DaggerSingletonObject
import com.android.launcher3.util.SafeCloseable
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn


@LauncherAppSingleton
class SmartspaceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : SafeCloseable, ServiceConnection {

    private val tagLog = javaClass.simpleName as String

    val dataSources = MutableStateFlow<List<SmartspaceDataSource>>(emptyList())
    private val providers: MutableList<Pair<ComponentName, IBottomBarProvider>> = mutableListOf()
    val action = "com.android.launcher3.nexus.bottombar.BOTTOM_BAR_PROVIDER"
    val builtInProviders = listOf(
        SmartspaceWidgetReader(context),
        BatteryStatusProvider(context),
        TorchProvider(context),
        NowPlayingProvider(context),
        OnboardingProvider(context),
        ConfigProvider(context),
    )

    init {
        val i = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        }
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    // refresh all providers
                    refreshProviders()
                }
            },
            i,
        )

        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    val cn = intent.getParcelableExtra("targetProviderCN", ComponentName::class.java)
                    val sources = dataSources.value
                    for (source in sources) {
                        if (source is RemoteDataSource) {
                            if (source.cn == cn) {
                                source.refresh()
                            }
                        }
                    }
                }
            },
            IntentFilter(BottomBarDataSource.ACTION_BOTTOM_BAR_TARGETS_UPDATED),
            ContextCompat.RECEIVER_EXPORTED,
        )

        refreshProviders()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = dataSources
        .flatMapLatest { sources ->
            sources
                .map { it.targets }
                .reduce { acc, flow -> flow.combine(acc) { a, b -> a + b } }
        }
        .shareIn(
            MainScope(),
            SharingStarted.WhileSubscribed(),
            replay = 1,
        )
    val targets = state
        .map {
            if (it.requiresSetup.isNotEmpty()) {
                listOf(setupTarget) + it.targets
            } else {
                it.targets
            }
        }
    val previewTargets = state
        .map { it.targets }

    private val setupTarget = SmartspaceTarget(
        id = "smartspaceSetup",
        headerAction = SmartspaceAction(
            id = "smartspaceSetupAction",
            title = context.getString(R.string.smartspace_requires_setup),
            intent = Intent(context, BottomBarPreferenceActivity::class.java),
        ),
        score = 999f,
        featureType = SmartspaceTarget.FeatureType.FEATURE_TIPS,
    )

    suspend fun startSetup(activity: Activity) {
        state
            .map { it.requiresSetup }
            .dropWhileBusy()
            .collect { sources ->
                sources.forEach {
                    it.startSetup(activity)
                    it.onSetupDone()
                }
            }
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    fun refreshProviders() {
        if (providers.isNotEmpty()) {
            context.unbindService(this)
        }
        providers.clear()
        dataSources.value = builtInProviders
        val intent = Intent(action)
        val services: MutableList<ResolveInfo> = context.packageManager.queryIntentServices(
            intent,
            PackageManager.MATCH_ALL,
        )

        for (info in services) {
            Log.i("BottomBar", info.serviceInfo.packageName)
            val packageName = info.serviceInfo.packageName
            val name = info.serviceInfo.name
            val cn = ComponentName.unflattenFromString("$packageName/$name")
            val service = Intent(intent).apply {
                component = cn
            }
            try {
                context.bindService(
                    service,
                    this,
                    BIND_AUTO_CREATE or BIND_ALLOW_ACTIVITY_STARTS,
                )
            } catch (e: SecurityException) {
                Log.e("BottomBar", "Unable to connect to service ${service.`package`}", e)
            }
        }
    }

    override fun onServiceConnected(
        name: ComponentName,
        service: IBinder,
    ) {
        providers.add(Pair(name, IBottomBarProvider.Stub.asInterface(service)))
        refreshProvidersFlowFromProvidersList()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        providers.removeByComponentName(name)
        refreshProvidersFlowFromProvidersList()
    }

    fun refreshProvidersFlowFromProvidersList() {
        val list = mutableListOf<SmartspaceDataSource>()
        for (provider in providers) {
            list.add(RemoteDataSource(context, provider.second, provider.first))
        }
        list.addAll(builtInProviders)
        dataSources.value = list
    }

    private fun MutableList<Pair<ComponentName, IBottomBarProvider>>.removeByComponentName(name: ComponentName) {
        for (i in indices) {
            val obj = this[i]
            if (obj.first.toString() == name.toString()) {
                remove(obj)
            }
        }
    }

    companion object {
        @JvmField
        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getSmartspaceProvider)
    }
}
