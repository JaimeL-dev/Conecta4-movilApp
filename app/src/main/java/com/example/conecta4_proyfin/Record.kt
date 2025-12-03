package com.example.conecta4_proyfin

import kotlinx.serialization.Serializable

/**
 * Representa un registro de puntuación (high score).
 * La anotación @Serializable es necesaria para que Kotlinx Serialization pueda convertir
 * la clase a y desde formato JSON.
 */
@Serializable
data class Record(
    val nombre: String,
    val puntaje: Int,
    val tiempoJuego: Long // Tiempo de juego en milisegundos
)