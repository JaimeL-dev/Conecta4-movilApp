package com.example.conecta4_proyfin

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TableroActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_MODO = "extra_modo"
        const val MODO_SOLO = "solo"
        const val MODO_SERVER = "server"
        const val MODO_CLIENTE = "cliente"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val REQUEST_CODE_RESULTADOS = 100
    }

    private lateinit var sensorManager: SensorManager
    private var sensorMovimiento: Sensor? = null
    private lateinit var tableroView: TableroView
    private lateinit var btnModo: Button

    private var modoJuego: String = MODO_SOLO
    private var serverIpDirecta: String? = null
    private var gameServer: GameServer? = null
    private var gameClient: GameClient? = null

    private var esMiTurno = true

    private val textoModo = arrayOf("MODO: TOUCH", "MODO: SENSOR CONTINUO", "MODO: GESTO IZQ-DER")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tablero)

        modoJuego = intent.getStringExtra(EXTRA_MODO) ?: MODO_SOLO
        serverIpDirecta = intent.getStringExtra(EXTRA_SERVER_IP)

        tableroView = findViewById(R.id.tableroView)
        btnModo = findViewById(R.id.btnModo)

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        tableroView.setTema(prefs.getString("tema", "Clasico") ?: "Clasico")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorMovimiento = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        tableroView.modoControl = 0
        btnModo.text = textoModo[0]

        btnModo.setOnClickListener {
            tableroView.modoControl = (tableroView.modoControl + 1) % 3
            btnModo.text = textoModo[tableroView.modoControl]
        }

        // ==============================================================
        //  CONFIGURACIÓN DE TURNOS Y BOT
        // ==============================================================
        tableroView.onFichaColocada = { columna ->

            if (modoJuego == MODO_SOLO) {
                // EL USUARIO JUGÓ -> TURNO DEL BOT

                // 1. Bloquear interacción para que no juegue mientras el bot piensa
                tableroView.isEnabled = false

                // 2. Esperar 1 segundo para dar realismo
                Handler(Looper.getMainLooper()).postDelayed({
                    // 3. El bot inteligente calcula y juega
                    tableroView.jugarBotInteligente()

                    // 4. Devolvemos el control al humano (si no terminó el juego)
                    // Nota: TableroView maneja internamente 'juegoTerminado',
                    // pero aquí reactivamos los inputs.
                    tableroView.isEnabled = true
                }, 1000)

            } else {
                // LÓGICA MULTIJUGADOR
                onJugadaLocalRealizada(columna)
            }
        }

        tableroView.onJuegoTerminado = { ganador, movimientos ->
            irAPantallaResultados(ganador, movimientos)
        }

        when (modoJuego) {
            MODO_SERVER -> iniciarModoServidor()
            MODO_CLIENTE -> iniciarModoCliente()
            else -> iniciarModoSolo()
        }
    }

    private fun iniciarModoSolo() {
        esMiTurno = true
        // En modo solo, el humano es Jugador 1
        tableroView.configurarJugador(1, esMultijugador = false)
        tableroView.isEnabled = true
        tableroView.iniciarJuego()
    }

    private fun iniciarModoServidor() {
        tableroView.configurarJugador(1, esMultijugador = true)
        bloquearTablero("Esperando conexión del rival...")
        esMiTurno = true
        gameServer = GameServer(
            onMessageReceived = { col, estado -> runOnUiThread { recibirJugadaRival(col, estado) } },
            onClientConnected = { runOnUiThread { Toast.makeText(this, "¡Rival conectado! Tu turno.", Toast.LENGTH_SHORT).show(); desbloquearTablero() }},
            onError = { error -> runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_LONG).show() } }
        )
        gameServer?.start()
        tableroView.iniciarJuego()
    }

    private fun iniciarModoCliente() {
        tableroView.configurarJugador(2, esMultijugador = true)
        bloquearTablero("Conectando al servidor...")
        esMiTurno = false
        gameClient = GameClient(
            onMessageReceived = { col, estado -> runOnUiThread { recibirJugadaRival(col, estado) } },
            onConnected = { runOnUiThread { Toast.makeText(this, "Conectado.", Toast.LENGTH_SHORT).show(); title = "Turno del Rival (Anfitrión)" }},
            onError = { error -> runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_LONG).show() } }
        )
        gameClient?.start(serverIpDirecta)
        tableroView.iniciarJuego()
    }

    private fun onJugadaLocalRealizada(columna: Int) {
        if (!esMiTurno) return
        val estadoAEnviar = "TU_TURNO"
        if (modoJuego == MODO_SERVER) gameServer?.sendMove(columna, estadoAEnviar)
        else if (modoJuego == MODO_CLIENTE) gameClient?.sendMove(columna, estadoAEnviar)
        esMiTurno = false
        bloquearTablero("Turno del rival")
    }

    private fun recibirJugadaRival(columna: Int, estado: String) {
        if (columna == -1 && estado == "RESET") {
            finishActivity(REQUEST_CODE_RESULTADOS)
            tableroView.reiniciarJuego()
            if (modoJuego == MODO_SERVER) {
                esMiTurno = true
                desbloquearTablero()
                Toast.makeText(this, "Rival reinició. Tu turno.", Toast.LENGTH_LONG).show()
            } else {
                esMiTurno = false
                bloquearTablero("Rival reinició. Su turno.")
                Toast.makeText(this, "Rival reinició la partida.", Toast.LENGTH_LONG).show()
            }
            return
        }

        tableroView.jugarFichaRemota(columna)
        if (estado == "TU_TURNO") {
            esMiTurno = true
            desbloquearTablero()
            Toast.makeText(this, "¡Tu turno!", Toast.LENGTH_SHORT).show()
        } else if (estado == "GANASTE") {
            // Animación continua
        } else if (estado == "PERDISTE") {
            bloquearTablero("Fin del juego")
        }
    }

    private fun irAPantallaResultados(ganador: Int, movimientos: Int) {
        val intent = Intent(this, activity_resultados2::class.java)
        intent.putExtra(activity_resultados2.EXTRA_GANADOR, ganador)
        intent.putExtra(activity_resultados2.EXTRA_MOVIMIENTOS, movimientos)
        val esMulti = (modoJuego == MODO_SERVER || modoJuego == MODO_CLIENTE)
        intent.putExtra(activity_resultados2.EXTRA_ES_MULTIJUGADOR, esMulti)
        val miId = if (modoJuego == MODO_CLIENTE) 2 else 1
        intent.putExtra(activity_resultados2.EXTRA_SOY_JUGADOR_ID, miId)
        startActivityForResult(intent, REQUEST_CODE_RESULTADOS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RESULTADOS) {
            if (resultCode == RESULT_OK) {
                tableroView.reiniciarJuego()
                if (modoJuego == MODO_SERVER) {
                    esMiTurno = true
                    desbloquearTablero()
                    gameServer?.sendRestart()
                } else if (modoJuego == MODO_CLIENTE) {
                    esMiTurno = false
                    bloquearTablero("Reiniciado. Turno Rival")
                    gameClient?.sendRestart()
                } else {
                    desbloquearTablero()
                }
            } else {
                finish()
            }
        }
    }

    private fun bloquearTablero(mensaje: String) { tableroView.isEnabled = false; btnModo.isEnabled = false; title = mensaje }
    private fun desbloquearTablero() { tableroView.isEnabled = true; btnModo.isEnabled = true; title = "Conecta 4 - Tu Turno" }

    override fun onDestroy() {
        super.onDestroy()
        gameServer?.stop()
        gameClient?.stop()
    }

    override fun onResume() { super.onResume(); sensorManager.registerListener(this, sensorMovimiento, SensorManager.SENSOR_DELAY_GAME) }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || (modoJuego != MODO_SOLO && !esMiTurno) || tableroView.modoControl == 0) return
        tableroView.procesarSensor(event.values[0], event.values[2])
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}