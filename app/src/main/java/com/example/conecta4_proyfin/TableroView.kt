package com.example.conecta4_proyfin

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

    // Callbacks
    var onFichaColocada: ((Int) -> Unit)? = null
    var onJuegoTerminado: ((Int, Int) -> Unit)? = null

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
     var turnoJugador = 1
    private var juegoTerminado = false
    private var esMultijugador = false
    private var jugadorLocalID = 1

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
     var temaName: String = "Clasico"
    private var imagenRojaSrc: Bitmap? = null
    private var imagenNegraSrc: Bitmap? = null
     var imagenRojaScaled: Bitmap? = null
     var imagenNegraScaled: Bitmap? = null

    // Stats
     var movimientos1 = 0
     var movimientos2 = 0
    private var inicioPartidaMs = 0L
    private var duracionPartidaMs = 0L

    init {
        sonidoFicha = try { MediaPlayer.create(context, R.raw.ficha) } catch (e: Exception) { null }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        temaName = prefs.getString("tema", "Clasico") ?: "Clasico"
        cargarImagenes()
        iniciarJuego()
    }

    // Configuración para Multijugador y Solo
    fun configurarJugador(jugadorID: Int, esMultijugador: Boolean) {
        this.jugadorLocalID = jugadorID
        this.esMultijugador = esMultijugador
        reiniciarJuego()
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

        // FICHA FANTASMA
        if (!juegoTerminado && isEnabled) {
            val cx = columnaSeleccionada * celdaSize + celdaSize / 2
            val cy = celdaSize / 2
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
                    if (img != null) canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
                    else dibujarFichaCanvas(canvas, cx, cy, fichaA_Dibujar, ghost = true)
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
                    if(temaName =="Minimalista"){
                        paint.shader = null
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 6f
                        paint.color = Color.BLACK
                        dibujarFichaCanvas(canvas, cx, cy, jugador, ghost = false)
                    } else {
                        val img = when (jugador) { 1 -> imagenRojaScaled; 2 -> imagenNegraScaled; else -> null }
                        if (img != null) canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
                        else dibujarFichaCanvas(canvas, cx, cy, jugador, false)
                    }
                }
            }
        }

        // ANIMACIÓN
        if (animandoFicha) {
            val cx = animColumnaFinal * celdaSize + celdaSize / 2
            val cy = animYActual
            val img = if (turnoJugador == 1) imagenRojaScaled else imagenNegraScaled
            if (temaName == "Clasico" || img == null) dibujarFichaCanvas(canvas, cx, cy, turnoJugador, false)
            else {
                if(temaName == "Minimalista"){
                    paint.shader = null
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.color = Color.BLACK
                    dibujarFichaCanvas(canvas, cx, cy, turnoJugador, ghost = false)
                } else canvas.drawBitmap(img, cx - img.width / 2, cy - img.height / 2, null)
            }
        }
    }

    private fun dibujarFichaCanvas(canvas: Canvas, cx: Float, cy: Float, jugador: Int, ghost: Boolean) {
        val colorBase = when (jugador) { 1 -> Color.RED; 2 -> Color.YELLOW; else -> Color.LTGRAY }
        paint.setShadowLayer(8f, 0f, 4f, Color.argb(120, 0, 0, 0))
        paint.shader = RadialGradient(cx, cy, radio, Color.WHITE, colorBase, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radio, paint)
        paint.shader = null
        paint.setShadowLayer(1f, 0f, 0f, Color.WHITE)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) return false
        if (event?.action == MotionEvent.ACTION_DOWN && !juegoTerminado) {
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

    private fun moverIzquierda() { columnaSeleccionada = (columnaSeleccionada - 1).coerceAtLeast(0); invalidate() }
    private fun moverDerecha() { columnaSeleccionada = (columnaSeleccionada + 1).coerceAtMost(columnas - 1); invalidate() }

    fun jugarFichaRemota(columna: Int) { post { soltarFicha(columna, esRemoto = true) } }

    // ================================================================
    //  BOT INTELIGENTE
    // ================================================================
    fun jugarBotInteligente() {
        val botID = 2
        val humanoID = 1
        if (turnoJugador != botID || juegoTerminado) return

        var columnaJugad = -1

        // 1. Ofensiva: tratar de ganar
        for (col in 0 until columnas) {
            if (simularJugada(col, botID)) {
                columnaJugad = col
                break
            }
        }

        // 2. Defensiva: bloquear al jugador
        if (columnaJugad == -1) {
            for (col in 0 until columnas) {
                if (simularJugada(col, humanoID)) {
                    columnaJugad = col
                    break
                }
            }
        }

        // 3. Estrategia de centro
        if (columnaJugad == -1) {
            val ordenPreferido = listOf(3, 2, 4, 1, 5, 0, 6)
            for (col in ordenPreferido) {
                if (tablero[0][col] == 0) {
                    columnaJugad = col
                    break
                }
            }
        }

        // ➤ Jugar la ficha real después de decidir la columna
        if (columnaJugad != -1) {
            // Se puede agregar un pequeño delay para ver la animación del bot
            postDelayed({ soltarFicha(columnaJugad, esRemoto = true) }, 400L)
        }
    }



    private fun simularJugada(col: Int, idJugador: Int): Boolean {
        if (tablero[0][col] != 0) return false
        var filaCaida = -1
        for (f in filas - 1 downTo 0) {
            if (tablero[f][col] == 0) {
                filaCaida = f
                break
            }
        }
        if (filaCaida == -1) return false

        tablero[filaCaida][col] = idJugador
        val gana = verificarVictoria(filaCaida, col)
        tablero[filaCaida][col] = 0
        return gana
    }

    // ================================================================

    private fun animarFicha(f: Int, c: Int, onEnd: () -> Unit) {
        animandoFicha = true
        animFilaFinal = f
        animColumnaFinal = c
        val yIni = celdaSize / 2f
        val yFin = f * celdaSize + celdaSize / 2f + celdaSize
        animYActual = yIni
        val anim = ValueAnimator.ofFloat(yIni, yFin)
        anim.duration = DURACION_ANIM
        anim.addUpdateListener { animYActual = it.animatedValue as Float; invalidate() }
        anim.addListener(onEnd = { onEnd() })
        anim.start()
    }

    // --------------------------------------------------------------------------------
    // CORRECCIÓN CRÍTICA AQUÍ: Añadido chequeo de 'juegoTerminado' al principio
    // --------------------------------------------------------------------------------
    fun soltarFicha(columna: Int, esRemoto: Boolean) {
        if (juegoTerminado || animandoFicha || columna !in 0 until columnas) return

        for (f in filas - 1 downTo 0) {
            if (tablero[f][columna] == 0) {
                if (!esRemoto) onFichaColocada?.invoke(columna)
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
                            if (turnoJugador == 1) { movimientos1++; turnoJugador = 2 }
                            else { movimientos2++; turnoJugador = 1 }
                        }
                    }
                    invalidate()
                }
                break
            }
        }
    }

     fun verificarVictoria(f: Int, c: Int): Boolean {
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
        while (ff in 0 until filas && cc in 0 until columnas && tablero[ff][cc] == jugador) { total++; ff += df; cc += dc }
        return total
    }

    private fun verificarEmpate() = tablero[0].all { it != 0 }

     fun mostrarGanador(jugador: Int) {
        duracionPartidaMs = System.currentTimeMillis() - inicioPartidaMs
        val totalMovimientos = if (jugador == 1) movimientos1 else movimientos2
        val nombre = if(esMultijugador) {
            if (jugador == jugadorLocalID) "Yo" else "Rival"
        } else {
            if (jugador == 1) "Jugador Humano" else "CPU"
        }
        RecordSubmitter.submitRecord(context, nombre, totalMovimientos + 1, duracionPartidaMs)
        onJuegoTerminado?.invoke(jugador, totalMovimientos + 1)
    }

    private fun mostrarEmpate() {
        onJuegoTerminado?.invoke(0, movimientos1 + movimientos2)
    }

    fun reiniciarJuego() {
        for (f in 0 until filas) for (c in 0 until columnas) tablero[f][c] = 0
        turnoJugador = 1
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