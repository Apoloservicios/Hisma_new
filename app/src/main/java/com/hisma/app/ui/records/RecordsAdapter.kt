package com.hisma.app.ui.records

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hisma.app.databinding.ItemRecordBinding
import com.hisma.app.domain.model.OilChangeRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsAdapter(private val listener: RecordClickListener) :
    ListAdapter<OilChangeRecord, RecordsAdapter.RecordViewHolder>(RecordDiffCallback()) {

    interface RecordClickListener {
        fun onRecordClick(record: OilChangeRecord)
        fun onPdfClick(record: OilChangeRecord)
        fun onWhatsAppClick(record: OilChangeRecord)
        fun onEditClick(record: OilChangeRecord)
        fun onDeleteClick(record: OilChangeRecord)
        fun onMenuClick(view: View, record: OilChangeRecord)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = getItem(position)
        if (record != null) {
            holder.bind(record)
        }
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRecordClick(getItem(position))
                }
            }

            binding.imageButtonPdf.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onPdfClick(getItem(position))
                }
            }

            binding.imageButtonWhatsapp.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onWhatsAppClick(getItem(position))
                }
            }

            binding.imageButtonMenu.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMenuClick(it, getItem(position))
                }
            }
        }

        fun bind(record: OilChangeRecord) {
            // Verificar que los datos no sean nulos antes de usarlos
            binding.textCustomerName.text = record.customerName.takeIf { it.isNotEmpty() } ?: "Cliente sin nombre"

            // Información del vehículo
            val vehicleInfo = StringBuilder()
            if (record.vehicleBrand.isNotEmpty()) {
                vehicleInfo.append(record.vehicleBrand)
            }
            if (record.vehicleModel.isNotEmpty()) {
                if (vehicleInfo.isNotEmpty()) vehicleInfo.append(" ")
                vehicleInfo.append(record.vehicleModel)
            }
            if (record.vehiclePlate.isNotEmpty()) {
                vehicleInfo.append(" - ${record.vehiclePlate}")
            }
            if (record.vehicleYear > 0) {
                vehicleInfo.append(" (${record.vehicleYear})")
            }

            binding.textVehicleInfo.text = if (vehicleInfo.isNotEmpty()) vehicleInfo.toString() else "Sin información del vehículo"

            // Información de aceite y filtro
            val oilInfo = StringBuilder("Aceite: ")
            oilInfo.append(if (record.oilType.isNotEmpty()) record.oilType else "N/A")
            oilInfo.append(" ")
            oilInfo.append(if (record.oilBrand.isNotEmpty()) record.oilBrand else "")
            oilInfo.append(" (${record.oilQuantity}L)")

            val filterInfo = if (record.filterChanged) {
                "Filtro: ${if (record.filterBrand.isNotEmpty()) record.filterBrand else "Cambiado"}"
            } else {
                "Sin cambio de filtro"
            }

            binding.textOilInfo.text = "$oilInfo - $filterInfo"

            // Información de kilometraje
            binding.textKmInfo.text = "KM: ${record.kilometrage} - Próximo: ${record.nextChangeKm}"

            // Fecha formateada
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textDate.text = try {
                dateFormat.format(Date(record.createdAt))
            } catch (e: Exception) {
                dateFormat.format(Date())
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<OilChangeRecord>() {
        override fun areItemsTheSame(oldItem: OilChangeRecord, newItem: OilChangeRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OilChangeRecord, newItem: OilChangeRecord): Boolean {
            return oldItem == newItem
        }
    }
}