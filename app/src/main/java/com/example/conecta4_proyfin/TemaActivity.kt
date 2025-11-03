package com.example.conecta4_proyfin

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TemaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.temas)

        val grupoTemas = findViewById<RadioGroup>(R.id.grupo_temas)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_tema)

        btnGuardar.setOnClickListener {
            val seleccion = grupoTemas.checkedRadioButtonId
            if (seleccion != -1) {
                val tema = findViewById<RadioButton>(seleccion).text.toString()
                Toast.makeText(this, "Tema seleccionado: $tema", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Selecciona un tema", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
