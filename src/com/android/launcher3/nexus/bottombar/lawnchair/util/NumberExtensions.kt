package com.android.launcher3.nexus.bottombar.lawnchair.util

import kotlin.math.floor

fun Double.round() = floor(x = this).let {
    if (this - it >= 0.5) it + 1 else it
}
