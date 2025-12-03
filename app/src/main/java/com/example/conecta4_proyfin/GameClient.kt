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
    private val onConnected: () -> Unit,
    private val onError: (String) -> Unit
) {

    private val udpPort = 4445 // Debe coincidir con el servidor
    private val scope = CoroutineScope(Dispatchers.IO)

    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null

    private var isListening = true

    fun start() {
        searchServerAndConnect()
    }

    // --- PARTE A: ESCUCHA UDP (Buscando al Servidor) ---
    private fun searchServerAndConnect() = scope.launch {
        try {
            // Abrimos socket UDP para escuchar en el puerto 4445
            DatagramSocket(null).use { udpSocket ->
                udpSocket.reuseAddress = true
                udpSocket.bind(InetSocketAddress(udpPort))
                // Timeout para no quedarse pegado eternamente si no hay server (opcional)
                // udpSocket.soTimeout = 15000

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isListening && socket == null) {
                    try {
                        // Bloquea hasta recibir paquete
                        udpSocket.receive(packet)

                        val message = String(packet.data, 0, packet.length)

                        // Validamos que sea NUESTRO servidor y no otro paquete random
                        if (message.startsWith("CONECTA4_HOST")) {
                            // Formato esperado: "CONECTA4_HOST:IP_SERVIDOR:PUERTO"
                            val parts = message.split(":")
                            if (parts.size == 3) {
                                val serverIp = parts[1]
                                val serverPort = parts[2].toInt()

                                // ¡Encontrado! Detenemos búsqueda y conectamos TCP
                                connectTcp(serverIp, serverPort)
                                break
                            }
                        }
                    } catch (e: Exception) {
                        // Errores de timeout o red en el discovery
                        if (isActive) continue
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Error buscando servidor: ${e.message}") }
        }
    }

    // --- PARTE B: CONEXIÓN TCP Y JUEGO ---
    private suspend fun connectTcp(ip: String, port: Int) {
        try {
            socket = Socket(ip, port)

            output = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))

            // Notificar a la UI que estamos conectados
            // NOTA: Como el servidor empieza, aquí NO debes habilitar tu tablero.
            // Solo muestra "Conectado. Esperando movimiento del anfitrión..."
            withContext(Dispatchers.Main) { onConnected() }

            // Entramos al bucle de espera de mensajes
            listenForServerMoves()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("No se pudo conectar al servidor: ${e.message}") }
        }
    }

    // --- ESCUCHAR TURNOS DEL SERVIDOR ---
    private suspend fun listenForServerMoves() {
        try {
            while (isListening) {
                val message = input?.readLine()

                if (message != null) {
                    // Protocolo: "COLUMNA:ESTADO"
                    val parts = message.split(":")
                    if (parts.size == 2) {
                        val column = parts[0].toInt()
                        val state = parts[1] // ej: "TU_TURNO", "PERDISTE"

                        withContext(Dispatchers.Main) {
                            onMessageReceived(column, state)
                        }
                    }
                } else {
                    // Server cerró conexión
                    isListening = false
                    withContext(Dispatchers.Main) { onError("El servidor se desconectó") }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Error recibiendo jugada: ${e.message}") }
        }
    }

    // --- ENVIAR MI TURNO AL SERVIDOR ---
    // Llamar a esto cuando el Cliente toca su pantalla (solo si es su turno)
    fun sendMove(column: Int, gameState: String) {
        scope.launch {
            try {
                // Formato: "4:TU_TURNO"
                output?.println("$column:$gameState")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error enviando jugada") }
            }
        }
    }

    fun stop() {
        isListening = false
        socket?.close()
        scope.cancel()
    }
}