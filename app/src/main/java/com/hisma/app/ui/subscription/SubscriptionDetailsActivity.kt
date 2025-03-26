package com.hisma.app.ui.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hisma.app.databinding.ActivitySubscriptionDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SubscriptionDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionDetailsBinding
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurar botones de contacto
        setupContactButtons()

        // Observar datos de suscripción
        observeSubscriptionData()
    }

    private fun setupContactButtons() {
        binding.buttonWhatsapp.setOnClickListener {
            openWhatsApp()
        }

        binding.buttonEmail.setOnClickListener {
            sendEmail()
        }
    }

    private fun observeSubscriptionData() {
        viewModel.subscription.observe(this) { subscription ->
            if (subscription != null) {
                // Actualizar estado de suscripción
                binding.textPlanValue.text = subscription.planType.name
                binding.textStatusValue.text = subscription.status.name

                // Formatear fechas
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.textStartDateValue.text = dateFormat.format(Date(subscription.startDate))
                binding.textEndDateValue.text = dateFormat.format(Date(subscription.endDate))

                // Calcular días restantes
                val currentTime = System.currentTimeMillis()
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(subscription.endDate - currentTime)
                binding.textDaysRemainingValue.text = "$daysRemaining días"

                // Mostrar uso de cambios de aceite
                binding.textOilChangesValue.text = "${subscription.oilChangesUsed}/${subscription.oilChangesLimit}"

                // Cambiar color si quedan pocos días
                if (daysRemaining <= 3) {
                    binding.textDaysRemainingValue.setTextColor(getColor(android.R.color.holo_red_light))
                }
            } else {
                // Valores por defecto si no hay suscripción
                binding.textPlanValue.text = "No disponible"
                binding.textStatusValue.text = "No disponible"
                binding.textStartDateValue.text = "No disponible"
                binding.textEndDateValue.text = "No disponible"
                binding.textDaysRemainingValue.text = "No disponible"
                binding.textOilChangesValue.text = "No disponible"
            }
        }
    }

    private fun openWhatsApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=542604515854&text=Hola,%20deseo%20renovar%20mi%20suscripción%20a%20HISMA"
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback si no está instalado WhatsApp
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://web.whatsapp.com/send?phone=542604515854")
            startActivity(intent)
        }
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:ventas@hisma.com.ar")
        intent.putExtra(Intent.EXTRA_SUBJECT, "Renovación de suscripción HISMA")
        intent.putExtra(Intent.EXTRA_TEXT, "Hola, me gustaría renovar mi suscripción a HISMA.\n\nSaludos")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
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