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
)

val CONTINENTS = listOf(
    "Europa",
    "Azja",
    "Afryka",
    "Ameryka Północna",
    "Ameryka Południowa",
    "Oceania",
)

enum class QuizMode(val id: String) {
    FLAGS("flagi"),
    MAP("mapa"),
    CAPITALS("stolice");

    companion object {
        fun fromId(id: String): QuizMode = entries.first { it.id == id }
    }
}

const val QUESTIONS_PER_SESSION = 10
const val MASTERY_STREAK = 3
