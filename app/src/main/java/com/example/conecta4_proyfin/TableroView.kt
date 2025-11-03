package com.example.conecta4_proyfin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TableroView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val filas = 6
    private val columnas = 7
    private val tablero = Array(filas) { IntArray(columnas) { 0 } } // 0 = vacío, 1 = rojo, 2 = amarillo

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celdaSize = 0f
    private var radio = 0f
    private var turnoJugador = 1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        celdaSize = (widthF / columnas).coerceAtMost(heightF / filas)
        radio = celdaSize * 0.4f

        // Fondo azul
        canvas.drawColor(Color.parseColor("#0044AA"))

        // Dibujar celdas
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
        if (event.action == MotionEvent.ACTION_DOWN) {
            val columna = (event.x / celdaSize).toInt()
            colocarFicha(columna)
            invalidate()
        }
        return true
    }

    private fun colocarFicha(columna: Int) {
        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {
                tablero[f][columna] = turnoJugador
                turnoJugador = if (turnoJugador == 1) 2 else 1
                break
            }
        }
    }
}
