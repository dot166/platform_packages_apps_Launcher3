package com.android.launcher3.nexus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Process
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.android.launcher3.R
import com.google.android.launcherclient.Constant


class FeedProviderPreference @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0): ListPreference(ctx, attrs, defStyleAttr, defStyleRes) {
    init {
        Log.i("-1", Constant.ACTION)
        val intent = Intent(Constant.ACTION)
            .setData(
                StringBuilder(ctx.packageName.toString().length + 18)
                    .append("app://")
                    .append(ctx.packageName)
                    .append(":")
                    .append(Process.myUid())
                    .toString().toUri().buildUpon()
                    .appendQueryParameter("v", 9.toString())
                    .appendQueryParameter("cv", 14.toString())
                    .build()
            );
        val services: MutableList<ResolveInfo> = ctx.packageManager.queryIntentServices(
            intent,
            PackageManager.MATCH_ALL
        )
        Log.i("-1", services.size.toString())
        val labels = mutableListOf(ctx.getString(R.string.default_stub))
        val components = mutableListOf("com.android.launcher3")

        for (info in services) {
            Log.i("-1", info.serviceInfo.packageName)
            if (info.serviceInfo.packageName == ctx.packageName) {
                continue // stub is already there
            }
            val label = info.loadLabel(ctx.packageManager).toString()
            labels.add(label)
            val packageName = info.serviceInfo.packageName
            components.add("$packageName")
        }
        entries = labels.toTypedArray()
        entryValues = components.toTypedArray()
    }

    override fun setValue(value: String?) {
        super.setValue(value)
        summary = "$entry - $value"
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrame = holder.findViewById(android.R.id.widget_frame)
        if (widgetFrame != null) {
            widgetFrame.visibility = View.GONE
        }
    }
}