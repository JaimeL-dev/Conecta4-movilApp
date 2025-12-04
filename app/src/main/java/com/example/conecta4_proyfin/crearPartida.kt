package com.example.conecta4_proyfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class crearPartida : AppCompatActivity() {

    private lateinit var gameServer: GameServer
    private lateinit var tvIpAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_partida)

        tvIpAddress = findViewById(R.id.tv_ip_address)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_servidor)

        // Inicializar el servidor y sus callbacks
        gameServer = GameServer(
            onMessageReceived = { _, _ -> /* Lógica para después de empezar el juego */ },
            onClientConnected = {
                // Cuando el cliente se conecta, cambiamos de pantalla
                handleClientConnected()
            },
            onError = { errorMessage ->
                handleServerError(errorMessage)
            }
        )

        // 1. Mostrar la IP inmediatamente
        val ip = gameServer.myIpAddress
        tvIpAddress.text = if (ip.isNotEmpty()) "Tu IP: $ip" else "No conectado a Wi-Fi"

        // 2. Iniciar el Servidor (búsqueda de cliente)
        gameServer.start()

        // 3. Configurar el botón de cancelar
        btnCancelar.setOnClickListener {
            handleCancelar()
        }
    }

    /**
     * Se llama cuando el GameServer detecta que un cliente se ha conectado.
     */
    private fun handleClientConnected() {
        runOnUiThread {
            Toast.makeText(this, "¡Oponente conectado! Iniciando...", Toast.LENGTH_SHORT).show()

            // 1. DETENER EL SERVIDOR ANTIGUO YA MISMO
            // Esto cierra el puerto 8080 en esta pantalla
            gameServer.stop()

            // 2. Iniciar la nueva pantalla
            val intent = Intent(this, TableroActivity::class.java).apply {
                putExtra("extra_modo", "server")
            }
            startActivity(intent)

            // 3. Cerrar esta pantalla
            finish()
        }
    }
    private fun handleServerError(errorMessage: String) {
        runOnUiThread {
            // Filtramos errores si ya estamos saliendo
            if (!isFinishing) {
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun handleCancelar() {
        gameServer.stop()
        Toast.makeText(this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de cerrar el puerto para evitar el error EADDRINUSE
        // al abrir TableroActivity inmediatamente después.
        if (::gameServer.isInitialized) {
            gameServer.stop()
        }
    }
}