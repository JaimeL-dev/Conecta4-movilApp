package com.example.conecta4_proyfin

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TableroActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensorMovimiento: Sensor? = null

    private lateinit var tableroView: TableroView
    private lateinit var btnModo: Button

    private val textoModo = arrayOf(
        "MODO: TOUCH",
        "MODO: SENSOR CONTINUO",
        "MODO: GESTO IZQ-DER"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tablero)

        tableroView = findViewById(R.id.tableroView)
        btnModo = findViewById(R.id.btnModo)

        // ================================
        //      CARGAR TEMA SELECCIONADO
        // ================================
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val tema = prefs.getString("tema", "Clasico") ?: "Clasico"
        tableroView.setTema(tema)

        // SENSOR
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorMovimiento = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // MODO DE CONTROL INICIAL
        tableroView.modoControl = 0
        btnModo.text = textoModo[0]

        // BOTÓN PARA CAMBIAR DE MODO
        btnModo.setOnClickListener {
            tableroView.modoControl = (tableroView.modoControl + 1) % 3
            btnModo.text = textoModo[tableroView.modoControl]
        }

        tableroView.iniciarJuego()
    }

    // SENSOR LISTENER
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorMovimiento,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (tableroView.modoControl == 0) return  // Modo touch → ignorar sensor

        val ax = event.values[0]   // izquierda / derecha real
        val ay = event.values[2]   // golpe hacia adelante (gesto)

        tableroView.procesarSensor(ax, ay)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
