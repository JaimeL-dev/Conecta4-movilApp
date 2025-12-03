package com.example.conecta4_proyfin

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress // IMPORTANTE: Necesario para el bind
import java.net.ServerSocket
import java.net.Socket
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class GameServer(
    private val onMessageReceived: (Int, String) -> Unit, // (Columna, Estado)
    private val onClientConnected: () -> Unit,
    private val onError: (String) -> Unit
) {

    private val tcpPort = 8080
    private val udpPort = 4445
    private val scope = CoroutineScope(Dispatchers.IO)

    // CORRECCIÓN 1: Definir serverSocket como variable de clase para poder cerrarlo en stop()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null

    // 1. Obtener IP automáticamente
    val myIpAddress: String = getLocalIpAddress()

    // Variable para controlar el bucle de lectura
    private var isListening = true

    fun start() {
        startUdpDiscovery()
        startTcpServer()
    }

    // --- PARTE A: ANUNCIO UDP ---
    private fun startUdpDiscovery() = scope.launch {
        if (myIpAddress.isEmpty()) {
            withContext(Dispatchers.Main) { onError("No se encontró una IP válida (¿Estás conectado a WiFi?)") }
            return@launch
        }

        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val message = "CONECTA4_HOST:$myIpAddress:$tcpPort"
                val buffer = message.toByteArray()
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, udpPort)

                while (isListening && clientSocket == null) { // Dejar de anunciar si alguien se conecta
                    socket.send(packet)
                    delay(2000)
                }
            }
        } catch (e: Exception) {
            // Manejo de errores silencioso para el discovery
        }
    }

    // --- PARTE B: SERVIDOR TCP Y JUEGO ---
    private fun startTcpServer() = scope.launch {
        try {
            // CORRECCIÓN 2: Configuración para reutilizar el puerto inmediatamente
            val server = ServerSocket()
            server.reuseAddress = true // Evita el error EADDRINUSE
            server.bind(InetSocketAddress(tcpPort))

            // Guardamos la referencia en la variable de clase
            this@GameServer.serverSocket = server

            server.use { validServerSocket ->
                // Esperar al cliente
                // Usamos validServerSocket que es el que acabamos de configurar
                clientSocket = validServerSocket.accept()

                // Configurar streams
                output = PrintWriter(OutputStreamWriter(clientSocket!!.getOutputStream()), true)
                input = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))

                // Notificar a la UI que el juego empieza
                withContext(Dispatchers.Main) { onClientConnected() }

                // Iniciar bucle de escucha de mensajes del cliente
                listenForClientMoves()
            }
        } catch (e: Exception) {
            // Si paramos el servidor manualmente, lanzará una excepción de socket cerrado,
            // solo mostramos error si debíamos estar escuchando.
            if (isListening) {
                withContext(Dispatchers.Main) { onError("Error en servidor: ${e.message}") }
            }
        }
    }

    // --- ESCUCHAR TURNOS DEL CLIENTE ---
    private suspend fun listenForClientMoves() {
        try {
            while (isListening) {
                // Bloqueante hasta recibir mensaje
                val message = input?.readLine()

                if (message != null) {
                    // Protocolo esperado: "COLUMNA:ESTADO"
                    val parts = message.split(":")
                    if (parts.size == 2) {
                        val column = parts[0].toInt()
                        val state = parts[1] // ej: "TU_TURNO", "GANASTE" (si el otro pierde)

                        // Pasar a la UI en el hilo principal
                        withContext(Dispatchers.Main) {
                            onMessageReceived(column, state)
                        }
                    }
                } else {
                    // Null significa desconexión
                    isListening = false
                }
            }
        } catch (e: Exception) {
            if (isListening) {
                withContext(Dispatchers.Main) { onError("Error recibiendo datos: ${e.message}") }
            }
        }
    }

    // --- ENVIAR MI TURNO AL CLIENTE ---
    fun sendMove(column: Int, gameState: String) {
        scope.launch {
            try {
                val messageToSend = "$column:$gameState"
                output?.println(messageToSend)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error enviando turno") }
            }
        }
    }

    // CORRECCIÓN 3: Método stop robusto
    fun stop() {
        isListening = false
        try {
            // Cierra el socket del cliente si existe
            clientSocket?.close()
            // Cierra el socket del servidor (ahora sí tenemos la referencia)
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Cancela todas las corrutinas en segundo plano
        scope.cancel()
    }

    fun getLocalIpAddress(): String {
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
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }
}