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


        gameServer = GameServer(
            onMessageReceived = { _, _ ->  },
            onClientConnected = {

                handleClientConnected()
            },
            onError = { errorMessage ->
                handleServerError(errorMessage)
            }
        )


        val ip = gameServer.myIpAddress
        tvIpAddress.text = if (ip.isNotEmpty()) "Tu IP: $ip" else "No conectado a Wi-Fi"


        gameServer.start()


        btnCancelar.setOnClickListener {
            handleCancelar()
        }
    }


    private fun handleClientConnected() {
        runOnUiThread {
            Toast.makeText(this, "¡Oponente conectado! Iniciando...", Toast.LENGTH_SHORT).show()


            gameServer.stop()


            val intent = Intent(this, TableroActivity::class.java).apply {
                putExtra("extra_modo", "server")
            }
            startActivity(intent)

            finish()
        }
    }
    private fun handleServerError(errorMessage: String) {
        runOnUiThread {

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
        if (::gameServer.isInitialized) {
            gameServer.stop()
        }
    }
}