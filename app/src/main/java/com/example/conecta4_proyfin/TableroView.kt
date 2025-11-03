package com.example.proyecto_vistas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.example.conecta4_proyfin.R

class TableroActivity(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val filas = 6
    private val columnas = 7
    private val tablero = Array(filas) { IntArray(columnas) { 0 } } // 0 = vacío, 1 = rojo, 2 = amarillo

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celdaSize = 0f
    private var radio = 0f
    private var turnoJugador = 1
    private var juegoTerminado = false

    // --- SONIDO ---
    private var soundPool: SoundPool
    private var sonidoFicha = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        // Archivo en res/raw/ficha_caer.mp3
        sonidoFicha = soundPool.load(context, R.raw.caida, 1)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        celdaSize = (widthF / columnas).coerceAtMost(heightF / filas)
        radio = celdaSize * 0.4f

        // Fondo del tablero
        canvas.drawColor(Color.parseColor("#0044AA"))

        // Dibujar las celdas
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                val cx = c * celdaSize + celdaSize / 2
                val cy = f * celdaSize + celdaSize / 2 + celdaSize

                paint.color = when (tablero[f][c]) {
                    1 -> Color.RED
                    2 -> Color.YELLOW
                    else -> Color.LTGRAY
                }

                canvas.drawCircle(cx, cy, radio, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (juegoTerminado) return true

        if (event.action == MotionEvent.ACTION_DOWN) {
            val columna = (event.x / celdaSize).toInt()
            if (columna in 0 until columnas) {
                if (colocarFicha(columna)) {
                    soundPool.play(sonidoFicha, 1f, 1f, 0, 0, 1f)
                    invalidate()
                }
            }
        }
        return true
    }

    private fun colocarFicha(columna: Int): Boolean {
        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {
                tablero[f][columna] = turnoJugador

                if (verificarVictoria(f, columna)) {
                    juegoTerminado = true
                    Toast.makeText(
                        context,
                        "¡Jugador $turnoJugador ha ganado!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    turnoJugador = if (turnoJugador == 1) 2 else 1
                }

                return true
            }
        }
        return false
    }

    // --- COMPROBAR CONDICIONES DE VICTORIA ---
    private fun verificarVictoria(fila: Int, columna: Int): Boolean {
        val jugador = tablero[fila][columna]

        // Horizontal
        var contador = 0
        for (c in 0 until columnas) {
            contador = if (tablero[fila][c] == jugador) contador + 1 else 0
            if (contador >= 4) return true
        }

        // Vertical
        contador = 0
        for (f in 0 until filas) {
            contador = if (tablero[f][columna] == jugador) contador + 1 else 0
            if (contador >= 4) return true
        }

        // Diagonal ↘
        contador = 0
        var f = fila
        var c = columna
        while (f > 0 && c > 0) { f--; c-- }
        while (f < filas && c < columnas) {
            contador = if (tablero[f][c] == jugador) contador + 1 else 0
            if (contador >= 4) return true
            f++; c++
        }

        // Diagonal ↙
        contador = 0
        f = fila
        c = columna
        while (f < filas - 1 && c > 0) { f++; c-- }
        while (f >= 0 && c < columnas) {
            contador = if (tablero[f][c] == jugador) contador + 1 else 0
            if (contador >= 4) return true
            f--; c++
        }

        return false
    }

    fun reiniciarJuego() {
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                tablero[f][c] = 0
            }
        }
        turnoJugador = 1
        juegoTerminado = false
        invalidate()
    }
}
