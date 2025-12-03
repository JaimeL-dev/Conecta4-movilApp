package com.example.conecta4_proyfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class unirsePartida : AppCompatActivity() {

    private lateinit var gameClient: GameClient
    private lateinit var tvEstadoCliente: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usamos el layout de búsqueda de partida
        setContentView(R.layout.activity_unirse_partida)

        tvEstadoCliente = findViewById(R.id.tv_estado_cliente)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_cliente)

        // Inicializar el cliente y sus callbacks
        gameClient = GameClient(
            onMessageReceived = { _, _ -> /* Lógica para después de empezar el juego */ },
            onConnected = {
                // !!! ESTO SE EJECUTA CUANDO SE CONECTA AL SERVIDOR TCP !!!
                handleServerFoundAndConnected()
            },
            onError = { errorMessage ->
                handleClientError(errorMessage)
            }
        )

        // 1. Iniciar la búsqueda del servidor UDP
        tvEstadoCliente.text = "Escuchando en red local..."
        gameClient.start()

        // 2. Configurar el botón de cancelar
        btnCancelar.setOnClickListener {
            handleCancelar()
        }
    }

    /**
     * Se llama cuando el GameClient se conecta exitosamente al servidor.
     */
    private fun handleServerFoundAndConnected() {
        // Ejecutamos esto en el hilo principal (UI thread)
        runOnUiThread {
            Toast.makeText(this, "Conectado al anfitrión. Iniciando partida...", Toast.LENGTH_LONG).show()

            // TODO: Cambiar TableroActivity por la clase real de tu tablero de juego
            val intent = Intent(this, TableroActivity::class.java).apply {
                putExtra("es_servidor", false) // Importante: Le dice al tablero que somos el cliente
            }
            startActivity(intent)
            finish() // Cierra esta Activity para que el usuario no vuelva al lobby
        }
    }

    /**
     * Muestra un error si falla la conexión o la búsqueda.
     */
    private fun handleClientError(errorMessage: String) {
        runOnUiThread {
            // Actualizar la UI con el error
            tvEstadoCliente.text = "Error: $errorMessage"
            Toast.makeText(this, "Error de conexión: $errorMessage", Toast.LENGTH_LONG).show()

            // Opcional: Permitir un pequeño tiempo para que el usuario vea el error antes de volver
            // Luego, volvemos a la Activity anterior.
            // finish()
        }
    }

    /**
     * Detiene el cliente y vuelve a la pantalla anterior.
     */
    private fun handleCancelar() {
        gameClient.stop()
        Toast.makeText(this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show()
        finish() // Vuelve a la Activity anterior (menuMulti)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de que el cliente se detenga si la Activity se destruye
        gameClient.stop()
    }
}