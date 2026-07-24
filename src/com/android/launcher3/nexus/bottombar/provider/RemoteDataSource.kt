package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.ConfigurationCompat
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RemoteDataSource(ctx: Context, private val provider: IBottomBarProvider, val cn: ComponentName) : SmartspaceDataSource(
    ctx,
    provider.getName(ConfigurationCompat.getLocales(ctx.resources.configuration)[0].toString()),
    BottomBarPreferences.RemoteBottomBarProviderDataSourcePreference(provider)
) {
    override val isAvailable: Boolean = provider.isAvailableFunction
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val refreshFlow = MutableSharedFlow<Long>()
    override val internalTargets = refreshFlow
        .map { _ ->
            provider.getTargets(ConfigurationCompat.getLocales(ctx.resources.configuration)[0].toString())
        }
    override val disabledTargets: List<SmartspaceTarget> = provider.disabledTargetsFunction
    override suspend fun requiresSetup(): Boolean = provider.requiresSetup()
    override suspend fun startSetup(activity: Activity) {
        provider.startSetup()
    }
    init {
        scope.launch {
            enabledPref.flow.collect {
                provider.forceRefresh()
            }
        }
    }
    fun refresh() {
        scope.launch {
            refreshFlow.emit(System.currentTimeMillis())
        }
    }
}
