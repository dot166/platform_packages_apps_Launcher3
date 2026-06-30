package com.android.launcher3.nexus.bottombar

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.settingslib.spa.framework.theme.SettingsTheme

class BottomBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewPager = ViewPager2(context)

    init {
        viewPager.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.adapter = ComposePagerAdapter()
        addView(viewPager)
    }

    private class ComposePagerAdapter : RecyclerView.Adapter<ComposeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeViewHolder {
            val composeView = ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            }
            return ComposeViewHolder(composeView)
        }

        override fun onBindViewHolder(holder: ComposeViewHolder, position: Int) {
            // TODO: Move rendering to service interface to allow other apps to compose here
            holder.composeView.setContent {
                SettingsTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Page: $position")
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = 4
    }

    private class ComposeViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
}
