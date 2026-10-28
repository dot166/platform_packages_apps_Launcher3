package com.android.launcher3.nexus.bottombar.provider

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.BcSmartSpaceUtil
import com.android.launcher3.nexus.bottombar.lawnchair.BlankActivity
import com.android.launcher3.nexus.bottombar.lawnchair.util.Temperature
import com.android.launcher3.nexus.bottombar.lawnchair.util.broadcastReceiverFlow
import com.android.launcher3.nexus.bottombar.lawnchair.util.round
import com.android.launcher3.nexus.bottombar.model.SmartspaceIconView
import com.android.launcher3.nexus.bottombar.model.SmartspaceScores
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.model.SmartspaceTextView
import com.android.launcher3.nexus.bottombar.model.SmartspaceView
import com.android.launcher3.nexus.bottombar.provider.BreezyWeatherResources.getWeatherIcon
import io.github.dot166.jlib.app.devtools.logError
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale
import java.util.Locale.getDefault
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class DateProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.smartspace_weather),
        prefs.smartspaceWeather,
    ) {
    override val isAvailable = isPackageInstalled(WEATHER_PACKAGE)

    val intentFilter = IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED).apply {
        addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(BreezyReceiver.INTERNAL_BROADCAST_CHANGED)
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
    }

    override val disabledTargets = listOf(getDisabledSmartspaceTarget())

    override suspend fun requiresSetup() = context.checkSelfPermission("org.breezyweather.READ_PROVIDER") != PackageManager.PERMISSION_GRANTED

    override suspend fun startSetup(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        BlankActivity.startBlankActivityForResult(activity, intent)
        Toast.makeText(activity, R.string.reminder_enable_source_toast, Toast.LENGTH_LONG).show()
    }

    fun isPackageInstalled(targetPackage: String?): Boolean {
        val packages = context.packageManager.getInstalledApplications(0)
        for (packageInfo in packages) {
            if (packageInfo.packageName == targetPackage) return true
        }
        return false
    }

    override val internalTargets = broadcastReceiverFlow(context, intentFilter, true).map { listOfNotNull(getSmartspaceTarget()) }

    fun getSmartspaceTarget(): SmartspaceTarget {
        val authority = "org.breezyweather.provider.weather"
        val uri = "content://$authority/weather".toUri().buildUpon()
            .appendQueryParameter("withDaily", "false")
            .appendQueryParameter("withHourly", "false")
            .appendQueryParameter("withMinutely", "false")
            .appendQueryParameter("withAlerts", "true")
            .appendQueryParameter("withNormals", "false")
            .build()

        val location = prefs.smartspaceWeatherLocation.get()
        try {
            val cursor = context.contentResolver.query(uri, null, "id=$location", null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val cityName = it.getString(it.getColumnIndexOrThrow("city"))
                    val weatherBlob = it.getBlob(it.getColumnIndexOrThrow("weather"))
                    if (weatherBlob != null) {
                        val jsonStr = decompressGzip(weatherBlob)
                        val json = JSONObject(jsonStr)
                        val current = json.optJSONObject("current")
                        // Fetch current weather status text
                        val statusText = current?.optString("weatherText") ?: "unknown"
                        val baseCode = current?.optString("weatherCode") ?: "UNKNOWN"
                        val hour = java.time.LocalTime.now().hour
                        val isNight = hour < 6 || hour >= 20 // breezy weather doesn't expose the sun status, just guess lol
                        val statusCode: String = when (baseCode) {
                            "clear", "partly_cloudy" -> {
                                val timeTag = if (isNight) "NIGHT" else "DAY"
                                "${baseCode}_$timeTag"
                            }
                            else -> baseCode
                        }
                        // Prepare resource name based on status: weather_{status}
                        val cleanStatus = statusCode.lowercase()?.replace(" ", "_")
                        val resourceName = "weather_$cleanStatus"
                        val tempObj = current?.optJSONObject("temperature")?.optJSONObject("temperature")
                        val temp = tempObj?.optDouble("value", Double.NaN)?.round()?.toInt() ?: -1
                        val tempUnit = tempObj?.optString("unit")?.uppercase(getDefault()) ?: ""
                        val alertsArray = json.optJSONArray("alerts")
                        val weatherData = parseWeatherData(getWeatherIcon(context, resourceName), "$temp/$tempUnit", cityName, alertsArray)
                        if (weatherData != null) {
                            val intent = Intent("$WEATHER_PACKAGE.Main").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("MAIN_ACTIVITY_LOCATION_FORMATTED_ID", location)
                            }
                            return SmartspaceTarget(
                                id = "smartspaceWidgetWeather",
                                icon = weatherData.icon,
                                title = "",
                                subtitle = weatherData.getTitle(withPlace = true, withAlerts = true),
                                contentDescription = statusText,
                                intent = intent,
                                tiles = getTiles(),
                                score = SmartspaceScores.SCORE_WEATHER,
                                featureType = SmartspaceTarget.FeatureType.INTERNAL_FEATURE_WEATHER_DATE_TIME,
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logError { "Provider query failed $e" }
        }

        return SmartspaceTarget(
            id = "date",
            title = "",
            intent = BcSmartSpaceUtil.getOpenCalendarIntent(),
            tiles = getTiles(),
            featureType = SmartspaceTarget.FeatureType.INTERNAL_FEATURE_WEATHER_DATE_TIME,
        )
    }

    fun getDisabledSmartspaceTarget(): SmartspaceTarget {
        return SmartspaceTarget(
            id = "date",
            title = "",
            intent = BcSmartSpaceUtil.getOpenCalendarIntent(),
            tiles = getTiles(),
            featureType = SmartspaceTarget.FeatureType.INTERNAL_FEATURE_WEATHER_DATE_TIME,
        )
    }

    private fun decompressGzip(contentBytes: ByteArray): String {
        return try {
            val out = StringBuilder()
            GZIPInputStream(ByteArrayInputStream(contentBytes)).use { gzip ->
                InputStreamReader(gzip, "UTF-8").use { reader ->
                    val buffer = CharArray(1024)
                    var n: Int
                    while (reader.read(buffer).also { n = it } != -1) {
                        out.append(buffer, 0, n)
                    }
                }
            }
            out.toString()
        } catch (e: Exception) {
            Log.e("WeatherReceiver", "Decompression failed", e)
            ""
        }
    }

    data class WeatherData(
        val icon: Icon,
        private val temperature: Temperature,
        private val location: String,
        private val alerts: List<WeatherAlert>,
    ) {

        fun getTitle(unit: Temperature.Unit = temperature.unit, withPlace: Boolean = false, withAlerts: Boolean = false): String {
            return "${temperature.inUnit(unit)}${unit.suffix} ${if (withPlace && location.isNotBlank()) " - $location" else ""} ${if (withAlerts && alerts.isNotEmpty()) alerts.getInfo() else ""}"
        }

        private fun List<WeatherAlert>.getInfo(): String {
            val time = System.currentTimeMillis()
            val sorted = this.filter { it.startDate <= time && (it.endDate >= time || it.endDate == 0L) && it.headline.isNotBlank()}
                .sortedByDescending { it.severity }
            val str = StringBuilder()
            for (alert in sorted) {
                str.append(" - ${alert.headline}")
            }
            return str.toString()
        }
    }

    data class WeatherAlert(
        //val alertId: String,
        val startDate: Long = 0,
        val endDate: Long = 0,
        val headline: String = "",
        val severity: Int = 0,
    )

    fun getTiles(): List<SmartspaceView> {
        val tiles = mutableListOf<SmartspaceView>()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val info = am.nextAlarmClock
        if (info != null) {
            val alarmTime = Calendar.getInstance()
            alarmTime.setTimeInMillis(info.triggerTime)
            val skeleton = if (DateFormat.is24HourFormat(context)) "EHm" else "Ehma"
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
            val alarm = DateFormat.format(pattern, alarmTime) as String
            if (!TextUtils.isEmpty(alarm)) {
                val description =
                    context.getString(R.string.next_alarm_description, alarm)
                tiles.add(
                    SmartspaceIconView(
                        icon = Icon.createWithResource(context, R.drawable.alarm_24px),
                        contentDescription = description,
                    )
                )
                tiles.add(
                    SmartspaceTextView(
                        text = alarm,
                        contentDescription = description,
                    )
                )
            }
        }
        val notificationManager = context.getSystemService<NotificationManager>()!!
        val filter = notificationManager.currentInterruptionFilter
        val isDndOn = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (isDndOn) {
            tiles.add(
                SmartspaceIconView(
                    icon = Icon.createWithResource(context, R.drawable.do_not_disturb_on_24px),
                    contentDescription = "Do not disturb enabled",
                )
            )
        }
        return tiles
    }

    companion object {
        const val WEATHER_PACKAGE = "org.breezyweather"

        fun parseWeatherData(
            weatherIcon: Icon?,
            temperature: String?,
            location: String?,
            alertsArray: JSONArray?
        ): WeatherData? {
            return if (weatherIcon != null && temperature != null) {
                try {
                    val alerts = parseAlerts(alertsArray ?: JSONArray())
                    val value = temperature.substring(0, temperature.indexOfFirst { (it !in '0'..'9') && it != '-' }).toInt()
                    WeatherData(
                        weatherIcon,
                        Temperature(
                            value,
                            when {
                                temperature.contains("C") -> Temperature.Unit.Celsius
                                temperature.contains("F") -> Temperature.Unit.Fahrenheit
                                temperature.contains("K") -> Temperature.Unit.Kelvin
                                else -> throw IllegalArgumentException("only supports C, F and K")
                            },
                        ),
                        location ?: "",
                        alerts
                    )
                } catch (e: NumberFormatException) {
                    logError { e.message.toString() }
                    null
                } catch (e: IllegalArgumentException) {
                    logError { e.message.toString() }
                    null
                }
            } else {
                null
            }
        }

        private fun parseAlerts(alertsArray: JSONArray): List<WeatherAlert> {
            val finalArray = mutableListOf<WeatherAlert>()
            for (i in 0 until alertsArray.length()) {
                val alert = alertsArray.optJSONObject(i)
                finalArray.add(
                    WeatherAlert(
                        startDate = alert.optLong("startDate"),
                        endDate = alert.optLong("endDate"),
                        headline = alert.optString("headline"),
                        severity = alert.optInt("severity"),
                    )
                )
            }
            return finalArray
        }
    }
}
