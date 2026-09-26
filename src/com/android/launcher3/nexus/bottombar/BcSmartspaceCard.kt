package com.android.launcher3.nexus.bottombar

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.layoutDirection
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.model.SmartspaceIconView
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.model.SmartspaceTextView
import com.android.launcher3.nexus.bottombar.model.SmartspaceView
import java.util.Locale

class BcSmartspaceCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private var dateView: IcuDateTextView? = null
    private var extrasGroup: ViewGroup? = null
    private var iconDrawable: DoubleShadowIconDrawable? = null
    private var iconTintColor = 0
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
    }

    fun setSmartspaceTarget(target: SmartspaceTarget, multipleCards: Boolean) {
        this.target = target
        usePageIndicatorUi = multipleCards

        iconDrawable = BcSmartSpaceUtil.getIconDrawable(target.icon, context)
            ?.let { DoubleShadowIconDrawable(it, context) }

        var title: CharSequence? = target.title
        var subtitle = target.subtitle
        val hasTitle = target.featureType == SmartspaceTarget.FeatureType.INTERNAL_FEATURE_DATE_TIME ||
            !title.isNullOrEmpty()
        val hasSubtitle = !subtitle.isNullOrEmpty()
        if (!hasTitle) {
            title = subtitle
        }
        val contentDescription = target.contentDescription
        setTitle(title, contentDescription, hasTitle != hasSubtitle)
        if (!hasTitle || !hasSubtitle) {
            subtitle = null
        }
        setSubtitle(subtitle, target.contentDescription)
        updateIconTint()

        extrasGroup?.let {
            it.removeAllViews()
            for (view in target.tiles) {
                it.addView(view.inflateView(context))
            }
        }

        BcSmartSpaceUtil.setOnClickListener(this, target, null, "BcSmartspaceCard")
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
        val isRTL = Locale.getDefault().layoutDirection == LAYOUT_DIRECTION_RTL
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
            SmartspaceTarget.FeatureType.INTERNAL_FEATURE_DATE_TIME -> icon.setTintList(null)
            else -> icon.setTint(iconTintColor)
        }
    }

    fun SmartspaceView.inflateView(ctx: Context): View {
        when (this) {
            is SmartspaceIconView -> {
                if (color != null) {
                    icon.tintList = ColorStateList.valueOf(color!!)
                }
                return ImageView(ctx).apply {
                    setImageIcon(icon)
                    contentDescription = this@inflateView.contentDescription
                    layoutParams = LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size),
                        resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_icon_size)
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = (4 * resources.displayMetrics.density).toInt()
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }

            }

            is SmartspaceTextView -> {
                return DoubleShadowTextView(ctx).apply {
                    text = this@inflateView.text
                    setTextAppearance(R.style.EnhancedSmartspaceTextSubtitle)
                    layoutParams = LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = (4 * resources.displayMetrics.density).toInt()
                    }
                }
            }

            else -> {
                return DoubleShadowTextView(ctx).apply {
                    text = "Unsupported View"
                    setTextAppearance(R.style.EnhancedSmartspaceTextSubtitle)
                    layoutParams = LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = (4 * resources.displayMetrics.density).toInt()
                    }
                }
            }
        }
    }
}
