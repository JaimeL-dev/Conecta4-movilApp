package com.example.conecta4_proyfin

import android.content.Context
import android.util.Log

/**
 * Objeto Singleton, esto aísla la lógica de RecordsManager de la Activity de juego.*/
object RecordSubmitter {

    fun submitRecord(context: Context, nombre: String, puntaje: Int, tiempoJuego: Long) {
        if (puntaje < 0) {
            Log.w("RecordSubmitter", "Puntaje inválido (<= 0). No se guardará el récord.")
            return
        }

        val recordsManager = RecordsManager(context)

        recordsManager.agregarNuevoRecord(nombre, puntaje, tiempoJuego)

        Log.i("RecordSubmitter", "Récord para $nombre con $puntaje y $tiempoJuego ms enviado a guardar.")
    }
}