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

        setContentView(R.layout.activity_menu_multi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_multi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupButtons()
    }

    private fun setupButtons() {
        val btnCrearPartida: Button = findViewById(R.id.btn_crear_partida)
        btnCrearPartida.setOnClickListener {
            val intent = Intent(this, crearPartida::class.java) // <-- ¡Ajuste aquí!
            startActivity(intent)
        }

        val btnUnirsePartida: Button = findViewById(R.id.btn_unirse_partida)
        btnUnirsePartida.setOnClickListener {
            val intent = Intent(this, unirsePartida::class.java) // <-- ¡Ajuste aquí!
            startActivity(intent)
        }


        val btnVolver: Button = findViewById(R.id.btn_volver)
        btnVolver.setOnClickListener {
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