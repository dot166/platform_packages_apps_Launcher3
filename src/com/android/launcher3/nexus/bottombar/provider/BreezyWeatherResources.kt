package com.android.launcher3.nexus.bottombar.provider

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.content.res.ResourcesCompat
import com.android.launcher3.nexus.bottombar.provider.DateProvider.Companion.WEATHER_PACKAGE

object BreezyWeatherResources {

    private fun getRemoteResources(context: Context, packageName: String = WEATHER_PACKAGE): Resources? {
        return try {
            context.packageManager.getResourcesForApplication(packageName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a drawable directly using the resource name provided by the content provider.
     */
    fun getWeatherDrawable(
        context: Context,
        resourceName: String
    ): Drawable? {
        if (resourceName.isEmpty()) return null
        val resources = getRemoteResources(context) ?: return null

        val resId = resources.getIdentifier(resourceName, "drawable", WEATHER_PACKAGE)
        if (resId != 0) {
            return ResourcesCompat.getDrawable(resources, resId, null)
        }
        return null
    }

    /**
     * Resolves an icon directly using the resource name provided by the content provider.
     */
    fun getWeatherIcon(
        context: Context,
        resourceName: String
    ): Icon? {
        if (resourceName.isEmpty()) return null
        val resources = getRemoteResources(context) ?: return null

        val resId = resources.getIdentifier(resourceName, "drawable", WEATHER_PACKAGE)
        if (resId != 0) {
            return Icon.createWithResource(WEATHER_PACKAGE, resId)
        }
        return null
    }
}