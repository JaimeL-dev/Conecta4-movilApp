package com.example.conecta4_proyfin

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class activity_instrucciones2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_instrucciones2)

        val btnVolver: Button = findViewById(R.id.btn_volver_inicio_instrucciones)

        btnVolver.setOnClickListener {
            finish()
        }
    }
}