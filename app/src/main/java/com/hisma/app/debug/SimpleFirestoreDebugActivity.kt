package com.hisma.app.debug

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Herramienta de depuración muy simple para Firestore
 */
class SimpleFirestoreDebugActivity : AppCompatActivity() {

    private val TAG = "SimpleFirestoreDebug"
    private val db = FirebaseFirestore.getInstance()

    private lateinit var lubricenterIdEditText: EditText
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_debug)

        // Inicializar vistas
        lubricenterIdEditText = findViewById(R.id.edit_lubricenter_id)
        resultTextView = findViewById(R.id.text_result)

        // Botón para crear registro de prueba
        findViewById<Button>(R.id.button_create_record).setOnClickListener {
            val lubricenterId = lubricenterIdEditText.text.toString().trim()
            if (lubricenterId.isEmpty()) {
                appendText("❌ Por favor ingresa un ID de lubricentro")
                return@setOnClickListener
            }
            createSimpleRecord(lubricenterId)
        }

        // Botón para verificar registros
        findViewById<Button>(R.id.button_check_records).setOnClickListener {
            val lubricenterId = lubricenterIdEditText.text.toString().trim()
            if (lubricenterId.isEmpty()) {
                appendText("❌ Por favor ingresa un ID de lubricentro")
                return@setOnClickListener
            }
            checkRecords(lubricenterId)
        }

        // Botón para limpiar log
        findViewById<Button>(R.id.button_clear).setOnClickListener {
            resultTextView.text = ""
        }
    }

    /**
     * Crea un registro muy simple directamente en Firestore
     */
    private fun createSimpleRecord(lubricenterId: String) {
        appendText("⏳ Creando registro simple...")

        val recordId = "test_" + UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Crear un mapa simple con los datos mínimos necesarios
        val record = hashMapOf(
            "id" to recordId,
            "lubricenterId" to lubricenterId,
            "customerName" to "Cliente Test ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
            "vehiclePlate" to "TEST${(100..999).random()}",
            "createdAt" to timestamp,
            "updatedAt" to timestamp
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Guardar directamente, evitando usar modelos
                withContext(Dispatchers.IO) {
                    db.collection("oilChanges")
                        .document(recordId)
                        .set(record)
                        .await()
                }

                appendText("✅ Registro creado exitosamente!")
                appendText("ID: $recordId")
                appendText("LubricenterId: $lubricenterId")
                appendText("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}")

                // Inmediatamente verificar si podemos recuperar el registro
                checkRecordExists(recordId)

            } catch (e: Exception) {
                Log.e(TAG, "Error creando registro", e)
                appendText("❌ Error: ${e.message}")
            }
        }
    }

    /**
     * Verifica si un registro específico existe
     */
    private fun checkRecordExists(recordId: String) {
        appendText("\n⏳ Verificando si el registro existe...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val doc = withContext(Dispatchers.IO) {
                    db.collection("oilChanges")
                        .document(recordId)
                        .get()
                        .await()
                }

                if (doc.exists()) {
                    appendText("✅ El registro existe!")
                    appendText("Datos: ${doc.data}")
                } else {
                    appendText("❌ El registro NO existe.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando registro", e)
                appendText("❌ Error: ${e.message}")
            }
        }
    }

    /**
     * Busca registros para un lubricenterId
     */
    private fun checkRecords(lubricenterId: String) {
        appendText("\n⏳ Verificando registros para LubricenterId: $lubricenterId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Consulta sin ordenamiento
                val querySnapshotNoOrder = withContext(Dispatchers.IO) {
                    db.collection("oilChanges")
                        .whereEqualTo("lubricenterId", lubricenterId)
                        .get()
                        .await()
                }

                appendText("Consulta sin ordenamiento: ${querySnapshotNoOrder.size()} resultados")

                if (querySnapshotNoOrder.isEmpty) {
                    appendText("❗ No se encontraron registros")
                } else {
                    appendText("✅ Registros encontrados:")
                    for (doc in querySnapshotNoOrder.documents) {
                        appendText("- ID: ${doc.id}")
                        appendText("  Cliente: ${doc.getString("customerName") ?: "N/A"}")
                        appendText("  Patente: ${doc.getString("vehiclePlate") ?: "N/A"}")
                    }
                }

                // Intentar también con ordenamiento
                try {
                    appendText("\n⏳ Intentando consulta con ordenamiento...")
                    val querySnapshotWithOrder = withContext(Dispatchers.IO) {
                        db.collection("oilChanges")
                            .whereEqualTo("lubricenterId", lubricenterId)
                            .orderBy("createdAt")
                            .get()
                            .await()
                    }

                    appendText("Consulta con ordenamiento: ${querySnapshotWithOrder.size()} resultados")
                } catch (e: Exception) {
                    appendText("❌ Error en consulta con ordenamiento: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando registros", e)
                appendText("❌ Error: ${e.message}")
            }
        }
    }

    /**
     * Agrega texto al TextView de resultados
     */
    private fun appendText(text: String) {
        runOnUiThread {
            resultTextView.append("$text\n")
            // Scroll al final
            val scrollView = resultTextView.parent as? androidx.core.widget.NestedScrollView
            scrollView?.fullScroll(androidx.core.widget.NestedScrollView.FOCUS_DOWN)
        }
    }
}