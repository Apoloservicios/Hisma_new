package com.hisma.app.debug

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.R
import com.hisma.app.domain.model.OilChangeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actividad de prueba para depurar operaciones directas con Firestore
 * NOTA: Esta actividad es solo para propósitos de depuración y no debe ser incluida en la versión final
 */
class TestFirestoreActivity : AppCompatActivity() {

    private val TAG = "TestFirestore"
    private val db = FirebaseFirestore.getInstance()
    private val oilChangesCollection = "oilChanges"

    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_firestore)

        resultTextView = findViewById(R.id.text_result)

        // Botón para crear registro de prueba
        findViewById<Button>(R.id.button_create_record).setOnClickListener {
            createTestRecord()
        }

        // Botón para verificar registros existentes
        findViewById<Button>(R.id.button_check_records).setOnClickListener {
            checkExistingRecords()
        }

        // Botón para verificar colecciones de Firestore
        findViewById<Button>(R.id.button_list_collections).setOnClickListener {
            checkFirestoreCollections()
        }

        // Botón para verificar conexión a Firestore
        findViewById<Button>(R.id.button_test_connection).setOnClickListener {
            testFirestoreConnection()
        }
    }

    /**
     * Crea un registro de prueba en Firestore
     */
    private fun createTestRecord() {
        appendText("Creando registro de prueba...")

        // Crear registro de prueba
        val testRecord = OilChangeRecord(
            id = "",  // Firestore asignará ID
            lubricenterId = "test_lubricenter_id", // Usar un ID real para pruebas
            customerId = "",
            customerName = "Cliente de Prueba",
            customerPhone = "1234567890",
            vehicleId = "",
            vehicleBrand = "Toyota",
            vehicleModel = "Corolla",
            vehiclePlate = "TEST123",
            vehicleYear = 2020,
            oilType = "5W30 Sintético",
            oilBrand = "Mobil",
            oilQuantity = 4.5,
            filterChanged = true,
            filterBrand = "Bosch",
            kilometrage = 50000,
            nextChangeKm = 55000,
            observations = "Registro de prueba creado el ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}",
            createdAt = System.currentTimeMillis(),
            createdBy = "Test User",
            updatedAt = System.currentTimeMillis()
        )

        // Guardar en Firestore
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val docRef = withContext(Dispatchers.IO) {
                    val ref = db.collection(oilChangesCollection).document()
                    val recordWithId = testRecord.copy(id = ref.id)
                    ref.set(recordWithId).await()
                    ref
                }

                appendText("✅ Registro creado exitosamente con ID: ${docRef.id}")
                Toast.makeText(this@TestFirestoreActivity, "Registro creado: ${docRef.id}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear registro de prueba", e)
                appendText("❌ Error al crear registro: ${e.message}")
                Toast.makeText(this@TestFirestoreActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Verifica registros existentes en la colección
     */
    private fun checkExistingRecords() {
        appendText("Verificando registros existentes...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    val querySnapshot = db.collection(oilChangesCollection)
                        .limit(10)  // Limitar a 10 para la prueba
                        .get()
                        .await()

                    querySnapshot.documents.mapNotNull { doc ->
                        val record = doc.toObject(OilChangeRecord::class.java)
                        record?.copy(id = doc.id)
                    }
                }

                if (records.isEmpty()) {
                    appendText("❗ No se encontraron registros")
                } else {
                    appendText("✅ Se encontraron ${records.size} registros:")
                    for (record in records) {
                        appendText("  - ID: ${record.id}")
                        appendText("    Cliente: ${record.customerName}")
                        appendText("    Vehículo: ${record.vehicleBrand} ${record.vehicleModel} (${record.vehiclePlate})")
                        appendText("    LubricenterId: ${record.lubricenterId}")
                        appendText("    Fecha: ${formatDate(record.createdAt)}")
                        appendText("")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al verificar registros", e)
                appendText("❌ Error al verificar registros: ${e.message}")
            }
        }
    }

    /**
     * Verifica las colecciones disponibles en Firestore
     * Nota: No usamos listCollections() porque requiere acceso privilegiado
     */
    private fun checkFirestoreCollections() {
        appendText("Verificando colecciones conocidas...")

        val collectionsToCheck = listOf(
            "oilChanges",
            "lubricenters",
            "users",
            "subscriptions",
            "customers",
            "vehicles"
        )

        CoroutineScope(Dispatchers.Main).launch {
            for (collectionName in collectionsToCheck) {
                try {
                    val count = withContext(Dispatchers.IO) {
                        val querySnapshot = db.collection(collectionName)
                            .limit(1)
                            .get()
                            .await()

                        querySnapshot.size()
                    }

                    if (count > 0) {
                        appendText("✅ Colección '$collectionName' existe y tiene documentos")
                    } else {
                        appendText("⚠️ Colección '$collectionName' existe pero está vacía")
                    }
                } catch (e: Exception) {
                    appendText("❌ Error al acceder a '$collectionName': ${e.message}")
                }
            }

            appendText("")
            appendText("Nota: Firestore no permite listar todas las colecciones desde el cliente.")
        }
    }

    /**
     * Prueba la conexión a Firestore
     */
    private fun testFirestoreConnection() {
        appendText("Probando conexión a Firestore...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Intentamos una operación simple de lectura
                    db.collection("test_connection")
                        .document("test_doc")
                        .get()
                        .await()
                }

                appendText("✅ Conexión exitosa a Firestore")

                // También probamos configuraciones de seguridad
                try {
                    val writeResult = withContext(Dispatchers.IO) {
                        db.collection("test_connection")
                            .document("test_doc")
                            .set(mapOf("timestamp" to System.currentTimeMillis()))
                            .await()
                    }
                    appendText("✅ Escritura de prueba exitosa")
                } catch (e: Exception) {
                    appendText("⚠️ Escritura de prueba fallida: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al probar conexión", e)
                appendText("❌ Error de conexión: ${e.message}")
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

    /**
     * Formatea una fecha en milisegundos a un formato legible
     */
    private fun formatDate(timestamp: Long): String {
        return try {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        } catch (e: Exception) {
            "Fecha inválida"
        }
    }
}