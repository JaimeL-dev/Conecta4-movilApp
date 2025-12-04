package com.example.conecta4_proyfin

import kotlinx.serialization.Serializable


@Serializable
data class Record(
    val nombre: String,
    val puntaje: Int,
    val tiempoJuego: Long // Tiempo de juego en milisegundos
)