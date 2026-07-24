package com.android.launcher3.nexus.bottombar.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class BottomBarPreference @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : Preference(ctx, attrs, defStyleAttr, defStyleRes) {

    init {
        intent = Intent(ctx, BottomBarPreferenceActivity::class.java)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val widgetFrame = holder.findViewById(android.R.id.widget_frame)
        if (widgetFrame != null) {
            widgetFrame.visibility = View.GONE
        }
    }
}
