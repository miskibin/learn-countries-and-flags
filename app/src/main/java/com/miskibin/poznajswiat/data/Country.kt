package com.miskibin.poznajswiat.data

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val cca2: String,
    val namePl: String,
    val capitalPl: String,
    val continent: String,
    val lat: Double,
    val lng: Double,
    val fact: String,
    val hasPoly: Boolean,
    val subregionPl: String = "",
    val neighbors: List<String> = emptyList(),
)

@Serializable
data class HistoryEvent(
    val id: Int = 0,
    val year: Int,
    val title: String,
    val desc: String,
    val countries: List<String> = emptyList(),
) {
    /** "44 p.n.e." / "1939" */
    val yearLabel: String
        get() = if (year < 0) "${-year} p.n.e." else "$year"
}

val CONTINENTS = listOf(
    "Europa",
    "Azja",
    "Afryka",
    "Ameryka Północna",
    "Ameryka Południowa",
    "Oceania",
)

/** A learnable category; also the key prefix under which progress is stored. */
enum class QuizMode(val id: String) {
    FLAGS("flagi"),
    MAP("mapa"),
    CAPITALS("stolice"),
    HISTORY("historia");

    companion object {
        fun fromId(id: String): QuizMode = entries.first { it.id == id }
    }
}

/** Mixed spaced-repetition session; not a storage category of its own. */
const val SESSION_REVIEW = "powtorka"

const val QUESTIONS_PER_SESSION = 10
const val REVIEW_SESSION_SIZE = 12
const val MASTERY_STREAK = 3
