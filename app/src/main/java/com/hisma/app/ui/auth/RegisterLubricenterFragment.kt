package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hisma.app.R
import com.hisma.app.databinding.FragmentRegisterLubricenterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterLubricenterFragment : Fragment() {

    private var _binding: FragmentRegisterLubricenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterLubricenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Botón de registro
        binding.buttonRegister.setOnClickListener {
            registerLubricenter()
        }

        // Texto "Iniciar sesión"
        binding.textLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        // Observar el estado del registro de lubricentro
        viewModel.lubricenterRegisterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.LubricenterRegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is AuthViewModel.LubricenterRegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registerLubricenterFragment_to_loginFragment)
                }
                is AuthViewModel.LubricenterRegisterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                }
            }
        }
    }

    private fun registerLubricenter() {
        // Obtener datos del formulario
        val ownerName = binding.editTextOwnerName.text.toString().trim()
        val ownerLastName = binding.editTextOwnerLastName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val lubricenterName = binding.editTextLubricenterName.text.toString().trim()
        val cuit = binding.editTextCuit.text.toString().trim()
        val address = binding.editTextAddress.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()

        // Validar datos
        if (ownerName.isEmpty() || email.isEmpty() || password.isEmpty() ||
            lubricenterName.isEmpty() || cuit.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Intentar registrar el lubricentro
        viewModel.registerLubricenter(
            ownerName,
            ownerLastName,
            email,
            password,
            lubricenterName,
            cuit,
            address,
            phone
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}