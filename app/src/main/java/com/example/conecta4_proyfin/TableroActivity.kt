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
        "MODO: GESTO IZQ-DER + CAÍDA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tablero)

        tableroView = findViewById(R.id.tableroView)
        btnModo = findViewById(R.id.btnModo)

        // Sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorMovimiento = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Modo inicial
        tableroView.modoControl = 0
        btnModo.text = textoModo[0]

        // Cambiar entre TOUCH → SENSOR CONTINUO → GESTO
        btnModo.setOnClickListener {
            tableroView.modoControl = (tableroView.modoControl + 1) % 3
            btnModo.text = textoModo[tableroView.modoControl]
        }

        tableroView.iniciarJuego()
    }

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

        // No usar sensor en modo TOUCH
        if (tableroView.modoControl == 0) return

        /*******************************************************************
         * Orientación del teléfono: vertical, pantalla hacia ti.
         *
         * event.values:
         *   X → inclinación real izquierda/derecha
         *   Y → inclinación arriba/abajo
         *   Z → empujón hacia adelante (lo usamos para soltar la ficha)
         *******************************************************************/
        val ax = event.values[0]  // izquierda/derecha
        val ay = event.values[2]  // gesto empujón hacia adelante (negativo)

        tableroView.procesarSensor(ax, ay)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
