package com.example.conecta4_proyfin

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

class GameServer(
    private val onMessageReceived: (Int, String) -> Unit, // (Columna, Estado)
    private val onClientConnected: () -> Unit,
    private val onError: (String) -> Unit
) {

    private val tcpPort = 8080
    private val udpPort = 4445
    private val scope = CoroutineScope(Dispatchers.IO)

    // Variable de clase para poder cerrar el socket desde stop()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null

    // Obtener IP automáticamente
    val myIpAddress: String = getLocalIpAddress()

    private var isListening = true

    fun start() {
        startUdpDiscovery()
        startTcpServer()
    }

    // --- PARTE A: ANUNCIO UDP ---
    private fun startUdpDiscovery() = scope.launch {
        if (myIpAddress.isEmpty()) {
            withContext(Dispatchers.Main) { onError("Sin red Wi-Fi") }
            return@launch
        }

        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val message = "CONECTA4_HOST:$myIpAddress:$tcpPort"
                val buffer = message.toByteArray()
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, udpPort)

                while (isListening && clientSocket == null) {
                    try {
                        socket.send(packet)
                        delay(2000)
                    } catch (e: Exception) {
                        if (isActive) delay(2000)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar errores de UDP si falla el bind
        }
    }

    // --- PARTE B: SERVIDOR TCP (AQUÍ ESTÁ LA SOLUCIÓN) ---
    // --- PARTE B: SERVIDOR TCP CON REINTENTOS DE BIND ---
    private fun startTcpServer() = scope.launch {
        var bindExitoso = false
        var intentos = 0

        // Bucle para intentar ocupar el puerto si está "busy"
        while (!bindExitoso && intentos < 10 && isListening) {
            try {
                // 1. Instanciar y configurar
                val server = ServerSocket()
                server.reuseAddress = true

                // 2. Intentar atar al puerto
                server.bind(InetSocketAddress(tcpPort))

                // Si pasa aquí, es que funcionó
                bindExitoso = true
                this@GameServer.serverSocket = server

                // 3. Comenzar a escuchar
                server.use { validServerSocket ->
                    // Log para depuración
                    println("Servidor: Puerto $tcpPort abierto correctamente.")

                    clientSocket = validServerSocket.accept()

                    output = PrintWriter(OutputStreamWriter(clientSocket!!.getOutputStream()), true)
                    input = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))

                    withContext(Dispatchers.Main) { onClientConnected() }

                    listenForClientMoves()
                }

            } catch (e: Exception) {
                // Si falla (EADDRINUSE), esperamos y reintentamos
                intentos++
                println("Servidor: Puerto ocupado, reintentando ($intentos/10)...")
                try {
                    // Cerrar el objeto fallido por si acaso
                    serverSocket?.close()
                } catch (ex: Exception) { }

                delay(500) // Esperar medio segundo antes de probar otra vez
            }
        }

        // Si después de 5 segundos sigue fallando:
        if (!bindExitoso && isListening) {
            withContext(Dispatchers.Main) {
                onError("No se pudo liberar el puerto 8080. Reinicia la App.")
            }
        }
    }
    // --- ESCUCHAR TURNOS ---
    private suspend fun listenForClientMoves() {
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
                }
            }
        } catch (e: Exception) {
            if (isListening) {
                withContext(Dispatchers.Main) { onError("Desconexión: ${e.message}") }
            }
        }
    }

    fun sendMove(column: Int, gameState: String) {
        scope.launch {
            try {
                output?.println("$column:$gameState")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error enviando turno") }
            }
        }
    }

    fun stop() {
        isListening = false
        try {
            clientSocket?.close()
            // Cerramos el ServerSocket explícitamente para liberar el puerto
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scope.cancel()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isLoopback && intf.isUp) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: ""
                        }
                    }
                }
            }
        } catch (ex: Exception) { }
        return ""
    }
}