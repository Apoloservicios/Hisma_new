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
        holder.bind(getItem(position))
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
            binding.textCustomerName.text = record.customerName
            binding.textVehicleInfo.text = "${record.vehicleBrand} ${record.vehicleModel} - ${record.vehiclePlate}" +
                    if (record.vehicleYear > 0) " (${record.vehicleYear})" else ""

            // Información de aceite y filtro
            val filterInfo = if (record.filterChanged) "Filtro: ${record.filterBrand}" else "Sin cambio de filtro"
            binding.textOilInfo.text = "Aceite: ${record.oilType} ${record.oilBrand} (${record.oilQuantity}L) - $filterInfo"

            // Información de kilometraje
            binding.textKmInfo.text = "KM: ${record.kilometrage} - Próximo: ${record.nextChangeKm}"

            // Fecha formateada
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.textDate.text = dateFormat.format(Date(record.createdAt))
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