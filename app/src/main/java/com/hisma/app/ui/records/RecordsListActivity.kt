package com.hisma.app.ui.records

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.hisma.app.R
import com.hisma.app.databinding.ActivityRecordsListBinding
import com.hisma.app.domain.model.OilChangeRecord
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordsListActivity : AppCompatActivity(), RecordsAdapter.RecordClickListener {

    private lateinit var binding: ActivityRecordsListBinding
    private val viewModel: RecordsViewModel by viewModels()
    private lateinit var adapter: RecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar búsqueda
        setupSearch()

        // Configurar botón flotante para agregar
        binding.fabAddRecord.setOnClickListener {
            // Abrir actividad para agregar registro
            Toast.makeText(this, "Agregar nuevo registro", Toast.LENGTH_SHORT).show()
            // Intent para navegar a la actividad de creación de registro (pendiente de implementar)
            // startActivity(Intent(this, CreateRecordActivity::class.java))
        }

        // Observar datos
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RecordsAdapter(this)
        binding.recyclerRecords.apply {
            layoutManager = LinearLayoutManager(this@RecordsListActivity)
            adapter = this@RecordsListActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            performSearch(v.text.toString())
            true
        }

        binding.layoutSearch.setEndIconOnClickListener {
            performSearch(binding.editTextSearch.text.toString())
        }
    }

    private fun performSearch(query: String) {
        viewModel.searchRecords(query)
    }

    private fun observeViewModel() {
        viewModel.records.observe(this) { records ->
            // Actualizar la lista
            adapter.submitList(records)

            // Mostrar estado vacío si aplica
            binding.textEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onRecordClick(record: OilChangeRecord) {
        // Abrir detalle del registro (pendiente de implementar)
        Toast.makeText(this, "Ver detalle de ${record.customerName}", Toast.LENGTH_SHORT).show()
    }

    override fun onPdfClick(record: OilChangeRecord) {
        // Generar PDF
        viewModel.generatePdf(record)
        Toast.makeText(this, "Generando PDF...", Toast.LENGTH_SHORT).show()
    }

    override fun onWhatsAppClick(record: OilChangeRecord) {
        // Compartir por WhatsApp
        if (record.customerPhone.isNotEmpty()) {
            viewModel.shareViaWhatsApp(this, record)
        } else {
            Toast.makeText(this, "No hay número de teléfono disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMenuClick(view: View, record: OilChangeRecord) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_record_item, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    // Navegar a pantalla de edición (pendiente de implementar)
                    Toast.makeText(this, "Editar registro", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete -> {
                    // Confirmar eliminación
                    showDeleteConfirmation(record)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(record: OilChangeRecord) {
        // Mostrar diálogo de confirmación para eliminar (implementación simplificada)
        android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Está seguro que desea eliminar este registro?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteRecord(record.id) }
            .setNegativeButton("Cancelar", null)
            .show()
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