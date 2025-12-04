package com.example.conecta4_proyfin

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer // Import necesario
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var contenedorAnimacion: FrameLayout

    // --- VARIABLES DE AUDIO ---
    private var musicaFondo: MediaPlayer? = null
    private var currentPosition = 0
    // --------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ===============================================================
        // 1. INICIALIZAR AUDIO EN LOOP
        // ===============================================================
        try {
            // Asegúrate de que el archivo se llame 'menu_loop.mp3' en res/raw
            musicaFondo = MediaPlayer.create(this, R.raw.menu_loop)
            musicaFondo?.isLooping = true // Activamos la repetición
            musicaFondo?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // ===============================================================

        contenedorAnimacion = findViewById(R.id.contenedorAnimacion)

        // Espera a que el contenedor mida su tamaño antes de crear las fichas
        contenedorAnimacion.post {
            iniciarAnimacionFichas()
        }

        // Botones
        findViewById<Button>(R.id.btn_un_jugador).setOnClickListener {
            // He actualizado esto para que coincida con tu TableroActivity
            val intent = Intent(this, TableroActivity::class.java)
            // Usamos las constantes que definimos antes:
            intent.putExtra(TableroActivity.EXTRA_MODO, TableroActivity.MODO_SOLO)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_dos_jugadores).setOnClickListener {
            // Este va a tu menú multijugador (crearPartida / unirsePartida)
            // Asumo que tu clase 'menuMulti' es donde eliges Server o Cliente
            // O si vas directo a crearPartida (como vimos antes), cámbialo aquí.
            val intent = Intent(this, crearPartida::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_temas).setOnClickListener {
            startActivity(Intent(this, TemaActivity::class.java))
        }
        findViewById<Button>(R.id.btn_instrucciones).setOnClickListener {
            val intentInstrucciones = Intent(this, activity_instrucciones2::class.java)
            startActivity(intentInstrucciones)
        }

        findViewById<Button>(R.id.btn_records).setOnClickListener {
            val intentRecords = Intent(this, activity_records2::class.java)
            startActivity(intentRecords)
        }

        findViewById<Button>(R.id.btn_salir).setOnClickListener {
            finish()
        }
    }

    private fun iniciarAnimacionFichas() {
        for (i in 1..10) {
            val ficha = View(this)
            val color = if (i % 2 == 0) Color.RED else Color.YELLOW

            // Crea un círculo con borde suave
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }

            ficha.background = drawable
            ficha.layoutParams = FrameLayout.LayoutParams(100, 100)
            contenedorAnimacion.addView(ficha)

            val startX = Random.nextInt(0, contenedorAnimacion.width.coerceAtLeast(500))
            ficha.translationX = startX.toFloat()
            ficha.translationY = -200f

            val animator = ObjectAnimator.ofFloat(
                ficha,
                "translationY",
                -200f,
                contenedorAnimacion.height + 300f
            )
            animator.duration = Random.nextLong(3000, 6000)
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.startDelay = (i * 300).toLong()
            animator.start()
        }
    }

    // ===============================================================
    // GESTIÓN DEL CICLO DE VIDA DEL AUDIO
    // ===============================================================

    override fun onResume() {
        super.onResume()
        // Si volvemos a esta pantalla, reanudamos
        musicaFondo?.let {
            if (!it.isPlaying) {
                it.seekTo(currentPosition)
                it.start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Si la app se va a segundo plano o cambias de pantalla, pausamos
        musicaFondo?.let {
            if (it.isPlaying) {
                it.pause()
                currentPosition = it.currentPosition
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos de memoria al cerrar la app totalmente
        musicaFondo?.stop()
        musicaFondo?.release()
        musicaFondo = null
    }
}