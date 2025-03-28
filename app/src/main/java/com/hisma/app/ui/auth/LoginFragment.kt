package com.hisma.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.hisma.app.R
import com.hisma.app.databinding.FragmentLoginBinding
import com.hisma.app.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar listeners de clics
        setupClickListeners()

        // Observar cambios en los datos del ViewModel
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            attemptLogin()
        }

        // Texto "Registrarse"
        binding.textRegister.setOnClickListener {
            navigateToRegisterSelection()
        }

        // Texto "¿Olvidaste tu contraseña?"
        binding.textForgotPassword.setOnClickListener {
            showPasswordResetDialog()
        }
    }

    private fun observeViewModel() {
        // Observar el estado de autenticación
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Authenticated -> {
                    binding.progressBar.visibility = View.GONE
                    navigateToDashboard()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validar datos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Intentar iniciar sesión
        viewModel.login(email, password)
    }

    private fun navigateToDashboard() {
        val intent = Intent(requireContext(), DashboardActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Cierra la actividad actual para que no se pueda volver atrás
    }

    private fun navigateToRegisterSelection() {
        try {
            // Intentar navegar usando el ID del destino
            findNavController().navigate(R.id.registerSelectionFragment)
        } catch (e: Exception) {
            // Si falla, intentar usar la acción
            try {
                findNavController().navigate(R.id.action_loginFragment_to_registerSelectionFragment)
            } catch (e: Exception) {
                // Como último recurso, mostrar un mensaje
                Toast.makeText(
                    requireContext(),
                    "Error al navegar: Verificar configuración de navegación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showPasswordResetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Recuperar Contraseña")

        // Configurar el campo de entrada
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "Correo electrónico"
        builder.setView(input)

        // Configurar los botones
        builder.setPositiveButton("Enviar") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                // Llamar al método para enviar el correo de restablecimiento
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(requireContext(), "Por favor ingrese su correo electrónico", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Se ha enviado un correo de recuperación a $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}