package com.hisma.app.ui.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hisma.app.databinding.ActivitySubscriptionExpiredBinding
import com.hisma.app.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscriptionExpiredActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionExpiredBinding
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionExpiredBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar mensaje personalizado si se recibió desde el intent
        intent.getStringExtra(EXTRA_MESSAGE)?.let { message ->
            binding.textMessage.text = message
        }

        // Configurar botones de contacto
        binding.buttonWhatsapp.setOnClickListener {
            openWhatsApp()
        }

        binding.buttonEmail.setOnClickListener {
            sendEmail()
        }

        // Configurar botón de cerrar sesión
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
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

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
    }
}