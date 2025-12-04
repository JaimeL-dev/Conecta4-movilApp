package com.example.conecta4_proyfin

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TableroActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_MODO = "extra_modo"
        const val MODO_SOLO = "solo"
        const val MODO_SERVER = "server"
        const val MODO_CLIENTE = "cliente"
        const val EXTRA_SERVER_IP = "extra_server_ip" // Nueva constante

        fun iniciar(context: Context, modo: String) {
            val intent = Intent(context, TableroActivity::class.java)
            intent.putExtra(EXTRA_MODO, modo)
            context.startActivity(intent)
        }
    }

    private lateinit var sensorManager: SensorManager
    private var sensorMovimiento: Sensor? = null
    private lateinit var tableroView: TableroView
    private lateinit var btnModo: Button

    // Lógica Multijugador
    private var modoJuego: String = MODO_SOLO
    private var serverIpDirecta: String? = null // IP para conexión directa

    private var gameServer: GameServer? = null
    private var gameClient: GameClient? = null

    // Control de turnos
    private var esMiTurno = true

    private val textoModo = arrayOf(
        "MODO: TOUCH",
        "MODO: SENSOR CONTINUO",
        "MODO: GESTO IZQ-DER"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tablero)

        // 1. Obtener datos del Intent
        modoJuego = intent.getStringExtra(EXTRA_MODO) ?: MODO_SOLO
        serverIpDirecta = intent.getStringExtra(EXTRA_SERVER_IP)

        tableroView = findViewById(R.id.tableroView)
        btnModo = findViewById(R.id.btnModo)

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val tema = prefs.getString("tema", "Clasico") ?: "Clasico"
        tableroView.setTema(tema)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorMovimiento = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        tableroView.modoControl = 0
        btnModo.text = textoModo[0]

        btnModo.setOnClickListener {
            tableroView.modoControl = (tableroView.modoControl + 1) % 3
            btnModo.text = textoModo[tableroView.modoControl]
        }

        // Callback de jugada local
        tableroView.onFichaColocada = { columna ->
            if (modoJuego != MODO_SOLO) {
                onJugadaLocalRealizada(columna)
            }
        }

        // 2. Iniciar lógica según modo
        when (modoJuego) {
            MODO_SERVER -> iniciarModoServidor()
            MODO_CLIENTE -> iniciarModoCliente()
            else -> iniciarModoSolo()
        }
    }

    // --- MODO SOLO ---
    private fun iniciarModoSolo() {
        esMiTurno = true
        tableroView.configurarJugador(1, esMultijugador = false)
        tableroView.isEnabled = true
        tableroView.iniciarJuego()
    }

    // --- MODO SERVIDOR (HOST) ---
    private fun iniciarModoServidor() {
        // Servidor es Jugador 1 (Rojo)
        tableroView.configurarJugador(jugadorID = 1, esMultijugador = true)

        bloquearTablero("Esperando conexión del rival...")
        esMiTurno = true

        gameServer = GameServer(
            onMessageReceived = { col, estado -> runOnUiThread { recibirJugadaRival(col, estado) } },
            onClientConnected = { runOnUiThread {
                Toast.makeText(this, "¡Rival conectado! Tu turno.", Toast.LENGTH_SHORT).show()
                desbloquearTablero()
            }},
            onError = { error -> runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_LONG).show() } }
        )
        gameServer?.start()
        tableroView.iniciarJuego()
    }

    // --- MODO CLIENTE (GUEST) ---
    private fun iniciarModoCliente() {
        // Cliente es Jugador 2 (Amarillo)
        tableroView.configurarJugador(jugadorID = 2, esMultijugador = true)

        bloquearTablero("Conectando al servidor...")
        esMiTurno = false

        gameClient = GameClient(
            onMessageReceived = { col, estado -> runOnUiThread { recibirJugadaRival(col, estado) } },
            onConnected = { ip -> runOnUiThread {
                Toast.makeText(this, "Conectado. Esperando turno del anfitrión.", Toast.LENGTH_SHORT).show()
                // Aún no desbloqueamos, porque el servidor siempre empieza
                title = "Turno del Rival (Anfitrión)"
            }},
            onError = { error -> runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_LONG).show() } }
        )

        // IMPORTANTE: Pasamos la IP directa para evitar re-escaneo UDP
        gameClient?.start(serverIpDirecta)

        tableroView.iniciarJuego()
    }

    // --- LÓGICA DE JUEGO ---
    private fun onJugadaLocalRealizada(columna: Int) {
        if (!esMiTurno) return

        val estadoAEnviar = "TU_TURNO"

        if (modoJuego == MODO_SERVER) {
            gameServer?.sendMove(columna, estadoAEnviar)
        } else if (modoJuego == MODO_CLIENTE) {
            gameClient?.sendMove(columna, estadoAEnviar)
        }

        esMiTurno = false
        bloquearTablero("Turno del rival")
    }

    private fun recibirJugadaRival(columna: Int, estado: String) {
        tableroView.jugarFichaRemota(columna)

        if (estado == "TU_TURNO") {
            esMiTurno = true
            desbloquearTablero()
            Toast.makeText(this, "¡Tu turno!", Toast.LENGTH_SHORT).show()
        } else if (estado == "GANASTE") {
            Toast.makeText(this, "¡Ganaste! El rival perdió.", Toast.LENGTH_LONG).show()
        } else if (estado == "PERDISTE") {
            Toast.makeText(this, "Has perdido.", Toast.LENGTH_LONG).show()
            bloquearTablero("Fin del juego")
        }
    }

    private fun bloquearTablero(mensaje: String) {
        tableroView.isEnabled = false
        btnModo.isEnabled = false
        title = mensaje
    }

    private fun desbloquearTablero() {
        tableroView.isEnabled = true
        btnModo.isEnabled = true
        title = "Conecta 4 - Tu Turno"
    }

    override fun onDestroy() {
        super.onDestroy()
        gameServer?.stop()
        gameClient?.stop()
    }

    // --- SENSORES ---
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensorMovimiento, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (modoJuego != MODO_SOLO && !esMiTurno) return
        if (tableroView.modoControl == 0) return

        val ax = event.values[0]
        val ay = event.values[2]
        tableroView.procesarSensor(ax, ay)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}