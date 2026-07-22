package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import kotlinx.coroutines.flow.flowOf

class RemoteDataSource(ctx: Context, private val provider: IBottomBarProvider, val cn: ComponentName) : SmartspaceDataSource(
    ctx,
    provider.getName(ConfigurationCompat.getLocales(ctx.resources.configuration)[0].toString()),
    BottomBarPreferences.RemoteBottomBarProviderDataSourcePreference(provider)
) {
    override val isAvailable: Boolean = provider.isAvailableFunction
    override var internalTargets = flowOf(provider.targets)
    override val disabledTargets: List<SmartspaceTarget> = provider.disabledTargetsFunction
    override suspend fun requiresSetup(): Boolean = provider.requiresSetup()
    override suspend fun startSetup(activity: Activity) {
        provider.startSetup()
    }
    fun refresh() {
        internalTargets = flowOf(provider.targets)
    }
}
