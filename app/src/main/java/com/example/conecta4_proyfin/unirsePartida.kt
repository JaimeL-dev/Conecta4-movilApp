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
        setContentView(R.layout.activity_unirse_partida)

        tvEstadoCliente = findViewById(R.id.tv_estado_cliente)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_cliente)

        // Inicializar el cliente para ESCANEO UDP
        gameClient = GameClient(
            onMessageReceived = { _, _ -> },
            onConnected = { serverIp ->
                // ¡Conectado! Pasamos la IP a
                handleServerFoundAndConnected(serverIp)
            },
            onError = { errorMessage ->
                handleClientError(errorMessage)
            }
        )

        // Iniciar búsqueda UDP (sin IP específica)
        tvEstadoCliente.text = "Buscando partida en red local..."
        gameClient.start()

        btnCancelar.setOnClickListener {
            handleCancelar()
        }
    }

    private fun handleServerFoundAndConnected(serverIp: String) {
        runOnUiThread {
            Toast.makeText(this, "Anfitrión encontrado ($serverIp). Entrando...", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, TableroActivity::class.java).apply {
                putExtra("extra_modo", "cliente")
                putExtra("extra_server_ip", serverIp)
            }
            startActivity(intent)

            finish()
        }
    }

    private fun handleClientError(errorMessage: String) {
        runOnUiThread {
            if (!isFinishing) {
            }
        }
    }

    private fun handleCancelar() {
        gameClient.stop()
        Toast.makeText(this, "Búsqueda cancelada.", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::gameClient.isInitialized) {
            gameClient.stop()
        }
    }
}