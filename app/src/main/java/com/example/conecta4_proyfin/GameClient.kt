package com.example.conecta4_proyfin

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

class GameClient(
    private val onMessageReceived: (Int, String) -> Unit, // (Columna, Estado)
    private val onConnected: (String) -> Unit, // CAMBIO: Ahora devuelve la IP
    private val onError: (String) -> Unit
) {

    private val udpPort = 4445
    private val tcpPort = 8080 // Puerto fijo del servidor
    private val scope = CoroutineScope(Dispatchers.IO)

    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null

    private var isListening = true

    // CAMBIO: start acepta IP opcional
    fun start(serverIp: String? = null) {
        isListening = true // Reiniciar flag
        if (serverIp != null) {
            // Si ya tenemos IP, conectamos directo (Reconexión rápida)
            connectTcpWithRetry(serverIp)
        } else {
            // Si no, buscamos por UDP (Primera vez)
            searchServerAndConnect()
        }
    }

    // --- PARTE A: ESCUCHA UDP ---
    private fun searchServerAndConnect() = scope.launch {
        try {
            DatagramSocket(null).use { udpSocket ->
                udpSocket.reuseAddress = true
                udpSocket.bind(InetSocketAddress(udpPort))

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isListening && socket == null) {
                    try {
                        udpSocket.receive(packet)
                        val message = String(packet.data, 0, packet.length)

                        if (message.startsWith("CONECTA4_HOST")) {
                            val parts = message.split(":")
                            if (parts.size >= 2) {
                                val serverIp = parts[1]
                                // Encontramos servidor, intentamos conectar TCP
                                connectTcpWithRetry(serverIp)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) continue
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Error buscando servidor: ${e.message}") }
        }
    }

    // --- PARTE B: CONEXIÓN TCP CON REINTENTOS ---
    private fun connectTcpWithRetry(ip: String) = scope.launch {
        var intentos = 0
        var conectado = false

        while (!conectado && intentos < 5 && isListening) {
            try {
                intentos++
                socket = Socket()
                // Timeout de conexión de 2 segundos
                socket?.connect(InetSocketAddress(ip, tcpPort), 2000)

                // Si pasa la línea anterior, es que conectó
                output = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
                input = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                conectado = true

                // Notificar éxito y pasar la IP
                withContext(Dispatchers.Main) { onConnected(ip) }

                // Empezar a escuchar jugadas
                listenForServerMoves()

            } catch (e: Exception) {
                // Si falla, esperamos 1 segundo y reintentamos
                delay(1000)
            }
        }

        if (!conectado && isListening) {
            withContext(Dispatchers.Main) { onError("No se pudo conectar al servidor tras $intentos intentos.") }
        }
    }

    // --- ESCUCHAR TURNOS DEL SERVIDOR ---
    private suspend fun listenForServerMoves() {
        try {
            while (isListening) {
                val message = input?.readLine()

                if (message != null) {
                    val parts = message.split(":")
                    if (parts.size == 2) {
                        val column = parts[0].toInt()
                        val state = parts[1]
                        withContext(Dispatchers.Main) {
                            onMessageReceived(column, state)
                        }
                    }
                } else {
                    isListening = false
                    withContext(Dispatchers.Main) { onError("El servidor se desconectó") }
                }
            }
        } catch (e: Exception) {
            if (isListening) {
                withContext(Dispatchers.Main) { onError("Error recibiendo jugada: ${e.message}") }
            }
        }
    }

    // --- ENVIAR MI TURNO ---
    fun sendMove(column: Int, gameState: String) {
        scope.launch {
            try {
                output?.println("$column:$gameState")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error enviando jugada") }
            }
        }
    }

    fun stop() {
        isListening = false
        try {
            socket?.close()
        } catch (e: Exception) { e.printStackTrace() }
        scope.cancel()
    }
}