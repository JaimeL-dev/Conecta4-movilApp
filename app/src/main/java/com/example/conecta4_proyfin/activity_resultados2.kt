package com.example.conecta4_proyfin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View // Importante para View.GONE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class activity_resultados2 : AppCompatActivity() {

    companion object {
        const val EXTRA_GANADOR = "extra_ganador"
        const val EXTRA_MOVIMIENTOS = "extra_movimientos"
        const val EXTRA_ES_MULTIJUGADOR = "extra_es_multi"
        const val EXTRA_SOY_JUGADOR_ID = "extra_soy_jugador_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados2)

        val tvResultado = findViewById<TextView>(R.id.tv_resultado_principal)
        val ivAnimacion = findViewById<ImageView>(R.id.iv_animacion_victoria)
        val tvDetalles = findViewById<TextView>(R.id.tv_detalles_partida)
        val btnJugarNuevo = findViewById<Button>(R.id.btn_jugar_de_nuevo)
        val btnVolverMenu = findViewById<Button>(R.id.btn_volver_menu)

        val ganador = intent.getIntExtra(EXTRA_GANADOR, 0)
        val movimientos = intent.getIntExtra(EXTRA_MOVIMIENTOS, 0)
        val esMultijugador = intent.getBooleanExtra(EXTRA_ES_MULTIJUGADOR, false)
        val soyJugadorID = intent.getIntExtra(EXTRA_SOY_JUGADOR_ID, 1)

        // ------------------------------------------------------------------
        // CAMBIO: OCULTAR BOTÓN EN MULTIJUGADOR
        // ------------------------------------------------------------------
        if (esMultijugador) {
            btnJugarNuevo.visibility = View.GONE
        } else {
            btnJugarNuevo.visibility = View.VISIBLE
        }
        // ------------------------------------------------------------------

        // Configuración de textos
        tvDetalles.text = "Movimientos totales: $movimientos"

        if (ganador == 0) {
            tvResultado.text = "¡EMPATE!"
            tvResultado.setTextColor(Color.GRAY)
        } else {
            val ganeYo = if (esMultijugador) (ganador == soyJugadorID) else true

            if (esMultijugador) {
                if (ganeYo) {
                    tvResultado.text = "¡HAS GANADO!"
                    tvResultado.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    tvResultado.text = "¡HAS PERDIDO!"
                    tvResultado.setTextColor(Color.parseColor("#F44336"))
                }
            } else {
                val colorJugador = if(ganador == 1) "Rojo" else "Amarillo"
                tvResultado.text = "¡Ganador: $colorJugador!"
                tvResultado.setTextColor(Color.parseColor("#FFB300"))
            }
        }

        // Botón Jugar de Nuevo (Solo visible en modo 1 jugador)
        btnJugarNuevo.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        // Botón Volver al Menú
        btnVolverMenu.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}