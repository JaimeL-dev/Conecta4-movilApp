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

class TableroView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Callback
    var onFichaColocada: ((Int) -> Unit)? = null

    // Modos
    var modoControl = 0

    // Tablero
    private val filas = 6
    private val columnas = 7
    private val tablero = Array(filas) { IntArray(columnas) { 0 } }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var celdaSize = 0f
    private var radio = 0f

    // Estado del Juego
    private var turnoJugador = 1
    private var juegoTerminado = false

    // MULTIJUGADOR (NUEVO)
    private var esMultijugador = false
    private var jugadorLocalID = 1 // 1 por defecto (Solo o Server)

    // Animación
    private var animFilaFinal = -1
    private var animColumnaFinal = -1
    private var animYActual = -1f
    private var animandoFicha = false
    private val DURACION_ANIM = 350L
    private var sonidoFicha: MediaPlayer? = null

    // Sensores
    private var columnaSeleccionada = 3
    private var filtroAX = 0f
    private val alpha = 0.15f
    private var tiempoUltMovimiento = 0L
    private var tiempoUltCaida = 0L
    private val COOLDOWN_MOV = 200L
    private val COOLDOWN_CAIDA = 450L
    private val UMBRAL_CONTINUO = 1.6f
    private val UMBRAL_GESTO = 3.0f
    private val UMBRAL_NEUTRO = 1.2f
    private var gestoBloqueado = false

    // Recursos
    private val prefs: SharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    private var temaName: String = "Clasico"
    private var imagenRojaSrc: Bitmap? = null
    private var imagenNegraSrc: Bitmap? = null
    private var imagenRojaScaled: Bitmap? = null
    private var imagenNegraScaled: Bitmap? = null

    // Stats
    private var movimientos1 = 0
    private var movimientos2 = 0
    private var inicioPartidaMs = 0L
    private var duracionPartidaMs = 0L

    init {
        sonidoFicha = try { MediaPlayer.create(context, R.raw.ficha) } catch (e: Exception) { null }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        temaName = prefs.getString("tema", "Clasico") ?: "Clasico"
        cargarImagenes()
        iniciarJuego()
    }

    // ------------------------------------------------------------
    // CONFIGURACIÓN (NUEVO MÉTODO IMPORTANTE)
    // ------------------------------------------------------------
    fun configurarJugador(jugadorID: Int, esMultijugador: Boolean) {
        this.jugadorLocalID = jugadorID
        this.esMultijugador = esMultijugador
        reiniciarJuego() // Reiniciamos para aplicar estado limpio
    }

    private fun cargarImagenes() {
        imagenRojaSrc = BitmapFactory.decodeResource(resources, R.drawable.ficha_roja)
        imagenNegraSrc = BitmapFactory.decodeResource(resources, R.drawable.ficha_negra)
        escalarImagenes()
    }

    private fun escalarImagenes() {
        if (celdaSize <= 0f) return
        val size = (radio * 2).toInt().coerceAtLeast(1)
        imagenRojaScaled = imagenRojaSrc?.let { Bitmap.createScaledBitmap(it, size, size, true) }
        imagenNegraScaled = imagenNegraSrc?.let { Bitmap.createScaledBitmap(it, size, size, true) }
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0044AA"))

        // FICHA FANTASMA (HOVER)
        // Solo dibujamos si el juego está activo y la vista habilitada (es mi turno)
        if (!juegoTerminado && isEnabled) {
            val cx = columnaSeleccionada * celdaSize + celdaSize / 2
            val cy = celdaSize / 2

            // DETERMINAR COLOR DE LA FICHA FANTASMA
            // En multijugador, siempre muestro MI color (jugadorLocalID).
            // En local, muestro el color del turno actual.
            val fichaA_Dibujar = if (esMultijugador) jugadorLocalID else turnoJugador

            if (temaName == "Clasico") {
                dibujarFichaCanvas(canvas, cx, cy, fichaA_Dibujar, ghost = true)
            } else {
                if(temaName =="Minimalista"){
                    paint.shader = null
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.color = Color.BLACK
                    dibujarFichaCanvas(canvas, cx, cy, fichaA_Dibujar, ghost = false)
                } else {
                    val img = if (fichaA_Dibujar == 1) imagenRojaScaled else imagenNegraScaled
                    if (img != null) {
                        canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
                    } else {
                        dibujarFichaCanvas(canvas, cx, cy, fichaA_Dibujar, ghost = true)
                    }
                }
            }
        }

        // DIBUJAR TABLERO (Fichas ya colocadas)
        for (f in 0 until filas) {
            for (c in 0 until columnas) {
                val cx = c * celdaSize + celdaSize / 2
                val cy = f * celdaSize + celdaSize / 2 + celdaSize
                val jugador = tablero[f][c]

                if (temaName == "Clasico") {
                    dibujarFichaCanvas(canvas, cx, cy, jugador, ghost = false)
                } else {
                    if(temaName =="Minimalista"){
                        paint.shader = null
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 6f
                        paint.color = Color.BLACK
                        dibujarFichaCanvas(canvas, cx, cy, jugador, ghost = false)
                    } else {
                        val img = when (jugador) {
                            1 -> imagenRojaScaled
                            2 -> imagenNegraScaled
                            else -> null
                        }
                        if (img != null)
                            canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
                        else
                            dibujarFichaCanvas(canvas, cx, cy, jugador, false)
                    }
                }
            }
        }

        // FICHA ANIMADA (CAYENDO)
        if (animandoFicha) {
            val cx = animColumnaFinal * celdaSize + celdaSize / 2
            val cy = animYActual
            // La ficha animada es siempre la del turno actual
            val img = if (turnoJugador == 1) imagenRojaScaled else imagenNegraScaled

            if (temaName == "Clasico" || img == null) {
                dibujarFichaCanvas(canvas, cx, cy, turnoJugador, false)
            } else {
                if(temaName == "Minimalista"){
                    paint.shader = null
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.color = Color.BLACK
                    dibujarFichaCanvas(canvas, cx, cy, turnoJugador, ghost = false)
                } else {
                    canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
                }
            }
        }
    }

    private fun dibujarFichaCanvas(canvas: Canvas, cx: Float, cy: Float, jugador: Int, ghost: Boolean) {
        val colorBase = when (jugador) {
            1 -> Color.RED
            2 -> Color.YELLOW
            else -> Color.LTGRAY
        }
        paint.setShadowLayer(8f, 0f, 4f, Color.argb(120, 0, 0, 0))
        paint.shader = RadialGradient(cx, cy, radio, Color.WHITE, colorBase, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radio, paint)
        paint.shader = null
        paint.setShadowLayer(1f, 0f, 0f, Color.WHITE)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) return false

        if (event?.action == MotionEvent.ACTION_DOWN && !juegoTerminado) {
            // Seguridad: En multijugador, si no es mi turno, no hago nada (aunque isEnabled debería bloquearlo)
            if (esMultijugador && turnoJugador != jugadorLocalID) return false

            when (modoControl) {
                0 -> soltarFicha((event.x / celdaSize).toInt(), esRemoto = false)
                1 -> soltarFicha(columnaSeleccionada, esRemoto = false)
            }
        }
        return true
    }

    fun procesarSensor(ax: Float, ay: Float) {
        if (!isEnabled || juegoTerminado || animandoFicha) return
        // Seguridad Extra
        if (esMultijugador && turnoJugador != jugadorLocalID) return

        val axCorr = -ax
        filtroAX += alpha * (axCorr - filtroAX)
        val ahora = System.currentTimeMillis()

        when (modoControl) {
            1 -> {
                if (ahora - tiempoUltMovimiento > COOLDOWN_MOV) {
                    when {
                        filtroAX < -UMBRAL_CONTINUO -> moverIzquierda()
                        filtroAX > UMBRAL_CONTINUO -> moverDerecha()
                    }
                    tiempoUltMovimiento = ahora
                }
            }
            2 -> {
                if (!gestoBloqueado) {
                    when {
                        filtroAX < -UMBRAL_GESTO -> { moverIzquierda(); gestoBloqueado = true }
                        filtroAX > UMBRAL_GESTO -> { moverDerecha(); gestoBloqueado = true }
                    }
                }
                if (filtroAX in -UMBRAL_NEUTRO..UMBRAL_NEUTRO) gestoBloqueado = false

                if (ay < -6f && ahora - tiempoUltCaida > COOLDOWN_CAIDA) {
                    soltarFicha(columnaSeleccionada, esRemoto = false)
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

    fun jugarFichaRemota(columna: Int) {
        post {
            soltarFicha(columna, esRemoto = true)
        }
    }

    private fun animarFicha(f: Int, c: Int, onEnd: () -> Unit) {
        animandoFicha = true
        animFilaFinal = f
        animColumnaFinal = c
        val yIni = celdaSize / 2f
        val yFin = f * celdaSize + celdaSize / 2f + celdaSize
        animYActual = yIni
        val anim = ValueAnimator.ofFloat(yIni, yFin)
        anim.duration = DURACION_ANIM
        anim.addUpdateListener {
            animYActual = it.animatedValue as Float
            invalidate()
        }
        anim.addListener(onEnd = { onEnd() })
        anim.start()
    }

    fun soltarFicha(columna: Int, esRemoto: Boolean) {
        if (animandoFicha || columna !in 0 until columnas) return

        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {

                if (!esRemoto) {
                    onFichaColocada?.invoke(columna)
                }

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
                            // Cambio de turno 1 <-> 2
                            if (turnoJugador == 1) {
                                movimientos1++
                                turnoJugador = 2
                            } else {
                                movimientos2++
                                turnoJugador = 1
                            }
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

    private fun verificarEmpate() = tablero[0].all { it != 0 }

    private fun mostrarGanador(jugador: Int) {
        duracionPartidaMs = System.currentTimeMillis() - inicioPartidaMs
        // Personalizamos el mensaje
        val msg = if (esMultijugador) {
            if (jugador == jugadorLocalID) "¡Has Ganado!" else "Has Perdido"
        } else {
            "El Jugador $jugador ganó"
        }

        RecordSubmitter.submitRecord(
            context = context,
            nombre = if(esMultijugador && jugador == jugadorLocalID) "Yo" else "Rival",
            puntaje = (if (jugador == 1) movimientos1 else movimientos2) + 1,
            tiempoJuego = duracionPartidaMs
        )

        AlertDialog.Builder(context)
            .setTitle("Partida Finalizada")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciarJuego() }
            .show()
    }

    private fun mostrarEmpate() {
        AlertDialog.Builder(context)
            .setTitle("¡Empate!")
            .setMessage("Tablero lleno.")
            .setCancelable(false)
            .setPositiveButton("Reiniciar") { _, _ -> reiniciarJuego() }
            .show()
    }

    private fun reiniciarJuego() {
        for (f in 0 until filas) for (c in 0 until columnas) tablero[f][c] = 0
        turnoJugador = 1 // Siempre empieza el 1 (Rojo/Server)
        juegoTerminado = false
        animandoFicha = false
        columnaSeleccionada = 3
        movimientos1 = 0
        movimientos2 = 0
        inicioPartidaMs = System.currentTimeMillis()
        invalidate()
    }

    fun iniciarJuego() {
        inicioPartidaMs = System.currentTimeMillis()
    }
}