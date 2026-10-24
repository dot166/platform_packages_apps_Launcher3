package com.android.launcher3

import android.content.Context
import com.android.launcher3.nexus.bottombar.preference.SmartspacePrefStore
import dev.patrickgold.jetpref.datastore.runtime.initAndroid
import kotlinx.coroutines.runBlocking

object KT {
    @JvmStatic
    fun initSmartspacePrefStore(ctx: Context) {
        runBlocking {
            SmartspacePrefStore.initAndroid(ctx, "SmartspacePrefs")
        }
    }
}