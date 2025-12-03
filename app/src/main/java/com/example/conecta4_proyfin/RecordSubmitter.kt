package com.example.conecta4_proyfin

import android.content.Context
import android.util.Log

/**
 * Objeto Singleton (Clase de Servicio) para encapsular la lógica de creación
 * y envío de un nuevo Record al sistema de persistencia.
 * Esto aísla la lógica de RecordsManager de la Activity de juego.
 */
object RecordSubmitter {

    /**
     * Crea un objeto Record e inmediatamente lo guarda usando RecordsManager.
     * * @param context El contexto es necesario para que RecordsManager acceda a los archivos.
     * @param nombre Nombre del jugador.
     * @param puntaje Puntuación (movimientos) final.
     * @param tiempoJuego Tiempo de partida en milisegundos.
     */
    fun submitRecord(context: Context, nombre: String, puntaje: Int, tiempoJuego: Long) {
        if (puntaje <= 0) {
            Log.w("RecordSubmitter", "Puntaje inválido (<= 0). No se guardará el récord.")
            return
        }

        // 1. Instanciar el gestor de records (se crea cada vez que se llama, es ligero)
        val recordsManager = RecordsManager(context)

        // 2. Llamar a la función de guardado
        // Observa que aquí ya no creamos el objeto Record directamente,
        // solo pasamos los parámetros al RecordsManager, que internamente crea el objeto.
        recordsManager.agregarNuevoRecord(nombre, puntaje, tiempoJuego)

        Log.i("RecordSubmitter", "Récord para $nombre con $puntaje y $tiempoJuego ms enviado a guardar.")
    }
}