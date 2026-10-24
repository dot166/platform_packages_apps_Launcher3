package com.android.launcher3.nexus.bottombar.preference

import com.android.launcher3.nexus.bottombar.model.SmartspaceCalendar
import com.android.launcher3.nexus.bottombar.model.SmartspaceTimeFormat
import com.android.launcher3.nexus.bottombar.provider.IBottomBarProvider
import dev.patrickgold.jetpref.datastore.annotations.Preferences
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import dev.patrickgold.jetpref.datastore.model.PreferenceSerializer
import dev.patrickgold.jetpref.datastore.model.PreferenceType
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val SmartspacePrefStore = jetprefDataStoreOf(SmartspacePreferenceModel::class)

@Preferences
abstract class SmartspacePreferenceModel : PreferenceModel() {
    val showDate = boolean("smartspace_show_date", true)

    val calendar = custom("smartspace_calendar", SmartspaceCalendar.Gregorian, object : PreferenceSerializer<SmartspaceCalendar> {
        override fun serialize(value: SmartspaceCalendar): String {
            return value.toString()
        }

        override fun deserialize(value: String): SmartspaceCalendar {
            return SmartspaceCalendar.fromString(value)
        }
    })

    val showTime = boolean("smartspace_show_time", true)

    val timeFormat = custom("smartspace_time_format", SmartspaceTimeFormat.FollowSystem, object : PreferenceSerializer<SmartspaceTimeFormat> {
        override fun serialize(value: SmartspaceTimeFormat): String {
            return value.toString()
        }

        override fun deserialize(value: String): SmartspaceTimeFormat {
            return SmartspaceTimeFormat.fromString(value)
        }
    })

    val smartspaceBatteryStatus = boolean(
        key = "enable_smartspace_battery_status",
        default = true
    )
    val smartspaceTorch = boolean(
        key = "enable_smartspace_torch",
        default = true
    )
    val smartspaceNowPlaying = boolean(
        key = "enable_smartspace_now_playing",
        default = true
    )
    val smartspaceOnboarding = boolean(
        key = "enable_smartspace_onboarding",
        default = true
    )
    val alwaysTrue = PreferenceDataLockedToDefault(
        key = "bottomBarHiddenAlwaysTrue",
        default = true,
        type = PreferenceType.boolean(),
        serializer = BooleanPreferenceSerializer
    )

    class RemoteBottomBarProviderDataSourcePreference(private val provider: IBottomBarProvider) :
        PreferenceData<Boolean> {
        override val key: String = "SUNSTONE" // - remember it well, you might need it!
        override val default: Boolean = true // use default value for isAvailable in LibLauncher3
        override val type = PreferenceType.boolean()
        override val serializer = BooleanPreferenceSerializer
        override val typedKey = TypedKey(type, key)
        private val cachedValue = AtomicReference(default)
        private val cachedValueFlow = MutableStateFlow(default)
        private val cachedReadOnlyValueFlow = cachedValueFlow.asStateFlow()
        private val cachedValueWriteGuard = Mutex()

        override fun get(): Boolean = cachedValue.get()

        override fun getOrNull(): Boolean = cachedValue.get()

        override fun asFlow() = cachedReadOnlyValueFlow

        override suspend fun set(value: Boolean) = cachedValueWriteGuard.withLock {
            cachedValue.set(value)
            cachedValueFlow.value = value
            Result.success(provider.setEnabled(value))
        }

        override suspend fun reset() = cachedValueWriteGuard.withLock {
            cachedValue.set(default)
            cachedValueFlow.value = default
            Result.success(provider.setEnabled(default))
        }

        override suspend fun init(
            value: Boolean?,
            handler: PreferenceData.ValuePersistHandler<Boolean>,
        ) {
            // Do nothing. Absolutely nothing.
            // We are not overthinking this.
        }
    }
    object BooleanPreferenceSerializer : PreferenceSerializer<Boolean> {
        override fun serialize(value: Boolean): String = value.toString()

        override fun deserialize(value: String): Boolean = value.toBoolean()
    }

    class PreferenceDataLockedToDefault<V : Any>(
        override val key: String,
        override val default: V,
        override val type: PreferenceType,
        override val serializer: PreferenceSerializer<V>,
    ) : PreferenceData<V> {
        override val typedKey = TypedKey(type, key)

        init {
            Validator.validateKey(key)
        }

        override fun get(): V = default

        override fun getOrNull(): V = default

        override fun asFlow() = MutableStateFlow(default)

        override suspend fun set(value: V): Result<Unit> {
            // This value is tied to its default.
            return Result.failure(Exception("You may struggle, but the value remains unchanged."))
        }

        override suspend fun reset(): Result<Unit> {
            // Someone clearly didn't read the terms and conditions.
            return Result.failure(Exception("Reset is not a safeword. Nice try, though."))
        }

        override suspend fun init(
            value: V?,
            handler: PreferenceData.ValuePersistHandler<V>,
        ) {
            // Do nothing. Absolutely nothing.
            // We have already negotiated this.
        }

        internal object Validator {
            private val KEY_REGEX = """^(([a-zA-Z_])|([a-zA-Z_][a-zA-Z0-9_-]*[a-zA-Z0-9_]))${'$'}""".toRegex()

            fun validateKey(key: String): String {
                if (!KEY_REGEX.matches(key)) {
                    throw IllegalArgumentException(
                        "Preference key '$key' does not conform to the expected format of $KEY_REGEX"
                    )
                }
                return key
            }
        }
    }
}