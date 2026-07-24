package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.preference.BottomBarPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

abstract class SmartspaceDataSource(
    val context: Context,
    val providerName: String,
    val enabledPref: BottomBarPreferences.BottomBarProviderDataSourcePreference,
) {
    open val isAvailable: Boolean = true

    protected abstract val internalTargets: Flow<List<SmartspaceTarget>>
    open val disabledTargets: List<SmartspaceTarget> = emptyList()

    private val restartSignal = MutableStateFlow(0)
    private val enabledTargets get() = internalTargets
        .onStart {
            if (requiresSetup()) throw RequiresSetupException()
        }
        .map { State(targets = it) }
        .catch {
            if (it is RequiresSetupException) {
                emit(
                    State(
                        targets = disabledTargets,
                        requiresSetup = listOf(this@SmartspaceDataSource),
                    ),
                )
            } else {
                Log.d("SmartspaceDataSource", "data source errored", it)
                enabledPref.set(false)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val targets = enabledPref.flow
        .flatMapLatest { isEnabled ->
            if (isAvailable && isEnabled) {
                restartSignal.flatMapLatest { enabledTargets }
            } else {
                flowOf(State(targets = disabledTargets))
            }
        }

    open suspend fun requiresSetup(): Boolean = false

    open suspend fun startSetup(activity: Activity) {}

    suspend fun onSetupDone() {
        if (!requiresSetup()) {
            restart()
        } else {
            enabledPref.set(false)
        }
    }

    fun restart() {
        restartSignal.value++
    }

    private class RequiresSetupException : RuntimeException()

    data class State(
        val targets: List<SmartspaceTarget> = emptyList(),
        val requiresSetup: List<SmartspaceDataSource> = emptyList(),
    ) {
        operator fun plus(other: State): State {
            return State(
                targets = this.targets + other.targets,
                requiresSetup = this.requiresSetup + other.requiresSetup,
            )
        }
    }
}
