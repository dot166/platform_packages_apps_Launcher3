/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.nexus.bottombar.lawnchair.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.view.View

@SuppressLint("DiscouragedApi")
private val pendingIntentTagId =
    Resources.getSystem().getIdentifier("pending_intent_tag", "id", "android")

val View?.pendingIntent get() = this?.getTag(pendingIntentTagId) as? PendingIntent

fun Context.checkPackagePermission(packageName: String, permissionName: String): Boolean {
    try {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.forEachIndexed { index, s ->
            if (s == permissionName) {
                return info.requestedPermissionsFlags?.get(index)?.hasFlag(REQUESTED_PERMISSION_GRANTED)!!
            }
        }
    } catch (_: PackageManager.NameNotFoundException) {
    }
    return false
}

