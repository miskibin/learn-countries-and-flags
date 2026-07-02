package com.miskibin.poznajswiat.data

import android.annotation.SuppressLint
import android.content.Context
import com.miskibin.poznajswiat.geo.WorldMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AppData(
    val countries: List<Country>,
    val byCode: Map<String, Country>,
    val flagRes: Map<String, Int>,
    val worldMap: WorldMap,
    val events: List<HistoryEvent>,
) {
    val eventsById: Map<Int, HistoryEvent> = events.associateBy { it.id }

    fun forContinent(continent: String?): List<Country> =
        if (continent.isNullOrEmpty()) countries
        else countries.filter { it.continent == continent }

    fun eventsFor(cca2: String): List<HistoryEvent> =
        events.filter { cca2 in it.countries }

    companion object {
        @Volatile
        private var cached: AppData? = null
        private val mutex = Mutex()

        suspend fun get(context: Context): AppData {
            cached?.let { return it }
            return mutex.withLock {
                cached ?: withContext(Dispatchers.IO) { load(context.applicationContext) }
                    .also { cached = it }
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun load(context: Context): AppData {
            val json = Json { ignoreUnknownKeys = true }
            val countries: List<Country> = json.decodeFromString(
                context.assets.open("countries.json").bufferedReader().readText()
            )
            val map = WorldMap.parse(
                context.assets.open("world_map.json").bufferedReader().readText()
            )
            val events: List<HistoryEvent> = json.decodeFromString<List<HistoryEvent>>(
                context.assets.open("events.json").bufferedReader().readText()
            ).sortedBy { it.year }.mapIndexed { i, e -> e.copy(id = i) }
            val flagRes = countries.associate { c ->
                c.cca2 to context.resources.getIdentifier(
                    "flag_${c.cca2.lowercase()}", "drawable", context.packageName
                )
            }
            return AppData(
                countries = countries,
                byCode = countries.associateBy { it.cca2 },
                flagRes = flagRes,
                worldMap = map,
                events = events,
            )
        }
    }
}
