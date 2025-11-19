package com.example.conecta4_proyfin

import android.app.AlertDialog
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
    private var juegoTerminado = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        celdaSize = (widthF / columnas).coerceAtMost(heightF / filas)
        radio = celdaSize * 0.4f

        // Fondo azul del tablero
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
        if (event.action == MotionEvent.ACTION_DOWN && !juegoTerminado) {
            val columna = (event.x / celdaSize).toInt()
            colocarFicha(columna)
            invalidate()
        }
        return true
    }

    private fun colocarFicha(columna: Int) {
        if (columna < 0 || columna >= columnas) return

        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {
                tablero[f][columna] = turnoJugador

                if (verificarVictoria(f, columna)) {
                    juegoTerminado = true
                    mostrarGanador(turnoJugador)
                } else if (verificarEmpate()) {
                    juegoTerminado = true
                    mostrarEmpate()
                } else {
                    turnoJugador = if (turnoJugador == 1) 2 else 1
                }
                break
            }
        }
    }

    private fun verificarVictoria(fila: Int, columna: Int): Boolean {
        val jugador = tablero[fila][columna]
        if (jugador == 0) return false

        val direcciones = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal ↘
            Pair(1, -1)   // Diagonal ↙
        )

        for ((df, dc) in direcciones) {
            var contador = 1
            contador += contarFichas(fila, columna, df, dc, jugador)
            contador += contarFichas(fila, columna, -df, -dc, jugador)

            if (contador >= 4) return true
        }

        return false
    }

    private fun contarFichas(fila: Int, columna: Int, df: Int, dc: Int, jugador: Int): Int {
        var count = 0
        var f = fila + df
        var c = columna + dc

        while (f in 0 until filas && c in 0 until columnas && tablero[f][c] == jugador) {
            count++
            f += df
            c += dc
        }

        return count
    }

    private fun verificarEmpate(): Boolean {
        // Si no hay espacios vacíos (0) en la fila superior, significa que el tablero está lleno
        for (c in 0 until columnas) {
            if (tablero[0][c] == 0) return false
        }
        return true
    }

    private fun mostrarGanador(jugador: Int) {
        val color = if (jugador == 1) "Rojo 🔴" else "Amarillo 🟡"

        AlertDialog.Builder(context)
            .setTitle("¡Ganador!")
            .setMessage("El jugador $color ha ganado la partida.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ ->
                reiniciarJuego()
            }
            .show()
    }

    private fun mostrarEmpate() {
        AlertDialog.Builder(context)
            .setTitle("¡Empate!")
            .setMessage("No quedan más movimientos disponibles.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ ->
                reiniciarJuego()
            }
            .show()
    }

    private fun reiniciarJuego() {
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
