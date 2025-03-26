package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hisma.app.databinding.FragmentRegisterEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterEmployeeFragment : Fragment() {

    private var _binding: FragmentRegisterEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    // Variable para almacenar el ID del lubricentro verificado
    private var verifiedLubricenterId: String? = null

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

        // Configurar botón de verificación
        binding.buttonVerifyLubricenter.setOnClickListener {
            val cuit = binding.editTextLubricenterCuit.text.toString()
            if (cuit.isNotEmpty()) {
                viewModel.verifyLubricenter(cuit)
            } else {
                Toast.makeText(requireContext(), "Ingrese el CUIT del lubricentro", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar botón de registro (inicialmente deshabilitado)
        binding.buttonRegister.isEnabled = false
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val lastName = binding.editTextLastName.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            // Solo permitir registro si el lubricentro ha sido verificado
            verifiedLubricenterId?.let { lubricenterId ->
                viewModel.registerEmployee(name, lastName, email, password, lubricenterId)
            }
        }

        // Observar estado de verificación del lubricentro
        viewModel.verifyLubricenterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.VerifyLubricenterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonVerifyLubricenter.isEnabled = false
                }
                is AuthViewModel.VerifyLubricenterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true

                    // Mostrar los detalles del lubricentro y habilitar registro
                    binding.layoutLubricenterDetails.visibility = View.VISIBLE
                    binding.textLubricenterName.text = "Nombre: ${state.lubricenter.fantasyName}"
                    binding.textLubricenterAddress.text = "Dirección: ${state.lubricenter.address}"
                    binding.textLubricenterPhone.text = "Teléfono: ${state.lubricenter.phone}"

                    // Guardar el ID del lubricentro verificado
                    verifiedLubricenterId = state.lubricenter.id

                    // Habilitar botón de registro
                    binding.buttonRegister.isEnabled = true

                    Toast.makeText(requireContext(), "Lubricentro verificado correctamente", Toast.LENGTH_SHORT).show()
                }
                is AuthViewModel.VerifyLubricenterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonVerifyLubricenter.isEnabled = true
                    binding.layoutLubricenterDetails.visibility = View.GONE
                    binding.buttonRegister.isEnabled = false
                    verifiedLubricenterId = null

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                null -> { /* No hacer nada */ }
            }
        }

        // Observar estado de registro
        viewModel.employeeRegisterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.EmployeeRegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is AuthViewModel.EmployeeRegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Registro exitoso. ¡Bienvenido a HISMA!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthViewModel.EmployeeRegisterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                null -> { /* No hacer nada */ }
            }
        }

        // Configurar texto para ir a login
        binding.textLogin.setOnClickListener {
            viewModel.navigateToLogin()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}