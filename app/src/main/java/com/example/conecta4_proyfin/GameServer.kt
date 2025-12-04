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
    private val onMessageReceived: (Int, String) -> Unit,
    private val onClientConnected: () -> Unit,
    private val onError: (String) -> Unit
) {

    private val tcpPort = 8080
    private val udpPort = 4445
    private val scope = CoroutineScope(Dispatchers.IO)

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var output: PrintWriter? = null
    private var input: BufferedReader? = null

    val myIpAddress: String = getLocalIpAddress()
    private var isListening = true

    fun start() {
        startUdpDiscovery()
        startTcpServer()
    }

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
                    try { socket.send(packet); delay(2000) } catch (e: Exception) { if (isActive) delay(2000) }
                }
            }
        } catch (e: Exception) { }
    }

    private fun startTcpServer() = scope.launch {
        var bindExitoso = false
        var intentos = 0

        while (!bindExitoso && intentos < 10 && isListening) {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(tcpPort))
                bindExitoso = true
                this@GameServer.serverSocket = server

                server.use { validServerSocket ->
                    clientSocket = validServerSocket.accept()
                    output = PrintWriter(OutputStreamWriter(clientSocket!!.getOutputStream()), true)
                    input = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                    withContext(Dispatchers.Main) { onClientConnected() }
                    listenForClientMoves()
                }
            } catch (e: Exception) {
                intentos++
                try { serverSocket?.close() } catch (ex: Exception) { }
                delay(500)
            }
        }
        if (!bindExitoso && isListening) {
            withContext(Dispatchers.Main) { onError("No se pudo liberar el puerto 8080. Reinicia la App.") }
        }
    }

    private suspend fun listenForClientMoves() {
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
                }
            }
        } catch (e: Exception) {
            if (isListening) withContext(Dispatchers.Main) { onError("Desconexión: ${e.message}") }
        }
    }

    fun sendMove(column: Int, gameState: String) {
        scope.launch {
            try { output?.println("$column:$gameState") } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error enviando turno") }
            }
        }
    }

    // --- NUEVO: ENVIAR REINICIO ---
    fun sendRestart() {
        scope.launch {
            try { output?.println("-1:RESET") } catch (e: Exception) { }
        }
    }

    fun stop() {
        isListening = false
        try { clientSocket?.close(); serverSocket?.close() } catch (e: Exception) { }
        scope.cancel()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isLoopback && intf.isUp) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (ex: Exception) { }
        return ""
    }
}