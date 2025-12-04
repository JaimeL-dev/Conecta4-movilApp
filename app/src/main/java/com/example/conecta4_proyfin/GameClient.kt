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
    private val onMessageReceived: (Int, String) -> Unit,
    private val onConnected: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val udpPort = 4445
    private val tcpPort = 8080
    private val scope = CoroutineScope(Dispatchers.IO)

    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null
    private var isListening = true

    fun start(serverIp: String? = null) {
        isListening = true
        if (serverIp != null) connectTcpWithRetry(serverIp)
        else searchServerAndConnect()
    }

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
                                connectTcpWithRetry(parts[1])
                                break
                            }
                        }
                    } catch (e: Exception) { if (isActive) continue }
                }
            }
        } catch (e: Exception) { withContext(Dispatchers.Main) { onError("Error UDP: ${e.message}") } }
    }

    private fun connectTcpWithRetry(ip: String) = scope.launch {
        var intentos = 0
        var conectado = false
        while (!conectado && intentos < 5 && isListening) {
            try {
                intentos++
                socket = Socket()
                socket?.connect(InetSocketAddress(ip, tcpPort), 2000)
                output = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
                input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                conectado = true
                withContext(Dispatchers.Main) { onConnected(ip) }
                listenForServerMoves()
            } catch (e: Exception) { delay(1000) }
        }
        if (!conectado && isListening) withContext(Dispatchers.Main) { onError("No se pudo conectar.") }
    }

    private suspend fun listenForServerMoves() {
        try {
            while (isListening) {
                val message = input?.readLine()
                if (message != null) {
                    val parts = message.split(":")
                    if (parts.size == 2) {
                        val column = parts[0].toInt()
                        val state = parts[1]
                        withContext(Dispatchers.Main) { onMessageReceived(column, state) }
                    }
                } else {
                    isListening = false
                    withContext(Dispatchers.Main) { onError("Servidor desconectado") }
                }
            }
        } catch (e: Exception) { if (isListening) withContext(Dispatchers.Main) { onError("Error datos: ${e.message}") } }
    }

    fun sendMove(column: Int, gameState: String) {
        scope.launch { try { output?.println("$column:$gameState") } catch (e: Exception) { } }
    }

    // --- NUEVO: ENVIAR REINICIO ---
    fun sendRestart() {
        scope.launch { try { output?.println("-1:RESET") } catch (e: Exception) { } }
    }

    fun stop() {
        isListening = false
        try { socket?.close() } catch (e: Exception) { }
        scope.cancel()
    }
}