package com.android.launcher3

import com.android.launcher3.uioverrides.QuickstepLauncher
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient

class jOSLauncher : QuickstepLauncher() {
    
    override fun onDestroy() {
        super.onDestroy()
        // Only actually closes if required, safe to call if not enabled
        SmartspacerClient.close()
    }
}