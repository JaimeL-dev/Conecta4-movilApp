package com.example.conecta4_proyfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Nombre de la clase ajustado a 'crearPartida'
class crearPartida : AppCompatActivity() {

    private lateinit var gameServer: GameServer
    private lateinit var tvIpAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el layout anterior se llama activity_crear_partida.xml
        setContentView(R.layout.activity_crear_partida)

        tvIpAddress = findViewById(R.id.tv_ip_address)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_servidor)

        // Inicializar el servidor y sus callbacks
        gameServer = GameServer(
            onMessageReceived = { _, _ -> /* Lógica para después de empezar el juego */ },
            onClientConnected = {
                // !!! ESTO SE EJECUTA CUANDO SE ENCUENTRA UN CLIENTE !!!
                handleClientConnected()
            },
            onError = { errorMessage ->
                handleServerError(errorMessage)
            }
        )

        // 1. Mostrar la IP inmediatamente
        tvIpAddress.text = "Tu IP: ${gameServer.myIpAddress}"

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
            Toast.makeText(this, "¡Oponente conectado! Iniciando partida...", Toast.LENGTH_LONG).show()

            // TODO: Cambiar TableroActivity por la clase real de tu tablero de juego
            val intent = Intent(this, TableroActivity::class.java).apply {
                putExtra("es_servidor", true)
            }
            startActivity(intent)
            finish() // Cierra esta Activity
        }
    }

    private fun handleServerError(errorMessage: String) {
        runOnUiThread {
            Toast.makeText(this, "Error de servidor: $errorMessage", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun handleCancelar() {
        gameServer.stop()
        Toast.makeText(this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de que el servidor se detenga si la Activity se destruye
        gameServer.stop()
    }
}