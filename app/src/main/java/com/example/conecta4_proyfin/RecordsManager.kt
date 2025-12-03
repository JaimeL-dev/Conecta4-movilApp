package com.example.conecta4_proyfin

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Clase para manejar la lectura y escritura de récords de juego usando Kotlinx Serialization.
 * Los datos se almacenan en un archivo JSON en el almacenamiento interno del dispositivo.
 *
 * @param context Contexto de la aplicación o actividad, necesario para las operaciones de archivo.
 */
class RecordsManager(private val context: Context) {

    // Constante para el nombre del archivo donde se guardarán los records
    private val FILENAME = "high_scores.json"

    // Configuración del objeto JSON para la serialización.
    // Ignorar claves desconocidas ayuda a prevenir errores si se modifica la clase Record en el futuro.
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Lee el archivo JSON del almacenamiento interno y devuelve la lista de récords.
     * Si el archivo no existe o hay un error de serialización, devuelve una lista vacía.
     *
     * @return Una lista de objetos Record, ordenada por puntaje descendente.
     */
    fun obtenerRecords(): List<Record> {
        val file = File(context.filesDir, FILENAME)

        // 1. Verificar si el archivo existe
        if (!file.exists() || file.length() == 0L) {
            Log.d("RecordsManager", "Archivo $FILENAME no encontrado o vacío. Devolviendo lista vacía.")
            return emptyList()
        }

        try {
            // 2. Leer el contenido del archivo como una cadena de texto
            val jsonString = context.openFileInput(FILENAME).bufferedReader().use { it.readText() }

            // 3. Deserializar la cadena JSON a una lista de Records
            val records = json.decodeFromString<List<Record>>(jsonString)

            // 4. Ordenar los récords por puntaje (descendente)
            return records.sortedByDescending { it.puntaje }

        } catch (e: FileNotFoundException) {
            // Manejar caso donde el archivo no se encuentra (aunque ya lo verificamos, es buena práctica)
            Log.e("RecordsManager", "Error: Archivo $FILENAME no encontrado.", e)
        } catch (e: IOException) {
            // Manejar errores de lectura/escritura (IO)
            Log.e("RecordsManager", "Error de I/O al leer el archivo.", e)
        } catch (e: Exception) {
            // Manejar errores de serialización (JSON mal formado, etc.)
            Log.e("RecordsManager", "Error de serialización al decodificar JSON.", e)
        }

        // Si ocurre algún error, devolver una lista vacía
        return emptyList()
    }

    /**
     * Agrega un nuevo registro a la lista, lo ordena y guarda la lista actualizada
     * en el archivo JSON, sobrescribiendo el contenido anterior.
     *
     * @param nombre Nombre del jugador.
     * @param puntaje Puntuación obtenida.
     * @param tiempoJuego Duración del juego en milisegundos.
     */
    fun agregarNuevoRecord(nombre: String, puntaje: Int, tiempoJuego: Long) {
        // 1. Crear el nuevo objeto Record
        val nuevoRecord = Record(nombre, puntaje, tiempoJuego)

        // 2. Obtener la lista actual de récords
        val recordsActuales = obtenerRecords().toMutableList()

        // 3. Añadir el nuevo registro
        recordsActuales.add(nuevoRecord)

        // 4. Ordenar la lista: primero por puntaje (descendente), luego por tiempo (ascendente)
        val listaOrdenada = recordsActuales
            .sortedWith(compareByDescending<Record> { it.puntaje }.thenBy { it.tiempoJuego })
            .toMutableList()

        // 5. Opcional: Limitar el número de récords a guardar (ej. el Top 10)
        val topRecords = if (listaOrdenada.size > 10) listaOrdenada.take(10) else listaOrdenada

        try {
            // 6. Serializar la lista actualizada a una cadena JSON
            val jsonString = json.encodeToString(topRecords)

            // 7. Escribir la cadena JSON al archivo. MODE_PRIVATE sobrescribe el contenido.
            context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }

            Log.d("RecordsManager", "Nuevo récord agregado y lista guardada exitosamente.")

        } catch (e: IOException) {
            Log.e("RecordsManager", "Error de I/O al escribir en el archivo.", e)
        } catch (e: Exception) {
            Log.e("RecordsManager", "Error de serialización al codificar JSON.", e)
        }
    }
}