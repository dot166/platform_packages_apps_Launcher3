package com.android.launcher3.nexus

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.doOnPreDraw
import com.android.launcher3.R
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.google.android.gsa.overlay.controllers.OverlayController

class DefaultOverlay(
    private val uid: Int,
    context: Context
): OverlayController(
    context,
    0,
    android.R.style.Theme_Translucent_NoTitleBar
) {

    companion object {
        private const val KEY_BACKGROUND_BLUR_PROGRESS = "background_blur_progress"
    }

    private var backgroundBlurProgress = 0f
    private val blurProvider = BlurProvider(resources)
    private var isResumed = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        container?.fitsSystemWindows = false
        window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.addFlags(Window.FEATURE_NO_TITLE)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            it.statusBarColor = Color.TRANSPARENT
            it.navigationBarColor = Color.TRANSPARENT
        }
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        container?.addView(ComposeView(this).apply {
            setContent {
                SettingsTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_activity_has_been_set_n_please_set_one_in_settings))
                    }
                }
            }
        })
        backgroundBlurProgress = bundle
            ?.getFloat(KEY_BACKGROUND_BLUR_PROGRESS, 0f) ?: 0f
    }

    override fun onPause() {
        if (!isResumed) return
        isResumed = false
        super.onPause()
    }

    override fun onDestroy(isFinishing: Boolean) {
        onPause()
        super.onDestroy(isFinishing)
    }

    override fun onStop() {
        onPause()
        super.onStop()
        blurProvider.applyBlurToWindow(window!!, 0f)
    }

    override fun onResume() {
        if (isResumed) return
        isResumed = true
        super.onResume()
        updateProgressViews(backgroundBlurProgress, true)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        updateProgressViews(progress)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putFloat(KEY_BACKGROUND_BLUR_PROGRESS, backgroundBlurProgress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false) {
        if (backgroundBlurProgress == progress && !force) return
        backgroundBlurProgress = progress
        container?.background = null
        blurProvider.applyBlurToWindow(window!!, progress)
    }

    fun View.removeStatusNavBackgroundOnPreDraw() = apply {
        doOnPreDraw {
            val statusBarBackground = it.findViewById<View>(android.R.id.statusBarBackground)
            statusBarBackground?.run {
                visibility = View.INVISIBLE
                alpha = 0f
            }
            val navigationBarBackground =
                it.findViewById<View>(android.R.id.navigationBarBackground)
            navigationBarBackground?.run {
                visibility = View.INVISIBLE
                alpha = 0f
            }
        }
    }

    override fun getSystemServiceName(serviceClass: Class<*>): String? {
        // fix compose by blocking requests to a service that doesn't exist for the 'window in a service' mess that L3 has for -1
        if (serviceClass.name.contains("ContentCapture")) {
            return null // "I don't know her. She doesn't live here."
        }
        return super.getSystemServiceName(serviceClass)
    }

}
