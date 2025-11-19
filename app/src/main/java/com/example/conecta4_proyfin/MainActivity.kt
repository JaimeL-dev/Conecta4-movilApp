package com.example.conecta4_proyfin


import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var contenedorAnimacion: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contenedorAnimacion = findViewById(R.id.contenedorAnimacion)

        // Espera a que el contenedor mida su tamaño antes de crear las fichas
        contenedorAnimacion.post {
            iniciarAnimacionFichas()
        }

        // Botones
        findViewById<Button>(R.id.btn_un_jugador).setOnClickListener {
            val intent = Intent(this, TableroActivity::class.java)
            intent.putExtra("modo", "un_jugador")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_dos_jugadores).setOnClickListener {
            val intent = Intent(this, TableroActivity::class.java)
            intent.putExtra("modo", "dos_jugadores")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_temas).setOnClickListener {
            startActivity(Intent(this, TemaActivity::class.java))
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
}