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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Acelerómetro normal
        sensorMovimiento = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Modo inicial
        tableroView.modoControl = 0
        btnModo.text = textoModo[0]

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
        if (tableroView.modoControl == 0) return  // NO usar sensor en modo touch

        /************************************************************
         * CONVERSIÓN PARA USAR EL TELÉFONO PARADO (VERTICAL)
         *
         * Cuando el teléfono está vertical:
         *  event.values[0] → movimiento izquierda/derecha REAL
         *  event.values[1] → inclinación hacia adelante/atrás (no útil)
         *  event.values[2] → gravedad / golpes
         *
         * Solo mandamos X e Y a TableroView
         ***********************************************************/

        val ax = event.values[0]  // movimiento horizontal real
        val ay = event.values[2]  // usamos Z como vertical para gestos fuertes

        tableroView.procesarSensor(ax, ay)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
