package com.example.conecta4_proyfin

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import androidx.core.view.size
import java.util.concurrent.TimeUnit

class activity_records2 : AppCompatActivity() {

    private lateinit var recordsManager: RecordsManager
    private lateinit var recordsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_records2)


        recordsManager = RecordsManager(this)


        recordsContainer = findViewById(R.id.ll_lista_records)


        val btnVolver: Button = findViewById(R.id.btn_volver_inicio_records)
        btnVolver.setOnClickListener {
            finish()
        }


        cargarYMostrarRecords()
    }


    private fun cargarYMostrarRecords() {
        recordsContainer.removeAllViews()

        val records = recordsManager.obtenerRecords()

        if (records.isEmpty()) {

            val tvNoRecords = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "¡Aún no hay récords! Sé el primero en jugar."
                textSize = 18f
                setTextColor(Color.parseColor("#1A237E"))
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dpToPx(16))
            }
            recordsContainer.addView(tvNoRecords)
            return
        }


        records.forEachIndexed { index, record ->
            val card = crearCardViewParaRecord(index + 1, record)
            recordsContainer.addView(card)
        }
    }

    private fun crearCardViewParaRecord(posicion: Int, record: Record): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12) // Añadir margen inferior
            }
            radius = dpToPx(12).toFloat() // cornerRadius
            cardElevation = dpToPx(6).toFloat() // cardElevation
            setCardBackgroundColor(Color.WHITE)
        }

        val textView = TextView(this).apply {
            // Generar el texto del récord con formato
            text = formatRecordText(posicion, record)
            textSize = 18f
            setTextColor(Color.parseColor("#1A237E"))
            setPadding(dpToPx(16))
        }

        card.addView(textView)
        return card
    }


    private fun formatRecordText(posicion: Int, record: Record): String {
        val icono = when (posicion) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "🔹"
        }


        val minutos = TimeUnit.MILLISECONDS.toMinutes(record.tiempoJuego)
        val segundos = TimeUnit.MILLISECONDS.toSeconds(record.tiempoJuego) -
                TimeUnit.MINUTES.toSeconds(minutos)

        val tiempoFormateado = String.format("%02d:%02d", minutos, segundos)


        return "$icono #${posicion}. ${record.nombre}\n" +
                "   Movimientos: ${record.puntaje} | Tiempo: $tiempoFormateado"
    }


    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}