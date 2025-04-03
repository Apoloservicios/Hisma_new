package com.hisma.app.ui.records

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.hisma.app.R
import com.hisma.app.databinding.ActivityRecordsListBinding
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.ui.oilchange.EditOilChangeActivity
import com.hisma.app.ui.oilchange.OilChangeViewModel
import com.hisma.app.ui.oilchange.RegisterOilChangeActivity
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "RecordsListActivity"

@AndroidEntryPoint
class RecordsListActivity : AppCompatActivity(), RecordsAdapter.RecordClickListener {

    private lateinit var binding: ActivityRecordsListBinding
    private val recordsViewModel: RecordsViewModel by viewModels()
    private val oilChangeViewModel: OilChangeViewModel by viewModels()
    private lateinit var adapter: RecordsAdapter

    // Launcher para la actividad de edición
    private val editRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar registros después de editar
            recordsViewModel.loadRecords()
            Toast.makeText(this, "Registro actualizado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para la actividad de creación
    private val createRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar registros después de crear
            Log.d(TAG, "Actividad de creación de registro finalizada con éxito. Recargando registros...")
            recordsViewModel.loadRecords()
            Toast.makeText(this, "Registro creado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial de Cambios"

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar búsqueda
        setupSearch()

        // Configurar botón flotante para agregar
        binding.fabAddRecord.setOnClickListener {
            // Abrir actividad para agregar registro
            navigateToCreateRecord()
        }

        // Observar datos
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Recargar registros cuando la actividad vuelve a estar visible
        Log.d(TAG, "onResume: Recargando registros...")
        recordsViewModel.loadRecords()
    }

    private fun setupRecyclerView() {
        adapter = RecordsAdapter(this)
        binding.recyclerRecords.apply {
            layoutManager = LinearLayoutManager(this@RecordsListActivity)
            adapter = this@RecordsListActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.editTextSearch.setOnEditorActionListener { v, _, _ ->
            performSearch(v.text.toString())
            true
        }

        binding.layoutSearch.setEndIconOnClickListener {
            performSearch(binding.editTextSearch.text.toString())
        }
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "Realizando búsqueda con query: '$query'")
        recordsViewModel.searchRecords(query)
    }

    private fun observeViewModel() {
        // Observar registros
        recordsViewModel.records.observe(this) { records ->
            Log.d(TAG, "Registros actualizados: ${records.size} elementos")
            // Actualizar la lista
            adapter.submitList(records)

            // Mostrar estado vacío si aplica
            binding.textEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observar estado de carga
        recordsViewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar mensajes de error o información
        recordsViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                recordsViewModel.clearError()
            }
        }

        // Observar estado de eliminación
        oilChangeViewModel.deleteState.observe(this) { state ->
            when (state) {
                is OilChangeViewModel.SaveState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is OilChangeViewModel.SaveState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registro eliminado correctamente", Toast.LENGTH_SHORT).show()
                    // Recargar registros
                    recordsViewModel.loadRecords()
                }
                is OilChangeViewModel.SaveState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error al eliminar registro: ${state.message}", Toast.LENGTH_LONG).show()
                }
                null -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        // Observar estado del PDF
        oilChangeViewModel.pdfFileState.observe(this) { state ->
            when(state) {
                is OilChangeViewModel.PdfState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is OilChangeViewModel.PdfState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "PDF generado correctamente", Toast.LENGTH_SHORT).show()

                    // Abrir el archivo PDF
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(state.uri, "application/pdf")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "No se pudo abrir el PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is OilChangeViewModel.PdfState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error generando PDF: ${state.message}", Toast.LENGTH_LONG).show()
                }
                null -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onRecordClick(record: OilChangeRecord) {
        // Mostrar detalles del registro
        showRecordDetailsDialog(record)
    }

    override fun onPdfClick(record: OilChangeRecord) {
        Toast.makeText(this, "Generando PDF...", Toast.LENGTH_SHORT).show()
        // Usar el viewModel para generar el PDF
        oilChangeViewModel.generatePdf(this, record.id)
    }

    override fun onWhatsAppClick(record: OilChangeRecord) {
        // Para la funcionalidad de WhatsApp
        if (record.customerPhone.isNotEmpty()) {
            // Compartir por WhatsApp
            recordsViewModel.shareViaWhatsApp(this, record)
        } else {
            Toast.makeText(this, "No hay número de teléfono disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEditClick(record: OilChangeRecord) {
        // Navegar a pantalla de edición
        Log.d(TAG, "Iniciando edición de registro con ID: ${record.id}")
        navigateToEditRecord(record.id)
    }

    override fun onDeleteClick(record: OilChangeRecord) {
        showDeleteConfirmation(record)
    }

    override fun onMenuClick(view: View, record: OilChangeRecord) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_record_item, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    onEditClick(record)
                    true
                }
                R.id.action_delete -> {
                    onDeleteClick(record)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showRecordDetailsDialog(record: OilChangeRecord) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Detalles del Registro")

        // Construir mensaje con detalles
        val message = StringBuilder()
        message.appendLine("Cliente: ${record.customerName}")
        message.appendLine("Vehículo: ${record.vehicleBrand} ${record.vehicleModel}")
        message.appendLine("Patente: ${record.vehiclePlate}")
        if (record.vehicleYear > 0) message.appendLine("Año: ${record.vehicleYear}")
        message.appendLine("\nKilometraje: ${record.kilometrage} km")
        message.appendLine("Próximo cambio: ${record.nextChangeKm} km")
        message.appendLine("\nAceite: ${record.oilType} ${record.oilBrand}")
        message.appendLine("Cantidad: ${record.oilQuantity}L")

        if (record.filterChanged) {
            message.appendLine("Filtro: ${record.filterBrand}")
        }

        if (record.observations.isNotEmpty()) {
            message.appendLine("\nObservaciones:")
            message.appendLine(record.observations)
        }

        builder.setMessage(message.toString())

        // Botones para acciones comunes
        builder.setPositiveButton("Editar") { dialog, _ ->
            dialog.dismiss()
            onEditClick(record)
        }

        builder.setNegativeButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showDeleteConfirmation(record: OilChangeRecord) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Está seguro que desea eliminar este registro? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                oilChangeViewModel.deleteOilChangeRecord(record.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateToEditRecord(recordId: String) {
        Log.d(TAG, "Navegando a editar registro con ID: $recordId")
        // Para debugging, verifica que el recordId no esté vacío
        if (recordId.isEmpty()) {
            Toast.makeText(this, "Error: ID de registro vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, EditOilChangeActivity::class.java)
        intent.putExtra(EditOilChangeActivity.EXTRA_RECORD_ID, recordId)
        editRecordLauncher.launch(intent)
    }

    private fun navigateToCreateRecord() {
        Log.d(TAG, "Navegando a crear nuevo registro")
        val intent = Intent(this, RegisterOilChangeActivity::class.java)
        createRecordLauncher.launch(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}