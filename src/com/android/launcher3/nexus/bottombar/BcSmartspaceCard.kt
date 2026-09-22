package com.android.launcher3.nexus.bottombar

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.layoutDirection
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.model.SmartspaceAction
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import java.util.Locale
import java.util.UUID

class BcSmartspaceCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private var dateView: IcuDateTextView? = null
    private var dndImageView: ImageView? = null
    private var extrasGroup: ViewGroup? = null
    private var iconDrawable: DoubleShadowIconDrawable? = null
    private var iconTintColor = 0
    private var nextAlarmImageView: ImageView? = null
    private var nextAlarmTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private lateinit var target: SmartspaceTarget
    private var titleTextView: TextView? = null
    private var topPadding = 0
    private var usePageIndicatorUi = false

    override fun onFinishInflate() {
        super.onFinishInflate()
        dateView = findViewById(R.id.date)
        titleTextView = findViewById(R.id.title_text)
        subtitleTextView = findViewById(R.id.subtitle_text)
        extrasGroup = findViewById(R.id.smartspace_extras_group)
        topPadding = paddingTop
        extrasGroup?.let {
            dndImageView = it.findViewById(R.id.dnd_icon)
            nextAlarmImageView = it.findViewById(R.id.alarm_icon)
            nextAlarmTextView = it.findViewById(R.id.alarm_text)
        }
    }

    fun setSmartspaceTarget(target: SmartspaceTarget, multipleCards: Boolean) {
        this.target = target
        val headerAction = target.headerAction
        usePageIndicatorUi = multipleCards

        if (headerAction != null) {
            iconDrawable = BcSmartSpaceUtil.getIconDrawable(headerAction.icon, context)
                ?.let { DoubleShadowIconDrawable(it, context) }

            var title: CharSequence? = headerAction.title
            var subtitle = headerAction.subtitle
            val hasTitle = target.featureType == SmartspaceTarget.FeatureType.FEATURE_WEATHER ||
                !title.isNullOrEmpty()
            val hasSubtitle = !subtitle.isNullOrEmpty()
            if (!hasTitle) {
                title = subtitle
            }
            val contentDescription = headerAction.contentDescription
            setTitle(title, contentDescription, hasTitle != hasSubtitle)
            if (!hasTitle || !hasSubtitle) {
                subtitle = null
            }
            setSubtitle(subtitle, headerAction.contentDescription)
            updateIconTint()
        }

        dateView?.let {
            val calendarAction = SmartspaceAction(
                id = headerAction?.id ?: UUID.randomUUID().toString(),
                title = "unusedTitle",
                intent = BcSmartSpaceUtil.getOpenCalendarIntent(),
            )
            BcSmartSpaceUtil.setOnClickListener(it, calendarAction, null, "BcSmartspaceCard")
        }

        BcSmartSpaceUtil.setOnClickListener(this, headerAction, null, "BcSmartspaceCard")
    }

    fun setPrimaryTextColor(textColor: Int) {
        titleTextView?.setTextColor(textColor)
        dateView?.setTextColor(textColor)
        subtitleTextView?.setTextColor(textColor)
        iconTintColor = textColor
        updateIconTint()
    }

    fun setTitle(title: CharSequence?, contentDescription: CharSequence?, hasIcon: Boolean) {
        val titleView = titleTextView ?: return
        val isRTL = Locale.getDefault().layoutDirection == View.LAYOUT_DIRECTION_RTL
        titleView.textAlignment = if (isRTL) TEXT_ALIGNMENT_TEXT_END else TEXT_ALIGNMENT_TEXT_START
        titleView.text = title
        titleView.setCompoundDrawablesRelative(
            if (hasIcon) iconDrawable else null,
            null,
            null,
            null,
        )
        titleView.ellipsize = if (target.featureType == SmartspaceTarget.FeatureType.FEATURE_CALENDAR &&
            Locale.ENGLISH.language == context.resources.configuration.locale.language
        ) {
            TextUtils.TruncateAt.MIDDLE
        } else {
            TextUtils.TruncateAt.END
        }
        if (hasIcon) {
            setFormattedContentDescription(titleView, title, contentDescription)
        }
    }

    private fun setSubtitle(subtitle: CharSequence?, charSequence2: CharSequence?) {
        val subtitleView = subtitleTextView ?: return
        subtitleView.text = subtitle
        subtitleTextView!!.setCompoundDrawablesRelative(
            if (subtitle.isNullOrEmpty()) null else iconDrawable,
            null,
            null,
            null,
        )
        subtitleTextView!!.maxLines = if (target.featureType == SmartspaceTarget.FeatureType.FEATURE_TIPS && !usePageIndicatorUi) 2 else 1
        setFormattedContentDescription(subtitleTextView!!, subtitle, charSequence2)
    }

    private fun setFormattedContentDescription(
        textView: TextView,
        title: CharSequence?,
        contentDescription: CharSequence?,
    ) {
        textView.contentDescription = when {
            title.isNullOrEmpty() -> contentDescription

            !contentDescription.isNullOrEmpty() -> context.getString(
                R.string.generic_smartspace_concatenated_desc,
                contentDescription,
                title,
            )

            else -> title
        }
    }

    private fun updateIconTint() {
        val icon = iconDrawable ?: return
        when (target.featureType) {
            SmartspaceTarget.FeatureType.FEATURE_WEATHER -> icon.setTintList(null)
            else -> icon.setTint(iconTintColor)
        }
    }
}
