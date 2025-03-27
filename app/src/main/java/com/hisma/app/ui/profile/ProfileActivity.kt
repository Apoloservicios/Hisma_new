package com.hisma.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.hisma.app.R
import com.hisma.app.databinding.ActivityProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Cloudinary
        viewModel.initializeCloudinary(this)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurar botones
        setupButtonListeners()

        // Observar datos del lubricentro
        observeLubricenterData()

        // Observar estado de guardado
        observeSaveState()
    }

    private fun setupButtonListeners() {
        binding.buttonChangeLogo.setOnClickListener {
            openImagePicker()
        }

        binding.buttonSave.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun observeLubricenterData() {
        viewModel.lubricenter.observe(this) { lubricenter ->
            if (lubricenter != null) {
                // Rellenar campos con datos actuales
                binding.editTextName.setText(lubricenter.fantasyName)
                binding.editTextCuit.setText(lubricenter.cuit)
                binding.editTextAddress.setText(lubricenter.address)
                binding.editTextPhone.setText(lubricenter.phone)
                binding.editTextEmail.setText(lubricenter.email)
                binding.editTextResponsible.setText(lubricenter.responsible)

                // Cargar logo si existe
                if (lubricenter.logoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(lubricenter.logoUrl)
                        .placeholder(R.drawable.ic_business)
                        .error(R.drawable.ic_business)
                        .into(binding.imageLogo)
                }
            }
        }
    }

    private fun observeSaveState() {
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is ProfileViewModel.SaveState.Loading -> {
                    binding.progressIndicator.visibility = View.VISIBLE
                    binding.buttonSave.isEnabled = false
                }
                is ProfileViewModel.SaveState.Success -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.buttonSave.isEnabled = true
                    Toast.makeText(this, "¡Perfil actualizado correctamente!", Toast.LENGTH_SHORT).show()
                }
                is ProfileViewModel.SaveState.Error -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.buttonSave.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                null -> {
                    binding.progressIndicator.visibility = View.GONE
                    binding.buttonSave.isEnabled = true
                }
            }
        }
    }

    private fun openImagePicker() {
        // Verificar si se necesita permisos para SDK > 32 (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Para versiones anteriores
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, abrir selector de imágenes
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        } else {
            // Permiso denegado
            Toast.makeText(
                this,
                "Se necesita permiso para acceder a la galería",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_business)
            .error(R.drawable.ic_business)
            .into(binding.imageLogo)
    }

    private fun saveProfileChanges() {
        val name = binding.editTextName.text.toString()
        val address = binding.editTextAddress.text.toString()
        val phone = binding.editTextPhone.text.toString()
        val email = binding.editTextEmail.text.toString()
        val responsible = binding.editTextResponsible.text.toString()

        viewModel.updateLubricenter(
            name,
            address,
            phone,
            email,
            responsible,
            selectedImageUri,
            this
        )
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