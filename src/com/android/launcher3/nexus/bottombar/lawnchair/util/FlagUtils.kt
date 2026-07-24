package com.android.launcher3.nexus.bottombar.lawnchair.util

infix fun Int.hasFlag(flag: Int): Boolean {
    return (this and flag) == flag
}
