package com.miskibin.poznajswiat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.min

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

/**
 * Spaced-repetition intervals in days, indexed by streak after a correct
 * answer (streak 1 -> review tomorrow, 2 -> in 2 days, ...).
 */
private val SRS_INTERVALS = intArrayOf(1, 2, 4, 7, 14, 30, 60, 120)

fun itemId(mode: QuizMode, key: String): String = "${mode.id}/$key"

const val XP_PER_CORRECT = 10
const val XP_PER_LEVEL = 150

data class Progress(
    val streaks: Map<String, Int> = emptyMap(),
    val attempts: Map<String, Int> = emptyMap(),
    val corrects: Map<String, Int> = emptyMap(),
    val dues: Map<String, Long> = emptyMap(),
    val dayStreak: Int = 0,
    val lastActiveDay: Long = 0,
    val xp: Int = 0,
) {
    val level: Int get() = xp / XP_PER_LEVEL + 1

    /** 0..1 progress towards the next level. */
    val levelFraction: Float get() = (xp % XP_PER_LEVEL).toFloat() / XP_PER_LEVEL

    fun streak(id: String): Int = streaks[id] ?: 0

    fun streak(mode: QuizMode, key: String): Int = streak(itemId(mode, key))

    fun isMastered(mode: QuizMode, key: String): Boolean =
        streak(mode, key) >= MASTERY_STREAK

    fun masteredCount(mode: QuizMode, keys: Iterable<String>): Int =
        keys.count { isMastered(mode, it) }

    /** Items answered before whose review date has arrived. */
    fun dueItems(today: Long): List<String> =
        dues.entries.filter { it.value <= today && (attempts[it.key] ?: 0) > 0 }
            .sortedBy { it.value }
            .map { it.key }

    /** Day streak shown in the UI: broken if the user skipped a whole day. */
    fun currentDayStreak(today: Long): Int =
        if (today - lastActiveDay <= 1) dayStreak else 0

    val totalAttempts: Int get() = attempts.values.sum()
    val totalCorrect: Int get() = corrects.values.sum()
}

class ProgressRepo(private val context: Context) {

    val progress: Flow<Progress> = context.dataStore.data.map { prefs ->
        val streaks = mutableMapOf<String, Int>()
        val attempts = mutableMapOf<String, Int>()
        val corrects = mutableMapOf<String, Int>()
        val dues = mutableMapOf<String, Long>()
        var dayStreak = 0
        var lastDay = 0L
        var xp = 0
        prefs.asMap().forEach { (key, value) ->
            val name = key.name
            when {
                name.startsWith("s/") -> streaks[name.removePrefix("s/")] = value as? Int ?: 0
                name.startsWith("a/") -> attempts[name.removePrefix("a/")] = value as? Int ?: 0
                name.startsWith("c/") -> corrects[name.removePrefix("c/")] = value as? Int ?: 0
                name.startsWith("d/") -> dues[name.removePrefix("d/")] = value as? Long ?: 0L
                name == "meta/dayStreak" -> dayStreak = value as? Int ?: 0
                name == "meta/lastDay" -> lastDay = value as? Long ?: 0L
                name == "meta/xp" -> xp = value as? Int ?: 0
            }
        }
        Progress(streaks, attempts, corrects, dues, dayStreak, lastDay, xp)
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { prefs ->
            val key = intPreferencesKey("meta/xp")
            prefs[key] = (prefs[key] ?: 0) + amount
        }
    }

    suspend fun record(id: String, correct: Boolean, today: Long = LocalDate.now().toEpochDay()) {
        context.dataStore.edit { prefs ->
            val sKey = intPreferencesKey("s/$id")
            val aKey = intPreferencesKey("a/$id")
            val cKey = intPreferencesKey("c/$id")
            val dKey = longPreferencesKey("d/$id")
            prefs[aKey] = (prefs[aKey] ?: 0) + 1
            if (correct) {
                val streak = (prefs[sKey] ?: 0) + 1
                prefs[cKey] = (prefs[cKey] ?: 0) + 1
                prefs[sKey] = streak
                prefs[dKey] = today + SRS_INTERVALS[min(streak - 1, SRS_INTERVALS.size - 1)]
                val xpKey = intPreferencesKey("meta/xp")
                prefs[xpKey] = (prefs[xpKey] ?: 0) + XP_PER_CORRECT
            } else {
                prefs[sKey] = 0
                prefs[dKey] = today
            }

            val lastDayKey = longPreferencesKey("meta/lastDay")
            val dayStreakKey = intPreferencesKey("meta/dayStreak")
            val lastDay = prefs[lastDayKey] ?: 0L
            when {
                lastDay == today -> Unit
                lastDay == today - 1 -> prefs[dayStreakKey] = (prefs[dayStreakKey] ?: 0) + 1
                else -> prefs[dayStreakKey] = 1
            }
            prefs[lastDayKey] = today
        }
    }

    companion object {
        @Volatile
        private var instance: ProgressRepo? = null

        fun get(context: Context): ProgressRepo =
            instance ?: synchronized(this) {
                instance ?: ProgressRepo(context.applicationContext).also { instance = it }
            }
    }
}
