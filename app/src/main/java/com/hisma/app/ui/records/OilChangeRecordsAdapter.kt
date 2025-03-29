package com.hisma.app.ui.records

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hisma.app.R
import com.hisma.app.databinding.ItemRecordBinding
import com.hisma.app.domain.model.OilChange
import java.text.SimpleDateFormat
import java.util.Locale

class OilChangeRecordsAdapter(
    private val onItemClick: (OilChange) -> Unit,
    private val onPdfClick: (OilChange) -> Unit,
    private val onWhatsappClick: (OilChange) -> Unit
) : ListAdapter<OilChange, OilChangeRecordsAdapter.ViewHolder>(OilChangeDiffCallback()) {

    class ViewHolder(
        private val binding: ItemRecordBinding,
        private val onItemClick: (OilChange) -> Unit,
        private val onPdfClick: (OilChange) -> Unit,
        private val onWhatsappClick: (OilChange) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(oilChange: OilChange) {
            binding.textCustomerName.text = oilChange.customerName
            binding.textVehicleInfo.text = "${oilChange.vehicleBrand} ${oilChange.vehicleModel} - ${oilChange.vehiclePlate} (${oilChange.vehicleYear})"
            binding.textOilInfo.text = "Aceite: ${oilChange.oilViscosity} ${oilChange.oilBrand} (${oilChange.oilQuantity}L)" +
                    if (oilChange.oilFilter) " - Filtro: Sí" else " - Filtro: No"
            binding.textKmInfo.text = "KM: ${oilChange.currentKm} - Próximo: ${oilChange.nextChangeKm}"
            binding.textDate.text = dateFormat.format(oilChange.serviceDate)

            // Manejar clics
            binding.root.setOnClickListener { onItemClick(oilChange) }
            binding.imageButtonPdf.setOnClickListener { onPdfClick(oilChange) }
            binding.imageButtonWhatsapp.setOnClickListener { onWhatsappClick(oilChange) }
            binding.imageButtonMenu.setOnClickListener { view ->
                // Mostrar menú contextual (editar/eliminar)
                showPopupMenu(view, oilChange)
            }
        }

        private fun showPopupMenu(view: View, oilChange: OilChange) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_record_item, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // Navegar a edición (implementar según sea necesario)
                        true
                    }
                    R.id.action_delete -> {
                        // Mostrar diálogo de confirmación para eliminar (implementar según sea necesario)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onPdfClick, onWhatsappClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class OilChangeDiffCallback : DiffUtil.ItemCallback<OilChange>() {
        override fun areItemsTheSame(oldItem: OilChange, newItem: OilChange): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OilChange, newItem: OilChange): Boolean {
            return oldItem == newItem
        }
    }
}