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
import com.hisma.app.databinding.FragmentRegisterEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterEmployeeFragment : Fragment() {

    private var _binding: FragmentRegisterEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var lubricenterId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Botón para verificar lubricentro
        binding.buttonVerifyLubricenter.setOnClickListener {
            val cuit = binding.editTextLubricenterCuit.text.toString().trim()
            if (cuit.isNotEmpty()) {
                viewModel.verifyLubricenter(cuit)
            } else {
                Toast.makeText(requireContext(), "Por favor ingrese el CUIT del lubricentro", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de registro
        binding.buttonRegister.setOnClickListener {
            registerEmployee()
        }

        // Texto "Iniciar sesión"
        binding.textLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        // Observar el estado de verificación del lubricentro
        viewModel.verifyLubricenterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.VerifyLubricenterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonVerifyLubricenter.isEnabled = false
                }
                is AuthViewModel.VerifyLubricenterState.Found -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true
                    lubricenterId = state.lubricenter.id

                    // Mostrar los detalles del lubricentro
                    binding.layoutLubricenterDetails.visibility = View.VISIBLE
                    binding.textLubricenterName.text = "Nombre: ${state.lubricenter.fantasyName}"
                    binding.textLubricenterAddress.text = "Dirección: ${state.lubricenter.address}"
                    binding.textLubricenterPhone.text = "Teléfono: ${state.lubricenter.phone}"
                }
                is AuthViewModel.VerifyLubricenterState.NotFound -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true
                    Toast.makeText(requireContext(), "No se encontró ningún lubricentro con ese CUIT", Toast.LENGTH_LONG).show()
                    binding.layoutLubricenterDetails.visibility = View.GONE
                }
                is AuthViewModel.VerifyLubricenterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    binding.layoutLubricenterDetails.visibility = View.GONE
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true
                }
            }
        }

        // Observar el estado del registro de empleado
        viewModel.employeeRegisterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.EmployeeRegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is AuthViewModel.EmployeeRegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registerEmployeeFragment_to_loginFragment)
                }
                is AuthViewModel.EmployeeRegisterState.Error -> {
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

    private fun registerEmployee() {
        // Verificar si el lubricentro ha sido verificado
        if (lubricenterId.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor verifique el lubricentro primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos del formulario
        val name = binding.editTextName.text.toString().trim()
        val lastName = binding.editTextLastName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validar datos
        if (name.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Intentar registrar el empleado
        viewModel.registerEmployee(name, lastName, email, password, lubricenterId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}