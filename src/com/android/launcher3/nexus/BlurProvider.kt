package com.android.launcher3.nexus

import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.android.launcher3.R

class BlurProvider(resources: Resources) {

    companion object {
        private fun lerp(start: Float, stop: Float, amount: Float): Float {
            return start + (stop - start) * amount
        }

    }

    val minBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.min_window_blur_radius).toFloat()
    }

    val maxBlurRadius by lazy {
        resources.getDimensionPixelSize(R.dimen.max_window_blur_radius).toFloat()
    }

    fun applyDialogBlur(dialogWindow: Window, appWindow: Window, ratio: Float) {
        applyBlurToView(appWindow.decorView, ratio)
    }
    fun applyBlurToWindow(window: Window, ratio: Float) {
        val radius = blurRadiusOfRatio(ratio)
        window.setBackgroundBlurRadius(radius)
    }
    fun applyBlurToView(view: View, ratio: Float): Boolean {
        val radius = blurRadiusOfRatio(ratio)
        if(radius == 0){
            view.setRenderEffect(null)
        }else {
            val renderEffect = RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.MIRROR)
            view.setRenderEffect(renderEffect)
        }
        return true
    }

    internal fun Window.clearDimming() {
        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    internal fun Window.addDimming() {
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    internal fun blurRadiusOfRatio(ratio: Float): Int {
        return if (ratio == 0.0f) 0 else lerp(minBlurRadius, maxBlurRadius, ratio).toInt()
    }

}