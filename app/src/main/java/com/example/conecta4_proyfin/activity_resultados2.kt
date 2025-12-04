package com.example.conecta4_proyfin

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
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


    private var mediaPlayer: MediaPlayer? = null

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


        if (esMultijugador) {
            btnJugarNuevo.visibility = View.GONE
        } else {
            btnJugarNuevo.visibility = View.VISIBLE
        }

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
                    reproducirSonido(R.raw.victory) // SONIDO VICTORIA
                } else {
                    tvResultado.text = "¡HAS PERDIDO!"
                    tvResultado.setTextColor(Color.parseColor("#F44336"))
                    reproducirSonido(R.raw.game_over) // SONIDO DERROTA
                }
            } else {

                if (ganador == 1) {
                    reproducirSonido(R.raw.victory) // Ganó Humano
                } else {
                    reproducirSonido(R.raw.game_over) // Ganó CPU
                }

                val colorJugador = if(ganador == 1) "Rojo" else "Amarillo"
                tvResultado.text = "¡Ganador: $colorJugador!"
                tvResultado.setTextColor(Color.parseColor("#FFB300"))
            }
        }

        btnJugarNuevo.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        btnVolverMenu.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun reproducirSonido(resId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}