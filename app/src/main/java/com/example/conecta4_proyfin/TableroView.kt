package com.example.conecta4_proyfin

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.animation.ValueAnimator

class TableroView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var modoControl = 0   // 0 touch, 1 continuo, 2 gesto

    private val filas = 6
    private val columnas = 7
    private val tablero = Array(filas) { IntArray(columnas) { 0 } }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celdaSize = 0f
    private var radio = 0f

    private var turnoJugador = 1
    private var juegoTerminado = false

    // ---- ANIMACIÓN ----
    private var animFilaFinal = -1
    private var animColumnaFinal = -1
    private var animYActual = -1f
    private var animandoFicha = false

    private var sonidoFicha: MediaPlayer? = null

    // ---- SENSOR ----
    private var columnaSeleccionada = 3

    private var filtroAX = 0f
    private var filtroAY = 0f
    private val alpha = 0.15f

    private var tiempoUltMovimiento = 0L
    private val COOLDOWN_MOV = 240L

    // Sensibilidad ajustada
    private val UMBRAL_CONTINUO = 2.0f
    private val UMBRAL_GESTO = 3.2f
    private val UMBRAL_NEUTRO = 1.3f

    // Para evitar repetición en modo gesto
    private var gestoBloqueado = false

    init {
        sonidoFicha = MediaPlayer.create(context, R.raw.ficha)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        celdaSize = width.toFloat() / columnas
        radio = celdaSize * 0.4f

        canvas.drawColor(Color.parseColor("#0044AA"))

        // Ficha fantasma
        if (!juegoTerminado) {
            val cx = columnaSeleccionada * celdaSize + celdaSize / 2
            val cy = celdaSize / 2
            paint.color = if (turnoJugador == 1) Color.YELLOW else Color.RED
            canvas.drawCircle(cx, cy, radio, paint)
        }

        // Tablero
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

        // Animación
        if (animandoFicha) {
            paint.color = if (turnoJugador == 1) Color.YELLOW else Color.RED
            val cx = animColumnaFinal * celdaSize + celdaSize / 2
            canvas.drawCircle(cx, animYActual, radio, paint)
        }
    }

    // TOUCH (modo 0) y también para soltar ficha en modo 1
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (event?.action == MotionEvent.ACTION_DOWN && !juegoTerminado) {

            when (modoControl) {
                0 -> {   // TOUCH NORMAL
                    val col = (event.x / celdaSize).toInt()
                    soltarFicha(col)
                }

                1 -> {   // SENSOR CONTINUO → toca para soltar ficha fantasma
                    soltarFicha(columnaSeleccionada)
                }
            }
        }
        return true
    }

    // SENSOR
    fun procesarSensor(ax: Float, ay: Float) {

        if (juegoTerminado) return
        if (animandoFicha) return

        // INVERSIÓN DEL EJE → arregla izquierda/derecha
        val axInv = -ax

        // Filtro suave
        filtroAX += alpha * (axInv - filtroAX)
        filtroAY += alpha * (ay - filtroAY)

        val ahora = System.currentTimeMillis()

        val deltaAY = ay - filtroAY

        when (modoControl) {

            // ---------------------------- MODO SENSOR CONTINUO ----------------------------
            1 -> {
                if (ahora - tiempoUltMovimiento > COOLDOWN_MOV) {

                    if (filtroAX < -UMBRAL_CONTINUO) {
                        moverIzquierda()
                        tiempoUltMovimiento = ahora
                    }
                    if (filtroAX > UMBRAL_CONTINUO) {
                        moverDerecha()
                        tiempoUltMovimiento = ahora
                    }
                }
            }

            // ---------------------------- MODO GESTO -------------------------------------
            2 -> {

                // GESTO DE COLUMNA (una vez)
                if (!gestoBloqueado) {
                    if (filtroAX < -UMBRAL_GESTO) {
                        moverIzquierda()
                        gestoBloqueado = true
                        tiempoUltMovimiento = ahora
                    }
                    if (filtroAX > UMBRAL_GESTO) {
                        moverDerecha()
                        gestoBloqueado = true
                        tiempoUltMovimiento = ahora
                    }
                }

                if (filtroAX in -UMBRAL_NEUTRO..UMBRAL_NEUTRO) {
                    gestoBloqueado = false
                }

                // GESTO PARA SOLTAR FICHA
                if (deltaAY < -6.5f && !animandoFicha) {
                    soltarFicha(columnaSeleccionada)
                    tiempoUltMovimiento = ahora
                }
            }
        }
    }

    private fun moverIzquierda() {
        columnaSeleccionada = (columnaSeleccionada - 1).coerceAtLeast(0)
        invalidate()
    }

    private fun moverDerecha() {
        columnaSeleccionada = (columnaSeleccionada + 1).coerceAtMost(columnas - 1)
        invalidate()
    }


    // ----------------------- ANIMAR - SOLTAR -----------------------
    private fun animarFicha(fila: Int, columna: Int) {
        animandoFicha = true
        animFilaFinal = fila
        animColumnaFinal = columna

        val yInicio = celdaSize / 2
        val yFinal = fila * celdaSize + celdaSize / 2 + celdaSize
        animYActual = yInicio

        val animator = ValueAnimator.ofFloat(yInicio, yFinal)
        animator.duration = 350
        animator.addUpdateListener {
            animYActual = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    fun soltarFicha(columna: Int) {
        if (animandoFicha || columna !in 0 until columnas) return

        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {

                animarFicha(f, columna)

                postDelayed({
                    tablero[f][columna] = turnoJugador
                    sonidoFicha?.start()
                    animandoFicha = false

                    if (verificarVictoria(f, columna)) {
                        juegoTerminado = true
                        mostrarGanador(turnoJugador)
                    } else if (verificarEmpate()) {
                        juegoTerminado = true
                        mostrarEmpate()
                    } else {
                        turnoJugador = if (turnoJugador == 1) 2 else 1
                    }

                    invalidate()
                }, 350)

                break
            }
        }
    }

    // ----------------------- LÓGICA -----------------------
    private fun verificarVictoria(f: Int, c: Int): Boolean {
        val jugador = tablero[f][c]
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

        for ((df, dc) in dirs) {
            var total = 1
            total += contar(f, c, df, dc, jugador)
            total += contar(f, c, -df, -dc, jugador)
            if (total >= 4) return true
        }
        return false
    }

    private fun contar(f: Int, c: Int, df: Int, dc: Int, jugador: Int): Int {
        var x = f + df
        var y = c + dc
        var count = 0
        while (x in 0 until filas && y in 0 until columnas && tablero[x][y] == jugador) {
            count++
            x += df
            y += dc
        }
        return count
    }

    private fun verificarEmpate() = tablero[0].none { it == 0 }

    private fun mostrarGanador(jugador: Int) {
        val color = if (jugador == 1) "Rojo 🔴" else "Amarillo 🟡"
        AlertDialog.Builder(context)
            .setTitle("Ganador")
            .setMessage("El jugador $color ha ganado.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciar() }
            .show()
    }

    private fun mostrarEmpate() {
        AlertDialog.Builder(context)
            .setTitle("Empate")
            .setMessage("No quedan movimientos.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciar() }
            .show()
    }

    private fun reiniciar() {
        for (f in 0 until filas)
            for (c in 0 until columnas)
                tablero[f][c] = 0

        turnoJugador = 1
        juegoTerminado = false
        columnaSeleccionada = 3
        invalidate()
    }

    fun iniciarJuego() {}
}
