package com.android.launcher3.nexus.bottombar

import android.app.ActivityOptions
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget

object BcSmartSpaceUtil {
    fun getIconDrawable(icon: Icon?, context: Context): Drawable? {
        if (icon == null) return null
        val drawable = icon.loadDrawable(context) ?: return null
        val iconSize =
            context.resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size)
        drawable.setBounds(0, 0, iconSize, iconSize)
        return drawable
    }

    fun setOnClickListener(
        view: View?,
        target: SmartspaceTarget,
        str: String?,
    ) {
        val options = ActivityOptions.makeBasic()
        if (Utilities.ATLEAST_U) {
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        if (view == null) {
            Log.e(str, "No tap action can be set up")
            return
        }
        view.setOnClickListener {
            runCatching {
                if (target.intent != null) {
                    view.context.startActivity(target.intent)
                } else if (target.pendingIntent != null) {
                    if (Utilities.ATLEAST_U) {
                        target.pendingIntent?.send(options.toBundle())
                    } else {
                        target.pendingIntent?.send()
                    }
                } else if (target.onClick != null) {
                    target.onClick?.run()
                }
            }
        }
    }

    fun getOpenCalendarIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).setData(
            ContentUris.appendId(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time"),
                System.currentTimeMillis(),
            ).build(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
}
