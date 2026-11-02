package com.android.launcher3.nexus.bottombar.provider

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.Icon
import android.icu.text.DateFormat
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import android.widget.Toast
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.android.launcher3.R
import com.android.launcher3.nexus.bottombar.lawnchair.BlankActivity
import com.android.launcher3.nexus.bottombar.lawnchair.util.queryAsFlow
import com.android.launcher3.nexus.bottombar.model.SmartspaceIconTextView
import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget
import com.android.launcher3.nexus.bottombar.model.SmartspaceTimeFormat
import com.android.launcher3.nexus.bottombar.preference.SmartspacePrefStore
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import android.text.format.DateFormat as AndroidDateFormat

class CalendarProvider(context: Context) :
    SmartspaceDataSource(
        context,
        context.getString(R.string.smartspace_calendar),
        prefs.smartspaceCalendar,
    ) {
    override suspend fun requiresSetup() = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED

    override suspend fun startSetup(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        BlankActivity.startBlankActivityForResult(activity, intent)
    }
    val prefs by SmartspacePrefStore
    override val internalTargets: Flow<List<SmartspaceTarget>> =
        combine(calendarFlow(), prefs.timeFormat.asFlow(), prefs.smartspaceCalendarLocation.asFlow()) { a, b, c ->
            Triple(a, b, c)
        }.map { it.first.getSmartspaceTargets(it.second, it.third) }

    fun calendarFlow(): Flow<List<CalendarEvent>> {
        if (!hasPermission()) return flowOf(emptyList())
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val startOfTwoDays = startOfToday.plusDays(1)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendEncodedPath(startOfToday.toEpochMilli().toString())
            .appendEncodedPath(startOfTwoDays.toEpochMilli().toString())
            .build()
        return context.contentResolver.queryAsFlow(
            uri,
            projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DTSTART,
                CalendarContract.Instances.DTEND,
                CalendarContract.Instances.DURATION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.SELF_ATTENDEE_STATUS,
                CalendarContract.Instances.LAST_DATE
            ),
        ).debounce(250L).mapLatest {
            it.map { event ->
                val startTime = Instant.ofEpochMilli(event.getLong(2))
                var endTime = event.getLongOrNull(3)?.let { end ->
                    Instant.ofEpochMilli(end)
                }
                //Duration is used for events which are recurring rather than one off, in RFC format
                val duration = event.getStringOrNull(4)?.let {
                    RFC2245Duration().apply { parse(it) }.millis
                }
                if (duration != null && endTime == null) {
                    endTime = startTime.plusMillis(duration)
                }
                //If the duration is still not set, fall back to the unreliable LAST_DATE field
                val lastDate = event.getLongOrNull(8)
                if (lastDate != null && endTime == null) {
                    endTime = Instant.ofEpochMilli(lastDate)
                }
                if (endTime == null) return@map null
                //All day flag isn't always accurate, so we need to check the times too
                val isAllDay = event.getInt(6) == 1 || isAllDay(startTime, endTime)
                // All-day events are stored as midnight UTC, but should be midnight local
                val finalStartTime = if (isAllDay) {
                    startTime.atZone(ZoneOffset.UTC).toLocalDate()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else startTime
                val finalEndTime = if (isAllDay) {
                    endTime.atZone(ZoneOffset.UTC).toLocalDate()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else endTime
                CalendarEvent(
                    event.getString(0),
                    event.getStringOrNull(1)
                        ?: context.getString(R.string.target_calendar_title_default),
                    finalStartTime,
                    finalEndTime,
                    event.getString(5),
                    isAllDay,
                    event.getInt(7)
                )
            }.filterNotNull().filterNot { event ->
                event.status == CalendarContract.Events.STATUS_CANCELED
            }
        }
    }

    private fun ZonedDateTime.toEpochMilli(): Long {
        return toInstant().toEpochMilli()
    }

    fun <T> Cursor.map(row: (Cursor) -> T): List<T> {
        moveToFirst()
        if (isAfterLast) return emptyList()
        val list = ArrayList<T>()
        do {
            list.add(row(this))
        } while (moveToNext())
        return list
    }

    fun List<CalendarEvent>.getSmartspaceTargets(timeFormat: SmartspaceTimeFormat, showLocation: Boolean) = map {
        it.getTarget(timeFormat, showLocation)
    }

    fun CalendarEvent.getTarget(timeFormat: SmartspaceTimeFormat, showLocation: Boolean): SmartspaceTarget {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLong())
        }
        val subComplication = location?.let {
            if (!showLocation || location.isBlank()) return@let null
            listOf(
                SmartspaceIconTextView(
                    Icon.createWithResource(
                        context, R.drawable.ic_target_calendar_location
                    ),
                    location,
                    location,
                )
            )
        } ?: listOf()
        val title = title.ifBlank {
            context.resources.getString(R.string.target_calendar_title_default)
        }.formatTitle(startTime)
        return SmartspaceTarget(
            "calendar_$id",
            Icon.createWithResource(context, R.drawable.ic_target_calendar),
            title,
            getSubtitle(timeFormat),
            intent = intent,
            tiles = subComplication,
            featureType = SmartspaceTarget.FeatureType.FEATURE_CALENDAR,
        )
    }

    private fun isAllDay(startTime: Instant, endTime: Instant): Boolean {
        val utcStartTime = startTime.atZone(ZoneOffset.UTC)
        val utcEndTime = endTime.atZone(ZoneOffset.UTC)
        val isMidnight =
            utcStartTime.hour == 0 && utcStartTime.minute == 0 && utcStartTime.second == 0 &&
                    utcEndTime.hour == 0 && utcEndTime.minute == 0 && utcEndTime.second == 0
        val isFullDay = Duration.between(startTime, endTime).toHours() % 24 == 0L
        return isMidnight && isFullDay && !startTime.isAfter(endTime) && startTime != endTime
    }

    private fun String.formatTitle(startTime: Instant): String {
        val now = ZonedDateTime.now().atStartOfMinute().toInstant()
        val minutes = if (!startTime.isBefore(now)) {
            Duration.between(now, startTime).toMinutes()
        } else 0
        return if (minutes in 1..60) {
            context.getString(
                R.string.target_calendar_title_with_time, this, minutes
            )
        } else {
            this
        }
    }

    private fun CalendarEvent.getSubtitle(timeFormat: SmartspaceTimeFormat): String {
        val now = LocalDateTime.now()
        val format: String = context.getString(
            when {
                timeFormat is SmartspaceTimeFormat.TwelveHourFormat -> R.string.smartspace_icu_date_pattern_gregorian_time_12h
                timeFormat is SmartspaceTimeFormat.TwentyFourHourFormat -> R.string.smartspace_icu_date_pattern_gregorian_time
                is24HourFormat(context) -> R.string.smartspace_icu_date_pattern_gregorian_time
                else -> R.string.smartspace_icu_date_pattern_gregorian_time_12h
            },
        )
        val dateFormat = DateFormat.getInstanceForSkeleton(format, Locale.getDefault())
        val start = dateFormat.format(Date.from(startTime))
        val end = dateFormat.format(Date.from(endTime))
        val startTime = startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val endTime = endTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        if (isAllDay) {
            return context.resources.getString(R.string.target_calendar_subtitle_all_day)
        }
        return when {
            startTime.isSameDay(now) && endTime.isSameDay(now) -> "$start – $end"
            startTime.isYesterday(now) -> {
                context.resources.getString(
                    R.string.target_calendar_subtitle_with_yesterday,
                    start,
                    end
                )
            }

            endTime.isTomorrow(now) -> {
                context.resources.getString(
                    R.string.target_calendar_subtitle_with_tomorrow,
                    start,
                    end
                )
            }

            startTime.isSameDay(now) -> {
                context.resources.getString(
                    R.string.target_calendar_subtitle_with_date_for_end,
                    start,
                    this.endTime.getDate(),
                    end,
                )
            }

            endTime.isSameDay(now) -> {
                context.resources.getString(
                    R.string.target_calendar_subtitle_with_date_for_start,
                    this.startTime.getDate(),
                    start,
                    end
                )
            }

            else -> "$start – $end"
        }
    }

    private fun LocalDateTime.isSameDay(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    private fun LocalDateTime.isYesterday(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear - 1
    }

    private fun LocalDateTime.isTomorrow(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear + 1
    }

    private fun Instant.getDate(): String {
        val format = AndroidDateFormat.getDateFormat(context)
        return format.format(Date.from(this))
    }

    fun ZonedDateTime.atStartOfMinute(): ZonedDateTime {
        return withSecond(0).withNano(0)
    }

    data class CalendarEvent(
        val id: String,
        val title: String,
        val startTime: Instant,
        val endTime: Instant,
        val location: String?,
        val isAllDay: Boolean,
        val status: Int
    )

    /**
     * According to RFC2445, durations are like this:
     * WEEKS
     * | DAYS [ HOURS [ MINUTES [ SECONDS ] ] ]
     * | HOURS [ MINUTES [ SECONDS ] ]
     * it doesn't specifically, say, but this sort of implies that you can't have
     * 70 seconds.
     */
    class RFC2245Duration {
        private var sign // 1 or -1
                = 1
        private var weeks = 0
        private var days = 0
        private var hours = 0
        private var minutes = 0
        private var seconds = 0

        /**
         * Parse according to RFC2445 ss4.3.6.  (It's actually a little loose with
         * its parsing, for better or for worse)
         */
        @Throws(RuntimeException::class)
        fun parse(str: String) {
            sign = 1
            weeks = 0
            days = 0
            hours = 0
            minutes = 0
            seconds = 0
            val len = str.length
            var index = 0
            var c: Char
            if (len < 1) {
                return
            }
            c = str[0]
            if (c == '-') {
                sign = -1
                index++
            } else if (c == '+') {
                index++
            }
            if (len < index) {
                return
            }
            c = str[index]
            if (c != 'P') {
                throw RuntimeException(
                    "Duration.parse(str='" + str + "') expected 'P' at index="
                            + index
                )
            }
            index++
            c = str[index]
            if (c == 'T') {
                index++
            }
            var n = 0
            while (index < len) {
                c = str[index]
                if (c >= '0' && c <= '9') {
                    n *= 10
                    n += (c.code - '0'.code)
                } else if (c == 'W') {
                    weeks = n
                    n = 0
                } else if (c == 'H') {
                    hours = n
                    n = 0
                } else if (c == 'M') {
                    minutes = n
                    n = 0
                } else if (c == 'S') {
                    seconds = n
                    n = 0
                } else if (c == 'D') {
                    days = n
                    n = 0
                } else if (c == 'T') {
                } else {
                    throw RuntimeException(
                        "Duration.parse(str='" + str + "') unexpected char '"
                                + c + "' at index=" + index
                    )
                }
                index++
            }
        }

        val millis: Long
            get() {
                val factor = (1000 * sign).toLong()
                return factor * (7 * 24 * 60 * 60 * weeks + 24 * 60 * 60 * days + 60 * 60 * hours + 60 * minutes
                        + seconds)
            }
    }

    fun hasPermission(): Boolean {
        return context.hasPermission(Manifest.permission.READ_CALENDAR)
    }
    fun Context.hasPermission(vararg permission: String): Boolean {
        return permission.all { checkCallingOrSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
}