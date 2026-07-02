package com.miskibin.poznajswiat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

data class Progress(
    val streaks: Map<String, Int> = emptyMap(),
    val attempts: Map<String, Int> = emptyMap(),
    val corrects: Map<String, Int> = emptyMap(),
) {
    fun streak(mode: QuizMode, cca2: String): Int = streaks["${mode.id}/$cca2"] ?: 0

    fun isMastered(mode: QuizMode, cca2: String): Boolean =
        streak(mode, cca2) >= MASTERY_STREAK

    fun masteredCount(mode: QuizMode, countries: List<Country>): Int =
        countries.count { isMastered(mode, it.cca2) }

    val totalAttempts: Int get() = attempts.values.sum()
    val totalCorrect: Int get() = corrects.values.sum()
}

class ProgressRepo(private val context: Context) {

    val progress: Flow<Progress> = context.dataStore.data.map { prefs ->
        val streaks = mutableMapOf<String, Int>()
        val attempts = mutableMapOf<String, Int>()
        val corrects = mutableMapOf<String, Int>()
        prefs.asMap().forEach { (key, value) ->
            val name = key.name
            val v = value as? Int ?: return@forEach
            when {
                name.startsWith("s/") -> streaks[name.removePrefix("s/")] = v
                name.startsWith("a/") -> attempts[name.removePrefix("a/")] = v
                name.startsWith("c/") -> corrects[name.removePrefix("c/")] = v
            }
        }
        Progress(streaks, attempts, corrects)
    }

    suspend fun record(mode: QuizMode, cca2: String, correct: Boolean) {
        val id = "${mode.id}/$cca2"
        context.dataStore.edit { prefs ->
            val sKey = intPreferencesKey("s/$id")
            val aKey = intPreferencesKey("a/$id")
            val cKey = intPreferencesKey("c/$id")
            prefs[aKey] = (prefs[aKey] ?: 0) + 1
            if (correct) {
                prefs[cKey] = (prefs[cKey] ?: 0) + 1
                prefs[sKey] = (prefs[sKey] ?: 0) + 1
            } else {
                prefs[sKey] = 0
            }
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
