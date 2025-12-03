package com.example.conecta4_proyfin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class menuMulti : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Asumiendo que has llamado a tu nuevo layout: activity_menu_multi.xml
        setContentView(R.layout.activity_menu_multi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_multi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Llama a la función para configurar los listeners de los botones
        setupButtons()
    }

    /**
     * Configura los listeners de clic para cada botón del menú.
     */
    private fun setupButtons() {
        // 1. Botón CREAR PARTIDA
        val btnCrearPartida: Button = findViewById(R.id.btn_crear_partida)
        btnCrearPartida.setOnClickListener {
            // El Intent debe apuntar a la clase 'crearPartida'
            val intent = Intent(this, crearPartida::class.java) // <-- ¡Ajuste aquí!
            startActivity(intent)
        }

        // 2. Botón UNIRSE A PARTIDA
        val btnUnirsePartida: Button = findViewById(R.id.btn_unirse_partida)
        btnUnirsePartida.setOnClickListener {
            // El Intent debe apuntar a la clase 'unirsePartida'
            val intent = Intent(this, unirsePartida::class.java) // <-- ¡Ajuste aquí!
            startActivity(intent)
        }

        // 3. Botón VOLVER (Si lo incluiste en el layout)
        val btnVolver: Button = findViewById(R.id.btn_volver)
        btnVolver.setOnClickListener {
            // Lógica para volver a la actividad anterior
            handleVolver()
        }
    }

    private fun handleCrearPartida() {

    }

    private fun handleUnirsePartida() {

    }

    private fun handleVolver() {
        finish()
    }
}