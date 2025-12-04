package com.example.conecta4_proyfin

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class RecordsManager(private val context: Context) {

    private val FILENAME = "high_scores.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun obtenerRecords(): List<Record> {
        val file = File(context.filesDir, FILENAME)


        if (!file.exists() || file.length() == 0L) {
            Log.d("RecordsManager", "Archivo $FILENAME no encontrado o vacío. Devolviendo lista vacía.")
            return emptyList()
        }

        try {
            val jsonString = context.openFileInput(FILENAME).bufferedReader().use { it.readText() }
            val records = json.decodeFromString<List<Record>>(jsonString)
            return records.sortedByDescending { it.puntaje }

        } catch (e: FileNotFoundException) {
            Log.e("RecordsManager", "Error: Archivo $FILENAME no encontrado.", e)
        } catch (e: IOException) {
            Log.e("RecordsManager", "Error de I/O al leer el archivo.", e)
        } catch (e: Exception) {
            Log.e("RecordsManager", "Error de serialización al decodificar JSON.", e)
        }


        return emptyList()
    }


    fun agregarNuevoRecord(nombre: String, puntaje: Int, tiempoJuego: Long) {
        val nuevoRecord = Record(nombre, puntaje, tiempoJuego)
        val recordsActuales = obtenerRecords().toMutableList()
        recordsActuales.add(nuevoRecord)
        val listaOrdenada = recordsActuales
            .sortedWith(compareByDescending<Record> { it.puntaje }.thenBy { it.tiempoJuego })
            .toMutableList()
        val topRecords = if (listaOrdenada.size > 10) listaOrdenada.take(30) else listaOrdenada
        try {
            val jsonString = json.encodeToString(topRecords)

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