package com.example.conecta4_proyfin

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.animation.ValueAnimator
import androidx.core.animation.addListener
import kotlin.math.abs

class TableroView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // ------------------------------------------------------------
    // MODOS DE CONTROL
    // ------------------------------------------------------------
    var modoControl = 0 // 0=touch, 1=sensor, 2=gesto

    // ------------------------------------------------------------
    // TABLERO
    // ------------------------------------------------------------
    private val filas = 6
    private val columnas = 7
    private val tablero = Array(filas) { IntArray(columnas) { 0 } }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celdaSize = 0f
    private var radio = 0f

    private var turnoJugador = 1
    private var juegoTerminado = false

    // ------------------------------------------------------------
    // ANIMACIÓN
    // ------------------------------------------------------------
    private var animFilaFinal = -1
    private var animColumnaFinal = -1
    private var animYActual = -1f
    private var animandoFicha = false
    private val DURACION_ANIM = 350L

    private var sonidoFicha: MediaPlayer? = null

    // ------------------------------------------------------------
    // SENSORES
    // ------------------------------------------------------------
    private var columnaSeleccionada = 3
    private var filtroAX = 0f
    private var filtroAY = 0f
    private val alpha = 0.15f
    private var tiempoUltMovimiento = 0L
    private var tiempoUltCaida = 0L
    private val COOLDOWN_MOV = 200L
    private val COOLDOWN_CAIDA = 500L
    private val UMBRAL_CONTINUO = 1.6f
    private val UMBRAL_GESTO = 3.0f
    private val UMBRAL_NEUTRO = 1.2f
    private var gestoBloqueado = false

    // ------------------------------------------------------------
    // TEMAS E IMÁGENES
    // ------------------------------------------------------------
    private val prefs: SharedPreferences =
        context.getSharedPreferences("config", Context.MODE_PRIVATE)

    private var temaName: String = "Clasico"

    private var imagenRojaSrc: Bitmap? = null
    private var imagenNegraSrc: Bitmap? = null
    private var imagenRojaScaled: Bitmap? = null
    private var imagenNegraScaled: Bitmap? = null

    init {
        sonidoFicha = try {
            MediaPlayer.create(context, R.raw.ficha)
        } catch (e: Exception) {
            null
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null)

        temaName = prefs.getString("tema", "Clasico") ?: "Clasico"

        cargarImagenes()
    }

    private fun cargarImagenes() {
        try {
            imagenRojaSrc = BitmapFactory.decodeResource(resources, R.drawable.ficha_roja)
            imagenNegraSrc = BitmapFactory.decodeResource(resources, R.drawable.ficha_negra)
        } catch (e: Exception) {
            imagenRojaSrc = null
            imagenNegraSrc = null
        }
        escalarImagenes()
    }

    fun setTema(tema: String) {
        temaName = tema
        prefs.edit().putString("tema", tema).apply()
        cargarImagenes()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        celdaSize = width.toFloat() / columnas
        radio = celdaSize * 0.40f
        escalarImagenes()
    }

    private fun escalarImagenes() {
        if (celdaSize <= 0f) return
        val size = (radio * 2).toInt().coerceAtLeast(1)

        imagenRojaScaled =
            imagenRojaSrc?.let { Bitmap.createScaledBitmap(it, size, size, true) }
        imagenNegraScaled =
            imagenNegraSrc?.let { Bitmap.createScaledBitmap(it, size, size, true) }
    }

    // ------------------------------------------------------------
    // DRAW
    // ------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        celdaSize = width.toFloat() / columnas
        radio = celdaSize * 0.40f

        canvas.drawColor(Color.parseColor("#0044AA"))

        // FICHA FANTASMA
        if (!juegoTerminado) {
            val cx = columnaSeleccionada * celdaSize + celdaSize / 2
            val cy = celdaSize / 2

            if (temaName == "Clasico") {
                dibujarFichaCanvas(canvas, cx, cy, turnoJugador, ghost = true)
            } else {
                val bmp =
                    if (turnoJugador == 1) imagenRojaScaled else imagenNegraScaled
                if (bmp != null) {
                    canvas.drawBitmap(bmp, cx - bmp.width / 2, cy - bmp.height / 2, null)
                } else {
                    dibujarFichaCanvas(canvas, cx, cy, turnoJugador, ghost = true)
                }
            }
        }

        // TABLERO
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                val cx = c * celdaSize + celdaSize / 2
                val cy = f * celdaSize + celdaSize / 2 + celdaSize
                val jugador = tablero[f][c]

                if (temaName == "Clasico") {
                    dibujarFichaCanvas(canvas, cx, cy, jugador, ghost = false)
                } else {
                    if (jugador == 0) {
                        dibujarFichaCanvas(canvas, cx, cy, 0, false)
                    } else {
                        val bmp = if (jugador == 1) imagenRojaScaled else imagenNegraScaled
                        if (bmp != null) {
                            canvas.drawBitmap(bmp, cx - bmp.width / 2, cy - bmp.height / 2, null)
                        } else {
                            dibujarFichaCanvas(canvas, cx, cy, jugador, ghost = false)
                        }
                    }
                }
            }
        }

        // ANIMACIÓN
        if (animandoFicha) {
            val cx = animColumnaFinal * celdaSize + celdaSize / 2
            val cy = animYActual

            val bmp =
                if (turnoJugador == 1) imagenRojaScaled else imagenNegraScaled

            if (temaName == "Clasico" || bmp == null) {
                dibujarFichaCanvas(canvas, cx, cy, turnoJugador, ghost = false)
            } else {
                canvas.drawBitmap(bmp, cx - bmp.width / 2, cy - bmp.height / 2, null)
            }
        }
    }

    private fun dibujarFichaCanvas(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        jugador: Int,
        ghost: Boolean
    ) {
        val baseColor = when (jugador) {
            1 -> Color.RED
            2 -> Color.YELLOW
            else -> Color.LTGRAY
        }

        paint.setShadowLayer(8f, 0f, 4f, Color.argb(120, 0, 0, 0))

        paint.shader = RadialGradient(
            cx, cy, radio,
            Color.WHITE,
            baseColor,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, radio, paint)

        paint.shader = null
        paint.setShadowLayer(0f, 0f, 0f, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (radio * 0.12f).coerceAtLeast(3f)
        paint.color = Color.BLACK
        canvas.drawCircle(cx, cy, radio, paint)

        paint.style = Paint.Style.FILL
    }

    // ------------------------------------------------------------
    // TOQUE
    // ------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && !juegoTerminado) {
            when (modoControl) {
                0 -> soltarFicha((event.x / celdaSize).toInt())
                1 -> soltarFicha(columnaSeleccionada)
            }
        }
        return true
    }

    // ------------------------------------------------------------
    // SENSORES
    // ------------------------------------------------------------
    fun procesarSensor(ax: Float, ay: Float) {
        if (juegoTerminado || animandoFicha) return

        val axCorr = -ax
        filtroAX += alpha * (axCorr - filtroAX)
        filtroAY += alpha * (ay - filtroAY)

        val ahora = System.currentTimeMillis()
        val deltaAY = ay - filtroAY

        when (modoControl) {
            1 -> {
                if (ahora - tiempoUltMovimiento > COOLDOWN_MOV) {
                    if (filtroAX < -UMBRAL_CONTINUO) moverIzquierda()
                    else if (filtroAX > UMBRAL_CONTINUO) moverDerecha()
                    tiempoUltMovimiento = ahora
                }
            }

            2 -> {
                if (!gestoBloqueado) {
                    if (filtroAX < -UMBRAL_GESTO) {
                        moverIzquierda()
                        gestoBloqueado = true
                    } else if (filtroAX > UMBRAL_GESTO) {
                        moverDerecha()
                        gestoBloqueado = true
                    }
                }

                if (filtroAX in -UMBRAL_NEUTRO..UMBRAL_NEUTRO) gestoBloqueado = false

                if (deltaAY < -6.5f && ahora - tiempoUltCaida > COOLDOWN_CAIDA) {
                    soltarFicha(columnaSeleccionada)
                    tiempoUltCaida = ahora
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

    // ------------------------------------------------------------
    // ANIMACIÓN + LÓGICA
    // ------------------------------------------------------------
    private fun animarFicha(f: Int, c: Int, onEnd: () -> Unit) {
        animandoFicha = true
        animFilaFinal = f
        animColumnaFinal = c

        val yIni = celdaSize / 2f
        val yFin = f * celdaSize + celdaSize / 2f + celdaSize
        animYActual = yIni

        val animator = ValueAnimator.ofFloat(yIni, yFin)
        animator.duration = DURACION_ANIM

        animator.addUpdateListener {
            animYActual = it.animatedValue as Float
            invalidate()
        }

        animator.addListener(onEnd = {
            onEnd()
        })

        animator.start()
    }

    fun soltarFicha(columna: Int) {
        if (animandoFicha || columna !in 0 until columnas) return

        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {

                animarFicha(f, columna) {
                    tablero[f][columna] = turnoJugador
                    sonidoFicha?.start()
                    animandoFicha = false

                    when {
                        verificarVictoria(f, columna) -> {
                            juegoTerminado = true
                            mostrarGanador(turnoJugador)
                        }
                        verificarEmpate() -> {
                            juegoTerminado = true
                            mostrarEmpate()
                        }
                        else -> {
                            turnoJugador = if (turnoJugador == 1) 2 else 1
                        }
                    }

                    invalidate()
                }

                break
            }
        }
    }

    private fun verificarVictoria(f: Int, c: Int): Boolean {
        val jugador = tablero[f][c]
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

        for ((df, dc) in dirs) {
            var cont = 1
            cont += contar(f, c, df, dc, jugador)
            cont += contar(f, c, -df, -dc, jugador)
            if (cont >= 4) return true
        }
        return false
    }

    private fun contar(f: Int, c: Int, df: Int, dc: Int, jugador: Int): Int {
        var ff = f + df
        var cc = c + dc
        var total = 0
        while (ff in 0 until filas && cc in 0 until columnas && tablero[ff][cc] == jugador) {
            total++
            ff += df
            cc += dc
        }
        return total
    }

    private fun verificarEmpate(): Boolean =
        tablero[0].none { it == 0 }

    private fun mostrarGanador(jugador: Int) {
        val color = if (jugador == 1) "Rojo 🔴" else "Negro ⚫"

        AlertDialog.Builder(context)
            .setTitle("¡Ganador!")
            .setMessage("El jugador $color ha ganado.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciarJuego() }
            .show()
    }

    private fun mostrarEmpate() {
        AlertDialog.Builder(context)
            .setTitle("¡Empate!")
            .setMessage("No hay más movimientos.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciarJuego() }
            .show()
    }

    private fun reiniciarJuego() {
        for (f in 0 until filas)
            for (c in 0 until columnas)
                tablero[f][c] = 0

        turnoJugador = 1
        juegoTerminado = false
        animandoFicha = false
        columnaSeleccionada = 3

        invalidate()
    }

    fun iniciarJuego() {}
}
